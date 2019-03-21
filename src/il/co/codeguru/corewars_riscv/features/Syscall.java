package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.cpu.riscv.CpuRiscV;

public abstract class Syscall {
    public abstract void call(CpuRiscV cpuRiscV);
}
