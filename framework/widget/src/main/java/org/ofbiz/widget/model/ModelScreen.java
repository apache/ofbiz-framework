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
package org.ofbiz.widget.model;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.renderer.ScreenRenderException;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.w3c.dom.Element;

/**
 * Widget Library - Screen model class
 */
@SuppressWarnings("serial")
public class ModelScreen extends ModelWidget {

    public static final String module = ModelScreen.class.getName();

    private final String sourceLocation;
    private final FlexibleStringExpander transactionTimeoutExdr;
    private final Map<String, ModelScreen> modelScreenMap;
    private final boolean useTransaction;
    private final boolean useCache;
    private final ModelScreenWidget.Section section;

    /** XML Constructor */
    public ModelScreen(Element screenElement, Map<String, ModelScreen> modelScreenMap, String sourceLocation) {
        super(screenElement);
        this.sourceLocation = sourceLocation;
        this.transactionTimeoutExdr = FlexibleStringExpander.getInstance(screenElement.getAttribute("transaction-timeout"));
        this.modelScreenMap = modelScreenMap;
        this.useTransaction = "true".equals(screenElement.getAttribute("use-transaction"));
        this.useCache = "true".equals(screenElement.getAttribute("use-cache"));

        // read in the section, which will read all sub-widgets too
        Element sectionElement = UtilXml.firstChildElement(screenElement, "section");
        if (sectionElement == null) {
            throw new IllegalArgumentException("No section found for the screen definition with name: " + getName());
        }
        this.section = new ModelScreenWidget.Section(this, sectionElement, true);
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    public String getTransactionTimeout() {
        return transactionTimeoutExdr.getOriginal();
    }

    public Map<String, ModelScreen> getModelScreenMap() {
        return modelScreenMap;
    }

    public boolean getUseTransaction() {
        return useTransaction;
    }

    public boolean getUseCache() {
        return useCache;
    }

    public ModelScreenWidget.Section getSection() {
        return section;
    }

    public String getSourceLocation() {
        return sourceLocation;
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
    public void renderScreenString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws ScreenRenderException {
        // make sure the "nullField" object is in there for entity ops
        context.put("nullField", GenericEntity.NULL_FIELD);

        // wrap the whole screen rendering in a transaction, should improve performance in querying and such
        Map<String, String> parameters = UtilGenerics.cast(context.get("parameters"));
        boolean beganTransaction = false;
        int transactionTimeout = -1;
        if (parameters != null) {
            String transactionTimeoutPar = parameters.get("TRANSACTION_TIMEOUT");
            if (transactionTimeoutPar != null) {
                try {
                    transactionTimeout = Integer.parseInt(transactionTimeoutPar);
                } catch (NumberFormatException nfe) {
                    String msg = "TRANSACTION_TIMEOUT parameter for screen [" + this.sourceLocation + "#" + getName() + "] is invalid and it will be ignored: " + nfe.toString();
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
            if (useTransaction) {
                if (transactionTimeout < 0) {
                    beganTransaction = TransactionUtil.begin();
                }
                if (transactionTimeout > 0) {
                    beganTransaction = TransactionUtil.begin(transactionTimeout);
                }
            }

            // render the screen, starting with the top-level section
            this.section.renderWidgetString(writer, context, screenStringRenderer);
            TransactionUtil.commit(beganTransaction);
        } catch (Exception e) {
            String errMsg = "Error rendering screen [" + this.sourceLocation + "#" + getName() + "]: " + e.toString();
            Debug.logError(errMsg + ". Rolling back transaction.", module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
            }

            // throw nested exception, don't need to log details here: Debug.logError(e, errMsg, module);

            // after rolling back, rethrow the exception
            throw new ScreenRenderException(errMsg, e);
        }
    }

    public LocalDispatcher getDispatcher(Map<String, Object> context) {
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        return dispatcher;
    }

    public Delegator getDelegator(Map<String, Object> context) {
        Delegator delegator = (Delegator) context.get("delegator");
        return delegator;
    }
}


