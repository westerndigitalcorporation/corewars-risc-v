package il.co.codeguru.corewars_riscv.gui.widgets;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

class ConsolePrintStream extends PrintStream {

    private Consumer<String> printFunction;

    public ConsolePrintStream(Consumer<String> printFunction) {
        super((OutputStream)null);
        this.printFunction = printFunction;
    }

    @Override
    public void println(String message)
    {
        printFunction.accept(message + "\n");
    }

    @Override
    public void print(String message)
    {
        printFunction.accept(message);
    }


}

public class Console  {
    public static native void log(String text)
    /*-{
        console.log(text);
    }-*/;

    public static native void debug(String text)
    /*-{
        console.log(text);
    }-*/;

    public static native void error(String text)
    /*-{
        console.error(text);
    }-*/;

    public static PrintStream stream() {
        return new ConsolePrintStream(Console::log);
    }

    public static PrintStream errorStream() {
        return new ConsolePrintStream(Console::error);
    }
    
}