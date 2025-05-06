package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class LogicalOperationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {LogicalOperation.NOT},
                {LogicalOperation.And.of(IntegerConstant.forValue(true), IntegerConstant.forValue(false))},
                {LogicalOperation.Or.of(IntegerConstant.forValue(false), IntegerConstant.forValue(true))},
        });
    }

    private final StackManipulation stackManipulation;

    public LogicalOperationTest(StackManipulation stackManipulation) {
        this.stackManipulation = stackManipulation;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testLogicalOperation() {
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);

        if (stackManipulation == LogicalOperation.NOT) {
            verifyNot(size);
        } else if (stackManipulation instanceof LogicalOperation.And) {
            verifyAnd(size);
        } else if (stackManipulation instanceof LogicalOperation.Or) {
            verifyOr(size);
        }

        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    private void verifyNot(StackManipulation.Size size) {
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(1));

        verify(methodVisitor).visitJumpInsn(eq(Opcodes.IFNE), any(Label.class));

        // then block
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.GOTO), any(Label.class));

        // else block
        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);

        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
    }

    private void verifyAnd(StackManipulation.Size size) {
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));

        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_1); // true constant
        verify(methodVisitor, times(2)).visitJumpInsn(eq(Opcodes.IFEQ), any(Label.class));
        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_0); // false constant
        verify(methodVisitor, times(2)).visitJumpInsn(eq(Opcodes.IFEQ), any(Label.class));

        // true block
        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_1);
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.GOTO), any(Label.class));

        // false block
        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_0);

        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
    }

    private void verifyOr(StackManipulation.Size size) {
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));

        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_0); // false constant
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.IFNE), any(Label.class));
        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_1); // true constant
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.IFEQ), any(Label.class));

        // true block
        verify(methodVisitor, times(3)).visitLabel(any(Label.class));
        verify(methodVisitor, times(2)).visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_1);
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.GOTO), any(Label.class));

        // false block
        verify(methodVisitor, times(3)).visitLabel(any(Label.class));
        verify(methodVisitor, times(2)).visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        verify(methodVisitor, times(2)).visitInsn(Opcodes.ICONST_0);

        verify(methodVisitor, times(3)).visitLabel(any(Label.class));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
    }
}
