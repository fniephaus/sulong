/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.listeners.constants;

import java.util.List;

import com.oracle.truffle.llvm.parser.listeners.Types;
import com.oracle.truffle.llvm.parser.model.generators.ConstantGenerator;
import com.oracle.truffle.llvm.runtime.types.Type;

public final class ConstantsVersion {

    public static class ConstantsV32 extends Constants {

        public ConstantsV32(Types types, List<Type> symbols, ConstantGenerator generator) {
            super(types, symbols, generator);
        }

        @Override
        protected void createGetElementPointerExpression(long[] args, boolean isInbounds) {
            int[] indices = new int[(args.length >> 1) - 1];

            for (int i = 0; i < indices.length; i++) {
                indices[i] = (int) args[((i + 1) << 1) + 1];
            }

            generator.createGetElementPointerExpression(type, (int) args[1], indices, isInbounds);
        }

    }

    public static class ConstantsV38 extends Constants {

        public ConstantsV38(Types types, List<Type> symbols, ConstantGenerator generator) {
            super(types, symbols, generator);
        }

        @Override
        protected void createGetElementPointerExpression(long[] args, boolean isInbounds) {
            int[] indices = new int[((args.length - 1) >> 1) - 1];

            for (int i = 0; i < indices.length; i++) {
                indices[i] = (int) args[(i + 2) << 1];
            }

            generator.createGetElementPointerExpression(type, (int) args[2], indices, isInbounds);
        }
    }
}