package il.co.codeguru.corewars_riscv.gui;


import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;

public interface IBreakpointCheck {
    boolean shouldBreak(CpuStateRiscV state);
}