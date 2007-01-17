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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.util.exception.ExceptionHelper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggerRepository;

import org.ofbiz.base.util.collections.FlexibleProperties;

/**
 * Configurable Debug logging wrapper class
 *
 */
public final class Debug {

    public static final boolean useLog4J = true;
    public static final String noModuleModule = "NoModule";  // set to null for previous behavior
    
    static DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    public static final String SYS_DEBUG = System.getProperty("DEBUG");
    public static final int ALWAYS = 0;
    public static final int VERBOSE = 1;
    public static final int TIMING = 2;
    public static final int INFO = 3;
    public static final int IMPORTANT = 4;
    public static final int WARNING = 5;
    public static final int ERROR = 6;
    public static final int FATAL = 7;

    public static final String[] levels = {"Always", "Verbose", "Timing", "Info", "Important", "Warning", "Error", "Fatal"};
    public static final String[] levelProps = {"", "print.verbose", "print.timing", "print.info", "print.important", "print.warning", "print.error", "print.fatal"};
    public static final Level[] levelObjs = {Level.INFO, Level.DEBUG, Level.DEBUG, Level.INFO, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};

    protected static Map levelStringMap = new HashMap();
    
    protected static PrintStream printStream = System.out;
    protected static PrintWriter printWriter = new PrintWriter(printStream);

    protected static boolean levelOnCache[] = new boolean[8];
    protected static boolean packException = true;
    protected static final boolean useLevelOnCache = true;
    
    protected static Logger root = Logger.getRootLogger();

    static {
        levelStringMap.put("verbose", new Integer(Debug.VERBOSE));
        levelStringMap.put("timing", new Integer(Debug.TIMING));
        levelStringMap.put("info", new Integer(Debug.INFO));
        levelStringMap.put("important", new Integer(Debug.IMPORTANT));
        levelStringMap.put("warning", new Integer(Debug.WARNING));
        levelStringMap.put("error", new Integer(Debug.ERROR));
        levelStringMap.put("fatal", new Integer(Debug.FATAL));
        levelStringMap.put("always", new Integer(Debug.ALWAYS));
        
        // initialize Log4J
        PropertyConfigurator.configure(FlexibleProperties.makeFlexibleProperties(UtilURL.fromResource("debug.properties")));

        // initialize levelOnCache
        for (int i = 0; i < 8; i++) {
            levelOnCache[i] = (i == Debug.ALWAYS || UtilProperties.propertyValueEqualsIgnoreCase("debug.properties", levelProps[i], "true"));
        }
        
        if (SYS_DEBUG != null) {
            for (int x = 0; x < 8; x++) {
                levelOnCache[x] = true;
            }
            LoggerRepository repo = root.getLoggerRepository();
            Enumeration en = repo.getCurrentLoggers();
            while (en.hasMoreElements()) {
                Logger thisLogger = (Logger) en.nextElement();
                thisLogger.setLevel(Level.DEBUG);
            }            
        }

        // configure exception packing
        packException = UtilProperties.propertyValueEqualsIgnoreCase("debug.properties", "pack.exception", "true");
    }
    
    public static PrintStream getPrintStream() {
        return printStream;
    }

    public static void setPrintStream(PrintStream printStream) {
        Debug.printStream = printStream;
        Debug.printWriter = new PrintWriter(printStream);
    }

    public static PrintWriter getPrintWriter() {
        return printWriter;
    }

    public static Logger getLogger(String module) {
        if (module != null && module.length() > 0) {
            return Logger.getLogger(module);
        } else {
            return root;
        }
    }

    /** Gets an Integer representing the level number from a String representing the level name; will return null if not found */
    public static Integer getLevelFromString(String levelName) {
        if (levelName == null) return null;
        return (Integer) levelStringMap.get(levelName.toLowerCase());
    }
    
    /** Gets an int representing the level number from a String representing the level name; if level not found defaults to Debug.INFO */
    public static int getLevelFromStringWithDefault(String levelName) {
        Integer levelInt = getLevelFromString(levelName);
        if (levelInt == null) {
            return Debug.INFO;
        } else {
            return levelInt.intValue();
        }
    }
    
    public static void log(int level, Throwable t, String msg, String module) {
        log(level, t, msg, module, "org.ofbiz.base.util.Debug");
    }

    public static void log(int level, Throwable t, String msg, String module, String callingClass) {
        if (isOn(level)) {
            // pack the exception
            if (packException && t != null) {
                msg = System.getProperty("line.separator") + ExceptionHelper.packException(msg, t, true);
                t = null;
            }

            // log
            if (useLog4J) {
                Logger logger = getLogger(module);
                if (SYS_DEBUG != null) {
                    logger.setLevel(Level.DEBUG);
                }
                logger.log(callingClass, levelObjs[level], msg, t);
            } else {
                StringBuffer prefixBuf = new StringBuffer();

                prefixBuf.append(dateFormat.format(new java.util.Date()));
                prefixBuf.append(" [OFBiz");
                if (module != null) {
                    prefixBuf.append(":");
                    prefixBuf.append(module);
                }
                prefixBuf.append(":");
                prefixBuf.append(levels[level]);
                prefixBuf.append("] ");
                if (msg != null) {
                    getPrintWriter().print(prefixBuf.toString());
                    getPrintWriter().println(msg);
                }
                if (t != null) {
                    getPrintWriter().print(prefixBuf.toString());
                    getPrintWriter().println("Received throwable:");
                    t.printStackTrace(getPrintWriter());
                }
            }
        }
    }

    public static boolean isOn(int level) {
        if (useLevelOnCache) {
            return levelOnCache[level];
        } else {
            return (level == Debug.ALWAYS || UtilProperties.propertyValueEqualsIgnoreCase("debug", levelProps[level], "true"));
        }
    }
    
    // leaving these here
    public static void log(String msg) {
        log(Debug.ALWAYS, null, msg, noModuleModule);
    }    
    public static void log(Throwable t) {
        log(Debug.ALWAYS, t, null, noModuleModule);
    }

    public static void log(String msg, String module) {
        log(Debug.ALWAYS, null, msg, module);
    }

    
    public static void log(Throwable t, String module) {
        log(Debug.ALWAYS, t, null, module);
    }

    public static void log(Throwable t, String msg, String module) {
        log(Debug.ALWAYS, t, msg, module);
    }

    public static boolean verboseOn() {
        return isOn(Debug.VERBOSE);
    }
    
    public static void logVerbose(String msg, String module) {
        log(Debug.VERBOSE, null, msg, module);
    }
   
    public static void logVerbose(Throwable t, String module) {
        log(Debug.VERBOSE, t, null, module);
    }

    public static void logVerbose(Throwable t, String msg, String module) {
        log(Debug.VERBOSE, t, msg, module);
    }

    public static boolean timingOn() {
        return isOn(Debug.TIMING);
    }
   
    public static void logTiming(String msg, String module) {
        log(Debug.TIMING, null, msg, module);
    }

    public static void logTiming(Throwable t, String module) {
        log(Debug.TIMING, t, null, module);
    }

    public static void logTiming(Throwable t, String msg, String module) {
        log(Debug.TIMING, t, msg, module);
    }

    public static boolean infoOn() {
        return isOn(Debug.INFO);
    }
    
    public static void logInfo(String msg, String module) {
        log(Debug.INFO, null, msg, module);
    }

    public static void logInfo(Throwable t, String module) {
        log(Debug.INFO, t, null, module);
    }

    public static void logInfo(Throwable t, String msg, String module) {
        log(Debug.INFO, t, msg, module);
    }

    public static boolean importantOn() {
        return isOn(Debug.IMPORTANT);
    }

    public static void logImportant(String msg, String module) {
        log(Debug.IMPORTANT, null, msg, module);
    }

    public static void logImportant(Throwable t, String module) {
        log(Debug.IMPORTANT, t, null, module);
    }

    public static void logImportant(Throwable t, String msg, String module) {
        log(Debug.IMPORTANT, t, msg, module);
    }

    public static boolean warningOn() {
        return isOn(Debug.WARNING);
    }

    public static void logWarning(String msg, String module) {
        log(Debug.WARNING, null, msg, module);
    }

    public static void logWarning(Throwable t, String module) {
        log(Debug.WARNING, t, null, module);
    }

    public static void logWarning(Throwable t, String msg, String module) {
        log(Debug.WARNING, t, msg, module);
    }

    public static boolean errorOn() {
        return isOn(Debug.ERROR);
    }

    public static void logError(String msg, String module) {
        log(Debug.ERROR, null, msg, module);
    }

    public static void logError(Throwable t, String module) {
        log(Debug.ERROR, t, null, module);
    }

    public static void logError(Throwable t, String msg, String module) {
        log(Debug.ERROR, t, msg, module);
    }

    public static boolean fatalOn() {
        return isOn(Debug.FATAL);
    }

    public static void logFatal(String msg, String module) {
        log(Debug.FATAL, null, msg, module);
    }

    public static void logFatal(Throwable t, String module) {
        log(Debug.FATAL, t, null, module);
    }

    public static void logFatal(Throwable t, String msg, String module) {
        log(Debug.FATAL, t, msg, module);
    }

    public static void set(int level, boolean on) {
        if (!useLevelOnCache)
            return;
        levelOnCache[level] = on;
    }

    public static synchronized Appender getNewFileAppender(String name, String logFile, long maxSize, int backupIdx, String pattern) {
        if (pattern == null) {
            pattern = "%-5r[%24F:%-3L:%-5p]%x %m%n";
        }

        PatternLayout layout = new PatternLayout(pattern);
        layout.activateOptions();

        RollingFileAppender newAppender = null;
        try {
            newAppender = new RollingFileAppender(layout, logFile, true);
        } catch (IOException e) {
            logFatal(e, Debug.class.getName());
        }

        if (newAppender != null) {
            if (backupIdx > 0) {
                newAppender.setMaxBackupIndex(backupIdx);
            }
            if (maxSize > 0) {
                newAppender.setMaximumFileSize(maxSize);
            }
            newAppender.setThreshold(Priority.DEBUG);
            newAppender.activateOptions();
            newAppender.setName(name);            
        }

        return newAppender;
    }

    public static boolean registerFileAppender(String module, String name, String logFile, long maxSize, int backupIdx, String pattern) {
        Logger logger = Logger.getLogger(module);
        boolean found = false;

        Appender foundAppender = logger.getAppender(name);
        if (foundAppender == null) {
            Enumeration currentLoggerEnum = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
            while (currentLoggerEnum.hasMoreElements() && foundAppender == null) {
                Logger log = (Logger) currentLoggerEnum.nextElement();
                foundAppender = log.getAppender(name);
            }
        } else {
            return true;
        }

        if (foundAppender == null) {
            if (logFile != null) {
                foundAppender = getNewFileAppender(name, logFile, maxSize, backupIdx, pattern);
                if (foundAppender != null) {
                    found = true;
                }
            }
        } else {
            found = true;
        }

        logger.addAppender(foundAppender);
        return found;
    }

    public static boolean registerFileAppender(String module, String name, String logFile) {
        return registerFileAppender(module, name, logFile, 0, 10, null);
    }

    public static boolean registerFileAppender(String module, String name) {
        return registerFileAppender(module, name, null, -1, -1, null);
    }
}
