package il.co.codeguru.corewars_riscv.gui.widgets;

import il.co.codeguru.corewars_riscv.war.ScoreEventListener;
import java.util.*;

/**
 * An event multicaster which broadcasts Events to a number of listeners.
 * @author BS
 */
public class EventMulticasterScore extends EventMulticasterBase<ScoreEventListener> {
    public ScoreEventListener proxy;
    public EventMulticasterScore() {
        proxy = new MulticasterHandler();
    }

    public boolean isEnabled()
    {
        return isDoneAdding;
    }
    private class MulticasterHandler implements ScoreEventListener  {
		@Override
		public void scoreChanged(String name, float addedValue, int groupIndex, int subIndex) {
		    assert isDoneAdding: "Calling observable method without finishing to add";
        	for (Object mListener : mListenersArr) {
                ((ScoreEventListener)mListener).scoreChanged(name, addedValue, groupIndex, subIndex);
			}
		}
        
    }



}