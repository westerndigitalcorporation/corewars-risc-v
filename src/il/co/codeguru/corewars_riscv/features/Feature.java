package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.memory.MemoryRegion;

import java.util.Collections;
import java.util.List;

public class Feature {
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    public List<Syscall> getSyscalls() {
        return Collections.emptyList();
    }

    public List<MemoryRegion> getMemoryRegions() {
        return Collections.emptyList();
    }

}
