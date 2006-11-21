/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.util.ByteWrapper;

public class EmailWorker {

    public final static String module = EmailWorker.class.getName();

    public String getForwardedField(MimeMessage message) {
    	
    	String fieldValue = null;
    	
    	return fieldValue;
    }
    
    public static int addAttachmentsToCommEvent(MimeMessage message, String communicationEventId, int bodyContentIndex, LocalDispatcher dispatcher, GenericValue userLogin) 
    	throws MessagingException, IOException, GenericServiceException {
    	int attachmentCount =0;
    	Map commEventMap = new HashMap();
    	commEventMap.put("communicationEventId", communicationEventId);
    	commEventMap.put("contentTypeId", "DOCUMENT");
		commEventMap.put("mimeTypeId", "text/html");
		commEventMap.put("userLogin", userLogin);
		String subject = message.getSubject();
		if (subject != null && subject.length() > 80) { 
			subject = subject.substring(0,80); // make sure not too big for database field. (20 characters for filename)
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	
		Multipart multipart = (Multipart)message.getContent();
		int multipartCount = multipart.getCount();
		for (int i=0; i < multipartCount; i++) {
			Part part = multipart.getBodyPart(i);
			String thisContentTypeRaw = part.getContentType();
            int idx2 = thisContentTypeRaw.indexOf(";");
            if (idx2 == -1) idx2 = thisContentTypeRaw.length();
            String thisContentType = thisContentTypeRaw.substring(0, idx2);
			String disposition = part.getDisposition();
			
			// The first test should not pass, because if it exists, it should be the bodyContentIndex part
			if (((disposition == null) && (i == 0) && (i != bodyContentIndex)  && thisContentType.startsWith("text")) 
               || ((disposition != null)
					 && (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))
				     && (i != bodyContentIndex)) )
		    {
				String attFileName = part.getFileName();
				if (attFileName != null && attFileName.length() > 17) {
					attFileName = attFileName.substring(0,17);
				}
				commEventMap.put("contentName", subject + "-" + i + " " + attFileName);
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

    	return attachmentCount;
    }
}
