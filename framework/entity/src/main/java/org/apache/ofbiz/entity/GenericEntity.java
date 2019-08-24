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
package org.apache.ofbiz.entity;

import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TreeSet;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.Observable;
import org.apache.ofbiz.base.util.Observer;
import org.apache.ofbiz.base.util.TimeDuration;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.LocalizedMap;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFieldMap;
import org.apache.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.apache.ofbiz.entity.model.ModelViewEntity.ModelAlias;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
/**
 * Generic Entity Value Object - Handles persistence for any defined entity.
 * <p>Note that this class extends <code>Observable</code> to achieve change notification for
 * <code>Observer</code>s. Whenever a field changes the name of the field will be passed to
 * the <code>notifyObservers()</code> method, and through that to the <code>update()</code> method of each
 * <code>Observer</code>.</p>
 * <p>This class is not thread-safe. If an instance of this class is shared between threads,
 * then it should be made immutable by calling the <code>setImmutable()</code> method.</p>
 *
 */
@SuppressWarnings("serial")
public class GenericEntity implements Map<String, Object>, LocalizedMap<Object>, Serializable, Comparable<GenericEntity>, Cloneable {

    public static final String module = GenericEntity.class.getName();
    public static final GenericEntity NULL_ENTITY = new NullGenericEntity();
    public static final NullField NULL_FIELD = new NullField();

    // Do not restore observers during deserialization. Instead, client code must add observers.
    private transient Observable observable = new Observable();

    /** Name of the GenericDelegator, used to re-get the GenericDelegator when deserialized */
    private String delegatorName = null;

    /** Reference to an instance of GenericDelegator used to do some basic operations on this entity value. If null various methods in this class will fail. This is automatically set by the GenericDelegator for all GenericValue objects instantiated through it. You may set this manually for objects you instantiate manually, but it is optional. */
    private transient Delegator internalDelegator = null;

    /** A Map containing the original field values from the database.
     */
    private Map<String, Object> originalDbValues = null;

    /** Contains the fields for this entity. Note that this should always be a
     *  HashMap to allow for two things: non-synchronized reads (synchronized
     *  writes are done through synchronized setters) and being able to store
     *  null values. Null values are important because with them we can distinguish
     *  between desiring to set a value to null and desiring to not modify the
     *  current value on an update.
     */
    private Map<String, Object> fields = new HashMap<>();

    /** Contains the entityName of this entity, necessary for efficiency when creating EJBs */
    private String entityName = null;

    /** Contains the ModelEntity instance that represents the definition of this entity, not to be serialized */
    private transient ModelEntity modelEntity = null;

    private boolean generateHashCode = true;
    private int cachedHashCode = 0;

    /** Used to specify whether or not this representation of the entity can be changed; generally cleared when this object comes from a cache */
    private boolean mutable = true;

    /** This is an internal field used to specify that a value has come from a sync process and that the auto-stamps should not be over-written */
    private boolean isFromEntitySync = false;

    /** Creates new GenericEntity - Should never be used, prefer the other options. */
    protected GenericEntity() { }

    /** Creates new GenericEntity */
    public static GenericEntity createGenericEntity(ModelEntity modelEntity) {
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
        }

        GenericEntity newEntity = new GenericEntity();
        newEntity.init(modelEntity);
        return newEntity;
    }

    /** Creates new GenericEntity from existing Map */
    public static GenericEntity createGenericEntity(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fields) {
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
        }

        GenericEntity newEntity = new GenericEntity();
        newEntity.init(delegator, modelEntity, fields);
        return newEntity;
    }

    /** Copy Factory Method: Creates new GenericEntity from existing GenericEntity */
    public static GenericEntity createGenericEntity(GenericEntity value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null value parameter");
        }

        GenericEntity newEntity = new GenericEntity();
        newEntity.init(value);
        return newEntity;
    }

    protected void assertIsMutable() {
        if (!this.mutable) {
            String msg = "This object has been flagged as immutable (unchangeable), probably because it came from an Entity Engine cache. Cannot modify an immutable entity object. Use the clone method to create a mutable copy of this object.";
            IllegalStateException toBeThrown = new IllegalStateException(msg);
            Debug.logError(toBeThrown, module);
            throw toBeThrown;
        }
    }

    private Observable getObservable() {
        if (this.observable == null) {
            this.observable = new Observable();
        }
        return this.observable;
    }

    /** Creates new GenericEntity */
    protected void init(ModelEntity modelEntity) {
        assertIsMutable();
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
        }
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.observable = new Observable();

        // check some things
        if (this.entityName == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
        }
    }

    /** Creates new GenericEntity from existing Map */
    protected void init(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fields) {
        assertIsMutable();
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
        }
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.delegatorName = delegator.getDelegatorName();
        this.internalDelegator = delegator;
        this.observable = new Observable();
        setFields(fields);

        // check some things
        if (this.entityName == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
        }
    }

    /** Creates new GenericEntity from existing Map */
    protected void init(Delegator delegator, ModelEntity modelEntity, Object singlePkValue) {
        assertIsMutable();
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
        }
        if (modelEntity.getPksSize() != 1) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with more than one primary key field");
        }
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        this.delegatorName = delegator.getDelegatorName();
        this.internalDelegator = delegator;
        this.observable = new Observable();
        set(modelEntity.getOnlyPk().getName(), singlePkValue);

        // check some things
        if (this.entityName == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
        }
    }

    /** Copy Constructor: Creates new GenericEntity from existing GenericEntity */
    protected void init(GenericEntity value) {
        assertIsMutable();
        // check some things
        if (value.entityName == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
        }
        this.entityName = value.getEntityName();
        // NOTE: could call getModelEntity to insure we have a value, just in case the value passed in has been serialized, but might as well leave it null to keep the object light if it isn't there
        this.modelEntity = value.modelEntity;
        if (value.fields != null) {
            this.fields.putAll(value.fields);
        }
        this.delegatorName = value.delegatorName;
        this.internalDelegator = value.internalDelegator;
        this.observable = new Observable(value.observable);
    }

    public void reset() {
        assertIsMutable();
        // from GenericEntity
        this.delegatorName = null;
        this.internalDelegator = null;
        this.originalDbValues = null;
        this.fields = new HashMap<>();
        this.entityName = null;
        this.modelEntity = null;
        this.generateHashCode = true;
        this.cachedHashCode = 0;
        this.mutable = true;
        this.isFromEntitySync = false;
        this.observable = new Observable();
    }

    public void refreshFromValue(GenericEntity newValue) throws GenericEntityException {
        assertIsMutable();
        if (newValue == null) {
            throw new GenericEntityException("Could not refresh value, new value not found for: " + this);
        }
        GenericPK thisPK = this.getPrimaryKey();
        GenericPK newPK = newValue.getPrimaryKey();
        if (!thisPK.equals(newPK)) {
            throw new GenericEntityException("Could not refresh value, new value did not have the same primary key; this PK=" + thisPK + ", new value PK=" + newPK);
        }
        this.fields = new HashMap<>(newValue.fields);
        this.setDelegator(newValue.getDelegator());
        this.generateHashCode = newValue.generateHashCode;
        this.cachedHashCode = newValue.cachedHashCode;
        this.observable = new Observable(newValue.observable);
    }

    /**
     *
     * @deprecated Use hasChanged()
     */
    @Deprecated
    public boolean isModified() {
        return this.hasChanged();
    }

    /**
     * Flags this object as being synchronized with the data source.
     * The entity engine will call this method immediately after
     * populating this object with data from the data source.
     */
    public void synchronizedWithDatasource() {
        assertIsMutable();
        this.originalDbValues = Collections.unmodifiableMap(getAllFields());
        this.clearChanged();
    }

    /**
     * Flags this object as being removed from the data source.
     * The entity engine will call this method immediately after
     * removing this value from the data source. Once this method is
     * called, the object is immutable.
     */
    public void removedFromDatasource() {
        assertIsMutable();
        this.clearChanged();
        this.setImmutable();
    }

    public boolean isMutable() {
        return this.mutable;
    }

    public void setImmutable() {
        if (this.mutable) {
            this.mutable = false;
            this.fields = Collections.unmodifiableMap(this.fields);
        }
    }

    /**
     * @return Returns the isFromEntitySync.
     */
    public boolean getIsFromEntitySync() {
        return this.isFromEntitySync;
    }

    /**
     * @param isFromEntitySync The isFromEntitySync to set.
     */
    public void setIsFromEntitySync(boolean isFromEntitySync) {
        assertIsMutable();
        this.isFromEntitySync = isFromEntitySync;
    }

    public String getEntityName() {
        return entityName;
    }

    public ModelEntity getModelEntity() {
        if (modelEntity == null) {
            if (entityName != null) {
                modelEntity = this.getDelegator().getModelEntity(entityName);
            }
            if (modelEntity == null) {
                throw new IllegalStateException("[GenericEntity.getModelEntity] could not find modelEntity for entityName " + entityName);
            }
        }
        return modelEntity;
    }

    /** Get the GenericDelegator instance that created this value object and that is responsible for it.
     *@return GenericDelegator object
     */
    public Delegator getDelegator() {
        if (internalDelegator == null) {
            if (delegatorName == null) {
                delegatorName = "default";
            }
            internalDelegator = DelegatorFactory.getDelegator(delegatorName);
            if (internalDelegator == null) {
                throw new IllegalStateException("[GenericEntity.getDelegator] could not find delegator with name " + delegatorName);
            }
        }
        return internalDelegator;
    }

    /** Set the GenericDelegator instance that created this value object and that is responsible for it. */
    public void setDelegator(Delegator internalDelegator) {
        assertIsMutable();
        if (internalDelegator == null) {
            return;
        }
        this.delegatorName = internalDelegator.getDelegatorName();
        this.internalDelegator = internalDelegator;
    }

    public Object get(String name) {
        if (getModelEntity().getField(name) == null) {
            throw new IllegalArgumentException("The field name (or key) [" + name + "] is not valid for entity [" + this.getEntityName() + "].");
        }
        return fields.get(name);
    }

    /** Returns true if the entity contains all of the primary key fields, but NO others. */
    public boolean isPrimaryKey() {
        return isPrimaryKey(false);
    }
    public boolean isPrimaryKey(boolean requireValue) {
        TreeSet<String> fieldKeys = new TreeSet<>(this.fields.keySet());
        for (ModelField curPk: this.getModelEntity().getPkFieldsUnmodifiable()) {
            String fieldName = curPk.getName();
            if (requireValue) {
                if (this.fields.get(fieldName) == null) {
                    return false;
                }
            } else {
                if (!this.fields.containsKey(fieldName)) {
                    return false;
                }
            }
            fieldKeys.remove(fieldName);
        }
        return fieldKeys.isEmpty();
    }

    /** Returns true if the entity contains all of the primary key fields. */
    public boolean containsPrimaryKey() {
        return containsPrimaryKey(false);
    }
    public boolean containsPrimaryKey(boolean requireValue) {
        for (ModelField curPk: this.getModelEntity().getPkFieldsUnmodifiable()) {
            String fieldName = curPk.getName();
            if (requireValue) {
                if (this.fields.get(fieldName) == null) {
                    return false;
                }
            } else {
                if (!this.fields.containsKey(fieldName)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getPkShortValueString() {
        StringBuilder sb = new StringBuilder();
        for (ModelField curPk: this.getModelEntity().getPkFieldsUnmodifiable()) {
            if (sb.length() > 0) {
                sb.append("::");
            }
            sb.append(this.get(curPk.getName()));
        }
        return sb.toString();
    }

    /** Sets the named field to the passed value, even if the value is null
     * @param name The field name to set
     * @param value The value to set
     */
    public void set(String name, Object value) {
        set(name, value, true);
    }

    /** Sets the named field to the passed value. If value is null, it is only
     *  set if the setIfNull parameter is true. This is useful because an update
     *  will only set values that are included in the HashMap and will store null
     *  values in the HashMap to the datastore. If a value is not in the HashMap,
     *  it will be left unmodified in the datastore.
     * @param name The field name to set
     * @param value The value to set
     * @param setIfNull Specifies whether or not to set the value if it is null
     */
    public Object set(String name, Object value, boolean setIfNull) {
        assertIsMutable();
        ModelField modelField = getModelEntity().getField(name);
        if (modelField == null) {
            throw new IllegalArgumentException("[GenericEntity.set] \"" + name + "\" is not a field of " + entityName + ", must be one of: " + getModelEntity().fieldNameString());
        }
        if (value != null || setIfNull) {
            ModelFieldType type = null;
            try {
                type = getDelegator().getEntityFieldType(getModelEntity(), modelField.getType());
            } catch (IllegalStateException | GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            if (type == null) {
                throw new IllegalArgumentException("Type " + modelField.getType() + " not found for entity [" + this.getEntityName() + "]; probably because there is no datasource (helper) setup for the entity group that this entity is in: [" + this.getDelegator().getEntityGroupName(this.getEntityName()) + "]");
            }

            if (value instanceof Boolean) {
                // if this is a Boolean check to see if we should convert from an indicator or just leave as is
                try {
                    int fieldType = SqlJdbcUtil.getType(type.getJavaType());
                    if (fieldType != 10) {
                        value = (Boolean) value ? "Y" : "N";
                    }
                } catch (GenericNotImplementedException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            } else if (value != null && !(value instanceof NULL)) {
                // make sure the type matches the field Java type
                if (value instanceof TimeDuration) {
                    try {
                        value = ObjectType.simpleTypeOrObjectConvert(value, type.getJavaType(), null, null);
                    } catch (GeneralException e) {
                        Debug.logError(e, module);
                    }
                } else if ((value instanceof String) && "byte[]".equals(type.getJavaType())) {
                    value = ((String) value).getBytes(StandardCharsets.UTF_8);
                }
                if (!ObjectType.instanceOf(value, type.getJavaType())) {
                    if (!("java.sql.Blob".equals(type.getJavaType()) && (value instanceof byte[] || value == null || ByteBuffer.class.isInstance(value)))) {
                        String errMsg = "In entity field [" + this.getEntityName() + "." + name + "] set the value passed in [" + value.getClass().getName() + "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                        // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                        Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning GenericEntity.set =-=-=-=-=-=-=-=-= " + errMsg, module);
                    }
                }
            }
            Object old = fields.put(name, value);

            generateHashCode = true;
            this.setChanged();
            this.notifyObservers(name);
            return old;
        }
        return fields.get(name);
    }

    public void dangerousSetNoCheckButFast(ModelField modelField, Object value) {
        assertIsMutable();
        if (modelField == null) {
            throw new IllegalArgumentException("Cannot set field with a null modelField");
        }
        generateHashCode = true;
        this.fields.put(modelField.getName(), value);
        this.setChanged();
        this.notifyObservers(modelField.getName());
    }

    public Object dangerousGetNoCheckButFast(ModelField modelField) {
        if (modelField == null) {
            throw new IllegalArgumentException("Cannot get field with a null modelField");
        }
        return this.fields.get(modelField.getName());
    }

    /** Sets the named field to the passed value, converting the value from a String to the corrent type using <code>Type.valueOf()</code>
     * @param name The field name to set
     * @param value The String value to convert and set
     */
    public void setString(String name, String value) {
        if (value == null) {
            set(name, null);
            return;
        }

        boolean isNullString = false;
        if ("null".equals(value) || "[null-field]".equals(value)) { // keep [null-field] but it'not used now
            // count this as a null too, but only for numbers and stuff, not for Strings
            isNullString = true;
        }

        ModelField field = getModelEntity().getField(name);
        if (field == null)
         {
            set(name, value); // this will get an error in the set() method...
        }

        ModelFieldType type = null;
        try {
            if (field != null) {
                type = getDelegator().getEntityFieldType(getModelEntity(), field.getType());
            }
        } catch (IllegalStateException | GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (type == null) {
            throw new IllegalArgumentException("Type " + field.getType() + " not found");
        }
        String fieldType = type.getJavaType();

        try {
            switch (SqlJdbcUtil.getType(fieldType)) {
            case 1:
                set(name, value);
                break;

            case 2:
                set(name, isNullString ? null : java.sql.Timestamp.valueOf(value));
                break;

            case 3:
                set(name, isNullString ? null : java.sql.Time.valueOf(value));
                break;

            case 4:
                set(name, isNullString ? null : java.sql.Date.valueOf(value));
                break;

            case 5:
                set(name, isNullString ? null : Integer.valueOf(value));
                break;

            case 6:
                set(name, isNullString ? null : Long.valueOf(value));
                break;

            case 7:
                set(name, isNullString ? null : Float.valueOf(value));
                break;

            case 8:
                set(name, isNullString ? null : Double.valueOf(value));
                break;

            case 9: // BigDecimal
                set(name, isNullString ? null : new BigDecimal(value));
                break;

            case 10:
                set(name, isNullString ? null : Boolean.valueOf(value));
                break;

            case 11: // Object
                set(name, value);
                break;

            case 12: // java.sql.Blob
                // TODO: any better way to handle Blob from String?
                set(name, value);
                break;

            case 13: // java.sql.Clob
                // TODO: any better way to handle Clob from String?
                set(name, value);
                break;

            case 14: // java.util.Date
                set(name, UtilDateTime.toDate(value));
                break;

            case 15: // java.util.Collection
                // TODO: how to convert from String to Collection? ie what should the default behavior be?
                set(name, value);
                break;
            }
        } catch (GenericNotImplementedException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /** Sets a field with an array of bytes, wrapping them automatically for easy use.
     * @param name The field name to set
     * @param bytes The byte array to be wrapped and set
     */
    public void setBytes(String name, byte[] bytes) {
        this.set(name, bytes);
    }

    public void setNextSeqId() {
        List<String> pkFieldNameList = this.modelEntity.getPkFieldNames();
        if (pkFieldNameList.size() != 1) {
            throw new IllegalArgumentException("Cannot setNextSeqId for entity [" + this.getEntityName() + "] that does not have a single primary key field, instead has [" + pkFieldNameList.size() + "]");
        }

        String pkFieldName = pkFieldNameList.get(0);
        if (this.get(pkFieldName) != null) {
            // don't throw exception, too much of a pain and usually intended: throw new IllegalArgumentException("Cannot setNextSeqId, pk field [" + pkFieldName + "] of entity [" + this.getEntityName() + "] already has a value [" + this.get(pkFieldName) + "]");
        }

        String sequencedValue = this.getDelegator().getNextSeqId(this.getEntityName());
        this.set(pkFieldName, sequencedValue);
    }

    public Boolean getBoolean(String name) {
        Object obj = get(name);

        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof String) {
            String value = (String) obj;

            if ("Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            } else if ("N".equalsIgnoreCase(value) || "F".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("getBoolean could not map the String '" + value + "' to Boolean type");
            }
        } else {
            throw new IllegalArgumentException("getBoolean could not map the object '" + obj.toString() + "' to Boolean type, unknown object type: " + obj.getClass().getName());
        }
    }

    /** Returns the specified field as a <code>TimeDuration</code> instance.
     * The field's Java data type can be either <code>String</code> or
     * <code>Number</code>. Invalid Java data types will throw
     * <code>IllegalArgumentException</code>.
     *
     * @param name The name of the desired field
     * @return A <code>TimeDuration</code> instance or <code>null</code>
     */
    public TimeDuration getDuration(String name) {
        Object obj = get(name);
        if (obj == null) {
            return null;
        }
        try {
            Number number = (Number) obj;
            return TimeDuration.fromNumber(number);
        } catch (Exception e) {}
        try {
            String duration = (String) obj;
            return TimeDuration.parseDuration(duration);
        } catch (Exception e) {}
        throw new IllegalArgumentException("getDuration could not map the object '" + obj.toString() + "' to TimeDuration type, incompatible object type: " + obj.getClass().getName());
    }

    public String getString(String name) {
        Object object = get(name);
        return object == null ? null : object.toString();
    }

    public java.sql.Timestamp getTimestamp(String name) {
        return (java.sql.Timestamp) get(name);
    }

    public java.sql.Time getTime(String name) {
        return (java.sql.Time) get(name);
    }

    public java.sql.Date getDate(String name) {
        return (java.sql.Date) get(name);
    }

    public Integer getInteger(String name) {
        return (Integer) get(name);
    }

    public Long getLong(String name) {
        return (Long) get(name);
    }

    public Float getFloat(String name) {
        return (Float) get(name);
    }

    public Double getDouble(String name) {
        // this "hack" is needed for now until the Double/BigDecimal issues are all resolved
        Object value = get(name);
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        return (Double) value;
    }

    public BigDecimal getBigDecimal(String name) {
        // this "hack" is needed for now until the Double/BigDecimal issues are all resolved
        // NOTE: for things to generally work properly BigDecimal should really be used as the java-type in the field type def XML files
        Object value = get(name);
        if (value instanceof Double) {
            return new BigDecimal((Double) value);
        }
        return (BigDecimal) value;
    }

    @SuppressWarnings("deprecation")
    public byte[] getBytes(String name) {
        Object value = get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Blob) {
            try {
                Blob valueBlob = (Blob) value;
                return valueBlob.getBytes(1, (int) valueBlob.length());
            } catch (SQLException e) {
                String errMsg = "Error getting byte[] from Blob: " + e.toString();
                Debug.logError(e, errMsg, module);
                return null;
            }
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof org.apache.ofbiz.entity.util.ByteWrapper) {
            // NOTE DEJ20071022: the use of ByteWrapper is not recommended and is deprecated, only old data should be stored that way
            Debug.logWarning("Found a ByteWrapper object in the database for field [" + this.getEntityName() + "." + name + "]; converting to byte[] and returning, but note that you need to update your database to unwrap these objects for future compatibility", module);
            org.apache.ofbiz.entity.util.ByteWrapper wrapper = (org.apache.ofbiz.entity.util.ByteWrapper) value;
            return wrapper.getBytes();
        }
        // uh-oh, this shouldn't happen...
        throw new IllegalArgumentException("In call to getBytes the value is not a supported type, should be byte[] or ByteWrapper, is: " + value.getClass().getName());
    }

    /** Checks a resource bundle for a value for this field using the entity name, the field name
     *    and a composite of the Primary Key field values as a key. If no value is found in the
     *    resource then the field value is returned. Uses the default-resource-name from the entity
     *    definition as the resource name. To specify a resource name manually, use the other getResource method.
     *
     *  So, the key in the resource bundle (properties file) should be as follows:
     *    &lt;entity-name&gt;.&lt;field-name&gt;.&lt;pk-field-value-1&gt;.&lt;pk-field-value-2&gt;...&lt;pk-field-value-n&gt;
     *  For example:
     *    ProductType.description.FINISHED_GOOD
     *
     * @param name The name of the field on the entity
     * @param locale The locale to use when finding the ResourceBundle, if null uses the default
     *    locale for the current instance of Java
     * @return If the corresponding resource is found and contains a key as described above, then that
     *    property value is returned; otherwise returns the field value
     */
    @Override
    public Object get(String name, Locale locale) {
        return get(name, null, locale);
    }

    /** Same as the getResource method that does not take resource name, but instead allows manually
     *    specifying the resource name. In general you should use the other method for more consistent
     *    naming and use of the corresponding properties files.
     * @param name The name of the field on the entity
     * @param resource The name of the resource to get the value from; if null defaults to the
     *    default-resource-name on the entity definition, if specified there
     * @param locale The locale to use when finding the ResourceBundle, if null uses the default
     *    locale for the current instance of Java
     * @return If the specified resource is found and contains a key as described above, then that
     *    property value is returned; otherwise returns the field value
     */
    public Object get(String name, String resource, Locale locale) {
        Object fieldValue = get(name);
        // In case of view entity first try to retrieve with View field names
        ModelEntity modelEntityToUse = this.getModelEntity();
        Object resourceValue = get(this.getModelEntity(), modelEntityToUse, name, resource, locale);
        if (resourceValue == null) {
          if (modelEntityToUse instanceof ModelViewEntity) {
              //  now try to retrieve with the field heading from the real entity linked to the view
              ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntityToUse;
              Iterator<ModelAlias> it = modelViewEntity.getAliasesIterator();
              while (it.hasNext()) {
                  ModelAlias modelAlias = it.next();
                  if (modelAlias.getName().equalsIgnoreCase(name)) {
                      modelEntityToUse = modelViewEntity.getMemberModelEntity(modelAlias.getEntityAlias());
                      name = modelAlias.getField();
                      break;
                  }
              }
              resourceValue = get(this.getModelEntity(), modelEntityToUse, name, resource, locale);
              if (resourceValue == null) {
                  return fieldValue;
              }
            return resourceValue;
          }
        return fieldValue;
        }
        return resourceValue;
    }

    /**
     * call by the previous method to be able to read with View entityName and entity Field and after for real entity
     * @param modelEntity the modelEntity, for a view it's the ViewEntity
     * @param modelEntityToUse, same as before except if it's a second call for a view, and so it's the real modelEntity
     * @return null or resourceValue
     */
    private Object get(ModelEntity modelEntity, ModelEntity modelEntityToUse, String name, String resource, Locale locale) {
        if (UtilValidate.isEmpty(resource)) {
            resource = modelEntityToUse.getDefaultResourceName();
            // still empty? return null
            if (UtilValidate.isEmpty(resource)) {
                return null;
            }
        }
        if (UtilProperties.isPropertiesResourceNotFound(resource, locale, false)) {
            // Properties do not exist for this resource+locale combination
            return null;
        }
        ResourceBundle bundle = null;
        try {
            bundle = UtilProperties.getResourceBundle(resource, locale);
        } catch (IllegalArgumentException e) {
            bundle = null;
        }
        if (bundle == null) {
            return null;
        }

        StringBuilder keyBuffer = new StringBuilder();
        // start with the Entity Name
        keyBuffer.append(modelEntityToUse.getEntityName());
        // next add the Field Name
        keyBuffer.append('.');
        keyBuffer.append(name);
        // finish off by adding the values of all PK fields
        if (modelEntity instanceof ModelViewEntity){
            // retrieve pkNames of realEntity
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            List<String> pkNamesToUse = new LinkedList<>();
            // iterate on realEntity for pkField
            Iterator<ModelField> iter = modelEntityToUse.getPksIterator();
            while (iter != null && iter.hasNext()) {
                ModelField curField = iter.next();
                String pkName = null;
                Iterator<ModelAlias> iterAlias = modelViewEntity.getAliasesIterator();
                //search aliasName for pkField of realEntity
                while (iterAlias != null && iterAlias.hasNext()) {
                    ModelAlias aliasField = iterAlias.next();
                    if (aliasField.getField().equals(curField.getName())){
                        ModelEntity memberModelEntity = modelViewEntity.getMemberModelEntity(aliasField.getEntityAlias());
                        if (memberModelEntity.getEntityName().equals(modelEntityToUse.getEntityName())) {
                            pkName = aliasField.getName();
                            break;
                        }
                    }
                }
                if (pkName == null) {
                    pkName = curField.getName();
                }
                pkNamesToUse.add(pkName);
            }
            // read value with modelEntity name of pkNames
            for (String pkName : pkNamesToUse) {
                if (this.containsKey(pkName)) {
                    keyBuffer.append('.');
                    keyBuffer.append(this.get(pkName));
                }
            }
        } else {
            Iterator<ModelField> iter = modelEntity.getPksIterator();
            while (iter != null && iter.hasNext()) {
                ModelField curField = iter.next();
                keyBuffer.append('.');
                keyBuffer.append(this.get(curField.getName()));
            }
        }

        String bundleKey = keyBuffer.toString();

        Object resourceValue = null;
        try {
            resourceValue = bundle.getObject(bundleKey);
        } catch (MissingResourceException e) {
            return null;
        }
        return resourceValue;
    }

    public GenericPK getPrimaryKey() {
        return GenericPK.create(this.getDelegator(), getModelEntity(), getFields(getModelEntity().getPkFieldNames()));
    }

    /** go through the pks and for each one see if there is an entry in fields to set */
    public void setPKFields(Map<? extends Object, ? extends Object> fields) {
        setAllFields(fields, true, null, Boolean.TRUE);
    }

    /** go through the pks and for each one see if there is an entry in fields to set */
    public void setPKFields(Map<? extends Object, ? extends Object> fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, null, Boolean.TRUE);
    }

    /** go through the non-pks and for each one see if there is an entry in fields to set */
    public void setNonPKFields(Map<? extends Object, ? extends Object> fields) {
        setAllFields(fields, true, null, Boolean.FALSE);
    }

    /** go through the non-pks and for each one see if there is an entry in fields to set */
    public void setNonPKFields(Map<? extends Object, ? extends Object> fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, null, Boolean.FALSE);
    }


    /** Intelligently sets fields on this entity from the Map of fields passed in
     * @param fields The fields Map to get the values from
     * @param setIfEmpty Used to specify whether empty/null values in the field Map should over-write non-empty values in this entity
     * @param namePrefix If not null or empty will be pre-pended to each field name (upper-casing the first letter of the field name first), and that will be used as the fields Map lookup name instead of the field-name
     * @param pks If null, get all values, if TRUE just get PKs, if FALSE just get non-PKs
     */
    public void setAllFields(Map<? extends Object, ? extends Object> fields, boolean setIfEmpty, String namePrefix, Boolean pks) {
        if (fields == null) {
            return;
        }
        Iterator<ModelField> iter = null;
        if (pks != null) {
            if (pks) {
                iter = this.getModelEntity().getPksIterator();
            } else {
                iter = this.getModelEntity().getNopksIterator();
            }
        } else {
            iter = this.getModelEntity().getFieldsIterator();
        }

        while (iter != null && iter.hasNext()) {
            ModelField curField = iter.next();
            String fieldName = curField.getName();
            String sourceFieldName = null;
            if (UtilValidate.isNotEmpty(namePrefix)) {
                sourceFieldName = namePrefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            } else {
                sourceFieldName = curField.getName();
            }

            if (fields.containsKey(sourceFieldName)) {
                Object field = fields.get(sourceFieldName);

                if (setIfEmpty) {
                    // if empty string, set to null
                    if (field != null && field instanceof String && ((String) field).length() == 0) {
                        this.set(curField.getName(), null);
                    } else {
                        this.set(curField.getName(), field);
                    }
                } else {
                    // okay, only set if not empty...
                    if (field != null) {
                        // if it's a String then we need to check length, otherwise set it because it's not null
                        if (field instanceof String) {
                            String fieldStr = (String) field;

                            if (fieldStr.length() > 0) {
                                this.set(curField.getName(), fieldStr);
                            }
                        } else {
                            this.set(curField.getName(), field);
                        }
                    }
                }
            }
        }
    }

    /** Returns keys of entity fields
     * @return java.util.Collection
     */
    public Collection<String> getAllKeys() {
        return fields.keySet();
    }

    /** Returns key/value pairs of entity fields
     * @return java.util.Map
     */
    public Map<String, Object> getAllFields() {
        return new HashMap<>(this.fields);
    }

    /** Used by clients to specify exactly the fields they are interested in
     * @param keysofFields the name of the fields the client is interested in
     * @return java.util.Map
     */
    public Map<String, Object> getFields(Collection<String> keysofFields) {
        if (keysofFields == null) {
            return null;
        }
        Map<String, Object> aMap = new HashMap<>();

        for (String aKey: keysofFields) {
            aMap.put(aKey, this.fields.get(aKey));
        }
        return aMap;
    }

    /** Used by clients to update particular fields in the entity
     * @param keyValuePairs java.util.Map
     */
    public void setFields(Map<? extends String, ? extends Object> keyValuePairs) {
        if (keyValuePairs == null) {
            return;
        }
        // this could be implement with Map.putAll, but we'll leave it like this for the extra features it has
        for (Map.Entry<? extends String, ? extends Object> anEntry: keyValuePairs.entrySet()) {
            this.set(anEntry.getKey(), anEntry.getValue(), true);
        }
    }

    public boolean matchesFields(Map<String, ? extends Object> keyValuePairs) {
        if (fields == null) {
            return true;
        }
        if (UtilValidate.isEmpty(keyValuePairs)) {
            return true;
        }
        for (Map.Entry<String, ? extends Object> anEntry: keyValuePairs.entrySet()) {
            if (!Objects.equals(anEntry.getValue(), this.fields.get(anEntry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** Used to indicate if locking is enabled for this entity
     * @return True if locking is enabled
     */
    public boolean lockEnabled() {
        return getModelEntity().lock();
    }

    // ======= XML Related Methods ========
    public static Document makeXmlDocument(Collection<GenericValue> values) {
        Document document = UtilXml.makeEmptyXmlDocument("entity-engine-xml");

        if (document == null) {
            return null;
        }

        addToXmlDocument(values, document);
        return document;
    }

    public static int addToXmlDocument(Collection<GenericValue> values, Document document) {
        return addToXmlElement(values, document, document.getDocumentElement());
    }

    public static int addToXmlElement(Collection<GenericValue> values, Document document, Element element) {
        if (values == null) {
            return 0;
        }
        if (document == null) {
            return 0;
        }

        int numberAdded = 0;

        for (GenericValue value: values) {
            Element valueElement = value.makeXmlElement(document);

            element.appendChild(valueElement);
            numberAdded++;
        }
        return numberAdded;
    }

    /** Makes an XML Element object with an attribute for each field of the entity
     *@param document The XML Document that the new Element will be part of
     *@return org.w3c.dom.Element object representing this generic entity
     */
    public Element makeXmlElement(Document document) {
        return makeXmlElement(document, null);
    }

    /** Makes an XML Element object with an attribute for each field of the entity
     *@param document The XML Document that the new Element will be part of
     *@param prefix A prefix to put in front of the entity name in the tag name
     *@return org.w3c.dom.Element object representing this generic entity
     */
    public Element makeXmlElement(Document document, String prefix) {
        Element element = null;

        if (prefix == null) {
            prefix = "";
        }
        if (document != null) {
            element = document.createElement(prefix + this.getEntityName());
        }
        if (element == null) {
            return null;
        }

        Iterator<ModelField> modelFields = this.getModelEntity().getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String name = modelField.getName();
            String value = this.getString(name);

            if (value != null) {
                if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                    UtilXml.addChildElementCDATAValue(element, name, value, document);
                } else {
                    element.setAttribute(name, value);
                }
            }
        }

        return element;
    }

    /** Writes XML text with an attribute or CDATA element for each field of the entity
     *@param writer A PrintWriter to write to
     *@param prefix A prefix to put in front of the entity name in the tag name
     */
    public void writeXmlText(PrintWriter writer, String prefix) {
        int indent = 4;
        StringBuilder indentStrBuf = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentStrBuf.append(' ');
        }
        String indentString = indentStrBuf.toString();

        if (prefix == null) {
            prefix = "";
        }

        writer.print(indentString);
        writer.print('<');
        writer.print(prefix);
        writer.print(this.getEntityName());

        // write attributes immediately and if a CDATA element is needed, put those in a Map for now
        Map<String, String> cdataMap = new HashMap<>();

        Iterator<ModelField> modelFields = this.getModelEntity().getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String name = modelField.getName();

            String type = modelField.getType();
            if (type != null && "blob".equals(type)) {
                Object obj = get(name);
                boolean b1 = obj instanceof byte [];
                if (b1) {
                    byte [] binData = (byte [])obj;
                    String strData = new String(Base64.getMimeEncoder().encode(binData), StandardCharsets.UTF_8);
                    cdataMap.put(name, strData);
                } else {
                    Debug.logWarning("Field:" + name + " is not of type 'byte[]'. obj: " + obj, module);
                }
            } else {
                String valueStr = this.getString(name);

                if (valueStr != null) {
                    StringBuilder value = new StringBuilder(valueStr);
                    boolean needsCdata = false;

                    // check each character, if line-feed or carriage-return is found set needsCdata to true; also look for invalid characters
                    for (int i = 0; i < value.length(); i++) {
                        char curChar = value.charAt(i);
                        /*
                         * Some common character for these invalid values, have seen these are mostly from MS Word, but may be part of some standard:
                         * 5 = ... 18 = apostrophe 19 = left quotation mark 20 = right quotation mark 22 = – 23 = - 25 = tm
                         */

                        switch (curChar) {
                        case '\'':
                            value.replace(i, i+1, "&apos;");
                            break;
                        case '"':
                            value.replace(i, i+1, "&quot;");
                            break;
                        case '&':
                            value.replace(i, i+1, "&amp;");
                            break;
                        case '<':
                            value.replace(i, i+1, "&lt;");
                            break;
                        case '>':
                            value.replace(i, i+1, "&gt;");
                            break;
                        case 0xA: // newline, \n
                            needsCdata = true;
                            break;
                        case 0xD: // carriage return, \r
                            needsCdata = true;
                            break;
                        case 0x9: // tab
                            // do nothing, just catch here so it doesn't get into the default
                            break;
                        case 0x5: // elipses (...)
                            value.replace(i, i+1, "...");
                            break;
                        case 0x12: // apostrophe
                            value.replace(i, i+1, "&apos;");
                            break;
                        case 0x13: // left quote
                            value.replace(i, i+1, "&quot;");
                            break;
                        case 0x14: // right quote
                            value.replace(i, i+1, "&quot;");
                            break;
                        case 0x16: // big(?) dash -
                            value.replace(i, i+1, "-");
                            break;
                        case 0x17: // dash -
                            value.replace(i, i+1, "-");
                            break;
                        case 0x19: // tm
                            value.replace(i, i+1, "tm");
                            break;
                        default:
                            if (curChar < 0x20) {
                                // if it is less that 0x20 at this point it is invalid because the only valid values < 0x20 are 0x9, 0xA, 0xD as caught above
                                Debug.logInfo("Removing invalid character [" + curChar + "] numeric value [" + (int) curChar + "] for field " + name + " of entity with PK: " + this.getPrimaryKey().toString(), module);
                                value.deleteCharAt(i);
                            } else if (curChar > 0x7F) {
                                // Replace each char which is out of the ASCII range with a XML entity
                                String replacement = "&#" + (int) curChar + ";";
                                if (Debug.verboseOn()) {
                                    Debug.logVerbose("Entity: " + this.getEntityName() + ", PK: " + this.getPrimaryKey().toString() + " -> char [" + curChar + "] replaced with [" + replacement + "]", module);
                                }
                                value.replace(i, i+1, replacement);
                            }
                        }
                    }

                    if (needsCdata) {
                        // use valueStr instead of the escaped value, not needed or wanted in a CDATA block
                        cdataMap.put(name, valueStr);
                    } else {
                        writer.print(' ');
                        writer.print(name);
                        writer.print("=\"");
                        // encode the value...
                        writer.print(value.toString());
                        writer.print("\"");
                    }
                }
            }
        }

        if (cdataMap.size() == 0) {
            writer.println("/>");
        } else {
            writer.println('>');

            for (Map.Entry<String, String> entry: cdataMap.entrySet()) {
                writer.print(indentString);
                writer.print(indentString);
                writer.print('<');
                writer.print(entry.getKey());
                writer.print("><![CDATA[");
                writer.print(entry.getValue());
                writer.print("]]></");
                writer.print(entry.getKey());
                writer.println('>');
            }

            // don't forget to close the entity.
            writer.print(indentString);
            writer.print("</");
            writer.print(this.getEntityName());
            writer.println(">");
        }
    }

    /** Determines the equality of two GenericEntity objects, overrides the default equals
     *@param  obj  The object (GenericEntity) to compare this two
     *@return      boolean stating if the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof GenericEntity) {
            GenericEntity that = (GenericEntity) obj;
            return this.entityName.equals(that.entityName) && this.fields.equals(that.fields);
        }
        return false;
    }

    /** Creates a hashCode for the entity, using the default String hashCode and Map hashCode, overrides the default hashCode
     *@return    Hashcode corresponding to this entity
     */
    @Override
    public int hashCode() {
        // divide both by two (shift to right one bit) to maintain scale and add together
        if (generateHashCode) {
            cachedHashCode = 0;
            if (getEntityName() != null) {
                cachedHashCode += getEntityName().hashCode() >> 1;
            }
            cachedHashCode += fields.hashCode() >> 1;
            generateHashCode = false;
        }
        return cachedHashCode;
    }

    /**
     * Creates a String for the entity, overrides the default toString
     * This method is secure, it will not display encrypted fields
     *
     *@return String corresponding to this entity
     */
    @Override
    public String toString() {
        StringBuilder theString = new StringBuilder();
        theString.append("[GenericEntity:");
        theString.append(getEntityName());
        theString.append(']');

        for (String curKey: new TreeSet<>(fields.keySet())) {
            Object curValue = fields.get(curKey);
            ModelField field = this.getModelEntity().getField(curKey);
            if (field.getEncryptMethod().isEncrypted() && curValue instanceof String) {
                String encryptField = (String) curValue;
                // the encryptField may not actually be UTF8, it could be any
                // random encoding; just treat it as a series of raw bytes.
                // This won't give the same output as the value stored in the
                // database, but should be good enough for printing
                curValue = HashCrypt.cryptBytes(null, null, encryptField.getBytes(StandardCharsets.UTF_8));
            }
            theString.append('[');
            theString.append(curKey);
            theString.append(',');
            theString.append(curValue);
            theString.append('(');
            theString.append(curValue != null ? curValue.getClass().getName() : "");
            theString.append(')');
            theString.append(']');
        }
        return theString.toString();
    }

    /**
     * Creates a String for the entity, overrides the default toString
     * This method is NOT secure, it WILL display encrypted fields
     *
     *@return String corresponding to this entity
     */
    public String toStringInsecure() {
        StringBuilder theString = new StringBuilder();
        theString.append("[GenericEntity:");
        theString.append(getEntityName());
        theString.append(']');

        for (String curKey: new TreeSet<>(fields.keySet())) {
            Object curValue = fields.get(curKey);
            theString.append('[');
            theString.append(curKey);
            theString.append(',');
            theString.append(curValue);
            theString.append('(');
            theString.append(curValue != null ? curValue.getClass().getName() : "");
            theString.append(')');
            theString.append(']');
        }
        return theString.toString();
    }

    protected int compareToFields(GenericEntity that, String name) {
        Comparable<Object> thisVal = UtilGenerics.cast(this.fields.get(name));
        Object thatVal = that.fields.get(name);

        if (thisVal == null) {
            if (thatVal == null) {
                return 0;
            // if thisVal is null, but thatVal is not, return 1 to put this earlier in the list
            }
            return 1;
        }
        // if thatVal is null, put the other earlier in the list
        if (thatVal == null) {
            return  -1;
        }
        return thisVal.compareTo(thatVal);
    }

    /** Compares this GenericEntity to the passed object
     *@param that Object to compare this to
     *@return int representing the result of the comparison (-1,0, or 1)
     */
    @Override
    public int compareTo(GenericEntity that) {
        // if null, it will push to the beginning
        if (that == null) {
            return -1;
        }

        int tempResult = this.entityName.compareTo(that.entityName);

        // if they did not match, we know the order, otherwise compare the primary keys
        if (tempResult != 0) {
            return tempResult;
        }

        // both have same entityName, should be the same so let's compare PKs
        Iterator<ModelField> pkIter = getModelEntity().getPksIterator();
        while (pkIter.hasNext()) {
            ModelField curField = pkIter.next();
            tempResult = compareToFields(that, curField.getName());
            if (tempResult != 0) {
                return tempResult;
            }
        }

        // okay, if we got here it means the primaryKeys are exactly the SAME, so compare the rest of the fields
        Iterator<ModelField> nopkIter = getModelEntity().getNopksIterator();
        while (nopkIter.hasNext()) {
            ModelField curField = nopkIter.next();
            if (!curField.getIsAutoCreatedInternal()) {
                tempResult = compareToFields(that, curField.getName());
                if (tempResult != 0) {
                    return tempResult;
                }
            }
        }

        // if we got here it means the two are exactly the same, so return tempResult, which should be 0
        return tempResult;
    }

    /** Clones this GenericEntity, this is a shallow clone and uses the default shallow HashMap clone
     *  @return Object that is a clone of this GenericEntity
     */
    @Override
    public Object clone() {
        GenericEntity newEntity = new GenericEntity();
        newEntity.init(this);

        newEntity.setDelegator(internalDelegator);
        return newEntity;
    }

    @Override
    public Object remove(Object key) {
        return this.fields.remove(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return this.fields.containsKey(key);
    }

    @Override
    public java.util.Set<Map.Entry<String, Object>> entrySet() {
        return Collections.unmodifiableMap(this.fields).entrySet();
    }

    @Override
    public Object put(String key, Object value) {
        return this.set(key, value, true);
    }

    @Override
    public void putAll(java.util.Map<? extends String, ? extends Object> map) {
        this.setFields(map);
    }

    @Override
    public void clear() {
        this.fields.clear();
    }

    @Override
    public Object get(Object key) {
        return this.get((String) key);
    }

    @Override
    public java.util.Set<String> keySet() {
        return Collections.unmodifiableSet(this.fields.keySet());
    }

    @Override
    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    @Override
    public java.util.Collection<Object> values() {
        return Collections.unmodifiableMap(this.fields).values();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.fields.containsValue(value);
    }

    @Override
    public int size() {
        return this.fields.size();
    }

    public boolean matches(EntityCondition condition) {
        return condition.entityMatches(this);
    }

    public void addObserver(Observer observer) {
        getObservable().addObserver(observer);
    }

    public void clearChanged() {
        getObservable().clearChanged();
    }

    public void deleteObserver(Observer observer) {
        getObservable().deleteObserver(observer);
    }

    public void deleteObservers() {
        getObservable().deleteObservers();
    }

    public boolean hasChanged() {
        return getObservable().hasChanged();
    }

    public void notifyObservers() {
        getObservable().notifyObservers();
    }

    public void notifyObservers(Object arg) {
        getObservable().notifyObservers(arg);
    }

    public void setChanged() {
        getObservable().setChanged();
    }

    public boolean originalDbValuesAvailable() {
        return this.originalDbValues != null ? true : false;
    }

    public Object getOriginalDbValue(String name) {
        if (getModelEntity().getField(name) == null) {
            throw new IllegalArgumentException("[GenericEntity.get] \"" + name + "\" is not a field of " + getEntityName());
        }
        if (originalDbValues == null) {
            return null;
        }
        return originalDbValues.get(name);
    }

    /**
     * Checks to see if all foreign key records exist in the database. Will create a dummy value for
     * those missing when specified.
     *
     * @param insertDummy Create a dummy record using the provided fields
     * @return true if all FKs exist (or when all missing are created)
     * @throws GenericEntityException
     */
    public boolean checkFks(boolean insertDummy) throws GenericEntityException {
        ModelEntity model = this.getModelEntity();
        Iterator<ModelRelation> relItr = model.getRelationsIterator();
        while (relItr.hasNext()) {
            ModelRelation relation = relItr.next();
            if ("one".equalsIgnoreCase(relation.getType())) {
                // see if the related value exists
                Map<String, Object> fields = new HashMap<>();
                for (ModelKeyMap keyMap : relation.getKeyMaps()) {
                    fields.put(keyMap.getRelFieldName(), this.get(keyMap.getFieldName()));
                }
                EntityFieldMap ecl = EntityCondition.makeCondition(fields);
                long count = this.getDelegator().findCountByCondition(relation.getRelEntityName(), ecl, null, null);
                if (count == 0) {
                    if (insertDummy) {
                        // create the new related value (dummy)
                        GenericValue newValue = this.getDelegator().makeValue(relation.getRelEntityName());
                        boolean allFieldsSet = true;
                        for (ModelKeyMap mkm : relation.getKeyMaps()) {
                            if (this.get(mkm.getFieldName()) != null) {
                                newValue.set(mkm.getRelFieldName(), this.get(mkm.getFieldName()));
                                if (Debug.infoOn()) {
                                    Debug.logInfo("Set [" + mkm.getRelFieldName() + "] to - " + this.get(mkm.getFieldName()), module);
                                }
                            } else {
                                allFieldsSet = false;
                            }
                        }
                        if (allFieldsSet) {
                            if (Debug.infoOn()) {
                                Debug.logInfo("Creating place holder value : " + newValue, module);
                            }

                            // inherit create and update times from this value in order to make this not seem like new/fresh data
                            newValue.put(ModelEntity.CREATE_STAMP_FIELD, this.get(ModelEntity.CREATE_STAMP_FIELD));
                            newValue.put(ModelEntity.CREATE_STAMP_TX_FIELD, this.get(ModelEntity.CREATE_STAMP_TX_FIELD));
                            newValue.put(ModelEntity.STAMP_FIELD, this.get(ModelEntity.STAMP_FIELD));
                            newValue.put(ModelEntity.STAMP_TX_FIELD, this.get(ModelEntity.STAMP_TX_FIELD));
                            // set isFromEntitySync so that create/update stamp fields set above will be preserved
                            newValue.setIsFromEntitySync(true);
                            // check the FKs for the newly created entity
                            newValue.checkFks(true);
                            newValue.create();
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static interface NULL {
    }

    public static class NullGenericEntity extends GenericEntity implements NULL {
        protected NullGenericEntity() {
            this.setImmutable();
        }

        @Override
        public String getEntityName() {
            return "[null-entity]";
        }
        @Override
        public String toString() {
            return "[null-entity]";
        }
    }

    public static class NullField implements NULL, Comparable<NullField> {
        protected NullField() { }

        @Override
        public String toString() {
            return "[null-field]";
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int compareTo(NullField other) {
            return equals(other) ? 0 : -1;
        }
    }
}
