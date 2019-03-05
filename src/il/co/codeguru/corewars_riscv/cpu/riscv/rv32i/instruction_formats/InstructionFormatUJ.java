package il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;

public class InstructionFormatUJ extends InstructionFormatBase {
    public InstructionFormatUJ(int raw)
    {
        super(raw);
    }

    public InstructionFormatUJ(InstructionFormatBase i)
    {
        this(i.raw);
    }

    public InstructionFormatUJ(int opcode, int rd, int imm)
    {
        this(
                (opcode & mask(7)) |
                        ((rd & mask(5)) << 7) |
                        (((imm >> 20) & 1) << 31) |
                        (((imm >> 1) & mask(10)) << 21) |
                        (((imm >> 11) & 1) << 20) |
                        (((imm >> 12) & mask(8)) << 12)
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

    public int getImmediate() {
        int bit20 = raw >> 31;
        int bit1_10 = (raw >> 21) & mask(10);
        int bit11 = (raw >> 20) & mask(1);
        int bit12_19 = (raw >> 12) & mask(8);
        return (bit1_10 << 1) | (bit11 << 11) | (bit12_19 << 12) | (bit20 << 20);
    }
}
