/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.util;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configurable Debug logging wrapper class
 *
 */
public final class Debug {

    private static final String NO_MODULE = "NoModule";  // set to null for previous behavior
    private static final Object[] EMPTY_PARAMS = new Object[0];

    public static final int ALWAYS = 0;
    public static final int VERBOSE = 1;
    public static final int TIMING = 2;
    public static final int INFO = 3;
    public static final int IMPORTANT = 4;
    public static final int WARNING = 5;
    public static final int ERROR = 6;
    public static final int FATAL = 7;

    private static final String[] LEVEL_PROPS = {"", "print.verbose", "print.timing", "print.info", "print.important", "print.warning",
            "print.error", "print.fatal"};
    private static final Level[] LEVEL_OBJS = {Level.OFF, Level.DEBUG, Level.TRACE, Level.INFO, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};

    private static final Map<String, Integer> LEVEL_STRING_MAP = new HashMap<>();

    private static final boolean LEVEL_ON_CACHE[] = new boolean[8]; // this field is not thread safe

    private static final Logger ROOT = LogManager.getRootLogger();

    static {
        LEVEL_STRING_MAP.put("verbose", Debug.VERBOSE);
        LEVEL_STRING_MAP.put("timing", Debug.TIMING);
        LEVEL_STRING_MAP.put("info", Debug.INFO);
        LEVEL_STRING_MAP.put("important", Debug.IMPORTANT);
        LEVEL_STRING_MAP.put("warning", Debug.WARNING);
        LEVEL_STRING_MAP.put("error", Debug.ERROR);
        LEVEL_STRING_MAP.put("fatal", Debug.FATAL);
        LEVEL_STRING_MAP.put("always", Debug.ALWAYS);

        // initialize LEVEL_ON_CACHE
        Properties properties = UtilProperties.createProperties("debug.properties");
        if (properties != null) {
            for (int i = 0; i < LEVEL_ON_CACHE.length; i++) {
                LEVEL_ON_CACHE[i] = (i == Debug.ALWAYS || "true".equalsIgnoreCase(properties.getProperty(LEVEL_PROPS[i])));
            }
        } else {
            throw new IllegalStateException("debug.properties file not found");
        }
    }

    public static Logger getLogger(String module) {
        if (UtilValidate.isNotEmpty(module)) {
            return LogManager.getLogger(module);
        }
        return ROOT;
    }

    /** Gets an Integer representing the level number from a String representing the level name; will return null if not found */
    public static Integer getLevelFromString(String levelName) {
        if (levelName == null) {
            return null;
        }
        return LEVEL_STRING_MAP.get(levelName.toLowerCase(Locale.getDefault()));
    }

    public static void log(int level, Throwable t, String msg, String module) {
        log(level, t, msg, module, "org.apache.ofbiz.base.util.Debug", EMPTY_PARAMS);
    }

    public static void log(int level, Throwable t, String msg, String module, Object... params) {
        log(level, t, msg, module, "org.apache.ofbiz.base.util.Debug", params);
    }

    public static void log(int level, Throwable t, String msg, String module, String callingClass) {
        log(level, t, msg, module, callingClass, new Object[0]);
    }

    public static void log(int level, Throwable t, String msg, String module, String callingClass, Object... params) {
        if (isOn(level)) {
            if (msg != null && params.length > 0) {
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb);
                formatter.format(msg, params);
                msg = sb.toString();
                formatter.close();
            }

            // log
            Logger logger = getLogger(module);
            logger.log(LEVEL_OBJS[level], msg, t);
        }
    }

    public static boolean isOn(int level) {
        return LEVEL_ON_CACHE[level];
    }

    // leaving these here
    public static void log(String msg) {
        log(Debug.ALWAYS, null, msg, NO_MODULE, EMPTY_PARAMS);
    }

    public static void log(String msg, Object... params) {
        log(Debug.ALWAYS, null, msg, NO_MODULE, params);
    }

    public static void log(Throwable t) {
        log(Debug.ALWAYS, t, null, NO_MODULE, EMPTY_PARAMS);
    }

    public static void log(String msg, String module) {
        log(Debug.ALWAYS, null, msg, module, EMPTY_PARAMS);
    }

    public static void log(String msg, String module, Object... params) {
        log(Debug.ALWAYS, null, msg, module, params);
    }

    public static void log(Throwable t, String module) {
        log(Debug.ALWAYS, t, null, module, EMPTY_PARAMS);
    }

    public static void log(Throwable t, String msg, String module) {
        log(Debug.ALWAYS, t, msg, module, EMPTY_PARAMS);
    }

    public static void log(Throwable t, String msg, String module, Object... params) {
        log(Debug.ALWAYS, t, msg, module, params);
    }

    public static boolean verboseOn() {
        return isOn(Debug.VERBOSE);
    }

    public static void logVerbose(String msg, String module) {
        log(Debug.VERBOSE, null, msg, module, EMPTY_PARAMS);
    }

    public static void logVerbose(String msg, String module, Object... params) {
        log(Debug.VERBOSE, null, msg, module, params);
    }

    public static void logVerbose(Throwable t, String module) {
        log(Debug.VERBOSE, t, null, module, EMPTY_PARAMS);
    }

    public static void logVerbose(Throwable t, String msg, String module) {
        log(Debug.VERBOSE, t, msg, module, EMPTY_PARAMS);
    }

    public static void logVerbose(Throwable t, String msg, String module, Object... params) {
        log(Debug.VERBOSE, t, msg, module, params);
    }

    public static boolean timingOn() {
        return isOn(Debug.TIMING);
    }

    public static void logTiming(String msg, String module) {
        log(Debug.TIMING, null, msg, module, EMPTY_PARAMS);
    }

    public static void logTiming(String msg, String module, Object... params) {
        log(Debug.TIMING, null, msg, module, params);
    }

    public static void logTiming(Throwable t, String module) {
        log(Debug.TIMING, t, null, module, EMPTY_PARAMS);
    }

    public static void logTiming(Throwable t, String msg, String module) {
        log(Debug.TIMING, t, msg, module, EMPTY_PARAMS);
    }

    public static void logTiming(Throwable t, String msg, String module, Object... params) {
        log(Debug.TIMING, t, msg, module, params);
    }

    public static boolean infoOn() {
        return isOn(Debug.INFO);
    }

    public static void logInfo(String msg, String module) {
        log(Debug.INFO, null, msg, module, EMPTY_PARAMS);
    }

    public static void logInfo(String msg, String module, Object... params) {
        log(Debug.INFO, null, msg, module, params);
    }

    public static void logInfo(Throwable t, String module) {
        log(Debug.INFO, t, null, module, EMPTY_PARAMS);
    }

    public static void logInfo(Throwable t, String msg, String module) {
        log(Debug.INFO, t, msg, module, EMPTY_PARAMS);
    }

    public static void logInfo(Throwable t, String msg, String module, Object... params) {
        log(Debug.INFO, t, msg, module, params);
    }

    public static boolean importantOn() {
        return isOn(Debug.IMPORTANT);
    }

    public static void logImportant(String msg, String module) {
        log(Debug.IMPORTANT, null, msg, module, EMPTY_PARAMS);
    }

    public static void logImportant(String msg, String module, Object... params) {
        log(Debug.IMPORTANT, null, msg, module, params);
    }

    public static void logImportant(Throwable t, String module) {
        log(Debug.IMPORTANT, t, null, module, EMPTY_PARAMS);
    }

    public static void logImportant(Throwable t, String msg, String module) {
        log(Debug.IMPORTANT, t, msg, module, EMPTY_PARAMS);
    }

    public static void logImportant(Throwable t, String msg, String module, Object... params) {
        log(Debug.IMPORTANT, t, msg, module, params);
    }

    public static boolean warningOn() {
        return isOn(Debug.WARNING);
    }

    public static void logWarning(String msg, String module) {
        log(Debug.WARNING, null, msg, module, EMPTY_PARAMS);
    }

    public static void logWarning(String msg, String module, Object... params) {
        log(Debug.WARNING, null, msg, module, params);
    }

    public static void logWarning(Throwable t, String module) {
        log(Debug.WARNING, t, null, module, EMPTY_PARAMS);
    }

    public static void logWarning(Throwable t, String msg, String module) {
        log(Debug.WARNING, t, msg, module, EMPTY_PARAMS);
    }

    public static void logWarning(Throwable t, String msg, String module, Object... params) {
        log(Debug.WARNING, t, msg, module, params);
    }

    public static boolean errorOn() {
        return isOn(Debug.ERROR);
    }

    public static void logError(String msg, String module) {
        log(Debug.ERROR, null, msg, module, EMPTY_PARAMS);
    }

    public static void logError(String msg, String module, Object... params) {
        log(Debug.ERROR, null, msg, module, params);
    }

    public static void logError(Throwable t, String module) {
        log(Debug.ERROR, t, null, module, EMPTY_PARAMS);
    }

    public static void logError(Throwable t, String msg, String module) {
        log(Debug.ERROR, t, msg, module, EMPTY_PARAMS);
    }

    public static void logError(Throwable t, String msg, String module, Object... params) {
        log(Debug.ERROR, t, msg, module, params);
    }

    public static boolean fatalOn() {
        return isOn(Debug.FATAL);
    }

    public static void logFatal(String msg, String module) {
        log(Debug.FATAL, null, msg, module, EMPTY_PARAMS);
    }

    public static void logFatal(String msg, String module, Object... params) {
        log(Debug.FATAL, null, msg, module, params);
    }

    public static void logFatal(Throwable t, String module) {
        log(Debug.FATAL, t, null, module, EMPTY_PARAMS);
    }

    public static void logFatal(Throwable t, String msg, String module) {
        log(Debug.FATAL, t, msg, module, EMPTY_PARAMS);
    }

    public static void logFatal(Throwable t, String msg, String module, Object... params) {
        log(Debug.FATAL, t, msg, module, params);
    }

    public static void set(int level, boolean on) {
        LEVEL_ON_CACHE[level] = on;
    }

    public static boolean get(int level) {
        return LEVEL_ON_CACHE[level];
    }
}
