package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.war.Warrior;

public abstract class Feature {
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    abstract public void initWarriorGroup(Warrior... warriors);

    }
