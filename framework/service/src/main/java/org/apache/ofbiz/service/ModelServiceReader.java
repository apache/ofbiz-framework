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
package org.apache.ofbiz.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.metrics.MetricsFactory;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.service.group.GroupModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Generic Service - Service Definition Reader
 */
@SuppressWarnings("serial")
public final class ModelServiceReader implements Serializable {

    private static final String MODULE = ModelServiceReader.class.getName();

    /** is either from a URL or from a ResourceLoader (through the ResourceHandler) */
    private boolean isFromURL;
    private URL readerURL = null;
    private ResourceHandler handler = null;
    private Delegator delegator = null;

    public static Map<String, ModelService> getModelServiceMap(URL readerURL, Delegator delegator) {
        if (readerURL == null) {
            Debug.logError("Cannot add reader with a null reader URL", MODULE);
            return null;
        }

        ModelServiceReader reader = new ModelServiceReader(true, readerURL, null, delegator);
        return reader.getModelServices();
    }

    public static Map<String, ModelService> getModelServiceMap(ResourceHandler handler, Delegator delegator) {
        ModelServiceReader reader = new ModelServiceReader(false, null, handler, delegator);
        return reader.getModelServices();
    }

    private ModelServiceReader(boolean isFromURL, URL readerURL, ResourceHandler handler, Delegator delegator) {
        this.isFromURL = isFromURL;
        this.readerURL = readerURL;
        this.handler = handler;
        this.delegator = delegator;
    }

    private Map<String, ModelService> getModelServices() {
        UtilTimer utilTimer = new UtilTimer();
        Document document;
        if (this.isFromURL) {
            document = getDocument(readerURL);

            if (document == null) {
                return null;
            }
        } else {
            try {
                document = handler.getDocument();
            } catch (GenericConfigException e) {
                Debug.logError(e, "Error getting XML document from resource", MODULE);
                return null;
            }
        }

        Map<String, ModelService> modelServices = new HashMap<>();

        Element docElement = document.getDocumentElement();
        if (docElement == null) {
            return null;
        }

        docElement.normalize();

        String resourceLocation = handler.getLocation();
        try {
            resourceLocation = handler.getURL().toExternalForm();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Could not get resource URL", MODULE);
        }

        int i = 0;
        Node curChild = docElement.getFirstChild();
        if (curChild != null) {
            if (this.isFromURL) {
                utilTimer.timerString("Before start of service loop in file " + readerURL);
            } else {
                utilTimer.timerString("Before start of service loop in " + handler);
            }

            do {
                if (curChild.getNodeType() == Node.ELEMENT_NODE && "service".equals(curChild.getNodeName())) {
                    i++;
                    Element curServiceElement = (Element) curChild;
                    String serviceName = UtilXml.checkEmpty(curServiceElement.getAttribute("name"));

                    // check to see if service with same name has already been read
                    if (modelServices.containsKey(serviceName)) {
                        Debug.logWarning("Service " + serviceName + " is defined more than once, "
                                + "most recent will over-write previous definition(s)", MODULE);
                    }
                    ModelService service = createModelService(curServiceElement, resourceLocation);

                    modelServices.put(serviceName, service);
                }
                curChild = curChild.getNextSibling();
            } while (curChild != null);
        } else {
            Debug.logWarning("No child nodes found.", MODULE);
        }
        if (this.isFromURL) {
            utilTimer.timerString("Finished file " + readerURL + " - Total Services: " + i + " FINISHED");
            Debug.logInfo("Loaded [" + i + "] Services from " + readerURL, MODULE);
        } else {
            utilTimer.timerString("Finished document in " + handler + " - Total Services: " + i + " FINISHED");
            Debug.logInfo("Loaded [" + i + "] Services from " + resourceLocation, MODULE);
        }
        return modelServices;
    }

    private ModelService createModelService(Element serviceElement, String resourceLocation) {
        ModelService service = new ModelService();

        service.setName(UtilXml.checkEmpty(serviceElement.getAttribute("name")).intern());
        service.setDefinitionLocation(resourceLocation);
        service.setEngineName(UtilXml.checkEmpty(serviceElement.getAttribute("engine")).intern());
        service.setLocation(UtilXml.checkEmpty(serviceElement.getAttribute("location")).intern());
        service.setInvoke(UtilXml.checkEmpty(serviceElement.getAttribute("invoke")).intern());
        service.setSemaphore(UtilXml.checkEmpty(serviceElement.getAttribute("semaphore")).intern());
        service.setDefaultEntityName(UtilXml.checkEmpty(serviceElement.getAttribute("default-entity-name")).intern());
        service.setFromLoader(isFromURL ? readerURL.toExternalForm() : handler.getLoaderName());
        service.setAction(UtilXml.checkEmpty(serviceElement.getAttribute("action")).intern());

        // these default to true; if anything but true, make false
        service.setAuth("true".equalsIgnoreCase(serviceElement.getAttribute("auth")));
        service.setExport("true".equalsIgnoreCase(serviceElement.getAttribute("export")));
        service.setDebug("true".equalsIgnoreCase(serviceElement.getAttribute("debug")));

        // these defaults to false; if anything but false, make it true
        service.setValidate(!"false".equalsIgnoreCase(serviceElement.getAttribute("validate")));
        service.setUseTransaction(!"false".equalsIgnoreCase(serviceElement.getAttribute("use-transaction")));
        service.setRequireNewTransaction(!"false".equalsIgnoreCase(serviceElement.getAttribute("require-new-transaction")));
        if (service.isRequireNewTransaction() && !service.isUseTransaction()) {
            // requireNewTransaction implies that a transaction is used
            service.setUseTransaction(true);
            Debug.logWarning("In service definition [" + service.getName() + "] the value use-transaction has been changed from false to true as"
                    + "required when require-new-transaction is set to true", MODULE);
        }
        service.setHideResultInLog(!"false".equalsIgnoreCase(serviceElement.getAttribute("hideResultInLog")));

        // set the semaphore sleep/wait times
        String semaphoreWaitStr = UtilXml.checkEmpty(serviceElement.getAttribute("semaphore-wait-seconds"));
        int semaphoreWait = 300;
        if (UtilValidate.isNotEmpty(semaphoreWaitStr)) {
            try {
                semaphoreWait = Integer.parseInt(semaphoreWaitStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting semaphore-wait to 5 minutes (default)", MODULE);
                semaphoreWait = 300;
            }
        }
        service.setSemaphoreWait(semaphoreWait);

        String semaphoreSleepStr = UtilXml.checkEmpty(serviceElement.getAttribute("semaphore-sleep"));
        int semaphoreSleep = 500;
        if (UtilValidate.isNotEmpty(semaphoreSleepStr)) {
            try {
                semaphoreSleep = Integer.parseInt(semaphoreSleepStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting semaphore-sleep to 1/2 second (default)", MODULE);
                semaphoreSleep = 500;
            }
        }
        service.setSemaphoreSleep(semaphoreSleep);

        // set the max retry field
        String maxRetryStr = UtilXml.checkEmpty(serviceElement.getAttribute("max-retry"));
        int maxRetry = 0;
        if (UtilValidate.isNotEmpty(maxRetryStr)) {
            try {
                maxRetry = Integer.parseInt(maxRetryStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting maxRetry to 0 (default)", MODULE);
                maxRetry = 0;
            }
        }
        service.setMaxRetry(maxRetry);

        // get the timeout and convert to int
        String timeoutStr = UtilXml.checkEmpty(serviceElement.getAttribute("transaction-timeout"), serviceElement.getAttribute("transaction-timout"));
        int timeout = 0;
        if (UtilValidate.isNotEmpty(timeoutStr)) {
            try {
                timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting timeout to 0 (default)", MODULE);
                timeout = 0;
            }
        }
        service.setTransactionTimeout(timeout);

        service.setDescription(getCDATADef(serviceElement, "description"));
        service.setNameSpace(getCDATADef(serviceElement, "namespace"));

        // construct the context
        service.setContextInfo(new HashMap<>());
        createNotification(serviceElement, service);
        createAloneServicePermission(serviceElement, service);
        createPermGroups(serviceElement, service);
        createGroupDefs(serviceElement, service);
        createImplDefs(serviceElement, service);
        this.createAutoAttrDefs(serviceElement, service);
        createAttrDefs(serviceElement, service);
        createOverrideDefs(serviceElement, service);
        createDeprecated(serviceElement, service);
        // Get metrics.
        Element metricsElement = UtilXml.firstChildElement(serviceElement, "metric");
        if (metricsElement != null) {
            service.setMetrics(MetricsFactory.getInstance(metricsElement));
        }
        return service;
    }

    private static String getCDATADef(Element baseElement, String tagName) {
        String value = "";
        NodeList nl = baseElement.getElementsByTagName(tagName);

        // if there are more then one decriptions we will use only the first one
        if (nl.getLength() > 0) {
            Node n = nl.item(0);
            NodeList childNodes = n.getChildNodes();

            if (childNodes.getLength() > 0) {
                Node cdata = childNodes.item(0);

                value = UtilXml.checkEmpty(cdata.getNodeValue());
            }
        }
        return value;
    }

    private static void createNotification(Element baseElement, ModelService model) {
        List<? extends Element> n = UtilXml.childElementList(baseElement, "notification");
        // default notification groups
        ModelNotification nSuccess = new ModelNotification();
        nSuccess.setNotificationEvent("success");
        nSuccess.setNotificationGroupName("default.success." + model.getFromLoader());
        model.getNotifications().add(nSuccess);

        ModelNotification nFail = new ModelNotification();
        nFail.setNotificationEvent("fail");
        nFail.setNotificationGroupName("default.fail." + model.getFromLoader());
        model.getNotifications().add(nFail);

        ModelNotification nError = new ModelNotification();
        nError.setNotificationEvent("error");
        nError.setNotificationGroupName("default.error." + model.getFromLoader());
        model.getNotifications().add(nError);

        if (n != null) {
            for (Element e: n) {
                ModelNotification notify = new ModelNotification();
                notify.setNotificationEvent(e.getAttribute("event"));
                notify.setNotificationGroupName(e.getAttribute("group"));
                model.getNotifications().add(notify);
            }
        }
    }

    private static ModelPermission createServicePermission(Element e, ModelService model) {
        ModelPermission modelPermission = new ModelPermission();
        modelPermission.setPermissionType(ModelPermission.getPermissionService());
        modelPermission.setPermissionServiceName(e.getAttribute("service-name"));
        modelPermission.setPermissionMainAction(e.getAttribute("main-action"));
        modelPermission.setPermissionResourceDesc(e.getAttribute("resource-description"));
        modelPermission.setPermissionRequireNewTransaction(!"false".equalsIgnoreCase(e.getAttribute("require-new-transaction")));
        modelPermission.setServiceModel(model);
        return modelPermission;
    }

    private static void createAloneServicePermission(Element baseElement, ModelService model) {
        Element e = UtilXml.firstChildElement(baseElement, "permission-service");
        if (e != null) {
            ModelPermission modelPermission = createServicePermission(e, model);
            model.setModelPermission(modelPermission);

            // auth is always required when permissions are set
            model.setAuth(true);
        }
    }

    private static void createPermGroups(Element baseElement, ModelService model) {
        for (Element element: UtilXml.childElementList(baseElement, "required-permissions")) {
            ModelPermGroup group = new ModelPermGroup();
            group.setJoinType(element.getAttribute("join-type"));
            createGroupPermissions(element, group, model);
            model.getPermissionGroups().add(group);
        }
    }

    private static void createGroupPermissions(Element baseElement, ModelPermGroup group, ModelService service) {
        // create the simple permissions
        for (Element element: UtilXml.childElementList(baseElement, "check-permission")) {
            ModelPermission perm = new ModelPermission();
            perm.setNameOrRole(element.getAttribute("permission").intern());
            perm.setAction(element.getAttribute("action").intern());
            if (UtilValidate.isNotEmpty(perm.getAction())) {
                perm.setPermissionType(ModelPermission.getEntityPermission());
            } else {
                perm.setPermissionType(ModelPermission.getPERMISSION());
            }
            perm.setServiceModel(service);
            group.getPermissions().add(perm);
        }

        // Create the permissions based on permission services
        for (Element element : UtilXml.childElementList(baseElement, "permission-service")) {
            group.getPermissions().add(createServicePermission(element, service));
        }
        if (UtilValidate.isNotEmpty(group.getPermissions())) {
            // auth is always required when permissions are set
            service.setAuth(true);
        }
    }

    private static void createGroupDefs(Element baseElement, ModelService service) {
        List<? extends Element> group = UtilXml.childElementList(baseElement, "group");
        if (UtilValidate.isNotEmpty(group)) {
            Element groupElement = group.get(0);
            groupElement.setAttribute("name", "_" + service.getName() + ".group");
            service.setInternalGroup(new GroupModel(groupElement));
            service.setInvoke(service.getInternalGroup().getGroupName());
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created INTERNAL GROUP model [" + service.getInternalGroup() + "]", MODULE);
            }
        }
    }

    private static void createImplDefs(Element baseElement, ModelService service) {
        for (Element implement: UtilXml.childElementList(baseElement, "implements")) {
            String serviceName = UtilXml.checkEmpty(implement.getAttribute("service")).intern();
            boolean optional = UtilXml.checkBoolean(implement.getAttribute("optional"), false);
            if (!serviceName.isEmpty()) {
                service.getImplServices().add(new ModelServiceIface(serviceName, optional));
            }
        }
    }

    private void createAutoAttrDefs(Element baseElement, ModelService service) {
        for (Element element: UtilXml.childElementList(baseElement, "auto-attributes")) {
            createAutoAttrDef(element, service);
        }
    }

    private void createAutoAttrDef(Element autoElement, ModelService service) {
        // get the entity name; first from the auto-attributes then from the service def
        String entityName = UtilXml.checkEmpty(autoElement.getAttribute("entity-name"));
        if (UtilValidate.isEmpty(entityName)) {
            entityName = service.getDefaultEntityName();
            if (UtilValidate.isEmpty(entityName)) {
                Debug.logWarning("Auto-Attribute does not specify an entity-name; not default-entity on service definition", MODULE);
            }
        }

        // get the include type 'pk|nonpk|all'
        String includeType = UtilXml.checkEmpty(autoElement.getAttribute("include"));
        boolean includePk = "pk".equals(includeType) || "all".equals(includeType);
        boolean includeNonPk = "nonpk".equals(includeType) || "all".equals(includeType);

        if (delegator == null) {
            Debug.logWarning("Cannot use auto-attribute fields with a null delegator", MODULE);
        }

        if (delegator != null && entityName != null) {
            Map<String, ModelParam> modelParamMap = new LinkedHashMap<>();
            try {
                ModelEntity entity = delegator.getModelEntity(entityName);
                if (entity == null) {
                    throw new GeneralException("Could not find entity with name [" + entityName + "]");
                }
                Iterator<ModelField> fieldsIter = entity.getFieldsIterator();
                while (fieldsIter.hasNext()) {
                    ModelField field = fieldsIter.next();
                    if ((!field.getIsAutoCreatedInternal()) && ((field.getIsPk() && includePk) || (!field.getIsPk() && includeNonPk))) {
                        ModelFieldType fieldType = delegator.getEntityFieldType(entity, field.getType());
                        if (fieldType == null) {
                            throw new GeneralException("Null field type from delegator for entity [" + entityName + "]");
                        }
                        ModelParam param = new ModelParam();
                        param.setEntityName(entityName);
                        param.setFieldName(field.getName());
                        param.setName(field.getName());
                        param.setType(fieldType.getJavaType());
                        // this is a special case where we use something different in the service layer than we do in the entity/data layer
                        if ("java.sql.Blob".equals(param.getType())) {
                            param.setType("java.nio.ByteBuffer");
                        }
                        param.setMode(UtilXml.checkEmpty(autoElement.getAttribute("mode")).intern());
                        param.setOptional("true".equalsIgnoreCase(autoElement.getAttribute("optional"))); // default to true
                        param.setFormDisplay(!"false".equalsIgnoreCase(autoElement.getAttribute("form-display"))); // default to false
                        param.setAllowHtml(UtilXml.checkEmpty(autoElement.getAttribute("allow-html"), "none").intern()); // default to none
                        modelParamMap.put(field.getName(), param);
                    }
                }

                // get the excludes list; and remove those from the map
                List<? extends Element> excludes = UtilXml.childElementList(autoElement, "exclude");
                if (excludes != null) {
                    for (Element exclude : excludes) {
                        modelParamMap.remove(UtilXml.checkEmpty(exclude.getAttribute("field-name")));
                    }
                }

                // now add in all the remaining params
                for (ModelParam thisParam : modelParamMap.values()) {
                    service.addParam(thisParam);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem loading auto-attributes [" + entityName + "] for " + service.getName(), MODULE);
            } catch (GeneralException e) {
                Debug.logError(e, "Cannot load auto-attributes : " + e.getMessage() + " for " + service.getName(), MODULE);
            }
        }
    }

    private static void createAttrDefs(Element baseElement, ModelService service) {
        // Add in the defined attributes (override the above defaults if specified)
        for (Element attribute: UtilXml.childElementList(baseElement, "attribute")) {
            ModelParam param = createAttrDef(attribute, null, service);
            service.addParam(param);
        }

        // Add the default optional parameters
        ModelParam def;

        // responseMessage
        def = new ModelParam();
        def.setName(ModelService.RESPONSE_MESSAGE);
        def.setType("String");
        def.setMode(ModelService.OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // errorMessage
        def = new ModelParam();
        def.setName(ModelService.ERROR_MESSAGE);
        def.setType("String");
        def.setMode(ModelService.OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // errorMessageList
        def = new ModelParam();
        def.setName(ModelService.ERROR_MESSAGE_LIST);
        def.setType("java.util.List");
        def.setMode(ModelService.OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // successMessage
        def = new ModelParam();
        def.setName(ModelService.SUCCESS_MESSAGE);
        def.setType("String");
        def.setMode(ModelService.OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // successMessageList
        def = new ModelParam();
        def.setName(ModelService.SUCCESS_MESSAGE_LIST);
        def.setType("java.util.List");
        def.setMode(ModelService.OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // userLogin
        def = new ModelParam();
        def.setName("userLogin");
        def.setType("org.apache.ofbiz.entity.GenericValue");
        def.setMode(ModelService.IN_OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // login.username
        def = new ModelParam();
        def.setName("login.username");
        def.setType("String");
        def.setMode(ModelService.IN_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // login.password
        def = new ModelParam();
        def.setName("login.password");
        def.setType("String");
        def.setMode(ModelService.IN_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // Locale
        def = new ModelParam();
        def.setName("locale");
        def.setType("java.util.Locale");
        def.setMode(ModelService.IN_OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // timeZone
        def = new ModelParam();
        def.setName("timeZone");
        def.setType("java.util.TimeZone");
        def.setMode(ModelService.IN_OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
        // visualTheme
        def = new ModelParam();
        def.setName("visualTheme");
        def.setType("org.apache.ofbiz.widget.renderer.VisualTheme");
        def.setMode(ModelService.IN_OUT_PARAM);
        def.setOptional(true);
        def.setInternal(true);
        service.addParam(def);
    }

    private static ModelParam createAttrDef(Element attribute, ModelParam parentParam, ModelService service) {
        boolean hasParent = parentParam != null;
        ModelParam param = new ModelParam();

        param.setName(UtilXml.checkEmpty(attribute.getAttribute("name")).intern());
        param.setDescription(getCDATADef(attribute, "description"));
        param.setType(UtilXml.checkEmpty(attribute.getAttribute("type")).intern());
        param.setMode(hasParent ? parentParam.getMode() : UtilXml.checkEmpty(attribute.getAttribute("mode")).intern()); //inherit mode from parent
        param.setEntityName(UtilXml.checkEmpty(attribute.getAttribute("entity-name")).intern());
        param.setFieldName(UtilXml.checkEmpty(attribute.getAttribute("field-name")).intern());
        param.setRequestAttributeName(UtilXml.checkEmpty(attribute.getAttribute("request-attribute-name")).intern());
        param.setSessionAttributeName(UtilXml.checkEmpty(attribute.getAttribute("session-attribute-name")).intern());
        param.setStringMapPrefix(UtilXml.checkEmpty(attribute.getAttribute("string-map-prefix")).intern());
        param.setStringListSuffix(UtilXml.checkEmpty(attribute.getAttribute("string-list-suffix")).intern());
        param.setFormLabel(attribute.hasAttribute("form-label") ? attribute.getAttribute("form-label").intern() : null);
        param.setOptional("true".equalsIgnoreCase(attribute.getAttribute("optional"))); // default to true
        param.setFormDisplay(!"false".equalsIgnoreCase(attribute.getAttribute("form-display"))); // default to false
        param.setAllowHtml(UtilXml.checkEmpty(attribute.getAttribute("allow-html"), "none").intern()); // default to none

         // default value
        String defValue = attribute.getAttribute("default-value");
        if (UtilValidate.isNotEmpty(defValue)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Got a default-value [" + defValue + "] for service attribute [" + service.getName() + "."
                         + param.getName() + "]", MODULE);
            }
            param.setDefaultValue(defValue.intern());
        }

        // set the entity name to the default if not specified
        if (param.getEntityName().length() == 0) {
            param.setEntityName(service.getDefaultEntityName());
        }

        // set the field-name to the name if entity name is specified but no field-name
        if (param.getFieldName().length() == 0 && param.getEntityName().length() > 0) {
            param.setFieldName(param.getName());
        }

        // set the validators
        addValidators(attribute, param);
        for (Element child: UtilXml.childElementList(attribute, "attribute")) {
            ModelParam childParam = createAttrDef(child, param, service);
            param.getChildren().add(childParam);
        }
        return param;
    }

    private static void createOverrideDefs(Element baseElement, ModelService service) {
        for (Element overrideElement: UtilXml.childElementList(baseElement, "override")) {
            String name = UtilXml.checkEmpty(overrideElement.getAttribute("name"));
            ModelParam param = service.getParam(name);
            boolean directToParams = true;
            if (param == null) {
                if (!service.isInheritedParameters() && (service.getInModelParamList().size() > 0 || "group".equals(service.getEngineName()))) {
                    // create a temp def to place in the ModelService
                    // this will get read when we read implemented services
                    directToParams = false;
                    param = new ModelParam();
                    param.setName(name);
                } else {
                    Debug.logWarning("No parameter found for override parameter named: " + name + " in service " + service.getName(), MODULE);
                }
            }

            if (param != null) {
                // set only modified values
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("type"))) {
                    param.setType(UtilXml.checkEmpty(overrideElement.getAttribute("type")).intern());
                }
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("mode"))) {
                    param.setMode(UtilXml.checkEmpty(overrideElement.getAttribute("mode")).intern());
                }
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("entity-name"))) {
                    param.setEntityName(UtilXml.checkEmpty(overrideElement.getAttribute("entity-name")).intern());
                }
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("field-name"))) {
                    param.setFieldName(UtilXml.checkEmpty(overrideElement.getAttribute("field-name")).intern());
                }
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("form-label"))) {
                    param.setFormLabel(UtilXml.checkEmpty(overrideElement.getAttribute("form-label")).intern());
                }
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("optional"))) {
                    param.setOptional("true".equalsIgnoreCase(overrideElement.getAttribute("optional"))); // default to true
                    param.setOverrideOptional(true);
                }
                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("form-display"))) {
                    param.setFormDisplay(!"false".equalsIgnoreCase(overrideElement.getAttribute("form-display"))); // default to false
                    param.setOverrideFormDisplay(true);
                }

                if (UtilValidate.isNotEmpty(overrideElement.getAttribute("allow-html"))) {
                    param.setAllowHtml(UtilXml.checkEmpty(overrideElement.getAttribute("allow-html")).intern());
                }

                // default value
                String defValue = overrideElement.getAttribute("default-value");
                if (UtilValidate.isNotEmpty(defValue)) {
                    param.setDefaultValue(defValue);
                }

                // override validators
                addValidators(overrideElement, param);

                if (directToParams) {
                    service.addParam(param);
                } else {
                    service.getOverrideParameters().add(param);
                }
            }
        }
    }

    private static void createDeprecated(Element baseElement, ModelService service) {
        Element deprecated = UtilXml.firstChildElement(baseElement, "deprecated");
        if (deprecated != null) {
            service.setDeprecatedUseInstead(deprecated.getAttribute("use-instead"));
            service.setDeprecatedSince(deprecated.getAttribute("since"));
            service.setDeprecatedReason(UtilXml.elementValue(deprecated));
            service.informIfDeprecated();
        }
    }

    private static void addValidators(Element attribute, ModelParam param) {
        List<? extends Element> validateElements = UtilXml.childElementList(attribute, "type-validate");
        if (UtilValidate.isNotEmpty(validateElements)) {
            // always clear out old ones; never append
            param.setValidators(new LinkedList<>());

            Element validate = validateElements.get(0);
            String methodName = validate.getAttribute("method").intern();
            String className = validate.getAttribute("class").intern();

            Element fail = UtilXml.firstChildElement(validate, "fail-message");
            if (fail != null) {
                String message = fail.getAttribute("message").intern();
                param.addValidator(className, methodName, message);
            } else {
                fail = UtilXml.firstChildElement(validate, "fail-property");
                if (fail != null) {
                    String resource = fail.getAttribute("resource").intern();
                    String property = fail.getAttribute("property").intern();
                    param.addValidator(className, methodName, resource, property);
                }
            }
        }
    }

    private static Document getDocument(URL url) {
        if (url == null) {
            return null;
        }
        Document document = null;

        try {
            document = UtilXml.readXmlDocument(url, true, true);
        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception x = sxe;

            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            x.printStackTrace();
        } catch (ParserConfigurationException | IOException e) {
            // Parser with specified options can't be built
            Debug.logError(e, MODULE);
        }

        return document;
    }
}
