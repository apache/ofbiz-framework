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
package org.ofbiz.birt.report.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.context.IContext;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.presentation.aggregation.layout.FramesetFragment;
import org.eclipse.birt.report.presentation.aggregation.layout.RunFragment;
import org.eclipse.birt.report.service.BirtReportServiceFactory;
import org.eclipse.birt.report.servlet.ViewerServlet;
import org.ofbiz.birt.BirtWorker;
import org.ofbiz.birt.container.BirtContainer;
import org.ofbiz.birt.report.context.OFBizBirtContext;
import org.ofbiz.birt.report.service.OFBizBirtViewerReportService;

@SuppressWarnings("serial")
public class BirtViewerServlet extends ViewerServlet {

    public final static String module = BirtViewerServlet.class.getName();

    @Override
    protected void __init(ServletConfig config) {
        BirtReportServiceFactory.init( new OFBizBirtViewerReportService( config
                .getServletContext( ) ) );

        // handle 'frameset' pattern
        viewer = new FramesetFragment( );
        viewer.buildComposite( );
        viewer.setJSPRootPath( "/webcontent/birt" );

        // handle 'run' pattern
        run = new RunFragment( );
        run.buildComposite( );
        run.setJSPRootPath( "/webcontent/birt" );
    }

    @Override
    protected IContext __getContext( HttpServletRequest request,
            HttpServletResponse response ) throws BirtException
    {
        IReportEngine reportEngine = BirtContainer.getReportEngine();
        BirtWorker.setWebContextObjects(reportEngine, request, response);
        
        BirtReportServiceFactory.getReportService( ).setContext(
                getServletContext( ), null );
        return new OFBizBirtContext( request, response );
    }
}
