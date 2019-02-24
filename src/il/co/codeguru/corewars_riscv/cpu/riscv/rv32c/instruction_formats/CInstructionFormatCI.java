package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats;

public class CInstructionFormatCI extends CInstructionFormatBase {

    public CInstructionFormatCI(short raw) {
        super(raw);
    }

    public CInstructionFormatCI(CInstructionFormatBase base) {
        this(base.getRaw());
    }

    public CInstructionFormatCI(byte opcode, byte funct3, byte rs1, byte immediate) {
        this((short)((opcode & mask(2)) |
                ((immediate & mask(5)) << 2) |
                ((rs1 & mask(5)) << 7) |
                (((immediate >> 5) & 1) << 12) |
                ((funct3 & mask(3)) << 13)));
    }

    public byte getRs1() {
        return (byte) ((raw >> 7) & mask(5));
    }

    public byte getImmediate()
    {
        byte extraImm = (byte) ((raw >> 12) & 1);
        byte base = (byte)((extraImm << 5) | ((raw >> 2) & mask(5)));
        return  (byte)(extraImm != 0
                ? base | (mask(2) << 6) // Why is this here?
                : base);
    }

    public byte getUnsignedImmediate()
    {
        return (byte)((((raw >> 12) & 1) << 5) | ((raw >> 2) & mask(5)));
    }
}
