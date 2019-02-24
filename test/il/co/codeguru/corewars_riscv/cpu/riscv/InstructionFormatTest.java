package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.*;
import il.co.codeguru.corewars_riscv.utils.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstructionFormatTest {

    @Before
    public void setUp() {
        Logger.setTestingMode();
    }

    @Test
    public void testZeroOpcode() {
        InstructionFormatBase a = new InstructionFormatBase(0);
        assertEquals(0, a.getOpcode());
    }

    @Test
    public void testOpCodeIs7LSB() {
        InstructionFormatBase a = new InstructionFormatBase((short) 0xFFFFFFFF);
        assertEquals(0x7F, a.getOpcode());
    }

    /*
     * InstructionFormatI
     */

    @Test
    public void testGoodInstructionI() {
        //addi x1,x4,14
        InstructionFormatI i = new InstructionFormatI(0x00e20093);
        assertEquals(RV32I.Opcodes.Addi.getOpcode(), i.getOpcode());
        assertEquals(1, i.getRd());
        assertEquals(RV32I.Opcodes.Addi.getFunct3(), i.getFunct3());
        assertEquals(4, i.getRs1());
        assertEquals(14, i.getImmediate());
    }

    @Test
    public void testGoodInstructionIConstructor() {
        InstructionFormatI i = new InstructionFormatI(RV32I.Opcodes.Addi.getOpcode(), 1, RV32I.Opcodes.Addi.getFunct3(), 4, 14);
        assertEquals(0x00e20093, i.getRaw());
    }

    @Test
    public void testAllZeroInstructionI() {
        InstructionFormatI a = new InstructionFormatI(0);
        assertEquals(0, a.getRd());
        assertEquals(0, a.getFunct3());
        assertEquals(0, a.getRs1());
        assertEquals(0, a.getImmediate());
    }

    @Test
    public void testAllZeroInstructionIConstructor() {
        InstructionFormatI i = new InstructionFormatI((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
        assertEquals(0, i.getRaw());
    }

    @Test
    public void testAllOneInstructionIConstructor() {
        InstructionFormatI i = new InstructionFormatI(-1, -1, -1, -1, -1);
        assertEquals(0xFFFFFFFF, i.getRaw());
    }

    /*
     * InstructionFormatU
     */

    @Test
    public void testGoodInstructionU() {
        //lui x14,10
        InstructionFormatU a = new InstructionFormatU(0x0000a737);
        assertEquals(RV32I.Opcodes.Lui.getOpcode(), a.getOpcode());
        assertEquals(14, a.getRd());
        assertEquals(10, a.getImmediate());
    }

    @Test
    public void testGoodInstructionUConstructor() {
        InstructionFormatU a = new InstructionFormatU(RV32I.Opcodes.Lui.getOpcode(), 14, 10);
        assertEquals(0x0000a737, a.getRaw());
    }

    @Test
    public void testAllZeroInstructionU() {
        InstructionFormatU a = new InstructionFormatU(0);
        assertEquals(0, a.getRd());
        assertEquals(0, a.getImmediate());
    }

    @Test
    public void testAllZeroInstructionUConstructor() {
        InstructionFormatU a = new InstructionFormatU((byte) 0, (byte) 0, 0);
        assertEquals(0, a.getRaw());
    }

    @Test
    public void testAllOneInstructionUConstructor() {
        InstructionFormatU a = new InstructionFormatU(-1, -1, -1);
        assertEquals(0xFFFFFFFF, a.getRaw());
    }

    /*
     * InstructionFormatR
     */

    @Test
    public void testGoodInstructionR() {
        //add x7,x13,x21
        InstructionFormatR a = new InstructionFormatR(0x015683b3);
        assertEquals(RV32I.Opcodes.Add.getOpcode(), a.getOpcode());
        assertEquals(7, a.getRd());
        assertEquals(RV32I.Opcodes.Add.getFunct3(), a.getFunct3());
        assertEquals(13, a.getRs1());
        assertEquals(21, a.getRs2());
        assertEquals(RV32I.Opcodes.Add.getFunct7(), a.getFunct7());
    }

    @Test
    public void testGoodInstructionRConstructor() {
        InstructionFormatR a = new InstructionFormatR(RV32I.Opcodes.Add.getOpcode(), 7, RV32I.Opcodes.Add.getFunct3(), 13, 21, RV32I.Opcodes.Add.getFunct7());
        assertEquals(0x015683b3, a.getRaw());
    }

    @Test
    public void testAllZeroInstructionR() {
        InstructionFormatR a = new InstructionFormatR(0);
        assertEquals(0, a.getRd());
        assertEquals(0, a.getFunct3());
        assertEquals(0, a.getRs1());
        assertEquals(0, a.getRs2());
        assertEquals(0, a.getFunct7());
    }

    @Test
    public void testAllZeroInstructionRConstructor() {
        InstructionFormatR a = new InstructionFormatR((byte) 0, (byte) 0, 0, 0, 0, 0);
        assertEquals(0, a.getRaw());
    }

    @Test
    public void testAllOneInstructionRConstructor() {
        InstructionFormatR a = new InstructionFormatR(-1, -1, -1, -1, -1, -1);
        assertEquals(0xFFFFFFFF, a.getRaw());
    }

    /*
     * InstructionFormatS
     */

    @Test
    public void testGoodInstructionS() {
        //sw x2,0(x1)
        InstructionFormatS a = new InstructionFormatS(0x0020A023);
        assertEquals(RV32I.Opcodes.Sw.getOpcode(), a.getOpcode());
        assertEquals(RV32I.Opcodes.Sw.getFunct3(), a.getFunct3());
        assertEquals(1, a.getRs1());
        assertEquals(2, a.getRs2());
        assertEquals(0, a.getImm());
    }

    @Test
    public void testGoodInstructionSConstructor() {
        InstructionFormatS a = new InstructionFormatS(RV32I.Opcodes.Sw.getOpcode(), RV32I.Opcodes.Sw.getFunct3(), 1, 2, 0);
        assertEquals(0x0020A023, a.getRaw());
    }

    @Test
    public void testZeroInstructionS() {
        InstructionFormatS a = new InstructionFormatS(0);
        assertEquals(0, a.getFunct3());
        assertEquals(0, a.getRs1());
        assertEquals(0, a.getRs2());
        assertEquals(0, a.getImm());
    }

    @Test
    public void testAllZeroInstructionSConstructor() {
        InstructionFormatS a = new InstructionFormatS((byte) 0, (byte) 0, 0, 0, 0);
        assertEquals(0, a.getRaw());
    }

    @Test
    public void testAllOneInstructionSConstructor() {
        InstructionFormatS a = new InstructionFormatS(-1, -1, -1, -1, -1);
        assertEquals(0xFFFFFFFF, a.getRaw());
    }

    /*
     * InstructionFormatUJ
     */

    @Test
    public void testGoodPositiveInstructionUJ() {
        //jal x9, 4
        InstructionFormatUJ a = new InstructionFormatUJ(0x004004ef);
        assertEquals(RV32I.Opcodes.Jal.getOpcode(), a.getOpcode());
        assertEquals(9, a.getRd());
        assertEquals(4, a.getImmediate());
    }

    @Test
    public void testGoodPositiveInstructionUJConstructor() {
        InstructionFormatUJ a = new InstructionFormatUJ(0x6f, 0, -8);
        assertEquals(0xff9ff06f, a.getRaw());
    }

    @Test
    public void testAllZeroInstructionUJ() {
        InstructionFormatUJ a = new InstructionFormatUJ(0);
        assertEquals(0, a.getRd());
        assertEquals(0, a.getImmediate());
    }

    @Test
    public void testGoodNegativeInstructionUJ() {
        //jal x0, -8
        InstructionFormatUJ a = new InstructionFormatUJ(0xff9ff06f);
        assertEquals(RV32I.Opcodes.Jal.getOpcode(), a.getOpcode());
        assertEquals(0, a.getRd());
        assertEquals(-8, a.getImmediate());
    }

    @Test
    public void testGoodNegativeInstructionUJConstructor() {
        InstructionFormatUJ a = new InstructionFormatUJ(0x6f, 9, 4);
        assertEquals(0x004004ef, a.getRaw());
    }

    @Test
    public void testAllZeroInstructionUJConstructor() {
        InstructionFormatUJ a = new InstructionFormatUJ((byte) 0, (byte) 0, 0);
        assertEquals(0, a.getRaw());
    }

    /*
     * InstructionFormatSB
     */

    @Test
    public void testZeroInstructionSB() {
        InstructionFormatS a = new InstructionFormatS(0);
        assertEquals(0, a.getFunct3());
        assertEquals(0, a.getRs1());
        assertEquals(0, a.getRs2());
        assertEquals(0, a.getImm());
    }

    @Test
    public void testGoodInstructionSB() {
        // beq x1,x3,0
        InstructionFormatS a = new InstructionFormatS(0x00308063);
        assertEquals(RV32I.Opcodes.Beq.getOpcode(), a.getOpcode());
        assertEquals(RV32I.Opcodes.Beq.getFunct3(), a.getFunct3());
        assertEquals(1, a.getRs1());
        assertEquals(3, a.getRs2());
        assertEquals(0, a.getImm());
    }

    @Test
    public void testGoodInstructionSBConstructor() {
        InstructionFormatS a = new InstructionFormatS(99, 0, 1, 3, 0);
        assertEquals(0x00308063, a.getRaw());
    }

    @Test
    public void testAllZeroInstructionSBConstructor() {
        InstructionFormatS a = new InstructionFormatS((byte) 0, (byte) 0, 0, 0, 0);
        assertEquals(0, a.getRaw());
    }

    @Test
    public void testAllOneInstructionSBConstructor() {
        InstructionFormatS a = new InstructionFormatS(-1, -1, -1, -1, -1);
        assertEquals(0xFFFFFFFF, a.getRaw());
    }

}
