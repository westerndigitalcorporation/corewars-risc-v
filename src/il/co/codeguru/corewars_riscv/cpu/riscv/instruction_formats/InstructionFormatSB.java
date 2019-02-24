package il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;

public class InstructionFormatSB extends InstructionFormatBase {

    public InstructionFormatSB(int raw)
    {
        super(raw);
    }

    public InstructionFormatSB(InstructionFormatBase i)
    {
        this(i.raw);
    }

    public InstructionFormatSB(int opcode, int funct3, int rs1, int rs2, int imm)
    {
        this(
                (opcode & mask(7)) |
                        (((imm >> 11) & 1) << 7) |
                        (((imm >> 1) & mask(4)) << 8) |
                        ((funct3 & mask(3)) << 12) |
                        ((rs1 & mask(5)) << 15) |
                        ((rs2 & mask(5)) << 20) |
                        (((imm >> 5) & mask(6) << 25)) |
                        (((imm >> 12) & 1 ) << 31)

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

    public byte getImm() {
        int bit11 = (this.raw >> 7) & 1;
        int first_part = (byte)((this.raw >> 8) & mask(4));
        int bit12 = (this.raw >> 31) & 1;
        int second_part = (byte)(this.raw >> 25) & mask(6);
        return (byte)((first_part << 1) | (second_part << 5) | (bit11 << 11) | (bit12 << 12));

    }

}
