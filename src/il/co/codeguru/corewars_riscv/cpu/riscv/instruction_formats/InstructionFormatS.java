package il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;

public class InstructionFormatS extends InstructionFormatBase
{
    public InstructionFormatS(int raw)
    {
        super(raw);
    }

    public InstructionFormatS(InstructionFormatBase i)
    {
        this(i.raw);
    }

    public InstructionFormatS(int opcode, int funct3, int rs1, int rs2, int imm)
    {
        this(
                (opcode & mask(7)) |
                        ((imm & mask(5)) << 7) |
                        ((funct3 & mask(3)) << 12) |
                        ((rs1 & mask(5)) << 15) |
                        ((rs2 & mask(5)) << 20) |
                        ((((imm >> 5) & mask(7)) << 25))
        );

    }

    @Override
    public String format(Instruction.InstructionInfo info)
    {
        return info.getName().toLowerCase() + " x" + getRs2() + ", " + getImm() + " (x" + getRs1() + ")";
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

    public short getImm() {
        int first_part = (byte)((this.raw >> 7) & mask(5));
        int second_part = (byte)((this.raw >> 25) & mask(7));
        // Sign-extend the result
        int shift = 32 - 12;
        return (short)((first_part | (second_part << 5)) << shift >> shift);
    }
}
