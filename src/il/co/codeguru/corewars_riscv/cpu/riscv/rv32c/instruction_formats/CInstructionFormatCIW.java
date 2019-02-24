package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

public class CInstructionFormatCIW extends CInstructionFormatBase {

    public CInstructionFormatCIW(short raw) {
        super(raw);
    }

    public CInstructionFormatCIW(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCIW(byte opcode, byte funct3, byte rd, byte imm) {
        this((short) ((opcode & mask(2)) |
                (((rd - 8) & mask(3)) << 2) |
                ((imm & mask(8)) << 5) |
                ((funct3 & mask(3)) << 13)));
    }

    public byte getRd() {
        return (byte) (((raw >> 2) & mask(3)) + 8);
    }

    public byte getImmediate() {
        return (byte) ((raw >> 5) & mask(8));
    }
}
