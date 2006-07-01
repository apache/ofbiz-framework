/*
 * $Id: JasperReportsXmlViewHandler.java 5966 2005-10-15 05:15:04Z jonesde $
 *
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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
