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
package org.ofbiz.base.util;

import java.util.Formatter;
import java.util.HashMap;
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

    private static final String noModuleModule = "NoModule";  // set to null for previous behavior
    private static final Object[] emptyParams = new Object[0];

    public static final int ALWAYS = 0;
    public static final int VERBOSE = 1;
    public static final int TIMING = 2;
    public static final int INFO = 3;
    public static final int IMPORTANT = 4;
    public static final int WARNING = 5;
    public static final int ERROR = 6;
    public static final int FATAL = 7;

    private static final String[] levelProps = {"", "print.verbose", "print.timing", "print.info", "print.important", "print.warning", "print.error", "print.fatal"};
    private static final Level[] levelObjs = {Level.FATAL, Level.DEBUG, Level.TRACE, Level.INFO, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};

    private static final Map<String, Integer> levelStringMap = new HashMap<String, Integer>();

    private static final boolean levelOnCache[] = new boolean[8]; // this field is not thread safe

    private static final Logger root = LogManager.getRootLogger();

    static {
        levelStringMap.put("verbose", Debug.VERBOSE);
        levelStringMap.put("timing", Debug.TIMING);
        levelStringMap.put("info", Debug.INFO);
        levelStringMap.put("important", Debug.IMPORTANT);
        levelStringMap.put("warning", Debug.WARNING);
        levelStringMap.put("error", Debug.ERROR);
        levelStringMap.put("fatal", Debug.FATAL);
        levelStringMap.put("always", Debug.ALWAYS);

        // initialize levelOnCache
        Properties properties = UtilProperties.createProperties("debug");
        if (properties != null) {
            for (int i = 0; i < levelOnCache.length; i++) {
                levelOnCache[i] = (i == Debug.ALWAYS || "true".equalsIgnoreCase(properties.getProperty(levelProps[i])));
            }
        } else {
            throw new IllegalStateException("debug.properties file not found");
        }
    }

    public static Logger getLogger(String module) {
        if (UtilValidate.isNotEmpty(module)) {
            return LogManager.getLogger(module);
        } else {
            return root;
        }
    }

    /** Gets an Integer representing the level number from a String representing the level name; will return null if not found */
    public static Integer getLevelFromString(String levelName) {
        if (levelName == null) return null;
        return levelStringMap.get(levelName.toLowerCase());
    }

    public static void log(int level, Throwable t, String msg, String module) {
        log(level, t, msg, module, "org.ofbiz.base.util.Debug", emptyParams);
    }

    public static void log(int level, Throwable t, String msg, String module, Object... params) {
        log(level, t, msg, module, "org.ofbiz.base.util.Debug", params);
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
            }

            // log
            Logger logger = getLogger(module);
            logger.log(levelObjs[level], msg, t);
        }
    }

    public static boolean isOn(int level) {
        return levelOnCache[level];
    }

    // leaving these here
    public static void log(String msg) {
        log(Debug.ALWAYS, null, msg, noModuleModule, emptyParams);
    }

    public static void log(String msg, Object... params) {
        log(Debug.ALWAYS, null, msg, noModuleModule, params);
    }

    public static void log(Throwable t) {
        log(Debug.ALWAYS, t, null, noModuleModule, emptyParams);
    }

    public static void log(String msg, String module) {
        log(Debug.ALWAYS, null, msg, module, emptyParams);
    }

    public static void log(String msg, String module, Object... params) {
        log(Debug.ALWAYS, null, msg, module, params);
    }

    public static void log(Throwable t, String module) {
        log(Debug.ALWAYS, t, null, module, emptyParams);
    }

    public static void log(Throwable t, String msg, String module) {
        log(Debug.ALWAYS, t, msg, module, emptyParams);
    }

    public static void log(Throwable t, String msg, String module, Object... params) {
        log(Debug.ALWAYS, t, msg, module, params);
    }

    public static boolean verboseOn() {
        return isOn(Debug.VERBOSE);
    }

    public static void logVerbose(String msg, String module) {
        log(Debug.VERBOSE, null, msg, module, emptyParams);
    }

    public static void logVerbose(String msg, String module, Object... params) {
        log(Debug.VERBOSE, null, msg, module, params);
    }

    public static void logVerbose(Throwable t, String module) {
        log(Debug.VERBOSE, t, null, module, emptyParams);
    }

    public static void logVerbose(Throwable t, String msg, String module) {
        log(Debug.VERBOSE, t, msg, module, emptyParams);
    }

    public static void logVerbose(Throwable t, String msg, String module, Object... params) {
        log(Debug.VERBOSE, t, msg, module, params);
    }

    public static boolean timingOn() {
        return isOn(Debug.TIMING);
    }

    public static void logTiming(String msg, String module) {
        log(Debug.TIMING, null, msg, module, emptyParams);
    }

    public static void logTiming(String msg, String module, Object... params) {
        log(Debug.TIMING, null, msg, module, params);
    }

    public static void logTiming(Throwable t, String module) {
        log(Debug.TIMING, t, null, module, emptyParams);
    }

    public static void logTiming(Throwable t, String msg, String module) {
        log(Debug.TIMING, t, msg, module, emptyParams);
    }

    public static void logTiming(Throwable t, String msg, String module, Object... params) {
        log(Debug.TIMING, t, msg, module, params);
    }

    public static boolean infoOn() {
        return isOn(Debug.INFO);
    }

    public static void logInfo(String msg, String module) {
        log(Debug.INFO, null, msg, module, emptyParams);
    }

    public static void logInfo(String msg, String module, Object... params) {
        log(Debug.INFO, null, msg, module, params);
    }

    public static void logInfo(Throwable t, String module) {
        log(Debug.INFO, t, null, module, emptyParams);
    }

    public static void logInfo(Throwable t, String msg, String module) {
        log(Debug.INFO, t, msg, module, emptyParams);
    }

    public static void logInfo(Throwable t, String msg, String module, Object... params) {
        log(Debug.INFO, t, msg, module, params);
    }

    public static boolean importantOn() {
        return isOn(Debug.IMPORTANT);
    }

    public static void logImportant(String msg, String module) {
        log(Debug.IMPORTANT, null, msg, module, emptyParams);
    }

    public static void logImportant(String msg, String module, Object... params) {
        log(Debug.IMPORTANT, null, msg, module, params);
    }

    public static void logImportant(Throwable t, String module) {
        log(Debug.IMPORTANT, t, null, module, emptyParams);
    }

    public static void logImportant(Throwable t, String msg, String module) {
        log(Debug.IMPORTANT, t, msg, module, emptyParams);
    }

    public static void logImportant(Throwable t, String msg, String module, Object... params) {
        log(Debug.IMPORTANT, t, msg, module, params);
    }

    public static boolean warningOn() {
        return isOn(Debug.WARNING);
    }

    public static void logWarning(String msg, String module) {
        log(Debug.WARNING, null, msg, module, emptyParams);
    }

    public static void logWarning(String msg, String module, Object... params) {
        log(Debug.WARNING, null, msg, module, params);
    }

    public static void logWarning(Throwable t, String module) {
        log(Debug.WARNING, t, null, module, emptyParams);
    }

    public static void logWarning(Throwable t, String msg, String module) {
        log(Debug.WARNING, t, msg, module, emptyParams);
    }

    public static void logWarning(Throwable t, String msg, String module, Object... params) {
        log(Debug.WARNING, t, msg, module, params);
    }

    public static boolean errorOn() {
        return isOn(Debug.ERROR);
    }

    public static void logError(String msg, String module) {
        log(Debug.ERROR, null, msg, module, emptyParams);
    }

    public static void logError(String msg, String module, Object... params) {
        log(Debug.ERROR, null, msg, module, params);
    }

    public static void logError(Throwable t, String module) {
        log(Debug.ERROR, t, null, module, emptyParams);
    }

    public static void logError(Throwable t, String msg, String module) {
        log(Debug.ERROR, t, msg, module, emptyParams);
    }

    public static void logError(Throwable t, String msg, String module, Object... params) {
        log(Debug.ERROR, t, msg, module, params);
    }

    public static boolean fatalOn() {
        return isOn(Debug.FATAL);
    }

    public static void logFatal(String msg, String module) {
        log(Debug.FATAL, null, msg, module, emptyParams);
    }

    public static void logFatal(String msg, String module, Object... params) {
        log(Debug.FATAL, null, msg, module, params);
    }

    public static void logFatal(Throwable t, String module) {
        log(Debug.FATAL, t, null, module, emptyParams);
    }

    public static void logFatal(Throwable t, String msg, String module) {
        log(Debug.FATAL, t, msg, module, emptyParams);
    }

    public static void logFatal(Throwable t, String msg, String module, Object... params) {
        log(Debug.FATAL, t, msg, module, params);
    }

    public static void set(int level, boolean on) {
        levelOnCache[level] = on;
    }

    public static boolean get(int level) {
        return levelOnCache[level];
    }
}
