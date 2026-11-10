/*
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.metrics.Metrics;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.group.GroupModel;
import org.apache.ofbiz.service.group.GroupServiceModel;
import org.apache.ofbiz.service.group.ServiceGroupReader;

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

    public static final String OUT_PARAM = "OUT";
    public static final String IN_PARAM = "IN";
    public static final String IN_OUT_PARAM = "INOUT";

    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String RESPOND_SUCCESS = "success";
    public static final String RESPOND_ERROR = "error";
    public static final String RESPOND_FAIL = "fail";
    public static final String ERROR_CODE = "errorCode";
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

    /** Can this service be called through the JMS listener, or from webtools by a user without SERVICE_INVOKE_ANY */
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
    private transient Set<ModelServiceIface> implServices = new LinkedHashSet<>();

    /** Set of override parameters */
    private Set<ModelParam> overrideParameters = new LinkedHashSet<>();

    /** List of permission groups for service invocation */
    private List<ModelPermGroup> permissionGroups = new LinkedList<>();

    /** List of email-notifications for this service */
    private transient List<ModelNotification> notifications = new LinkedList<>();

    /** Internal Service Group */
    private transient GroupModel internalGroup = null;

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
        this.definitionLocation = model.definitionLocation;
        this.description = model.description;
        this.engineName = model.engineName;
        this.location = model.location;
        this.invoke = model.invoke;
        this.defaultEntityName = model.defaultEntityName;
        this.auth = model.auth;
        this.export = model.export;
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
        if (model.implServices != null) {
            this.implServices = new LinkedHashSet<>(model.implServices);
        }
        this.overrideParameters = new LinkedHashSet<>(model.overrideParameters);
        this.permissionGroups = new LinkedList<>(model.permissionGroups);
        if (model.notifications != null) {
            this.notifications = new LinkedList<>(model.notifications);
        }
        this.deprecatedUseInstead = model.deprecatedUseInstead;
        this.deprecatedSince = model.deprecatedSince;
        this.deprecatedReason = model.deprecatedReason;
        this.inheritedParameters = model.inheritedParameters();
        this.internalGroup = model.internalGroup;
        this.hideResultInLog = model.hideResultInLog;
        this.metrics = model.metrics;
        for (ModelParam param: model.contextParamList) {
            ModelParam copy = new ModelParam(param);
            this.contextInfo.put(copy.getName(), copy);
            this.contextParamList.add(copy);
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
        buf.append(location).append("::");
        buf.append(invoke).append("::");
        buf.append(defaultEntityName).append("::");
        buf.append(auth).append("::");
        buf.append(export).append("::");
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
     *
     * @param dispatcher
     * @param context    the context
     * @param mode       Test either mode IN or mode OUT
     * @param locale     the actual locale to use
     */
    public void validate(LocalDispatcher dispatcher, Map<String, Object> context, String mode, Locale locale) throws ServiceValidationException {
        validate(dispatcher, this.contextParamList, context, mode, locale);
    }

    /**
     * Validates a Map against the IN or OUT parameter information for a given list of modelParam
     * this is used for recursive validation of map and list modelParam in service definition
     *
     * @param dispatcher     Dispatcher where come from the validation call
     * @param modelParamList List of paramList to validate
     * @param context        the context
     * @param mode           Test either mode IN or mode OUT
     * @param locale         the actual locale to use
     */
    public void validate(LocalDispatcher dispatcher, List<ModelParam> modelParamList, Map<String, Object> context, String mode, Locale locale)
            throws ServiceValidationException {
        // do not validate results with errors
        if (mode.equals(OUT_PARAM) && resultServiceContainsError(context)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("[ModelService.validate] : {" + this.name + "} : response was an error, not validating.", MODULE);
            }
            return;
        }

        final Map<String, ModelParam> requiredInfo = prepareRequiredParamsMap(mode, modelParamList);
        final Map<String, ModelParam> optionalInfo = prepareOptionalParamsMap(mode, modelParamList);

        if (Debug.verboseOn()) {
            Debug.logVerbose("[ModelService.validate] : {" + this.name + "} : Validating context - " + context, MODULE);
        }


        // get the test values
        Map<String, Object> requiredValues = resolveRequiredValues(requiredInfo, context, locale, mode);
        Map<String, Object> optionalValues = resolveOptionalValues(optionalInfo, context);
        checkUnwantedParameter(modelParamList, context, mode);

        if (Debug.verboseOn()) {
            String requiredNames = String.join(", ", requiredInfo.keySet());
            Debug.logVerbose("[ModelService.validate] : required fields - " + requiredNames, MODULE);
            Debug.logVerbose("[ModelService.validate] : {" + name + "} : (" + mode + ") Required - "
                    + requiredValues.size() + " / " + requiredInfo.size(), MODULE);
            Debug.logVerbose("[ModelService.validate] : {" + name + "} : (" + mode + ") Optional - "
                    + optionalValues.size() + " / " + optionalInfo.size(), MODULE);
        }
        try {
            validate(dispatcher, requiredInfo, requiredValues, true, this, mode, locale);
            validate(dispatcher, optionalInfo, optionalValues, false, this, mode, locale);
        } catch (ServiceValidationException e) {
            Debug.logError("[ModelService.validate] : {" + name + "} : (" + mode + ") Required test error: " + e, MODULE);
            throw e;
        }

        // required and type validation complete, do allow-html validation
        if (IN_PARAM.equals(mode)) {
            Map<String, ModelParam> allModelParams = new HashMap<>(requiredInfo);
            allModelParams.putAll(optionalInfo);
            Map<String, Object> allValues = new HashMap<>(requiredValues);
            allValues.putAll(optionalValues);
            allowHtmlValidation(allValues, allModelParams, locale);
        }
    }

    /**
     * Check the presence of not desired parameters in context.
     * @param modelParamList List of parameters
     * @param context the context
     * @param mode The mode (IN/OUT)
     * @throws ServiceValidationException When parameters should not be present in context
     */
    private void checkUnwantedParameter(List<ModelParam> modelParamList, Map<String, Object> context, String mode)
            throws ServiceValidationException {
        if (context == null) {
            return;
        }
        List<String> paramNames = modelParamList.stream().map(ModelParam::getName).collect(Collectors.toList());
        // This is to see if the info set contains all from the test set
        List<String> unwantedParamNames = context.keySet().stream()
                .filter(k -> !paramNames.contains(k))
                .collect(Collectors.toList());
        List<String> errorMessageList = unwantedParamNames.stream()
                .map(k -> "Unknown parameter found: [" + k + "]")
                .collect(Collectors.toList());
        if (UtilValidate.isNotEmpty(errorMessageList)) {
            throw new ServiceValidationException(errorMessageList, this, null, unwantedParamNames, mode);
        }
    }

    /**
     * Validate IN String service parameters according HTML data
     * @param values all values
     * @param modelParams service parameters to analyse
     * @param locale the locale
     * @throws ServiceValidationException
     */
    private void allowHtmlValidation(Map<String, Object> values, Map<String, ModelParam> modelParams, Locale locale)
            throws ServiceValidationException {
        List<String> errorMessageList = new LinkedList<>();
        final List<String> allowHtmls = List.of("none", "safe");
        final List<String> modes = List.of(IN_OUT_PARAM, IN_PARAM);
        modelParams.values()
                .stream()
                .filter(v -> v.getAllowHtml() != null)
                .filter(v -> allowHtmls.contains(v.getAllowHtml()))
                .filter(v -> modes.contains(v.getMode()))
                .filter(v -> v.getType().endsWith("String"))
                .filter(v -> values.get(v.getName()) != null)
                .collect(Collectors.toList())
                .forEach(p -> {
                    String paramName = p.getName();
                    String value = String.valueOf(values.get(paramName));
                    if ("none".equals(p.getAllowHtml())) {
                        UtilCodec.checkStringForHtmlStrictNone(paramName, value, errorMessageList, locale);
                    } else {
                        UtilCodec.checkStringForHtmlSafe(paramName, value, errorMessageList, locale,
                                EntityUtilProperties.getPropertyAsBoolean("owasp", "sanitizer.enable", true));
                    }
                });
        if (!errorMessageList.isEmpty()) {
            throw new ServiceValidationException(errorMessageList, this, IN_PARAM);
        }
    }

    /**
     * Check that all required service parameters are present and not null in context
     * and Build a map with all required values if true.
     * @param requiredInfo Map of requiredParameters
     * @param context the context
     * @param locale the locale
     * @param mode The mode (IN/OUT)
     * @return Map of required values
     * @throws ServiceValidationException when required values are found null
     */
    private Map<String, Object> resolveRequiredValues(Map<String, ModelParam> requiredInfo, Map<String, Object> context,
                                                      Locale locale, String mode)
            throws ServiceValidationException {
        Map<String, Object> requiredValues = new HashMap<>();
        if (context != null) {
            requiredValues = requiredInfo.keySet()
                    .stream()
                    .filter(k -> context.containsKey(k) && context.get(k) != null)
                    .collect(Collectors.toMap(k -> k, context::get));

            List<String> requiredButNull = requiredInfo.keySet()
                    .stream()
                    .filter(k -> context.get(k) == null)
                    .collect(Collectors.toList());

            // check for requiredButNull fields and return an error since null values are not allowed for required fields
            if (UtilValidate.isNotEmpty(requiredButNull)) {
                List<String> missingMsg = requiredButNull.stream()
                        .map(k -> requiredInfo.get(k).getFailMessage(locale))
                        .collect(Collectors.toList());
                throw new ServiceValidationException(missingMsg, this, requiredButNull, null, mode);
            }
        }
        return requiredValues;
    }

    /**
     * Build a map with all optional values
     * @param optionalInfo Map of optional parameters
     * @param context the context
     * @return Map of optional values
     */
    private Map<String, Object> resolveOptionalValues(Map<String, ModelParam> optionalInfo, Map<String, Object> context) {
        Map<String, Object> optionalValues = new HashMap<>();
        if (context != null) {
            optionalValues = optionalInfo.keySet()
                    .stream()
                    .filter(k -> context.containsKey(k) && context.get(k) != null)
                    .collect(Collectors.toMap(k -> k, context::get));
        }
        return optionalValues;
    }

    private static boolean resultServiceContainsError(Map<String, Object> context) {
        return context != null && context.containsKey(RESPONSE_MESSAGE)
                && (RESPOND_ERROR.equals(context.get(RESPONSE_MESSAGE))
                || RESPOND_FAIL.equals(context.get(RESPONSE_MESSAGE)));
    }

    /**
     * Return map of required service parameters in given mode
     * @param mode The mode (IN/OUT)
     * @param modelParams List of parameters to analyse
     * @return Map of name and ModelParam object that are required
     */
    private Map<String, ModelParam> prepareRequiredParamsMap(String mode, List<ModelParam> modelParams) {
        return prepareParamsMap(mode, modelParams, false);
    }
    /**
     * Return map of optional service parameters in given mode
     * @param mode The mode (IN/OUT)
     * @param modelParams List of parameters to analyse
     * @return Map of name and ModelParam object that are optional
     */
    private Map<String, ModelParam> prepareOptionalParamsMap(String mode, List<ModelParam> modelParams) {
        return prepareParamsMap(mode, modelParams, true);
    }

    /**
     * Return map of service parameters in given mode
     * @param mode The mode (IN/OUT)
     * @param modelParams List of parameters to analyse
     * @param optional boolean that indicates if optional
     * @return Map of filtered name and ModelParam object
     */
    private Map<String, ModelParam> prepareParamsMap(String mode, List<ModelParam> modelParams, boolean optional) {
        // get the info values
        final List<String> modes = List.of(IN_OUT_PARAM, mode);
        return modelParams.stream()
                .filter(p -> modes.contains(p.getMode()))
                .filter(p -> optional == p.isOptional())
                .collect(Collectors.toMap(ModelParam::getName, p -> p, (p1, p2) -> p1));
    }

    /**
     * Check a Map against the IN parameter information, uses the validate() method for that
     * Always called with only IN_PARAM, so to be called before the service is called with the passed context
     *
     * @param dispatcher
     * @param context    the passed context
     * @param locale     the actual locale to use
     * @return boolean True is the service called with these IN_PARAM is valid
     */
    public boolean isValid(LocalDispatcher dispatcher, Map<String, Object> context, Locale locale) {
        try {
            validate(dispatcher, context, IN_PARAM, locale);
        } catch (ServiceValidationException e) {
            return false;
        }
        return true;
    }

    /**
     * Validates a map of name, object types to a map of name, objects
     *
     * @param dispatcher
     * @param modelParamMap The map of name, modelParam
     * @param values        The map to test its value types.
     * @param reverse       Test the maps in reverse.
     */
    public void validate(LocalDispatcher dispatcher, Map<String, ModelParam> modelParamMap, Map<String, ?> values,
                         boolean reverse, ModelService model, String mode, Locale locale)
            throws ServiceValidationException {
        if (modelParamMap == null || values == null) {
            throw new ServiceValidationException("Cannot validate NULL maps", model);
        }

        // * Validate keys first
        Set<String> valuesSet = values.keySet();
        Set<String> modelParamSet = modelParamMap.keySet();

        // Quick check for sizes
        if (modelParamMap.isEmpty() && values.isEmpty()) {
            return;
        }
        // This is to see if the test set contains all from the info set (reverse)
        if (reverse && !valuesSet.containsAll(modelParamSet)) {
            Set<String> missing = new TreeSet<>(modelParamSet);

            missing.removeAll(valuesSet);
            List<String> missingMsgs = new LinkedList<>();
            for (String key: missing) {
                String msg = modelParamMap.get(key).getPrimaryFailMessage(locale);
                if (msg == null) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.getResource(), "ModelService.following_required_parameter_missing",
                            locale);
                    msg = errMsg + " [" + mode + "] [" + model.name + "." + key + "]";
                }
                missingMsgs.add(msg);
            }

            List<String> missingCopy = new LinkedList<>(missing);
            throw new ServiceValidationException(missingMsgs, model, missingCopy, null, mode);
        }

        // * Validate types next
        List<String> typeFailMsgs = new LinkedList<>();
        for (String key: valuesSet) {
            ModelParam param = modelParamMap.get(key);

            Object testObject = values.get(key);
            String infoType = modelParamMap.get(key).getType();

            if (UtilValidate.isNotEmpty(param.getValidators())) {
                for (ModelParam.ModelParamValidator val: param.getValidators()) {
                    if (UtilValidate.isNotEmpty(val.getMethodName())) {
                        try {
                            if (!typeValidate(dispatcher, val, testObject)) {
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

        for (String paramName : modelParamSet) {
            List<ModelParam> childrenModelParams = modelParamMap.get(paramName).getChildren();
            if (UtilValidate.isNotEmpty(childrenModelParams)
                    && UtilValidate.isNotEmpty(values.get(paramName))) {
                if (modelParamMap.get(paramName).getType().endsWith("Map")) {
                    validate(dispatcher,
                            childrenModelParams,
                            UtilGenerics.cast(values.get(paramName)), mode, locale);
                } else if (modelParamMap.get(paramName).getType().endsWith("List")) {
                    List<Map<String, Object>> subParameters = UtilGenerics.cast(values.get(paramName));
                    if (UtilValidate.isNotEmpty(subParameters)) {
                        for (Map<String, Object> paramMap : subParameters) {
                            validate(dispatcher, childrenModelParams, paramMap, mode, locale);
                        }
                    }
                }
            }
        }
    }

    public static boolean typeValidate(LocalDispatcher dispatcher, ModelParam.ModelParamValidator vali, Object testValue) throws GeneralException {
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

        Method validatorMethod;
        List<Object> params = new LinkedList<>();
        Optional<Method> validatorMethodOp = Arrays.stream(validatorClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(vali.getMethodName()))
                .findFirst();
        if (validatorMethodOp.isEmpty()) {
            throw new GeneralException("Unable to find validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName() + "]");
        }
        validatorMethod = validatorMethodOp.get();
        for (Class<?> paramType: validatorMethod.getParameterTypes()) {
            switch (paramType.getName()) {
            case "org.apache.ofbiz.entity.Delegator" -> params.add(dispatcher.getDelegator());
            case "org.apache.ofbiz.service.LocalDispatcher" -> params.add(dispatcher);
            case "java.lang.String" -> params.add(ObjectType.simpleTypeOrObjectConvert(testValue, "String", null, null));
            default -> params.add(vali);
            }
        }

        // run the validator
        Boolean resultBool;
        try {
            resultBool = (Boolean) validatorMethod.invoke(null, params.toArray());
        } catch (ClassCastException e) {
            throw new GeneralException("Validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName()
                    + "] did not return expected Boolean");
        } catch (Exception e) {
            throw new GeneralException("Unable to run validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName() + "]");
        }

        return resultBool;
    }

    /**
     * Gets the parameter names of the specified mode (IN/OUT). The
     * parameters will be returned in the order specified in the file.
     * Note: IN and OUT will also contains INOUT parameters.
     * @param mode The mode (IN/OUT)
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
     * @param errorMessages List of errorMessages
     * @param timeZone The TimeZone
     * @param locale Locale to use to do some type conversion
     * @return
     */
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode, boolean includeInternal, List<Object> errorMessages,
                                         TimeZone timeZone, Locale locale) {
        return makeValid(this.contextParamList, source, mode, includeInternal, errorMessages, timeZone, locale);
    }
    /**
     * Creates a new Map based from an existing map with just valid parameters.
     * Tries to convert parameters to required type.
     * @param modelParams Model parameters to validate
     * @param source The source map
     * @param mode The mode which to build the new map
     * @param includeInternal When false will exclude internal fields
     * @param errorMessages the list of error messages
     * @param timeZone TimeZone to use to do some type conversion
     * @param locale Locale to use to do some type conversion
     */
    public Map<String, Object> makeValid(List<ModelParam> modelParams, Map<String, ? extends Object> source, String mode,
                                         boolean includeInternal, List<Object> errorMessages,
                                         TimeZone timeZone, Locale locale) {
        Map<String, Object> target = new HashMap<>();

        if (source == null || contextInfo.isEmpty() || !List.of(IN_PARAM, OUT_PARAM, IN_OUT_PARAM).contains(mode)) {
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

        for (ModelParam modelParam: modelParams) {
            if (!modelParam.getMode().equals(IN_OUT_PARAM) && !modelParam.getMode().equals(mode)) {
                continue;
            }
            String paramName = modelParam.getName();

            // internal map of strings
            if (UtilValidate.isNotEmpty(modelParam.getStringMapPrefix()) && !source.containsKey(paramName)) {
                Map<String, Object> paramMap = makePrefixMap(source, modelParam);
                if (UtilValidate.isNotEmpty(paramMap)) {
                    target.put(paramName, paramMap);
                }
            // internal list of strings
            } else if (UtilValidate.isNotEmpty(modelParam.getStringListSuffix()) && !source.containsKey(paramName)) {
                List<Object> paramList = makeSuffixList(source, modelParam);
                if (UtilValidate.isNotEmpty(paramList)) {
                    target.put(paramName, paramList);
                }
            // other attributes
            } else if (source.containsKey(paramName)) {
                if (!modelParam.getInternal() || includeInternal) {
                    Object value = source.get(paramName);

                    try {
                        // no need to fail on type conversion; the validator will catch this
                        value = ObjectType.simpleTypeOrObjectConvert(value, modelParam.getType(), null, timeZone, locale, false);
                    } catch (GeneralException e) {
                        String errMsg = "Type conversion of field [" + paramName + "] to type [" + modelParam.getType() + "] failed for value \""
                                + value + "\": " + e;
                        Debug.logWarning("[ModelService.makeValid] : %s", MODULE, errMsg);
                        if (errorMessages != null) {
                            errorMessages.add(errMsg);
                        }
                    }
                    if (UtilValidate.isNotEmpty(modelParam.getChildren())) {
                        if (modelParam.getType().endsWith("Map")) {
                            value = makeValid(modelParam.getChildren(), UtilGenerics.cast(value), mode,
                                    includeInternal, errorMessages, timeZone, locale);
                        } else if (modelParam.getType().endsWith("List")) {
                            List<Map<String, Object>> subParameters = UtilGenerics.cast(value);
                            if (UtilValidate.isNotEmpty(subParameters)) {
                                List<Map<String, Object>> valueList = new LinkedList<>();
                                for (Map<String, Object> paramMap : subParameters) {
                                    valueList.add(makeValid(modelParam.getChildren(), paramMap, mode,
                                            includeInternal, errorMessages, timeZone, locale));
                                }
                                value = valueList;
                            }
                        }
                    }
                    target.put(paramName, value);
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
}
