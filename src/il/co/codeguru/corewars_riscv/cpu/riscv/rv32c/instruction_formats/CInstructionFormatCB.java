package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

public class CInstructionFormatCB extends CInstructionFormatBase {

    public CInstructionFormatCB(short raw) {
        super(raw);
    }

    public CInstructionFormatCB(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCB(byte opcode, byte funct3, byte funct2, byte rs1, byte imm) {
        this((short) ((opcode & mask(2)) |
                ((imm & mask(5)) << 2) |
                (((rs1 - 8) & mask(3)) << 7) |
                ((funct2 & mask(2)) << 10) |
                (((imm >> 5) & mask(1)) << 12) |
                ((funct3 & mask(3)) << 13)));
    }

    /**
     * Alternative version of the custom-field constructor, which uses the alternative form of immediate that branch instructions use.
     */
    public static CInstructionFormatCB forBranch(byte opcode, byte funct3, byte rs1, byte imm) {
        int bit21 = (imm >> 1) & mask(2);
        int bit43 = (imm >> 3) & mask(2);
        int bit5  = (imm >> 5) & 1;
        int bit76 = (imm >> 6) & mask(2);
        int bit8  = (imm >> 8) & 1;
        return new CInstructionFormatCB((short)
                ((opcode & mask(2)) |
                (bit5 << 2) |
                (bit21 << 3) |
                (bit76 << 5) |
                (((rs1 - 8) & mask(3)) << 7) |
                (bit43 << 10) |
                (bit8 << 12) |
                ((funct3 & mask(3)) << 13))
        );
    }

    public byte getRs1() {
        return (byte) (((raw >> 7) & mask(3)) + 8);
    }

    public byte getBranchImmediate() {
        int bit5  = (raw >> 2) & 1;
        int bit21 = (raw >> 3) & mask(2);
        int bit76 = (raw >> 5) & mask(2);
        int bit43 = (raw >> 10)& mask(2);
        int bit8  = (raw >> 12)& 1;
        return (byte) ((bit21<<1) | (bit43 << 3) | (bit5 << 5) | (bit76 << 6) | (bit8 << 8));
    }

    public byte getImmediate() {
        int bit40 = (raw >> 2) & mask(5);
        int bit5  = (raw >> 12) & 1;
        return (byte)(bit40 | (bit5 << 5));
    }

    public byte getFunct2() {
        return (byte)((raw >> 10) & mask(2));
    }
}
