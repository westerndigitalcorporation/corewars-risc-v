package il.co.codeguru.corewars_riscv.gui.widgets;


import java.util.*;

/**
 * An event multicaster which broadcasts Events to a number of listeners.
 * @author BS
 */
public class EventMulticasterBase<T> {
    
    protected Set<T> mListeners = new HashSet<>(); //new LinkedHashSet<>();
	protected Object[] mListenersArr = null;
    protected boolean isDoneAdding = false;


    /** Add an event listener to the list.
     */
    public void add(T pListener) {
        assert !isDoneAdding: "add after doneAdding";
 		mListeners.add(pListener);
    }

    public void doneAdding() {
        isDoneAdding = true;
        mListenersArr = mListeners.toArray();
        mListeners = null;
    }
    


}