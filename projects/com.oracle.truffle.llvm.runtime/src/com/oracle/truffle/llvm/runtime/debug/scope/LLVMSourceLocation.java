/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.runtime.debug.scope;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.debug.LLVMSourceSymbol;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class LLVMSourceLocation {

    private static final SourceSection UNAVAILABLE_SECTION;

    static {
        final Source source = Source.newBuilder("Source unavailable!").name("<unavailable>").mimeType("text/plain").build();
        UNAVAILABLE_SECTION = source.createUnavailableSection();
    }

    private static final List<LLVMSourceSymbol> NO_SYMBOLS = Collections.emptyList();

    public enum Kind {
        TYPE,
        LINE,
        MODULE,
        BLOCK,
        FUNCTION,
        NAMESPACE,
        COMPILEUNIT,
        FILE,
        GLOBAL,
        LOCAL,
        UNKNOWN;
    }

    private final LLVMSourceLocation parent;
    private final Kind kind;
    private final String name;

    private LLVMSourceLocation(LLVMSourceLocation parent, Kind kind, String name) {
        this.parent = parent;
        this.kind = kind;
        this.name = name;
    }

    public LLVMSourceLocation getParent() {
        return parent;
    }

    public Kind getKind() {
        return kind;
    }

    public abstract SourceSection getSourceSection();

    public abstract String describeFile();

    public abstract String describeLocation();

    public void addSymbol(@SuppressWarnings("unused") LLVMSourceSymbol symbol) {
    }

    public boolean hasSymbols() {
        return false;
    }

    public List<LLVMSourceSymbol> getSymbols() {
        return NO_SYMBOLS;
    }

    public LLVMSourceLocation getCompileUnit() {
        if (kind == Kind.COMPILEUNIT) {
            return this;

        } else if (parent != null) {
            return parent.getCompileUnit();

        } else {
            return null;
        }
    }

    @TruffleBoundary
    public String getName() {
        switch (kind) {
            case NAMESPACE: {
                if (name != null) {
                    return "namespace " + name;
                } else {
                    return "namespace";
                }
            }

            case FILE: {
                return String.format("<%s>", describeFile());
            }

            case COMPILEUNIT:
                return "<static>";

            case MODULE:
                if (name != null) {
                    return "module " + name;
                } else {
                    return "<module>";
                }

            case FUNCTION: {
                if (name != null) {
                    return "function " + name;
                } else {
                    return "<function>";
                }
            }

            case BLOCK:
                return "<block>";

            case LINE:
                return String.format("<%s>", describeLocation());

            case TYPE: {
                if (name != null) {
                    return name;
                } else {
                    return "<type>";
                }
            }

            case GLOBAL:
            case LOCAL:
                if (name != null) {
                    return name;
                } else {
                    return "<symbol>";
                }

            default:
                return "<scope>";
        }
    }

    private static class LineScope extends LLVMSourceLocation {

        private final SourceSection sourceSection;

        LineScope(LLVMSourceLocation parent, Kind kind, String name, SourceSection sourceSection) {
            super(parent, kind, name);
            this.sourceSection = sourceSection;
        }

        @Override
        public SourceSection getSourceSection() {
            return sourceSection;
        }

        @Override
        public String describeFile() {
            return sourceSection.getSource().getName();
        }

        @Override
        @TruffleBoundary
        public String describeLocation() {
            final String sourceName = sourceSection.getSource().getName();
            final int line = sourceSection.getStartLine();
            final int col = sourceSection.getStartColumn();
            final StringBuilder sb = new StringBuilder(sourceName);
            if (sourceSection.isAvailable()) {
                if (line >= 0) {
                    sb.append(':').append(line);
                    if (col >= 0) {
                        sb.append(':').append(col);
                    }
                }
            }
            return sb.toString();
        }
    }

    private static class DefaultScope extends LineScope {

        private final List<LLVMSourceSymbol> symbols;

        DefaultScope(LLVMSourceLocation parent, Kind kind, String name, SourceSection sourceSection) {
            super(parent, kind, name, sourceSection);
            this.symbols = new LinkedList<>();
        }

        @TruffleBoundary
        @Override
        public void addSymbol(LLVMSourceSymbol symbol) {
            if (symbol != null) {
                symbols.add(symbol);
            }
        }

        @TruffleBoundary
        @Override
        public boolean hasSymbols() {
            return !symbols.isEmpty();
        }

        @Override
        public List<LLVMSourceSymbol> getSymbols() {
            return symbols;
        }
    }

    private static final class FunctionScope extends DefaultScope {

        private final LLVMSourceLocation compileUnit;

        FunctionScope(LLVMSourceLocation parent, Kind kind, String name, SourceSection sourceSection, LLVMSourceLocation compileUnit) {
            super(parent, kind, name, sourceSection);
            this.compileUnit = compileUnit;
        }

        @Override
        public LLVMSourceLocation getCompileUnit() {
            return compileUnit;
        }
    }

    private static final class UnavailableScope extends LLVMSourceLocation {

        private final String file;
        private final int line;
        private final int col;

        UnavailableScope(LLVMSourceLocation parent, Kind kind, String name, String file, int line, int col) {
            super(parent, kind, name);
            this.file = file;
            this.line = line;
            this.col = col;
        }

        @Override
        public SourceSection getSourceSection() {
            return UNAVAILABLE_SECTION;
        }

        @Override
        public String describeFile() {
            return file != null ? file : "<unavailable file>";
        }

        @TruffleBoundary
        @Override
        public String describeLocation() {
            final StringBuilder sb = new StringBuilder(describeFile());
            if (line >= 0) {
                sb.append(':').append(line);
                if (col >= 0) {
                    sb.append(':').append(col);
                }
            }
            return sb.toString();
        }

    }

    public static LLVMSourceLocation create(LLVMSourceLocation parent, LLVMSourceLocation.Kind kind, String name, SourceSection sourceSection, LLVMSourceLocation compileUnit) {
        assert sourceSection != null;

        switch (kind) {
            case LINE:
            case GLOBAL:
            case LOCAL:
                return new LineScope(parent, kind, name, sourceSection);

            case FUNCTION:
                if (compileUnit != null) {
                    return new FunctionScope(parent, kind, name, sourceSection, compileUnit);
                } else {
                    return new DefaultScope(parent, kind, name, sourceSection);
                }

            default:
                return new DefaultScope(parent, kind, name, sourceSection);
        }
    }

    public static LLVMSourceLocation createUnavailable(LLVMSourceLocation.Kind kind, String name, String file, int line, int col) {
        return new UnavailableScope(null, kind, name, file, line, col);
    }

    public static LLVMSourceLocation createBitcodeFunction(String name, SourceSection simpleSection) {
        return new DefaultScope(null, Kind.FUNCTION, name, simpleSection);
    }

    public static LLVMSourceLocation createUnknown(SourceSection sourceSection) {
        return new LineScope(null, Kind.UNKNOWN, "<unknown>", sourceSection != null ? sourceSection : UNAVAILABLE_SECTION);
    }
}
