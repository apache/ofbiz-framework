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
package org.ofbiz.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.ofbiz.base.util.Debug;

public class MimeMessageWrapper implements java.io.Serializable {

    public static final String module = MimeMessageWrapper.class.getName();
    protected transient MimeMessage message = null;
    protected transient Session session = null;
    protected Properties mailProperties = null;
    protected byte[] serializedBytes = null;

    public MimeMessageWrapper(Session session, MimeMessage message) {
        this(session);
        this.setMessage(message);
    }

    public MimeMessageWrapper(Session session) {
        this.setSession(session);
    }

    public void setSession(Session session) {
        this.session = session;
        this.mailProperties = session.getProperties();
    }

    public synchronized Session getSession() {
        if (session == null) {
            session = Session.getInstance(mailProperties, null);
        }
        return session;
    }

    public void setMessage(MimeMessage message) {
        if (message != null) {
            // serialize the message
            this.message = message;            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                message.writeTo(baos);
                baos.flush();
                serializedBytes = baos.toByteArray();
            } catch (MessagingException e) {
                Debug.logError(e, module);
            } catch (IOException e) {
                Debug.logError(e, module);
            } finally {
                try {
                    baos.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }

    public synchronized MimeMessage getMessage() {
        if (message == null) {
            // deserialize the message
            if (serializedBytes != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(serializedBytes);
                try {
                    message = new MimeMessage(this.getSession(), bais);
                } catch (MessagingException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return message;
    }
}
