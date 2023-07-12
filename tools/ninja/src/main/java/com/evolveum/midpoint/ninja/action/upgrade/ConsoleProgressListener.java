/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.PrintStream;
import java.text.DecimalFormat;

import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;

public class ConsoleProgressListener implements ProgressListener {

    private static final DecimalFormat FORMAT = new DecimalFormat("###");

    private PrintStream stream;

    boolean firstUpdate = true;

    double progress = 0;

    public ConsoleProgressListener(PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public void update(long bytesRead, long contentLength, boolean done) {
        if (done) {
            stream.println(Ansi.ansi().cursorUpLine().eraseLine().fgGreen().a("Download complete").reset());
            return;
        }

        if (firstUpdate) {
            firstUpdate = false;

            String size = contentLength == -1 ? "unknown" : FileUtils.byteCountToDisplaySize(contentLength);

            stream.println(Ansi.ansi().fgBlack().a("Download size: " + size).reset());
            // this empy line will be removed with progress update
            stream.println();
        }

        double newProgress = (double) (100 * bytesRead) / contentLength;
        if (newProgress - progress > 1) {
            stream.println(Ansi.ansi().cursorUpLine().eraseLine(Ansi.Erase.ALL).a("Progress: ").fgBlue().a(FORMAT.format(newProgress)).a("%").reset());

            progress = newProgress;
        }
    }
}
