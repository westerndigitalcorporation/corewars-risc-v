package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.MemoryBus;
import il.co.codeguru.corewars_riscv.memory.MemoryRegion;
import il.co.codeguru.corewars_riscv.memory.RawMemory;
import il.co.codeguru.corewars_riscv.war.Warrior;

import java.util.HashMap;
import java.util.Map;

public class NewMemory extends Feature {

    public static final MemoryRegion SharedMemory = new MemoryRegion(0x40000, 0x40399);
    public static final MemoryRegion StackMemory = new MemoryRegion(0x20000, 0x20799);

    @Override
    public void initWarriorGroup(Warrior... warriors) {
        Map<Integer, Memory> teamMap = new HashMap<>();
        for (Warrior warrior : warriors) {
            if (warrior != null) {
                if (!teamMap.containsKey(warrior.getTeamId())) {
                    teamMap.put(warrior.getTeamId(), new RawMemory(SharedMemory.getSize()));
                }
                MemoryBus bus = warrior.getBus();
                bus.addRegion(SharedMemory, teamMap.get(warrior.getTeamId()));
                bus.addRegion(StackMemory, new RawMemory(StackMemory.getSize()));

                warrior.getCpuState().setReg(2, StackMemory.m_start);
                warrior.getCpuState().setReg(3, SharedMemory.m_start);
            }
        }
    }
}
