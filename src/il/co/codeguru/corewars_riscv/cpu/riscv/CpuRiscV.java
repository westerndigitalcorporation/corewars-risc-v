package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.InstructionFormatBase;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.InstructionDecoderRv32c;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats.CInstructionFormatBase;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.MemoryException;

public class CpuRiscV {

    private CpuStateRiscV state;
    private Memory Memory;
    private InstructionDecoder decoder;
    private InstructionDecoderRv32c cDecoder;
    private InstructionRunner runner;

    public CpuStateRiscV getState() {
        return state;
    }

    public Memory getMemory() {
        return Memory;
    }

    public CpuRiscV(CpuStateRiscV state, Memory Memory)
    {
        this.state = state;
        this.Memory = Memory;
        this.decoder = new InstructionDecoder();
        this.cDecoder = new InstructionDecoderRv32c();
        this.runner = new InstructionRunner(this);
    }

    public void nextOpcode() throws CpuException, MemoryException
    {
        if(tryRv32cSet())
            return;

        int rawCode = Memory.loadWord(state.getPc());
        InstructionFormatBase instructionRaw = new InstructionFormatBase(rawCode);

        Instruction instruction = decoder.decode(instructionRaw);

        instruction.execute(runner);

        state.setPc(state.getPc() + 4);
    }

    private boolean tryRv32cSet() throws CpuException, MemoryException
    {
        short rawComppressedCode = Memory.loadHalfWord(state.getPc());
        CInstructionFormatBase commpressedInstruction = new CInstructionFormatBase(rawComppressedCode);
        Instruction i = cDecoder.decode(commpressedInstruction);
        if(i!=null)
        {
            i.execute(runner);
            state.setPc(state.getPc() + 2);
        }
        return i!=null;
    }

}
