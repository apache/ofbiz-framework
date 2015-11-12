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
 */

import java.util.*;
import javax.wsdl.WSDLException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.ofbiz.service.ModelPermGroup;
import org.ofbiz.service.ModelPermission;
import org.ofbiz.service.ServiceContainer;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.util.EntityUtilProperties;

List getEcaListForService(String selectedService) {
    ecaMap = org.ofbiz.service.eca.ServiceEcaUtil.getServiceEventMap(selectedService);

    if (!ecaMap) return null;

    //ecaMap is a HashMap so get keyset & iterate
    ecaMapList = [];

    ecaMap.each { ecaKey, ecaValue ->
        ecaValue.each { curRule ->
            curRuleMap = [:];

            curRuleMap.ruleKey = ecaKey;

            curRuleClass = curRule.getClass();

            //event name for rule
            eventName = curRuleClass.getDeclaredField("eventName");
            eventName.setAccessible(true);
            eventNameVal = eventName.get(curRule);
            if (eventNameVal) {
                curRuleMap.eventName = eventNameVal as String;
            }
            eventName.setAccessible(false);

            //runOnError
            runOnError = curRuleClass.getDeclaredField("runOnError");
            runOnError.setAccessible(true);
            runOnErrorVal = runOnError.get(curRule);
            if (runOnErrorVal) {
                curRuleMap.runOnError = runOnErrorVal as String;
            }
            runOnError.setAccessible(false);

            //runOnFailure
            runOnFailure = curRuleClass.getDeclaredField("runOnFailure");
            runOnFailure.setAccessible(true);
            runOnFailureVal = runOnFailure.get(curRule);
            if (runOnFailureVal) {
                curRuleMap.runOnFailure = runOnFailureVal as String;
            }
            runOnFailure.setAccessible(false);

            //extract actions
            actions = curRuleClass.getDeclaredField("actionsAndSets");
            actions.setAccessible(true);
            actionsVal = actions.get(curRule);
            if (actionsVal) {
                actionsList = new ArrayList();
                setsList = new ArrayList();
                actionsVal.each { curAction ->
                    actionClass = curAction.getClass();
                    if (org.ofbiz.service.eca.ServiceEcaAction.equals(actionClass)) {
                        actionMap = [:];

                        //eventName
                        eventName = actionClass.getDeclaredField("eventName");
                        eventName.setAccessible(true);
                        eventNameVal = eventName.get(curAction);
                        if (eventNameVal) {
                            actionMap.eventName = eventNameVal as String;
                        }
                        eventName.setAccessible(false);

                        //ignoreError
                        ignoreError = actionClass.getDeclaredField("ignoreError");
                        ignoreError.setAccessible(true);
                        ignoreErrorVal = ignoreError.get(curAction);
                        if (ignoreErrorVal) {
                            actionMap.ignoreError = ignoreErrorVal as String;
                        }
                        ignoreError.setAccessible(false);

                        //ignoreFailure
                        ignoreFailure = actionClass.getDeclaredField("ignoreFailure");
                        ignoreFailure.setAccessible(true);
                        ignoreFailureVal = ignoreFailure.get(curAction);
                        if (ignoreFailureVal) {
                            actionMap.ignoreFailure = ignoreFailureVal as String;
                        }
                        ignoreFailure.setAccessible(false);

                        //persist
                        persist = actionClass.getDeclaredField("persist");
                        persist.setAccessible(true);
                        persistVal = persist.get(curAction);
                        if (persistVal) {
                            actionMap.persist = persistVal as String;
                        }
                        persist.setAccessible(false);

                        //resultMapName
                        resultMapName = actionClass.getDeclaredField("resultMapName");
                        resultMapName.setAccessible(true);
                        resultMapNameVal = resultMapName.get(curAction);
                        if (resultMapNameVal) {
                            actionMap.resultMapName = resultMapNameVal as String;
                        }
                        resultMapName.setAccessible(false);

                        //resultToContext
                        resultToContext = actionClass.getDeclaredField("resultToContext");
                        resultToContext.setAccessible(true);
                        resultToContextVal = resultToContext.get(curAction);
                        if (resultToContextVal) {
                            actionMap.resultToContext = resultToContextVal as String;
                        }
                        resultToContext.setAccessible(false);

                        //resultToResult
                        resultToResult = actionClass.getDeclaredField("resultToResult");
                        resultToResult.setAccessible(true);
                        resultToResultVal = resultToResult.get(curAction);
                        if (resultToResultVal) {
                            actionMap.resultToResult = resultToResultVal as String;
                        }
                        resultToResult.setAccessible(false);

                        //serviceMode
                        serviceMode = actionClass.getDeclaredField("serviceMode");
                        serviceMode.setAccessible(true);
                        serviceModeVal = serviceMode.get(curAction);
                        if (serviceModeVal) {
                            actionMap.serviceMode = serviceModeVal as String;
                        }
                        serviceMode.setAccessible(false);

                        //serviceName
                        serviceName = actionClass.getDeclaredField("serviceName");
                        serviceName.setAccessible(true);
                        serviceNameVal = serviceName.get(curAction);
                        if (serviceNameVal) {
                            actionMap.serviceName = serviceNameVal as String;
                        }
                        serviceName.setAccessible(false);

                        actionsList.add(actionMap);

                    } else {  // FIXME : we should also show field-names and values for set operation
                        setMap = [:];

                        // fieldName
                        fieldName = actionClass.getDeclaredField("fieldName");
                        fieldName.setAccessible(true);
                        fieldNameVal = fieldName.get(curAction);
                        if (fieldNameVal) {
                            setMap.fieldName = fieldNameVal as String;
                        }
                        fieldName.setAccessible(false);

                        // envName
                        envName = actionClass.getDeclaredField("envName");
                        envName.setAccessible(true);
                        envNameVal = envName.get(curAction);
                        if (envNameVal) {
                            setMap.envName = envNameVal as String;
                        }
                        envName.setAccessible(false);

                        // value
                        value = actionClass.getDeclaredField("value");
                        value.setAccessible(true);
                        valueVal = value.get(curAction);
                        if (valueVal) {
                            setMap.value = valueVal as String;
                        }
                        value.setAccessible(false);

                        // format
                        format = actionClass.getDeclaredField("format");
                        format.setAccessible(true);
                        formatVal = format.get(curAction);
                        if (formatVal) {
                            setMap.format = formatVal as String;
                        }
                        format.setAccessible(false);

                        setsList.add(setMap);
                    }
                }

                curRuleMap.actions = actionsList;
                curRuleMap.sets= setsList;
            }
            actions.setAccessible(true);

            //extract conditions
            conditions = curRuleClass.getDeclaredField("conditions");
            conditions.setAccessible(true);
            conditionsVal = conditions.get(curRule);
            if (conditionsVal) {
                curRuleMap.conditions = runOnFailureVal as String;
                condList = new ArrayList(conditionsVal.size());
                conditionsVal.each { condVal ->
                    condValClass = condVal.getClass();
                    condMap = [:];

                    //compareType
                    compareType = condValClass.getDeclaredField("compareType");
                    compareType.setAccessible(true);
                    compareTypeVal = compareType.get(condVal);
                    if (compareTypeVal) {
                        condMap.compareType = compareTypeVal as String;
                    }
                    compareType.setAccessible(false);

                    //conditionService
                    conditionService = condValClass.getDeclaredField("conditionService");
                    conditionService.setAccessible(true);
                    conditionServiceVal = conditionService.get(condVal);
                    if (conditionServiceVal) {
                        condMap.conditionService = conditionServiceVal as String;
                    }
                    conditionService.setAccessible(false);

                    //format
                    format = condValClass.getDeclaredField("format");
                    format.setAccessible(true);
                    formatVal = format.get(condVal);
                    if (formatVal) {
                        condMap.format = formatVal as String;
                    }
                    format.setAccessible(false);

                    //isConstant
                    isConstant = condValClass.getDeclaredField("isConstant");
                    isConstant.setAccessible(true);
                    isConstantVal = isConstant.get(condVal);
                    if (isConstantVal) {
                        condMap.isConstant = isConstantVal as String;
                    }
                    isConstant.setAccessible(false);

                    //isService
                    isService = condValClass.getDeclaredField("isService");
                    isService.setAccessible(true);
                    isServiceVal = isService.get(condVal);
                    if (isServiceVal) {
                        condMap.isService = isServiceVal as String;
                    }
                    isService.setAccessible(false);

                    //lhsMapName
                    lhsMapName = condValClass.getDeclaredField("lhsMapName");
                    lhsMapName.setAccessible(true);
                    lhsMapNameVal = lhsMapName.get(condVal);
                    if (lhsMapNameVal) {
                        condMap.lhsMapName = lhsMapNameVal as String;
                    }
                    lhsMapName.setAccessible(false);

                    //lhsValueName
                    lhsValueName = condValClass.getDeclaredField("lhsValueName");
                    lhsValueName.setAccessible(true);
                    lhsValueNameVal = lhsValueName.get(condVal);
                    if (lhsValueNameVal) {
                        condMap.lhsValueName = lhsValueNameVal as String;
                    }
                    lhsValueName.setAccessible(false);

                    //operator
                    operator = condValClass.getDeclaredField("operator");
                    operator.setAccessible(true);
                    operatorVal = operator.get(condVal);
                    if (operatorVal) {
                        condMap.operator = operatorVal as String;
                    }
                    operator.setAccessible(false);

                    //rhsMapName
                    rhsMapName = condValClass.getDeclaredField("rhsMapName");
                    rhsMapName.setAccessible(true);
                    rhsMapNameVal = rhsMapName.get(condVal);
                    if (rhsMapNameVal) {
                        condMap.rhsMapName = rhsMapNameVal as String;
                    }
                    rhsMapName.setAccessible(false);

                    //rhsValueName
                    rhsValueName = condValClass.getDeclaredField("rhsValueName");
                    rhsValueName.setAccessible(true);
                    rhsValueNameVal = rhsValueName.get(condVal);
                    if (rhsValueNameVal) {
                        condMap.rhsValueName = rhsValueNameVal as String;
                    }
                    rhsValueName.setAccessible(false);

                    condList.add(condMap);
                }
                curRuleMap.conditions = condList;
            }
            conditions.setAccessible(false);

            ecaMapList.add(curRuleMap);
        }
    }
    return ecaMapList;
}


//Local Dispatchers
dispArrList = new TreeSet();
dispArrList.addAll(ServiceContainer.getAllDispatcherNames());
context.dispArrList = dispArrList;

uiLabelMap = UtilProperties.getResourceBundleMap("WebtoolsUiLabels", locale);
uiLabelMap.addBottomResourceBundle("CommonUiLabels");

curDispatchContext = dispatcher.getDispatchContext();
context.dispatcherName = dispatcher.getName();

selectedService = parameters.sel_service_name;

if (selectedService) {
    curServiceMap = [:];

    curServiceMap.serviceName = selectedService;
    curServiceModel = curDispatchContext.getModelService(selectedService);

    if (curServiceModel != null) {
        curServiceMap.description = curServiceModel.description;
        engineName = curServiceModel.engineName ?: "NA";
        defaultEntityName = curServiceModel.defaultEntityName ?: "NA";
        export = curServiceModel.export ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
        exportBool = curServiceModel.export ? "true" : "false";
        permissionGroups = curServiceModel.permissionGroups ?: "NA";
        implServices = curServiceModel.implServices ?: "NA";
        overrideParameters = curServiceModel.overrideParameters;
        useTrans = curServiceModel.useTransaction ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
        maxRetry = curServiceModel.maxRetry;

        //Test for ECA's
        ecaMapList = getEcaListForService(selectedService);
        if (ecaMapList) {
            context.ecaMapList = ecaMapList;
        }
        //End Test for ECA's

        invoke = curServiceModel.invoke ?: "NA";
        location = curServiceModel.location ?: "NA";
        requireNewTransaction = curServiceModel.requireNewTransaction ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;

        curServiceMap.engineName = engineName;
        curServiceMap.defaultEntityName = defaultEntityName;
        curServiceMap.invoke = invoke;
        curServiceMap.location = location;
        curServiceMap.definitionLocation = curServiceModel.definitionLocation.replaceFirst("file:/" + System.getProperty("ofbiz.home") + "/", "");
        curServiceMap.requireNewTransaction = requireNewTransaction;
        curServiceMap.export = export;
        curServiceMap.exportBool = exportBool;

        if (permissionGroups && !permissionGroups.equals("NA")) {
            permList = new ArrayList(permissionGroups.size());
            permissionGroups.each { curPerm ->  //This is a ModelPermGroup
                curPerm.permissions.each { curPermObj ->
                    permObj = [:];
                    permObj.action = curPermObj.action;
                    permType = curPermObj.permissionType;
                    if (permType == 1) {
                        permType = "Simple Permission";
                    } else if (permType == 2) {
                        permType = "Entity Permission";
                    } else if (permType == 3) {
                        permType = "Role Member";
                    }
                    permObj.permType = permType;
                    permObj.nameOrRole = curPermObj.nameOrRole;
                    permList.add(permObj);
                }
            }
            curServiceMap.permissionGroups = permList;
        } else {
            curServiceMap.permissionGroups = permissionGroups;
        }

        curServiceMap.implServices = implServices;
        curServiceMap.useTrans = useTrans;
        curServiceMap.maxRetry = maxRetry;

        allParamsList = new ArrayList(3);

        inParams = curServiceModel.getInParamNames();
        inParamsList = new ArrayList(inParams.size());
        inParams.each { paramName ->
            curParam = curServiceModel.getParam(paramName);
            curInParam = [:];
            curInParam.entityName = curParam.entityName;
            curInParam.fieldName = curParam.fieldName;
            curInParam.internal = curParam.internal ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
            curInParam.mode = curParam.mode;
            curInParam.name = curParam.name;
            curInParam.description = curParam.description;
            curInParam.optional = curParam.optional ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
            curInParam.type = curParam.type;
            inParamsList.add(curInParam);
        }
        inParamMap = [:];
        inParamMap.title = uiLabelMap.WebtoolsInParameters;
        inParamMap.paramList = inParamsList;
        allParamsList.add(inParamMap);

        outParams = curServiceModel.getOutParamNames();
        outParamsList = new ArrayList(outParams.size());
        outParams.each { paramName ->
            curParam = curServiceModel.getParam(paramName);
            curOutParam = [:];
            curOutParam.entityName = curParam.entityName;
            curOutParam.fieldName = curParam.fieldName;
            curOutParam.internal = curParam.internal ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
            curOutParam.mode = curParam.mode;
            curOutParam.name = curParam.name;
            curOutParam.description = curParam.description;
            curOutParam.optional = curParam.optional ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
            curOutParam.type = curParam.type;
            outParamsList.add(curOutParam);
        }
        outParamMap = [:];
        outParamMap.title = uiLabelMap.get("WebtoolsOutParameters");
        outParamMap.paramList = outParamsList;
        allParamsList.add(outParamMap);

        if (overrideParameters) {
            ovrPrmList = new ArrayList(overrideParameters.size());
            overrideParameters.each { curParam ->
                curOvrPrm = [:];
                curOvrPrm.entityName = curParam.entityName;
                curOvrPrm.fieldName = curParam.fieldName;
                curOvrPrm.internal = curParam.internal ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
                curOvrPrm.mode = curParam.mode;
                curOvrPrm.name = curParam.name;
                curOvrPrm.description = curParam.description;
                curOvrPrm.optional = curParam.optional ? uiLabelMap.CommonTrue : uiLabelMap.CommonFalse;
                curOvrPrm.type = curParam.type;
                ovrPrmList.add(curOvrPrm);
            }
            ovrParamMap = [:];
            ovrParamMap.title = "Override parameters";
            ovrParamMap.paramList = ovrPrmList;
            allParamsList.add(ovrParamMap);
        }
        curServiceMap.allParamsList = allParamsList;
    }

    showWsdl = parameters.show_wsdl;

    if (showWsdl?.equals("true")) {
        try {
            wsdl = curServiceModel.toWSDL("http://${request.getServerName()}:${EntityUtilProperties.getPropertyValue("url", "port.http", "80", delegator)}${parameters._CONTROL_PATH_}/SOAPService");
            curServiceMap.wsdl = UtilXml.writeXmlDocument(wsdl);
        } catch (WSDLException ex) {
            curServiceMap.wsdl = ex.getLocalizedMessage();
        }
        context.showWsdl = true;
    }
    context.selectedServiceMap = curServiceMap;
}


if (!selectedService) {

    //get constraints if any
    constraint = parameters.constraint;

    serviceNames = curDispatchContext.getAllServiceNames();
    serviceNamesAlphaList = new ArrayList(26);
    servicesList = new ArrayList(serviceNames.size());
    servicesFoundCount = 0;
    serviceNames.each { serviceName ->
        //add first char of service name to list
        if (serviceName) {
            serviceCharAt1 = serviceName[0];
            if (!serviceNamesAlphaList.contains(serviceCharAt1)) {
                serviceNamesAlphaList.add(serviceCharAt1);
            }
        }

        //create basic service def
        curServiceMap = [:];
        curServiceMap.serviceName = serviceName;
        curServiceModel = curDispatchContext.getModelService(serviceName);

        canIncludeService = true;
        if (constraint && curServiceModel != null) {
            consArr = constraint.split("@");
            constraintName = consArr[0];
            constraintVal = consArr[1];

            if (constraintName.equals("engine_name")) {
                canIncludeService = curServiceModel.engineName.equals(constraintVal);
                if (constraintVal.equals("NA")) {
                    canIncludeService = curServiceModel.engineName ? false : true;
                }
            }

            if (canIncludeService && constraintName.equals("default_entity_name")) {
                canIncludeService = curServiceModel.defaultEntityName.equals(constraintVal);
                if (constraintVal.equals("NA")) {
                    canIncludeService = curServiceModel.defaultEntityName ? false : true;
                }
            }

            if (canIncludeService && constraintName.equals("location")) {
                canIncludeService = curServiceModel.location.equals(constraintVal);
                if (constraintVal.equals("NA")) {
                    canIncludeService = curServiceModel.location ? false : true;
                }
            }

            if (canIncludeService && constraintName.equals("definitionLocation")) {
                fullPath = "file:/" + System.getProperty("ofbiz.home") + "/" + constraintVal;
                canIncludeService = curServiceModel.definitionLocation.equals(fullPath);
            }

            if (canIncludeService && constraintName.equals("alpha")) {
                canIncludeService = (serviceName[0]).equals(constraintVal);
                if (constraintVal.equals("NA")) {
                    canIncludeService = true;
                }
            }
        }

        if (curServiceModel != null && canIncludeService) {
            engineName = curServiceModel.engineName ?: "NA";
            defaultEntityName = curServiceModel.defaultEntityName ?: "NA";
            invoke = curServiceModel.invoke ?: "NA";
            location = curServiceModel.location ?: "NA";
            requireNewTransaction = curServiceModel.requireNewTransaction;

            curServiceMap.engineName = engineName;
            curServiceMap.defaultEntityName = defaultEntityName;
            curServiceMap.invoke = invoke;
            curServiceMap.location = location;
            curServiceMap.definitionLocation = curServiceModel.definitionLocation.replaceFirst("file:/" + System.getProperty("ofbiz.home") + "/", "");
            curServiceMap.requireNewTransaction = requireNewTransaction;

            servicesList.add(curServiceMap);
            servicesFoundCount++;
        }
    }

    context.servicesList = servicesList;
    context.serviceNamesAlphaList = serviceNamesAlphaList;
    context.servicesFoundCount = servicesFoundCount;
}
