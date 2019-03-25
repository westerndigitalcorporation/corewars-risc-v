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
import static org.junit.Assert.assertNotEquals;

@RunWith(JUnitParamsRunner.class)
public class LoadTest {
    private static final int RS1_INDEX = 1;
    private static final int RD_INDEX = 2;

    private CpuStateRiscV state;
    private Memory memory;
    private CpuRiscV cpu;

    @Before
    public void setUp() {
        state = new CpuStateRiscV();
        memory = new RawMemory(0x10000);
        cpu = new CpuRiscV(state, memory);
        //To not interrupt the 0 memory location
        state.setPc(0x100);
        Logger.setTestingMode();

    }

    private void loadInstruction(InstructionFormatBase i) throws MemoryException {
        cpu.getMemory().storeWord(state.getPc(), i.getRaw());
    }

    @Test
    @Parameters({
            " 0, 0,  0, 17",
            "15, 0, 15, 17",
            "15,20, 35, 17",
            "15,-5, 10, 17",
            " 0, 0,  0, -1"
    })
    public void testLw(int rs1, int imm, int expectedAddress, int val) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RD_INDEX, 0);
        memory.storeWord(expectedAddress, val);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lw, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(val, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 0, 0,  0, 17",
            "15, 0, 15, 17",
            "15,20, 35, 17",
            "15,-5, 10, 17",
            " 0, 0,  0, -1"
    })
    public void testLh(int rs1, int imm, int expectedAddress, int val) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RD_INDEX, 0);
        memory.storeHalfWord(expectedAddress,(short)val);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lh, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(val, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 0, 0,  0, 17",
            "15, 0, 15, 17",
            "15,20, 35, 17",
            "15,-5, 10, 17",
    })
    public void testLhu(int rs1, int imm, int expectedAddress, int val) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RD_INDEX, 0);
        memory.storeHalfWord(expectedAddress,(short) val);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lhu, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(val, state.getReg(RD_INDEX));
    }

    @Test
    public void testLhuNegativeValue() throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, 0);
        state.setReg(RD_INDEX, 0);
        memory.storeHalfWord(0, (short) (-1));
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lhu, RD_INDEX, RS1_INDEX, 0));
        cpu.nextOpcode();
        assertNotEquals(-1, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 0, 0,  0, 17",
            "15, 0, 15, 17",
            "15,20, 35, 17",
            "15,-5, 10, 17",
            " 0, 0,  0, -1"
    })
    public void testLb(int rs1, int imm, int expectedAddress, int val) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RD_INDEX, 0);
        memory.storeByte(expectedAddress,(byte) val);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lb, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(val, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 0, 0,  0, 17",
            "15, 0, 15, 17",
            "15,20, 35, 17",
            "15,-5, 10, 17",
    })
    public void testLbu(int rs1, int imm, int expectedAddress, int val) throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, rs1);
        state.setReg(RD_INDEX, 0);
        memory.storeByte(expectedAddress,(byte) val);
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lbu, RD_INDEX, RS1_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(val, state.getReg(RD_INDEX));
    }

    @Test
    public void testLbuNegativeValue() throws MemoryException, CpuException {
        state.setReg(RS1_INDEX, 0);
        state.setReg(RD_INDEX, 0);
        memory.storeByte(0, (byte) (-1));
        loadInstruction(RV32I.instructionI(RV32I.Opcodes.Lbu, RD_INDEX, RS1_INDEX, 0));
        cpu.nextOpcode();
        assertNotEquals(-1, state.getReg(RD_INDEX));
    }
}
