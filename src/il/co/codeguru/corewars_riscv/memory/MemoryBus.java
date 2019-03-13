package il.co.codeguru.corewars_riscv.memory;
import il.co.codeguru.corewars_riscv.utils.Logger;
import il.co.codeguru.corewars_riscv.utils.Pair;

import java.util.ArrayList;
import java.util.List;

import static il.co.codeguru.corewars_riscv.jsadd.Format.hex;

public class MemoryBus extends Memory {
    private List<Pair<MemoryRegion, Memory>> regions = new ArrayList<>();

    private Pair<MemoryRegion, Memory> findRegion(int index) {
        for (Pair<MemoryRegion, Memory> region : regions) {
            if (region.getKey().isInRegion(index)) {
                return region;
            }
        }
        //If there is only arena, return it and make it cyclic
        if(regions.size() == 1) {
            return regions.get(0);
        }
        return null;
    }

    public void addRegion(MemoryRegion region, Memory memory) {
        regions.add(new Pair<>(region, memory));
    }

    @Override
    public void storeByte(int index, byte value) throws MemoryException {
        Pair<MemoryRegion, Memory> region = findRegion(index);
        if(region == null) {
            throw new MemoryException("Write at forbidden location - at " + hex(index));
        }
        region.getValue().storeByte(region.getKey().normalize(index), value);
    }

    @Override
    public byte loadByte(int index) throws MemoryException {
        Pair<MemoryRegion, Memory> region = findRegion(index);
        if(region == null) {
            throw new MemoryException("Read at forbidden location - at " + hex(index));
        }
        return region.getValue().loadByte(region.getKey().normalize(index));
    }
}
