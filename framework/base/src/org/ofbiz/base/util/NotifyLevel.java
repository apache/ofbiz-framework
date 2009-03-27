/*
 * Copyright (c) 2007 Andrew Zeneski. All Rights Reserved.
 */

package org.ofbiz.base.util;

import org.apache.log4j.Level;
import org.apache.log4j.net.SyslogAppender;

/**
 * NotifyLevel
 */
public class NotifyLevel extends Level {

    public static final int NOTIFY_INT = FATAL_INT + 10000;
    public static final Level NOTIFY = new NotifyLevel(NOTIFY_INT, "NOTIFY", SyslogAppender.LOG_LOCAL0);
    public static final Level notify = new NotifyLevel(NOTIFY_INT, "notify", SyslogAppender.LOG_LOCAL0);

    public NotifyLevel(int level, String levelStr, int syslogEquivalent) {
        super(level, levelStr, syslogEquivalent);
    }
}
