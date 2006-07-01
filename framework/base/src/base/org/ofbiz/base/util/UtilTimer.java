/*
 * $Id: UtilTimer.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.base.util;

import java.util.Map;
import java.util.HashMap;

/**
 * Timer  handling utility
 * Utility class for simple reporting of the progress of a process. 
 * Steps are labelled, and the time between each label (or message) 
 * and the time since the start are reported in each call to timerString.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class UtilTimer {
    
    public static final String module = UtilTimer.class.getName();
    protected static Map staticTimers = new HashMap();

    protected String timerName = null;
    protected String lastMessage = null;

    protected long realStartTime;
    protected long startTime;
    protected long lastMessageTime;
    protected boolean running = false;
    protected boolean log = false;
    
    public static UtilTimer makeTimer() {
        return new UtilTimer();
    }

    /** Default constructor. Starts the timer. */
    public UtilTimer() {
        this("", true);
    }

    public UtilTimer(String timerName, boolean start) {
        this(timerName, start, false);
    }

    public UtilTimer(String timerName, boolean start, boolean log) {
        this.timerName = timerName;
        this.setLog(log);
        if (start) {
            this.startTimer();
        }
    }

    public void startTimer() {
        this.lastMessageTime = realStartTime = startTime = System.currentTimeMillis();
        this.lastMessage = "Begin";
        this.running = true;
    }

    public String getName() {
        return this.timerName;
    }

    public boolean isRunning() {
        return this.running;
    }

    /** Creates a string with information including the passed message, the last passed message and the time since the last call, and the time since the beginning
     * @param message A message to put into the timer String
     * @return A String with the timing information, the timer String
     */
    public String timerString(String message) {
        return timerString(message, this.getClass().getName());
    }

    /** Creates a string with information including the passed message, the last passed message and the time since the last call, and the time since the beginning
     * @param message A message to put into the timer String
     * @param module The debug/log module/thread to use, can be null for root module
     * @return A String with the timing information, the timer String
     */
    public String timerString(String message, String module) {
        // time this call to avoid it interfering with the main timer
        long tsStart = System.currentTimeMillis();

        String retString = "[[" + message + "- total:" + secondsSinceStart();
        if (lastMessage != null) {
            retString += ",since last(" + ((lastMessage.length() > 20) ? (lastMessage.substring(0, 17) + "...") : lastMessage) + "):" +
                    secondsSinceLast() + "]]";
        } else {
            retString += "]]";
        }

        // append the timer name
        if (UtilValidate.isNotEmpty(timerName)) {
            retString = retString + " - '" + timerName + "'";
        }

        lastMessage = message;
        if (log) Debug.log(Debug.TIMING, null, retString, module, "org.ofbiz.base.util.UtilTimer");

        // have lastMessageTime come as late as possible to just time what happens between calls
        lastMessageTime = System.currentTimeMillis();
        // update startTime to disclude the time this call took
        startTime += (lastMessageTime - tsStart);

        return retString;
    }

    /** Returns the number of seconds since the timer started
     * @return The number of seconds since the timer started
     */
    public double secondsSinceStart() {
        return ((double) timeSinceStart()) / 1000.0;
    }

    /** Returns the number of seconds since the last time timerString was called
     * @return The number of seconds since the last time timerString was called
     */
    public double secondsSinceLast() {
        return ((double) timeSinceLast()) / 1000.0;
    }

    /** Returns the number of milliseconds since the timer started
     * @return The number of milliseconds since the timer started
     */
    public long timeSinceStart() {
        long currentTime = System.currentTimeMillis();

        return currentTime - startTime;
    }

    /** Returns the number of milliseconds since the last time timerString was called
     * @return The number of milliseconds since the last time timerString was called
     */
    public long timeSinceLast() {
        long currentTime = System.currentTimeMillis();

        return currentTime - lastMessageTime;
    }

    /** Sets the value of the log member, denoting whether log output is off or not
     * @param log The new value of log
     */
    public void setLog(boolean log) {
        this.log = log;
    }

    /** Gets the value of the log member, denoting whether log output is off or not
     * @return The value of log
     */
    public boolean getLog() {
        return log;
    }

    /** Creates a string with information including the passed message, the time since the last call,
     * and the time since the beginning.  This version allows an integer level to be specified to
     * improve readability of the output.
     * @param level Integer specifying how many levels to indent the timer string so the output can be more easily read through nested method calls.
     * @param message A message to put into the timer String
     * @return A String with the timing information, the timer String
     */
    public String timerString(int level, String message) {
        // String retString =  "[[" + message + ": seconds since start: " + secondsSinceStart() + ",since last(" + lastMessage + "):" + secondsSinceLast() + "]]";

        StringBuffer retStringBuf = new StringBuffer();

        for (int i = 0; i < level; i++) {
            retStringBuf.append("| ");
        }
        retStringBuf.append("(");

        String timeSinceStartStr = String.valueOf(timeSinceStart());

        // int spacecount = 5 - timeSinceStartStr.length();
        // for (int i=0; i < spacecount; i++) { retStringBuf.append(' '); }
        retStringBuf.append(timeSinceStartStr + ",");

        String timeSinceLastStr = String.valueOf(timeSinceLast());

        // spacecount = 4 - timeSinceLastStr.length();
        // for (int i=0; i < spacecount; i++) { retStringBuf.append(' '); }
        retStringBuf.append(timeSinceLastStr);

        retStringBuf.append(")");
        int spacecount = 12 + (2 * level) - retStringBuf.length();

        for (int i = 0; i < spacecount; i++) {
            retStringBuf.append(' ');
        }
        retStringBuf.append(message);

        // lastMessageTime = (new Date()).getTime();
        lastMessageTime = System.currentTimeMillis();
        // lastMessage = message;

        String retString = retStringBuf.toString();

        // if(!quiet) Debug.logInfo(retString, module);
        if (log && Debug.timingOn()) Debug.logTiming(retString, module);
        return retString;
    }

    // static logging timer - be sure to close the timer when finished!

    public static UtilTimer getTimer(String timerName) {
        return getTimer(timerName, true);
    }

    public static UtilTimer getTimer(String timerName, boolean log) {
        UtilTimer timer = (UtilTimer) staticTimers.get(timerName);
        if (timer == null) {
            synchronized(UtilTimer.class) {
                timer = (UtilTimer) staticTimers.get(timerName);
                if (timer == null) {
                    timer = new UtilTimer(timerName, false);
                    timer.setLog(log);
                    staticTimers.put(timerName, timer);
                }
            }
        }
        return timer;
    }

    public static void timerLog(String timerName, String message, String module) {
        UtilTimer timer = UtilTimer.getTimer(timerName);
        if (!timer.isRunning()) {
            timer.startTimer();
        }

        if (timer.getLog()) {
            if (module == null) {
                module = timer.getClass().getName();
            }
            timer.timerString(message, module);
        }
    }

    public static void closeTimer(String timerName) {
        UtilTimer.closeTimer(timerName, null, null);
    }

    public static void closeTimer(String timerName, String message) {
        UtilTimer.closeTimer(timerName, message, null);
    }

    public static void closeTimer(String timerName, String message, String module) {
        if (message != null) {
            UtilTimer.timerLog(timerName, message, module);
        }
        synchronized(UtilTimer.class) {
            staticTimers.remove(timerName);
        }
    }
}
