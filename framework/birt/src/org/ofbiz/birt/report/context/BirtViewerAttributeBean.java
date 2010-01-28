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
package org.ofbiz.birt.report.context;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.birt.report.context.ViewerAttributeBean;
import org.eclipse.birt.report.exception.ViewerException;
import org.eclipse.birt.report.resource.ResourceConstants;
import org.eclipse.birt.report.utility.DataUtil;
import org.eclipse.birt.report.utility.ParameterAccessor;
import org.ofbiz.base.location.FlexibleLocation;

public class BirtViewerAttributeBean extends ViewerAttributeBean {

    public final static String module = BirtViewerAttributeBean.class.getName();

    public BirtViewerAttributeBean(HttpServletRequest request) {
        super(request);
    }

    protected void __init( HttpServletRequest request ) throws Exception
    {
        String reportParam = DataUtil.trimString( ParameterAccessor.getParameter( request, ParameterAccessor.PARAM_REPORT ));
        if (reportParam.startsWith("component://")) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = BirtViewerAttributeBean.class.getClassLoader();
            }
            URL reportFileUrl = null;
            reportFileUrl = FlexibleLocation.resolveLocation(reportParam, loader);
            if (reportFileUrl == null) {
                throw new IllegalArgumentException("Could not resolve location to URL: " + reportParam);
            }
            this.reportDesignName = reportFileUrl.getPath();
        } else {
            this.reportDesignName = ParameterAccessor.getReport( request, null );
        }

        this.reportDesignHandle = getDesignHandle( request );
        if ( this.reportDesignHandle == null )
            throw new ViewerException(
                    ResourceConstants.GENERAL_EXCEPTION_NO_REPORT_DESIGN );

        // Initialize report parameters.
        __initParameters( request );
    }
}
