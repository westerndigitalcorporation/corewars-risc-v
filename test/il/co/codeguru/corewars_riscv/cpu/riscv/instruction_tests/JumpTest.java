package il.co.codeguru.corewars_riscv.cpu.riscv.instruction_tests;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.exceptions.MisalignedMemoryLoadException;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuRiscV;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.RawMemory;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.RV32I;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats.InstructionFormatBase;
import il.co.codeguru.corewars_riscv.memory.MemoryException;
import il.co.codeguru.corewars_riscv.utils.Logger;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitParamsRunner.class)
public class JumpTest {
    private static final int x0 = 0;
    private static final int RS1_INDEX = 1;
    private static final int RS2_INDEX = 2;
    private static final int RD_INDEX = 3;

    private CpuStateRiscV state;
    private CpuRiscV cpu;

    @Before
    public void setUp() {
        state = new CpuStateRiscV();
        Memory memory = new RawMemory(0x10000);
        cpu = new CpuRiscV(state, memory);
        Logger.setTestingMode();

    }

    private void loadInstruction(InstructionFormatBase i) {
        cpu.getMemory().storeWord(state.getPc(), i.getRaw());
    }

    @Test
    public void testJ() throws MemoryException, CpuException {
        state.setPc(0);
        loadInstruction(RV32I.instructionUJ(RV32I.Opcodes.Jal, x0, 4));
        cpu.nextOpcode();
        assertEquals(4, state.getPc());
    }

    @Test
    public void testJal() throws MemoryException, CpuException {
        state.setPc(12);
        loadInstruction(RV32I.instructionUJ(RV32I.Opcodes.Jal, RD_INDEX, -8));
        cpu.nextOpcode();
        assertEquals(4, state.getPc());
        assertEquals(16, state.getReg(RD_INDEX));
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testJalMisalignedAddress() throws MemoryException, CpuException {
        state.setPc(24);
        loadInstruction(RV32I.instructionUJ(RV32I.Opcodes.Jal, RD_INDEX, 14));
        cpu.nextOpcode();
    }

    @Test
    @Parameters({
            " 0, 8, 0,    8, 4",
            "12,32, 0,   32,16",
            "12,32,16,   48,16"
    })
    public void testJalr(int initialPc, int reg, int imm, int expectedPc, int expectedReg) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        state.setPc(initialPc);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Jalr, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expectedPc, state.getPc());
        assertEquals(expectedReg, state.getReg(RD_INDEX));
    }
    
    
    /*
     * Test for the case where jalr is called with the source register and destination register are the same
     * such as <code>jalr x3, x3</code>
     */
    @Test
    @Parameters({
            " 0, 8, 0,    8, 4",
            "12,32, 0,   32,16",
            "12,32,16,   48,16"
    })
    public void testJalrSameSrcDst(int initialPc, int reg, int imm, int expectedPc, int expectedReg) throws MemoryException, CpuException {
        state.setReg(RD_INDEX, reg);
        state.setPc(initialPc);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Jalr, RD_INDEX, RD_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expectedPc, state.getPc());
        assertEquals(expectedReg, state.getReg(RD_INDEX));
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testJalrMisalignedAddress() throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, 14);
        state.setPc(24);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Jalr, RD_INDEX, RS1_INDEX, 0));
        cpu.nextOpcode();
    }

    @Test
    public void testBeqAsJ() throws MemoryException, CpuException {
        state.setPc(0);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Beq, x0, x0, 8));
        cpu.nextOpcode();
        assertEquals(8, state.getPc());
    }

    @Test
    @Parameters({
            "5, 5, 16, -8, 8",
            "5, 6, 16, -8,20",
            "6, 5, 16, -8,20"
    })
    public void testBeq(int rs1, int rs2, int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RS2_INDEX, rs2);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Beq, RS1_INDEX, RS2_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getPc());

    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testBeqMisalignedAddressTakenBranch() throws MemoryException, CpuException {
        state.setPc(24);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Beq, x0, x0, 14));
        cpu.nextOpcode();
    }

    @Test
    @Parameters({
            "5, 5, 16, -8, 20",
            "5, 6, 16, -8,  8",
            "6, 5, 16, -8,  8"
    })
    public void testBne(int rs1, int rs2, int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RS2_INDEX, rs2);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bne, RS1_INDEX, RS2_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getPc());
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testBneMisalignedAddressBranchTaken() throws MemoryException, CpuException {
        state.setPc(24);
        state.setReg(RS2_INDEX, 5);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bne, RS1_INDEX, RS2_INDEX, 14));
        cpu.nextOpcode();
    }

    @Test
    @Parameters({
            "5, 5, 16, -8, 20",
            "5, 6, 16, -8, 8",
            "6, 6, 16, -8, 20",
            "-1,1, 16, -8, 8",
            "1,-1, 16, -8, 20"
    })
    public void testBlt(int rs1, int rs2, int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RS2_INDEX, rs2);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Blt, RS1_INDEX, RS2_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getPc());
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testBltMisalignedAddress() throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, 5);
        state.setReg(RS2_INDEX, 6);
        state.setPc(24);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Blt, RS1_INDEX, RS2_INDEX, 14));
        cpu.nextOpcode();
    }

    @Test
    @Parameters({
            "5, 5, 16, -8, 20",
            "5, 6, 16, -8, 8",
            "6, 6, 16, -8, 20",
            "-1,1, 16, -8, 20",
            "1,-1, 16, -8, 8"
    })
    public void testBltu(int rs1, int rs2, int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RS2_INDEX, rs2);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bltu, RS1_INDEX, RS2_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getPc());
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testBltuMisalignedAddressBranchTaken() throws MemoryException, CpuException {
        state.setPc(24);
        state.setReg(RS1_INDEX, 5);
        state.setReg(RS2_INDEX, 6);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bltu, RS1_INDEX, RS2_INDEX, 14));
        cpu.nextOpcode();
    }

    @Test
    @Parameters({
            "5, 5, 16, -8, 8",
            "5, 6, 16, -8, 20",
            "6, 6, 16, -8, 8",
            "-1,1, 16, -8, 20",
            "1,-1, 16, -8, 8"
    })
    public void testBge(int rs1, int rs2, int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RS2_INDEX, rs2);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bge, RS1_INDEX, RS2_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getPc());
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testBgeMisalignedAddressBranchTaken() throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, 5);
        state.setPc(24);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bge, RS1_INDEX, RS2_INDEX, 14));
        cpu.nextOpcode();
    }

    @Test
    @Parameters({
            "5, 5, 16, -8, 8",
            "5, 6, 16, -8, 20",
            "6, 6, 16, -8, 8",
            "-1,1, 16, -8, 8",
            "1,-1, 16, -8, 20"
    })
    public void testBgeu(int rs1, int rs2, int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RS2_INDEX, rs2);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bgeu, RS1_INDEX, RS2_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getPc());
    }

    /*
     * As of RV32C, jumps no longer throw a misaligned memory load exception when reaching an address that
     * is not divisible by 4, and that is why this kind of tests are now disabled.
     */
    @Ignore
    @Test(expected = MisalignedMemoryLoadException.class)
    public void testBgeuMisalignedAddress() throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, 5);
        state.setPc(24);
        loadInstruction(RV32I.instructionSB(RV32I.Opcodes.Bgeu, x0, x0, 14));
        cpu.nextOpcode();
    }
}
