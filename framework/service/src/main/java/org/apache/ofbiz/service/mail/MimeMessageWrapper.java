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
package org.apache.ofbiz.service.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.ofbiz.base.conversion.AbstractConverter;
import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilDateTime;

@SuppressWarnings("serial")
public class MimeMessageWrapper implements java.io.Serializable {

    private static final String MODULE = MimeMessageWrapper.class.getName();
    private transient MimeMessage message = null;
    private transient Session session = null;
    private Properties mailProperties = null;
    private String contentType = null;
    private byte[] serializedBytes = null;
    private int parts = 0;

    public MimeMessageWrapper(Session session, MimeMessage message) {
        this(session);
        this.setMessage(message);
    }

    public MimeMessageWrapper(Session session) {
        this.setSession(session);
    }

    /**
     * Sets session.
     * @param session the session
     */
    public synchronized void setSession(Session session) {
        this.session = session;
        this.mailProperties = session.getProperties();
    }

    /**
     * Gets session.
     * @return the session
     */
    public synchronized Session getSession() {
        if (session == null) {
            session = Session.getInstance(mailProperties, null);
        }
        return session;
    }

    /**
     * Sets message.
     * @param message the message
     */
    public synchronized void setMessage(MimeMessage message) {
        if (message != null) {
            // serialize the message
            this.message = message;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                message.writeTo(baos);
                baos.flush();
                serializedBytes = baos.toByteArray();
                this.contentType = message.getContentType();

                // see if this is a multi-part message
                Object content = message.getContent();
                if (content instanceof Multipart) {
                    Multipart mp = (Multipart) content;
                    this.parts = mp.getCount();
                } else {
                    this.parts = 0;
                }
            } catch (IOException | MessagingException e) {
                Debug.logError(e, MODULE);
            }
        }
    }

    /**
     * Gets message.
     * @return the message
     */
    public synchronized MimeMessage getMessage() {
        if (message == null) {
            // deserialize the message
            if (serializedBytes != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedBytes)) {
                    message = new MimeMessage(this.getSession(), bais);
                } catch (MessagingException | IOException e) {
                    Debug.logError(e, MODULE);
                    throw new GeneralRuntimeException(e.getMessage(), e);
                }
            }
        }
        return message;
    }

    /**
     * Gets first header.
     * @param header the header
     * @return the first header
     */
    public String getFirstHeader(String header) {
        String[] headers = getHeader(header);
        if (headers != null && headers.length > 0) {
            return headers[0];
        }
        return null;
    }

    /**
     * Get header string [ ].
     * @param header the header
     * @return the string [ ]
     */
    public String[] getHeader(String header) {
        MimeMessage message = getMessage();
        try {
            return message.getHeader(header);
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Get from address [ ].
     * @return the address [ ]
     */
    public Address[] getFrom() {
        MimeMessage message = getMessage();
        try {
            return message.getFrom();
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Get to address [ ].
     * @return the address [ ]
     */
    public Address[] getTo() {
        MimeMessage message = getMessage();
        try {
            return message.getRecipients(MimeMessage.RecipientType.TO);
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Get cc address [ ].
     * @return the address [ ]
     */
    public Address[] getCc() {
        MimeMessage message = getMessage();
        try {
            return message.getRecipients(MimeMessage.RecipientType.CC);
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Get bcc address [ ].
     * @return the address [ ]
     */
    public Address[] getBcc() {
        MimeMessage message = getMessage();
        try {
            return message.getRecipients(MimeMessage.RecipientType.BCC);
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Gets subject.
     * @return the subject
     */
    public String getSubject() {
        MimeMessage message = getMessage();
        try {
            return message.getSubject();
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Gets message id.
     * @return the message id
     */
    public String getMessageId() {
        MimeMessage message = getMessage();
        try {
            return message.getMessageID();
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Gets sent date.
     * @return the sent date
     */
    public Timestamp getSentDate() {
        MimeMessage message = getMessage();
        try {
            return UtilDateTime.toTimestamp(message.getSentDate());
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Gets received date.
     * @return the received date
     */
    public Timestamp getReceivedDate() {
        MimeMessage message = getMessage();
        try {
            return UtilDateTime.toTimestamp(message.getReceivedDate());
        } catch (MessagingException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Gets content type.
     * @return the content type
     */
    public synchronized String getContentType() {
        return contentType;
    }

    /**
     * Gets main part count.
     * @return the main part count
     */
    public synchronized int getMainPartCount() {
        return this.parts;
    }

    /**
     * Gets sub part count.
     * @param index the index
     * @return the sub part count
     */
    public int getSubPartCount(int index) {
        BodyPart part = getPart(Integer.toString(index));
        try {
            Object content = part.getContent();
            if (content instanceof Multipart) {
                return ((Multipart) content).getCount();
            }
            return 0;
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return -1;
        }
    }

    /**
     * Gets attachment indexes.
     * @return the attachment indexes
     */
    public List<String> getAttachmentIndexes() {
        List<String> attachments = new LinkedList<>();
        if (getMainPartCount() == 0) { // single part message (no attachments)
            return attachments;
        }
        for (int i = 0; i < getMainPartCount(); i++) {
            int subPartCount = getSubPartCount(i);
            String idx = Integer.toString(i);
            if (subPartCount > 0) {
                for (int si = 0; si < subPartCount; si++) {
                    String sidx = idx + "." + Integer.toString(si);
                    if (getPartDisposition(sidx) != null && (getPartDisposition(sidx).equalsIgnoreCase(Part.ATTACHMENT)
                            || getPartDisposition(sidx).equalsIgnoreCase(Part.INLINE))) {
                        attachments.add(sidx);
                    }
                }
            } else {
                if (getPartDisposition(idx) != null && (getPartDisposition(idx).equalsIgnoreCase(Part.ATTACHMENT)
                        || getPartDisposition(idx).equalsIgnoreCase(Part.INLINE))) {
                    attachments.add(idx);
                }
            }
        }
        return attachments;
    }

    /**
     * Gets message body.
     * @return the message body
     */
    public String getMessageBody() {
        MimeMessage message = getMessage();
        if (getMainPartCount() == 0) { // single part message
            try {
                Object content = message.getContent();
                return getContentText(content);
            } catch (Exception e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        StringBuffer body = new StringBuffer();
        for (int i = 0; i < getMainPartCount(); i++) {
            int subPartCount = getSubPartCount(i);
            String idx = Integer.toString(i);
            if (subPartCount > 0) {
                for (int si = 0; si < subPartCount; si++) {
                    String sidx = idx + "." + Integer.toString(si);
                    if (getPartContentType(sidx) != null && getPartContentType(sidx).toLowerCase(Locale.getDefault()).startsWith("text")) {
                        if (getPartDisposition(sidx) == null || getPartDisposition(sidx).equals(Part.INLINE)) {
                            body.append(getPartText(sidx)).append("\n");
                        }
                    }
                }
            } else {
                if (getPartContentType(idx) != null && getPartContentType(idx).toLowerCase(Locale.getDefault()).startsWith("text")) {
                    // make sure the part isn't an attachment
                    if (getPartDisposition(idx) == null || getPartDisposition(idx).equals(Part.INLINE)) {
                        body.append(getPartText(idx)).append("\n");
                    }
                }
            }
        }
        return body.toString();
    }

    /**
     * Gets message body content type.
     * @return the message body content type
     */
    public String getMessageBodyContentType() {
        String contentType = getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.getDefault()).startsWith("text")) {
            return contentType;
        }
        for (int i = 0; i < getMainPartCount(); i++) {
            int subPartCount = getSubPartCount(i);
            String idx = Integer.toString(i);
            if (subPartCount > 0) {
                for (int si = 0; si < subPartCount; si++) {
                    String sidx = idx + "." + Integer.toString(si);
                    if (getPartContentType(sidx) != null && getPartContentType(sidx).toLowerCase(Locale.getDefault()).startsWith("text")) {
                        if (getPartDisposition(sidx) == null || getPartDisposition(sidx).equals(Part.INLINE)) {
                            return getPartContentType(sidx);
                        }
                    }
                }
            } else {
                if (getPartContentType(idx) != null && getPartContentType(idx).toLowerCase(Locale.getDefault()).startsWith("text")) {
                    // make sure the part isn't an attachment
                    if (getPartDisposition(idx) == null || getPartDisposition(idx).equals(Part.INLINE)) {
                        return getPartContentType(idx);
                    }
                }
            }
        }
        return "text/html";
    }

    /**
     * Gets message raw text.
     * @return the message raw text
     */
    public String getMessageRawText() {
        MimeMessage message = getMessage();
        try {
            return getTextFromStream(message.getInputStream());
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Gets part description.
     * @param index the index
     * @return the part description
     */
    public String getPartDescription(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try {
                return part.getDescription();
            } catch (MessagingException e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part content type.
     * @param index the index
     * @return the part content type
     */
    public String getPartContentType(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try {
                return part.getContentType();
            } catch (MessagingException e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part disposition.
     * @param index the index
     * @return the part disposition
     */
    public String getPartDisposition(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try {
                return part.getDisposition();
            } catch (MessagingException e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part filename.
     * @param index the index
     * @return the part filename
     */
    public String getPartFilename(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try {
                return part.getFileName();
            } catch (MessagingException e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part byte buffer.
     * @param index the index
     * @return the part byte buffer
     */
    public ByteBuffer getPartByteBuffer(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try (InputStream stream = part.getInputStream()) {
                return getByteBufferFromStream(stream);
            } catch (Exception e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part text.
     * @param index the index
     * @return the part text
     */
    public String getPartText(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try {
                return getContentText(part.getContent());
            } catch (Exception e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part raw text.
     * @param index the index
     * @return the part raw text
     */
    public String getPartRawText(String index) {
        BodyPart part = getPart(index);
        if (part != null) {
            try {
                return getTextFromStream(part.getInputStream());
            } catch (Exception e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets part.
     * @param indexStr the index str
     * @return the part
     */
    public BodyPart getPart(String indexStr) {
        int mainIndex;
        int subIndex;
        try {
            if (indexStr.indexOf('.') == -1) {
                mainIndex = Integer.parseInt(indexStr);
                subIndex = -1;
            } else {
                String[] indexSplit = indexStr.split("\\.");
                mainIndex = Integer.parseInt(indexSplit[0]);
                subIndex = Integer.parseInt(indexSplit[1]);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Debug.logError(e, "Illegal index string format. Should be part 'dot' subpart: " + indexStr, MODULE);
            return null;
        }

        if (getMainPartCount() > 0 && getMainPartCount() > mainIndex) {
            MimeMessage message = this.getMessage();
            try {
                Multipart mp = (Multipart) message.getContent();
                if (subIndex == -1) {
                    return mp.getBodyPart(mainIndex);
                }
                BodyPart part = mp.getBodyPart(mainIndex);
                int subPartCount = getSubPartCount(mainIndex);
                if (subPartCount > subIndex) {
                    Multipart sp = (Multipart) part.getContent();
                    return sp.getBodyPart(subIndex);
                }
                Debug.logWarning("Requested a subpart [" + subIndex + "] which deos not exist; only [" + getSubPartCount(mainIndex)
                        + "] parts", MODULE);
                // there is no sub part to find
                return part;
            } catch (MessagingException | IOException e) {
                Debug.logError(e, MODULE);
                return null;
            }
        }
        Debug.logWarning("Requested a part [" + mainIndex + "] which deos not exist; only [" + getMainPartCount() + "] parts", MODULE);
        return null;
    }

    /**
     * Gets content text.
     * @param content the content
     * @return the content text
     */
    protected String getContentText(Object content) {
        if (content == null) {
            return null;
        }
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof InputStream) {
            return getTextFromStream((InputStream) content);
        } else if (content instanceof Message) {
            try {
                return getTextFromStream(((Message) content).getInputStream());
            } catch (Exception e) {
                Debug.logError(e, MODULE);
                return null;
            }
        } else {
            Debug.logWarning("Content was not a string or a stream; no known handler -- " + content.toString(), MODULE);
            return null;
        }
    }

    /**
     * Gets text from stream.
     * @param stream the stream
     * @return the text from stream
     */
    protected String getTextFromStream(InputStream stream) {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        try {
            // CHECKSTYLE_OFF: ALMOST_ALL
            for (int n; (n = stream.read(buffer)) != -1;) {
                // CHECKSTYLE_ON: ALMOST_ALL
                builder.append(new String(buffer, 0, n, "UTF-8"));
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return null;
        }
        return builder.toString();
    }

    /**
     * Gets byte buffer from stream.
     * @param stream the stream
     * @return the byte buffer from stream
     */
    protected ByteBuffer getByteBufferFromStream(InputStream stream) {
        byte[] buffer = new byte[4096];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // CHECKSTYLE_OFF: ALMOST_ALL
            for (int n; (n = stream.read(buffer)) != -1;) {
                // CHECKSTYLE_ON: ALMOST_ALL
                baos.write(buffer, 0, n);
            }
            return ByteBuffer.wrap(baos.toByteArray());
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    static {
        Converters.registerConverter(new MimeMessageToString());
    }

    /**
     * Convert MimeMessageWrapper to String. This is used when sending emails.
     */
    private static class MimeMessageToString extends AbstractConverter<MimeMessageWrapper, String> {
        MimeMessageToString() {
            super(MimeMessageWrapper.class, String.class);
        }

        @Override
        public String convert(MimeMessageWrapper obj) throws ConversionException {
            return obj.toString();
        }
    }
}
