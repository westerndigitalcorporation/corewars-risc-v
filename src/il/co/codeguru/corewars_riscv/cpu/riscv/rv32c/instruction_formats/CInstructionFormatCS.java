package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

public class CInstructionFormatCS extends CInstructionFormatBase {

    public CInstructionFormatCS(short raw) {
        super(raw);
    }

    public CInstructionFormatCS(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCS(byte opcode, byte funct3, byte rs1, byte rs2, byte imm) {
        this((short) ((opcode & mask(2)) |
                (((rs2 - 8) & mask(3)) << 2) |
                (((imm >> 6) & 1) << 5) |
                (((imm >> 2) & 1) << 6) |
                (((rs1 - 8) & mask(3)) << 7) |
                (((imm >> 3) & mask(3)) << 10) |
                ((funct3 & mask(3)) << 13)));
    }

    /**
     * Alternative version of the custom-field constructor, which uses the alternative form of the cs-format (C1, funct3=100)
     */
    public static CInstructionFormatCS fromFunct6(byte opcode, byte funct6, byte funct2, byte rs1, byte rs2)
    {
        return new CInstructionFormatCS((short) ((opcode & mask(2)) |
                (((rs2 - 8) & mask(3)) << 2) |
                ((funct2 & mask(2)) << 5) |
                (((rs1 - 8) & mask(3)) << 7) |
                ((funct6 & mask(6)) << 10)));
    }


    public byte getRs2() {
        return (byte) (((raw >> 2) & mask(3)) + 8);
    }

    public byte getRs1() {
        return (byte) (((raw >> 7) & mask(3)) + 8);
    }

    public byte getImmediate() {
        int bit6 = (raw >> 5) & 1;
        int bit2 = (raw >> 6) & 1;
        int bit53 = (raw >> 10) & mask(3);
        return (byte) ((bit2 | (bit53 << 1) | (bit6 << 4)) << 2);
    }

    public byte getFunct6() {
        return (byte)((raw >> 10) & mask(6));
    }

    public byte getFunct2() {
        return (byte)((raw >> 5) & mask(2));
    }
}
