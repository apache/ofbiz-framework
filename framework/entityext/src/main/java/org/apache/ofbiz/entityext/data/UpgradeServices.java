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
package org.apache.ofbiz.entityext.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Entity Data Upgrade Services
 *
 */

public class UpgradeServices {
    private static final String MODULE = UpgradeServices.class.getName();
    private static final String RESOURCE = "EntityExtUiLabels";


    /**
     * Generate sql file for data migration from mySql.5 and earlier version to mySql.6 to later version
     * mySql added support in 5.6 to support microseconds for datetime field.
     * https://dev.mysql.com/doc/refman/5.6/en/fractional-seconds.html
     * <ul>
     * <li>Service will take [groupName] as in param,</li>
     * <li>iterate all the entity and check for datetime and time field</li>
     * <li>it will generate alter table sql statement to update the field data type</li>
     * <li>datetime will be altered with DATETIME(3)</li>
     * <li>time will be altered with TIME(3)</li>
     * <li>sql fiel will be created at following location</li>
     * <li>${ofbiz.home}/runtime/tempfiles/[groupName].sql</li>
     * </ul>
     * @param dctx
     * @param context
     * @return Map with the success result of the service,
     */
    public static Map<String, Object> generateMySqlFileWithAlterTableForTimestamps(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            Debug.logError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtServicePermissionNotGranted", locale));
        }

        String groupName = (String) context.get("groupName");

        Map<String, ModelEntity> modelEntities;
        try (PrintWriter dataWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(System.getProperty("ofbiz.home") + "/runtime/tempfiles/" + groupName + ".sql")), "UTF-8")))) {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);

            /* TODO:
            1) fetch the meta data of the "date-time" field using the JDBC connection and JDBC meta data;
            2) compare it to date-time and only generate the alter statement if they differs;
            */

            dataWriter.println("SET FOREIGN_KEY_CHECKS=0;");
            for (ModelEntity modelEntity: modelEntities.values()) {
                List<ModelField> fields = modelEntity.getFieldsUnmodifiable();
                for (ModelField field: fields) {
                    if (modelEntity.getPlainTableName() != null) {
                        if ("date-time".equals(field.getType())) {
                            dataWriter.println("ALTER TABLE " + modelEntity.getPlainTableName() + " MODIFY " + field.getColName() + " DATETIME(3);");
                        }
                        if ("time".equals(field.getType())) {
                            dataWriter.println("ALTER TABLE " + modelEntity.getPlainTableName() + " MODIFY " + field.getColName() + " TIME(3);");
                        }
                    }
                }
            }
            dataWriter.println("SET FOREIGN_KEY_CHECKS=1;");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting list of entities in group: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtErrorGettingListOfEntityInGroup",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

}
