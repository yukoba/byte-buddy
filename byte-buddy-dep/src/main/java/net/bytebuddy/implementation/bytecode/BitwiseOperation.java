/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Stack manipulation for bitwise operators.
 */
public enum BitwiseOperation implements StackManipulation {
    /**
     * Bitwise NOT operation {@code ~} for integer values.
     */
    INTEGER_NOT(new Size(0, 1)),

    /**
     * Bitwise AND operation {@code &} for integer values.
     */
    INTEGER_AND(StackSize.SINGLE.toDecreasingSize()),

    /**
     * Bitwise OR operation {@code |} for integer values.
     */
    INTEGER_OR(StackSize.SINGLE.toDecreasingSize()),

    /**
     * Bitwise XOR operation {@code ^} for integer values.
     */
    INTEGER_XOR(StackSize.SINGLE.toDecreasingSize()),

    /**
     * Bitwise NOT operation {@code ~} for long values.
     */
    LONG_NOT(new Size(0, 2)),

    /**
     * Bitwise AND operation {@code &} for long values.
     */
    LONG_AND(StackSize.DOUBLE.toDecreasingSize()),

    /**
     * Bitwise OR operation {@code |} for long values.
     */
    LONG_OR(StackSize.DOUBLE.toDecreasingSize()),

    /**
     * Bitwise XOR operation {@code ^} for long values.
     */
    LONG_XOR(StackSize.DOUBLE.toDecreasingSize());

    private final Size size;

    BitwiseOperation(Size size) {
        this.size = size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context context) {
        switch (this) {
            case INTEGER_NOT:
                methodVisitor.visitInsn(Opcodes.ICONST_M1);
                methodVisitor.visitInsn(Opcodes.IXOR);
                break;
            case INTEGER_AND:
                methodVisitor.visitInsn(Opcodes.IAND);
                break;
            case INTEGER_OR:
                methodVisitor.visitInsn(Opcodes.IOR);
                break;
            case INTEGER_XOR:
                methodVisitor.visitInsn(Opcodes.IXOR);
                break;
            case LONG_NOT:
                methodVisitor.visitLdcInsn(-1L);
                methodVisitor.visitInsn(Opcodes.LXOR);
                break;
            case LONG_AND:
                methodVisitor.visitInsn(Opcodes.LAND);
                break;
            case LONG_OR:
                methodVisitor.visitInsn(Opcodes.LOR);
                break;
            case LONG_XOR:
                methodVisitor.visitInsn(Opcodes.LXOR);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
        return size;
    }
}
