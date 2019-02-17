package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuRiscV;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.RawMemory;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats.CInstructionFormatBase;
import il.co.codeguru.corewars_riscv.memory.MemoryException;
import il.co.codeguru.corewars_riscv.utils.Logger;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


@RunWith(JUnitParamsRunner.class)
public class InstructionTest {
    private static final int RS1 = 8;
    private static final int RS2 = 9;
    private static final int RD = 10;
    private final int VAL = 25;


    private CpuStateRiscV state;
    private CpuRiscV cpu;
    private Memory memory;

    @Before
    public void setUp() {
        state = new CpuStateRiscV();
        memory = new RawMemory(0x10000);
        cpu = new CpuRiscV(state, memory);
        Logger.setTestingMode();
    }

    private void loadInstruction(CInstructionFormatBase i) {
        state.setPc(0);
        cpu.getMemory().storeWord(state.getPc(), i.getRaw());
    }

    @Test
    @Parameters({
            " 0, 10, 10",
            " 5,  5, 10",
            " 5, -1,  4",
            " 0, -1, -1",
            "-1, -1, -2",
            "" + Integer.MAX_VALUE + ", 1," + Integer.MIN_VALUE
    })
    public void testCAdd(int rs1, int rs2, int result) throws MemoryException, CpuException
    {
        state.setReg(RS1, rs1);
        state.setReg(RS2, rs2);
        loadInstruction(RV32C.cInstructionFormatCR(RV32C.Opcodes.CADD, RS1, RS2));

        cpu.nextOpcode();

        assertEquals(result, state.getReg(RS1));
        assertEquals(2, state.getPc());
    }

    @Test
    public void testMv() throws MemoryException, CpuException
    {
        state.setReg(RS2, 5);
        loadInstruction(RV32C.cInstructionFormatCR(RV32C.Opcodes.CMV, RS1, RS2));

        cpu.nextOpcode();

        assertEquals(5, state.getReg(RS1));
        assertEquals(2, state.getPc());
    }

    @Test
    public void testJr() throws MemoryException, CpuException
    {
        state.setReg(RS1, 24);
        loadInstruction(RV32C.cInstructionFormatCR(RV32C.Opcodes.CJR, RS1, 0));

        cpu.nextOpcode();

        assertEquals(24, state.getPc());
    }

    @Test
    public void testJalr() throws MemoryException, CpuException
    {
        state.setReg(RS1, 24);
        loadInstruction(RV32C.cInstructionFormatCR(RV32C.Opcodes.CJALR, RS1, 0));

        cpu.nextOpcode();

        assertEquals(24, state.getPc());
    }

    @Test
    @Parameters({
            " 0, 10, 10",
            " 5,  5, 10",
            " 5, -1,  4",
            " 0, -1, -1",
            "-1, -1, -2",
            "" + Integer.MAX_VALUE + ", 1," + Integer.MIN_VALUE
    })
    public void testCAddi(int rs1, int imm, int result) throws MemoryException, CpuException
    {
        state.setReg(RS1, rs1);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CADDI, RS1, imm));

        cpu.nextOpcode();

        assertEquals(result, state.getReg(RS1));
        assertEquals(2, state.getPc());
    }

    @Test
    @Parameters({
            " 1, 2,  4",
            " 3, 2, 12",
            "-1, 2, -4",
            "" + Integer.MIN_VALUE + ", 1, 0"
    })
    public void testSlli(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CSLLI, RS1, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    public void testLi() throws MemoryException, CpuException
    {
        state.setReg(RS1, 124);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CLI, RS1, VAL));
        cpu.nextOpcode();
        assertEquals(VAL, state.getReg(RS1));

        state.setReg(RS1, 124);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CLI, RS1, -VAL));
        cpu.nextOpcode();
        assertEquals(-VAL, state.getReg(RS1));
    }

    @Test
    @Parameters({
            " 0, 10, 40960",
            " 5,  5,  20485",
            " 5, -1, -4091",
            " 0, -1, -4096",
            "-1, -1, -1",
            "-1,  0, 4095"
    })
    public void testLui(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CLUI, RS1, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    public void testLwsp() throws MemoryException, CpuException {
        state.setReg(2, 15);
        memory.storeWord(15, VAL);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CLWSP, RS1, 0));
        cpu.nextOpcode();
        assertEquals(VAL, state.getReg(RS1));

        state.setReg(2, 15);
        memory.storeWord(35, VAL);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CLWSP, RS1, 20));
        cpu.nextOpcode();
        assertEquals(VAL, state.getReg(RS1));

        state.setReg(2, 0);
        memory.storeWord( (-15) & 0xFFFF, VAL);
        loadInstruction(RV32C.cInstructionFormatCI(RV32C.Opcodes.CLWSP, RS1, -15));
        cpu.nextOpcode();
        assertNotEquals(VAL, state.getReg(RS1));
    }

    @Test
    public void testSwsp() throws MemoryException, CpuException {
        state.setReg(RS1, VAL);
        state.setReg(2, 15);
        loadInstruction(RV32C.cInstructionFormatCSSwithWord(RV32C.Opcodes.CSWSP, RS1, 0));
        cpu.nextOpcode();
        assertEquals(VAL, memory.loadWord(15));

        state.setReg(RS1, VAL);
        state.setReg(2, 15);
        loadInstruction(RV32C.cInstructionFormatCSSwithWord(RV32C.Opcodes.CSWSP, RS1, 20));
        cpu.nextOpcode();
        assertEquals(VAL, memory.loadWord(35));

        state.setReg(RS1, VAL);
        state.setReg(2, 0);
        // 252 is the largest offset that can be in SWSP
        loadInstruction(RV32C.cInstructionFormatCSSwithWord(RV32C.Opcodes.CSWSP, RS1, 252));
        cpu.nextOpcode();
        assertEquals(VAL, memory.loadWord(252));
    }

    @Test
    public void testAddi4spn() throws MemoryException, CpuException
    {
        state.setReg(2, 0x12);
        loadInstruction(RV32C.cInstructionFormatCIW(RV32C.Opcodes.CADDI4SPN, RS1, 0x80));
        cpu.nextOpcode();
        assertEquals(0x12 + 0x20, state.getReg(RS1));
    }

    @Test
    public void testLw() throws MemoryException, CpuException {
        state.setReg(RS1, 15);
        memory.storeWord( 15, VAL);
        loadInstruction(RV32C.cInstructionFormatCL(RV32C.Opcodes.CLW, RD, RS1, 0));
        cpu.nextOpcode();
        assertEquals(VAL, state.getReg(RD));

        state.setReg(RS1, 15);
        memory.storeWord(35, VAL);
        loadInstruction(RV32C.cInstructionFormatCL(RV32C.Opcodes.CLW, RD, RS1, 20));
        cpu.nextOpcode();
        assertEquals(VAL, state.getReg(RD ));

        state.setReg(RS1, 0);
        memory.storeWord((-15) & 0xFFFF, VAL);
        loadInstruction(RV32C.cInstructionFormatCL(RV32C.Opcodes.CLW, RD, RS1, -15));
        cpu.nextOpcode();
        assertNotEquals(VAL, state.getReg(RD));
    }

    @Test
    public void testSw() throws MemoryException, CpuException {
        state.setReg(RS2, VAL);
        state.setReg(RS1, 15);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.CSW, RS1, RS2, 0));
        cpu.nextOpcode();
        assertEquals(VAL, memory.loadWord(15));

        state.setReg(RS2, VAL);
        state.setReg(RS1, 15);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.CSW, RS1, RS2,20));
        cpu.nextOpcode();
        assertEquals(VAL, memory.loadWord(35));

        state.setReg(RS2, VAL);
        state.setReg(RS1, 0);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.CSW, RS1, RS2,-15));
        cpu.nextOpcode();
        assertNotEquals(VAL, memory.loadWord((-15) & 0xFFFF));
    }

    @Test
    @Parameters({
            "5, 6, 4",
            "5, 5, 5",
            "5, 0, 0",
            "5, -1, 5"
    })
    public void testAnd(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg1);
        state.setReg(RS2, reg2);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.CAND, RS1, RS2));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    @Parameters({
            "1, 2, 3",
            "5, 4, 5",
            "5, 0, 5",
            "5, -1, -1"
    })
    public void testOr(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg1);
        state.setReg(RS2, reg2);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.COR, RS1, RS2));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    @Parameters({
            " 1,  2,  3",
            " 3, 10,  9",
            " 3, -1, -4",
            "-4, -1,  3",
            " 5,  0,  5"
    })
    public void testXor(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg1);
        state.setReg(RS2, reg2);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.CXOR, RS1, RS2));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    @Parameters({
            " 10, 0, 10",
            " 10,  5, 5",
            " 4, -1,  5",
            " 1, 2, -1",
            "-1, -1, 0",
            Integer.MIN_VALUE + ", 1, " + Integer.MAX_VALUE
    })
    public void testSub(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg1);
        state.setReg(RS2, reg2);
        loadInstruction(RV32C.cInstructionFormatCS(RV32C.Opcodes.CSUB, RS1, RS2));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    @Parameters({
            " 4, 2, 1",
            "12, 2, 3",
            " 1, 1, 0",
            " 0, 1, 0",
            "-1, 1, " + Integer.MAX_VALUE
    })
    public void testSrli(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg);
        loadInstruction(RV32C.cInstructionFormatCB(RV32C.Opcodes.CSRLI, RS1, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    @Parameters({
            " 4, 2, 1",
            "12, 2, 3",
            " 1, 1, 0",
            " 0, 1, 0",
            "-1, 1, -1"
    })
    public void testSrai(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg);
        loadInstruction(RV32C.cInstructionFormatCB(RV32C.Opcodes.CSRAI, RS1, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    @Parameters({
            "5, 6, 4",
            "5, 5, 5",
            "5, 0, 0",
            "5, -1, 5"
    })
    public void testAndi(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1, reg);
        loadInstruction(RV32C.cInstructionFormatCB(RV32C.Opcodes.CANDI, RS1, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RS1));
    }

    @Test
    public void testBeqz() throws MemoryException, CpuException {
        state.setReg(RS1, 0);
        loadInstruction(RV32C.cInstructionFormatCBBranch(RV32C.Opcodes.CBEQZ, RS1, 8));
        cpu.nextOpcode();
        assertEquals(8, state.getPc());

        state.setReg(RS1, 0);
        loadInstruction(RV32C.cInstructionFormatCBBranch(RV32C.Opcodes.CBEQZ, RS1, -8));
        cpu.nextOpcode();
        assertEquals(-8, state.getPc());

        state.setReg(RS1, 5);
        loadInstruction(RV32C.cInstructionFormatCBBranch(RV32C.Opcodes.CBEQZ, RS1, -8));
        cpu.nextOpcode();
        assertEquals(2, state.getPc());
    }

    @Test
    public void testBnez() throws MemoryException, CpuException {
        state.setReg(RS1, 5);
        loadInstruction(RV32C.cInstructionFormatCBBranch(RV32C.Opcodes.CBNEZ, RS1, 8));
        cpu.nextOpcode();
        assertEquals(8, state.getPc());

        state.setReg(RS1, 5);
        loadInstruction(RV32C.cInstructionFormatCBBranch(RV32C.Opcodes.CBNEZ, RS1, -8));
        cpu.nextOpcode();
        assertEquals(-8, state.getPc());

        state.setReg(RS1, 0);
        loadInstruction(RV32C.cInstructionFormatCBBranch(RV32C.Opcodes.CBNEZ, RS1, -8));
        cpu.nextOpcode();
        assertEquals(2, state.getPc());
    }

    @Test
    public void testJ() throws MemoryException, CpuException {
        loadInstruction(RV32C.cInstructionFormatCJ(RV32C.Opcodes.CJ, 24));

        cpu.nextOpcode();

        assertEquals(24, state.getPc());

        loadInstruction(RV32C.cInstructionFormatCJ(RV32C.Opcodes.CJ, -8));

        cpu.nextOpcode();

        assertEquals(-8, state.getPc());
    }

    @Test
    public void testJal() throws  MemoryException, CpuException {
        loadInstruction(RV32C.cInstructionFormatCJ(RV32C.Opcodes.CJAL, 24));

        cpu.nextOpcode();

        assertEquals(24, state.getPc());
        assertEquals(2, state.getReg(1));

        loadInstruction(RV32C.cInstructionFormatCJ(RV32C.Opcodes.CJAL, -8));

        cpu.nextOpcode();

        assertEquals(-8, state.getPc());
    }
}
