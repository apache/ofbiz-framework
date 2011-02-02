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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.ConfigXMLReader;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.ModelWidgetAction;
import org.w3c.dom.Element;

/**
 * Widget Library - Screen model class
 */
@SuppressWarnings("serial")
public class ModelScreen extends ModelWidget {

    public static final String module = ModelScreen.class.getName();

    protected String sourceLocation;
    protected FlexibleStringExpander transactionTimeoutExdr;
    protected Map<String, ModelScreen> modelScreenMap;
    protected boolean useTransaction;
    protected boolean useCache;

    protected ModelScreenWidget.Section section;

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    protected ModelScreen() {}

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
            throw new IllegalArgumentException("No section found for the screen definition with name: " + this.name);
        }
        this.section = new ModelScreenWidget.Section(this, sectionElement);
        this.section.isMainSection = true;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public Set<String> getAllServiceNamesUsed() {
        Set<String> allServiceNamesUsed = FastSet.newInstance();
        findServiceNamesUsedInWidget(this.section, allServiceNamesUsed);
        return allServiceNamesUsed;
    }

    protected static void findServiceNamesUsedInWidget(ModelScreenWidget currentWidget, Set<String> allServiceNamesUsed) {
        if (currentWidget instanceof ModelScreenWidget.Section) {
            List<ModelWidgetAction> actions = ((ModelScreenWidget.Section)currentWidget).actions;
            List<ModelScreenWidget> subWidgets = ((ModelScreenWidget.Section)currentWidget).subWidgets;
            List<ModelScreenWidget> failWidgets = ((ModelScreenWidget.Section)currentWidget).failWidgets;
            if (actions != null) {
                for (ModelWidgetAction screenOperation: actions) {
                    if (screenOperation instanceof ModelWidgetAction.Service) {
                        String serviceName = ((ModelWidgetAction.Service) screenOperation).getServiceNameExdr().getOriginal();
                        if (UtilValidate.isNotEmpty(serviceName)) allServiceNamesUsed.add(serviceName);
                    }
                }
            }
            if (subWidgets != null) {
                for (ModelScreenWidget widget: subWidgets) {
                    findServiceNamesUsedInWidget(widget, allServiceNamesUsed);
                }
            }
            if (failWidgets != null) {
                for (ModelScreenWidget widget: failWidgets) {
                    findServiceNamesUsedInWidget(widget, allServiceNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorSection) {
            ModelScreenWidget.DecoratorSection decoratorSection = (ModelScreenWidget.DecoratorSection)currentWidget;
            if (decoratorSection.subWidgets != null) {
                for (ModelScreenWidget widget: decoratorSection.subWidgets) {
                    findServiceNamesUsedInWidget(widget, allServiceNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorScreen) {
            ModelScreenWidget.DecoratorScreen decoratorScreen = (ModelScreenWidget.DecoratorScreen)currentWidget;
            if (decoratorScreen.sectionMap != null) {
                Collection<ModelScreenWidget.DecoratorSection> sections = decoratorScreen.sectionMap.values();
                for (ModelScreenWidget section: sections) {
                    findServiceNamesUsedInWidget(section, allServiceNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Container) {
            ModelScreenWidget.Container container = (ModelScreenWidget.Container)currentWidget;
            if (container.subWidgets != null) {
                for (ModelScreenWidget widget: container.subWidgets) {
                    findServiceNamesUsedInWidget(widget, allServiceNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Screenlet) {
            ModelScreenWidget.Screenlet screenlet = (ModelScreenWidget.Screenlet)currentWidget;
            if (screenlet.subWidgets != null) {
                for (ModelScreenWidget widget: screenlet.subWidgets) {
                    findServiceNamesUsedInWidget(widget, allServiceNamesUsed);
                }
            }
        }
    }
    public Set<String> getAllEntityNamesUsed() {
        Set<String> allEntityNamesUsed = FastSet.newInstance();
        findEntityNamesUsedInWidget(this.section, allEntityNamesUsed);
        return allEntityNamesUsed;
    }
    protected static void findEntityNamesUsedInWidget(ModelScreenWidget currentWidget, Set<String> allEntityNamesUsed) {
        if (currentWidget instanceof ModelScreenWidget.Section) {
            List<ModelWidgetAction> actions = ((ModelScreenWidget.Section)currentWidget).actions;
            List<ModelScreenWidget> subWidgets = ((ModelScreenWidget.Section)currentWidget).subWidgets;
            List<ModelScreenWidget> failWidgets = ((ModelScreenWidget.Section)currentWidget).failWidgets;
            if (actions != null) {
                for (ModelWidgetAction screenOperation: actions) {
                    if (screenOperation instanceof ModelWidgetAction.EntityOne) {
                        String entName = ((ModelWidgetAction.EntityOne) screenOperation).getFinder().getEntityName();
                        if (UtilValidate.isNotEmpty(entName)) allEntityNamesUsed.add(entName);
                    } else if (screenOperation instanceof ModelWidgetAction.EntityAnd) {
                        String entName = ((ModelWidgetAction.EntityAnd) screenOperation).getFinder().getEntityName();
                        if (UtilValidate.isNotEmpty(entName)) allEntityNamesUsed.add(entName);
                    } else if (screenOperation instanceof ModelWidgetAction.EntityCondition) {
                        String entName = ((ModelWidgetAction.EntityCondition) screenOperation).getFinder().getEntityName();
                        if (UtilValidate.isNotEmpty(entName)) allEntityNamesUsed.add(entName);
                    } else if (screenOperation instanceof ModelWidgetAction.GetRelated) {
                        String relationName = ((ModelWidgetAction.GetRelated) screenOperation).getRelationName();
                        if (UtilValidate.isNotEmpty(relationName)) allEntityNamesUsed.add(relationName);
                    } else if (screenOperation instanceof ModelWidgetAction.GetRelatedOne) {
                        String relationName = ((ModelWidgetAction.GetRelatedOne) screenOperation).getRelationName();
                        if (UtilValidate.isNotEmpty(relationName)) allEntityNamesUsed.add(relationName);
                    }
                }
            }
            if (subWidgets != null) {
                for (ModelScreenWidget widget: subWidgets) {
                    findEntityNamesUsedInWidget(widget, allEntityNamesUsed);
                }
            }
            if (failWidgets != null) {
                for (ModelScreenWidget widget: failWidgets) {
                    findEntityNamesUsedInWidget(widget, allEntityNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorSection) {
            ModelScreenWidget.DecoratorSection decoratorSection = (ModelScreenWidget.DecoratorSection)currentWidget;
            if (decoratorSection.subWidgets != null) {
                for (ModelScreenWidget widget: decoratorSection.subWidgets) {
                    findEntityNamesUsedInWidget(widget, allEntityNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorScreen) {
            ModelScreenWidget.DecoratorScreen decoratorScreen = (ModelScreenWidget.DecoratorScreen)currentWidget;
            if (decoratorScreen.sectionMap != null) {
                Collection<ModelScreenWidget.DecoratorSection> sections = decoratorScreen.sectionMap.values();
                for (ModelScreenWidget section: sections) {
                    findEntityNamesUsedInWidget(section, allEntityNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Container) {
            ModelScreenWidget.Container container = (ModelScreenWidget.Container)currentWidget;
            if (container.subWidgets != null) {
                for (ModelScreenWidget widget: container.subWidgets) {
                    findEntityNamesUsedInWidget(widget, allEntityNamesUsed);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Screenlet) {
            ModelScreenWidget.Screenlet screenlet = (ModelScreenWidget.Screenlet)currentWidget;
            if (screenlet.subWidgets != null) {
                for (ModelScreenWidget widget: screenlet.subWidgets) {
                    findEntityNamesUsedInWidget(widget, allEntityNamesUsed);
                }
            }
        }
    }
    public Set<String> getAllFormNamesIncluded() {
        Set<String> allFormNamesIncluded = FastSet.newInstance();
        findFormNamesIncludedInWidget(this.section, allFormNamesIncluded);
        return allFormNamesIncluded;
    }
    protected static void findFormNamesIncludedInWidget(ModelScreenWidget currentWidget, Set<String> allFormNamesIncluded) {
        if (currentWidget instanceof ModelScreenWidget.Form) {
            ModelScreenWidget.Form form = (ModelScreenWidget.Form) currentWidget;
            allFormNamesIncluded.add(form.locationExdr.getOriginal() + "#" + form.nameExdr.getOriginal());
        } else if (currentWidget instanceof ModelScreenWidget.Section) {
            ModelScreenWidget.Section section = (ModelScreenWidget.Section) currentWidget;
            if (section.subWidgets != null) {
                for (ModelScreenWidget widget: section.subWidgets) {
                    findFormNamesIncludedInWidget(widget, allFormNamesIncluded);
                }
            }
            if (section.failWidgets != null) {
                for (ModelScreenWidget widget: section.failWidgets) {
                    findFormNamesIncludedInWidget(widget, allFormNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorSection) {
            ModelScreenWidget.DecoratorSection decoratorSection = (ModelScreenWidget.DecoratorSection) currentWidget;
            if (decoratorSection.subWidgets != null) {
                for (ModelScreenWidget widget: decoratorSection.subWidgets) {
                    findFormNamesIncludedInWidget(widget, allFormNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorScreen) {
            ModelScreenWidget.DecoratorScreen decoratorScreen = (ModelScreenWidget.DecoratorScreen) currentWidget;
            if (decoratorScreen.sectionMap != null) {
                Collection<ModelScreenWidget.DecoratorSection> sections = decoratorScreen.sectionMap.values();
                for (ModelScreenWidget section: sections) {
                    findFormNamesIncludedInWidget(section, allFormNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Container) {
            ModelScreenWidget.Container container = (ModelScreenWidget.Container) currentWidget;
            if (container.subWidgets != null) {
                for (ModelScreenWidget widget: container.subWidgets) {
                    findFormNamesIncludedInWidget(widget, allFormNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Screenlet) {
            ModelScreenWidget.Screenlet screenlet = (ModelScreenWidget.Screenlet) currentWidget;
            if (screenlet.subWidgets != null) {
                for (ModelScreenWidget widget: screenlet.subWidgets) {
                    findFormNamesIncludedInWidget(widget, allFormNamesIncluded);
                }
            }
        }
    }

    public Set<String> getAllRequestsLocationAndUri() throws GeneralException {
        Set<String> allRequestNamesIncluded = FastSet.newInstance();
        findRequestNamesLinkedtoInWidget(this.section, allRequestNamesIncluded);
        return allRequestNamesIncluded;
    }
    protected static void findRequestNamesLinkedtoInWidget(ModelScreenWidget currentWidget, Set<String> allRequestNamesIncluded) throws GeneralException {
        if (currentWidget instanceof ModelScreenWidget.Link) {
            ModelScreenWidget.Link link = (ModelScreenWidget.Link) currentWidget;
            String target = link.getTarget(null);
            String urlMode = link.getUrlMode();
            // Debug.logInfo("In findRequestNamesLinkedtoInWidget found link [" + link.rawString() + "] with target [" + target + "]", module);

            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerRequestUniqueForTargetType(target, urlMode);
            if (controllerLocAndRequestSet == null) return;
            allRequestNamesIncluded.addAll(controllerLocAndRequestSet);
        } else if (currentWidget instanceof ModelScreenWidget.Section) {
            ModelScreenWidget.Section section = (ModelScreenWidget.Section) currentWidget;
            if (section.subWidgets != null) {
                for (ModelScreenWidget widget: section.subWidgets) {
                    findRequestNamesLinkedtoInWidget(widget, allRequestNamesIncluded);
                }
            }
            if (section.failWidgets != null) {
                for (ModelScreenWidget widget: section.failWidgets) {
                    findRequestNamesLinkedtoInWidget(widget, allRequestNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorSection) {
            ModelScreenWidget.DecoratorSection decoratorSection = (ModelScreenWidget.DecoratorSection) currentWidget;
            if (decoratorSection.subWidgets != null) {
                for (ModelScreenWidget widget: decoratorSection.subWidgets) {
                    findRequestNamesLinkedtoInWidget(widget, allRequestNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.DecoratorScreen) {
            ModelScreenWidget.DecoratorScreen decoratorScreen = (ModelScreenWidget.DecoratorScreen) currentWidget;
            if (decoratorScreen.sectionMap != null) {
                Collection<ModelScreenWidget.DecoratorSection> sections = decoratorScreen.sectionMap.values();
                for (ModelScreenWidget section: sections) {
                    findRequestNamesLinkedtoInWidget(section, allRequestNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Container) {
            ModelScreenWidget.Container container = (ModelScreenWidget.Container) currentWidget;
            if (container.subWidgets != null) {
                for (ModelScreenWidget widget: container.subWidgets) {
                    findRequestNamesLinkedtoInWidget(widget, allRequestNamesIncluded);
                }
            }
        } else if (currentWidget instanceof ModelScreenWidget.Screenlet) {
            ModelScreenWidget.Screenlet screenlet = (ModelScreenWidget.Screenlet) currentWidget;
            if (screenlet.subWidgets != null) {
                for (ModelScreenWidget widget: screenlet.subWidgets) {
                    findRequestNamesLinkedtoInWidget(widget, allRequestNamesIncluded);
                }
            }
        }
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
     *    - delegator, dispatcher, authz, security
     *    - null (represents a null field value for entity operations)
     *    - sections (used for decorators to reference the sections to be decorated and render them)
     * @param screenStringRenderer An implementation of the ScreenStringRenderer
     *   interface that is responsible for the actual text generation for
     *   different screen elements; implementing your own makes it possible to
     *   use the same screen definitions for many types of screen UIs
     */
    public void renderScreenString(Appendable writer, Map<String, Object> context, ScreenStringRenderer screenStringRenderer) throws ScreenRenderException {
        // make sure the "null" object is in there for entity ops
        context.put("null", GenericEntity.NULL_FIELD);

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
        } catch (ScreenRenderException e) {
            throw e;
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
            throw new ScreenRenderException(errMsg, e);
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
            throw new ScreenRenderException(errMsg, e);
        } finally {
            // only commit the transaction if we started one... this will throw an exception if it fails
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "Could not commit transaction: " + e2.toString(), module);
            }
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


