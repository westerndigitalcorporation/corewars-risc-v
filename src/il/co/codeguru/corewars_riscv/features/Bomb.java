package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.cpu.riscv.CpuRiscV;
import il.co.codeguru.corewars_riscv.war.Warrior;

public class Bomb extends Feature {

    public static final int BombSyscallId = 2;
    public static final int StartIndexRegister = 9;
    public static final int BombSize = 64;
    public static final int Reg1Data = 10;
    public static final int Reg2Data = 11;

    @Override
    public void initWarriorGroup(Warrior... warriors) {
        for(Warrior warrior : warriors) {
            if(warrior != null) {
                warrior.getCpu().registerSyscall(BombSyscallId, new BombSyscall());
            }
        }
    }

    private class BombSyscall extends Syscall {

        int bombsLeft = 2;

        @Override
        public void call(CpuRiscV cpuRiscV) {
            if(bombsLeft == 0) {
                return;
            }
            for(int i=0;i<BombSize; i+=8) {
                int offset = cpuRiscV.getState().getReg(StartIndexRegister) + i;
                cpuRiscV.getMemory().storeWord(offset, cpuRiscV.getState().getReg(Reg1Data));
                cpuRiscV.getMemory().storeWord(offset + 4, cpuRiscV.getState().getReg(Reg2Data));
            }
            bombsLeft--;
        }
    }
}


