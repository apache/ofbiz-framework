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
package org.ofbiz.content.email;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.TemplateException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Provides generic services related to preparing and
 * delivering notifications via email.
 * <p>
 * To use the NotificationService, a message specific service should be 
 * defined for a particular <a href="http://freemarker.sourceforge.net/docs/dgui_quickstart_template.html">
 * Freemarker Template</a> mapping the required fields of the 
 * template to the required attributes of the service.
 * <p>
 * This service definition should extend the <code>sendNotificationInterface<code>
 * or the <code>prepareNotificationInterface</code> service interface
 * and simply invoke the associated method defined in this class.
 * <p>
 * <blockquote>
 * <pre>
 *     &lt;service name="sendPoPickupNotification" engine="java"
 *             location="org.ofbiz.content.email.NotificationServices" invoke="sendNotification"&gt;
 *         &lt;description&gt;Sends notification based on a message template&lt;/description&gt;
 *         &lt;implements service="sendNotificationInterface"/&gt;
 *         &lt;attribute name="orderId" type="String" mode="IN" optional="false"/&gt;
 *     &lt;/service&gt;
 * </pre>
 * </blockquote>
 * <p>
 * An optional parameter available to all message templates is <code>baseUrl</code>
 * which can either be specified when the service is invoked or let the 
 * <code>NotificationService</code> attempt to resolve it as best it can, 
 * see {@link #setBaseUrl(GenericDelegator, String, Map) setBaseUrl(Map)} for details on how this is achieved.
 * <p>
 * The following example shows what a simple notification message template, 
 * associated with the above service, might contain: 
 * <blockquote>
 * <pre>
 *     Please use the following link to schedule a delivery date:
 *     &lt;p&gt;
 *     ${baseUrl}/ordermgr/control/schedulepo?orderId=${orderId}"
 * </pre>
 * </blockquote> 
 * <p>
 * The template file must be found on the classpath at runtime and
 * match the "templateName" field passed to the service when it
 * is invoked.
 * <p>
 * For complex messages with a large number of dynamic fields, it may be wise
 * to implement a custom service that takes one or two parameters that can
 * be used to resolve the rest of the required fields and then pass them to
 * the {@link #prepareNotification(DispatchContext, Map) prepareNotification(DispatchContext, Map)}
 * or {@link #sendNotification(DispatchContext, Map) sendNotification(DispatchContext, Map)} 
 * methods directly to generate or generate and send the notification respectively.
 * 
 */
public class NotificationServices {

    public static final String module = NotificationServices.class.getName();

    /** 
     * This will use the {@link #prepareNotification(DispatchContext, Map) prepareNotification(DispatchContext, Map)}
     * method to generate the body of the notification message to send
     * and then deliver it via the "sendMail" service.
     * <p>
     * If the "body" parameter is already specified, the message body generation
     * phase will be skipped and the notification will be sent with the
     * specified body instead. This can be used to combine both service
     * calls in a decoupled manner if other steps are required between
     * generating the message body and sending the notification.
     * 
     * @param ctx   The dispatching context of the service
     * @param context The map containing all the fields associated with
     * the sevice
     * @return A Map with the service response messages in it
     */
    public static Map sendNotification(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();                      
        Map result = null;
                
        try {
            // see whether the optional 'body' attribute was specified or needs to be processed
            // nulls are handled the same as not specified
            String body = (String) context.get("body");            
            
            if (body == null) {                                        
                // prepare the body of the notification email                         
                Map bodyResult = prepareNotification(ctx, context);
                
                // ensure the body was generated successfully                
                if (bodyResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                    body = (String) bodyResult.get("body");
                } else {
                    // otherwise just report the error
                    Debug.logError("prepareNotification failed: " + bodyResult.get(ModelService.ERROR_MESSAGE), module);
                    body = null;
                }
            }
            
            // make sure we have a valid body before sending
            if (body != null) {
                // retain only the required attributes for the sendMail service
                Map emailContext = new HashMap();
                emailContext.put("sendTo", context.get("sendTo"));
                emailContext.put("body", body);
                emailContext.put("sendCc", context.get("sendCc"));
                emailContext.put("sendBcc", context.get("sendBcc"));
                emailContext.put("sendFrom", context.get("sendFrom"));
                emailContext.put("subject", context.get("subject"));
                emailContext.put("sendVia", context.get("sendVia"));
                emailContext.put("sendType", context.get("sendType"));
                emailContext.put("contentType", context.get("contentType"));
    
                // pass on to the sendMail service                 
                result = dispatcher.runSync("sendMail", emailContext);            
            } else {
                Debug.logError("Invalid email body; null is not allowed", module);
                result = ServiceUtil.returnError("Invalid email body; null is not allowed");
            }
        } catch (GenericServiceException serviceException) {
            Debug.logError(serviceException, "Error sending email", module);
            result = ServiceUtil.returnError("Email delivery error, see error log");
        }

        return result;
    }

    /**
     * This will process the associated notification template definition
     * with all the fields contained in the given context and generate
     * the message body of the notification.
     * <p>
     * The result returned will contain the appropriate response
     * messages indicating succes or failure and the OUT parameter,
     * "body" containing the generated message.
     * 
     * @param ctx   The dispatching context of the service
     * @param context The map containing all the fields associated with
     * the sevice
     * @return A new Map indicating success or error containing the
     * body generated from the template and the input parameters. 
     */
    public static Map prepareNotification(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        String templateName = (String) context.get("templateName");
        Map templateData = (Map) context.get("templateData");
        String webSiteId = (String) context.get("webSiteId");
                        
        Map result = null;                               
        if (templateData == null) {
            templateData = new HashMap();
        }
               
        try {
            // ensure the baseURl is defined
            setBaseUrl(delegator, webSiteId, templateData);
                                   
            // initialize the template reader and processor
            URL templateUrl = UtilURL.fromResource(templateName);
            
            if (templateUrl == null) {
                Debug.logError("Problem getting the template URL: " + templateName + " not found", module);
                return ServiceUtil.returnError("Problem finding template; see logs");
            }
                                    
            // process the template with the given data and write
            // the email body to the String buffer
            Writer writer = new StringWriter();
            FreeMarkerWorker.renderTemplate(templateUrl.toExternalForm(), templateData, writer);

            // extract the newly created body for the notification email
            String notificationBody = writer.toString();            
            
            // generate the successfull reponse
            result = ServiceUtil.returnSuccess("Message body generated successfully");
            result.put("body", notificationBody);
        } catch (IOException ie) {
            Debug.logError(ie, "Problems reading template", module);
            result = ServiceUtil.returnError("Template reading problem, see error logs");
        } catch (TemplateException te) {
            Debug.logError(te, "Problems processing template", module);
            result = ServiceUtil.returnError("Template processing problem, see error log");
        }

        return result;
    }

    /**
     * The expectation is that a lot of notification messages will include
     * a link back to one or more pages in the system, which will require knowledge
     * of the base URL to extrapolate. This method will ensure that the
     * <code>baseUrl</code> field is set in the given context.
     * <p>
     * If it has been specified a default <code>baseUrl</code> will be
     * set using a best effort approach. If it is specified in the 
     * url.properties configuration files of the system, that will be
     * used, otherwise it will attempt resolve the fully qualified 
     * local host name.
     * <p>
     * <i>Note:</i> I thought it might be useful to have some dynamic way
     * of extending the default properties provided by the NotificationService,
     * such as the <code>baseUrl</code>, perhaps using the standard 
     * <code>ResourceBundle</code> java approach so that both classes
     * and static files may be invoked.
     *  
     * @param context   The context to check and, if necessary, set the
     * <code>baseUrl</code>.
     */
    public static void setBaseUrl(GenericDelegator delegator, String webSiteId, Map context) {
        // If the baseUrl was not specified we can do a best effort instead
        if (!context.containsKey("baseUrl")) {
            StringBuffer httpBase = null;
            StringBuffer httpsBase = null;
                    
            String localServer = null;        
                   
            String httpsPort = null;
            String httpsServer = null;
            String httpPort = null;
            String httpServer = null;
            Boolean enableHttps = null;        

            try {
                // using just the IP address of localhost if we don't have a defined server
                InetAddress localHost = InetAddress.getLocalHost();
                localServer = localHost.getHostAddress();
            } catch (UnknownHostException hostException) {
                Debug.logWarning(hostException, "Could not determine localhost, using '127.0.0.1'", module);
                localServer = "127.0.0.1";
            }

            // load the properties from the website entity        
            GenericValue webSite = null;
            if (webSiteId != null) {
                try {
                    webSite = delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId));
                    if (webSite != null) {
                        httpsPort = webSite.getString("httpsPort");
                        httpsServer = webSite.getString("httpsHost");
                        httpPort = webSite.getString("httpPort");
                        httpServer = webSite.getString("httpHost");
                        enableHttps = webSite.getBoolean("enableHttps");
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "Problems with WebSite entity; using global defaults", module);
                }
            }
            
            // fill in any missing properties with fields from the global file
            if (UtilValidate.isEmpty(httpsPort)) {            
                httpsPort = UtilProperties.getPropertyValue("url.properties", "port.https", "443");
            }
            if (UtilValidate.isEmpty(httpsServer)) {
                httpsServer = UtilProperties.getPropertyValue("url.properties", "force.https.host", localServer);
            }
            if (UtilValidate.isEmpty(httpPort)) {
                httpPort = UtilProperties.getPropertyValue("url.properties", "port.http", "80");
            }
            if (UtilValidate.isEmpty(httpServer)) {
                httpServer = UtilProperties.getPropertyValue("url.properties", "force.http.host", localServer);
            }
            if (UtilValidate.isEmpty(enableHttps)) {
                enableHttps = (UtilProperties.propertyValueEqualsIgnoreCase("url.properties", "port.https.enabled", "Y")) ? Boolean.TRUE : Boolean.FALSE;
            }
                                
            // prepare the (non-secure) URL
            httpBase = new StringBuffer("http://");
            httpBase.append(httpServer);
            if (!"80".equals(httpPort)) {
                httpBase.append(":");
                httpBase.append(httpPort);
            }

            // set the base (non-secure) URL for any messages requiring it
            context.put("baseUrl", httpBase.toString());
            
            if (enableHttps.booleanValue()) {
                // prepare the (secure) URL
                httpsBase = new StringBuffer("https://");
                httpsBase.append(httpsServer);
                if (!"443".equals(httpsPort)) {
                    httpsBase.append(":");
                    httpsBase.append(httpsPort);
                }
    
                // set the base (secure) URL for any messages requiring it
                context.put("baseSecureUrl", httpsBase.toString());
            } else {
                context.put("baseSecureUrl", httpBase.toString());
            }                        
        }
    }
}
