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
package org.apache.ofbiz.birt.report.servlet;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.eclipse.birt.report.utility.DataUtil;
import org.eclipse.birt.report.utility.ParameterAccessor;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;

public class ViewerServletRequest extends HttpServletRequestWrapper {
    
    public final static String module = ViewerServletRequest.class.getName();

    protected String originalReportParam = null;

    public ViewerServletRequest(String originalReportParam, HttpServletRequest request) {
        super(request);
        this.originalReportParam = originalReportParam;
    }
    
    @Override
    public String getParameter(String name) {
        if (ParameterAccessor.PARAM_REPORT.equals(name)) {
            String reportParam = DataUtil.trimString(originalReportParam);
            if (reportParam.startsWith("component://")) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) {
                    loader = ViewerServletRequest.class.getClassLoader();
                }
                URL reportFileUrl = null;
                try {
                    reportFileUrl = FlexibleLocation.resolveLocation(reportParam, loader);
                } catch (MalformedURLException e) {
                    Debug.logError(e, module);
                }
                if (reportFileUrl == null) {
                    throw new IllegalArgumentException("Could not resolve location to URL: " + reportParam);
                }
                return reportFileUrl.getPath();
            } else {
                return originalReportParam;
            }
        }
        return super.getParameter(name);
    }
}
