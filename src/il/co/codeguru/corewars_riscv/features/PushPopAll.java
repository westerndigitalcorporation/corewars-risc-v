package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.cpu.riscv.CpuRiscV;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.war.Warrior;

public class PushPopAll extends Feature {

    @Override
    public void initWarriorGroup(Warrior... warriors) {
        for (Warrior warrior : warriors) {
            if (warrior != null) {
                warrior.getCpu().registerSyscall(3, new PushAll());
                warrior.getCpu().registerSyscall(4, new PopAll());
            }
        }
    }

    // TODO: This is not really appropiate as a syscall, more as a command.
    // In the future, there is a need to make this a RISC-V command under the CG extension.

    private class PushAll extends Syscall {

        int remainingUses = 1;

        @Override
        public void call(CpuRiscV cpuRiscV) {
            if (remainingUses == 0) {
                return;
            }

            Memory memory = cpuRiscV.getMemory();
            CpuStateRiscV state = cpuRiscV.getState();
            for (int i = 3; i < 32; i++) {
                memory.storeWord(state.getReg(2), state.getReg(i));
                state.setReg(2, state.getReg(2) + 4);
            }

            remainingUses--;
        }
    }

    private class PopAll extends Syscall {
        @Override
        public void call(CpuRiscV cpuRiscV) {
            Memory memory = cpuRiscV.getMemory();
            CpuStateRiscV state = cpuRiscV.getState();
            for (int i = 3; i < 32; i++) {
                state.setReg(i, memory.loadWord(state.getReg(2)));
                state.setReg(2, state.getReg(2) + 4);
            }
        }
    }
}
