package il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;
import il.co.codeguru.corewars_riscv.cpu.riscv.InstructionFormat;

public class InstructionFormatBase implements InstructionFormat {
    final protected int raw;

    public InstructionFormatBase(Integer raw) {
        this.raw = raw;
    }

    protected static int mask(int len)
    {
        return (1<<len) - 1;
    }

    public byte getOpcode()
    {
        return (byte)(this.raw & mask(7));
    }

    public String format(Instruction.InstructionInfo info)
    {
        throw new UnsupportedOperationException("Trying to get formatted instruction from the base instruction format");
    }

    public int getRaw()
    {
        return this.raw;
    }
}
