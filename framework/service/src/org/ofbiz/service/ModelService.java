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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.group.GroupModel;
import org.ofbiz.service.group.GroupServiceModel;
import org.ofbiz.service.group.ServiceGroupReader;
import org.w3c.dom.Document;

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
    private static final Map<String, Field> MODEL_SERVICE_FIELD_MAP = FastMap.newInstance();
    static {
        MODEL_SERVICE_FIELDS = ModelService.class.getFields();
        for (Field field: MODEL_SERVICE_FIELDS) {
            MODEL_SERVICE_FIELD_MAP.put(field.getName(), field);
        }
    }
    public static final String module = ModelService.class.getName();

    public static final String XSD = "http://www.w3.org/2001/XMLSchema";
    public static final String TNS = "http://ofbiz.apache.org/service/";
    public static final String OUT_PARAM = "OUT";
    public static final String IN_PARAM = "IN";

    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String RESPOND_SUCCESS = "success";
    public static final String RESPOND_ERROR = "error";
    public static final String RESPOND_FAIL = "fail";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_MESSAGE_LIST = "errorMessageList";
    public static final String ERROR_MESSAGE_MAP = "errorMessageMap";
    public static final String SUCCESS_MESSAGE = "successMessage";
    public static final String SUCCESS_MESSAGE_LIST = "successMessageList";

    public static final String resource = "ServiceErrorUiLabels";

    /** The name of this service */
    public String name;

    /** The location of the definition this service */
    public String definitionLocation;

    /** The description of this service */
    public String description;

    /** The name of the service engine */
    public String engineName;

    /** The namespace of this service */
    public String nameSpace;

    /** The package name or location of this service */
    public String location;

    /** The method or function to invoke for this service */
    public String invoke;

    /** The default Entity to use for auto-attributes */
    public String defaultEntityName;

    /** The loader which loaded this definition */
    public String fromLoader;

    /** Does this service require authorization */
    public boolean auth;

    /** Can this service be exported via RPC, RMI, SOAP, etc */
    public boolean export;

    /** Enable verbose debugging when calling this service */
    public boolean debug;

    /** Validate the context info for this service */
    public boolean validate;

    /** Create a transaction for this service (if one is not already in place...)? */
    public boolean useTransaction;

    /** Require a new transaction for this service */
    public boolean requireNewTransaction;

    /** Override the default transaction timeout, only works if we start the transaction */
    public int transactionTimeout;

    /** Sets the max number of times this service will retry when failed (persisted async only) */
    public int maxRetry = -1;

    /** Permission service name */
    public String permissionServiceName;

    /** Permission service main-action */
    public String permissionMainAction;

    /** Permission service resource-description */
    public String permissionResourceDesc;

    /** Semaphore setting (wait, fail, none) */
    public String semaphore;

    /** Semaphore wait time (in milliseconds) */
    public int semaphoreWait;

    /** Semaphore sleep time (in milliseconds) */
    public int semaphoreSleep;

    /** Set of services this service implements */
    public Set<ModelServiceIface> implServices = new LinkedHashSet<ModelServiceIface>();

    /** Set of override parameters */
    public Set<ModelParam> overrideParameters = new LinkedHashSet<ModelParam>();

    /** List of permission groups for service invocation */
    public List<ModelPermGroup> permissionGroups = FastList.newInstance();

    /** List of email-notifications for this service */
    public List<ModelNotification> notifications = FastList.newInstance();

    /** Internal Service Group */
    public GroupModel internalGroup = null;

    /** Context Information, a Map of parameters used by the service, contains ModelParam objects */
    protected Map<String, ModelParam> contextInfo = FastMap.newInstance();

    /** Context Information, a List of parameters used by the service, contains ModelParam objects */
    protected List<ModelParam> contextParamList = FastList.newInstance();

    /** Flag to say if we have pulled in our addition parameters from our implemented service(s) */
    protected boolean inheritedParameters = false;

    public GenericInvoker invoker;

    public ModelService() {}

    public ModelService(ModelService model) {
        this.name = model.name;
        this.definitionLocation = model.definitionLocation;
        this.description = model.description;
        this.engineName = model.engineName;
        this.nameSpace = model.nameSpace;
        this.location = model.location;
        this.invoke = model.invoke;
        this.defaultEntityName = model.defaultEntityName;
        this.auth = model.auth;
        this.export = model.export;
        this.validate = model.validate;
        this.useTransaction = model.useTransaction || true;
        this.requireNewTransaction = model.requireNewTransaction;
        this.transactionTimeout = model.transactionTimeout;
        this.maxRetry = model.maxRetry;
        this.permissionServiceName = model.permissionServiceName;
        this.permissionMainAction = model.permissionMainAction;
        this.permissionResourceDesc = model.permissionResourceDesc;
        this.implServices = model.implServices;
        this.overrideParameters = model.overrideParameters;
        this.inheritedParameters = model.inheritedParameters();
        this.internalGroup = model.internalGroup;
        this.invoker = model.invoker.copy(this);

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

        public String getKey() {
            return field.getName();
        }

        public Object getValue() {
            try {
                return field.get(ModelService.this);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return field.hashCode() ^ System.identityHashCode(ModelService.this);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ModelServiceMapEntry)) return false;
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

                    public boolean hasNext() {
                        return i < MODEL_SERVICE_FIELDS.length;
                    }

                    public Map.Entry<String, Object> next() {
                        return new ModelServiceMapEntry(MODEL_SERVICE_FIELDS[i++]);
                    }

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
        buf.append(validate).append("::");
        buf.append(useTransaction).append("::");
        buf.append(requireNewTransaction).append("::");
        buf.append(transactionTimeout).append("::");
        buf.append(implServices).append("::");
        buf.append(overrideParameters).append("::");
        buf.append(contextInfo).append("::");
        buf.append(contextParamList).append("::");
        buf.append(inheritedParameters).append("::");
        return buf.toString();
    }

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
    public boolean inheritedParameters() {
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
            contextInfo.put(param.name, param);
            contextParamList.add(param);
        }
    }

    /* DEJ20060125 This is private but not used locally, so just commenting it out for now... may remove later
    private void copyParams(Collection params) {
        if (params != null) {
            Iterator i = params.iterator();
            while (i.hasNext()) {
                ModelParam param = (ModelParam) i.next();
                addParam(param);
            }
        }
    }
    */

    /**
     * Adds a clone of a parameter definition to this service
     */
    public void addParamClone(ModelParam param) {
        if (param != null) {
            ModelParam newParam = new ModelParam(param);
            addParam(newParam);
        }
    }

    public Set<String> getAllParamNames() {
        Set<String> nameList = new TreeSet<String>();
        for (ModelParam p: this.contextParamList) {
            nameList.add(p.name);
        }
        return nameList;
    }

    public Set<String> getInParamNames() {
        Set<String> nameList = new TreeSet<String>();
        for (ModelParam p: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if (p.isIn()) nameList.add(p.name);
        }
        return nameList;
    }

    // only returns number of defined parameters (not internal)
    public int getDefinedInCount() {
        int count = 0;

        for (ModelParam p: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if (p.isIn() && !p.internal) count++;
        }

        return count;
    }

    public Set<String> getOutParamNames() {
        Set<String> nameList = new TreeSet<String>();
        for (ModelParam p: this.contextParamList) {
            // don't include IN parameters in this list, only OUT and INOUT
            if (p.isOut()) nameList.add(p.name);
        }
        return nameList;
    }

    // only returns number of defined parameters (not internal)
    public int getDefinedOutCount() {
        int count = 0;

        for (ModelParam p: this.contextParamList) {
            // don't include IN parameters in this list, only OUT and INOUT
            if (p.isOut() && !p.internal) count++;
        }

        return count;
    }

    public void updateDefaultValues(Map<String, Object> context, String mode) {
        List<ModelParam> params = this.getModelParamList();
        if (params != null) {
            for (ModelParam param: params) {
                if ("INOUT".equals(param.mode) || mode.equals(param.mode)) {
                    Object defaultValueObj = param.getDefaultValue();
                    if (defaultValueObj != null && context.get(param.name) == null) {
                        context.put(param.name, defaultValueObj);
                        Debug.logInfo("Set default value [" + defaultValueObj + "] for parameter [" + param.name + "]", module);
                    }
                }
            }
        }
    }

    /**
     * Validates a Map against the IN or OUT parameter information
     * @param test The Map object to test
     * @param mode Test either mode IN or mode OUT
     */
    public void validate(Map<String, Object> context, String mode, Locale locale) throws ServiceValidationException {
        Map<String, String> requiredInfo = FastMap.newInstance();
        Map<String, String> optionalInfo = FastMap.newInstance();
        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) Debug.logVerbose("[ModelService.validate] : {" + this.name + "} : Validating context - " + context, module);

        // do not validate results with errors
        if (mode.equals(OUT_PARAM) && context != null && context.containsKey(RESPONSE_MESSAGE)) {
            if (RESPOND_ERROR.equals(context.get(RESPONSE_MESSAGE)) || RESPOND_FAIL.equals(context.get(RESPONSE_MESSAGE))) {
                if (verboseOn) Debug.logVerbose("[ModelService.validate] : {" + this.name + "} : response was an error, not validating.", module);
                return;
            }
        }

        // get the info values
        for (ModelParam modelParam: this.contextParamList) {
            // Debug.logInfo("In ModelService.validate preparing parameter [" + modelParam.name + (modelParam.optional?"(optional):":"(required):") + modelParam.mode + "] for service [" + this.name + "]", module);
            if ("INOUT".equals(modelParam.mode) || mode.equals(modelParam.mode)) {
                if (modelParam.optional) {
                    optionalInfo.put(modelParam.name, modelParam.type);
                } else {
                    requiredInfo.put(modelParam.name, modelParam.type);
                }
            }
        }

        // get the test values
        Map<String, Object> requiredTest = FastMap.newInstance();
        Map<String, Object> optionalTest = FastMap.newInstance();

        if (context == null) context = FastMap.newInstance();
        requiredTest.putAll(context);

        List<String> requiredButNull = FastList.newInstance();
        List<String> keyList = FastList.newInstance();
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
        if (requiredButNull.size() > 0) {
            List<String> missingMsg = FastList.newInstance();
            for (String missingKey: requiredButNull) {
                String message = this.getParam(missingKey).getPrimaryFailMessage(locale);
                if (message == null) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "ModelService.following_required_parameter_missing", locale);
                    message = errMsg + " [" + this.name + "." + missingKey + "]";
                }
                missingMsg.add(message);
            }
            throw new ServiceValidationException(missingMsg, this, requiredButNull, null, mode);
        }

        if (verboseOn) {
            StringBuilder requiredNames = new StringBuilder();

            for (String key: requiredInfo.keySet()) {
                if (requiredNames.length() > 0) {
                    requiredNames.append(", ");
                }
                requiredNames.append(key);
            }
            Debug.logVerbose("[ModelService.validate] : required fields - " + requiredNames, module);

            Debug.logVerbose("[ModelService.validate] : {" + name + "} : (" + mode + ") Required - " +
                requiredTest.size() + " / " + requiredInfo.size(), module);
            Debug.logVerbose("[ModelService.validate] : {" + name + "} : (" + mode + ") Optional - " +
                optionalTest.size() + " / " + optionalInfo.size(), module);
        }

        try {
            validate(requiredInfo, requiredTest, true, this, mode, locale);
            validate(optionalInfo, optionalTest, false, this, mode, locale);
        } catch (ServiceValidationException e) {
            Debug.logError("[ModelService.validate] : {" + name + "} : (" + mode + ") Required test error: " + e.toString(), module);
            throw e;
        }

        // required and type validation complete, do allow-html validation
        if ("IN".equals(mode)) {
            List<String> errorMessageList = FastList.newInstance();
            for (ModelParam modelParam: this.contextInfo.values()) {
                if (context.get(modelParam.name) != null &&
                        ("String".equals(modelParam.type) || "java.lang.String".equals(modelParam.type)) &&
                        !"any".equals(modelParam.allowHtml) &&
                        ("INOUT".equals(modelParam.mode) || "IN".equals(modelParam.mode))) {
                    // the param is a String, allow-html is none or safe, and we are looking at an IN parameter during input parameter validation
                    String value = (String) context.get(modelParam.name);
                    if ("none".equals(modelParam.allowHtml)) {
                        StringUtil.checkStringForHtmlStrictNone(modelParam.name, value, errorMessageList);
                    } else if ("safe".equals(modelParam.allowHtml)) {
                        StringUtil.checkStringForHtmlSafeOnly(modelParam.name, value, errorMessageList);
                    }
                }
            }
            if (errorMessageList.size() > 0) {
                throw new ServiceValidationException(errorMessageList, this, mode);
            }
        }
    }

    /**
     * Validates a map of name, object types to a map of name, objects
     * @param info The map of name, object types
     * @param test The map to test its value types.
     * @param reverse Test the maps in reverse.
     */
    public static void validate(Map<String, String> info, Map<String, ? extends Object> test, boolean reverse, ModelService model, String mode, Locale locale) throws ServiceValidationException {
        if (info == null || test == null) {
            throw new ServiceValidationException("Cannot validate NULL maps", model);
        }

        // * Validate keys first
        Set<String> testSet = test.keySet();
        Set<String> keySet = info.keySet();

        // Quick check for sizes
        if (info.size() == 0 && test.size() == 0) return;
        // This is to see if the test set contains all from the info set (reverse)
        if (reverse && !testSet.containsAll(keySet)) {
            Set<String> missing = new TreeSet<String>(keySet);

            missing.removeAll(testSet);
            List<String> missingMsgs = FastList.newInstance();
            for (String key: missing) {
                String msg = model.getParam(key).getPrimaryFailMessage(locale);
                if (msg == null) {
                    String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "ModelService.following_required_parameter_missing", locale) ;
                    msg = errMsg + " [" + mode + "] [" + model.name + "." + key + "]";
                }
                missingMsgs.add(msg);
            }

            List<String> missingCopy = FastList.newInstance();
            missingCopy.addAll(missing);
            throw new ServiceValidationException(missingMsgs, model, missingCopy, null, mode);
        }

        // This is to see if the info set contains all from the test set
        if (!keySet.containsAll(testSet)) {
            Set<String> extra = new TreeSet<String>(testSet);

            extra.removeAll(keySet);
            List<String> extraMsgs = FastList.newInstance();
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

            List<String> extraCopy = FastList.newInstance();
            extraCopy.addAll(extra);
            throw new ServiceValidationException(extraMsgs, model, null, extraCopy, mode);
        }

        // * Validate types next
        List<String> typeFailMsgs = FastList.newInstance();
        for (String key: testSet) {
            ModelParam param = model.getParam(key);

            Object testObject = test.get(key);
            String infoType = info.get(key);

            if (UtilValidate.isNotEmpty(param.validators)) {
                for (ModelParam.ModelParamValidator val: param.validators) {
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
                            Debug.logError(e, module);
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
                    String msg = "Type check failed for field [" + model.name + "." + key + "]; expected type is [" + infoType + "]; actual type is [" + testType + "]";
                    typeFailMsgs.add(msg);
                }
            }
        }

        if (typeFailMsgs.size() > 0) {
            throw new ServiceValidationException(typeFailMsgs, model, mode);
        }
    }

    public static boolean typeValidate(ModelParam.ModelParamValidator vali, Object testValue) throws GeneralException {
        // find the validator class
        Class<?> validatorClass = null;
        try {
            validatorClass = ObjectType.loadClass(vali.getClassName());
        } catch (ClassNotFoundException e) {
            Debug.logWarning(e, module);
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
                Debug.logWarning(e2, module);
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
                converted = (String) ObjectType.simpleTypeConvert(testValue, "String", null, null);
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
            throw new GeneralException("Validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName() + "] did not return expected Boolean");
        } catch (Exception e) {
            throw new GeneralException("Unable to run validation method [" + vali.getMethodName() + "] in class [" + vali.getClassName() + "]");
        }

        return resultBool.booleanValue();
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
        List<String> names = FastList.newInstance();

        if (!"IN".equals(mode) && !"OUT".equals(mode) && !"INOUT".equals(mode)) {
            return names;
        }
        if (contextInfo.size() == 0) {
            return names;
        }
        for (ModelParam param: contextParamList) {
            if (param.mode.equals("INOUT") || param.mode.equals(mode)) {
                if (optional || !param.optional) {
                    if (internal || !param.internal) {
                        names.add(param.name);
                    }
                }
            }
        }
        return names;
    }

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
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode, boolean includeInternal, List<Object> errorMessages, Locale locale) {
        return makeValid(source, mode, includeInternal, errorMessages, null, locale);
    }

    /**
     * Creates a new Map based from an existing map with just valid parameters.
     * Tries to convert parameters to required type.
     * @param source The source map
     * @param mode The mode which to build the new map
     * @param includeInternal When false will exclude internal fields
     * @param tz TimeZone to use to do some type conversion
     * @param locale Locale to use to do some type conversion
     */
    public Map<String, Object> makeValid(Map<String, ? extends Object> source, String mode, boolean includeInternal, List<Object> errorMessages, TimeZone timeZone, Locale locale) {
        Map<String, Object> target = FastMap.newInstance();

        if (source == null) {
            return target;
        }
        if (!"IN".equals(mode) && !"OUT".equals(mode) && !"INOUT".equals(mode)) {
            return target;
        }
        if (contextInfo.size() == 0) {
            return target;
        }

        if (locale == null) {
            // if statement here to avoid warning messages for Entity ECA service input validation, even though less efficient that doing a straight get
            if (source.containsKey("locale")) {
                locale = (Locale) source.get("locale");
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }
        }

        if (timeZone == null) {
            // if statement here to avoid warning messages for Entity ECA service input validation, even though less efficient that doing a straight get
            if (source.containsKey("timeZone")) {
                timeZone = (TimeZone) source.get("timeZone");
            }
            if (timeZone == null) {
                timeZone = TimeZone.getDefault();
            }
        }

        for (ModelParam param: contextParamList) {
            //boolean internalParam = param.internal;

            if (param.mode.equals("INOUT") || param.mode.equals(mode)) {
                String key = param.name;

                // internal map of strings
                if (UtilValidate.isNotEmpty(param.stringMapPrefix) && !source.containsKey(key)) {
                    Map<String, Object> paramMap = this.makePrefixMap(source, param);
                    if (UtilValidate.isNotEmpty(paramMap)) {
                        target.put(key, paramMap);
                    }
                // internal list of strings
                } else if (UtilValidate.isNotEmpty(param.stringListSuffix) && !source.containsKey(key)) {
                    List<Object> paramList = this.makeSuffixList(source, param);
                    if (UtilValidate.isNotEmpty(paramList)) {
                        target.put(key, paramList);
                    }
                // other attributes
                } else {
                    if (source.containsKey(key)) {
                        if ((param.internal && includeInternal) || (!param.internal)) {
                            Object value = source.get(key);

                            try {
                                // no need to fail on type conversion; the validator will catch this
                                value = ObjectType.simpleTypeConvert(value, param.type, null, timeZone, locale, false);
                            } catch (GeneralException e) {
                                String errMsg = "Type conversion of field [" + key + "] to type [" + param.type + "] failed for value \"" + value + "\": " + e.toString();
                                Debug.logWarning("[ModelService.makeValid] : " + errMsg, module);
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

    private Map<String, Object> makePrefixMap(Map<String, ? extends Object> source, ModelParam param) {
        Map<String, Object> paramMap = FastMap.newInstance();
        for (Map.Entry<String, ? extends Object> entry: source.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(param.stringMapPrefix)) {
                paramMap.put(key, entry.getValue());
            }
        }
        return paramMap;
    }

    private List<Object> makeSuffixList(Map<String, ? extends Object> source, ModelParam param) {
        List<Object> paramList = FastList.newInstance();
        for (Map.Entry<String, ? extends Object> entry: source.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(param.stringListSuffix)) {
                paramList.add(entry.getValue());
            }
        }
        return paramList;
    }

    public boolean containsPermissions() {
        return (UtilValidate.isNotEmpty(this.permissionGroups));
    }

    /**
     * Evaluates permission-service for this service.
     * @param dctx DispatchContext from the invoked service
     * @param context Map containing userLogin and context infromation
     * @return result of permission service invocation
     */
    public Map<String, Object> evalPermission(DispatchContext dctx, Map<String, ? extends Object> context) {
        if (UtilValidate.isNotEmpty(this.permissionServiceName)) {
            ModelService thisService;
            ModelService permission;
            try {
                thisService = dctx.getModelService(this.name);
                permission = dctx.getModelService(this.permissionServiceName);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Failed to get ModelService: " + e.toString(), module);
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("hasPermission", Boolean.FALSE);
                result.put("failMessage", e.getMessage());
                return result;
            }
            if (permission != null) {
                Map<String, Object> ctx = permission.makeValid(context, ModelService.IN_PARAM);
                if (UtilValidate.isNotEmpty(this.permissionMainAction)) {
                    ctx.put("mainAction", this.permissionMainAction);
                }
                if (UtilValidate.isNotEmpty(this.permissionResourceDesc)) {
                    ctx.put("resourceDescription", this.permissionResourceDesc);
                } else if (thisService != null) {
                    ctx.put("resourceDescription", thisService.name);
                }

                LocalDispatcher dispatcher = dctx.getDispatcher();
                Map<String, Object> resp;
                try {
                    resp = dispatcher.runSync(permission.name,  ctx, 300, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    Map<String, Object> result = ServiceUtil.returnSuccess();
                    result.put("hasPermission", Boolean.FALSE);
                    result.put("failMessage", e.getMessage());
                    return result;
                }
                if (ServiceUtil.isError(resp) || ServiceUtil.isFailure(resp)) {
                    Map<String, Object> result = ServiceUtil.returnSuccess();
                    result.put("hasPermission", Boolean.FALSE);
                    String failMessage = (String) resp.get("failMessage");
                    if (UtilValidate.isEmpty(failMessage)) {
                        failMessage = ServiceUtil.getErrorMessage(resp);
                    }
                    result.put("failMessage", failMessage);
                    return result;
                }
                return resp;
            } else {
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("hasPermission", Boolean.FALSE);
                result.put("failMessage", "No ModelService found with the name [" + this.permissionServiceName + "]");
                return result;
            }
        } else {
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("hasPermission", Boolean.FALSE);
            result.put("failMessage", "No ModelService found; no service name specified!");
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
     * @param context Map containing userLogin infromation
     * @return true if all permissions evaluate true.
     */
    public boolean evalPermissions(DispatchContext dctx, Map<String, ? extends Object> context) {
        // old permission checking
        if (this.containsPermissions()) {
            for (ModelPermGroup group: this.permissionGroups) {
                if (!group.evalPermissions(dctx, context)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * Gets a list of required IN parameters in sequence.
     * @return A list of required IN parameters in the order which they were defined.
     */
    public List<Object> getInParameterSequence(Map<String, ? extends Object> source) {
        List<Object> target = FastList.newInstance();
        if (source == null) {
            return target;
        }
        if (UtilValidate.isEmpty(contextInfo)) {
            return target;
        }
        for (ModelParam modelParam: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if ("OUT".equals(modelParam.mode)) continue;

            Object srcObject = source.get(modelParam.name);
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
        List<ModelParam> newList = FastList.newInstance();
        newList.addAll(this.contextParamList);
        return newList;
    }

    /**
     * Returns a list of ModelParam objects in the order they were defined when
     * the service was created.
     */
    public List<ModelParam> getInModelParamList() {
        List<ModelParam> inList = FastList.newInstance();
        for (ModelParam modelParam: this.contextParamList) {
            // don't include OUT parameters in this list, only IN and INOUT
            if ("OUT".equals(modelParam.mode)) continue;

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
            if (this.engineName.equals("group") && implServices.size() == 0) {
                GroupModel group = internalGroup;
                if (group == null) {
                    group = ServiceGroupReader.getGroupModel(this.location);
                }
                if (group != null) {
                    for (GroupServiceModel sm: group.getServices()) {
                        implServices.add(new ModelServiceIface(sm.getName(), sm.isOptional()));
                        if (Debug.verboseOn()) Debug.logVerbose("Adding service [" + sm.getName() + "] as interface of: [" + this.name + "]", module);
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
                            ModelParam existingParam = this.contextInfo.get(newParam.name);
                            if (existingParam != null) {
                                // if the existing param is not INOUT and the newParam.mode is different from existingParam.mode, make the existing param optional and INOUT
                                // TODO: this is another case where having different optional/required settings for IN and OUT would be quite valuable...
                                if (!"INOUT".equals(existingParam.mode) && !existingParam.mode.equals(newParam.mode)) {
                                    existingParam.mode = "INOUT";
                                    if (existingParam.optional || newParam.optional) {
                                        existingParam.optional = true;
                                    }
                                }
                            } else {
                                ModelParam newParamClone = new ModelParam(newParam);
                                if (optional) {
                                    // default option is to make this optional, however the service can override and
                                    // force the clone to use the parents setting.
                                    newParamClone.optional = true;
                                }
                                this.addParam(newParamClone);
                            }
                        }
                    } else {
                        Debug.logWarning("Inherited model [" + serviceName + "] not found for [" + this.name + "]", module);
                    }
                }
            }

            // handle any override parameters
            if (UtilValidate.isNotEmpty(overrideParameters)) {
                for (ModelParam overrideParam: overrideParameters) {
                    ModelParam existingParam = contextInfo.get(overrideParam.name);

                    // keep the list clean, remove it then add it back
                    contextParamList.remove(existingParam);

                    if (existingParam != null) {
                        // now re-write the parameters
                        if (UtilValidate.isNotEmpty(overrideParam.type)) {
                            existingParam.type = overrideParam.type;
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.mode)) {
                            existingParam.mode = overrideParam.mode;
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.entityName)) {
                            existingParam.entityName = overrideParam.entityName;
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.fieldName)) {
                            existingParam.fieldName = overrideParam.fieldName;
                        }
                        if (UtilValidate.isNotEmpty(overrideParam.formLabel)) {
                            existingParam.formLabel = overrideParam.formLabel;
                        }
                        if (overrideParam.getDefaultValue() != null) {
                            existingParam.copyDefaultValue(overrideParam);
                        }
                        if (overrideParam.overrideFormDisplay) {
                            existingParam.formDisplay = overrideParam.formDisplay;
                        }
                        if (overrideParam.overrideOptional) {
                            existingParam.optional = overrideParam.optional;
                        }
                        addParam(existingParam);
                    } else {
                        Debug.logWarning("Override param found but no parameter existing; ignoring: " + overrideParam.name, module);
                    }
                }
            }

            // set the flag so we don't do this again
            this.inheritedParameters = true;
        }
    }

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

    public void getWSDL(Definition def, String locationURI) throws WSDLException {
        // set the IN parameters
        Input input = def.createInput();
        Set<String> inParam = this.getInParamNames();
        if (inParam != null) {
            Message inMessage = def.createMessage();
            inMessage.setQName(new QName(TNS, this.name + "Request"));
            inMessage.setUndefined(false);
            for (String paramName: inParam) {
                ModelParam param = this.getParam(paramName);
                if (!param.internal) {
                    inMessage.addPart(param.getWSDLPart(def));
                }
            }
            def.addMessage(inMessage);
            input.setMessage(inMessage);
        }

        // set the OUT parameters
        Output output = def.createOutput();
        Set<String> outParam = this.getOutParamNames();
        if (outParam != null) {
            Message outMessage = def.createMessage();
            outMessage.setQName(new QName(TNS, this.name + "Response"));
            outMessage.setUndefined(false);
            for (String paramName: outParam) {
                ModelParam param = this.getParam(paramName);
                if (!param.internal) {
                    outMessage.addPart(param.getWSDLPart(def));
                }
            }
            def.addMessage(outMessage);
            output.setMessage(outMessage);
        }

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
        soapOperation.setSoapActionURI(""); // ?
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
}
