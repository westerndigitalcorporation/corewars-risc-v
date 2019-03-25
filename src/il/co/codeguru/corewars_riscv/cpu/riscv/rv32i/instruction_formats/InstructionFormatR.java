package il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;

public class InstructionFormatR extends InstructionFormatBase
{
    public InstructionFormatR(int raw)
    {
        super(raw);
    }

    public InstructionFormatR(InstructionFormatBase i)
    {
        this(i.raw);
    }

    public InstructionFormatR(int opcode, int rd, int funct3, int rs1, int rs2, int funct7)
    {
        this(
                (opcode & mask(7)) |
                        ((rd & mask(5)) << 7) |
                        ((funct3 & mask(3)) << 12) |
                        ((rs1 & mask(5)) << 15) |
                        ((rs2 & mask(5)) << 20) |
                        ((funct7 & mask(7)) << 25)
        );
    }

    @Override
    public String format(Instruction.InstructionInfo info)
    {
        return info.getName().toLowerCase() + " x" + getRd() + ", x" + getRs1() + ", x" + getRs2();
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

    public byte getRs2()
    {
        return (byte)((this.raw >> 20) & mask(5));
    }

    public byte getFunct7() { return (byte)(this.raw >> 25);}
}
