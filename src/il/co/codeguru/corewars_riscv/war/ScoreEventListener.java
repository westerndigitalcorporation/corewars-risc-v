package il.co.codeguru.corewars_riscv.war;

import java.util.EventListener;

/**
 * @author BS
 */
public interface ScoreEventListener  {
    void scoreChanged(String name, float addedValue, int groupIndex, int subIndex);
}
