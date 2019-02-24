package il.co.codeguru.corewars_riscv.gui.widgets;

import il.co.codeguru.corewars_riscv.memory.MemoryEventListener;

/**
 * An event multicaster which broadcasts Events to a number of listeners.
 * @author BS
 */
public class EventMulticasterMemory extends EventMulticasterBase<MemoryEventListener> {

    public MemoryEventListener debugProxy, competeProxy;

    public EventMulticasterMemory() {
        debugProxy = new DebugHandler();
        competeProxy = new CompeteHandler();
    }

    private class CompeteHandler implements MemoryEventListener {
        @Override
        public void onMemoryWrite(int address, byte value) {
        }

        @Override
        public void onWriteState(EWriteState state) {
        }

    }

    private class DebugHandler implements MemoryEventListener {
		@Override
		public void onMemoryWrite(int address, byte value) {
			for (Object mListener : mListenersArr) {
                ((MemoryEventListener)mListener).onMemoryWrite(address, value);
			}
		}

        @Override
        public void onWriteState(EWriteState state) {
            for (Object mListener : mListenersArr) {
                ((MemoryEventListener)mListener).onWriteState(state);
            }
        }

    }



}