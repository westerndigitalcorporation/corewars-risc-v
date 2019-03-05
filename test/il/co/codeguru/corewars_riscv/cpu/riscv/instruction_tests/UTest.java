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
public class UTest {
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
            " 0, 10, 40960",
            " 5,  5,  20485",
            " 5, -1, -4091",
            " 0, -1, -4096",
            "-1, -1, -1",
            "-1,  0, 4095"
    })
    public void testLui(int reg, int imm, int expected) throws MemoryException, CpuException {
        state.setReg(RD_INDEX, reg);
        loadInstruction(RV32I.instructionU(RV32I.Opcodes.Lui, RD_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }

    @Test
    @Parameters({
            " 0, 10, 40960",
            " 5,  5,  20485",
            " 5, -1, -4091",
            " 0, -1, -4096"
    })
    public void testAuipc(int pc, int imm, int expected) throws MemoryException, CpuException {
        state.setPc(pc);
        loadInstruction(RV32I.instructionU(RV32I.Opcodes.Auipc, RD_INDEX, imm));
        cpu.nextOpcode();
        assertEquals(expected, state.getReg(RD_INDEX));
    }


}
