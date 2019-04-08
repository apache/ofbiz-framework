package org.apache.ofbiz.entityext.data;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Entity Data Upgrade Services
 *
 */

public class UpgradeServices {
    public static final String module = UpgradeServices.class.getName();
    public static final String resource = "EntityExtUiLabels";


    /**
     * Generate sql file for data migration from mySql.5 and earlier version to mySql.6 to later version
     * mySql added support in 5.6 to support microseconds for datetime field.
     * https://dev.mysql.com/doc/refman/5.6/en/fractional-seconds.html
     * <ul>
     * <li>Service will take groupName as in param,</li>
     * <li>iterate all the entity and check for datetime and time field</li>
     * <li>it will generate alter table sql statement to update the field data type</li>
     * <li>datetime will be altered with DATETIME(3)</li>
     * <li>time will be altered with TIME(3)</li>
     * <li>sql fiel will be created at following location</li>
     * <li>${ofbiz.home}/runtime/tempfiles/<groupName>.sql</></li>
     * </ul>
     * @param dctx
     * @param context
     * @return Map with the success result of the service,
     */
    public static Map<String, Object> generateMySqlFileWithAlterTableForTimestamps (DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            Debug.logError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale));
        }

        String groupName = (String) context.get("groupName");

        Map<String, ModelEntity> modelEntities;
        PrintWriter dataWriter = null;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
            dataWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(System.getProperty("ofbiz.home") + "/runtime/tempfiles/" + groupName + ".sql")), "UTF-8")));

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
            dataWriter.close();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting list of entities in group: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorGettingListOfEntityInGroup", UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            if (dataWriter != null)
                dataWriter.close();
        }

        return ServiceUtil.returnSuccess();
    }

}
