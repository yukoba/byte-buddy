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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class TernaryOperationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TernaryOperation.of(IntegerConstant.forValue(true), IntegerConstant.forValue(4), IntegerConstant.forValue(5))},
        });
    }

    private final StackManipulation stackManipulation;

    public TernaryOperationTest(StackManipulation stackManipulation) {
        this.stackManipulation = stackManipulation;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testTernaryOperation() {
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(1));
        assertThat(size.getSizeImpact(), is(1));
        verifyCode();
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    private void verifyCode() {
        // condition
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);

        // if
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.IFEQ), any(Label.class));

        // then block
        verify(methodVisitor).visitInsn(Opcodes.ICONST_4);
        verify(methodVisitor).visitJumpInsn(eq(Opcodes.GOTO), any(Label.class));

        // else block
        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_5);

        // end block
        verify(methodVisitor, times(2)).visitLabel(any(Label.class));
    }
}
