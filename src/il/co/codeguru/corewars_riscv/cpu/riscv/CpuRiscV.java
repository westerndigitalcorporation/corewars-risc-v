package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats.InstructionFormatBase;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.InstructionDecoderRv32c;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats.CInstructionFormatBase;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.InstructionDecoder32I;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.InstructionRunner32I;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.MemoryException;

public class CpuRiscV {

    private CpuStateRiscV state;
    private Memory Memory;
    private InstructionDecoder32I decoder;
    private InstructionDecoderRv32c cDecoder;
    private InstructionRunner32I runner;

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
        this.decoder = new InstructionDecoder32I();
        this.cDecoder = new InstructionDecoderRv32c();
        this.runner = new InstructionRunner32I(this);
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
        short rawCompressedCode = Memory.loadHalfWord(state.getPc());
        CInstructionFormatBase compressedInstruction = new CInstructionFormatBase(rawCompressedCode);
        Instruction i = cDecoder.decode(compressedInstruction);
        if(i!=null)
        {
            i.execute(runner);
            state.setPc(state.getPc() + 2);
        }
        return i!=null;
    }

}
