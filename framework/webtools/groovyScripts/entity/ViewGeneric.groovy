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
import org.apache.ofbiz.entity.GenericPK
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.model.ModelReader
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.model.ModelField
import org.apache.ofbiz.entity.model.ModelFieldType
import org.apache.ofbiz.entity.model.ModelRelation
import org.apache.ofbiz.entity.model.ModelKeyMap
import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilMisc

String entityName = parameters.entityName
context.entityName = entityName

ModelReader reader = delegator.getModelReader()
ModelEntity entity = reader.getModelEntity(entityName)

context.entity = entity
context.plainTableName = entity.getPlainTableName()

boolean hasAllView = security.hasEntityPermission("ENTITY_DATA", "_VIEW", session)
boolean hasAllCreate = security.hasEntityPermission("ENTITY_DATA", "_CREATE", session)
boolean hasAllUpdate = security.hasEntityPermission("ENTITY_DATA", "_UPDATE", session)
boolean hasAllDelete = security.hasEntityPermission("ENTITY_DATA", "_DELETE", session)
boolean hasViewPermission = hasAllView || security.hasEntityPermission(entity.getPlainTableName(), "_VIEW", session)
boolean hasCreatePermission = hasAllCreate || security.hasEntityPermission(entity.getPlainTableName(), "_CREATE", session)
boolean hasUpdatePermission = hasAllUpdate || security.hasEntityPermission(entity.getPlainTableName(), "_UPDATE", session)
boolean hasDeletePermission = hasAllDelete || security.hasEntityPermission(entity.getPlainTableName(), "_DELETE", session)

context.hasAllView = hasAllView
context.hasAllCreate = hasAllCreate
context.hasAllUpdate = hasAllUpdate
context.hasAllDelete = hasAllDelete
context.hasViewPermission = hasViewPermission
context.hasCreatePermission = hasCreatePermission
context.hasUpdatePermission = hasUpdatePermission
context.hasDeletePermission = hasDeletePermission


// Resolve and prepare pkValues from request to support rest or oldest request call
Map<String, String> pkNamesValuesMap = null
if (parameters.pkValues) {
    pkNamesValuesMap = EntityUtil.getPkValuesMapFromPath(
            delegator.getModelEntity(entityName), parameters.pkValues)
    parameters << pkNamesValuesMap
}
GenericValue valueFromParameters = makeValue(entityName)
valueFromParameters.setPKFields(parameters)
GenericPK findByPK = valueFromParameters.getPrimaryKey()
context.currentFindString = UtilFormatOut.encodeQuery(EntityUtil.entityToPath(valueFromParameters))
context.findByPk = findByPK.toString()
context.pkNamesValuesMap = pkNamesValuesMap ?: valueFromParameters.getPrimaryKey().getAllFields()

GenericValue value = null
//only try to find it if this is a valid primary key...
if (findByPK.isPrimaryKey()) {
    value = from(findByPK.getEntityName()).where(findByPK).queryOne()
}
context.value = value

boolean useValue = value != null

if (value) {
    List fieldList = []
    Iterator fieldIterator = entity.getFieldsIterator()
    while (fieldIterator.hasNext()) {
        Map mapField = [:]

        ModelField field = fieldIterator.next()

        String fieldValue = UtilFormatOut.safeToString(value.get(field.getName()))
        mapField.name = field.getName()
        mapField.value = fieldValue

        fieldList << mapField
    }
    context.fields = fieldList
}

context.pkNotFound = !value && !findByPK.getAllFields().isEmpty()

String lastUpdateMode = parameters.get("_method")
if ((session.getAttribute("_ERROR_MESSAGE_") != null || request.getAttribute("_ERROR_MESSAGE_") != null) &&
    lastUpdateMode != null && "DELETE" != lastUpdateMode) {
    //if we are updating and there is an error, do not use the entity data for the fields, use parameters to get the old value
    useValue = false
}
context.useValue = useValue

List newFieldPkList = []
pkIterator = entity.getPksIterator()
while (pkIterator.hasNext()) {
    Map mapField = [:]

    ModelField field = pkIterator.next()
    ModelFieldType type = delegator.getEntityFieldType(entity, field.getType())

    String stringLength = ""
    String fieldName = field.getName()
    String fieldValue = findByPK && useValue ?
            UtilFormatOut.safeToString(findByPK.get(fieldName)) :
            (useValue ? "" : UtilFormatOut.checkNull(parameters.get(fieldName)))
    String javaType = StringUtil.split(type.getJavaType(), ".").last()
    String fieldType = javaType
    if ("Timestamp" == javaType) {
        fieldType = "DateTime"
    } else {
        if ("String" == javaType) {
            if (type.stringLength() <= 80) {
                fieldType = "StringOneRow"
            } else if (type.stringLength() > 255) {
                fieldType = "Textarea"
            }
            stringLength = type.stringLength().toString()
        }
    }
    mapField.name = fieldName
    mapField.value = fieldValue
    mapField.fieldType = fieldType
    mapField.stringLength = stringLength
    newFieldPkList << mapField
}
context.newFieldPkList = newFieldPkList

List newFieldNoPkList = []
Iterator noPkIterator = entity.getNopksIterator()
while (noPkIterator.hasNext()) {
    Map mapField = [:]

    ModelField field = noPkIterator.next()
    ModelFieldType type = delegator.getEntityFieldType(entity, field.getType())
    String fieldName = field.getName()
    String fieldValue =  useValue ?
            UtilFormatOut.safeToString(value.get(fieldName)) :
            UtilFormatOut.checkNull(parameters.get(fieldName))

    String javaType = StringUtil.split(type.getJavaType(), ".").last()
    String stringLength = ""
    String fieldType = javaType
    if ("Timestamp" == javaType) {
        fieldType = "DateTime"
    } else {
        if ("String" == javaType) {
            if (type.stringLength() <= 80) {
                fieldType = "StringOneRow"
            } else if (type.stringLength() > 255) {
                fieldType = "Textarea"
            }
            stringLength = type.stringLength().toString()
        }
    }
    mapField.name = fieldName
    mapField.value = fieldValue
    mapField.fieldType = fieldType
    mapField.stringLength = stringLength

    newFieldNoPkList << mapField
}
context.newFieldNoPkList = newFieldNoPkList

List relationFieldList = []
for (int relIndex = 0; relIndex < entity.getRelationsSize(); relIndex++) {
    Map mapRelation = [:]

    ModelRelation relation = entity.getRelation(relIndex)
    ModelEntity relatedEntity = reader.getModelEntity(relation.getRelEntityName())

    boolean relCreate = security.hasEntityPermission(relatedEntity.getPlainTableName(), "_CREATE", session)

    mapRelation.type = relation.getType()
    mapRelation.title = relation.getTitle()
    mapRelation.relEntityName = relation.getRelEntityName()
    mapRelation.sortName = relation.getTitle() + relation.getRelEntityName()
    mapRelation.relatedTable = relatedEntity.getEntityName()
    mapRelation.relCreate = relCreate

    if ("one" == relation.getType() || "one-nofk" == relation.getType()) {
        if (value) {
            if (hasAllView || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_VIEW", session)) {
                Iterator tempIter = UtilMisc.toIterator(value.getRelated(relation.getTitle() + relatedEntity.getEntityName(), null, null, false))
                GenericValue valueRelated = null
                if (tempIter && tempIter.hasNext()) {
                    valueRelated = (GenericValue) tempIter.next()
                }

                List relatedFieldsList = []
                Iterator relFieldIterator = relatedEntity.getFieldsIterator()
                while (relFieldIterator.hasNext()) {
                    Map mapRelatedFields = [:]
                    ModelField field = relFieldIterator.next()
                    ModelFieldType type = delegator.getEntityFieldType(entity, field.getType())
                    String fieldName = field.getName()

                    String fieldValue =  valueRelated ?
                            UtilFormatOut.safeToString(valueRelated.get(fieldName)) : ""

                    String javaType = StringUtil.split(type.getJavaType(), ".").last()
                    String fieldType = javaType
                    if ("Timestamp" == javaType) {
                        fieldType = "DateTime"
                    }

                    mapRelatedFields.name = fieldName
                    mapRelatedFields.type = fieldType
                    mapRelatedFields.value = fieldValue
                    relatedFieldsList << mapRelatedFields
                }

                mapRelation.valueRelated = valueRelated
                if (valueRelated) {
                    mapRelation.valueRelatedPk = valueRelated.getPrimaryKey().toString()
                }
                mapRelation.relatedFieldsList = relatedFieldsList
                mapRelation.relType = "one"

                String findString = "entityName=" + relatedEntity.getEntityName()
                for (ModelKeyMap keyMap : relation.getKeyMaps()) {
                    if (value.get(keyMap.getFieldName())) {
                        findString += "&" + keyMap.getRelFieldName() + "=" + value.get(keyMap.getFieldName())
                    }
                }
                String encodeFindString = UtilFormatOut.encodeQuery(findString)
                mapRelation.encodeRelatedEntityFindString = encodeFindString

                relationFieldList << mapRelation
            }
        }
    } else if (relation.getType() == "many") {
        if (value) {
            if (hasAllView || security.hasEntityPermission(relatedEntity.getPlainTableName(), "_VIEW", session)) {
                mapRelation.relType = "many"

                String findString = "entityName=" + relatedEntity.getEntityName()
                for (ModelKeyMap keyMap : relation.getKeyMaps()) {
                    if (value.get(keyMap.getFieldName())) {
                        findString += "&" + keyMap.getRelFieldName() + "=" + value.get(keyMap.getFieldName())
                    }
                }
                String encodeFindString = UtilFormatOut.encodeQuery(findString)
                mapRelation.encodeRelatedEntityFindString = encodeFindString

                relationFieldList << mapRelation
            }
        }
    }
}
context.relationFieldList = UtilMisc.sortMaps(relationFieldList, ["sortName"])
context.relSize = (relationFieldList.size() + 2)
