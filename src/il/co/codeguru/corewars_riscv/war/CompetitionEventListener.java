package il.co.codeguru.corewars_riscv.war;



/**
 * Interface for War events' listeners (implemented by the UI).
 * 
 * @author DL
 */
public interface CompetitionEventListener   {

    // called before warriors are loaded to clear the canvas
    void onWarPreStartClear();

    /**
     * Called when a new War is started, after the warriors are already loaded
     * @param warId  0-based war counter.
     */
    void onWarStart();

    /** Possible reasons for a war session to end. */
    public static final int SINGLE_WINNER = 0;
    public static final int MAX_ROUND_REACHED = 1;
    public static final int ABORTED = 2;

    /**
     * Called when a War ends.
     * @param reason   One of the above reasons.
     * @param winners  Winning warrior(s) name(s).
     */
    void onWarEnd(int reason, String winners, boolean inDebug);

    /**
     * Called when a new round is started.
     * @param round   0-based round number.
     */
    void onRound(int round);

    /**
     * Called when a new warrior enters the arena.
     * @param w  Warrior's Warrior.
     */
    void onWarriorBirth(Warrior w);

    /**
     * Called when a warrior dies.
     * @param warrior      Warrior.
     * @param reason       Reason for death.
     */
    void onWarriorDeath(Warrior warrior, String reason);

    void onCompetitionStart();

    void onCompetitionEnd();
    
    void onEndRound();

    void onPaused();

    // happens if we run more steps after the war ended and no players to step though are left alive
    void onNoneAlive();
}
