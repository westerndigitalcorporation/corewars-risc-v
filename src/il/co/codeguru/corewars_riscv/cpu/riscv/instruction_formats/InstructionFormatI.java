package il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;

public class InstructionFormatI extends InstructionFormatBase {
    public InstructionFormatI(int raw)
    {
        super(raw);
    }

    public InstructionFormatI(InstructionFormatBase i)
    {
        this(i.raw);
    }

    public InstructionFormatI(int opcode, int rd, int funct3, int rs1, int imm)
    {
        this(
       (opcode & mask(7)) |
            ((rd & mask(5)) << 7) |
            ((funct3 & mask(3)) << 12) |
            ((rs1 & mask(5)) << 15) |
            ((imm & mask(12)) << 20)
        );
    }

    @Override
    public String format(Instruction.InstructionInfo info)
    {
        return info.getName().toLowerCase() + " x" + getRd() + ", x" + getRs1() + ", " + getImmediate();
    }

    public byte getRd()
    {
        return (byte)((this.raw >> 7) & mask(5));
    }

    public byte getFunct3()
    {
        return (byte)((this.raw >> 12) & mask(3));
    }

    public byte getRs1()
    {
        return (byte)((this.raw >> 15) & mask(5));
    }

    public short getImmediate()
    {
        return (short)(this.raw >> 20);
    }
}
