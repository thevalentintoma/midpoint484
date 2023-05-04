/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import com.beust.jcommander.Parameter;

public class BaseMiningOptions {

    public static final String P_ZIP = "-z";
    public static final String P_ZIP_LONG = "--zip";

    public static final String P_MULTI_THREAD = "-l";
    public static final String P_MULTI_THREAD_LONG = "--multi-thread";
    @Parameter(names = { P_ZIP, P_ZIP_LONG }, descriptionKey = "baseImportExport.zip")
    private boolean zip;

    @Parameter(names = { P_MULTI_THREAD, P_MULTI_THREAD_LONG }, descriptionKey = "baseImportExport.multiThread")
    private int multiThread = 1;

    public boolean isZip() {
        return zip;
    }

    public int getMultiThread() {
        return multiThread;
    }
}
