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
package org.apache.ofbiz.base.conversion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.model.DomainModel;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;

/** Model Converter classes. */
public class ModelConverters implements ConverterLoader {

    public static class HashMapToDomainModel extends AbstractModelConverter<LinkedHashMap<String, Object>, DomainModel> {

        public HashMapToDomainModel() {
            super(LinkedHashMap.class, DomainModel.class);
        }

        @Override
        public DomainModel convert(LinkedHashMap<String, Object> obj, String targetClassName) throws ConversionException {
            try {
                Class<?> modelClass = Class.forName(targetClassName);
                JSON jsonObj = JSON.from(obj);
                DomainModel target = UtilGenerics.cast(jsonObj.toObject(modelClass));
                return target;
            } catch (IOException | ClassNotFoundException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public DomainModel convert(LinkedHashMap<String, Object> obj) throws ConversionException {
            // cant convert without target class, so abort
            throw new ConversionException("Need target class to convert from HashMap to DomainModel");
        }
    }

    /**
     * Load this class <code>ModelConverters</code>.
     */
    public void loadConverters() {
        Converters.loadContainedConverters(ModelConverters.class);
    }

    /**
     * Method to convert serviceParameter List values to specific DomainModel
     * @param context         the service context map containing the list to convert
     * @param listName        the key under which the raw List is stored in the context
     * @param domainModelClass the target DomainModel class each element should be converted to
     * @return a List of converted DomainModel objects; an empty List if the raw list is missing or empty
     * @throws GeneralException
     */
    public static List<DomainModel> convertRawListToDomainModelType(Map<String, Object> context, String listName, Class<?> domainModelClass)
            throws GeneralException {
        List<Object> dataList = UtilGenerics.checkCollection(context.get(listName), Object.class);

        return convertRawListToDomainModelType(dataList, domainModelClass);
    }

    /**
     * Converts objects in list to specified DomainModel
     * @param dataList        the List of raw objects to convert
     * @param domainModelClass the target DomainModel class each element should be converted to
     * @return a List of converted DomainModel objects; an empty List if {@code dataList} is empty
     * @throws GeneralException
     */
    public static List<DomainModel> convertRawListToDomainModelType(List<Object> dataList, Class<?> domainModelClass) throws GeneralException {
        List<DomainModel> domainModelList = new ArrayList<>();
        if (UtilValidate.isEmpty(dataList)) {
            return domainModelList;
        }
        for (Object rawData : dataList) {
            DomainModel domainObject = (DomainModel) ObjectType.simpleTypeOrObjectConvert(rawData, domainModelClass.getName(), null, null);
            domainModelList.add(domainObject);
        }
        return domainModelList;
    }
}
