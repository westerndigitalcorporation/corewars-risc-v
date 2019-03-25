package il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;

public class InstructionFormatU extends InstructionFormatBase {
    public InstructionFormatU(int raw)
    {
        super(raw);
    }

    public InstructionFormatU(InstructionFormatBase i)
    {
        this(i.raw);
    }

    public InstructionFormatU(int opcode, int rd, int imm)
    {
        this(
                (opcode & mask(7)) |
                        ((rd & mask(5)) << 7) |
                        ((imm & mask(20)) << 12)
        );
    }

    @Override
    public String format(Instruction.InstructionInfo info)
    {
        return info.getName().toLowerCase() + " x" + getRd() + ", " + getImmediate();
    }

    public byte getRd()
    {
        return (byte)((this.raw >> 7) & mask(5));
    }

    public int getImmediate() { return raw >> 12;}
}
