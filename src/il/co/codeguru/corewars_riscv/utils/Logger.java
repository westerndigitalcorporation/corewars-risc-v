package il.co.codeguru.corewars_riscv.utils;


import il.co.codeguru.corewars_riscv.gui.widgets.Console;

/**
 * Singleton to enable testing in places with console.
 * If it is set to testing, the calls to log will be ignored, otherwise, forwarded to console class.
 */
public class Logger {
    private static Logger ourInstance = new Logger();
    private boolean testing = false;
    public static Logger getInstance() {
        return ourInstance;
    }

    private Logger() {
    }

    public static void outputToStdout()
    {
        getInstance().testing = true;
    }

    public static void log(String message)
    {
        if(getInstance().testing) System.out.println(message);
        else Console.log(message);
    }

    public static void error(String message)
    {
        if(getInstance().testing) System.err.println(message);
        else Console.error(message);
    }

    public static void setTestingMode() {
        getInstance().testing = true;
    }
}
