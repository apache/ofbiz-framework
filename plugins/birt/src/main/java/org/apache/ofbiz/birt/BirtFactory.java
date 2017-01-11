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
package org.apache.ofbiz.birt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.birt.report.engine.api.IReportEngine;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.widget.model.ScreenFactory;
import org.xml.sax.SAXException;

/**
 * BIRT Factory
 * @author chatree
 *
 */
public class BirtFactory {

    public final static String module = BirtFactory.class.getName();

    protected static IReportEngine engine;
    
    /**
     * set report engine
     * @param engine
     */
    public static void setReportEngine(IReportEngine engine) {
        BirtFactory.engine = engine;
    }

    /**
     * get report engine
     * @return
     */
    public static IReportEngine getReportEngine() {
        return engine;
    }

    /**
     * get report input stream from location
     * @param resourceName
     * @return returns the input stream from location
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static InputStream getReportInputStreamFromLocation(String resourceName)
        throws IOException, SAXException, ParserConfigurationException{

        InputStream reportInputStream = null;
        synchronized (BirtFactory.class) {
            long startTime = System.currentTimeMillis();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = ScreenFactory.class.getClassLoader();
            }
            URL reportFileUrl = null;
            reportFileUrl = FlexibleLocation.resolveLocation(resourceName, loader);
            if (reportFileUrl == null) {
                throw new IllegalArgumentException("Could not resolve location to URL: " + resourceName);
            }
            reportInputStream = reportFileUrl.openStream();
            double totalSeconds = (System.currentTimeMillis() - startTime)/1000.0;
            Debug.logInfo("Got report in " + totalSeconds + "s from: " + reportFileUrl.toExternalForm(), module);
        }

        if (reportInputStream == null) {
            throw new IllegalArgumentException("Could not find report file with location [" + resourceName + "]");
        }
        return reportInputStream;
    }
}
