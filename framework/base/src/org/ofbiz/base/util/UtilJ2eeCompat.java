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

import javax.servlet.ServletContext;

/**
 * Misc J2EE Compatibility Utility Functions
 *
 */
public class UtilJ2eeCompat {

    public static final String module = UtilJ2eeCompat.class.getName();

    private static final String TOMCAT = "apache tomcat";
    private static final String ORION = "orion";
    private static final String REX_IP = "tradecity";
    private static final String JRUN = "jrun";
    private static final String JETTY = "jetty";
    private static final String WEBSPHERE = "websphere";

    private volatile static UtilJ2eeCompat instance;

    private final boolean useOutputStreamNotWriter;

    private UtilJ2eeCompat(ServletContext context) {
        boolean usestream = true;
        // if context is null use an empty string here which will cause the defaults to be used
        String serverInfo = context == null ? "" : context.getServerInfo().toLowerCase();

        Debug.logInfo("serverInfo: " + serverInfo, module);

        if (serverInfo.indexOf(TOMCAT) >= 0) {
            Debug.logInfo("Apache Tomcat detected, using response.getWriter to write text out instead of response.getOutputStream", module);
            usestream = false;
        } else if (serverInfo.indexOf(REX_IP) >= 0) {
            Debug.logInfo("Trade City RexIP detected, using response.getWriter to write text out instead of response.getOutputStream", module);
            usestream = false;
        } else if (serverInfo.indexOf(JRUN) >= 0) {
            Debug.logInfo("JRun detected, using response.getWriter to write text out instead of response.getOutputStream", module);
            usestream = false;
        } else if (serverInfo.indexOf(JETTY) >= 0) {
            Debug.logInfo("Jetty detected, using response.getWriter to write text out instead of response.getOutputStream", module);
            usestream = false;
        } else if (serverInfo.indexOf(ORION) >= 0) {
            Debug.logInfo("Orion detected, using response.getWriter to write text out instead of response.getOutputStream", module);
            usestream = false;
        } else if (serverInfo.indexOf(WEBSPHERE) >= 0) {
            Debug.logInfo("IBM Websphere Application Server detected, using response.getWriter to write text out instead of response.getOutputStream", module);
            usestream = false;
        }

        useOutputStreamNotWriter = usestream;
    }

    private static UtilJ2eeCompat getInstance(ServletContext context) {
        if (instance == null) {
            synchronized (UtilJ2eeCompat.class) {
                if (instance == null) {
                    instance = new UtilJ2eeCompat(context);
                }
            }
        }
        return instance;
    }

    public static boolean useOutputStreamNotWriter(ServletContext context) {
        return getInstance(context).useOutputStreamNotWriter;
    }

}
