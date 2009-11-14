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
package org.ofbiz.webapp.event ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastMap;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/**
 * WfsEventHandler - WFS Event Handler implementation
 */
public class WfsEventHandler implements EventHandler {

    public static final String module = WfsEventHandler.class.getName();

    public static final String InputTemplateUrl ="component://webapp/script/org/ofbiz/webapp/event/processWfs.ftl";

    /**
     * @see org.ofbiz.webapp.event.EventHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws EventHandlerException {
    }

    /** Invoke the web event
     *@param eventPath The path or location of this event
     *@param eventMethod The method to invoke
     *@param request The servlet request object
     *@param response The servlet response object
     *@return String Result code
     *@throws EventHandlerException
     */
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        //LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String typeName = null;
        Element queryElem = null;

        try {
            typeName = (String)request.getParameter("typename");
            //determine if "get" or "post" and get "filter" param accordingly
            if (UtilValidate.isNotEmpty(typeName)) {
                String queryFieldCoded = (String)request.getParameter("filter");
                String queryFieldDecoded = UtilFormatOut.decodeQueryValue(queryFieldCoded);
                Document doc = UtilXml.readXmlDocument(queryFieldDecoded);
                queryElem = doc.getDocumentElement();
            } else {
                Document doc = UtilXml.readXmlDocument(request.getInputStream(), "WFS Request");
                Element getFeatureElem = doc.getDocumentElement();
                queryElem = UtilXml.firstChildElement(getFeatureElem, "Query");
                typeName = queryElem.getAttribute("typeName");
            }
            // Take "ogc:filter" element and transform it to a Simple Method query script
            String inputTmplUrl = UtilProperties.getPropertyValue("wfs", "input.template.path", WfsEventHandler.InputTemplateUrl);
            String xmlScript = processWfsEntity(typeName, queryElem, inputTmplUrl);

            // run simple method script to get a list of entities
            Document simpleDoc = UtilXml.readXmlDocument(xmlScript);
            Element simpleElem = simpleDoc.getDocumentElement();
            SimpleMethod meth = new SimpleMethod(simpleElem, null, null);
            MethodContext methodContext = new MethodContext(request, response, null);
            String retStr = meth.exec(methodContext); //Need to check return string
            List<GenericValue> entityList = UtilGenerics.cast(request.getAttribute("entityList"));
            request.setAttribute("entityList", entityList);

        } catch (TemplateException ioe) {
            sendError(response, "Problem handling event");
            throw new EventHandlerException("Problem processing template", ioe);
        } catch (FileNotFoundException ioe) {
            sendError(response, "Problem handling event");
            throw new EventHandlerException("Cannot find file", ioe);
        } catch (URISyntaxException ioe) {
            sendError(response, "Problem handling event");
            throw new EventHandlerException("Cannot read the input stream", ioe);
        } catch (SAXException ioe) {
            sendError(response, "Problem handling event");
            throw new EventHandlerException("Cannot read the input stream", ioe);
        } catch (ParserConfigurationException ioe) {
            sendError(response, "Problem handling event");
            throw new EventHandlerException("Cannot read the input stream", ioe);
        } catch (IOException ioe) {
            sendError(response, "Problem handling event");
            throw new EventHandlerException("Cannot read the input stream", ioe);
        }


        return "success";
    }

    private void sendError(HttpServletResponse res, Object obj) throws EventHandlerException {
//        Message msg = new Message(obj);

        try {
//            res.setContentType(msg.getContentType(Constants.DEFAULT_WFS_VERSION));
//            res.setContentLength(Integer.parseInt(Long.toString(msg.getContentLength())));
//            msg.writeTo(res.getOutputStream());
//            res.flushBuffer();
        } catch (Exception e) {
            throw new EventHandlerException(e.getMessage(), e);
        }
    }

    private String getLocationURI(HttpServletRequest request) {
        StringBuilder uri = new StringBuilder();
//        uri.append(request.getScheme());
//        uri.append("://");
//        uri.append(request.getServerName());
//        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
//            uri.append(":");
//            uri.append(request.getServerPort());
//        }
//        uri.append(request.getContextPath());
//        uri.append(request.getServletPath());
//
//        String reqInfo = RequestHandler.getRequestUri(request.getPathInfo());
//        if (!reqInfo.startsWith("/")) {
//            reqInfo = "/" + reqInfo;
//        }
//
//        uri.append(reqInfo);
        return uri.toString();
    }

    public static String processWfsEntity(String entityName, Node domNode, String templatePath) throws TemplateException, FileNotFoundException, IOException, URISyntaxException {
        String result = null;
        NodeModel nodeModel = NodeModel.wrap(domNode);
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("doc", nodeModel);
        ctx.put("entityName", entityName);
        StringWriter outWriter = new StringWriter();
        Template template = getDocTemplate(templatePath);
        template.process(ctx, outWriter);
        outWriter.close();
        result = outWriter.toString();
        return result;
    }

    public static Template getDocTemplate(String fileUrl)  throws FileNotFoundException, IOException, TemplateException, URISyntaxException {
        Template template = null;
        URL screenFileUrl = FlexibleLocation.resolveLocation(fileUrl, null);
        String urlStr = screenFileUrl.toString();
        URI uri = new URI(urlStr);
        File f = new File(uri);
        FileReader templateReader = new FileReader(f);
        Configuration conf = makeDefaultOfbizConfig();
        template = new Template("FMImportFilter", templateReader, conf);
        return template;
    }

    public static Configuration makeDefaultOfbizConfig() throws TemplateException, IOException {
        Configuration config = new Configuration();
        config.setObjectWrapper(BeansWrapper.getDefaultInstance());
        config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
        Configuration defaultOfbizConfig = config;
        return defaultOfbizConfig;
    }
}
