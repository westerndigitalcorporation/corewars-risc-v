package  il.co.codeguru.corewars_riscv.jsadd;

public class Random
{

    static final String BadBound = "bound must be positive";
    private boolean haveNextNextGaussian = false;
    private long seed;

    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;

    synchronized public void setSeed(long seed) {
        this.seed = initialScramble(seed);
        haveNextNextGaussian = false;
    }

    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }

    protected int next(int bits) {
        long oldseed, nextseed;
        long seed = this.seed;
        //do {
            oldseed = seed;
            nextseed = (oldseed * multiplier + addend) & mask;
        //} while (!seed.compareAndSet(oldseed, nextseed));
        this.seed = seed;
        return (int)(nextseed >>> (48 - bits));
    }

    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);

        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = (int)((bound * (long)r) >> 31);
        else {
            for (int u = r;
                 u - (r = u % bound) + m < 0;
                 u = next(31))
                ;
        }
        return r;
    }

}