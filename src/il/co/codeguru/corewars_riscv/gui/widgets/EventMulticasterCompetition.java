package il.co.codeguru.corewars_riscv.gui.widgets;

import il.co.codeguru.corewars_riscv.war.CompetitionEventListener;
import il.co.codeguru.corewars_riscv.war.Warrior;

import java.util.*;

/**
 * An event multicaster which broadcasts Events to a number of listeners.
 * @author BS
 */
public class EventMulticasterCompetition extends EventMulticasterBase<CompetitionEventListener>
{
    public CompetitionEventListener debugProxy, competeProxy;

    public EventMulticasterCompetition() {
        debugProxy = new DebugHandler();
        competeProxy = new CompeteHandler();
    }

    private class CompeteHandler  implements CompetitionEventListener {
        public void onWarPreStartClear() {
        }
        public void onWarStart() {
        }
        @Override
        public void onWarEnd(int reason, String winners, boolean inDebug) {
            for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onWarEnd(reason, winners, inDebug);
            }
        }
        @Override
        public void onRound(int round) {
        }
        public void onWarriorBirth(Warrior w) {
        }
        public void onWarriorDeath(Warrior warrior, String reason) {
        }
        @Override
        public void onCompetitionStart() {
            for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onCompetitionStart();
            }
        }

        @Override
        public void onCompetitionEnd() {
            for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onCompetitionEnd();
            }
        }
        @Override
        public void onEndRound() {
        }

        public void onPaused() {
        }
        public void onNoneAlive() {
        }
    }


    private class DebugHandler implements CompetitionEventListener {

        @Override
        public void onWarPreStartClear() {
            for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onWarPreStartClear();
            }
        }

		@Override
		public void onWarStart() {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onWarStart();
			}
		}

		@Override
		public void onWarEnd(int reason, String winners, boolean inDebug) {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onWarEnd(reason, winners, inDebug);
			}
		}

		@Override
		public void onRound(int round) {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onRound(round);
			}
		}

		@Override
		public void onWarriorBirth(Warrior w) {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onWarriorBirth(w);
			}
		}

		@Override
		public void onWarriorDeath(Warrior warrior, String reason) {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onWarriorDeath(warrior, reason);
			}
		}

		@Override
		public void onCompetitionStart() {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onCompetitionStart();
			}
		}

		@Override
		public void onCompetitionEnd() {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onCompetitionEnd();
			}
		}

		@Override
		public void onEndRound() {
			for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onEndRound();
			}
		}

        @Override
        public void onPaused() {
            for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onPaused();
            }
        }

        @Override
        public void onNoneAlive() {
            for (Object mListener : mListenersArr) {
                ((CompetitionEventListener)mListener).onNoneAlive();
            }
        }
    }



}