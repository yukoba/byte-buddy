package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Parameterized.class)
public class BitwiseOperationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BitwiseOperation.INTEGER_NOT, new StackManipulation.Size(0, 1)},
                {BitwiseOperation.INTEGER_AND, StackSize.SINGLE.toDecreasingSize()},
                {BitwiseOperation.INTEGER_OR, StackSize.SINGLE.toDecreasingSize()},
                {BitwiseOperation.INTEGER_XOR, StackSize.SINGLE.toDecreasingSize()},

                {BitwiseOperation.LONG_NOT, new StackManipulation.Size(0, 2)},
                {BitwiseOperation.LONG_AND, StackSize.DOUBLE.toDecreasingSize()},
                {BitwiseOperation.LONG_OR, StackSize.DOUBLE.toDecreasingSize()},
                {BitwiseOperation.LONG_XOR, StackSize.DOUBLE.toDecreasingSize()},
        });
    }

    private final StackManipulation stackManipulation;

    private final StackManipulation.Size stackSize;

    public BitwiseOperationTest(StackManipulation stackManipulation, StackManipulation.Size stackSize) {
        this.stackManipulation = stackManipulation;
        this.stackSize = stackSize;
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testBitwiseOperation() {
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getMaximalSize(), is(stackSize.getMaximalSize()));
        assertThat(size.getSizeImpact(), is(stackSize.getSizeImpact()));

        if (stackManipulation == BitwiseOperation.INTEGER_NOT) {
            verify(methodVisitor).visitInsn(Opcodes.ICONST_M1);
            verify(methodVisitor).visitInsn(Opcodes.IXOR);
        } else if (stackManipulation == BitwiseOperation.INTEGER_AND) {
            verify(methodVisitor).visitInsn(Opcodes.IAND);
        } else if (stackManipulation == BitwiseOperation.INTEGER_OR) {
            verify(methodVisitor).visitInsn(Opcodes.IOR);
        } else if (stackManipulation == BitwiseOperation.INTEGER_XOR) {
            verify(methodVisitor).visitInsn(Opcodes.IXOR);
        } else if (stackManipulation == BitwiseOperation.LONG_NOT) {
            verify(methodVisitor).visitLdcInsn(-1L);
            verify(methodVisitor).visitInsn(Opcodes.LXOR);
        } else if (stackManipulation == BitwiseOperation.LONG_AND) {
            verify(methodVisitor).visitInsn(Opcodes.LAND);
        } else if (stackManipulation == BitwiseOperation.LONG_OR) {
            verify(methodVisitor).visitInsn(Opcodes.LOR);
        } else if (stackManipulation == BitwiseOperation.LONG_XOR) {
            verify(methodVisitor).visitInsn(Opcodes.LXOR);
        }

        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
