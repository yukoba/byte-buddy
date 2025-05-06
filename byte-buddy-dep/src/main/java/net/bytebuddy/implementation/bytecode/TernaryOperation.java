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
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * StackManipulation for ternary operator {@code ? :}. This class includes both ternary operators and if statements.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TernaryOperation implements StackManipulation {
    private final StackManipulation condition;
    private final StackManipulation thenPart;
    private final StackManipulation elsePart;

    private TernaryOperation(StackManipulation condition, StackManipulation thenPart, StackManipulation elsePart) {
        this.condition = condition;
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    /**
     * Ternary operator {@code ? :}.
     * <p>
     * When using this class, you need to construct a stack map frame,
     * so you must call
     * {@code DynamicType.Builder.visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES))}
     * before {@link DynamicType.Builder#make()}.
     *
     * @param condition The condition expression. A {@link StackManipulation} that returns a boolean value.
     *                  {@link StackManipulation.Size} must have sizeImpact of 1. maximalSize must be 1 or greater.
     * @param thenPart  The {@link StackManipulation} to be executed if the condition is true.
     * @param elsePart  The {@link StackManipulation} to be executed if the condition is false.
     *                  The sizeImpact of {@link StackManipulation.Size} for thenPart and elsePart must be the same.
     */
    public static TernaryOperation of(StackManipulation condition, StackManipulation thenPart, StackManipulation elsePart) {
        return new TernaryOperation(condition, thenPart, elsePart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return condition.isValid() && thenPart.isValid() && elsePart.isValid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context context) {
        Label elseLabel = new Label();
        Label endLabel = new Label();

        // condition
        Size conditionSize = condition.apply(methodVisitor, context);
        if (conditionSize.getSizeImpact() != 1) {
            throw new IllegalArgumentException("The StackManipulation.Size.sizeImpact of condition must be 1.");
        }
        if (conditionSize.getMaximalSize() <= 0) {
            throw new IllegalArgumentException("The StackManipulation.Size.maximalSize of condition must be greater than 0.");
        }

        // if
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, elseLabel);

        // then block
        Size thenSize = thenPart.apply(methodVisitor, context);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        // else block
        methodVisitor.visitLabel(elseLabel);
        Size elseSize = elsePart.apply(methodVisitor, context);

        if (thenSize.getSizeImpact() != elseSize.getSizeImpact()) {
            throw new IllegalArgumentException("The StackManipulation.Size.sizeImpact of thenPart and elsePart must be same.");
        }

        // end block
        methodVisitor.visitLabel(endLabel);

        int maximalSize = Math.max(conditionSize.getMaximalSize(), Math.max(thenSize.getMaximalSize(), elseSize.getMaximalSize()));
        return new Size(thenSize.getSizeImpact(), maximalSize);
    }
}
