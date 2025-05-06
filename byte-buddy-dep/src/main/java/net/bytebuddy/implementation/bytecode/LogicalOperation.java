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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A class that performs stack manipulation for logical operators.
 */
public class LogicalOperation {
    private LogicalOperation() {
    }

    /**
     * StackManipulation for logical negation {@code !}.
     * Calculates the negation of the boolean value on the operand stack and pushes it onto the stack.
     */
    public final static StackManipulation NOT = new Not();

    private static class Not extends StackManipulation.AbstractBase {
        /**
         * {@inheritDoc}
         */
        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context context) {
            Label elseLabel = new Label();
            Label endLabel = new Label();

            methodVisitor.visitJumpInsn(Opcodes.IFNE, elseLabel);

            // then block
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

            // else block
            methodVisitor.visitLabel(elseLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(Opcodes.ICONST_0);

            methodVisitor.visitLabel(endLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});

            return new Size(0, 1);
        }
    }

    /**
     * StackManipulation for logical AND {@code &&}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class And implements StackManipulation {
        private final StackManipulation leftOperand;
        private final StackManipulation rightOperand;

        private And(StackManipulation leftOperand, StackManipulation rightOperand) {
            this.leftOperand = leftOperand;
            this.rightOperand = rightOperand;
        }

        /**
         * Logical AND {@code &&}.
         *
         * @param leftOperand  The left operand. A {@link StackManipulation} that returns a boolean value.
         *                     The {@link StackManipulation.Size} must have sizeImpact of 1 and maximalSize must be greater than 0.
         * @param rightOperand The right operand. A {@link StackManipulation} that returns a boolean value.
         *                     Executed when the left operand is true.
         *                     The {@link StackManipulation.Size} must have sizeImpact of 1 and maximalSize must be greater than 0.
         */
        public static And of(StackManipulation leftOperand, StackManipulation rightOperand) {
            return new And(leftOperand, rightOperand);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isValid() {
            return leftOperand.isValid() && rightOperand.isValid();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context context) {
            Label falseLabel = new Label();
            Label endLabel = new Label();

            Size leftSize = leftOperand.apply(methodVisitor, context);

            if (leftSize.getSizeImpact() != 1) {
                throw new IllegalArgumentException("The StackManipulation.Size.sizeImpact of leftOperand must be 1.");
            }
            if (leftSize.getMaximalSize() <= 0) {
                throw new IllegalArgumentException("The StackManipulation.Size.maximalSize of leftOperand must be greater than 0.");
            }

            methodVisitor.visitJumpInsn(Opcodes.IFEQ, falseLabel);

            Size rightSize = rightOperand.apply(methodVisitor, context);

            if (rightSize.getSizeImpact() != 1) {
                throw new IllegalArgumentException("The StackManipulation.Size.sizeImpact of rightOperand must be 1.");
            }
            if (rightSize.getMaximalSize() <= 0) {
                throw new IllegalArgumentException("The StackManipulation.Size.maximalSize of rightOperand must be greater than 0.");
            }

            methodVisitor.visitJumpInsn(Opcodes.IFEQ, falseLabel);

            // true block
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

            // false block
            methodVisitor.visitLabel(falseLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(Opcodes.ICONST_0);

            methodVisitor.visitLabel(endLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});

            return new Size(1, Math.max(leftSize.getMaximalSize(), rightSize.getMaximalSize()));
        }
    }

    /**
     * StackManipulation for logical OR {@code ||}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    public static class Or implements StackManipulation {
        private final StackManipulation leftOperand;
        private final StackManipulation rightOperand;

        private Or(StackManipulation leftOperand, StackManipulation rightOperand) {
            this.leftOperand = leftOperand;
            this.rightOperand = rightOperand;
        }

        /**
         * Logical OR {@code ||}.
         *
         * @param leftOperand  The left operand. A {@link StackManipulation} that returns a boolean value.
         *                     The {@link StackManipulation.Size} must have sizeImpact of 1 and maximalSize must be greater than 0.
         * @param rightOperand The right operand. A {@link StackManipulation} that returns a boolean value.
         *                     Executed when the left operand is false.
         *                     The {@link StackManipulation.Size} must have sizeImpact of 1 and maximalSize must be greater than 0.
         */
        public static Or of(StackManipulation leftOperand, StackManipulation rightOperand) {
            return new Or(leftOperand, rightOperand);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isValid() {
            return leftOperand.isValid() && rightOperand.isValid();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context context) {
            Label trueLabel = new Label();
            Label falseLabel = new Label();
            Label endLabel = new Label();

            Size leftSize = leftOperand.apply(methodVisitor, context);

            if (leftSize.getSizeImpact() != 1) {
                throw new IllegalArgumentException("The StackManipulation.Size.sizeImpact of leftOperand must be 1.");
            }
            if (leftSize.getMaximalSize() <= 0) {
                throw new IllegalArgumentException("The StackManipulation.Size.maximalSize of leftOperand must be greater than 0.");
            }

            methodVisitor.visitJumpInsn(Opcodes.IFNE, trueLabel);

            Size rightSize = rightOperand.apply(methodVisitor, context);

            if (rightSize.getSizeImpact() != 1) {
                throw new IllegalArgumentException("The StackManipulation.Size.sizeImpact of rightOperand must be 1.");
            }
            if (rightSize.getMaximalSize() <= 0) {
                throw new IllegalArgumentException("The StackManipulation.Size.maximalSize of rightOperand must be greater than 0.");
            }

            methodVisitor.visitJumpInsn(Opcodes.IFEQ, falseLabel);

            // true block
            methodVisitor.visitLabel(trueLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

            // false block
            methodVisitor.visitLabel(falseLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(Opcodes.ICONST_0);

            methodVisitor.visitLabel(endLabel);
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});

            return new Size(1, Math.max(leftSize.getMaximalSize(), rightSize.getMaximalSize()));
        }
    }
}
