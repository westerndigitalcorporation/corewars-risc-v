package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;


public class CInstructionFormatCJ extends CInstructionFormatBase {

    public CInstructionFormatCJ(short raw) {
        super(raw);
    }

    public CInstructionFormatCJ(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCJ(byte opcode, byte funct3, short imm) {
        this((short)((opcode & mask(2)) |
                (((imm >> 5) & mask(1)) << 2) |
                (((imm >> 1) & mask(3)) << 3) |
                (((imm >> 7) & mask(1)) << 6) |
                (((imm >> 6) & mask(1)) << 7) |
                (((imm >> 10)& mask(1))<< 8) |
                (((imm >> 8) & mask(2))<< 9) |
                (((imm >> 4) & mask(1)) << 11)|
                (((imm >> 11)& mask(1))<< 12)|
                ((funct3 & mask(3)) << 13)));
    }

    public short getImmediate() {
        int bit5 = (raw >> 2) & 1;
        int bit31= (raw >> 3) & mask(3);
        int bit7 = (raw >> 6) & 1;
        int bit6 = (raw >> 7) & 1;
        int bit10= (raw >> 8) & 1;
        int bit98= (raw >> 9) & mask(2);
        int bit4 = (raw >> 11) & 1;
        int bit11= (raw >> 12) & 1;
        int imm = ((
                 bit31 |
                (bit4 << 3) |
                (bit5 << 4) |
                (bit6 << 5) |
                (bit7 << 6) |
                (bit98 << 7) |
                (bit10 << 9) |
                (bit11 << 10)
        ) << 1);
        return (short)(((imm >> 11) & 1) == 1 ? imm | (mask(4) << 12): imm);
    }
}