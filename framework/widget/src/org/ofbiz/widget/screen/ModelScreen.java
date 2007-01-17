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
package org.ofbiz.widget.screen;

import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

/**
 * Widget Library - Screen model class
 */
public class ModelScreen implements Serializable {

    public static final String module = ModelScreen.class.getName();

    protected String name;
    protected String sourceLocation;
    protected FlexibleStringExpander transactionTimeoutExdr;
    protected Map modelScreenMap;
    
    protected ModelScreenWidget.Section section;

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    protected ModelScreen() {}

    /** XML Constructor */
    public ModelScreen(Element screenElement, Map modelScreenMap, String sourceLocation) {
        this.sourceLocation = sourceLocation;
        this.name = screenElement.getAttribute("name");
        this.transactionTimeoutExdr = new FlexibleStringExpander(screenElement.getAttribute("transaction-timeout"));
        this.modelScreenMap = modelScreenMap;

        // read in the section, which will read all sub-widgets too
        Element sectionElement = UtilXml.firstChildElement(screenElement, "section");
        if (sectionElement == null) {
            throw new IllegalArgumentException("No section found for the screen definition with name: " + this.name);
        }
        this.section = new ModelScreenWidget.Section(this, sectionElement);
    }

    /**
     * Renders this screen to a String, i.e. in a text format, as defined with the
     * ScreenStringRenderer implementation.
     *
     * @param writer The Writer that the screen text will be written to
     * @param context Map containing the screen context; the following are
     *   reserved words in this context:
     *    - parameters (contains any special initial parameters coming in)
     *    - userLogin (if a user is logged in)
     *    - autoUserLogin (if a user is automatically logged in, ie no password has been entered)
     *    - formStringRenderer
     *    - request, response, session, application (special case, only in HTML contexts, etc) 
     *    - delegator, dispatcher, security
     *    - null (represents a null field value for entity operations)
     *    - sections (used for decorators to reference the sections to be decorated and render them) 
     * @param screenStringRenderer An implementation of the ScreenStringRenderer
     *   interface that is responsible for the actual text generation for
     *   different screen elements; implementing your own makes it possible to
     *   use the same screen definitions for many types of screen UIs
     */
    public void renderScreenString(Writer writer, Map context, ScreenStringRenderer screenStringRenderer) throws GeneralException {
        // make sure the "null" object is in there for entity ops
        context.put("null", GenericEntity.NULL_FIELD);

        // wrap the whole screen rendering in a transaction, should improve performance in querying and such
        boolean beganTransaction = false;
        Map parameters = (Map) context.get("parameters");
        int transactionTimeout = -1;
        if (parameters != null) {
            String transactionTimeoutPar = (String) parameters.get("TRANSACTION_TIMEOUT");
            if (transactionTimeoutPar != null) {
                try {
                    transactionTimeout = Integer.parseInt(transactionTimeoutPar);
                } catch(NumberFormatException nfe) {
                    String msg = "TRANSACTION_TIMEOUT parameter for screen [" + this.sourceLocation + "#" + this.name + "] is invalid and it will be ignored: " + nfe.toString();
                    Debug.logWarning(msg, module);
                }
            }
        }
        
        if (transactionTimeout < 0 && !transactionTimeoutExdr.isEmpty()) {
            // no TRANSACTION_TIMEOUT parameter, check screen attribute
            String transactionTimeoutStr = transactionTimeoutExdr.expandString(context);
            if (UtilValidate.isNotEmpty(transactionTimeoutStr)) {
                try {
                    transactionTimeout = Integer.parseInt(transactionTimeoutStr);
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, "Could not parse transaction-timeout value, original=[" + transactionTimeoutExdr + "], expanded=[" + transactionTimeoutStr + "]", module);
                }
            }
        }
        
        try {
            // If transaction timeout is not present (i.e. is equal to -1), the default transaction timeout is used
            // If transaction timeout is present, use it to start the transaction
            // If transaction timeout is set to zero, no transaction is started
            if (transactionTimeout < 0) {
                beganTransaction = TransactionUtil.begin();
            }
            if (transactionTimeout > 0) {
                beganTransaction = TransactionUtil.begin(transactionTimeout);
            }

            // render the screen, starting with the top-level section
            this.section.renderWidgetString(writer, context, screenStringRenderer);
        } catch (RuntimeException e) {
            String errMsg = "Error rendering screen [" + this.sourceLocation + "#" + this.name + "]: " + e.toString();
            Debug.logError(errMsg + ". Rolling back transaction.", module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw new GeneralException(errMsg, e);
        } catch (Exception e) {
            String errMsg = "Error rendering screen [" + this.sourceLocation + "#" + this.name + "]: " + e.toString();
            Debug.logError(errMsg + ". Rolling back transaction.", module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }
            
            // throw nested exception, don't need to log details here: Debug.logError(e, errMsg, module);
            
            // after rolling back, rethrow the exception
            throw new GeneralException(errMsg, e);
        } finally {
            // only commit the transaction if we started one... this will throw an exception if it fails
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not commit transaction: " + e2.toString(), module);
            }
        }
    }

    public LocalDispatcher getDispatcher(Map context) {
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        return dispatcher;
    }

    public GenericDelegator getDelegator(Map context) {
        GenericDelegator delegator = (GenericDelegator) context.get("delegator");
        return delegator;
    }
    
    public String getName() {
        return name;
    }
}

