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

import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.util.ByteWrapper;

public class EmailWorker {

    public final static String module = EmailWorker.class.getName();

    public String getForwardedField(MimeMessage message) {
        String fieldValue = null;
        return fieldValue;
    }
    
    public static int addAttachmentsToCommEvent(MimeMessage message, String communicationEventId, LocalDispatcher dispatcher, GenericValue userLogin) 
        throws MessagingException, IOException, GenericServiceException {
        Map commEventMap = new HashMap();
        commEventMap.put("communicationEventId", communicationEventId);
        commEventMap.put("contentTypeId", "DOCUMENT");
        commEventMap.put("mimeTypeId", "text/html");
        commEventMap.put("userLogin", userLogin);
        String subject = message.getSubject();
        if (subject != null && subject.length() > 80) { 
            subject = subject.substring(0,80); // make sure not too big for database field. (20 characters for filename)
        }
        currentIndex = "";
        attachmentCount = 0;
        return addMultipartAttachementToComm((Multipart)message.getContent(), commEventMap, subject, dispatcher, userLogin);

    }
    private static String currentIndex = "";
    private static int attachmentCount = 0;
    private static int addMultipartAttachementToComm(Multipart multipart, Map commEventMap, String subject, LocalDispatcher dispatcher, GenericValue userLogin)
    throws MessagingException, IOException, GenericServiceException {
        try {
            int multipartCount = multipart.getCount();
            for (int i=0; i < multipartCount; i++) {
                Part part = multipart.getBodyPart(i);
                String thisContentTypeRaw = part.getContentType();
                int idx2 = thisContentTypeRaw.indexOf(";");
                if (idx2 == -1) idx2 = thisContentTypeRaw.length();
                String thisContentType = thisContentTypeRaw.substring(0, idx2);
                String disposition = part.getDisposition();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                if (thisContentType.startsWith("multipart") || thisContentType.startsWith("Multipart")) {
                    currentIndex = currentIndex.concat("." + i);
                    return    addMultipartAttachementToComm((Multipart) part.getContent(), commEventMap, subject, dispatcher, userLogin);
                }
                
                if(currentIndex.concat("." + i).equals(EmailServices.contentIndex)) continue;

                // The first test should not pass, because if it exists, it should be the bodyContentIndex part
                if (((disposition == null) && (i == 0) && thisContentType.startsWith("text")) 
                        || ((disposition != null)
                                && (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))
                                ) )
                {
                    String attFileName = part.getFileName(); 
                    if (!UtilValidate.isEmpty(attFileName)) { 
                           commEventMap.put("contentName", attFileName); 
                           commEventMap.put("description", subject + "-" + attachmentCount);
                    } else {
                        commEventMap.put("contentName", subject + "-" + attachmentCount);
                    }
                    commEventMap.put("drMimeTypeId", thisContentType);
                    if (thisContentType.startsWith("text")) {
                        String content = (String)part.getContent();
                        commEventMap.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
                        commEventMap.put("textData", content);
                    } else {
                        InputStream is = part.getInputStream();
                        int c;
                        while ((c = is.read()) > -1) {
                            baos.write(c);
                        }
                        ByteWrapper imageData = new ByteWrapper(baos.toByteArray());
                        int len = imageData.getLength();
                        if (Debug.infoOn()) Debug.logInfo("imageData length: " + len, module);
                        commEventMap.put("drDataResourceName", part.getFileName());
                        commEventMap.put("imageData", imageData);
                        commEventMap.put("drDataResourceTypeId", "IMAGE_OBJECT");
                        commEventMap.put("_imageData_contentType", thisContentType);
                    }
                    dispatcher.runSync("createCommContentDataResource", commEventMap);
                    attachmentCount++;
                }
            }
        } catch (MessagingException e) {
            Debug.logError(e, module);
        } catch (IOException e) {
            Debug.logError(e, module);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return attachmentCount;
    }
}
