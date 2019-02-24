package com.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import il.co.codeguru.corewars_riscv.CoreWarsEngine;
import il.co.codeguru.corewars_riscv.gui.widgets.Console;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class codewars_js implements EntryPoint {

  public static native void console(String text)
  /*-{
    console.log(text);
  }-*/;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    Console.stream().println("onModuleLoad");

    GWT.setUncaughtExceptionHandler(e -> e.printStackTrace(Console.stream()));

    try {
        CoreWarsEngine.main(null);
    }
    catch(Throwable e) {
        e.printStackTrace(Console.stream());
    }


  }
}
