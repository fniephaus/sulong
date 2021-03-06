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
package com.oracle.truffle.llvm.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.LLVMSharedGlobalVariable;
import com.oracle.truffle.llvm.runtime.interop.LLVMAddressMessageResolutionNode.LLVMAddressReadMessageResolutionNode;
import com.oracle.truffle.llvm.runtime.interop.LLVMAddressMessageResolutionNode.LLVMAddressWriteMessageResolutionNode;
import com.oracle.truffle.llvm.runtime.interop.LLVMAddressMessageResolutionNodeFactory.LLVMAddressReadMessageResolutionNodeGen;
import com.oracle.truffle.llvm.runtime.interop.LLVMAddressMessageResolutionNodeFactory.LLVMAddressWriteMessageResolutionNodeGen;

@MessageResolution(receiverType = LLVMSharedGlobalVariable.class)
public class LLVMGlobalVariableMessageResolution {

    @Resolve(message = "READ")
    public abstract static class ForeignGlobalRead extends Node {

        @Child private LLVMAddressReadMessageResolutionNode node;

        protected Object access(VirtualFrame frame, LLVMSharedGlobalVariable receiver, long index) {
            return access(frame, receiver, (int) index);
        }

        protected Object access(VirtualFrame frame, LLVMSharedGlobalVariable receiver, int index) {
            if (node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                node = insert(LLVMAddressReadMessageResolutionNodeGen.create());
            }
            return node.executeWithTarget(frame, receiver, index);
        }

        @SuppressWarnings("unused")
        protected Object access(VirtualFrame frame, LLVMSharedGlobalVariable receiver, String name) {
            CompilerDirectives.transferToInterpreter();
            String message = String.format("Identifier %s is currently unsupported. Please use a numeric index to access a C pointer.", name);
            throw UnknownIdentifierException.raise(message);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class ForeignGlobalWrite extends Node {

        @Child private LLVMAddressWriteMessageResolutionNode node;

        protected Object access(VirtualFrame frame, LLVMSharedGlobalVariable receiver, long index, Object value) {
            return access(frame, receiver, (int) index, value);
        }

        protected Object access(VirtualFrame frame, LLVMSharedGlobalVariable receiver, int index, Object value) {
            if (node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                node = insert(LLVMAddressWriteMessageResolutionNodeGen.create());
            }
            return node.executeWithTarget(frame, receiver, index, value);
        }

        @SuppressWarnings("unused")
        protected Object access(VirtualFrame frame, LLVMSharedGlobalVariable receiver, String name, Object value) {
            CompilerDirectives.transferToInterpreter();
            String message = String.format("Identifier %s is currently unsupported. Please use a numeric index to access a C pointer.", name);
            throw UnknownIdentifierException.raise(message);
        }
    }
}
