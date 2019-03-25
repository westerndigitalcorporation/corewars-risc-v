package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;
import il.co.codeguru.corewars_riscv.cpu.riscv.InstructionFormat;

public class CInstructionFormatBase implements InstructionFormat {
    final protected short raw;

    public CInstructionFormatBase(Integer raw) {
        this.raw = raw.shortValue();
    }
    public CInstructionFormatBase(Short raw) {this.raw = raw;}

    protected static int mask(int len)
    {
        return (1<<len) - 1;
    }

    public String format(Instruction.InstructionInfo info)
    {
        throw new UnsupportedOperationException("Trying to get formatted instruction from the base instruction format");
    }

    public short getRaw()
    {
        return this.raw;
    }

    public int getOpcode()
    {
        return (byte)(this.raw & mask(2));
    }
    public int getFunct3() {return (byte)((this.raw >> 13) & mask(3));}
}
