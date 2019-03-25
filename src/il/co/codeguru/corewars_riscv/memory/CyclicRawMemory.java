package il.co.codeguru.corewars_riscv.memory;

public class CyclicRawMemory extends RawMemory {

    public CyclicRawMemory(int size) {
        super(size);
    }

    @Override
    public void storeByte(int index, byte value) throws MemoryException {
        super.storeByte(index & 0xFFFF, value);
    }

    @Override
    public byte loadByte(int index) throws MemoryException {
       return super.loadByte(index & 0xFFFF);
    }
}
