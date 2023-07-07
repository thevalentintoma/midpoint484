/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class Main {

    public static void main(String[] args) {
        new Main().run(args);
    }

    private PrintStream out = System.out;

    private PrintStream err = System.err;

    public PrintStream getOut() {
        return out;
    }

    public void setOut(@NotNull PrintStream out) {
        this.out = out;
    }

    public PrintStream getErr() {
        return err;
    }

    public void setErr(@NotNull PrintStream err) {
        this.err = err;
    }

    protected <T> Object run(String[] args) {
        AnsiConsole.systemInstall();

        JCommander jc = NinjaUtils.setupCommandLineParser();

        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            err.println(ex.getMessage());
            return null;
        }

        String parsedCommand = jc.getParsedCommand();

        BaseOptions base = Objects.requireNonNull(NinjaUtils.getOptions(jc.getObjects(), BaseOptions.class));

        ConsoleFormat.setBatchMode(base.isBatchMode());

        if (base.isVerbose() && base.isSilent()) {
            err.println("Cant' use " + BaseOptions.P_VERBOSE + " and " + BaseOptions.P_SILENT
                    + " together (verbose and silent)");
            printHelp(jc, parsedCommand);
            return null;
        }

        if (BooleanUtils.isTrue(base.isVersion())) {
            printVersion(base.isVerbose());
            return null;
        }

        if (base.isHelp() || parsedCommand == null) {
            printHelp(jc, parsedCommand);
            return null;
        }

        NinjaContext context = null;
        try {
            Action<T, ?> action = Command.createAction(parsedCommand);

            if (action == null) {
                err.println("Action for command '" + parsedCommand + "' not found");
                return null;
            }

            //noinspection unchecked
            T options = (T) jc.getCommands().get(parsedCommand).getObjects().get(0);

            List<Object> allOptions = new ArrayList<>(jc.getObjects());
            allOptions.add(options);

            context = new NinjaContext(out, err, allOptions, action.getApplicationContextLevel(allOptions));

            try {
                action.init(context, options);

                context.getLog().info(ConsoleFormat.formatActionStartMessage(action));

                return action.execute();
            } finally {
                action.destroy();
            }
        } catch (Exception ex) {
            handleException(base, ex);
        } finally {
            cleanupResources(base, context);

            AnsiConsole.systemUninstall();
        }

        return null;
    }

    private void cleanupResources(BaseOptions opts, NinjaContext context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception ex) {
            if (opts.isVerbose()) {
                String stack = NinjaUtils.printStackToString(ex);

                err.println("Unexpected exception occurred (" + ex.getClass()
                        + ") during destroying context. Exception stack trace:\n" + stack);
            }
        }
    }

    private void handleException(BaseOptions opts, Exception ex) {
        if (!opts.isSilent()) {
            err.println("Unexpected exception occurred (" + ex.getClass() + "), reason: " + ex.getMessage());
        }

        if (opts.isVerbose()) {
            String stack = NinjaUtils.printStackToString(ex);

            err.println("Exception stack trace:\n" + stack);
        }
    }

    private void printVersion(boolean verbose) {
        try (InputStream is = Main.class.getResource("/version").openStream()) {
            String version = IOUtils.toString(is).trim();
            out.println(version);
        } catch (Exception ex) {
            err.println("Couldn't obtains version");
            if (verbose) {
                String stack = NinjaUtils.printStackToString(ex);
                err.println("Exception stack trace:\n" + stack);
            }
        }
    }

    private void printHelp(JCommander jc, String parsedCommand) {
        if (parsedCommand != null) {
            jc.getUsageFormatter().usage(parsedCommand);
            return;
        }
        jc.usage();
    }
}
