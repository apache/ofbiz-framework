/*
 * $Id: PrimaryKeyFinder.java 5720 2005-09-13 03:10:59Z jonesde $
 *
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.entity.finder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a condition
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.1
 */
public class PrimaryKeyFinder implements Serializable {
    public static final String module = PrimaryKeyFinder.class.getName();         
    
    protected FlexibleStringExpander entityNameExdr;
    protected FlexibleMapAccessor valueNameAcsr;
    protected FlexibleStringExpander useCacheExdr;
    protected FlexibleStringExpander autoFieldMapExdr;
    protected Map fieldMap;
    protected List selectFieldExpanderList;

    public PrimaryKeyFinder(Element entityOneElement) {
        this.entityNameExdr = new FlexibleStringExpander(entityOneElement.getAttribute("entity-name"));
        if (UtilValidate.isNotEmpty(entityOneElement.getAttribute("value-name")))
            this.valueNameAcsr = new FlexibleMapAccessor(entityOneElement.getAttribute("value-name"));
        this.useCacheExdr = new FlexibleStringExpander(entityOneElement.getAttribute("use-cache"));
        this.autoFieldMapExdr = new FlexibleStringExpander(entityOneElement.getAttribute("auto-field-map"));

        // process field-map
        this.fieldMap = EntityFinderUtil.makeFieldMap(entityOneElement);

        // process select-field
        selectFieldExpanderList = EntityFinderUtil.makeSelectFieldExpanderList(entityOneElement);
    }

    public void runFind(Map context, GenericDelegator delegator) throws GeneralException {
        String entityName = this.entityNameExdr.expandString(context);
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        
        String useCacheString = this.useCacheExdr.expandString(context);
        // default to false
        boolean useCacheBool = "true".equals(useCacheString);

        String autoFieldMapString = this.autoFieldMapExdr.expandString(context);
        // default to true
        boolean autoFieldMapBool = !"false".equals(autoFieldMapString);

        // assemble the field map
        Map entityContext = new HashMap();
        if (autoFieldMapBool) {
            GenericValue tempVal = delegator.makeValue(entityName, null);

            // try a map called "parameters", try it first so values from here are overriden by values in the main context
            Object parametersObj = context.get("parameters");
            if (parametersObj != null && parametersObj instanceof Map) {
                tempVal.setAllFields((Map) parametersObj, true, null, Boolean.TRUE);
            }

            // just get the primary keys, and hopefully will get all of them, if not they must be manually filled in below in the field-maps
            tempVal.setAllFields(context, true, null, Boolean.TRUE);

            entityContext.putAll(tempVal);
        }
        EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, entityContext);
        //Debug.logInfo("PrimaryKeyFinder: entityContext=" + entityContext, module);
        // then convert the types...
        modelEntity.convertFieldMapInPlace(entityContext, delegator);
        
        // get the list of fieldsToSelect from selectFieldExpanderList
        Set fieldsToSelect = EntityFinderUtil.makeFieldsToSelect(selectFieldExpanderList, context);
        
        //if fieldsToSelect != null and useCacheBool is true, throw an error
        if (fieldsToSelect != null && useCacheBool) {
            throw new IllegalArgumentException("Error in entity-one definition, cannot specify select-field elements when use-cache is set to true");
        }
        
        try {
            GenericValue valueOut = null;
            GenericPK entityPK = delegator.makePK(entityName, entityContext);

            // make sure we have a full primary key, if any field is null then just log a warning and return null instead of blowing up
            if (entityPK.containsPrimaryKey(true)) {
                if (useCacheBool) {
                    valueOut = delegator.findByPrimaryKeyCache(entityPK);
                } else {
                    if (fieldsToSelect != null) {
                        valueOut = delegator.findByPrimaryKeyPartial(entityPK, fieldsToSelect);
                    } else {
                        valueOut = delegator.findByPrimaryKey(entityPK);
                    }
                }
            } else {
                if (Debug.infoOn()) Debug.logInfo("Returning null because found incomplete primary key in find: " + entityPK, module);
            }
            
            //Debug.logInfo("PrimaryKeyFinder: valueOut=" + valueOut, module);
            //Debug.logInfo("PrimaryKeyFinder: going into=" + this.valueNameAcsr.getOriginalName(), module);
            if (valueNameAcsr != null) {
               this.valueNameAcsr.put(context, valueOut);
            } else {
               if (valueOut != null) {
                   context.putAll(valueOut);
               }
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error finding entity value by primary key with entity-one: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
    }
}

