/*
 * $Id: MimeMessageWrapper.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.ofbiz.base.util.Debug;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
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
