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
package org.ofbiz.webapp.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.webapp.control.ContextFilter;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.jdbc.ConnectionFactory;

/**
 * Handles JasperReports PDF view rendering
 */
public class JasperReportsXmlViewHandler implements ViewHandler {
    
    public static final String module = JasperReportsXmlViewHandler.class.getName();

    protected ServletContext context;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null) {
            throw new ViewHandlerException("The HttpServletRequest object was null, how did that happen?");
        }
        if (page == null || page.length() == 0) {
            throw new ViewHandlerException("View page was null or empty, but must be specified");
        }
        if (info == null || info.length() == 0) {
            Debug.logWarning("View info string was null or empty, but must be used to specify an Entity that is mapped to the Entity Engine datasource that the report will use.", module);
        }

        // tell the ContextFilter we are forwarding
        request.setAttribute(ContextFilter.FORWARDED_FROM_SERVLET, new Boolean(true));

        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");

        if (delegator == null) {
            throw new ViewHandlerException("The delegator object was null, how did that happen?");
        }

        try {
            String datasourceName = delegator.getEntityHelperName(info);
            InputStream is = context.getResourceAsStream(page);
            Map parameters = UtilHttp.getParameterMap(request);

            JasperReport report = JasperCompileManager.compileReport(is);

            response.setContentType("text/xml");

            PipedOutputStream fillToPrintOutputStream = new PipedOutputStream();
            PipedInputStream fillToPrintInputStream = new PipedInputStream(fillToPrintOutputStream);

            if (datasourceName != null && datasourceName.length() > 0) {
                JasperFillManager.fillReportToStream(report, fillToPrintOutputStream, parameters, ConnectionFactory.getConnection(datasourceName));
            } else {
                JasperFillManager.fillReportToStream(report, fillToPrintOutputStream, parameters, new JREmptyDataSource());
            }
            JasperExportManager.exportReportToXmlStream(fillToPrintInputStream, response.getOutputStream());
        } catch (IOException ie) {
            throw new ViewHandlerException("IO Error in region", ie);
        } catch (java.sql.SQLException e) {
            throw new ViewHandlerException("Database error while running report", e);
        } catch (Exception e) {
            throw new ViewHandlerException("Error in report", e);
            // } catch (ServletException se) {
            // throw new ViewHandlerException("Error in region", se.getRootCause());
        }
    }
}
