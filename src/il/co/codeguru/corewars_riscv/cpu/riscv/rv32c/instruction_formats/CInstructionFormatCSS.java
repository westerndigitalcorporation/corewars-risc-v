package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

public class CInstructionFormatCSS extends CInstructionFormatBase {

    public CInstructionFormatCSS(short raw) {
        super(raw);
    }

    public CInstructionFormatCSS(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCSS(byte opcode, byte funct3, byte rs2, byte imm) {
        this((short)((opcode & mask(2)) |
                ((rs2 & mask(5)) << 2) |
                ((imm & mask(6)) << 7) |
                ((funct3 & mask(3)) << 13)));
    }

    public static CInstructionFormatCSS createWithWord(byte opcode, byte funct3, byte rs2, byte imm)
    {
        int bit76 = (imm >> 6) & mask(2);
        int bit52 = (imm >> 2) & mask(4);
        byte word = (byte)(bit76 | (bit52 << 2));
        return new CInstructionFormatCSS(opcode, funct3, rs2, word);
    }

    public byte getRs2() {
        return (byte) ((raw >> 2) & mask(5));
    }

    public byte getImmediate() {
        return (byte) ((raw >> 7) & mask(6));
    }

    public short getWord()
    {
        int bit76 = this.getImmediate() & mask(2);
        int bit52 = (this.getImmediate() >> 2) & mask(4);
        return (short)(((bit52 | (bit76 << 4)) << 2) & mask(8));
    }
}
