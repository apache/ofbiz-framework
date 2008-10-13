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


import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.mail.MimeMessageWrapper;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.ofbiz.widget.fo.FoScreenRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.xml.sax.SAXException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.security.Security;

/**
 * Email Services
 */
public class EmailServices {

    public final static String module = EmailServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoScreenRenderer foScreenRenderer = new FoScreenRenderer();

    /**
     * Basic JavaMail Service
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMail(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map results = ServiceUtil.returnSuccess();
        String subject = (String) context.get("subject");
        String partyId = (String) context.get("partyId");
        String body = (String) context.get("body");
        List bodyParts = (List) context.get("bodyParts");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        results.put("partyId", partyId);
        results.put("subject", subject);
        if (UtilValidate.isNotEmpty(body)) {
            results.put("body", body);
        }
        if (UtilValidate.isNotEmpty(bodyParts)) {
            results.put("bodyParts", bodyParts);
        }
        results.put("userLogin", userLogin);

        // first check to see if sending mail is enabled
        String mailEnabled = UtilProperties.getPropertyValue("general.properties", "mail.notifications.enabled", "N");
        if (!"Y".equalsIgnoreCase(mailEnabled)) {
            // no error; just return as if we already processed
            Debug.logImportant("Mail notifications disabled in general.properties; here is the context with info that would have been sent: " + context, module);
            return results;
        }
        String sendTo = (String) context.get("sendTo");
        String sendCc = (String) context.get("sendCc");
        String sendBcc = (String) context.get("sendBcc");

        // check to see if we should redirect all mail for testing
        String redirectAddress = UtilProperties.getPropertyValue("general.properties", "mail.notifications.redirectTo");
        if (UtilValidate.isNotEmpty(redirectAddress)) {
            String originalRecipients = " [To: " + sendTo + ", Cc: " + sendCc + ", Bcc: " + sendBcc + "]";
            subject += originalRecipients;
            sendTo = redirectAddress;
            sendCc = null;
            sendBcc = null;
        }

        String sendFrom = (String) context.get("sendFrom");
        String sendType = (String) context.get("sendType");
        String port = (String) context.get("port");
        String socketFactoryClass = (String) context.get("socketFactoryClass");
        String socketFactoryPort  = (String) context.get("socketFactoryPort");
        String socketFactoryFallback  = (String) context.get("socketFactoryFallback");
        String sendVia = (String) context.get("sendVia");
        String authUser = (String) context.get("authUser");
        String authPass = (String) context.get("authPass");
        String messageId = (String) context.get("messageId");
        String contentType = (String) context.get("contentType");

        boolean useSmtpAuth = false;

        // define some default
        if (sendType == null || sendType.equals("mail.smtp.host")) {
            sendType = "mail.smtp.host";
            if (UtilValidate.isEmpty(sendVia)) {
                sendVia = UtilProperties.getPropertyValue("general.properties", "mail.smtp.relay.host", "localhost");
            }
            if (UtilValidate.isEmpty(authUser)) {
                authUser = UtilProperties.getPropertyValue("general.properties", "mail.smtp.auth.user");
            }
            if (UtilValidate.isEmpty(authPass)) {
                authPass = UtilProperties.getPropertyValue("general.properties", "mail.smtp.auth.password");
            }
            if (UtilValidate.isNotEmpty(authUser)) {
                useSmtpAuth = true;
            }
            if (UtilValidate.isEmpty(port)) {
                port = UtilProperties.getPropertyValue("general.properties", "mail.smtp.port");
            }
            if (UtilValidate.isEmpty(socketFactoryPort)) {
                socketFactoryPort = UtilProperties.getPropertyValue("general.properties", "mail.smtp.socketFactory.port");
            }
            if (UtilValidate.isEmpty(socketFactoryClass)) {
                socketFactoryClass = UtilProperties.getPropertyValue("general.properties", "mail.smtp.socketFactory.class");
            }                
            if (UtilValidate.isEmpty(socketFactoryFallback)) {
                socketFactoryFallback = UtilProperties.getPropertyValue("general.properties", "mail.smtp.socketFactory.fallback", "false");
            }                                        
        } else if (sendVia == null) {
            return ServiceUtil.returnError("Parameter sendVia is required when sendType is not mail.smtp.host");
        }

        if (contentType == null) {
            contentType = "text/html";
        }
        
        if (UtilValidate.isNotEmpty(bodyParts)) {
            contentType = "multipart/mixed";
        }
        results.put("contentType", contentType);

        try {
            Properties props = System.getProperties();
            props.put(sendType, sendVia);
            if (UtilValidate.isNotEmpty(port)) {
                props.put("mail.smtp.port", port);
            }
            if (UtilValidate.isNotEmpty(socketFactoryPort)) {
                props.put("mail.smtp.socketFactory.port", socketFactoryPort);                
            }                        
            if (UtilValidate.isNotEmpty(socketFactoryClass)) {
                props.put("mail.smtp.socketFactory.class", socketFactoryClass);
                Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            }
            if (UtilValidate.isNotEmpty(socketFactoryFallback)) {
                props.put("mail.smtp.socketFactory.fallback", socketFactoryFallback);
            }
            if (useSmtpAuth) {
                props.put("mail.smtp.auth", "true");
            }

            Session session = Session.getInstance(props);
            boolean debug = UtilProperties.propertyValueEqualsIgnoreCase("general.properties", "mail.debug.on", "Y");
            session.setDebug(debug);                        

            MimeMessage mail = new MimeMessage(session);
            if (messageId != null) {
                mail.setHeader("In-Reply-To", messageId);
                mail.setHeader("References", messageId);
            }
            mail.setFrom(new InternetAddress(sendFrom));
            mail.setSubject(subject, "UTF-8");
            mail.setHeader("X-Mailer", "Apache OFBiz, The Apache Open For Business Project");
            mail.setSentDate(new Date());
            mail.addRecipients(Message.RecipientType.TO, sendTo);

            if (UtilValidate.isNotEmpty(sendCc)) {
                mail.addRecipients(Message.RecipientType.CC, sendCc);
            }
            if (UtilValidate.isNotEmpty(sendBcc)) {
                mail.addRecipients(Message.RecipientType.BCC, sendBcc);
            }

            if (UtilValidate.isNotEmpty(bodyParts)) {
                // check for multipart message (with attachments)
                // BodyParts contain a list of Maps items containing content(String) and type(String) of the attachement
                MimeMultipart mp = new MimeMultipart();
                Debug.logInfo(bodyParts.size() + " multiparts found",module);
                Iterator bodyPartIter = bodyParts.iterator();
                while (bodyPartIter.hasNext()) {
                    Map bodyPart = (Map) bodyPartIter.next();
                    Object bodyPartContent = bodyPart.get("content");
                    MimeBodyPart mbp = new MimeBodyPart();

                    if (bodyPartContent instanceof String) {
                        Debug.logInfo("part of type: " + bodyPart.get("type") + " and size: " + bodyPart.get("content").toString().length() , module);
                        mbp.setText((String) bodyPartContent, "UTF-8", ((String) bodyPart.get("type")).substring(5));
                    } else if (bodyPartContent instanceof byte[]) {
                        ByteArrayDataSource bads = new ByteArrayDataSource((byte[]) bodyPartContent, (String) bodyPart.get("type"));
                        Debug.logInfo("part of type: " + bodyPart.get("type") + " and size: " + ((byte[]) bodyPartContent).length , module);
                        mbp.setDataHandler(new DataHandler(bads));
                    } else {
                        mbp.setDataHandler(new DataHandler(bodyPartContent, (String) bodyPart.get("type")));
                    }

                    String fileName = (String) bodyPart.get("filename");
                    if (fileName != null) {
                        mbp.setFileName(fileName);
                    }
                    mp.addBodyPart(mbp);
                }
                mail.setContent(mp);
                mail.saveChanges();
            } else {
                // create the singelpart message
                if (contentType.startsWith("text")) {
                    mail.setText(body, "UTF-8", contentType.substring(5));
                } else {
                    mail.setContent(body, contentType);
                }
                mail.saveChanges();
            }

            Transport trans = session.getTransport("smtp");
            if (!useSmtpAuth) {
                trans.connect();
            } else {
                trans.connect(sendVia, authUser, authPass);
            }
            trans.sendMessage(mail, mail.getAllRecipients());
            results.put("messageId", mail.getMessageID());
            trans.close();
        } catch (Exception e) {
            String errMsg = "Cannot send email message to [" + sendTo + "] from [" + sendFrom + "] cc [" + sendCc + "] bcc [" + sendBcc + "] subject [" + subject + "]";
            Debug.logError(e, errMsg, module);
            Debug.logError(e, "Email message that could not be sent to [" + sendTo + "] had context: " + context, module);
            return ServiceUtil.returnError(errMsg);
        }
        return results;
    }

    /**
     * JavaMail Service that gets body content from a URL
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMailFromUrl(DispatchContext ctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        // pretty simple, get the content and then call the sendMail method below
        String bodyUrl = (String) context.remove("bodyUrl");
        Map bodyUrlParameters = (Map) context.remove("bodyUrlParameters");

        URL url = null;

        try {
            url = new URL(bodyUrl);
        } catch (MalformedURLException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError("Malformed URL: " + bodyUrl + "; error was: " + e.toString());
        }

        HttpClient httpClient = new HttpClient(url, bodyUrlParameters);
        String body = null;

        try {
            body = httpClient.post();
        } catch (HttpClientException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError("Error getting content: " + e.toString());
        }

        context.put("body", body);
        Map result = sendMail(ctx, context);

        result.put("body", body);
        return result;
    }

    /**
     * JavaMail Service that gets body content from a Screen Widget
     * defined in the product store record and if available as attachment also.
     *@param dctx The DispatchContext that this service is operating in
     *@param serviceContext Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> sendMailFromScreen(DispatchContext dctx, Map<String, ? extends Object> rServiceContext) {
        Map<String, Object> serviceContext = UtilMisc.makeMapWritable(rServiceContext);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String webSiteId = (String) serviceContext.remove("webSiteId");
        String bodyText = (String) serviceContext.remove("bodyText");
        String bodyScreenUri = (String) serviceContext.remove("bodyScreenUri");
        String xslfoAttachScreenLocation = (String) serviceContext.remove("xslfoAttachScreenLocation");
        String attachmentName = (String) serviceContext.remove("attachmentName");
        Locale locale = (Locale) serviceContext.get("locale");
        Map bodyParameters = (Map) serviceContext.remove("bodyParameters");
        if (bodyParameters == null) {
            bodyParameters = MapStack.create();
        }
        if (!bodyParameters.containsKey("locale")) {
            bodyParameters.put("locale", locale);
        } else {
            locale = (Locale) bodyParameters.get("locale");
        }
        String partyId = (String) bodyParameters.get("partyId");
        if (UtilValidate.isNotEmpty(webSiteId)) {
            NotificationServices.setBaseUrl(dctx.getDelegator(), webSiteId, bodyParameters);
        }
        String contentType = (String) serviceContext.remove("contentType");

        if (UtilValidate.isEmpty(attachmentName)) {
            attachmentName = "Details.pdf";
        }
        StringWriter bodyWriter = new StringWriter();

        MapStack screenContext = MapStack.create();
        screenContext.put("locale", locale);
        ScreenRenderer screens = new ScreenRenderer(bodyWriter, screenContext, htmlScreenRenderer);
        screens.populateContextForService(dctx, bodyParameters);
        screenContext.putAll(bodyParameters);

        if (bodyScreenUri != null) {
            try {
                screens.render(bodyScreenUri);
            } catch (GeneralException e) {
                String errMsg = "Error rendering screen for email: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (IOException e) {
                String errMsg = "Error rendering screen for email: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (SAXException e) {
                String errMsg = "Error rendering screen for email: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (ParserConfigurationException e) {
                String errMsg = "Error rendering screen for email: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }
        
        boolean isMultiPart = false;
        
        // check if attachment screen location passed in
        if (UtilValidate.isNotEmpty(xslfoAttachScreenLocation)) {
            isMultiPart = true;
            // start processing fo pdf attachment
            try {
                Writer writer = new StringWriter();
                MapStack screenContextAtt = MapStack.create();
                // substitute the freemarker variables...
                ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContext, foScreenRenderer);
                screensAtt.populateContextForService(dctx, bodyParameters);
                screenContextAtt.putAll(bodyParameters);
                screensAtt.render(xslfoAttachScreenLocation);
                
                /*
                try { // save generated fo file for debugging
                    String buf = writer.toString();
                    java.io.FileWriter fw = new java.io.FileWriter(new java.io.File("/tmp/file1.xml"));
                    fw.write(buf.toString());
                    fw.close();
                } catch (IOException e) {
                    Debug.logError(e, "Couldn't save xsl-fo xml debug file: " + e.toString(), module);
                }
                */

                // create the input stream for the generation
                StreamSource src = new StreamSource(new StringReader(writer.toString()));

                // create the output stream for the generation
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                Fop fop = ApacheFopWorker.createFopInstance(baos, MimeConstants.MIME_PDF);
                ApacheFopWorker.transform(src, null, fop);

                // and generate the PDF
                baos.flush();
                baos.close();

                // store in the list of maps for sendmail....
                List bodyParts = FastList.newInstance();
                if (bodyText != null) {
                    bodyText = FlexibleStringExpander.expandString(bodyText, screenContext,  locale);
                    bodyParts.add(UtilMisc.toMap("content", bodyText, "type", "text/html"));
                } else {
                    bodyParts.add(UtilMisc.toMap("content", bodyWriter.toString(), "type", "text/html"));
                }
                bodyParts.add(UtilMisc.toMap("content", baos.toByteArray(), "type", "application/pdf", "filename", attachmentName));
                serviceContext.put("bodyParts", bodyParts);
            } catch (GeneralException ge) {
                String errMsg = "Error rendering PDF attachment for email: " + ge.toString();
                Debug.logError(ge, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (IOException ie) {
                String errMsg = "Error rendering PDF attachment for email: " + ie.toString();
                Debug.logError(ie, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (FOPException fe) {
                String errMsg = "Error rendering PDF attachment for email: " + fe.toString();
                Debug.logError(fe, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (SAXException se) {
                String errMsg = "Error rendering PDF attachment for email: " + se.toString();
                Debug.logError(se, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (ParserConfigurationException pe) {
                String errMsg = "Error rendering PDF attachment for email: " + pe.toString();
                Debug.logError(pe, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            isMultiPart = false;
            // store body and type for single part message in the context.
            if (bodyText != null) {
                bodyText = FlexibleStringExpander.expandString(bodyText, screenContext,  locale);
                serviceContext.put("body", bodyText);
            } else {
                serviceContext.put("body", bodyWriter.toString());
            }

            // Only override the default contentType in case of plaintext, since other contentTypes may be multipart
            //    and would require specific handling.
            if (contentType != null && contentType.equalsIgnoreCase("text/plain")) {
                serviceContext.put("contentType", "text/plain");
            } else {
                serviceContext.put("contentType", "text/html");
            }
        }
        
        // also expand the subject at this point, just in case it has the FlexibleStringExpander syntax in it...
        String subject = (String) serviceContext.remove("subject");
        subject = FlexibleStringExpander.expandString(subject, screenContext, locale);
        serviceContext.put("subject", subject);
        serviceContext.put("partyId", partyId);

        if (Debug.verboseOn()) Debug.logVerbose("sendMailFromScreen sendMail context: " + serviceContext, module);

        Map result = ServiceUtil.returnSuccess();
        try {
            if (isMultiPart) {
                dispatcher.runSync("sendMailMultiPart", serviceContext);
            } else {
                dispatcher.runSync("sendMail", serviceContext);
            }
        } catch (Exception e) {
            String errMsg = "Error send email :" + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } 
        result.put("body", bodyWriter.toString());
        return result;
    }
    
    /**
     * Store email as communication event
     *@param dctx The DispatchContext that this service is operating in
     *@param serviceContext Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
     public static Map<String, Object> storeEmailAsCommunication(DispatchContext dctx, Map<String, ? extends Object> serviceContext) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) serviceContext.get("userLogin");
        
        String subject = (String) serviceContext.get("subject");
        String body = (String) serviceContext.get("body");
        String partyId = (String) serviceContext.get("partyId");
        String communicationEventId = (String) serviceContext.get("communicationEventId");
        String contentType = (String) serviceContext.get("contentType");
        
        // only create a new communication event if the email is not already associated with one
        if (communicationEventId == null) {
            String partyIdFrom = (String) userLogin.get("partyId");
            Map commEventMap = FastMap.newInstance();
            commEventMap.put("communicationEventTypeId", "EMAIL_COMMUNICATION");
            commEventMap.put("statusId", "COM_COMPLETE");
            commEventMap.put("contactMechTypeId", "EMAIL_ADDRESS");
            commEventMap.put("partyIdFrom", partyIdFrom);
            commEventMap.put("partyIdTo", partyId);
            commEventMap.put("datetimeStarted", UtilDateTime.nowTimestamp());
            commEventMap.put("datetimeEnded", UtilDateTime.nowTimestamp());
            commEventMap.put("subject", subject);
            commEventMap.put("content", body);
            commEventMap.put("userLogin", userLogin);
            commEventMap.put("contentMimeTypeId", contentType);
            try {
                dispatcher.runSync("createCommunicationEvent", commEventMap);
            } catch (Exception e) {
                Debug.logError(e, "Cannot store email as communication event", module);
                return ServiceUtil.returnError("Cannot store email as communication event; see logs");
            }
        }
        
        return ServiceUtil.returnSuccess();
    }

    /** class to create a file in memory required for sending as an attachment */
    public static class StringDataSource implements DataSource {
        private String contentType;
        private ByteArrayOutputStream contentArray;
        
        public StringDataSource(String content, String contentType) throws IOException {
            this.contentType = contentType;
            contentArray = new ByteArrayOutputStream();
            contentArray.write(content.getBytes("iso-8859-1"));
            contentArray.flush();
            contentArray.close();
        }
        
        public String getContentType() {
            return contentType == null ? "application/octet-stream" : contentType;
        }
 
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(contentArray.toByteArray());
        }
 
        public String getName() {
            return "stringDatasource";
        }
 
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Cannot write to this read-only resource");
        }
    }

    /** class to create a file in memory required for sending as an attachment */
    public static class ByteArrayDataSource implements DataSource {
        private String contentType;
        private byte[] contentArray;
        
        public ByteArrayDataSource(byte[] content, String contentType) throws IOException {
            this.contentType = contentType;
            this.contentArray = content;
        }
        
        public String getContentType() {
            return contentType == null ? "application/octet-stream" : contentType;
        }
 
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(contentArray);
        }
 
        public String getName() {
            return "ByteArrayDataSource";
        }
 
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Cannot write to this read-only resource");
        }
    }
    
    /*
     * Helper method to retrieve the party information from the first email address of the Address[] specified.
     */
    private static Map getParyInfoFromEmailAddress(Address [] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException
    {
        InternetAddress emailAddress = null;
        Map map = null;
        Map result = null;
        
        if (addresses == null) {
            return null;
        }
        
        if (addresses.length > 0) {
            Address addr = addresses[0];
            if (addr instanceof InternetAddress) {
                emailAddress = (InternetAddress)addr;
            }
        }
        
        if (!UtilValidate.isEmpty(emailAddress)) {
            map = FastMap.newInstance();
            map.put("address", emailAddress.getAddress());
            map.put("userLogin", userLogin);
            result = dispatcher.runSync("findPartyFromEmailAddress", map);            
        }        
        
        return result;
    }
    
    /*
     * Calls findPartyFromEmailAddress service and returns a List of the results for the array of addresses 
     */
    private static List buildListOfPartyInfoFromEmailAddresses(Address [] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException
    {
        InternetAddress emailAddress = null;
        Address addr = null;
        Map map = null;
        Map result = null;
        List tempResults = FastList.newInstance();
        
        if (addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                addr = addresses[i];
                if (addr instanceof InternetAddress) {
                    emailAddress = (InternetAddress)addr;
                    
                    if (!UtilValidate.isEmpty(emailAddress)) {
                        result = dispatcher.runSync("findPartyFromEmailAddress", 
                                UtilMisc.<String, Object>toMap("address", emailAddress.getAddress(), "userLogin", userLogin));
                        if (result.get("partyId") != null) {
                            tempResults.add(result);
                        }
                    }
                }    
            }
        }
        return tempResults;
    }   
    
    public static String contentIndex = "";
    private static Map addMessageBody( Map commEventMap, Multipart multipart) 
    throws MessagingException, IOException {
        try {
            int multipartCount = multipart.getCount();
            for (int i=0; i < multipartCount  && i < 10; i++) { 
                Part part = multipart.getBodyPart(i);
                String thisContentTypeRaw = part.getContentType();
                String content = null;
                int idx2 = thisContentTypeRaw.indexOf(";");
                if (idx2 == -1) {
                    idx2 = thisContentTypeRaw.length();
                }
                String thisContentType = thisContentTypeRaw.substring(0, idx2);
                if (thisContentType == null || thisContentType.equals("")) thisContentType = "text/html";
                String disposition = part.getDisposition();

                if (thisContentType.startsWith("multipart") || thisContentType.startsWith("Multipart")) {
                    contentIndex = contentIndex.concat("." + i);
                    return addMessageBody(commEventMap, (Multipart) part.getContent());
                }
                // See this case where the disposition of the inline text is null
                else if ((disposition == null) && (i == 0) && thisContentType.startsWith("text")) {
                    content = (String)part.getContent();
                    if (UtilValidate.isNotEmpty(content)) {
                        contentIndex = contentIndex.concat("." + i);
                        commEventMap.put("content", content);
                        commEventMap.put("contentMimeTypeId", thisContentType);
                        return commEventMap;
                    }
                } else if ((disposition != null)
                        && (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))
                        && thisContentType.startsWith("text")) {
                    contentIndex = contentIndex.concat("." + i);
                    commEventMap.put("content", part.getContent());
                    commEventMap.put("contentMimeTypeId", thisContentType);
                    return commEventMap;
                }
            }
            return commEventMap;
        } catch (MessagingException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    /**
     * This service is the main one for processing incoming emails.
     * 
     * Its only argument is a wrapper for the JavaMail MimeMessage object. 
     * From this object, all the fields, headers and content of the message can be accessed.
     * 
     * The first thing this service does is try to discover the partyId of the message sender
     * by doing a reverse find on the email address. It uses the findPartyFromEmailAddress service to do this.
     * 
     * It then creates a CommunicationEvent entity by calling the createCommunicationEvent service using the appropriate fields from the email and the
     * discovered partyId, if it exists, as the partyIdFrom. Note that it sets the communicationEventTypeId
     * field to AUTO_EMAIL_COMM. This is useful for tracking email generated communications.
     * 
     * The service tries to find appropriate content for inclusion in the CommunicationEvent.content field. 
     * If the contentType of the content starts with "text", the getContent() call returns a string and it is used.
     * If the contentType starts with "multipart", then the "parts" of the content are iterated thru and the first
     * one of mime type, "text/..." is used.
     * 
     * If the contentType has a value of "multipart" then the parts of the content (except the one used in the main 
     * CommunicationEvent.content field) are cycled thru and attached to the CommunicationEvent entity using the 
     * createCommContentDataResource service. This happens in the EmailWorker.addAttachmentsToCommEvent method.
     * 
     * However multiparts can contain multiparts. A recursive function has been added.
     * 
     * -Al Byers - Hans Bakker
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> storeIncomingEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");
        MimeMessage message = wrapper.getMessage();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyIdTo = null;
        String partyIdFrom = null;
        String contentType = null;
        String communicationEventId = null;
        String contactMechIdFrom = null;
        String contactMechIdTo = null;
        
        Map result = null;
        try {
            String contentTypeRaw = message.getContentType();
            int idx = contentTypeRaw.indexOf(";");
            if (idx == -1) idx = contentTypeRaw.length();
            contentType = contentTypeRaw.substring(0, idx);
            if (contentType == null || contentType.equals("")) contentType = "text/html";
            Address[] addressesFrom = message.getFrom();
            Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
            Address[] addressesCC = message.getRecipients(MimeMessage.RecipientType.CC);
            Address[] addressesBCC = message.getRecipients(MimeMessage.RecipientType.BCC);
            String messageId = message.getMessageID();
            
            String aboutThisEmail = "message [" + messageId + "] from [" + 
                (addressesFrom[0] == null? "not found" : addressesFrom[0].toString()) + "] to [" + 
                (addressesTo[0] == null? "not found" : addressesTo[0].toString()) + "]";
            if (Debug.verboseOn()) Debug.logVerbose("Processing Incoming Email " + aboutThisEmail, module);

            // ignore the message when the spam status = yes
            String spamHeaderName = UtilProperties.getPropertyValue("general.properties", "mail.spam.name", "N");
            String configHeaderValue = UtilProperties.getPropertyValue("general.properties", "mail.spam.value");
            //          only execute when config file has been set && header variable found
            if (!spamHeaderName.equals("N") && message.getHeader(spamHeaderName) != null && message.getHeader(spamHeaderName).length > 0) { 
                String msgHeaderValue = message.getHeader(spamHeaderName)[0];
                if(msgHeaderValue != null && msgHeaderValue.startsWith(configHeaderValue)) {
                    Debug.logInfo("Incoming Email message ignored, was detected by external spam checker", module);
                    return ServiceUtil.returnSuccess(" Message Ignored: detected by external spam checker");
                }
            }

            // if no 'from' addresses specified ignore the message
            if (addressesFrom == null) {
                Debug.logInfo("Incoming Email message ignored, had not 'from' email address", module);
                return ServiceUtil.returnSuccess(" Message Ignored: no 'From' address specified");
            }

            // make sure this isn't a duplicate
            List commEvents;
            try {
                commEvents = delegator.findByAnd("CommunicationEvent", UtilMisc.toMap("messageId", messageId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (!commEvents.isEmpty()) {
                Debug.logInfo("Ignoring Duplicate Email: " + aboutThisEmail, module);
                return ServiceUtil.returnSuccess(" Message Ignored: deplicate messageId");
            } else {
                Debug.logInfo("Persisting New Email: " + aboutThisEmail, module);
            }

            
            // get the related partId's
            List toParties = buildListOfPartyInfoFromEmailAddresses(addressesTo, userLogin, dispatcher);        
            List ccParties = buildListOfPartyInfoFromEmailAddresses(addressesCC, userLogin, dispatcher);
            List bccParties = buildListOfPartyInfoFromEmailAddresses(addressesBCC, userLogin, dispatcher);

            //Get the first address from the list - this is the partyIdTo field of the CommunicationEvent
            if (!toParties.isEmpty()) {
                Iterator itr = toParties.iterator();
                Map firstAddressTo = (Map) itr.next();
                partyIdTo = (String)firstAddressTo.get("partyId");
                contactMechIdTo = (String)firstAddressTo.get("contactMechId");
            }

            String deliveredTo = null;
            if (message.getHeader("Delivered-To") != null) {
                deliveredTo = message.getHeader("Delivered-To")[0];
                // check if started with the domain name if yes remove including the dash.
                String dn = deliveredTo.substring(deliveredTo.indexOf("@")+1, deliveredTo.length());
                if (deliveredTo.startsWith(dn)) {
                    deliveredTo = deliveredTo.substring(dn.length()+1, deliveredTo.length());
                }
            }
            
            // if partyIdTo not found try to find the "to" address using the delivered-to header
            if ((partyIdTo == null) && (deliveredTo != null)) {
                result = dispatcher.runSync("findPartyFromEmailAddress", UtilMisc.<String, Object>toMap("address", deliveredTo, "userLogin", userLogin));          
                partyIdTo = (String)result.get("partyId");
                contactMechIdTo = (String)result.get("contactMechId");
            }
            if (userLogin.get("partyId") == null && partyIdTo != null) { 
                int ch = 0;
                for (ch=partyIdTo.length(); ch > 0 && Character.isDigit(partyIdTo.charAt(ch-1)); ch--) {
                    ;
                }
                userLogin.put("partyId", partyIdTo.substring(0,ch)); //allow services to be called to have prefix
            }
            
            // get the 'from' partyId
            result = getParyInfoFromEmailAddress(addressesFrom, userLogin, dispatcher);
            partyIdFrom = (String)result.get("partyId");
            contactMechIdFrom = (String)result.get("contactMechId");
            
            Map commEventMap = FastMap.newInstance();
            commEventMap.put("communicationEventTypeId", "AUTO_EMAIL_COMM");
            commEventMap.put("contactMechTypeId", "EMAIL_ADDRESS");
            commEventMap.put("messageId", messageId);
            
            String subject = message.getSubject();
            commEventMap.put("subject", subject);
                                    
            // Set sent and received dates
            commEventMap.put("entryDate", nowTimestamp);
            commEventMap.put("datetimeStarted", UtilDateTime.toTimestamp(message.getSentDate()));
            commEventMap.put("datetimeEnded", UtilDateTime.toTimestamp(message.getReceivedDate()));

            // default role types (_NA_)
            commEventMap.put("roleTypeIdFrom", "_NA_");
            commEventMap.put("roleTypeIdTo", "_NA_");

            // get the content(type) part
            Object messageContent = message.getContent();
            if (contentType.startsWith("text")) {
                commEventMap.put("content", messageContent);
                commEventMap.put("contentMimeTypeId", contentType);
            } else if (messageContent instanceof Multipart) {
                contentIndex = "";
                commEventMap = addMessageBody(commEventMap, (Multipart) messageContent);
            }

            // check for for a reply to communication event (using in-reply-to the parent messageID)
            String[] inReplyTo = message.getHeader("In-Reply-To");
            if (inReplyTo != null && inReplyTo[0] != null) {
                GenericValue parentCommEvent = null;
                try {
                    List events = delegator.findByAnd("CommunicationEvent", UtilMisc.toMap("messageId", inReplyTo[0]));
                    parentCommEvent = EntityUtil.getFirst(events);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (parentCommEvent != null) {
                    String parentCommEventId = parentCommEvent.getString("communicationEventId");
                    String orgCommEventId = parentCommEvent.getString("origCommEventId");
                    if (orgCommEventId == null) orgCommEventId = parentCommEventId;
                    commEventMap.put("parentCommEventId", parentCommEventId);
                    commEventMap.put("origCommEventId", orgCommEventId);
                } 
            }

            // Retrieve all the addresses from the email
            Set emailAddressesFrom = new TreeSet();
            Set emailAddressesTo = new TreeSet();
            Set emailAddressesCC = new TreeSet();
            Set emailAddressesBCC = new TreeSet();
            for (int x = 0 ; x < addressesFrom.length ; x++) {
                emailAddressesFrom.add(((InternetAddress) addressesFrom[x]).getAddress());
            }
            for (int x = 0 ; x < addressesTo.length ; x++) {
                emailAddressesTo.add(((InternetAddress) addressesTo[x]).getAddress());
            }
            if (addressesCC != null) {
                for (int x = 0 ; x < addressesCC.length ; x++) {
                    emailAddressesCC.add(((InternetAddress) addressesCC[x]).getAddress());
                }
            }
            if (addressesBCC != null) {
                for (int x = 0 ; x < addressesBCC.length ; x++) {
                    emailAddressesBCC.add(((InternetAddress) addressesBCC[x]).getAddress());
                }
            }
            String fromString = StringUtil.join(UtilMisc.toList(emailAddressesFrom), ",");
            String toString = StringUtil.join(UtilMisc.toList(emailAddressesTo), ",");
            String ccString = StringUtil.join(UtilMisc.toList(emailAddressesCC), ",");
            String bccString = StringUtil.join(UtilMisc.toList(emailAddressesBCC), ",");

            if (UtilValidate.isNotEmpty(fromString)) commEventMap.put("fromString", fromString);
            if (UtilValidate.isNotEmpty(toString)) commEventMap.put("toString", toString);
            if (UtilValidate.isNotEmpty(ccString)) commEventMap.put("ccString", ccString);            
            if (UtilValidate.isNotEmpty(bccString)) commEventMap.put("bccString", bccString);

            // store from/to parties, but when not found make a note of the email to/from address in the workEffort Note Section.
            String commNote = "";
            if (partyIdFrom != null) {
                commEventMap.put("partyIdFrom", partyIdFrom);                
                commEventMap.put("contactMechIdFrom", contactMechIdFrom);
            } else {
                commNote += "Sent from: " +  ((InternetAddress)addressesFrom[0]).getAddress() + "; ";
                commNote += "Sent Name from: " + ((InternetAddress)addressesFrom[0]).getPersonal() + "; ";
            }

            if (partyIdTo != null) {
                commEventMap.put("partyIdTo", partyIdTo);               
                commEventMap.put("contactMechIdTo", contactMechIdTo);
            } else {
                commNote += "Sent to: " + ((InternetAddress)addressesTo[0]).getAddress()  + "; ";
                if (deliveredTo != null) {
                    commNote += "Delivered-To: " + deliveredTo + "; ";
                }
            }
            
            commNote += "Sent to: " + ((InternetAddress)addressesTo[0]).getAddress()  + "; ";
            commNote += "Delivered-To: " + deliveredTo + "; ";

            if (partyIdTo != null && partyIdFrom != null) {
                commEventMap.put("statusId", "COM_ENTERED");
            } else {
                commEventMap.put("statusId", "COM_UNKNOWN_PARTY");
            }
            if (commNote.length() > 255) commNote = commNote.substring(0,255);
            
            if (!("".equals(commNote))) {
                commEventMap.put("note", commNote);
            }
            
            commEventMap.put("userLogin", userLogin);
            
            // Populate the CommunicationEvent.headerString field with the email headers
            String headerString = "";
            Enumeration headerLines = message.getAllHeaderLines();
            while (headerLines.hasMoreElements()) {
                headerString += System.getProperty("line.separator");
                headerString += headerLines.nextElement();
            }
            commEventMap.put("headerString", headerString);

            result = dispatcher.runSync("createCommunicationEvent", commEventMap);
            communicationEventId = (String)result.get("communicationEventId");
            
            if (messageContent instanceof Multipart) {
            	Debug.logInfo("===message has attachments=====", module);
                int attachmentCount = EmailWorker.addAttachmentsToCommEvent((Multipart) messageContent, subject, communicationEventId, dispatcher, userLogin);
                if (Debug.infoOn()) Debug.logInfo(attachmentCount + " attachments added to CommunicationEvent:" + communicationEventId,module);
            }
            
            // For all addresses create a CommunicationEventRoles
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, toParties, "ADDRESSEE");
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, ccParties, "CC");
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, bccParties, "BCC");
            
            Map results = ServiceUtil.returnSuccess();
            results.put("communicationEventId", communicationEventId);
            results.put("statusId", commEventMap.get("statusId"));
            return results;
        } catch (MessagingException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
    
    private static void createCommEventRoles(GenericValue userLogin, GenericDelegator delegator, LocalDispatcher dispatcher, String communicationEventId, List parties, String roleTypeId) {
    	// It's not clear what the "role" of this communication event should be, so we'll just put _NA_
    	// check and see if this role was already created and ignore if true
    	try {
    		Iterator it = parties.iterator();
    		while (it.hasNext()) {
    			Map result = (Map) it.next();
    			String partyId = (String) result.get("partyId");
    			GenericValue commEventRole = delegator.findByPrimaryKey("CommunicationEventRole", 
    					UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId));
    			if (commEventRole == null) {
    				// Check if the role exists for the partyId. If not, then first associate that role with the partyId
    				GenericValue partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId));
    				if (partyRole == null) {
    					dispatcher.runSync("createPartyRole", UtilMisc.<String, Object>toMap("partyId", partyId, "roleTypeId", roleTypeId, "userLogin", userLogin));
    				}
    				Map input = UtilMisc.toMap("communicationEventId", communicationEventId, 
    						"partyId", partyId, "roleTypeId", roleTypeId, "userLogin", userLogin, 
    						"contactMechId", (String) result.get("contactMechId"),
    						"statusId", "COM_ROLE_CREATED");
    				dispatcher.runSync("createCommunicationEventRole", input);
    			}
    		}
    	} catch (GenericServiceException e) {
    		Debug.logError(e, module);
    	} catch (Exception e) {
    		Debug.logError(e, module);
    	}
    }

}
