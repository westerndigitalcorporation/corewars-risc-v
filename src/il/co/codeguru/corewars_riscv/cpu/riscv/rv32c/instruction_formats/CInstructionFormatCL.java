package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

public class CInstructionFormatCL extends CInstructionFormatBase {

    public CInstructionFormatCL(short raw) {
        super(raw);
    }

    public CInstructionFormatCL(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCL(byte opcode, byte funct3, byte rd, byte rs1, byte imm) {
        this((short)((opcode & mask(2)) |
                (((rd - 8) & mask(3)) << 2) |
                (((imm >> 6) & 1) << 5) |
                (((imm >> 2) & 1) << 6) |
                (((rs1 - 8) & mask(3)) << 7) |
                (((imm >> 3) & mask(3)) << 10) |
                ((funct3 & mask(3)) << 13)));
    }

    public byte getRd() {
        return (byte) (((raw >> 2) & mask(3)) + 8);
    }

    public byte getRs1() {return (byte) (((raw >> 7) & mask(3)) + 8); }

    public byte getImmediate() {
        int bit6 = (raw >> 5) & 1;
        int bit2 = (raw >> 6) & 1;
        int bit53 = (raw >> 10) & mask(3);
        return (byte)((bit2 | (bit53 << 1) | (bit6 << 4)) << 2);
    }
}
