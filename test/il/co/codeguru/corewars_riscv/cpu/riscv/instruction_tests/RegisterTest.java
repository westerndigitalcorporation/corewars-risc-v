package il.co.codeguru.corewars_riscv.cpu.riscv.instruction_tests;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitParamsRunner.class)
public class RegisterTest {
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

    private void loadInstruction(InstructionFormatBase i) throws MemoryException {
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
    public void testAdd(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Add, RD_INDEX, RS1_INDEX, RS2_INDEX));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 10, 0, 10",
            " 10,  5, 5",
            " 4, -1,  5",
            " 1, 2, -1",
            "-1, -1, 0",
            "" + Integer.MIN_VALUE + ", 1, " + Integer.MAX_VALUE
    })
    public void testSub(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Sub, RD_INDEX, RS1_INDEX, RS2_INDEX));
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
    public void testAnd(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.And, RD_INDEX, RS1_INDEX, RS2_INDEX));
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
    public void testOr(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Or, RD_INDEX, RS1_INDEX, RS2_INDEX));
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
    public void testXor(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Xor, RD_INDEX, RS1_INDEX, RS2_INDEX));
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
    public void testSll(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Sll, RD_INDEX, RS1_INDEX, RS2_INDEX));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }


    @Test
    @Parameters({
            " 4, 2, 1",
            "12, 2, 3",
            " 1, 1, 0",
            " 0, 1, 0",
            "-1, 1, " + Integer.MAX_VALUE,
            "-2, 1, " + Integer.MAX_VALUE,
    })
    public void testSrl(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Srl, RD_INDEX, RS1_INDEX, RS2_INDEX));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }


    @Test
    @Parameters({
            " 4, 2, 1",
            "12, 2, 3",
            " 1, 1, 0",
            " 0, 1, 0",
            "-1, 1, -1",
            "-2, 1, -1"
    })
    public void testSra(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Sra, RD_INDEX, RS1_INDEX, RS2_INDEX));
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
    public void testSlt(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Slt, RD_INDEX, RS1_INDEX, RS2_INDEX));
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
    public void testSltu(int reg1, int reg2, int expected) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, reg1);
        state.setReg(RS2_INDEX, reg2);
        loadInstruction(RV32I.instructionR(RV32I.Opcodes.Sltu, RD_INDEX, RS1_INDEX, RS2_INDEX));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

}
