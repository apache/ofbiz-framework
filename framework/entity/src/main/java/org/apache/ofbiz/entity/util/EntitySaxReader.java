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
package org.apache.ofbiz.entity.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Base64;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilIO;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericEntityNotFoundException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.datasource.GenericHelper;
import org.apache.ofbiz.entity.eca.EntityEcaHandler;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;

/**
 * SAX XML Parser Content Handler for Entity Engine XML files
 */
public class EntitySaxReader extends DefaultHandler {
    public static final String module = EntitySaxReader.class.getName();
    public static final int DEFAULT_TX_TIMEOUT = 7200;

    protected org.xml.sax.Locator locator;
    private Delegator delegator;
    private EntityEcaHandler<?> ecaHandler = null;
    private GenericValue currentValue = null;
    private CharSequence currentFieldName = null;
    private char[] currentFieldValue = null;
    private long numberRead = 0;
    private long numberCreated = 0;
    private long numberUpdated = 0;
    private long numberReplaced = 0;
    private long numberDeleted = 0;
    private long numberSkipped = 0;

    private int valuesPerWrite = 100;
    private int valuesPerMessage = 1000;
    private int transactionTimeout = 7200;
    private boolean useTryInsertMethod = false;
    private boolean maintainTxStamps = false;
    private boolean createDummyFks = false;
    private boolean checkDataOnly = false;
    private boolean continueOnFail = false;
    private enum Action {CREATE, CREATE_UPDATE, CREATE_REPLACE, DELETE}
    private List<String> actionTags = UtilMisc.toList("create", "create-update", "create-replace", "delete");
    private Action currentAction = Action.CREATE_UPDATE;
    private List<Object> messageList = null;

    private List<GenericValue> valuesToWrite = new ArrayList<>(valuesPerWrite);
    private List<GenericValue> valuesToDelete = new ArrayList<>(valuesPerWrite);

    private boolean isParseForTemplate = false;
    private CharSequence templatePath = null;
    private Node rootNodeForTemplate = null;
    private Node currentNodeForTemplate = null;
    private Document documentForTemplate = null;
    private Map<String, Object> placeholderValues = null; //contains map of values for corresponding placeholders (eg. ${key}) in the entity xml data file.

    protected EntitySaxReader() {}

    public EntitySaxReader(Delegator delegator, int transactionTimeout) {
        // clone the delegator right off so there is no chance of making change to the initial object
        this.delegator = delegator.cloneDelegator();
        this.transactionTimeout = transactionTimeout;
    }

    public EntitySaxReader(Delegator delegator) {
        this(delegator, DEFAULT_TX_TIMEOUT);
    }

    public int getTransactionTimeout() {
        return this.transactionTimeout;
    }

    public void setUseTryInsertMethod(boolean value) {
        this.useTryInsertMethod = value;
    }

    public void setTransactionTimeout(int transactionTimeout) throws GenericTransactionException {
        if (this.transactionTimeout != transactionTimeout) {
            TransactionUtil.setTransactionTimeout(transactionTimeout);
            this.transactionTimeout = transactionTimeout;
        }
    }

    public void setMaintainTxStamps(boolean maintainTxStamps) {
        this.maintainTxStamps = maintainTxStamps;
    }

    public void setCreateDummyFks(boolean createDummyFks) {
        this.createDummyFks = createDummyFks;
    }

    public void setCheckDataOnly(boolean checkDataOnly) {
        this.checkDataOnly = checkDataOnly;
    }

    public void setContinueOnFail(boolean continueOnFail) {
        this.continueOnFail = continueOnFail;
    }

    public void setPlaceholderValues(Map<String,Object> placeholderValues) {
        this.placeholderValues = placeholderValues;
    }

    public List<Object> getMessageList() {
        if (this.checkDataOnly && this.messageList == null) {
            messageList = new LinkedList<>();
        }
        return this.messageList;
    }

    public void setDisableEeca(boolean disableEeca) {
        if (disableEeca) {
            if (this.ecaHandler == null) {
                this.ecaHandler = delegator.getEntityEcaHandler();
            }
            this.delegator.setEntityEcaHandler(null);
        } else {
            if (ecaHandler != null) {
                this.delegator.setEntityEcaHandler(ecaHandler);
            }
        }
    }

    private void setAction(Action action) {
        this.currentAction = action;
    }

    public long parse(String content) throws SAXException, java.io.IOException {
        if (content == null) {
            Debug.logWarning("content was null, doing nothing", module);
            return 0;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes("UTF-8"))) {
        return this.parse(bis, "Internal Content");
        }
    }

    public long parse(URL location) throws SAXException, java.io.IOException {
        if (location == null) {
            Debug.logWarning("location URL was null, doing nothing", module);
            return 0;
        }
        Debug.logImportant("Beginning import from URL: " + location.toExternalForm(), module);
        long numberRead = 0;
        try (InputStream is = location.openStream()) {
            numberRead = this.parse(is, location.toString());
        }
        return numberRead;
    }

    private long parse(InputStream is, String docDescription) throws SAXException, java.io.IOException {
        SAXParser parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch(ParserConfigurationException pce) {
            throw new SAXException("Unable to create the SAX parser", pce);
        }
        numberRead = 0;
        try {
            boolean beganTransaction = false;
            if (transactionTimeout > -1) {
                beganTransaction = TransactionUtil.begin(transactionTimeout);
                Debug.logImportant("Transaction Timeout set to " + transactionTimeout / 3600 + " hours (" + transactionTimeout + " seconds)", module);
            }
            try {
                parser.parse(is, this);
                // make sure all of the values to write got written...
                if (!valuesToWrite.isEmpty()) {
                    writeValues(valuesToWrite);
                    valuesToWrite.clear();
                }
                if (!valuesToDelete.isEmpty()) {
                    delegator.removeAll(valuesToDelete);
                    valuesToDelete.clear();
                }
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException | IOException | IllegalArgumentException | SAXException e) {
                String errMsg = "An error occurred saving the data, rolling back transaction (" + beganTransaction + ")";
                Debug.logError(e, errMsg, module);
                TransactionUtil.rollback(beganTransaction, errMsg, e);
                throw new SAXException("A transaction error occurred reading data", e);
            }
        } catch (GenericTransactionException e) {
            throw new SAXException("A transaction error occurred reading data", e);
        }
        Debug.logImportant("Finished " + numberRead + " values from " + docDescription, module);
        if (Debug.verboseOn()) { 
            Debug.logVerbose("  Detail created : " + numberCreated + ", skipped : " + numberSkipped +
                    ", updated : " + numberUpdated + ", replaced : " + numberReplaced +
                    ", deleted : " + numberDeleted, module);
        }
        return numberRead;
    }

    private void writeValues(List<GenericValue> valuesToWrite) throws GenericEntityException {
        if (this.checkDataOnly) {
            EntityDataAssert.checkValueList(valuesToWrite, delegator, this.getMessageList());
        } else {
            delegator.storeAll(valuesToWrite, new EntityStoreOptions(createDummyFks));
        }
    }

    private void countValue(boolean skip, boolean exist) {
        if (skip) numberSkipped++;
        else if (Action.DELETE == currentAction) numberDeleted++;
        else if (Action.CREATE == currentAction || !exist) numberCreated++;
        else if (Action.CREATE_REPLACE == currentAction) numberReplaced++;
        else numberUpdated++;
    }

    // ======== ContentHandler interface implementation ========

    @Override
    public void characters(char[] values, int offset, int count) throws SAXException {
        if (isParseForTemplate) {
            // if null, don't worry about it
            if (this.currentNodeForTemplate != null) {
                Node newNode = this.documentForTemplate.createTextNode(new String(values, offset, count));
                this.currentNodeForTemplate.appendChild(newNode);
            }
            return;
        }

        if (currentValue != null && currentFieldName != null) {
            char[] newChunk = Arrays.copyOfRange(values, offset, offset + count);
            if (currentFieldValue == null) {
                // this is the first chunk
                currentFieldValue = newChunk;
            } else {
                // append the new chunk to currentFieldValue
                char[] combined = new char[currentFieldValue.length + newChunk.length];
                System.arraycopy(currentFieldValue, 0, combined, 0, currentFieldValue.length);
                System.arraycopy(newChunk, 0, combined, currentFieldValue.length, newChunk.length);
                currentFieldValue = combined;
            }
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String fullNameString) throws SAXException {
        if (Debug.verboseOn()) Debug.logVerbose("endElement: localName=" + localName + ", fullName=" + fullNameString + ", numberRead=" + numberRead, module);
        if ("entity-engine-xml".equals(fullNameString)) {
            return;
        }
        if ("entity-engine-transform-xml".equals(fullNameString)) {
            // transform file & parse it, then return
            URL templateUrl = null;
            try {
                templateUrl = FlexibleLocation.resolveLocation(templatePath.toString());
            } catch (MalformedURLException e) {
                throw new SAXException("Could not find transform template with resource path [" + templatePath + "]; error was: " + e.toString());
            }

            if (templateUrl == null) {
                throw new SAXException("Could not find transform template with resource path: " + templatePath);
            } else {
                try {
                    BufferedReader templateReader = new BufferedReader(new InputStreamReader(templateUrl.openStream(),UtilIO.getUtf8()));

                    StringWriter outWriter = new StringWriter();
                    Configuration config = FreeMarkerWorker.newConfiguration();
                    config.setObjectWrapper(FreeMarkerWorker.getDefaultOfbizWrapper());
                    config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");

                    Template template = new Template("FMImportFilter", templateReader, config);
                    NodeModel nodeModel = NodeModel.wrap(this.rootNodeForTemplate);

                    Map<String, Object> context = new HashMap<>();
                    TemplateHashModel staticModels = FreeMarkerWorker.getDefaultOfbizWrapper().getStaticModels();
                    context.put("Static", staticModels);

                    context.put("doc", nodeModel);
                    template.process(context, outWriter);
                    String s = outWriter.toString();
                    if (Debug.verboseOn()) Debug.logVerbose("transformed xml: " + s, module);

                    EntitySaxReader reader = new EntitySaxReader(delegator);
                    reader.setUseTryInsertMethod(this.useTryInsertMethod);
                    try {
                        reader.setTransactionTimeout(this.transactionTimeout);
                    } catch (GenericTransactionException e1) {
                        Debug.logWarning("couldn't set tx timeout, hopefully shouldn't be a big deal", module);
                    }

                    numberRead += reader.parse(s);
                } catch (TemplateException | IOException e) {
                    throw new SAXException("Error storing value", e);
                }
            }

            return;
        }

        if (isParseForTemplate) {
            this.currentNodeForTemplate = this.currentNodeForTemplate.getParentNode();
            return;
        }

        //Test if end action tag, set action to default
        if (actionTags.contains(fullNameString)) {
            setAction(Action.CREATE_UPDATE);
            return;
        }

        if (currentValue != null) {
            if (currentFieldName != null) {
                if (UtilValidate.isNotEmpty(currentFieldValue)) {
                    if (currentValue.getModelEntity().isField(currentFieldName.toString())) {
                        ModelEntity modelEntity = currentValue.getModelEntity();
                        ModelField modelField = modelEntity.getField(currentFieldName.toString());
                        String type = modelField.getType();
                        if (type != null && "blob".equals(type)) {
                            byte[] binData = Base64.base64Decode((new String(currentFieldValue)).getBytes());
                            currentValue.setBytes(currentFieldName.toString(), binData);
                        } else {
                            currentValue.setString(currentFieldName.toString(), new String(currentFieldValue));
                        }
                    } else {
                        Debug.logWarning("Ignoring invalid field name [" + currentFieldName + "] found for the entity: "
                                + currentValue.getEntityName() + " with value=" + currentFieldValue.toString(), module);
                    }
                    currentFieldValue = null;
                }
                currentFieldName = null;
            } else {
                // before we write currentValue check to see if PK is there, if not and it is one field, generate it from a sequence using the entity name
                if (!currentValue.containsPrimaryKey()) {
                    if (currentValue.getModelEntity().getPksSize() == 1) {
                        ModelField modelField = currentValue.getModelEntity().getOnlyPk();
                        String newSeq = delegator.getNextSeqId(currentValue.getEntityName());
                        currentValue.setString(modelField.getName(), newSeq);
                    } else {
                        throw new SAXException("Cannot store value with incomplete primary key with more than 1 primary key field: " + currentValue);
                    }
                }

                try {
                    boolean exist = true;
                    boolean skip = false;
                    //if verbose on, check if entity exist on database for count each action
                    //It's necessary to check also for specific action CREATE and DELETE to ensure it's OK
                    if (Action.CREATE == currentAction || Action.DELETE == currentAction || Debug.verboseOn()) {
                        GenericHelper helper = delegator.getEntityHelper(currentValue.getEntityName());
                        if (currentValue.containsPrimaryKey()) {
                            try {
                                helper.findByPrimaryKey(currentValue.getPrimaryKey());
                            } catch (GenericEntityNotFoundException e) {exist = false;}
                        }
                        if (Action.CREATE == currentAction && exist) { skip = true; }
                        else if (Action.DELETE == currentAction && !exist) { skip = true; }
                    }
                    if (!skip) {
                        if (this.useTryInsertMethod && !this.checkDataOnly) {
                            if (Action.DELETE == currentAction) {
                                currentValue.remove();
                            } else {
                                currentValue.create();
                            }
                        } else {
                            if (Action.DELETE == currentAction) {
                                valuesToDelete.add(currentValue);
                                if (valuesToDelete.size() >= valuesPerWrite) {
                                    delegator.removeAll(valuesToDelete);
                                    valuesToDelete.clear();
                                }
                            } else {
                                valuesToWrite.add(currentValue);
                                if (valuesToWrite.size() >= valuesPerWrite) {
                                    writeValues(valuesToWrite);
                                    valuesToWrite.clear();
                                }
                            }
                        }
                    }
                    numberRead++;
                    if (Debug.verboseOn()) countValue(skip, exist);
                    if ((numberRead % valuesPerMessage) == 0) {
                        Debug.logImportant("Another " + valuesPerMessage + " values imported: now up to " + numberRead, module);
                    }
                    currentValue = null;
                } catch (GenericEntityException e) {
                    String errMsg = "Error performing action " + currentAction;
                    Debug.logError(e, errMsg, module);
                    throw new SAXException(errMsg, e);
                }
            }
        }
    }

    @Override
    public void setDocumentLocator(org.xml.sax.Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String namepsaceURI, String localName, String fullNameString, Attributes attributes) throws SAXException {
        if (Debug.verboseOn()) Debug.logVerbose("startElement: localName=" + localName + ", fullName=" + fullNameString + ", attributes=" + attributes, module);
        if ("entity-engine-xml".equals(fullNameString)) {
            // check the maintain-timestamp flag
            CharSequence maintainTx = attributes.getValue("maintain-timestamps");
            if (maintainTx != null) {
                this.setMaintainTxStamps("true".equalsIgnoreCase(maintainTx.toString()));
            }

            // check the disable-eeca flag
            CharSequence ecaDisable = attributes.getValue("disable-eeca");
            if (ecaDisable != null) {
                this.setDisableEeca("true".equalsIgnoreCase(ecaDisable.toString()));
            }

            // check the use-dummy-fk flag
            CharSequence dummyFk = attributes.getValue("create-dummy-fk");
            if (dummyFk != null) {
                this.setCreateDummyFks("true".equalsIgnoreCase(dummyFk.toString()));
            }

            return;
        }

        if ("entity-engine-transform-xml".equals(fullNameString)) {
            templatePath = attributes.getValue("template");
            isParseForTemplate = true;
            documentForTemplate = UtilXml.makeEmptyXmlDocument();
            return;
        }

        if (isParseForTemplate) {
            Element newElement = this.documentForTemplate.createElement(fullNameString);
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                CharSequence name = attributes.getLocalName(i);
                CharSequence value = attributes.getValue(i);

                if (UtilValidate.isEmpty(name)) {
                    name = attributes.getQName(i);
                }
                newElement.setAttribute(name.toString(), value.toString());
            }

            if (this.currentNodeForTemplate == null) {
                this.currentNodeForTemplate = newElement;
                this.rootNodeForTemplate = newElement;
            } else {
                this.currentNodeForTemplate.appendChild(newElement);
                this.currentNodeForTemplate = newElement;
            }
            return;
        }

        //Test if action change
        if (actionTags.contains(fullNameString)) {
            if ("create".equals(fullNameString)) setAction(Action.CREATE);
            if ("create-update".equals(fullNameString)) setAction(Action.CREATE_UPDATE);
            if ("create-replace".equals(fullNameString)) setAction(Action.CREATE_REPLACE);
            if ("delete".equals(fullNameString)) setAction(Action.DELETE);
            return;
        }

        if (currentValue != null) {
            // we have a nested value/CDATA element
            currentFieldName = fullNameString;
        } else {
            String entityName = fullNameString;

            // if a dash or colon is in the tag name, grab what is after it
            if (entityName.indexOf('-') > 0) {
                entityName = entityName.substring(entityName.indexOf('-') + 1);
            }
            if (entityName.indexOf(':') > 0) {
                entityName = entityName.substring(entityName.indexOf(':') + 1);
            }

            try {
                currentValue = delegator.makeValue(entityName);
                if (this.maintainTxStamps) {
                    currentValue.setIsFromEntitySync(true);
                }
            } catch (Exception e) {
                if (continueOnFail) {
                    Debug.logError(e, module);
                } else {
                    throw new SAXException(e);
                }
            }

            if (currentValue != null) {
                int length = attributes.getLength();
                List<String> absentFields = null;
                if (Action.CREATE_REPLACE == currentAction) {
                    //get all non pk fields
                    ModelEntity currentEntity = currentValue.getModelEntity();
                    absentFields = currentEntity.getNoPkFieldNames();
                    absentFields.removeAll(currentEntity.getAutomaticFieldNames());
                }

                for (int i = 0; i < length; i++) {
                    CharSequence name = attributes.getLocalName(i);
                    CharSequence value = attributes.getValue(i);
                    if (UtilValidate.isNotEmpty(value)) {
                        String tmp = FlexibleStringExpander.expandString(value.toString(), placeholderValues);
                        value = tmp.subSequence(0, tmp.length());
                    }
                    if (UtilValidate.isEmpty(name)) {
                        name = attributes.getQName(i);
                    }
                    try {
                        // treat empty strings as nulls, but do NOT ignore them, instead set as null and update
                        if (value != null) {
                            if (currentValue.getModelEntity().isField(name.toString())) {
                                String valueString = (value.length() > 0 ? value.toString() : null);
                                currentValue.setString(name.toString(), valueString);
                                if (Action.CREATE_REPLACE == currentAction && absentFields != null) absentFields.remove(name);
                            } else {
                                Debug.logWarning("Ignoring invalid field name [" + name + "] found for the entity: " + currentValue.getEntityName() + " with value=" + value, module);
                            }
                        }
                    } catch (Exception e) {
                        Debug.logWarning(e, "Could not set field " + entityName + "." + name + " to the value " + value, module);
                    }
                }
                if (Action.CREATE_REPLACE == currentAction && absentFields != null) {
                    for (String fieldName : absentFields) {
                        currentValue.set(fieldName, null);
                    }
                }
            }
        }
    }

    // ======== ErrorHandler interface implementation ========

    @Override
    public void error(org.xml.sax.SAXParseException exception) throws SAXException {
        Debug.logWarning(exception, "Error reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), module);
    }

    @Override
    public void fatalError(org.xml.sax.SAXParseException exception) throws SAXException {
        Debug.logError(exception, "Fatal Error reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), module);
        throw new SAXException("Fatal Error reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), exception);
    }

    @Override
    public void warning(org.xml.sax.SAXParseException exception) throws SAXException {
        Debug.logWarning(exception, "Warning reading XML on line " + exception.getLineNumber() + ", column " + exception.getColumnNumber(), module);
    }
}
