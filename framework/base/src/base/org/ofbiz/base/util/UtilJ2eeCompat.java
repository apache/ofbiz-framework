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

    public static final String TOMCAT = "Apache Tomcat";
    public static final String ORION = "Orion";
    public static final String RESIN = "Resin";
    public static final String REX_IP = "TradeCity";
    public static final String OC4J = "Oracle";
    public static final String JRUN = "JRun";
    public static final String JETTY = "Jetty";
    public static final String WEBSPHERE = "Websphere";

    protected static Boolean doFlushOnRenderValue = null;
    protected static Boolean useOutputStreamNotWriterValue = null;
    protected static Boolean useNestedJspException = null;

    public static boolean doFlushOnRender(ServletContext context) {
        initCompatibilityOptions(context);
        return doFlushOnRenderValue.booleanValue();
    }

    public static boolean useOutputStreamNotWriter(ServletContext context) {
        initCompatibilityOptions(context);
        return useOutputStreamNotWriterValue.booleanValue();
    }

    public static boolean useNestedJspException(ServletContext context) {
        initCompatibilityOptions(context);
        return useNestedJspException.booleanValue();
    }

    protected static void initCompatibilityOptions(ServletContext context) {
        // this check to see if we should flush is done because on most servers this 
        // will just slow things down and not solve any problems, but on Tomcat, Orion, etc it is necessary
        if (useOutputStreamNotWriterValue == null || doFlushOnRenderValue == null) {
            boolean doflush = true;
            boolean usestream = true;
            boolean nestjspexception = true;
            // if context is null use an empty string here which will cause the defaults to be used
            String serverInfo = context == null ? "" : context.getServerInfo();

            Debug.logInfo("serverInfo: " + serverInfo, module);

            if (serverInfo.indexOf(RESIN) >= 0) {
                Debug.logImportant("Resin detected, disabling the flush on the region render from PageContext for better performance", module);
                doflush = false;
            } else if (serverInfo.indexOf(REX_IP) >= 0) {
                Debug.logImportant("Trade City RexIP detected, using response.getWriter to write text out instead of response.getOutputStream", module);
                usestream = false;
            } else if (serverInfo.indexOf(TOMCAT) >= 0) {
                Debug.logImportant("Apache Tomcat detected, using response.getWriter to write text out instead of response.getOutputStream", module);
                usestream = false;
            } else if (serverInfo.indexOf(JRUN) >= 0) {
                Debug.logImportant("JRun detected, using response.getWriter to write text out instead of response.getOutputStream", module);
                usestream = false;
            } else if (serverInfo.indexOf(JETTY) >= 0) {
                Debug.logImportant("Jetty detected, using response.getWriter to write text out instead of response.getOutputStream", module);
                usestream = false;
            } else if (serverInfo.indexOf(ORION) >= 0) {
                Debug.logImportant("Orion detected, using response.getWriter to write text out instead of response.getOutputStream", module);
                usestream = false;
                Debug.logImportant("Orion detected, using non-nested JspException", module);
                nestjspexception = false;
            } else if (serverInfo.indexOf(WEBSPHERE) >= 0) {
                Debug.logImportant("IBM Websphere Application Server detected, using response.getWriter to write text out instead of response.getOutputStream", module);
                usestream = false;
            }

            doFlushOnRenderValue = new Boolean(doflush);
            useOutputStreamNotWriterValue = new Boolean(usestream);
            useNestedJspException = new Boolean(nestjspexception);
        }
    }
}
