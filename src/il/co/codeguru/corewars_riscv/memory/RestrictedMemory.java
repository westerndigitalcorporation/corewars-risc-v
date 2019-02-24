package il.co.codeguru.corewars_riscv.memory;

import static il.co.codeguru.corewars_riscv.jsadd.Format.hex;

public class RestrictedMemory extends Memory{
    private Memory memory;
    private MemoryRegion[] allowedRegions;
    private boolean useNewMemory;

    public RestrictedMemory(Memory raw, MemoryRegion[] allowedRegions) { this(raw,allowedRegions,true);}

    public RestrictedMemory(Memory raw, MemoryRegion[] allowedRegions, boolean useNewMemory)
    {
        this.memory = raw;
        this.allowedRegions = allowedRegions;
        this.useNewMemory = useNewMemory;
        setListener(raw.getListener());
    }

    private boolean isAddressAllowed(int index)
    {
        for(MemoryRegion region : allowedRegions)
        {
            if(region.isInRegion(index)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void storeByte(int index, byte value) throws MemoryException {
        if(useNewMemory)
        {
            if(!isAddressAllowed(index)) throw new MemoryException("Write at forbidden location - at " + hex(index));
            memory.storeByte(index, value);
        }
        else
        {
            memory.storeByte(index & 0xFFFF, value); //Loop around to the arena memory
        }
    }

    @Override
    public byte loadByte(int index) throws MemoryException {
        if(useNewMemory)
        {
            if(!isAddressAllowed(index)) throw new MemoryException("Read at forbidden location - at " + hex(index));
            return memory.loadByte(index);
        }
        else
        {
            return memory.loadByte(index & 0xFFFF); //Loop around to the arena memory
        }

    }
}
