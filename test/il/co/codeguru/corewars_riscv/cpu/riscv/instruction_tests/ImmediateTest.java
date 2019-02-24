package il.co.codeguru.corewars_riscv.cpu.riscv.instruction_tests;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuRiscV;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.RawMemory;
import il.co.codeguru.corewars_riscv.cpu.riscv.RV32I;
import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.InstructionFormatBase;
import il.co.codeguru.corewars_riscv.memory.MemoryException;
import il.co.codeguru.corewars_riscv.utils.Logger;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitParamsRunner.class)
public class ImmediateTest {
    private static final int RS1_INDEX = 1;
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
    @Parameters({
            " 0, 10, 10",
            " 5,  5, 10",
            " 5, -1,  4",
            " 0, -1, -1",
            "-1, -1, -2",
            "" + Integer.MAX_VALUE + ", 1, " + Integer.MIN_VALUE
    })
    public void testAddi(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Addi, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            "5, 6, 4",
            "5, 5, 5",
            "5, 0, 0",
            "5, -1, 5"
    })
    public void testAndi(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Andi, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            "1, 2, 3",
            "5, 4, 5",
            "5, 0, 5",
            "5, -1, -1"

    })
    public void testOri(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Ori, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 1,  2,  3",
            " 3, 10,  9",
            " 3, -1, -4",
            "-4, -1,  3",
            " 5,  0,  5"

    })
    public void testXori(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Xori, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 1, 2,  4",
            " 3, 2, 12",
            "-1, 2, -4",
            "" + Integer.MIN_VALUE + ", 1, 0"
    })
    public void testSlli(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Slli, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
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
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Srli, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
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
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Srai, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            "2, 1, 0",
            "1, 2, 1",
            "-1,1, 1",
            "1,-1, 0",
    })
    public void testSlti(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Slti, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            "2, 1, 0",
            "1, 2, 1",
            "-1,1, 0",
            "1,-1, 1",

    })
    public void testSltiu(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Sltiu, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }
}
