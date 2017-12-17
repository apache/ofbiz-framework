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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Timer  handling utility
 * Utility class for simple reporting of the progress of a process.
 * Steps are labelled, and the time between each label (or message)
 * and the time since the start are reported in each call to timerString.
 *
 */
public class UtilTimer {

    public static final String module = UtilTimer.class.getName();
    protected static final ConcurrentHashMap<String, UtilTimer> staticTimers = new ConcurrentHashMap<>();

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

        StringBuilder retBuf = new StringBuilder();
        retBuf.append("[[").append(message).append("- total:").append(secondsSinceStart());
        if (lastMessage != null) {
            retBuf.append(",since last(").append(((lastMessage.length() > 20) ? (lastMessage.substring(0, 17) + "...") : lastMessage)).append("):").append(secondsSinceLast());
        }
        retBuf.append("]]");

        // append the timer name
        if (UtilValidate.isNotEmpty(timerName)) {
            retBuf.append(" - '").append(timerName).append("'");
        }

        lastMessage = message;
        String retString = retBuf.toString();
        if (log) {
            Debug.log(Debug.TIMING, null, retString, module, "org.apache.ofbiz.base.util.UtilTimer");
        }

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
        return (timeSinceStart()) / 1000.0;
    }

    /** Returns the number of seconds since the last time timerString was called
     * @return The number of seconds since the last time timerString was called
     */
    public double secondsSinceLast() {
        return (timeSinceLast()) / 1000.0;
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

        StringBuilder retStringBuf = new StringBuilder();

        for (int i = 0; i < level; i++) {
            retStringBuf.append("| ");
        }
        retStringBuf.append("(");

        String timeSinceStartStr = String.valueOf(timeSinceStart());
        retStringBuf.append(timeSinceStartStr + ",");

        String timeSinceLastStr = String.valueOf(timeSinceLast());
        retStringBuf.append(timeSinceLastStr);

        retStringBuf.append(")");
        int spacecount = 12 + (2 * level) - retStringBuf.length();

        for (int i = 0; i < spacecount; i++) {
            retStringBuf.append(' ');
        }
        retStringBuf.append(message);

        lastMessageTime = System.currentTimeMillis();
        String retString = retStringBuf.toString();

        if (log && Debug.timingOn()) {
            Debug.logTiming(retString, module);
        }
        return retString;
    }

    // static logging timer - be sure to close the timer when finished!

    public static UtilTimer getTimer(String timerName) {
        return getTimer(timerName, true);
    }

    public static UtilTimer getTimer(String timerName, boolean log) {
        UtilTimer timer = staticTimers.get(timerName);
        if (timer == null) {
            timer = new UtilTimer(timerName, false);
            timer.setLog(log);
            staticTimers.putIfAbsent(timerName, timer);
            timer = staticTimers.get(timerName);
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
        staticTimers.remove(timerName);
    }
}
