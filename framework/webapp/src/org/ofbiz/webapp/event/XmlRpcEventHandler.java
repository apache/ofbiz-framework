/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.webapp.event;

import org.apache.xmlrpc.common.ServerStreamConnection;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfig;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.util.HttpUtil;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.ofbiz.service.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;
import java.util.Locale;
import java.util.Iterator;

import javolution.util.FastMap;

/**
 * XmlRpcEventHandler
 */
public class XmlRpcEventHandler extends XmlRpcHttpServer implements EventHandler {

    public static final String module = XmlRpcEventHandler.class.getName();
    public static final String dispatcherName = "xmlrpc-dispatcher";
    protected GenericDelegator delegator;
    protected LocalDispatcher dispatcher;
            
    public void init(ServletContext context) throws EventHandlerException {
        String delegatorName = context.getInitParameter("delegatorName");
        this.delegator = GenericDelegator.getGenericDelegator(delegatorName);
        this.dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);
        this.setHandlerMapping(new ServiceRpcHandler());        
    }

    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        String report = request.getParameter("echo");
        if (report != null) {
            StringBuffer buf = new StringBuffer();
            try {
                // read the inputstream buffer
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    buf.append(line).append("\n");
                }
            } catch (Exception e) {
                throw new EventHandlerException(e.getMessage(), e);
            }
            Debug.logInfo("Echo: " + buf.toString(), module);

            // echo back the request
            try {
                response.setContentType("text/xml");
                Writer out = response.getWriter();
                out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.write("<methodResponse>");
                out.write("<params><param>");
                out.write("<value><string><![CDATA[");
                out.write(buf.toString());
                out.write("]]></string></value>");
                out.write("</param></params>");
                out.write("</methodResponse>");
                out.flush();
            } catch (Exception e) {
                throw new EventHandlerException(e.getMessage(), e);
            }
        } else {
            try {
                this.execute(this.getXmlRpcConfig(request), new HttpStreamConnection(request, response));
            } catch (XmlRpcException e) {
                Debug.logError(e, module);
                throw new EventHandlerException(e.getMessage(), e);
            }
        }

        return null;
    }

    protected void setResponseHeader(ServerStreamConnection con, String header, String value) {
		((HttpStreamConnection) con).getResponse().setHeader(header, value);
	}

    protected XmlRpcHttpRequestConfig getXmlRpcConfig(HttpServletRequest req) {
		XmlRpcHttpRequestConfigImpl result = new XmlRpcHttpRequestConfigImpl();
		XmlRpcHttpServerConfig serverConfig = (XmlRpcHttpServerConfig) getConfig();

        result.setBasicEncoding(serverConfig.getBasicEncoding());
		result.setContentLengthOptional(serverConfig.isContentLengthOptional());
		result.setEnabledForExtensions(serverConfig.isEnabledForExtensions());
		result.setGzipCompressing(HttpUtil.isUsingGzipEncoding(req.getHeader("Content-Encoding")));
		result.setGzipRequesting(HttpUtil.isUsingGzipEncoding(req.getHeaders("Accept-Encoding")));
		result.setEncoding(req.getCharacterEncoding());
        //result.setEnabledForExceptions(serverConfig.isEnabledForExceptions());
		HttpUtil.parseAuthorization(result, req.getHeader("Authorization"));
        
        return result;
    }

    class OfbizRpcAuthHandler implements AbstractReflectiveHandlerMapping.AuthenticationHandler {

        public boolean isAuthorized(XmlRpcRequest xmlRpcReq) throws XmlRpcException {
            XmlRpcHttpRequestConfig config = (XmlRpcHttpRequestConfig) xmlRpcReq.getConfig();

            ModelService model;
            try {
                model = dispatcher.getDispatchContext().getModelService(xmlRpcReq.getMethodName());
            } catch (GenericServiceException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }

            if (model != null && model.auth) {
                String username = config.getBasicUserName();
                String password = config.getBasicPassword();

                // check the account
                Map context = FastMap.newInstance();
                context.put("login.username", username);
                context.put("login.password", password);

                Map resp;
                try {
                    resp = dispatcher.runSync("userLogin", context);
                } catch (GenericServiceException e) {
                    throw new XmlRpcException(e.getMessage(), e);
                }

                if (ServiceUtil.isError(resp)) {
                    return false;
                }
            }

            return true;
        }
    }

    class ServiceRpcHandler extends AbstractReflectiveHandlerMapping implements XmlRpcHandler {

        public ServiceRpcHandler() {
            this.setAuthenticationHandler(new OfbizRpcAuthHandler());
        }
        
        public XmlRpcHandler getHandler(String method) throws XmlRpcNoSuchHandlerException, XmlRpcException {
            ModelService model = null;
            try {
                model = dispatcher.getDispatchContext().getModelService(method);
            } catch (GenericServiceException e) {
                Debug.logWarning(e, module);
            }
            if (model == null) {
                throw new XmlRpcNoSuchHandlerException("No such service [" + method + "]");
            }
            return this;
        }

        public Object execute(XmlRpcRequest xmlRpcReq) throws XmlRpcException {
            DispatchContext dctx = dispatcher.getDispatchContext();
            String serviceName = xmlRpcReq.getMethodName();
            ModelService model = null;
            try {
                model = dctx.getModelService(serviceName);
            } catch (GenericServiceException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }

            // check remote invocation security
            if (model == null || !model.export) {
                throw new XmlRpcException("Unknown method");
            }

            // prepare the context -- single parameter type struct (map)
            Map context = this.getContext(xmlRpcReq, serviceName);            

            // add in auth parameters
            XmlRpcHttpRequestConfig config = (XmlRpcHttpRequestConfig) xmlRpcReq.getConfig();
            String username = config.getBasicUserName();
            String password = config.getBasicPassword();
            if (UtilValidate.isNotEmpty(username)) {
                context.put("login.username", username);
                context.put("login.password", password);
            }

            // add the locale to the context
            context.put("locale", Locale.getDefault());

            // invoke the service
            Map resp;
            try {
                resp = dispatcher.runSync(serviceName, context);
            } catch (GenericServiceException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
            if (ServiceUtil.isError(resp)) {
                Debug.logError(ServiceUtil.getErrorMessage(resp), module);                
            }

            // return only definied parameters                        
            return model.makeValid(resp, ModelService.OUT_PARAM, false, null);
        }

        protected Map getContext(XmlRpcRequest xmlRpcReq, String serviceName) throws XmlRpcException {
            ModelService model;
            try {
                model = dispatcher.getDispatchContext().getModelService(serviceName);
            } catch (GenericServiceException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }

            // context placeholder
            Map context = FastMap.newInstance();

            if (model != null) {
                int parameterCount = xmlRpcReq.getParameterCount();

                // more than one parameter; use list notation based on service def order
                if (parameterCount > 1) {
                    int x = 0;
                    Iterator i = model.getInParamNames().iterator();
                    while (i.hasNext()) {
                        String name = (String) i.next();
                        context.put(name, xmlRpcReq.getParameter(x));
                        x++;

                        if (x == parameterCount) {
                            break;
                        }
                    }

                // only one parameter; if its a map use it as the context; otherwise make sure the service takes one param
                } else if (parameterCount == 1) {
                    Object param = xmlRpcReq.getParameter(0);
                    if (param instanceof Map) {
                        context = (Map) param;
                    } else {
                        if (model.getDefinedInCount() == 1) {
                            String paramName = (String) model.getInParamNames().iterator().next();
                            context.put(paramName, xmlRpcReq.getParameter(0));
                        } else {
                            throw new XmlRpcException("More than one parameter defined on service; cannot call via RPC with parameter list");
                        }
                    }
                }

                // do map value conversions
                context = model.makeValid(context, ModelService.IN_PARAM);
            }

            return context;
        }
    }

    class HttpStreamConnection implements ServerStreamConnection {

        protected HttpServletRequest request;
        protected HttpServletResponse response;

        protected HttpStreamConnection(HttpServletRequest req, HttpServletResponse res) {
            this.request = req;
            this.response = res;
        }

        public HttpServletRequest getRequest() {
            return request;
        }

        public HttpServletResponse getResponse() {
            return response;
        }

        public InputStream newInputStream() throws IOException {
            return request.getInputStream();
        }

        public OutputStream newOutputStream() throws IOException {
            response.setContentType("text/xml");
            return response.getOutputStream();
        }

        public void close() throws IOException {
            response.getOutputStream().close();
        }
    }
}
