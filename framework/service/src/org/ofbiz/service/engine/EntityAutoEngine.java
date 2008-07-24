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
package org.ofbiz.service.engine;

import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.finder.PrimaryKeyFinder;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Standard Java Static Method Service Engine
 */
public final class EntityAutoEngine extends GenericAsyncEngine {

    public static final String module = EntityAutoEngine.class.getName();

    public EntityAutoEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }
 
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        // static java service methods should be: public Map<String, Object> methodName(DispatchContext dctx, Map<String, Object> context)
        DispatchContext dctx = dispatcher.getLocalContext(localName);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        // check the package and method names
        if (modelService.invoke == null || (!"create".equals(modelService.invoke) && !"update".equals(modelService.invoke) && !"delete".equals(modelService.invoke))) {
            throw new GenericServiceException("In Service [" + modelService.name + "] the invoke value must be create, update, or delete for entity-auto engine");
        }
        
        if (UtilValidate.isEmpty(modelService.defaultEntityName)) {
            throw new GenericServiceException("In Service [" + modelService.name + "] you must specify a default-entity-name for entity-auto engine");
        }
        
        ModelEntity modelEntity = dctx.getDelegator().getModelEntity(modelService.defaultEntityName);
        if (modelEntity == null) {
            throw new GenericServiceException("In Service [" + modelService.name + "] the specified default-entity-name [" + modelService.defaultEntityName + "] is not valid");
        }
        
        try {
            boolean allPksInOnly = true;
            for (ModelField pkField: modelEntity.getPkFieldsUnmodifiable()) {
                ModelParam pkParam = modelService.getParam(pkField.getName());
                if (pkParam.isOut()) {
                    allPksInOnly = false;
                }
            }
            
            if ("create".equals(modelService.invoke)) {
                GenericValue newEntity = dctx.getDelegator().makeValue(modelEntity.getEntityName());
                
                boolean isSinglePk = modelEntity.getPksSize() == 1;
                boolean isDoublePk = modelEntity.getPksSize() == 2;
                Iterator<ModelField> pksIter = modelEntity.getPksIterator();
                
                ModelField singlePkModeField = isSinglePk ? pksIter.next() : null;
                ModelParam singlePkModelParam = isSinglePk ? modelService.getParam(singlePkModeField.getName()) : null; 
                boolean isSinglePkIn = isSinglePk ? singlePkModelParam.isIn() : false;
                boolean isSinglePkOut = isSinglePk ? singlePkModelParam.isOut() : false;
                
                ModelParam doublePkPrimaryInParam = null;
                ModelParam doublePkSecondaryOutParam = null;
                ModelField doublePkSecondaryOutField = null;
                if (isDoublePk) {
                    ModelField firstPkField = pksIter.next();
                    ModelParam firstPkParam = modelService.getParam(firstPkField.getName());
                    ModelField secondPkField = pksIter.next();
                    ModelParam secondPkParam = modelService.getParam(secondPkField.getName());
                    if (firstPkParam.isIn() && secondPkParam.isOut()) {
                        doublePkPrimaryInParam = firstPkParam;
                        doublePkSecondaryOutParam = secondPkParam;
                        doublePkSecondaryOutField = secondPkField;
                    } else if (firstPkParam.isOut() && secondPkParam.isIn()) {
                        doublePkPrimaryInParam = secondPkParam;
                        doublePkSecondaryOutParam = firstPkParam;
                        doublePkSecondaryOutField = firstPkField;
                    } else {
                        // we don't have an IN and an OUT... so do nothing and leave them null
                    }
                }
                
                
                if (isSinglePk && isSinglePkOut && !isSinglePkIn) {
                    /*
                     **** primary sequenced primary key ****
                     * 
                    <auto-attributes include="pk" mode="OUT" optional="false"/>
                     * 
                    <make-value entity-name="Example" value-name="newEntity"/>
                    <sequenced-id-to-env sequence-name="Example" env-name="newEntity.exampleId"/> <!-- get the next sequenced ID -->
                    <field-to-result field-name="newEntity.exampleId" result-name="exampleId"/>
                    <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
                    <create-value value-name="newEntity"/>
                     *
                     */
                    
                    String sequencedId = dctx.getDelegator().getNextSeqId(modelEntity.getEntityName());
                    newEntity.set(singlePkModeField.getName(), sequencedId);
                    result.put(singlePkModelParam.name, sequencedId);
                } else if (isSinglePk && isSinglePkOut && isSinglePkIn) {
                    /*
                     **** primary sequenced key with optional override passed in ****
                     * 
                    <auto-attributes include="pk" mode="INOUT" optional="true"/>
                     * 
                    <make-value value-name="newEntity" entity-name="Product"/>
                    <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
                    <set from-field="parameters.productId" field="newEntity.productId"/>
                    <if-empty field="newEntity.productId">
                        <sequenced-id-to-env sequence-name="Product" env-name="newEntity.productId"/>
                    <else>
                        <check-id field-name="productId" map-name="newEntity"/>
                        <check-errors/>
                    </else>
                    </if-empty>
                    <field-to-result field-name="productId" map-name="newEntity" result-name="productId"/>
                    <create-value value-name="newEntity"/>
                     * 
                     */

                    Object pkValue = context.get(singlePkModelParam.name);
                    if (UtilValidate.isEmpty(pkValue)) {
                        pkValue = dctx.getDelegator().getNextSeqId(modelEntity.getEntityName());
                    } else {
                        // pkValue passed in, check and if there are problems return an error
                        
                        if (pkValue instanceof String) {
                            StringBuffer errorDetails = new StringBuffer();
                            if (!UtilValidate.isValidDatabaseId((String) pkValue, errorDetails)) {
                                return ServiceUtil.returnError("The ID value in the parameter [" + singlePkModelParam.name + "] was not valid: " + errorDetails);
                            }
                        }
                    }
                    newEntity.set(singlePkModeField.getName(), pkValue);
                    result.put(singlePkModelParam.name, pkValue);
                } else if (isDoublePk && doublePkPrimaryInParam != null && doublePkSecondaryOutParam != null) {
                    /*
                     **** secondary sequenced primary key ****
                     * 
                    <auto-attributes include="pk" mode="IN" optional="false"/>
                    <override name="exampleItemSeqId" mode="OUT"/> <!-- make this OUT rather than IN, we will automatically generate the next sub-sequence ID -->
                     * 
                    <make-value entity-name="ExampleItem" value-name="newEntity"/>
                    <set-pk-fields map-name="parameters" value-name="newEntity"/>
                    <make-next-seq-id value-name="newEntity" seq-field-name="exampleItemSeqId"/> <!-- this finds the next sub-sequence ID -->
                    <field-to-result field-name="newEntity.exampleItemSeqId" result-name="exampleItemSeqId"/>
                    <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
                    <create-value value-name="newEntity"/>  
                     */
                    
                    newEntity.setPKFields(context, true);
                    dctx.getDelegator().setNextSubSeqId(newEntity, doublePkSecondaryOutField.getName(), 5, 1);
                    result.put(doublePkSecondaryOutParam.name, newEntity.get(doublePkSecondaryOutField.getName()));
                } else if (allPksInOnly) {
                    /*
                     **** plain specified primary key ****
                     * 
                    <auto-attributes include="pk" mode="IN" optional="false"/>
                     * 
                    <make-value entity-name="Example" value-name="newEntity"/>
                    <set-pk-fields map-name="parameters" value-name="newEntity"/>
                    <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
                    <create-value value-name="newEntity"/>
                     *
                     */
                    newEntity.setPKFields(context, true);
                } else {
                    throw new GenericServiceException("In Service [" + modelService.name + "] which uses the entity-auto engine with the create invoke option: " +
                    		"could not find a valid combination of primary key settings to do a known create operation; options include: " +
                    		"1. a single OUT pk for primary auto-sequencing, " +
                    		"2. a single INOUT pk for primary auto-sequencing with optional override, " +
                    		"3. a 2-part pk with one part IN (existing primary pk) and one part OUT (the secdonary pk to sub-sequence, " +
                    		"4. all pk fields are IN for a manually specified primary key");
                }
                
                // handle the case where there is a fromDate in the pk of the entity, and it is optional or undefined in the service def, populate automatically
                ModelField fromDateField = modelEntity.getField("fromDate");
                if (fromDateField != null && fromDateField.getIsPk()) {
                    ModelParam fromDateParam = modelService.getParam("fromDate");
                    if (fromDateParam == null || (fromDateParam.isOptional() && context.get("fromDate") == null)) {
                        newEntity.set("fromDate", UtilDateTime.nowTimestamp());
                    }
                }
                
                newEntity.setNonPKFields(context, true);
                newEntity.create();
            } else if ("update".equals(modelService.invoke)) {
                /*
                <auto-attributes include="pk" mode="IN" optional="false"/>
                 * 
                <entity-one entity-name="ExampleItem" value-name="lookedUpValue"/>
                <set-nonpk-fields value-name="lookedUpValue" map-name="parameters"/>
                <store-value value-name="lookedUpValue"/>
                 */
                
                // check to make sure that all primary key fields are defined as IN attributes
                if (!allPksInOnly) {
                    throw new GenericServiceException("In Service [" + modelService.name + "] which uses the entity-auto engine with the update invoke option not all pk fields have the mode IN");
                }

                GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, context, dctx.getDelegator(), false, true, null, null);
                lookedUpValue.setNonPKFields(context, true);
                lookedUpValue.store();
            } else if ("delete".equals(modelService.invoke)) {
                /*
                <auto-attributes include="pk" mode="IN" optional="false"/>
                 * 
                <entity-one entity-name="ExampleItem" value-name="lookedUpValue"/>
                <remove-value value-name="lookedUpValue"/>
                 */
                
                // check to make sure that all primary key fields are defined as IN attributes
                if (!allPksInOnly) {
                    throw new GenericServiceException("In Service [" + modelService.name + "] which uses the entity-auto engine with the delete invoke option not all pk fields have the mode IN");
                }
                
                GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, context, dctx.getDelegator(), false, true, null, null);
                if (lookedUpValue != null) {
                    lookedUpValue.remove();
                }
            }
        } catch (GeneralException e) {
            String errMsg = "Error doing entity-auto operation for entity [" + modelEntity.getEntityName() + "] in service [" + modelService.name + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return result;
    }
}
