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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ofbiz.base.metrics.Metrics;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.group.GroupModel;
import org.apache.ofbiz.service.group.GroupServiceModel;
import org.apache.ofbiz.service.group.ServiceGroupReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;

/**
 * Generic Service Model Class
 */
@SuppressWarnings("serial")
public class ModelService extends AbstractMap<String, Object> implements Serializable {
    private static final Field[] MODEL_SERVICE_FIELDS;
    private static final Map<String, Field> MODEL_SERVICE_FIELD_MAP = new LinkedHashMap<>();
    static {
        MODEL_SERVICE_FIELDS = ModelService.class.getFields();
        for (Field field: MODEL_SERVICE_FIELDS) {
            MODEL_SERVICE_FIELD_MAP.put(field.getName(), field);
        }
    }
    private static final String MODULE = ModelService.class.getName();

    public static final String XSD = "http://www.w3.org/2001/XMLSchema";
    public static final String TNS = "http://ofbiz.apache.org/service/";
    public static final String OUT_PARAM = "OUT";
    public static final String IN_PARAM = "IN";
    public static final String IN_OUT_PARAM = "INOUT";

    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String RESPOND_SUCCESS = "success";
    public static final String RESPOND_ERROR = "error";
    public static final String RESPOND_FAIL = "fail";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_MESSAGE_LIST = "errorMessageList";
    public static final String ERROR_MESSAGE_MAP = "errorMessageMap";
    public static final String SUCCESS_MESSAGE = "successMessage";
    public static final String SUCCESS_MESSAGE_LIST = "successMessageList";

    private static final String RESOURCE = "ServiceErrorUiLabels";

    /** The name of this service */
    private String name;

    /** The location of the definition this service */
    private String definitionLocation;

    /** The description of this service */
    private String description;

    /** The name of the service engine */
    private String engineName;

    /** The namespace of this service */
    private String nameSpace;

    /** The corresponding REST verb behaviour for this service */
    private String action;

    /** The package name or location of this service */
    private String location;

    /** The method or function to invoke for this service */
    private String invoke;

    /** The default Entity to use for auto-attributes */
    private String defaultEntityName;

    /** The loader which loaded this definition */
    private String fromLoader;

    /** Does this service require authorization */
    private boolean auth;

    /** Can this service be exported via RPC, RMI, SOAP, etc */
    private boolean export;

    /** Enable verbose debugging when calling this service */
    private boolean debug;

    /** Validate the context info for this service */
    private boolean validate;

    /** Create a transaction for this service (if one is not already in place...)? */
    private boolean useTransaction;

    /** Require a new transaction for this service */
    private boolean requireNewTransaction;

    /** Override the default transaction timeout, only works if we start the transaction */
    private int transactionTimeout;

    /** Sets the max number of times this service will retry when failed (persisted async only) */
    private int maxRetry = 0;

    /** Permission service*/
    private ModelPermission modelPermission = null;

    /** Semaphore setting (wait, fail, none) */
    private String semaphore;

    /** Semaphore wait time (in milliseconds) */
    private int semaphoreWait;

    /** Semaphore sleep time (in milliseconds) */
    private int semaphoreSleep;

    /** Require a new transaction for this service */
    private boolean hideResultInLog;

    /** Set of services this service implements */
    private Set<ModelServiceIface> implServices = new LinkedHashSet<>();

    /** Set of override parameters */
    private Set<ModelParam> overrideParameters = new LinkedHashSet<>();

    /** List of permission groups for service invocation */
    private List<ModelPermGroup> permissionGroups = new LinkedList<>();

    /** List of email-notifications for this service */
    private List<ModelNotification> notifications = new LinkedList<>();

    /** Internal Service Group */
    private GroupModel internalGroup = null;

    /**Deprecated information*/
    private String deprecatedUseInstead = null;
    private String deprecatedSince = null;
    private String deprecatedReason = null;

    /** Context Information, a Map of parameters used by the service, contains ModelParam objects */
    private Map<String, ModelParam> contextInfo = new LinkedHashMap<>();

    /** Context Information, a List of parameters used by the service, contains ModelParam objects */
    private List<ModelParam> contextParamList = new LinkedList<>();

    /** Flag to say if we have pulled in our addition parameters from our implemented service(s) */
    private boolean inheritedParameters = false;

    /**
     * Service metrics.
     */
    private Metrics metrics = null;

    /**
     * Sets name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets definition location.
     * @param definitionLocation the definition location
     */
    public void setDefinitionLocation(String definitionLocation) {
        this.definitionLocation = definitionLocation;
    }

    /**
     * Sets description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets engine name.
     * @param engineName the engine name
     */
    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    /**
     * Sets name space.
     * @param nameSpace the name space
     */
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    /**
     * Sets action.
     * @param action the action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Sets location.
     * @param location the location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets invoke.
     * @param invoke the invoke
     */
    public void setInvoke(String invoke) {
        this.invoke = invoke;
    }

    /**
     * Sets default entity name.
     * @param defaultEntityName the default entity name
     */
    public void setDefaultEntityName(String defaultEntityName) {
        this.defaultEntityName = defaultEntityName;
    }

    /**
     * Sets from loader.
     * @param fromLoader the from loader
     */
    public void setFromLoader(String fromLoader) {
        this.fromLoader = fromLoader;
    }

    /**
     * Sets auth.
     * @param auth the auth
     */
    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    /**
     * Sets export.
     * @param export the export
     */
    public void setExport(boolean export) {
        this.export = export;
    }

    /**
     * Sets debug.
     * @param debug the debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets validate.
     * @param validate the validate
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * Sets use transaction.
     * @param useTransaction the use transaction
     */
    public void setUseTransaction(boolean useTransaction) {
        this.useTransaction = useTransaction;
    }

    /**
     * Sets require new transaction.
     * @param requireNewTransaction the require new transaction
     */
    public void setRequireNewTransaction(boolean requireNewTransaction) {
        this.requireNewTransaction = requireNewTransaction;
    }

    /**
     * Sets transaction timeout.
     * @param transactionTimeout the transaction timeout
     */
    public void setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    /**
     * Sets max retry.
     * @param maxRetry the max retry
     */
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    /**
     * Sets model permission.
     * @param modelPermission the model permission
     */
    public void setModelPermission(ModelPermission modelPermission) {
        this.modelPermission = modelPermission;
    }

    /**
     * Sets semaphore.
     * @param semaphore the semaphore
     */
    public void setSemaphore(String semaphore) {
        this.semaphore = semaphore;
    }

    /**
     * Sets semaphore wait.
     * @param semaphoreWait the semaphore wait
     */
    public void setSemaphoreWait(int semaphoreWait) {
        this.semaphoreWait = semaphoreWait;
    }

    /**
     * Sets semaphore sleep.
     * @param semaphoreSleep the semaphore sleep
     */
    public void setSemaphoreSleep(int semaphoreSleep) {
        this.semaphoreSleep = semaphoreSleep;
    }

    /**
     * Sets hide result in log.
     * @param hideResultInLog the hide result in log
     */
    public void setHideResultInLog(boolean hideResultInLog) {
        this.hideResultInLog = hideResultInLog;
    }

    /**
     * Gets definition location.
     * @return the definition location
     */
    public String getDefinitionLocation() {
        return definitionLocation;
    }

    /**
     * Sets impl services.
     * @param implServices the impl services
     */
    public void setImplServices(Set<ModelServiceIface> implServices) {
        this.implServices = implServices;
    }

    /**
     * Sets override parameters.
     * @param overrideParameters the override parameters
     */
    public void setOverrideParameters(Set<ModelParam> overrideParameters) {
        this.overrideParameters = overrideParameters;
    }

    /**
     * Sets permission groups.
     * @param permissionGroups the permission groups
     */
    public void setPermissionGroups(List<ModelPermGroup> permissionGroups) {
        this.permissionGroups = permissionGroups;
    }

    /**
     * Sets notifications.
     * @param notifications the notifications
     */
    public void setNotifications(List<ModelNotification> notifications) {
        this.notifications = notifications;
    }

    /**
     * Sets internal group.
     * @param internalGroup the internal group
     */
    public void setInternalGroup(GroupModel internalGroup) {
        this.internalGroup = internalGroup;
    }

    /**
     * Sets deprecated use instead.
     * @param deprecatedUseInstead the deprecated use instead
     */
    public void setDeprecatedUseInstead(String deprecatedUseInstead) {
        this.deprecatedUseInstead = deprecatedUseInstead;
    }

    /**
     * Sets deprecated since.
     * @param deprecatedSince the deprecated since
     */
    public void setDeprecatedSince(String deprecatedSince) {
        this.deprecatedSince = deprecatedSince;
    }

    /**
     * Sets deprecated reason.
     * @param deprecatedReason the deprecated reason
     */
    public void setDeprecatedReason(String deprecatedReason) {
        this.deprecatedReason = deprecatedReason;
    }

    /**
     * Sets context info.
     * @param contextInfo the context info
     */
    public void setContextInfo(Map<String, ModelParam> contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets context param list.
     * @param contextParamList the context param list
     */
    public void setContextParamList(List<ModelParam> contextParamList) {
        this.contextParamList = contextParamList;
    }

    /**
     * Sets inherited parameters.
     * @param inheritedParameters the inherited parameters
     */
    public void setInheritedParameters(boolean inheritedParameters) {
        this.inheritedParameters = inheritedParameters;
    }

    /**
     * Sets metrics.
     * @param metrics the metrics
     */
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Gets description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets name space.
     * @return the name space
     */
    public String getNameSpace() {
        return nameSpace;
    }

    /**
     * Gets action.
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Gets default entity name.
     * @return the default entity name
     */
    public String getDefaultEntityName() {
        return defaultEntityName;
    }

    /**
     * Gets from loader.
     * @return the from loader
     */
    public String getFromLoader() {
        return fromLoader;
    }

    /**
     * Is export boolean.
     * @return the boolean
     */
    public boolean isExport() {
        return export;
    }

    /**
     * Gets semaphore wait.
     * @return the semaphore wait
     */
    public int getSemaphoreWait() {
        return semaphoreWait;
    }

    /**
     * Gets semaphore sleep.
     * @return the semaphore sleep
     */
    public int getSemaphoreSleep() {
        return semaphoreSleep;
    }

    /**
     * Gets impl services.
     * @return the impl services
     */
    public Set<ModelServiceIface> getImplServices() {
        return implServices;
    }

    /**
     * Gets override parameters.
     * @return the override parameters
     */
    public Set<ModelParam> getOverrideParameters() {
        return overrideParameters;
    }

    /**
     * Gets permission groups.
     * @return the permission groups
     */
    public List<ModelPermGroup> getPermissionGroups() {
        return permissionGroups;
    }

    /**
     * Gets notifications.
     * @return the notifications
     */
    public List<ModelNotification> getNotifications() {
        return notifications;
    }

    /**
     * Gets internal group.
     * @return the internal group
     */
    public GroupModel getInternalGroup() {
        return internalGroup;
    }

    /**
     * Gets deprecated use instead.
     * @return the deprecated use instead
     */
    public String getDeprecatedUseInstead() {
        return deprecatedUseInstead;
    }

    /**
     * Gets deprecated since.
     * @return the deprecated since
     */
    public String getDeprecatedSince() {
        return deprecatedSince;
    }

    /**
     * Gets deprecated reason.
     * @return the deprecated reason
     */
    public String getDeprecatedReason() {
        return deprecatedReason;
    }

    /**
     * Gets context info.
     * @return the context info
     */
    public Map<String, ModelParam> getContextInfo() {
        return contextInfo;
    }

    /**
     * Gets context param list.
     * @return the context param list
     */
    public List<ModelParam> getContextParamList() {
        return contextParamList;
    }

    /**
     * Is inherited parameters boolean.
     * @return the boolean
     */
    public boolean isInheritedParameters() {
        return inheritedParameters;
    }

    /**
     * Gets name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets invoke.
     * @return the invoke
     */
    public String getInvoke() {
        return invoke;
    }

    /**
     * Is hide result in log boolean.
     * @return the boolean
     */
    public boolean isHideResultInLog() {
        return hideResultInLog;
    }

    /**
     * Gets metrics.
     * @return the metrics
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Is auth boolean.
     * @return the boolean
     */
    public boolean isAuth() {
        return auth;
    }

    /**
     * Is validate boolean.
     * @return the boolean
     */
    public boolean isValidate() {
        return validate;
    }

    /**
     * Gets model permission.
     * @return the model permission
     */
    public ModelPermission getModelPermission() {
        return modelPermission;
    }

    /**
     * Is use transaction boolean.
     * @return the boolean
     */
    public boolean isUseTransaction() {
        return useTransaction;
    }

    /**
     * Is require new transaction boolean.
     * @return the boolean
     */
    public boolean isRequireNewTransaction() {
        return requireNewTransaction;
    }

    /**
     * Gets transaction timeout.
     * @return the transaction timeout
     */
    public int getTransactionTimeout() {
        return transactionTimeout;
    }

    /**
     * Gets max retry.
     * @return the max retry
     */
    public int getMaxRetry() {
        return maxRetry;
    }

    /**
     * Gets engine name.
     * @return the engine name
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Gets location.
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Is debug boolean.
     * @return the boolean
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Gets semaphore.
     * @return the semaphore
     */
    public String getSemaphore() {
        return semaphore;
    }

    public ModelService() { }

    public ModelService(ModelService model) {
        this.name = model.name;
        this.semaphore = model.semaphore;
        this.fromLoader = model.fromLoader;
        this.debug = model.debug;
        this.semaphoreWait = model.semaphoreWait;
        this.semaphoreSleep = model.semaphoreSleep;
        this.contextInfo = model.contextInfo;
        this.definitionLocation = model.definitionLocation;
        this.description = model.description;
        this.engineName = model.engineName;
        this.nameSpace = model.nameSpace;
        this.location = model.location;
        this.invoke = model.invoke;
        this.defaultEntityName = model.defaultEntityName;
        this.auth = model.auth;
        this.export = model.export;
        this.action = model.action;
        this.validate = model.validate;
        this.useTransaction = model.useTransaction;
        this.requireNewTransaction = model.requireNewTransaction;
        if (this.requireNewTransaction && !this.useTransaction) {
            // requireNewTransaction implies that a transaction is used
            this.useTransaction = true;
        }
        this.transactionTimeout = model.transactionTimeout;
        this.maxRetry = model.maxRetry;
        if (model.modelPermission != null) {
            modelPermission = model.modelPermission;
        }
        this.implServices = model.implServices;
        this.overrideParameters = model.overrideParameters;
        this.inheritedParameters = model.inheritedParameters();
        this.internalGroup = model.internalGroup;
        this.hideResultInLog = model.hideResultInLog;
        this.metrics = model.metrics;
        List<ModelParam> modelParamList = model.getModelParamList();
        for (ModelParam param: modelParamList) {
            this.addParamClone(param);
        }
    }

    @Override
    public Object get(Object name) {
        Field field = MODEL_SERVICE_FIELD_MAP.get(name.toString());
        if (field != null) {
            try {
                return field.get(this);
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private final class ModelServiceMapEntry implements Map.Entry<String, Object> {
        private final Field field;

        protected ModelServiceMapEntry(Field field) {
            this.field = field;
        }

        @Override
        public String getKey() {
            return field.getName();
        }

        @Override
        public Object getValue() {
            try {
                return field.get(ModelService.this);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return field.hashCode() ^ System.identityHashCode(ModelService.this);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ModelServiceMapEntry)) {
                return false;
            }
            ModelServiceMapEntry other = (ModelServiceMapEntry) o;
            return field.equals(other.field) && ModelService.this == other.getModelService();
        }

        private ModelService getModelService() {
            return ModelService.this;
        }
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return new AbstractSet<Map.Entry<String, Object>>() {
            @Override
            public int size() {
                return MODEL_SERVICE_FIELDS.length;
            }

            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return new Iterator<Map.Entry<String, Object>>() {
                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < MODEL_SERVICE_FIELDS.length;
                    }

                    @Override
                    public Map.Entry<String, Object> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return new ModelServiceMapEntry(MODEL_SERVICE_FIELDS[i++]);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public Object put(String o1, Object o2) {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(name).append("::");
        buf.append(definitionLocation).append("::");
        buf.append(description).append("::");
        buf.append(engineName).append("::");
        buf.append(nameSpace).append("::");
        buf.append(location).append("::");
        buf.append(invoke).append("::");
        buf.append(defaultEntityName).append("::");
        buf.append(auth).append("::");
        buf.append(export).append("::");
        buf.append(action).append("::");
        buf.append(validate).append("::");
        buf.append(useTransaction).append("::");
        buf.append(requireNewTransaction).append("::");
        buf.append(transactionTimeout).append("::");
        buf.append(implServices).append("::");
        buf.append(overrideParameters).append("::");
        buf.append(contextInfo).append("::");
        buf.append(contextParamList).append("::");
        buf.append(inheritedParameters).append("::");
        buf.append(hideResultInLog).append("::");
        return buf.toString();
    }

    /**
     * Debug info string.
     * @return the string
     */
    public String debugInfo() {
        if (debug || Debug.verboseOn()) {
            return " [" + this.toString() + "]";
        }
        return "";
    }

    /**
     * Test if we have already inherited our interface parameters
     * @return boolean
     */
    public synchronized boolean inheritedParameters() {
        return this.inheritedParameters;
    }

    /**
     * Gets the ModelParam by name
     * @param name The name of the parameter to get
     * @return ModelParam object with the specified name
     */
    public ModelParam getParam(String name) {
        return contextInfo.get(name);
    }

    /**
     * Adds a parameter definition to this service; puts on list in order added
     * then sorts by order if specified.
     */
    public void addParam(ModelParam param) {
        if (param != null) {
            contextInfo.put(param.getName(), param);
            contextParamList.add(param);
        }
    }

    /**
     * Adds a clone of a parameter definition to this service
     */
    public void addParamClone(ModelParam param) {
        if (param != null) {
            ModelParam newParam = new ModelParam(param);
            addParam(newParam);
        }
    }

    /**
     * Gets all param names.
     * @return the all param names
     */
    public Set<String> getAllParamNames() {
        Set<String> nameList = new TreeSet<>();
        for (ModelParam p: this.contextParamList) {
            nameList.add(p.getName());
        }
        return nameList;
    }

    /**
     * Gets in param names.
     * @return the in param names
     */
    public Set<String> getInParamNames() {
        Set<String> nameList = new TreeSet<>();
        for (ModelParam p: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if (p.isIn()) {
                nameList.add(p.getName());
            }
        }
        return nameList;
    }
    /**
     * Creates a map of service IN parameters using Name as key and Type as value.
     * Skips internal parameters
     * @return Map of IN parameters
     */
    public Map<String, String> getInParamNamesMap() {
        // TODO : Does not yet support getting nested parameters
        return getInModelParamList().stream().filter(param -> !param.getInternal())
                .collect(Collectors.toMap(ModelParam::getName, param -> param.getType(), (existingValue, newValue) -> newValue));
    }

    /**
     * Creates a map of service OUT parameters using Name as key and Type as value.
     * Skips internal parameters
     * @return Map of OUT parameters
     */
    public Map<String, String> getOutParamNamesMap() {
        // TODO : Does not yet support getting nested parameters
        return getModelParamList().stream().filter(param -> param.isOut() && !param.getInternal())
                .collect(Collectors.toMap(ModelParam::getName, param -> param.getType(), (existingValue, newValue) -> newValue));
    }

    /**
     * Gets defined in count.
     * @return the defined in count
     */
    public int getDefinedInCount() {
        int count = 0;

        for (ModelParam p: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if (p.isIn() && !p.getInternal()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Gets out param names.
     * @return the out param names
     */
    public Set<String> getOutParamNames() {
        Set<String> nameList = new TreeSet<>();
        for (ModelParam p: this.contextParamList) {
            // don't include IN parameters in this list, only OUT and INOUT
            if (p.isOut()) {
                nameList.add(p.getName());
            }
        }
        return nameList;
    }

    /** only returns number of defined parameters (not internal) */
    public int getDefinedOutCount() {
        int count = 0;

        for (ModelParam p: this.contextParamList) {
            // don't include IN parameters in this list, only OUT and INOUT
            if (p.isOut() && !p.getInternal()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Update default values.
     * @param context the context
     * @param mode    the mode
     */
    public void updateDefaultValues(Map<String, Object> context, String mode) {
        List<ModelParam> params = this.getModelParamList();
        for (ModelParam param: params) {
            if (param.getDefaultValue() != null
                    && (IN_OUT_PARAM.equals(param.getMode()) || mode.equals(param.getMode()))) {
                Object defaultValueObj = param.getDefaultValue(context);
                if (defaultValueObj != null && context.get(param.getName()) == null) {
                    context.put(param.getName(), defaultValueObj);
                    Debug.logInfo("Set default value [" + defaultValueObj + "] for parameter [" + param.getName() + "]", MODULE);
                }
            }
        }
    }

    /**
     * Validates a Map against the IN or OUT parameter information
     * @param context the context
     * @param mode Test either mode IN or mode OUT
     * @param locale the actual locale to use
     */
    public void validate(Map<String, Object> context, String mode, Locale locale) throws ServiceValidationException {
        Map<String, String> requiredInfo = new HashMap<>();
        Map<String, String> optionalInfo = new HashMap<>();

        if (Debug.verboseOn()) {
            Debug.logVerbose("[ModelService.validate] : {" + this.name + "} : Validating context - " + context, MODULE);
        }

        // do not validate results with errors
        if (mode.equals(OUT_PARAM) && context != null && context.containsKey(RESPONSE_MESSAGE)) {
            if (RESPOND_ERROR.equals(context.get(RESPONSE_MESSAGE)) || RESPOND_FAIL.equals(context.get(RESPONSE_MESSAGE))) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[ModelService.validate] : {" + this.name + "} : response was an error, not validating.", MODULE);
                }
                return;
            }
        }

        // get the info values
        for (ModelParam modelParam: this.contextParamList) {
            if (IN_OUT_PARAM.equals(modelParam.getMode()) || mode.equals(modelParam.getMode())) {
                if (modelParam.isOptional()) {
                    optionalInfo.put(modelParam.getName(), modelParam.getType());
                } else {
                    requiredInfo.put(modelParam.getName(), modelParam.getType());
                }
            }
        }

        // get the test values
        Map<String, Object> requiredTest = new HashMap<>();
        Map<String, Object> optionalTest = new HashMap<>();

        if (context == null) {
            context = new HashMap<>();
        }
        requiredTest.putAll(context);

        List<String> requiredButNull = new LinkedList<>();
        List<String> keyList = new LinkedList<>();
        keyList.addAll(requiredTest.keySet());
        for (String key: keyList) {
            Object value = requiredTest.get(key);

            if (!requiredInfo.containsKey(key)) {
                requiredTest.remove(key);
                optionalTest.put(key, value);
            } else if (value == null) {
                requiredButNull.add(key);
            }
        }

        // check for requiredButNull fields and return an error since null values are not allowed for required fields
        if (!requiredButNull.isEmpty()) {
            List<String> missingMsg = new LinkedList<>();
            for (String missingKey: requiredButNull) {
                String message = this.getParam(missingKey).getPrimaryFailMessage(locale);
                if (message == null) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.getResource(),
                            "ModelService.following_required_parameter_missing", locale);
                    message = errMsg + " [" + this.name + "." + missingKey + "]";
                }
                missingMsg.add(message);
            }
            throw new ServiceValidationException(missingMsg, this, requiredButNull, null, mode);
        }

        if (Debug.verboseOn()) {
            StringBuilder requiredNames = new StringBuilder();

            for (String key: requiredInfo.keySet()) {
                if (requiredNames.length() > 0) {
                    requiredNames.append(", ");
                }
                requiredNames.append(key);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("[ModelService.validate] : required fields - " + requiredNames, MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("[ModelService.validate] : {" + name + "} : (" + mode + ") Required - "
                        + requiredTest.size() + " / " + requiredInfo.size(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("[ModelService.validate] : {" + name + "} : (" + mode + ") Optional - "
                        + optionalTest.size() + " / " + optionalInfo.size(), MODULE);
            }
        }
        try {
            validate(requiredInfo, requiredTest, true, this, mode, locale);
            validate(optionalInfo, optionalTest, false, this, mode, locale);
        } catch (ServiceValidationException e) {
            Debug.logError("[ModelService.validate] : {" + name + "} : (" + mode + ") Required test error: " + e.toString(), MODULE);
            throw e;
        }

        // required and type validation complete, do allow-html validation
        if (IN_PARAM.equals(mode)) {
            List<String> errorMessageList = new LinkedList<>();
            for (ModelParam modelParam : this.contextInfo.values()) {
                // the param is a String, allow-html is not any, and we are looking at an IN parameter during input parameter validation
                if (context.get(modelParam.getName()) != null && ("String".equals(modelParam.getType())
                        || "java.lang.String".equals(modelParam.getType()))
                        && !"any".equals(modelParam.getAllowHtml()) && (IN_OUT_PARAM.equals(modelParam.getMode())
                        || IN_PARAM.equals(modelParam.getMode()))) {
                    String value = (String) context.get(modelParam.getName());
                    if ("none".equals(modelParam.getAllowHtml())) {
                        UtilCodec.checkStringForHtmlStrictNone(modelParam.getName(), value, errorMessageList, (Locale) context.get("locale"));
                    } else if ("safe".equals(modelParam.getAllowHtml())) {
                        UtilCodec.checkStringForHtmlSafe(modelParam.getName(), value, errorMessageList,
                                (Locale) context.get("locale"),
                                EntityUtilProperties.getPropertyAsBoolean("owasp", "sanitizer.enable", true));
                    }
                }
            }
            if (!errorMessageList.isEmpty()) {
                throw new ServiceValidationException(errorMessageList, this, mode);
            }
        }
    }

    /**
     * Check a Map against the IN parameter information, uses the validate() method for that
     * Always called with only IN_PARAM, so to be called before the service is called with the passed context
     * @param context the passed context
     * @param locale the actual locale to use
     * @return boolean True is the service called with these IN_PARAM is valid
     */
    public boolean isValid(Map<String, Object> context, Locale locale) {
        try {
            validate(context, IN_PARAM, locale);
        } catch (ServiceValidationException e) {
            return false;
        }
        return true;
    }

    /**
     * Validates a map of name, object types to a map of name, objects
     * @param info The map of name, object types
     * @param test The map to test its value types.
     * @param reverse Test the maps in reverse.
     */
    public static void validate(Map<String, String> info, Map<String, ? extends Object> test, boolean reverse, ModelService model,
                                String mode, Locale locale) throws ServiceValidationException {
        if (info == null || test == null) {
            throw new ServiceValidationException("Cannot validate NULL maps", model);
        }

        // * Validate keys first
        Set<String> testSet = test.keySet();
        Set<String> keySet = info.keySet();

        // Quick check for sizes
        if (info.isEmpty() && test.isEmpty()) {
            return;
        }
        // This is to see if the test set contains all from the info set (reverse)
        if (reverse && !testSet.containsAll(keySet)) {
            Set<String> missing = new TreeSet<>(keySet);

            missing.removeAll(testSet);
            List<String> missingMsgs = new LinkedList<>();
            for (String key: missing) {
                String msg = model.getParam(key).getPrimaryFailMessage(locale);
                if (msg == null) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.getResource(), "ModelService.following_required_parameter_missing",
                            locale);
                    msg = errMsg + " [" + mode + "] [" + model.name + "." + key + "]";
                }
                missingMsgs.add(msg);
            }

            List<String> missingCopy = new LinkedList<>();
            missingCopy.addAll(missing);
            throw new ServiceValidationException(missingMsgs, model, missingCopy, null, mode);
        }

        // This is to see if the info set contains all from the test set
        if (!keySet.containsAll(testSet)) {
            Set<String> extra = new TreeSet<>(testSet);

            extra.removeAll(keySet);
            List<String> extraMsgs = new LinkedList<>();
            for (String key: extra) {
                ModelParam param = model.getParam(key);
                String msg = null;
                if (param != null) {
                    msg = param.getPrimaryFailMessage(locale);
                }
                if (msg == null) {
                    msg = "Unknown parameter found: [" + model.name + "." + key + "]";
                }
                extraMsgs.add(msg);
            }

            List<String> extraCopy = new LinkedList<>();
            extraCopy.addAll(extra);
            throw new ServiceValidationException(extraMsgs, model, null, extraCopy, mode);
        }

        // * Validate types next
        List<String> typeFailMsgs = new LinkedList<>();
        for (String key: testSet) {
            ModelParam param = model.getParam(key);

            Object testObject = test.get(key);
            String infoType = info.get(key);

            if (UtilValidate.isNotEmpty(param.getValidators())) {
                for (ModelParam.ModelParamValidator val: param.getValidators()) {
                    if (UtilValidate.isNotEmpty(val.getMethodName())) {
                        try {
                            if (!typeValidate(val, testObject)) {
                                String msg = val.getFailMessage(locale);
                                if (msg == null) {
                                    msg = "The following parameter failed validation: [" + model.name + "." + key + "]";
                                }
                                typeFailMsgs.add(msg);
                            }
                        } catch (GeneralException e) {
                            Debug.logError(e, MODULE);
                            String msg = param.getPrimaryFailMessage(locale);
                            if (msg == null) {
                                msg = "The following parameter failed validation: [" + model.name + "." + key + "]";
                            }
                            typeFailMsgs.add(msg);
                        }
                    } else {
                        if (!ObjectType.instanceOf(testObject, infoType, null)) {
                            String msg = val.getFailMessage(locale);
                            if (msg == null) {
                                msg = "The following parameter failed validation: [" + model.name + "." + key + "]";
                            }
                            typeFailMsgs.add(msg);
                        }
                    }
                }
            } else {
                if (!ObjectType.instanceOf(testObject, infoType, null)) {
                    String testType = testObject == null ? "null" : testObject.getClass().getName();
                    String msg = "Type check failed for field [" + model.name + "." + key + "]; expected type is [" + infoType
                            + "]; actual type is [" + testType + "]";
                    typeFailMsgs.add(msg);
                }
            }
        }

        if (!typeFailMsgs.isEmpty()) {
            throw new ServiceValidationException(typeFailMsgs, model, mode);
        }
    }

    public static boolean typeValidate(ModelParam.ModelParamValidator vali, Object testValue) throws GeneralException {
        // find the validator class
        Class<?> validatorClass = null;
        try {
            validatorClass = ObjectType.loadClass(vali.getClassName());
        } catch (ClassNotFoundException e) {
            Debug.logWarning(e, MODULE);
        }

        if (validatorClass == null) {
            throw new GeneralException("Unable to load validation class [" + vali.getClassName() + "]");
        }

        boolean foundObjectParam = true;

        Method validatorMethod = null;
        try {
            // try object type first
            validatorMethod = validatorClass.getMethod(vali.getMethodName(), Object.class);
        } catch (NoSuchMethodException e) {
            foundObjectParam = false;
            // next try string type
            try {
                validatorMethod = validatorClass.getMethod(vali.getMethodName(), String.class);
            } catch (NoSuchMethodException e2) {
                Debug.logWarning(e2, MODULE);
            }
        }

        if (validatorMethod == null) {
            throw new GeneralException("Unable to find validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName() + "]");
        }

        Object param;
        if (!foundObjectParam) {
            // convert to string
            String converted;
            try {
                converted = (String) ObjectType.simpleTypeOrObjectConvert(testValue, "String", null, null);
            } catch (GeneralException e) {
                throw new GeneralException("Unable to convert parameter to String");
            }
            param = converted;
        } else {
            // use plain object
            param = testValue;
        }

        // run the validator
        Boolean resultBool;
        try {
            resultBool = (Boolean) validatorMethod.invoke(null, param);
        } catch (ClassCastException e) {
            throw new GeneralException("Validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName()
                    + "] did not return expected Boolean");
        } catch (Exception e) {
            throw new GeneralException("Unable to run validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName() + "]");
        }

        return resultBool;
    }

    /**
     * Gets the parameter names of the specified mode (IN/OUT/INOUT). The
     * parameters will be returned in the order specified in the file.
     * Note: IN and OUT will also contains INOUT parameters.
     * @param mode The mode (IN/OUT/INOUT)
     * @param optional True if to include optional parameters
     * @param internal True to include internal parameters
     * @return List of parameter names
     */
    public List<String> getParameterNames(String mode, boolean optional, boolean internal) {
        List<String> names = new LinkedList<>();

        if (!IN_PARAM.equals(mode) && !OUT_PARAM.equals(mode) && !IN_OUT_PARAM.equals(mode)) {
            return names;
        }
        if (contextInfo.isEmpty()) {
            return names;
        }
        for (ModelParam param: contextParamList) {
            if (param.getMode().equals(IN_OUT_PARAM) || param.getMode().equals(mode)) {
                if (optional || !param.isOptional()) {
                    if (internal || !param.getInternal()) {
                        names.add(param.getName());
                    }
                }
            }
        }
        return names;
    }

    /**
     * Gets parameter names.
     * @param mode     the mode
     * @param optional the optional
     * @return the parameter names
     */
    public List<String> getParameterNames(String mode, boolean optional) {
        return this.getParameterNames(mode, optional, true);
    }

    /**
     * Creates a new Map based from an existing map with just valid parameters.
     * Tries to convert parameters to required type.
     * @param source The source map
     * @param mode The mode which to build the new map
     */
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode) {
        return makeValid(source, mode, true, null);
    }

    /**
     * Creates a new Map based from an existing map with just valid parameters.
     * Tries to convert parameters to required type.
     * @param source The source map
     * @param mode The mode which to build the new map
     * @param includeInternal When false will exclude internal fields
     */
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode, boolean includeInternal, List<Object> errorMessages) {
        return makeValid(source, mode, includeInternal, errorMessages, null);
    }

    /**
     * Creates a new Map based from an existing map with just valid parameters.
     * Tries to convert parameters to required type.
     * @param source The source map
     * @param mode The mode which to build the new map
     * @param includeInternal When false will exclude internal fields
     * @param locale Locale to use to do some type conversion
     */
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode, boolean includeInternal, List<Object> errorMessages,
                                         Locale locale) {
        return makeValid(source, mode, includeInternal, errorMessages, null, locale);
    }

    /**
     * Creates a new Map based from an existing map with just valid parameters.
     * Tries to convert parameters to required type.
     * @param source The source map
     * @param mode The mode which to build the new map
     * @param includeInternal When false will exclude internal fields
     * @param errorMessages the list of error messages
     * @param timeZone TimeZone to use to do some type conversion
     * @param locale Locale to use to do some type conversion
     */
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode, boolean includeInternal, List<Object> errorMessages,
                                         TimeZone timeZone, Locale locale) {
        Map<String, Object> target = new HashMap<>();

        if (source == null) {
            return target;
        }
        if (!IN_PARAM.equals(mode) && !OUT_PARAM.equals(mode) && !IN_OUT_PARAM.equals(mode)) {
            return target;
        }
        if (contextInfo.isEmpty()) {
            return target;
        }

        if (locale == null) {
            // if statement here to avoid warning messages for Entity ECA service input validation,
            // even though less efficient that doing a straight get
            if (source.containsKey("locale")) {
                locale = (Locale) source.get("locale");
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }
        }

        if (timeZone == null) {
            // if statement here to avoid warning messages for Entity ECA service input validation,
            // even though less efficient that doing a straight get
            if (source.containsKey("timeZone")) {
                timeZone = (TimeZone) source.get("timeZone");
            }
            if (timeZone == null) {
                timeZone = TimeZone.getDefault();
            }
        }

        for (ModelParam param: contextParamList) {
            if (param.getMode().equals(IN_OUT_PARAM) || param.getMode().equals(mode)) {
                String key = param.getName();

                // internal map of strings
                if (UtilValidate.isNotEmpty(param.getStringMapPrefix()) && !source.containsKey(key)) {
                    Map<String, Object> paramMap = makePrefixMap(source, param);
                    if (UtilValidate.isNotEmpty(paramMap)) {
                        target.put(key, paramMap);
                    }
                // internal list of strings
                } else if (UtilValidate.isNotEmpty(param.getStringListSuffix()) && !source.containsKey(key)) {
                    List<Object> paramList = makeSuffixList(source, param);
                    if (UtilValidate.isNotEmpty(paramList)) {
                        target.put(key, paramList);
                    }
                // other attributes
                } else {
                    if (source.containsKey(key)) {
                        if ((param.getInternal() && includeInternal) || (!param.getInternal())) {
                            Object value = source.get(key);

                            try {
                                // no need to fail on type conversion; the validator will catch this
                                value = ObjectType.simpleTypeOrObjectConvert(value, param.getType(), null, timeZone, locale, false);
                            } catch (GeneralException e) {
                                String errMsg = "Type conversion of field [" + key + "] to type [" + param.getType() + "] failed for value \""
                                        + value + "\": " + e.toString();
                                Debug.logWarning("[ModelService.makeValid] : " + errMsg, MODULE);
                                if (errorMessages != null) {
                                    errorMessages.add(errMsg);
                                }
                            }
                            target.put(key, value);
                        }
                    }
                }
            }
        }
        return target;
    }

    private static Map<String, Object> makePrefixMap(Map<String, ? extends Object> source, ModelParam param) {
        Map<String, Object> paramMap = new HashMap<>();
        for (Map.Entry<String, ? extends Object> entry: source.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(param.getStringMapPrefix())) {
                key = key.replace(param.getStringMapPrefix(), "");
                paramMap.put(key, entry.getValue());
            }
        }
        return paramMap;
    }

    private static List<Object> makeSuffixList(Map<String, ? extends Object> source, ModelParam param) {
        List<Object> paramList = new LinkedList<>();
        for (Map.Entry<String, ? extends Object> entry: source.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(param.getStringListSuffix())) {
                paramList.add(entry.getValue());
            }
        }
        return paramList;
    }

    /**
     * Contains permissions boolean.
     * @return the boolean
     */
    public boolean containsPermissions() {
        return (UtilValidate.isNotEmpty(this.permissionGroups));
    }

    /**
     * Evaluates permission-service for this service.
     * @param dctx DispatchContext from the invoked service
     * @param context Map containing userLogin and context information
     * @return result of permission service invocation
     */
    public Map<String, Object> evalPermission(DispatchContext dctx, Map<String, ? extends Object> context) {
        if (this.modelPermission != null) {
            return modelPermission.evalPermission(dctx, context);
        } else {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("hasPermission", Boolean.FALSE);
            result.put("failMessage", UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorDefinitionProblem", (Locale) context.get("locale")));
            return result;
        }
    }

    /**
     * Evaluates notifications
     */
    public void evalNotifications(DispatchContext dctx, Map<String, ? extends Object> context, Map<String, Object> result) {
        for (ModelNotification notify: this.notifications) {
            notify.callNotify(dctx, this, context, result);
        }
    }

    /**
     * Evaluates permissions for a service.
     * @param dctx DispatchContext from the invoked service
     * @param context Map containing userLogin information
     * @return Map if all permissions evaluate return success else return the error message list.
     */
    public Map<String, Object> evalPermissions(DispatchContext dctx, Map<String, ? extends Object> context) {
        List<String> permGroupErrors = new ArrayList<>();

        // old permission checking
        if (this.containsPermissions()) {
            for (ModelPermGroup group: this.permissionGroups) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose(" Permission : Analyse " + group.toString(), MODULE);
                }
                Map<String, Object> permResult = group.evalPermissions(dctx, context);
                if (!ServiceUtil.isSuccess(permResult)) {
                    ServiceUtil.addErrors(permGroupErrors, null, permResult);
                }
            }
        }
        if (UtilValidate.isEmpty(permGroupErrors)) {
            return ServiceUtil.returnSuccess();
        }
        return ServiceUtil.returnError(permGroupErrors);
    }

    /**
     * Gets a list of required IN parameters in sequence.
     * @return A list of required IN parameters in the order which they were defined.
     */
    public List<Object> getInParameterSequence(Map<String, ? extends Object> source) {
        List<Object> target = new LinkedList<>();
        if (source == null) {
            return target;
        }
        if (UtilValidate.isEmpty(contextInfo)) {
            return target;
        }
        for (ModelParam modelParam: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if (OUT_PARAM.equals(modelParam.getMode())) {
                continue;
            }

            Object srcObject = source.get(modelParam.getName());
            if (srcObject != null) {
                target.add(srcObject);
            }
        }
        return target;
    }

    /**
     * Returns a list of ModelParam objects in the order they were defined when
     * the service was created.
     */
    public List<ModelParam> getModelParamList() {
        List<ModelParam> newList = new LinkedList<>();
        newList.addAll(this.contextParamList);
        return newList;
    }

    /**
     * Returns a list of ModelParam objects in the order they were defined when
     * the service was created.
     */
    public List<ModelParam> getInModelParamList() {
        List<ModelParam> inList = new LinkedList<>();
        for (ModelParam modelParam: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if (OUT_PARAM.equals(modelParam.getMode())) {
                continue;
            }

            inList.add(modelParam);
        }
        return inList;
    }

    /**
     * Run the interface update and inherit all interface parameters
     * @param dctx The DispatchContext to use for service lookups
     */
    public synchronized void interfaceUpdate(DispatchContext dctx) throws GenericServiceException {
        if (!inheritedParameters) {
            // services w/ engine 'group' auto-implement the grouped services
            if ("group".equals(this.engineName) && implServices.isEmpty()) {
                GroupModel group = internalGroup;
                if (group == null) {
                    group = ServiceGroupReader.getGroupModel(this.location);
                }
                if (group != null) {
                    for (GroupServiceModel sm: group.getServices()) {
                        implServices.add(new ModelServiceIface(sm.getName(), sm.isOptional()));
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Adding service [" + sm.getName() + "] as interface of: [" + this.name + "]", MODULE);
                        }
                    }
                }
            }

            // handle interfaces
            if (UtilValidate.isNotEmpty(implServices) && dctx != null) {
                for (ModelServiceIface iface: implServices) {
                    String serviceName = iface.getService();
                    boolean optional = iface.isOptional();

                    ModelService model = dctx.getModelService(serviceName);
                    if (model != null) {
                        for (ModelParam newParam: model.contextParamList) {
                            ModelParam existingParam = this.contextInfo.get(newParam.getName());
                            if (existingParam != null) {
                                // if the existing param is not INOUT and the newParam.mode is different from existingParam.mode,
                                // make the existing param optional and INOUT
                                // TODO: this is another case where having different optional/required settings for IN and OUT
                                //  would be quite valuable...
                                if (!IN_OUT_PARAM.equals(existingParam.getMode()) && !existingParam.getMode().equals(newParam.getMode())) {
                                    existingParam.setMode(IN_OUT_PARAM);
                                    if (existingParam.isOptional() || newParam.isOptional()) {
                                        existingParam.setOptional(true);
                                    }
                                }
                            } else {
                                ModelParam newParamClone = new ModelParam(newParam);
                                if (optional) {
                                    // default option is to make this optional, however the service can override and
                                    // force the clone to use the parents setting.
                                    newParamClone.setOptional(true);
                                }
                                this.addParam(newParamClone);
                            }
                        }
                    } else {
                        Debug.logWarning("Inherited model [" + serviceName + "] not found for [" + this.name + "]", MODULE);
                    }
                }
            }

            // handle any override parameters
            if (UtilValidate.isNotEmpty(overrideParameters)) {
                for (ModelParam overrideParam: overrideParameters) {
                    ModelParam existingParam = contextInfo.get(overrideParam.getName());

                    // keep the list clean, remove it then add it back
                    contextParamList.remove(existingParam);

                    if (existingParam != null) {
                        // now re-write the parameters
                        if (UtilValidate.isNotEmpty(overrideParam.getType())) {
                            existingParam.setType(overrideParam.getType());
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.getMode())) {
                            existingParam.setMode(overrideParam.getMode());
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.getEntityName())) {
                            existingParam.setEntityName(overrideParam.getEntityName());
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.getFieldName())) {
                            existingParam.setFieldName(overrideParam.getFieldName());
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.getFormLabel())) {
                            existingParam.setFormLabel(overrideParam.getFormLabel());
                        }
                        if (overrideParam.getDefaultValue() != null) {
                            existingParam.copyDefaultValue(overrideParam);
                        }
                        if (overrideParam.isOverrideFormDisplay()) {
                            existingParam.setFormDisplay(overrideParam.isFormDisplay());
                        }
                        if (overrideParam.isOverrideOptional()) {
                            existingParam.setOptional(overrideParam.isOptional());
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.getAllowHtml())) {
                            existingParam.setAllowHtml(overrideParam.getAllowHtml());
                        }
                        addParam(existingParam);
                    } else {
                        Debug.logWarning("Override param found but no parameter existing; ignoring: " + overrideParam.getName(), MODULE);
                    }
                }
            }

            // set the flag so we don't do this again
            this.inheritedParameters = true;
        }
    }

    /**
     * if the service is declare as deprecated, create a log warning with the reason
     */
    public void informIfDeprecated() {
        if (this.deprecatedUseInstead != null) {
            StringBuilder informMsg = new StringBuilder("DEPRECATED: the service ")
                    .append(name).append(" has been deprecated and replaced by ").append(deprecatedUseInstead);
            if (this.deprecatedSince != null) {
                informMsg.append(", since ").append(deprecatedSince);
            }
            if (deprecatedReason != null) {
                informMsg.append(" because '").append(deprecatedReason).append("'");
            }
            Debug.logWarning(informMsg.toString(), MODULE);
        }
    }

    /**
     * To wsdl document.
     * @param locationURI the location uri
     * @return the document
     * @throws WSDLException the wsdl exception
     */
    public Document toWSDL(String locationURI) throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        Definition def = factory.newDefinition();
        def.setTargetNamespace(TNS);
        def.addNamespace("xsd", XSD);
        def.addNamespace("tns", TNS);
        def.addNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        this.getWSDL(def, locationURI);
        return factory.newWSDLWriter().getDocument(def);
    }

    /**
     * Gets wsdl.
     * @param def         the def
     * @param locationURI the location uri
     * @throws WSDLException the wsdl exception
     */
    public void getWSDL(Definition def, String locationURI) throws WSDLException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document document = null;
        try {
            builder = factory.newDocumentBuilder();
            document = builder.newDocument();
        } catch (Exception e) {
            throw new WSDLException("can not create WSDL", MODULE);
        }
        def.setTypes(this.getTypes(document, def));

        // set the IN parameters
        Input input = def.createInput();
        Set<String> inParam = this.getInParamNames();
        Message inMessage = def.createMessage();
        inMessage.setQName(new QName(TNS, this.name + "Request"));
        inMessage.setUndefined(false);
        Part parametersPart = def.createPart();
        parametersPart.setName("map-Map");
        parametersPart.setTypeName(new QName(TNS, "map-Map"));
        inMessage.addPart(parametersPart);
        Element documentation = document.createElement("wsdl:documentation");
        for (String paramName : inParam) {
            ModelParam param = this.getParam(paramName);
            if (!param.getInternal()) {
                Part part = param.getWSDLPart(def);
                Element attribute = document.createElement("attribute");
                attribute.setAttribute("name", paramName);
                attribute.setAttribute("type", part.getTypeName().getLocalPart());
                attribute.setAttribute("namespace", part.getTypeName().getNamespaceURI());
                attribute.setAttribute("java-class", param.getType());
                attribute.setAttribute("optional", Boolean.toString(param.isOptional()));
                documentation.appendChild(attribute);
            }
        }
        Element usernameAttr = document.createElement("attribute");
        usernameAttr.setAttribute("name", "login.username");
        usernameAttr.setAttribute("type", "std-String");
        usernameAttr.setAttribute("namespace", TNS);
        usernameAttr.setAttribute("java-class", String.class.getName());
        usernameAttr.setAttribute("optional", Boolean.toString(!this.auth));
        documentation.appendChild(usernameAttr);

        Element passwordAttr = document.createElement("attribute");
        passwordAttr.setAttribute("name", "login.password");
        passwordAttr.setAttribute("type", "std-String");
        passwordAttr.setAttribute("namespace", TNS);
        passwordAttr.setAttribute("java-class", String.class.getName());
        passwordAttr.setAttribute("optional", Boolean.toString(!this.auth));
        documentation.appendChild(passwordAttr);

        parametersPart.setDocumentationElement(documentation);
        def.addMessage(inMessage);
        input.setMessage(inMessage);

        // set the OUT parameters
        Output output = def.createOutput();
        Set<String> outParam = this.getOutParamNames();
        Message outMessage = def.createMessage();
        outMessage.setQName(new QName(TNS, this.name + "Response"));
        outMessage.setUndefined(false);
        Part resultsPart = def.createPart();
        resultsPart.setName("map-Map");
        resultsPart.setTypeName(new QName(TNS, "map-Map"));
        outMessage.addPart(resultsPart);
        documentation = document.createElement("wsdl:documentation");
        for (String paramName : outParam) {
            ModelParam param = this.getParam(paramName);
            if (!param.getInternal()) {
                Part part = param.getWSDLPart(def);
                Element attribute = document.createElement("attribute");
                attribute.setAttribute("name", paramName);
                attribute.setAttribute("type", part.getTypeName().getLocalPart());
                attribute.setAttribute("namespace", part.getTypeName().getNamespaceURI());
                attribute.setAttribute("java-class", param.getType());
                attribute.setAttribute("optional", Boolean.toString(param.isOptional()));
                documentation.appendChild(attribute);
            }
        }
        resultsPart.setDocumentationElement(documentation);
        def.addMessage(outMessage);
        output.setMessage(outMessage);

        // set port type
        Operation operation = def.createOperation();
        operation.setName(this.name);
        operation.setUndefined(false);
        operation.setOutput(output);
        operation.setInput(input);

        PortType portType = def.createPortType();
        portType.setQName(new QName(TNS, this.name + "PortType"));
        portType.addOperation(operation);
        portType.setUndefined(false);
        def.addPortType(portType);

        // SOAP binding
        SOAPBinding soapBinding = new SOAPBindingImpl();
        soapBinding.setStyle("rpc");
        soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");

        Binding binding = def.createBinding();
        binding.setQName(new QName(TNS, this.name + "SoapBinding"));
        binding.setPortType(portType);
        binding.setUndefined(false);
        binding.addExtensibilityElement(soapBinding);

        BindingOperation bindingOperation = def.createBindingOperation();
        bindingOperation.setName(operation.getName());
        bindingOperation.setOperation(operation);

        SOAPBody soapBody = new SOAPBodyImpl();
        soapBody.setUse("literal");
        soapBody.setNamespaceURI(TNS);
        soapBody.setEncodingStyles(UtilMisc.toList("http://schemas.xmlsoap.org/soap/encoding/"));

        BindingOutput bindingOutput = def.createBindingOutput();
        bindingOutput.addExtensibilityElement(soapBody);
        bindingOperation.setBindingOutput(bindingOutput);

        BindingInput bindingInput = def.createBindingInput();
        bindingInput.addExtensibilityElement(soapBody);
        bindingOperation.setBindingInput(bindingInput);

        SOAPOperation soapOperation = new SOAPOperationImpl();
        // soapAction should be set to the location of the SOAP URI, or Visual Studio won't construct the correct SOAP message
        soapOperation.setSoapActionURI(locationURI);
        // this is the RPC/literal style.  See http://www.ibm.com/developerworks/webservices/library/ws-whichwsdl/
        // this parameter is necessary or Apache Synapse won't recognize the WSDL
        soapOperation.setStyle("rpc");
        bindingOperation.addExtensibilityElement(soapOperation);

        binding.addBindingOperation(bindingOperation);
        def.addBinding(binding);

        // Service port
        Port port = def.createPort();
        port.setBinding(binding);
        port.setName(this.name + "Port");

        if (locationURI != null) {
            SOAPAddress soapAddress = new SOAPAddressImpl();
            soapAddress.setLocationURI(locationURI);
            port.addExtensibilityElement(soapAddress);
        }

        Service service = def.createService();
        service.setQName(new QName(TNS, this.name));
        service.addPort(port);
        def.addService(service);
    }

    /**
     * Gets types.
     * @param document the document
     * @param def the def
     * @return the types
     */
    public Types getTypes(Document document, Definition def) {
        Types types = def.createTypes();
        /* Schema */
        Element schema = document.createElement("xsd:schema");
        schema.setAttribute("targetNamespace", TNS);

        /*-----------------------------------*/
        /*--------- Standard Objects --------*/
        /*-----------------------------------*/

        /* null Element */
        Element stdNullElement = document.createElement("xsd:element");
        stdNullElement.setAttribute("name", "null");
        stdNullElement.setAttribute("nillable", "true");
        Element stdNullElement0 = document.createElement("xsd:complexType");
        stdNullElement.appendChild(stdNullElement0);
        Element stdNullElement1 = document.createElement("xsd:attribute");
        stdNullElement0.appendChild(stdNullElement1);
        stdNullElement1.setAttribute("name", "value");
        stdNullElement1.setAttribute("type", "xsd:string");
        stdNullElement1.setAttribute("use", "required");
        schema.appendChild(stdNullElement);
        /* std-String Element */
        Element stdStringElement = document.createElement("xsd:element");
        stdStringElement.setAttribute("name", "std-String");
        Element stdStringElement0 = document.createElement("xsd:complexType");
        stdStringElement.appendChild(stdStringElement0);
        Element stdStringElement1 = document.createElement("xsd:attribute");
        stdStringElement0.appendChild(stdStringElement1);
        stdStringElement1.setAttribute("name", "value");
        stdStringElement1.setAttribute("type", "xsd:string");
        stdStringElement1.setAttribute("use", "required");
        schema.appendChild(stdStringElement);
        /* std-Integer Element */
        Element stdIntegerElement = document.createElement("xsd:element");
        stdIntegerElement.setAttribute("name", "std-Integer");
        Element stdIntegerElement0 = document.createElement("xsd:complexType");
        stdIntegerElement.appendChild(stdIntegerElement0);
        Element stdIntegerElement1 = document.createElement("xsd:attribute");
        stdIntegerElement0.appendChild(stdIntegerElement1);
        stdIntegerElement1.setAttribute("name", "value");
        stdIntegerElement1.setAttribute("type", "xsd:integer");
        stdIntegerElement1.setAttribute("use", "required");
        schema.appendChild(stdIntegerElement);
        /* std-Long Element */
        Element stdLongElement = document.createElement("xsd:element");
        stdLongElement.setAttribute("name", "std-Long");
        Element stdLongElement0 = document.createElement("xsd:complexType");
        stdLongElement.appendChild(stdLongElement0);
        Element stdLongElement1 = document.createElement("xsd:attribute");
        stdLongElement0.appendChild(stdLongElement1);
        stdLongElement1.setAttribute("name", "value");
        stdLongElement1.setAttribute("type", "xsd:long");
        stdLongElement1.setAttribute("use", "required");
        schema.appendChild(stdLongElement);
        /* std-Float Element */
        Element stdFloatElement = document.createElement("xsd:element");
        stdFloatElement.setAttribute("name", "std-Float");
        Element stdFloatElement0 = document.createElement("xsd:complexType");
        stdFloatElement.appendChild(stdFloatElement0);
        Element stdFloatElement1 = document.createElement("xsd:attribute");
        stdFloatElement0.appendChild(stdFloatElement1);
        stdFloatElement1.setAttribute("name", "value");
        stdFloatElement1.setAttribute("type", "xsd:float");
        stdFloatElement1.setAttribute("use", "required");
        schema.appendChild(stdFloatElement);
        /* std-Double Element */
        Element stdDoubleElement = document.createElement("xsd:element");
        stdDoubleElement.setAttribute("name", "std-Double");
        Element stdDoubleElement0 = document.createElement("xsd:complexType");
        stdDoubleElement.appendChild(stdDoubleElement0);
        Element stdDoubleElement1 = document.createElement("xsd:attribute");
        stdDoubleElement0.appendChild(stdDoubleElement1);
        stdDoubleElement1.setAttribute("name", "value");
        stdDoubleElement1.setAttribute("type", "xsd:double");
        stdDoubleElement1.setAttribute("use", "required");
        schema.appendChild(stdDoubleElement);
        /* std-Boolean Element */
        Element stdBooleanElement = document.createElement("xsd:element");
        stdBooleanElement.setAttribute("name", "std-Boolean");
        Element stdBooleanElement0 = document.createElement("xsd:complexType");
        stdBooleanElement.appendChild(stdBooleanElement0);
        Element stdBooleanElement1 = document.createElement("xsd:attribute");
        stdBooleanElement0.appendChild(stdBooleanElement1);
        stdBooleanElement1.setAttribute("name", "value");
        stdBooleanElement1.setAttribute("type", "xsd:boolean");
        stdBooleanElement1.setAttribute("use", "required");
        schema.appendChild(stdBooleanElement);
        /* std-Locale Element */
        Element stdLocaleElement = document.createElement("xsd:element");
        stdLocaleElement.setAttribute("name", "std-Locale");
        Element stdLocaleElement0 = document.createElement("xsd:complexType");
        stdLocaleElement.appendChild(stdLocaleElement0);
        Element stdLocaleElement1 = document.createElement("xsd:attribute");
        stdLocaleElement0.appendChild(stdLocaleElement1);
        stdLocaleElement1.setAttribute("name", "value");
        stdLocaleElement1.setAttribute("type", "xsd:string");
        stdLocaleElement1.setAttribute("use", "required");
        schema.appendChild(stdLocaleElement);
        /* std-BigDecimal Element */
        Element stdBigDecimalElement = document.createElement("xsd:element");
        stdBigDecimalElement.setAttribute("name", "std-BigDecimal");
        Element stdBigDecimalElement0 = document.createElement("xsd:complexType");
        stdBigDecimalElement.appendChild(stdBigDecimalElement0);
        Element stdBigDecimalElement1 = document.createElement("xsd:attribute");
        stdBigDecimalElement0.appendChild(stdBigDecimalElement1);
        stdBigDecimalElement1.setAttribute("name", "value");
        stdBigDecimalElement1.setAttribute("type", "xsd:decimal");
        stdBigDecimalElement1.setAttribute("use", "required");
        schema.appendChild(stdBigDecimalElement);

        /*-----------------------------------*/
        /*----------- SQL Objects -----------*/
        /*-----------------------------------*/

        /* sql-Timestamp Element */
        Element sqlTimestampElement = document.createElement("xsd:element");
        sqlTimestampElement.setAttribute("name", "sql-Timestamp");
        Element sqlTimestampElement0 = document.createElement("xsd:complexType");
        sqlTimestampElement.appendChild(sqlTimestampElement0);
        Element sqlTimestampElement1 = document.createElement("xsd:attribute");
        sqlTimestampElement0.appendChild(sqlTimestampElement1);
        sqlTimestampElement1.setAttribute("name", "value");
        sqlTimestampElement1.setAttribute("type", "xsd:dateTime");
        sqlTimestampElement1.setAttribute("use", "required");
        schema.appendChild(sqlTimestampElement);
        /* sql-Date Element */
        Element sqlDateElement = document.createElement("xsd:element");
        sqlDateElement.setAttribute("name", "sql-Date");
        Element sqlDateElement0 = document.createElement("xsd:complexType");
        sqlDateElement.appendChild(sqlDateElement0);
        Element sqlDateElement1 = document.createElement("xsd:attribute");
        sqlDateElement0.appendChild(sqlDateElement1);
        sqlDateElement1.setAttribute("name", "value");
        sqlDateElement1.setAttribute("type", "xsd:date");
        sqlDateElement1.setAttribute("use", "required");
        schema.appendChild(sqlDateElement);
        /* sql-Time Element */
        Element sqlTimeElement = document.createElement("xsd:element");
        sqlTimeElement.setAttribute("name", "sql-Time");
        Element sqlTimeElement0 = document.createElement("xsd:complexType");
        sqlTimeElement.appendChild(sqlTimeElement0);
        Element sqlTimeElement1 = document.createElement("xsd:attribute");
        sqlTimeElement0.appendChild(sqlTimeElement1);
        sqlTimeElement1.setAttribute("name", "value");
        sqlTimeElement1.setAttribute("type", "xsd:time");
        sqlTimeElement1.setAttribute("use", "required");
        schema.appendChild(sqlTimeElement);

        /*-----------------------------------*/
        /*----------- List Objects -----------*/
        /*-----------------------------------*/

        /* col-ArrayList Element */
        Element colArrayListElement = document.createElement("xsd:element");
        colArrayListElement.setAttribute("name", "col-ArrayList");
        colArrayListElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colArrayListElement);
        /* col-LinkedList Element */
        Element colLinkedListElement = document.createElement("xsd:element");
        colLinkedListElement.setAttribute("name", "col-LinkedList");
        colLinkedListElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colLinkedListElement);
        /* col-Stack Element */
        Element colStackElement = document.createElement("xsd:element");
        colStackElement.setAttribute("name", "col-Stack");
        colStackElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colStackElement);
        /* col-Vector Element */
        Element colVectorElement = document.createElement("xsd:element");
        colVectorElement.setAttribute("name", "col-Vector");
        colVectorElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colVectorElement);
        /* col-TreeSet Element */
        Element colTreeSetElement = document.createElement("xsd:element");
        colTreeSetElement.setAttribute("name", "col-TreeSet");
        colTreeSetElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colTreeSetElement);
        /* col-HashSet Element */
        Element colHashSetElement = document.createElement("xsd:element");
        colHashSetElement.setAttribute("name", "col-HashSet");
        colHashSetElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colHashSetElement);
        /* col-Collection Element */
        Element colCollectionElement = document.createElement("xsd:element");
        colCollectionElement.setAttribute("name", "col-Collection");
        colCollectionElement.setAttribute("type", "tns:col-Collection");
        schema.appendChild(colCollectionElement);

        /*-----------------------------------*/
        /*----------- Map Objects -----------*/
        /*-----------------------------------*/

        /* map-TreeMap Element */
        Element mapTreeMapElement = document.createElement("xsd:element");
        mapTreeMapElement.setAttribute("name", "map-TreeMap");
        mapTreeMapElement.setAttribute("type", "tns:map-Map");
        schema.appendChild(mapTreeMapElement);
        /* map-WeakHashMap Element */
        Element mapWeakHashMapElement = document.createElement("xsd:element");
        mapWeakHashMapElement.setAttribute("name", "map-WeakHashMap");
        mapWeakHashMapElement.setAttribute("type", "tns:map-Map");
        schema.appendChild(mapWeakHashMapElement);
        /* map-Hashtable Element */
        Element mapHashtableElement = document.createElement("xsd:element");
        mapHashtableElement.setAttribute("name", "map-Hashtable");
        mapHashtableElement.setAttribute("type", "tns:map-Map");
        schema.appendChild(mapHashtableElement);
        /* map-Properties Element */
        Element mapPropertiesElement = document.createElement("xsd:element");
        mapPropertiesElement.setAttribute("name", "map-Properties");
        mapPropertiesElement.setAttribute("type", "tns:map-Map");
        schema.appendChild(mapPropertiesElement);
        /* map-HashMap Element */
        Element mapHashMapElement = document.createElement("xsd:element");
        mapHashMapElement.setAttribute("name", "map-HashMap");
        mapHashMapElement.setAttribute("type", "tns:map-Map");
        schema.appendChild(mapHashMapElement);
        /* map-Map Element */
        Element mapMapElement = document.createElement("xsd:element");
        mapMapElement.setAttribute("name", "map-Map");
        mapMapElement.setAttribute("type", "tns:map-Map");
        schema.appendChild(mapMapElement);
        /* map-Entry Element */
        Element mapEntryElement = document.createElement("xsd:element");
        mapEntryElement.setAttribute("name", "map-Entry");
        mapEntryElement.setAttribute("type", "tns:map-Entry");
        schema.appendChild(mapEntryElement);
        /* map-Key Element */
        Element mapKeyElement = document.createElement("xsd:element");
        mapKeyElement.setAttribute("name", "map-Key");
        mapKeyElement.setAttribute("type", "tns:map-Key");
        schema.appendChild(mapKeyElement);
        /* map-Value Element */
        Element mapValueElement = document.createElement("xsd:element");
        mapValueElement.setAttribute("name", "map-Value");
        mapValueElement.setAttribute("type", "tns:map-Value");
        schema.appendChild(mapValueElement);
        /* eepk- Element */
        Element eepkElement = document.createElement("xsd:element");
        eepkElement.setAttribute("name", "eepk-");
        eepkElement.setAttribute("type", "tns:map-Map");
        Element eepkElement0 = document.createElement("xsd:annotation");
        eepkElement.appendChild(eepkElement0);
        Element eepkElement1 = document.createElement("xsd:documentation");
        eepkElement0.appendChild(eepkElement1);
        eepkElement1.setTextContent("The name of element need to be appended with name of entity such as eepk-Product for Product entity.");
        schema.appendChild(eepkElement);
        /* eeval- Element */
        Element eevalElement = document.createElement("xsd:element");
        eevalElement.setAttribute("name", "eeval-");
        eevalElement.setAttribute("type", "tns:map-Map");
        Element eevalElement0 = document.createElement("xsd:annotation");
        eevalElement.appendChild(eevalElement0);
        Element eevalElement1 = document.createElement("xsd:documentation");
        eevalElement0.appendChild(eevalElement1);
        eevalElement1.setTextContent("The name of element need to be appended with name of entity such as eeval-Product for Product entity.");
        schema.appendChild(eevalElement);

        /*-----------------------------------*/
        /*----------- Custom Objects -----------*/
        /*-----------------------------------*/

        /* cus-obj Element */
        Element cusObjElement = document.createElement("xsd:element");
        cusObjElement.setAttribute("name", "cus-obj");
        Element cusObjElement0 = document.createElement("xsd:annotation");
        cusObjElement.appendChild(cusObjElement0);
        Element cusObjElement1 = document.createElement("xsd:documentation");
        cusObjElement0.appendChild(cusObjElement1);
        cusObjElement1.setTextContent("Object content is hex encoded so does not need to be in a CDATA block.");
        schema.appendChild(cusObjElement);

        /*-----------------------------------*/
        /*---------- Complex Types ----------*/
        /*-----------------------------------*/

        /* map-Map Complex Type */
        Element mapMapComplexType = document.createElement("xsd:complexType");
        mapMapComplexType.setAttribute("name", "map-Map");
        Element mapMapComplexType0 = document.createElement("xsd:sequence");
        mapMapComplexType.appendChild(mapMapComplexType0);
        Element mapMapComplexType1 = document.createElement("xsd:element");
        mapMapComplexType1.setAttribute("ref", "tns:map-Entry");
        mapMapComplexType1.setAttribute("minOccurs", "0");
        mapMapComplexType1.setAttribute("maxOccurs", "unbounded");
        mapMapComplexType0.appendChild(mapMapComplexType1);
        schema.appendChild(mapMapComplexType);
        /* map-Entry Complex Type */
        Element mapEntryComplexType = document.createElement("xsd:complexType");
        mapEntryComplexType.setAttribute("name", "map-Entry");
        Element mapEntryComplexType0 = document.createElement("xsd:sequence");
        mapEntryComplexType.appendChild(mapEntryComplexType0);
        Element mapEntryComplexType1 = document.createElement("xsd:element");
        mapEntryComplexType1.setAttribute("ref", "tns:map-Key");
        mapEntryComplexType1.setAttribute("minOccurs", "1");
        mapEntryComplexType1.setAttribute("maxOccurs", "1");
        mapEntryComplexType0.appendChild(mapEntryComplexType1);
        Element mapEntryComplexType2 = document.createElement("xsd:element");
        mapEntryComplexType2.setAttribute("ref", "tns:map-Value");
        mapEntryComplexType2.setAttribute("minOccurs", "1");
        mapEntryComplexType2.setAttribute("maxOccurs", "1");
        mapEntryComplexType0.appendChild(mapEntryComplexType2);
        schema.appendChild(mapEntryComplexType);
        /* map-Key Complex Type */
        Element mapKeyComplexType = document.createElement("xsd:complexType");
        mapKeyComplexType.setAttribute("name", "map-Key");
        Element mapKeyComplexType0 = document.createElement("xsd:all");
        mapKeyComplexType.appendChild(mapKeyComplexType0);
        Element mapKeyComplexType1 = document.createElement("xsd:element");
        mapKeyComplexType1.setAttribute("ref", "tns:std-String");
        mapKeyComplexType1.setAttribute("minOccurs", "1");
        mapKeyComplexType1.setAttribute("maxOccurs", "1");
        mapKeyComplexType0.appendChild(mapKeyComplexType1);
        schema.appendChild(mapKeyComplexType);
        /* map-Value Complex Type */
        Element mapValueComplexType = document.createElement("xsd:complexType");
        mapValueComplexType.setAttribute("name", "map-Value");
        Element mapValueComplexType0 = document.createElement("xsd:choice");
        mapValueComplexType.appendChild(mapValueComplexType0);
        Element mapValueComplexTypeNull = document.createElement("xsd:element");
        mapValueComplexTypeNull.setAttribute("ref", "tns:null");
        mapValueComplexTypeNull.setAttribute("minOccurs", "1");
        mapValueComplexTypeNull.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexTypeNull);
        Element mapValueComplexType1 = document.createElement("xsd:element");
        mapValueComplexType1.setAttribute("ref", "tns:std-String");
        mapValueComplexType1.setAttribute("minOccurs", "1");
        mapValueComplexType1.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType1);
        Element mapValueComplexType2 = document.createElement("xsd:element");
        mapValueComplexType2.setAttribute("ref", "tns:std-Integer");
        mapValueComplexType2.setAttribute("minOccurs", "1");
        mapValueComplexType2.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType2);
        Element mapValueComplexType3 = document.createElement("xsd:element");
        mapValueComplexType3.setAttribute("ref", "tns:std-Long");
        mapValueComplexType3.setAttribute("minOccurs", "1");
        mapValueComplexType3.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType3);
        Element mapValueComplexType4 = document.createElement("xsd:element");
        mapValueComplexType4.setAttribute("ref", "tns:std-Float");
        mapValueComplexType4.setAttribute("minOccurs", "1");
        mapValueComplexType4.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType4);
        Element mapValueComplexType5 = document.createElement("xsd:element");
        mapValueComplexType5.setAttribute("ref", "tns:std-Double");
        mapValueComplexType5.setAttribute("minOccurs", "1");
        mapValueComplexType5.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType5);
        Element mapValueComplexType6 = document.createElement("xsd:element");
        mapValueComplexType6.setAttribute("ref", "tns:std-Boolean");
        mapValueComplexType6.setAttribute("minOccurs", "1");
        mapValueComplexType6.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType6);
        Element mapValueComplexType7 = document.createElement("xsd:element");
        mapValueComplexType7.setAttribute("ref", "tns:std-Locale");
        mapValueComplexType7.setAttribute("minOccurs", "1");
        mapValueComplexType7.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType7);
        Element mapValueComplexType8 = document.createElement("xsd:element");
        mapValueComplexType8.setAttribute("ref", "tns:sql-Timestamp");
        mapValueComplexType8.setAttribute("minOccurs", "1");
        mapValueComplexType8.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType8);
        Element mapValueComplexType9 = document.createElement("xsd:element");
        mapValueComplexType9.setAttribute("ref", "tns:sql-Date");
        mapValueComplexType9.setAttribute("minOccurs", "1");
        mapValueComplexType9.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType9);
        Element mapValueComplexType10 = document.createElement("xsd:element");
        mapValueComplexType10.setAttribute("ref", "tns:sql-Time");
        mapValueComplexType10.setAttribute("minOccurs", "1");
        mapValueComplexType10.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType10);
        Element mapValueComplexType11 = document.createElement("xsd:element");
        mapValueComplexType11.setAttribute("ref", "tns:col-ArrayList");
        mapValueComplexType11.setAttribute("minOccurs", "1");
        mapValueComplexType11.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType11);
        Element mapValueComplexType12 = document.createElement("xsd:element");
        mapValueComplexType12.setAttribute("ref", "tns:col-LinkedList");
        mapValueComplexType12.setAttribute("minOccurs", "1");
        mapValueComplexType12.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType12);
        Element mapValueComplexType13 = document.createElement("xsd:element");
        mapValueComplexType13.setAttribute("ref", "tns:col-Stack");
        mapValueComplexType13.setAttribute("minOccurs", "1");
        mapValueComplexType13.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType13);
        Element mapValueComplexType14 = document.createElement("xsd:element");
        mapValueComplexType14.setAttribute("ref", "tns:col-Vector");
        mapValueComplexType14.setAttribute("minOccurs", "1");
        mapValueComplexType14.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType14);
        Element mapValueComplexType15 = document.createElement("xsd:element");
        mapValueComplexType15.setAttribute("ref", "tns:col-TreeSet");
        mapValueComplexType15.setAttribute("minOccurs", "1");
        mapValueComplexType15.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType15);
        Element mapValueComplexType16 = document.createElement("xsd:element");
        mapValueComplexType16.setAttribute("ref", "tns:col-HashSet");
        mapValueComplexType16.setAttribute("minOccurs", "1");
        mapValueComplexType16.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType16);
        Element mapValueComplexType17 = document.createElement("xsd:element");
        mapValueComplexType17.setAttribute("ref", "tns:col-Collection");
        mapValueComplexType17.setAttribute("minOccurs", "1");
        mapValueComplexType17.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType17);
        Element mapValueComplexType18 = document.createElement("xsd:element");
        mapValueComplexType18.setAttribute("ref", "tns:map-HashMap");
        mapValueComplexType18.setAttribute("minOccurs", "1");
        mapValueComplexType18.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType18);
        Element mapValueComplexType19 = document.createElement("xsd:element");
        mapValueComplexType19.setAttribute("ref", "tns:map-Properties");
        mapValueComplexType19.setAttribute("minOccurs", "1");
        mapValueComplexType19.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType19);
        Element mapValueComplexType20 = document.createElement("xsd:element");
        mapValueComplexType20.setAttribute("ref", "tns:map-Hashtable");
        mapValueComplexType20.setAttribute("minOccurs", "1");
        mapValueComplexType20.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType20);
        Element mapValueComplexType21 = document.createElement("xsd:element");
        mapValueComplexType21.setAttribute("ref", "tns:map-WeakHashMap");
        mapValueComplexType21.setAttribute("minOccurs", "1");
        mapValueComplexType21.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType21);
        Element mapValueComplexType22 = document.createElement("xsd:element");
        mapValueComplexType22.setAttribute("ref", "tns:map-TreeMap");
        mapValueComplexType22.setAttribute("minOccurs", "1");
        mapValueComplexType22.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType22);
        Element mapValueComplexType23 = document.createElement("xsd:element");
        mapValueComplexType23.setAttribute("ref", "tns:map-Map");
        mapValueComplexType23.setAttribute("minOccurs", "1");
        mapValueComplexType23.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType23);
        Element mapValueComplexType24 = document.createElement("xsd:element");
        mapValueComplexType24.setAttribute("ref", "tns:eepk-");
        mapValueComplexType24.setAttribute("minOccurs", "1");
        mapValueComplexType24.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType24);
        Element mapValueComplexType25 = document.createElement("xsd:element");
        mapValueComplexType25.setAttribute("ref", "tns:eeval-");
        mapValueComplexType25.setAttribute("minOccurs", "1");
        mapValueComplexType25.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType25);
        schema.appendChild(mapValueComplexType);
        Element mapValueComplexType26 = document.createElement("xsd:element");
        mapValueComplexType26.setAttribute("ref", "tns:std-BigDecimal");
        mapValueComplexType26.setAttribute("minOccurs", "1");
        mapValueComplexType26.setAttribute("maxOccurs", "1");
        mapValueComplexType0.appendChild(mapValueComplexType26);
        schema.appendChild(mapValueComplexType);

        /* col-Collection Complex Type */
        Element colCollectionComplexType = document.createElement("xsd:complexType");
        colCollectionComplexType.setAttribute("name", "col-Collection");
        Element colCollectionComplexType0 = document.createElement("xsd:choice");
        colCollectionComplexType.appendChild(colCollectionComplexType0);
        Element colCollectionComplexTypeNull = document.createElement("xsd:element");
        colCollectionComplexTypeNull.setAttribute("ref", "tns:null");
        colCollectionComplexTypeNull.setAttribute("minOccurs", "0");
        colCollectionComplexTypeNull.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexTypeNull);
        Element colCollectionComplexType1 = document.createElement("xsd:element");
        colCollectionComplexType1.setAttribute("ref", "tns:std-String");
        colCollectionComplexType1.setAttribute("minOccurs", "0");
        colCollectionComplexType1.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType1);
        Element colCollectionComplexType2 = document.createElement("xsd:element");
        colCollectionComplexType2.setAttribute("ref", "tns:std-Integer");
        colCollectionComplexType2.setAttribute("minOccurs", "0");
        colCollectionComplexType2.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType2);
        Element colCollectionComplexType3 = document.createElement("xsd:element");
        colCollectionComplexType3.setAttribute("ref", "tns:std-Long");
        colCollectionComplexType3.setAttribute("minOccurs", "0");
        colCollectionComplexType3.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType3);
        Element colCollectionComplexType4 = document.createElement("xsd:element");
        colCollectionComplexType4.setAttribute("ref", "tns:std-Float");
        colCollectionComplexType4.setAttribute("minOccurs", "0");
        colCollectionComplexType4.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType4);
        Element colCollectionComplexType5 = document.createElement("xsd:element");
        colCollectionComplexType5.setAttribute("ref", "tns:std-Double");
        colCollectionComplexType5.setAttribute("minOccurs", "0");
        colCollectionComplexType5.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType5);
        Element colCollectionComplexType6 = document.createElement("xsd:element");
        colCollectionComplexType6.setAttribute("ref", "tns:std-Boolean");
        colCollectionComplexType6.setAttribute("minOccurs", "0");
        colCollectionComplexType6.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType6);
        Element colCollectionComplexType7 = document.createElement("xsd:element");
        colCollectionComplexType7.setAttribute("ref", "tns:std-Locale");
        colCollectionComplexType7.setAttribute("minOccurs", "0");
        colCollectionComplexType7.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType7);
        Element colCollectionComplexType8 = document.createElement("xsd:element");
        colCollectionComplexType8.setAttribute("ref", "tns:sql-Timestamp");
        colCollectionComplexType8.setAttribute("minOccurs", "0");
        colCollectionComplexType8.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType8);
        Element colCollectionComplexType9 = document.createElement("xsd:element");
        colCollectionComplexType9.setAttribute("ref", "tns:sql-Date");
        colCollectionComplexType9.setAttribute("minOccurs", "0");
        colCollectionComplexType9.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType9);
        Element colCollectionComplexType10 = document.createElement("xsd:element");
        colCollectionComplexType10.setAttribute("ref", "tns:sql-Time");
        colCollectionComplexType10.setAttribute("minOccurs", "0");
        colCollectionComplexType10.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType10);
        Element colCollectionComplexType11 = document.createElement("xsd:element");
        colCollectionComplexType11.setAttribute("ref", "tns:col-ArrayList");
        colCollectionComplexType11.setAttribute("minOccurs", "0");
        colCollectionComplexType11.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType11);
        Element colCollectionComplexType12 = document.createElement("xsd:element");
        colCollectionComplexType12.setAttribute("ref", "tns:col-LinkedList");
        colCollectionComplexType12.setAttribute("minOccurs", "0");
        colCollectionComplexType12.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType12);
        Element colCollectionComplexType13 = document.createElement("xsd:element");
        colCollectionComplexType13.setAttribute("ref", "tns:col-Stack");
        colCollectionComplexType13.setAttribute("minOccurs", "0");
        colCollectionComplexType13.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType13);
        Element colCollectionComplexType14 = document.createElement("xsd:element");
        colCollectionComplexType14.setAttribute("ref", "tns:col-Vector");
        colCollectionComplexType14.setAttribute("minOccurs", "0");
        colCollectionComplexType14.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType14);
        Element colCollectionComplexType15 = document.createElement("xsd:element");
        colCollectionComplexType15.setAttribute("ref", "tns:col-TreeSet");
        colCollectionComplexType15.setAttribute("minOccurs", "0");
        colCollectionComplexType15.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType15);
        Element colCollectionComplexType16 = document.createElement("xsd:element");
        colCollectionComplexType16.setAttribute("ref", "tns:col-HashSet");
        colCollectionComplexType16.setAttribute("minOccurs", "0");
        colCollectionComplexType16.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType16);
        Element colCollectionComplexType17 = document.createElement("xsd:element");
        colCollectionComplexType17.setAttribute("ref", "tns:col-Collection");
        colCollectionComplexType17.setAttribute("minOccurs", "0");
        colCollectionComplexType17.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType17);
        Element colCollectionComplexType18 = document.createElement("xsd:element");
        colCollectionComplexType18.setAttribute("ref", "tns:map-HashMap");
        colCollectionComplexType18.setAttribute("minOccurs", "0");
        colCollectionComplexType18.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType18);
        Element colCollectionComplexType19 = document.createElement("xsd:element");
        colCollectionComplexType19.setAttribute("ref", "tns:map-Properties");
        colCollectionComplexType19.setAttribute("minOccurs", "0");
        colCollectionComplexType19.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType19);
        Element colCollectionComplexType20 = document.createElement("xsd:element");
        colCollectionComplexType20.setAttribute("ref", "tns:map-Hashtable");
        colCollectionComplexType20.setAttribute("minOccurs", "0");
        colCollectionComplexType20.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType20);
        Element colCollectionComplexType21 = document.createElement("xsd:element");
        colCollectionComplexType21.setAttribute("ref", "tns:map-WeakHashMap");
        colCollectionComplexType21.setAttribute("minOccurs", "0");
        colCollectionComplexType21.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType21);
        Element colCollectionComplexType22 = document.createElement("xsd:element");
        colCollectionComplexType22.setAttribute("ref", "tns:map-TreeMap");
        colCollectionComplexType22.setAttribute("minOccurs", "0");
        colCollectionComplexType22.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType22);
        Element colCollectionComplexType23 = document.createElement("xsd:element");
        colCollectionComplexType23.setAttribute("ref", "tns:map-Map");
        colCollectionComplexType23.setAttribute("minOccurs", "0");
        colCollectionComplexType23.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType23);
        Element colCollectionComplexType24 = document.createElement("xsd:element");
        colCollectionComplexType24.setAttribute("ref", "tns:eepk-");
        colCollectionComplexType24.setAttribute("minOccurs", "0");
        colCollectionComplexType24.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType24);
        Element colCollectionComplexType25 = document.createElement("xsd:element");
        colCollectionComplexType25.setAttribute("ref", "tns:eeval-");
        colCollectionComplexType25.setAttribute("minOccurs", "0");
        colCollectionComplexType25.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType25);
        schema.appendChild(colCollectionComplexType);
        Element colCollectionComplexType26 = document.createElement("xsd:element");
        colCollectionComplexType26.setAttribute("ref", "tns:std-BigDecimal");
        colCollectionComplexType26.setAttribute("minOccurs", "0");
        colCollectionComplexType26.setAttribute("maxOccurs", "unbounded");
        colCollectionComplexType0.appendChild(colCollectionComplexType26);
        schema.appendChild(colCollectionComplexType);

        types.setDocumentationElement(schema);
        return types;
    }
}
