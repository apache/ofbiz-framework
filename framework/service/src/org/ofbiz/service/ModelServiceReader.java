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
package org.ofbiz.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.collections.map.LinkedMap;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.service.group.GroupModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Generic Service - Service Definition Reader
 */

public class ModelServiceReader implements Serializable {

    public static final String module = ModelServiceReader.class.getName();

    /** is either from a URL or from a ResourceLoader (through the ResourceHandler) */
    protected boolean isFromURL;
    protected URL readerURL = null;
    protected ResourceHandler handler = null;
    protected Map modelServices = null;
    protected DispatchContext dctx = null;

    public static Map getModelServiceMap(URL readerURL, DispatchContext dctx) {
        if (readerURL == null) {
            Debug.logError("Cannot add reader with a null reader URL", module);
            return null;
        }

        ModelServiceReader reader = new ModelServiceReader(readerURL, dctx);
        return reader.getModelServices();
    }

    public static Map getModelServiceMap(ResourceHandler handler, DispatchContext dctx) {
        ModelServiceReader reader = new ModelServiceReader(handler, dctx);
        return reader.getModelServices();
    }

    protected ModelServiceReader(URL readerURL, DispatchContext dctx) {
        this.isFromURL = true;
        this.readerURL = readerURL;
        this.handler = null;
        this.dctx = dctx;
        // preload models...
        getModelServices();
    }

    protected ModelServiceReader(ResourceHandler handler, DispatchContext dctx) {
        this.isFromURL = false;
        this.readerURL = null;
        this.handler = handler;
        this.dctx = dctx;
        // preload models...
        getModelServices();
    }

    public Map getModelServices() {
        if (modelServices == null) { // don't want to block here
            synchronized (ModelServiceReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (modelServices == null) { // now it's safe
                    modelServices = FastMap.newInstance();

                    UtilTimer utilTimer = new UtilTimer();

                    Document document;

                    if (this.isFromURL) {
                        // utilTimer.timerString("Before getDocument in file " + readerURL);
                        document = getDocument(readerURL);

                        if (document == null) {
                            modelServices = null;
                            return null;
                        }
                    } else {
                        // utilTimer.timerString("Before getDocument in " + handler);
                        try {
                            document = handler.getDocument();
                        } catch (GenericConfigException e) {
                            Debug.logError(e, "Error getting XML document from resource", module);
                            return null;
                        }
                    }

                    if (this.isFromURL) {// utilTimer.timerString("Before getDocumentElement in file " + readerURL);
                    } else {// utilTimer.timerString("Before getDocumentElement in " + handler);
                    }

                    Element docElement = document.getDocumentElement();
                    if (docElement == null) {
                        modelServices = null;
                        return null;
                    }

                    docElement.normalize();

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
                                Element curService = (Element) curChild;
                                String serviceName = UtilXml.checkEmpty(curService.getAttribute("name"));

                                // check to see if service with same name has already been read
                                if (modelServices.containsKey(serviceName)) {
                                    Debug.logWarning("WARNING: Service " + serviceName + " is defined more than once, " +
                                        "most recent will over-write previous definition(s)", module);
                                }

                                // utilTimer.timerString("  After serviceName -- " + i + " --");
                                ModelService service = createModelService(curService);

                                // utilTimer.timerString("  After createModelService -- " + i + " --");
                                if (service != null) {
                                    modelServices.put(serviceName, service);
                                    // utilTimer.timerString("  After modelServices.put -- " + i + " --");
                                    /*
                                    int reqIn = service.getParameterNames(ModelService.IN_PARAM, false).size();
                                    int optIn = service.getParameterNames(ModelService.IN_PARAM, true).size() - reqIn;
                                    int reqOut = service.getParameterNames(ModelService.OUT_PARAM, false).size();
                                    int optOut = service.getParameterNames(ModelService.OUT_PARAM, true).size() - reqOut;

                                    if (Debug.verboseOn()) {
                                        String msg = "-- getModelService: # " + i + " Loaded service: " + serviceName +
                                            " (IN) " + reqIn + "/" + optIn + " (OUT) " + reqOut + "/" + optOut;

                                        Debug.logVerbose(msg, module);                                        
                                    }
                                    */
                                } else {
                                    Debug.logWarning(
                                        "-- -- SERVICE ERROR:getModelService: Could not create service for serviceName: " +
                                        serviceName, module);
                                }

                            }
                        } while ((curChild = curChild.getNextSibling()) != null);
                    } else {
                        Debug.logWarning("No child nodes found.", module);
                    }
                    if (this.isFromURL) {
                        utilTimer.timerString("Finished file " + readerURL + " - Total Services: " + i + " FINISHED");
                        Debug.logImportant("Loaded " + i + " Service definitions from " + readerURL, module);
                    } else {
                        utilTimer.timerString("Finished document in " + handler + " - Total Services: " + i + " FINISHED");
						if (Debug.importantOn()) {
							String resourceLocation = handler.getLocation();
							try {
								resourceLocation = handler.getURL().toExternalForm();
							} catch (GenericConfigException e) {
								Debug.logError(e, "Could not get resource URL", module);
							}
	                        Debug.logImportant("Loaded " + i + " Service definitions from " + resourceLocation, module);
						}
                    }
                }
            }
        }
        return modelServices;
    }

    /** 
     * Gets an Service object based on a definition from the specified XML Service descriptor file.
     * @param serviceName The serviceName of the Service definition to use.
     * @return An Service object describing the specified service of the specified descriptor file.
     */
    public ModelService getModelService(String serviceName) {
        Map ec = getModelServices();

        if (ec != null)
            return (ModelService) ec.get(serviceName);
        else
            return null;
    }

    /** 
     * Creates a Iterator with the serviceName of each Service defined in the specified XML Service Descriptor file.
     * @return A Iterator of serviceName Strings
     */
    public Iterator getServiceNamesIterator() {
        Collection collection = getServiceNames();

        if (collection != null) {
            return collection.iterator();
        } else {
            return null;
        }
    }

    /** 
     * Creates a Collection with the serviceName of each Service defined in the specified XML Service Descriptor file.
     * @return A Collection of serviceName Strings
     */
    public Collection getServiceNames() {
        Map ec = getModelServices();

        return ec.keySet();
    }

    protected ModelService createModelService(Element serviceElement) {
        ModelService service = new ModelService();

        service.name = UtilXml.checkEmpty(serviceElement.getAttribute("name"));
        service.engineName = UtilXml.checkEmpty(serviceElement.getAttribute("engine"));
        service.location = UtilXml.checkEmpty(serviceElement.getAttribute("location"));
        service.invoke = UtilXml.checkEmpty(serviceElement.getAttribute("invoke"));  
        service.defaultEntityName = UtilXml.checkEmpty(serviceElement.getAttribute("default-entity-name"));
        service.fromLoader = isFromURL ? readerURL.toExternalForm() : handler.getLoaderName();        

        // these default to true; if anything but true, make false
        service.auth = "true".equalsIgnoreCase(serviceElement.getAttribute("auth"));
        service.export = "true".equalsIgnoreCase(serviceElement.getAttribute("export"));
        service.debug = "true".equalsIgnoreCase(serviceElement.getAttribute("debug"));
        
        // this defaults to true; if anything but false, make it true
        service.validate = !"false".equalsIgnoreCase(serviceElement.getAttribute("validate"));
        service.useTransaction = !"false".equalsIgnoreCase(serviceElement.getAttribute("use-transaction"));
        service.requireNewTransaction = !"false".equalsIgnoreCase(serviceElement.getAttribute("require-new-transaction"));

        // set the max retry field
        String maxRetryStr = UtilXml.checkEmpty(serviceElement.getAttribute("max-retry"));
        int maxRetry = -1;
        if (!UtilValidate.isEmpty(maxRetryStr)) {
            try {
                maxRetry = Integer.parseInt(maxRetryStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting maxRetry to -1 (default)", module);
                maxRetry = -1;
            }
        }
        service.maxRetry = maxRetry;
        
        // get the timeout and convert to int
        String timeoutStr = UtilXml.checkEmpty(serviceElement.getAttribute("transaction-timeout"), serviceElement.getAttribute("transaction-timout"));
        int timeout = 0;
        if (!UtilValidate.isEmpty(timeoutStr)) {
            try {
                timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting timeout to 0 (default)", module);
                timeout = 0;
            }
        }
        service.transactionTimeout = timeout;
                       
        service.description = getCDATADef(serviceElement, "description");
        service.nameSpace = getCDATADef(serviceElement, "namespace");  
        
        // contruct the context
        service.contextInfo = FastMap.newInstance();
        this.createNotification(serviceElement, service);
        this.createPermission(serviceElement, service);
        this.createPermGroups(serviceElement, service);
        this.createGroupDefs(serviceElement, service);
        this.createImplDefs(serviceElement, service);
        this.createAutoAttrDefs(serviceElement, service);
        this.createAttrDefs(serviceElement, service);
        this.createOverrideDefs(serviceElement, service);
               
        return service;
    }

    protected String getCDATADef(Element baseElement, String tagName) {
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

    protected void createNotification(Element baseElement, ModelService model) {
        List n = UtilXml.childElementList(baseElement, "notification");
        // default notification groups
        ModelNotification nSuccess = new ModelNotification();
        nSuccess.notificationEvent = "success";
        nSuccess.notificationGroupName = "default.success." + model.fromLoader;
        model.notifications.add(nSuccess);

        ModelNotification nFail = new ModelNotification();
        nFail.notificationEvent = "fail";
        nFail.notificationGroupName = "default.fail." + model.fromLoader;
        model.notifications.add(nFail);

        ModelNotification nError = new ModelNotification();
        nError.notificationEvent = "error";
        nError.notificationGroupName = "default.error." + model.fromLoader;
        model.notifications.add(nError);

        if (n != null) {
            Iterator i = n.iterator();
            while (i.hasNext()) {
                Element e = (Element) i.next();
                ModelNotification notify = new ModelNotification();
                notify.notificationEvent = e.getAttribute("event");
                notify.notificationGroupName = e.getAttribute("group");
                model.notifications.add(notify);                
            }
        }
    }

    protected void createPermission(Element baseElement, ModelService model) {
        Element e = UtilXml.firstChildElement(baseElement, "permission-service");
        if (e != null) {
            model.permissionServiceName = e.getAttribute("service-name");
            model.permissionMainAction = e.getAttribute("main-action");
            model.permissionResourceDesc = e.getAttribute("resource-description");
            model.auth = true; // auth is always required when permissions are set
        }
    }

    protected void createPermGroups(Element baseElement, ModelService model) {
        List permGroups = UtilXml.childElementList(baseElement, "required-permissions");
        Iterator permIter = permGroups.iterator();

        while (permIter.hasNext()) {
            Element element = (Element) permIter.next();
            ModelPermGroup group = new ModelPermGroup();
            group.joinType = element.getAttribute("join-type");
            createGroupPermissions(element, group, model);
            model.permissionGroups.add(group);
        }
    }

    protected void createGroupPermissions(Element baseElement, ModelPermGroup group, ModelService service) {
        List permElements = UtilXml.childElementList(baseElement, "check-permission");
        List rolePermElements = UtilXml.childElementList(baseElement, "check-role-member");        

        // create the simple permissions
        Iterator si = permElements.iterator();
        while (si.hasNext()) {
            Element element = (Element) si.next();
            ModelPermission perm = new ModelPermission();
            perm.nameOrRole = element.getAttribute("permission");
            perm.action = element.getAttribute("action");
            if (perm.action != null && perm.action.length() > 0) {
                perm.permissionType = ModelPermission.ENTITY_PERMISSION;
            } else {
                perm.permissionType = ModelPermission.PERMISSION;
            }
            perm.serviceModel = service;
            group.permissions.add(perm);
        }

        // create the role member permissions
        Iterator ri = rolePermElements.iterator();
        while (ri.hasNext()) {
            Element element = (Element) ri.next();
            ModelPermission perm = new ModelPermission();
            perm.permissionType = ModelPermission.ROLE_MEMBER;
            perm.nameOrRole = element.getAttribute("role-type");
            perm.serviceModel = service;
            group.permissions.add(perm);
        }
    }

    protected void createGroupDefs(Element baseElement, ModelService service) {
        List group = UtilXml.childElementList(baseElement, "group");
        if (group != null && group.size() > 0) {
            Element groupElement = (Element) group.get(0);
            groupElement.setAttribute("name", "_" + service.name + ".group");
            service.internalGroup = new GroupModel(groupElement);
            service.invoke = service.internalGroup.getGroupName();
            Debug.logWarning("Created INTERNAL GROUP model [" + service.internalGroup + "]", module);
        }
    }
    
    protected void createImplDefs(Element baseElement, ModelService service) {
        List implElements = UtilXml.childElementList(baseElement, "implements");
        Iterator implIter = implElements.iterator();
                
        while (implIter.hasNext()) {
            Element implement = (Element) implIter.next();
            String serviceName = UtilXml.checkEmpty(implement.getAttribute("service"));
            boolean optional = UtilXml.checkBoolean(implement.getAttribute("optional"), false);
            if (serviceName.length() > 0)
                service.implServices.add(new ModelServiceIface(serviceName, optional));
                //service.implServices.add(serviceName);
        }
    }
    
    protected void createAutoAttrDefs(Element baseElement, ModelService service) {
        List autoElement = UtilXml.childElementList(baseElement, "auto-attributes");
        Iterator autoIter = autoElement.iterator();
        
        while (autoIter.hasNext()) {
            Element element = (Element) autoIter.next();            
            createAutoAttrDef(element, service);
        }
    }
    
    protected void createAutoAttrDef(Element autoElement, ModelService service) {
        // get the entity name; first from the auto-attributes then from the service def                 
        String entityName = UtilXml.checkEmpty(autoElement.getAttribute("entity-name"));        
        if (entityName == null || entityName.length() == 0) {
            entityName = service.defaultEntityName;
            if (entityName == null || entityName.length() == 0) {
                Debug.logWarning("Auto-Attribute does not specify an entity-name; not default-entity on service definition", module);
            }
        }
        
        // get the include type 'pk|nonpk|all'
        String includeType = UtilXml.checkEmpty(autoElement.getAttribute("include"));
        boolean includePk = "pk".equals(includeType) || "all".equals(includeType);
        boolean includeNonPk = "nonpk".equals(includeType) || "all".equals(includeType);
        
        // need a delegator for this
        GenericDelegator delegator = dctx.getDelegator();
        if (delegator == null) {
            Debug.logWarning("Cannot use auto-attribute fields with a null delegator", module);
        }
        
        if (delegator != null && entityName != null) {
            Map modelParamMap = new LinkedMap();
            try {            
                ModelEntity entity = delegator.getModelEntity(entityName);
                if (entity == null) {
                    throw new GeneralException("Could not find entity with name [" + entityName + "]");
                }
                Iterator fieldsIter = entity.getFieldsIterator();
                if (fieldsIter != null) {            
                    while (fieldsIter.hasNext()) {
                        ModelField field = (ModelField) fieldsIter.next();
                        if ((!field.getIsAutoCreatedInternal()) && ((field.getIsPk() && includePk) || (!field.getIsPk() && includeNonPk))) {
                            ModelFieldType fieldType = delegator.getEntityFieldType(entity, field.getType());
                            if (fieldType == null) {
                                throw new GeneralException("Null field type from delegator for entity [" + entityName + "]");
                            }
                            ModelParam param = new ModelParam();
                            param.entityName = entityName;
                            param.fieldName = field.getName();
                            param.name = field.getName();
                            param.type = fieldType.getJavaType();
                            param.mode = UtilXml.checkEmpty(autoElement.getAttribute("mode"));
                            param.optional = "true".equalsIgnoreCase(autoElement.getAttribute("optional")); // default to true
                            param.formDisplay = !"false".equalsIgnoreCase(autoElement.getAttribute("form-display")); // default to false                        
                            modelParamMap.put(field.getName(), param);
                        }
                    }
                    
                    // get the excludes list; and remove those from the map
                    List excludes = UtilXml.childElementList(autoElement, "exclude");
                    if (excludes != null) {                    
                        Iterator excludesIter = excludes.iterator();
                        while (excludesIter.hasNext()) {
                            Element exclude = (Element) excludesIter.next();
                            modelParamMap.remove(UtilXml.checkEmpty(exclude.getAttribute("field-name")));
                        }
                    }
                    
                    // now add in all the remaining params
                    Set keySet = modelParamMap.keySet();
                    Iterator setIter = keySet.iterator();
                    while (setIter.hasNext()) {
                        ModelParam thisParam = (ModelParam) modelParamMap.get(setIter.next()); 
                        //Debug.logInfo("Adding Param to " + service.name + ": " + thisParam.name + " [" + thisParam.mode + "] " + thisParam.type + " (" + thisParam.optional + ")", module);                       
                        service.addParam(thisParam);
                    }                    
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem loading auto-attributes [" + entityName + "] for " + service.name, module);
            } catch (GeneralException e) {
                Debug.logError(e, "Cannot load auto-attributes : " + e.getMessage() + " for " + service.name, module);
            }            
        }
    }
            
    protected void createAttrDefs(Element baseElement, ModelService service) {
        // Add in the defined attributes (override the above defaults if specified)
        List paramElements = UtilXml.childElementList(baseElement, "attribute");
        Iterator paramIter = paramElements.iterator();

        while (paramIter.hasNext()) {
            Element attribute = (Element) paramIter.next();
            ModelParam param = new ModelParam();

            param.name = UtilXml.checkEmpty(attribute.getAttribute("name"));
            param.type = UtilXml.checkEmpty(attribute.getAttribute("type"));
            param.mode = UtilXml.checkEmpty(attribute.getAttribute("mode"));
            param.entityName = UtilXml.checkEmpty(attribute.getAttribute("entity-name"));
            param.fieldName = UtilXml.checkEmpty(attribute.getAttribute("field-name"));
            param.stringMapPrefix = UtilXml.checkEmpty(attribute.getAttribute("string-map-prefix"));
            param.stringListSuffix = UtilXml.checkEmpty(attribute.getAttribute("string-list-suffix"));
            param.formLabel = attribute.hasAttribute("form-label")?attribute.getAttribute("form-label"):null;
            param.optional = "true".equalsIgnoreCase(attribute.getAttribute("optional")); // default to true
            param.formDisplay = !"false".equalsIgnoreCase(attribute.getAttribute("form-display")); // default to false

            // default value
            String defValue = attribute.getAttribute("default-value");
            if (UtilValidate.isNotEmpty(defValue)) {
                param.defaultValue = defValue;
                if (param.type != null) {
                    param.defaultValueObj = service.convertDefaultValue(service.name, param.name, param.type, defValue);
                }
                param.optional = true;
            }
            
            // set the entity name to the default if not specified
            if (param.entityName.length() == 0) {
                param.entityName = service.defaultEntityName;
            }   
            
            // set the field-name to the name if entity name is specified but no field-name
            if (param.fieldName.length() == 0 && param.entityName.length() > 0) {
                param.fieldName = param.name;         
            }

            // set the validators
            this.addValidators(attribute, param);
            service.addParam(param);
        }

        // Add the default optional parameters
        ModelParam def;

        // responseMessage
        def = new ModelParam();
        def.name = ModelService.RESPONSE_MESSAGE;
        def.type = "String";
        def.mode = "OUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
        // errorMessage
        def = new ModelParam();
        def.name = ModelService.ERROR_MESSAGE;
        def.type = "String";
        def.mode = "OUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
        // errorMessageList
        def = new ModelParam();
        def.name = ModelService.ERROR_MESSAGE_LIST;
        def.type = "java.util.List";
        def.mode = "OUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
        // successMessage
        def = new ModelParam();
        def.name = ModelService.SUCCESS_MESSAGE;
        def.type = "String";
        def.mode = "OUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
        // successMessageList
        def = new ModelParam();
        def.name = ModelService.SUCCESS_MESSAGE_LIST;
        def.type = "java.util.List";
        def.mode = "OUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
        // userLogin
        def = new ModelParam();
        def.name = "userLogin";
        def.type = "org.ofbiz.entity.GenericValue";
        def.mode = "INOUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
        // Locale
        def = new ModelParam();
        def.name = "locale";
        def.type = "java.util.Locale";
        def.mode = "INOUT";
        def.optional = true;
        def.internal = true;
        service.addParam(def);
    }
    
    protected void createOverrideDefs(Element baseElement, ModelService service) {
        List paramElements = UtilXml.childElementList(baseElement, "override");
        Iterator paramIter = paramElements.iterator();

        while (paramIter.hasNext()) {
            Element attribute = (Element) paramIter.next();
            String name = UtilXml.checkEmpty(attribute.getAttribute("name"));
            ModelParam param = service.getParam(name);
            boolean directToParams = true;
            if (param == null) {
                if (!service.inheritedParameters && (service.implServices.size() > 0 || "group".equals(service.engineName))) {                 
                    // create a temp def to place in the ModelService
                    // this will get read when we read implemented services 
                    directToParams = false;               
                    param = new ModelParam();
                    param.name = name;
                } else {
                    Debug.logWarning("No parameter found for override parameter named: " + name + " in service " + service.name, module);
                }
            }             
            
            if (param != null) {                                                        
                // set only modified values
                if (attribute.getAttribute("type") != null && attribute.getAttribute("type").length() > 0) {                
                    param.type = UtilXml.checkEmpty(attribute.getAttribute("type"));
                }
                if (attribute.getAttribute("mode") != null && attribute.getAttribute("mode").length() > 0) {                            
                    param.mode = UtilXml.checkEmpty(attribute.getAttribute("mode"));
                }
                if (attribute.getAttribute("entity-name") != null && attribute.getAttribute("entity-name").length() > 0) {
                   param.entityName = UtilXml.checkEmpty(attribute.getAttribute("entity-name"));
                }
                if (attribute.getAttribute("field-name") != null && attribute.getAttribute("field-name").length() > 0) {
                    param.fieldName = UtilXml.checkEmpty(attribute.getAttribute("field-name"));
                }
                if (attribute.getAttribute("form-label") != null && attribute.getAttribute("form-label").length() > 0) {                
                    param.formLabel = UtilXml.checkEmpty(attribute.getAttribute("form-label"));
                }
                if (attribute.getAttribute("optional") != null && attribute.getAttribute("optional").length() > 0) {                            
                    param.optional = "true".equalsIgnoreCase(attribute.getAttribute("optional")); // default to true
                    param.overrideOptional = true;
                }
                if (attribute.getAttribute("form-display") != null && attribute.getAttribute("form-display").length() > 0) {
                    param.formDisplay = !"false".equalsIgnoreCase(attribute.getAttribute("form-display")); // default to false
                    param.overrideFormDisplay = true;
                }                

                // default value
                String defValue = attribute.getAttribute("default-value");
                if (UtilValidate.isNotEmpty(defValue)) {
                    param.defaultValue = defValue;
                    if (param.type != null) {
                        param.defaultValueObj = service.convertDefaultValue(service.name, param.name, param.type, defValue);
                    }                    
                    param.optional = true;
                }

                // override validators
                this.addValidators(attribute, param);

                if (directToParams) {
                    service.addParam(param);
                } else {                  
                    service.overrideParameters.add(param);                    
                }
            }                                                                                      
        }        
    }

    protected void addValidators(Element attribute, ModelParam param) {
        List validateElements = UtilXml.childElementList(attribute, "type-validate");
        if (validateElements != null && validateElements.size() > 0) {
            // always clear out old ones; never append
            param.validators = FastList.newInstance();

            Iterator i = validateElements.iterator();
            Element validate = (Element) i.next();
            String methodName = validate.getAttribute("method");
            String className = validate.getAttribute("class");

            Element fail = UtilXml.firstChildElement(validate, "fail-message");
            if (fail != null) {
                String message = fail.getAttribute("message");
                param.addValidator(className, methodName, message);
            } else {
                fail = UtilXml.firstChildElement(validate, "fail-property");
                if (fail != null) {
                    String resource = fail.getAttribute("resource");
                    String property = fail.getAttribute("property");
                    param.addValidator(className, methodName, resource, property);
                }
            }
        }
    }

    protected Document getDocument(URL url) {
        if (url == null)
            return null;
        Document document = null;

        try {
            document = UtilXml.readXmlDocument(url, true);
        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception x = sxe;

            if (sxe.getException() != null)
                x = sxe.getException();
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return document;
    }
}
