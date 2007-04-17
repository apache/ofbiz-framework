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
package org.ofbiz.entity;

import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Observable;
import java.util.ResourceBundle;
import java.util.TreeSet;

import javolution.lang.Reusable;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Base64;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.LocalizedMap;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelAlias;

import org.ofbiz.entity.util.ByteWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Generic Entity Value Object - Handles persisntence for any defined entity.
 * <p>Note that this class extends <code>Observable</code> to achieve change notification for
 * <code>Observer</code>s. Whenever a field changes the name of the field will be passed to
 * the <code>notifyObservers()</code> method, and through that to the <code>update()</code> method of each
 * <code>Observer</code>.
 *
 */
public class GenericEntity extends Observable implements Map, LocalizedMap, Serializable, Comparable, Cloneable, Reusable {
    public static final String module = GenericEntity.class.getName();
    public static final GenericEntity NULL_ENTITY = new NullGenericEntity();
    public static final NullField NULL_FIELD = new NullField();

    /** Name of the GenericDelegator, used to reget the GenericDelegator when deserialized */
    protected String delegatorName = null;

    /** Reference to an instance of GenericDelegator used to do some basic operations on this entity value. If null various methods in this class will fail. This is automatically set by the GenericDelegator for all GenericValue objects instantiated through it. You may set this manually for objects you instantiate manually, but it is optional. */
    protected transient GenericDelegator internalDelegator = null;

    /** Contains the fields for this entity. Note that this should always be a
     *  HashMap to allow for two things: non-synchronized reads (synchronized
     *  writes are done through synchronized setters) and being able to store
     *  null values. Null values are important because with them we can distinguish
     *  between desiring to set a value to null and desiring to not modify the
     *  current value on an update.
     */
    protected Map fields = FastMap.newInstance();

    /** Contains the entityName of this entity, necessary for efficiency when creating EJBs */
    protected String entityName = null;

    /** Contains the ModelEntity instance that represents the definition of this entity, not to be serialized */
    protected transient ModelEntity modelEntity = null;

    /** Denotes whether or not this entity has been modified, or is known to be out of sync with the persistent record */
    protected boolean modified = false;
    protected boolean generateHashCode = true;
    protected int cachedHashCode = 0;

    /** Used to specify whether or not this representation of the entity can be changed; generally cleared when this object comes from a cache */
    protected boolean mutable = true;

    /** This is an internal field used to specify that a value has come from a sync process and that the auto-stamps should not be over-written */
    protected boolean isFromEntitySync = false;
    
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
    public static GenericEntity createGenericEntity(ModelEntity modelEntity, Map fields) {
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
        }
        
        GenericEntity newEntity = new GenericEntity();
        newEntity.init(modelEntity, fields);
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

    /** Creates new GenericEntity */
    protected void init(ModelEntity modelEntity) {
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannont create a GenericEntity with a null modelEntity parameter");
        }
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        
        // check some things
        if (this.entityName == null) {
            throw new IllegalArgumentException("Cannont create a GenericEntity with a null entityName in the modelEntity parameter");
        }
    }

    /** Creates new GenericEntity from existing Map */
    protected void init(ModelEntity modelEntity, Map fields) {
        if (modelEntity == null) {
            throw new IllegalArgumentException("Cannont create a GenericEntity with a null modelEntity parameter");
        }
        this.modelEntity = modelEntity;
        this.entityName = modelEntity.getEntityName();
        setFields(fields);
        
        // check some things
        if (this.entityName == null) {
            throw new IllegalArgumentException("Cannont create a GenericEntity with a null entityName in the modelEntity parameter");
        }
    }

    /** Copy Constructor: Creates new GenericEntity from existing GenericEntity */
    protected void init(GenericEntity value) {
        if (value.modelEntity == null) {
            throw new IllegalArgumentException("Cannont create a GenericEntity from another GenericEntity with a null modelEntity in the value parameter");
        }
        this.entityName = value.modelEntity.getEntityName();
        this.modelEntity = value.modelEntity;
        if (value.fields != null) this.fields.putAll(value.fields);
        this.delegatorName = value.delegatorName;
        this.internalDelegator = value.internalDelegator;
        
        // check some things
        if (this.entityName == null) {
            throw new IllegalArgumentException("Cannont create a GenericEntity with a null entityName in the modelEntity parameter");
        }
    }

    public void reset() {
        // from GenericEntity
        this.delegatorName = null;
        this.internalDelegator = null;
        this.fields = FastMap.newInstance();
        this.entityName = null;
        this.modelEntity = null;
        this.modified = false;
        this.generateHashCode = true;
        this.cachedHashCode = 0;
        this.mutable = true;
        this.isFromEntitySync = false;
    }
    
    public void refreshFromValue(GenericEntity newValue) throws GenericEntityException {
        if (newValue == null) {
            throw new GenericEntityException("Could not refresh value, new value not found for: " + this);
        }
        GenericPK thisPK = this.getPrimaryKey();
        GenericPK newPK = newValue.getPrimaryKey();
        if (!thisPK.equals(newPK)) {
            throw new GenericEntityException("Could not refresh value, new value did not have the same primary key; this PK=" + thisPK + ", new value PK=" + newPK);
        }
        this.fields = newValue.fields;
        this.setDelegator(newValue.getDelegator());
        this.generateHashCode = newValue.generateHashCode;
        this.cachedHashCode = newValue.cachedHashCode;
        this.modified = false;
    }

    public boolean isModified() {
        return this.modified;
    }

    public void synchronizedWithDatasource() {
        this.modified = false;
    }

    public void removedFromDatasource() {
        // seems kind of minimal, but should do for now...
        this.modified = true;
    }

    public boolean isMutable() {
        return this.mutable;
    }

    public void setImmutable() {
        this.mutable = false;
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
        this.isFromEntitySync = isFromEntitySync;
    }
    
    public String getEntityName() {
        return entityName;
    }

    public ModelEntity getModelEntity() {
        if (modelEntity == null) {
            if (entityName != null) modelEntity = this.getDelegator().getModelEntity(entityName);
            if (modelEntity == null) {
                throw new IllegalStateException("[GenericEntity.getModelEntity] could not find modelEntity for entityName " + entityName);
            }
        }
        return modelEntity;
    }

    /** Get the GenericDelegator instance that created this value object and that is repsonsible for it.
     *@return GenericDelegator object
     */
    public GenericDelegator getDelegator() {
        if (internalDelegator == null) {
            if (delegatorName == null) delegatorName = "default";
            if (delegatorName != null) internalDelegator = GenericDelegator.getGenericDelegator(delegatorName);
            if (internalDelegator == null) {
                throw new IllegalStateException("[GenericEntity.getDelegator] could not find delegator with name " + delegatorName);
            }
        }
        return internalDelegator;
    }

    /** Set the GenericDelegator instance that created this value object and that is repsonsible for it. */
    public void setDelegator(GenericDelegator internalDelegator) {
        if (internalDelegator == null) return;
        this.delegatorName = internalDelegator.getDelegatorName();
        this.internalDelegator = internalDelegator;
    }

    public Object get(String name) {
        if (getModelEntity().getField(name) == null) {
            throw new IllegalArgumentException("[GenericEntity.get] \"" + name + "\" is not a field of " + entityName);
        }
        return fields.get(name);
    }

    /** Returns true if the entity contains all of the primary key fields, but NO others. */
    public boolean isPrimaryKey() {
        return isPrimaryKey(false);
    }
    public boolean isPrimaryKey(boolean requireValue) {
        TreeSet fieldKeys = new TreeSet(this.fields.keySet());
        Iterator pkIter = getModelEntity().getPksIterator();
        while (pkIter.hasNext()) {
            ModelField curPk = (ModelField) pkIter.next();
            String fieldName = curPk.getName();
            if (requireValue) {
                if (this.fields.get(fieldName) == null) return false;
            } else {
                if (!this.fields.containsKey(fieldName)) return false;
            }
            fieldKeys.remove(fieldName);
        }
        if (!fieldKeys.isEmpty()) return false;
        return true;
    }

    /** Returns true if the entity contains all of the primary key fields. */
    public boolean containsPrimaryKey() {
        return containsPrimaryKey(false);
    }
    public boolean containsPrimaryKey(boolean requireValue) {
        //TreeSet fieldKeys = new TreeSet(fields.keySet());
        Iterator pkIter = getModelEntity().getPksIterator();
        while (pkIter.hasNext()) {
            ModelField curPk = (ModelField) pkIter.next();
            String fieldName = curPk.getName();
            if (requireValue) {
                if (this.fields.get(fieldName) == null) return false;
            } else {
                if (!this.fields.containsKey(fieldName)) return false;
            }
        }
        return true;
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
    public synchronized Object set(String name, Object value, boolean setIfNull) {
        if (!this.mutable) {
            // comment this out to disable the mutable check
            throw new IllegalStateException("This object has been flagged as immutable (unchangeable), probably because it came from an Entity Engine cache. Cannot set a value in an immutable entity object.");
        }

        ModelField modelField = getModelEntity().getField(name);
        if (modelField == null) {
            throw new IllegalArgumentException("[GenericEntity.set] \"" + name + "\" is not a field of " + entityName + ", must be one of: " + getModelEntity().fieldNameString());
        }
        if (value != null || setIfNull) {
            ModelFieldType type = null;
            try {
                type = getDelegator().getEntityFieldType(getModelEntity(), modelField.getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            if (type == null) {
                throw new IllegalArgumentException("Type " + modelField.getType() + " not found");
            }

            if (value instanceof Boolean) {
                // if this is a Boolean check to see if we should convert from an indicator or just leave as is
                try {
                    int fieldType = SqlJdbcUtil.getType(type.getJavaType());
                    if (fieldType != 9) {
                        value = ((Boolean) value).booleanValue() ? "Y" : "N";
                    }
                } catch (GenericNotImplementedException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            } else if (value != null) {
                // make sure the type matches the field Java type
                if (!ObjectType.instanceOf(value, type.getJavaType())) {
                    String errMsg = "In entity field [" + this.getEntityName() + "." + name + "] set the value passed in [" + value.getClass().getName() + "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                    Debug.logWarning(errMsg, module);
                }
            }
            Object old = fields.put(name, value);

            generateHashCode = true;
            modified = true;
            this.setChanged();
            this.notifyObservers(name);
            return old;
        } else {
            return fields.get(name);
        }
    }

    public void dangerousSetNoCheckButFast(ModelField modelField, Object value) {
        if (modelField == null) throw new IllegalArgumentException("Cannot set field with a null modelField");
        generateHashCode = true;
        this.fields.put(modelField.getName(), value);
    }

    public Object dangerousGetNoCheckButFast(ModelField modelField) {
        if (modelField == null) throw new IllegalArgumentException("Cannot get field with a null modelField");
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
        if ("null".equals(value)) {
            // count this as a null too, but only for numbers and stuff, not for Strings
            isNullString = true;
        }
        
        ModelField field = getModelEntity().getField(name);
        if (field == null) set(name, value); // this will get an error in the set() method...

        ModelFieldType type = null;
        try {
            type = getDelegator().getEntityFieldType(getModelEntity(), field.getType());
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (type == null) throw new IllegalArgumentException("Type " + field.getType() + " not found");
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

            case 9:
                set(name, isNullString ? null : Boolean.valueOf(value));
                break;

            case 10: // BigDecimal
                set(name, isNullString ? null : new BigDecimal(value));
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
        this.set(name, new ByteWrapper(bytes));
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

    public String getString(String name) {
        // might be nice to add some ClassCastException handling... and auto conversion? hmmm...
        Object object = get(name);
        if (object == null) return null;
        if (object instanceof java.lang.String) {
            return (String) object;
        } else {
            return object.toString();
        }
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
            return new Double(((BigDecimal) value).doubleValue());
        } else {
            return (Double) get(name);
        }
    }

    public BigDecimal getBigDecimal(String name) {
        // this "hack" is needed for now until the Double/BigDecimal issues are all resolved
        // NOTE: for this to be used properly it should really be used as the java-type in the field type def XML files
        Object value = get(name);
        if (value instanceof Double) {
            return new BigDecimal(((Double) value).doubleValue());
        } else {
            return (BigDecimal) get(name);
        }
    }

    public byte[] getBytes(String name) {
        Object value = get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof ByteWrapper) {
            ByteWrapper wrapper = (ByteWrapper) value;
            return wrapper.getBytes();
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
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
     *    <entity-name>.<field-name>.<pk-field-value-1>.<pk-field-value-2>...<pk-field-value-n>
     *  For example:
     *    ProductType.description.FINISHED_GOOD
     *
     * @param name The name of the field on the entity
     * @param locale The locale to use when finding the ResourceBundle, if null uses the default
     *    locale for the current instance of Java
     * @return If the corresponding resource is found and contains a key as described above, then that
     *    property value is returned; otherwise returns the field value
     */
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
        Object fieldValue = null;
        try {
            fieldValue = get(name);
        } catch (IllegalArgumentException e) {
            if (Debug.verboseOn()) {
                Debug.logVerbose(e, "The field name (or key) [" + name + "] is not valid for entity [" + this.getEntityName() + "], printing IllegalArgumentException instead of throwing it because Map interface specification does not allow throwing that exception.", module);
            } else {
                Debug.logWarning("The field name (or key) [" + name + "] is not valid for entity [" + this.getEntityName() + "], printing IllegalArgumentException instead of throwing it because Map interface specification does not allow throwing that exception.", module);
            }
            fieldValue = null;
        }
        
        // In case of view entity try to retrieve the field heading from the real entity linked to the view
        ModelEntity modelEntityToUse = this.getModelEntity();
        if (modelEntityToUse instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntityToUse;
            Iterator it = modelViewEntity.getAliasesIterator();
            while (it.hasNext()) {
                ModelAlias modelAlias = (ModelAlias) it.next();                
                if (modelAlias.getName().equalsIgnoreCase(name)) {
                    modelEntityToUse = modelViewEntity.getMemberModelEntity(modelAlias.getEntityAlias());
                    name = modelAlias.getField();
                    break;
                }
            }
        }
        if (UtilValidate.isEmpty(resource)) {
            resource = modelEntityToUse.getDefaultResourceName();
            // still empty? return the fieldValue
            if (UtilValidate.isEmpty(resource)) {
                //Debug.logWarning("Tried to getResource value for field named " + name + " but no resource name was passed to the method or specified in the default-resource-name attribute of the entity definition", module);
                return fieldValue;
            }
        }
        ResourceBundle bundle = null;
        try {
            bundle = UtilProperties.getResourceBundle(resource, locale);
        } catch (IllegalArgumentException e) {
            bundle = null;
        }
        if (bundle == null) {
            //Debug.logWarning("Tried to getResource value for field named " + name + " but no resource was found with the name " + resource + " in the locale " + locale, module);
            return fieldValue;
        }

        StringBuffer keyBuffer = new StringBuffer();
        // start with the Entity Name
        keyBuffer.append(modelEntityToUse.getEntityName());
        // next add the Field Name
        keyBuffer.append('.');
        keyBuffer.append(name);
        // finish off by adding the values of all PK fields
        Iterator iter = modelEntityToUse.getPksIterator();
        while (iter != null && iter.hasNext()) {
            ModelField curField = (ModelField) iter.next();
            keyBuffer.append('.');
            keyBuffer.append(this.get(curField.getName()));
        }

        String bundleKey = keyBuffer.toString();

        Object resourceValue = null;
        try {
            resourceValue = bundle.getObject(bundleKey);
        } catch (MissingResourceException e) {
            return fieldValue;
        }
        if (resourceValue == null) {
            return fieldValue;
        } else {
            return resourceValue;
        }
    }

    public GenericPK getPrimaryKey() {
        Collection pkNames = FastList.newInstance();
        Iterator iter = this.getModelEntity().getPksIterator();
        while (iter != null && iter.hasNext()) {
            ModelField curField = (ModelField) iter.next();
            pkNames.add(curField.getName());
        }
        GenericPK newPK = GenericPK.create(getModelEntity(), this.getFields(pkNames));
        newPK.setDelegator(this.getDelegator());
        return newPK;
    }

    /** go through the pks and for each one see if there is an entry in fields to set */
    public void setPKFields(Map fields) {
        setAllFields(fields, true, null, Boolean.TRUE);
    }

    /** go through the pks and for each one see if there is an entry in fields to set */
    public void setPKFields(Map fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, null, Boolean.TRUE);
    }

    /** go through the non-pks and for each one see if there is an entry in fields to set */
    public void setNonPKFields(Map fields) {
        setAllFields(fields, true, null, Boolean.FALSE);
    }

    /** go through the non-pks and for each one see if there is an entry in fields to set */
    public void setNonPKFields(Map fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, null, Boolean.FALSE);
    }
    
    
    /** Intelligently sets fields on this entity from the Map of fields passed in
     * @param fields The fields Map to get the values from
     * @param setIfEmpty Used to specify whether empty/null values in the field Map should over-write non-empty values in this entity
     * @param namePrefix If not null or empty will be pre-pended to each field name (upper-casing the first letter of the field name first), and that will be used as the fields Map lookup name instead of the field-name
     * @param pks If null, get all values, if TRUE just get PKs, if FALSE just get non-PKs
     */
    public void setAllFields(Map fields, boolean setIfEmpty, String namePrefix, Boolean pks) {
        if (fields == null) {
            return;
        }
        Iterator iter = null;
        if (pks != null) {
            if (pks.booleanValue()) {
                iter = this.getModelEntity().getPksIterator();
            } else {
                iter = this.getModelEntity().getNopksIterator();
            }
        } else {
            iter = this.getModelEntity().getFieldsIterator();
        }
            
        while (iter != null && iter.hasNext()) {
            ModelField curField = (ModelField) iter.next();
            String fieldName = curField.getName();
            String sourceFieldName = null;
            if (UtilValidate.isNotEmpty(namePrefix)) {
                sourceFieldName = namePrefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            } else {
                sourceFieldName = curField.getName();
            }
            
            if (fields.containsKey(sourceFieldName)) {
                Object field = fields.get(sourceFieldName);

                // if (Debug.verboseOn()) Debug.logVerbose("Setting field " + curField.getName() + ": " + field + ", setIfEmpty = " + setIfEmpty, module);
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
                                this.set(curField.getName(), field);
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
    public Collection getAllKeys() {
        return fields.keySet();
    }

    /** Returns key/value pairs of entity fields
     * @return java.util.Map
     */
    public Map getAllFields() {
        Map newMap = FastMap.newInstance();
        newMap.putAll(this.fields);
        return newMap;
    }

    /** Used by clients to specify exactly the fields they are interested in
     * @param keysofFields the name of the fields the client is interested in
     * @return java.util.Map
     */
    public Map getFields(Collection keysofFields) {
        if (keysofFields == null) return null;
        Iterator keys = keysofFields.iterator();
        Object aKey = null;
        Map aMap = FastMap.newInstance();

        while (keys.hasNext()) {
            aKey = keys.next();
            aMap.put(aKey, this.fields.get(aKey));
        }
        return aMap;
    }

    /** Used by clients to update particular fields in the entity
     * @param keyValuePairs java.util.Map
     */
    public synchronized void setFields(Map keyValuePairs) {
        if (keyValuePairs == null) return;
        Iterator entries = keyValuePairs.entrySet().iterator();
        Map.Entry anEntry = null;

        // this could be implement with Map.putAll, but we'll leave it like this for the extra features it has
        while (entries.hasNext()) {
            anEntry = (Map.Entry) entries.next();
            this.set((String) anEntry.getKey(), anEntry.getValue(), true);
        }
    }

    public boolean matchesFields(Map keyValuePairs) {
        if (fields == null) return true;
        if (keyValuePairs == null || keyValuePairs.size() == 0) return true;
        Iterator entries = keyValuePairs.entrySet().iterator();

        while (entries.hasNext()) {
            Map.Entry anEntry = (Map.Entry) entries.next();

            if (!UtilValidate.areEqual(anEntry.getValue(), this.fields.get(anEntry.getKey()))) {
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
    public static Document makeXmlDocument(Collection values) {
        Document document = UtilXml.makeEmptyXmlDocument("entity-engine-xml");

        if (document == null) return null;

        addToXmlDocument(values, document);
        return document;
    }

    public static int addToXmlDocument(Collection values, Document document) {
        return addToXmlElement(values, document, document.getDocumentElement());
    }

    public static int addToXmlElement(Collection values, Document document, Element element) {
        if (values == null) return 0;
        if (document == null) return 0;

        Iterator iter = values.iterator();
        int numberAdded = 0;

        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();
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

        if (prefix == null) prefix = "";
        if (document != null) element = document.createElement(prefix + this.getEntityName());
        // else element = new ElementImpl(null, this.getEntityName());
        if (element == null) return null;

        Iterator modelFields = this.getModelEntity().getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = (ModelField) modelFields.next();
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
        final int indent = 4;

        if (prefix == null) prefix = "";

        for (int i = 0; i < indent; i++) writer.print(' ');
        writer.print('<');
        writer.print(prefix);
        writer.print(this.getEntityName());

        // write attributes immediately and if a CDATA element is needed, put those in a Map for now
        Map cdataMap = FastMap.newInstance();

        Iterator modelFields = this.getModelEntity().getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = (ModelField) modelFields.next();
            String name = modelField.getName();
            
            String type = modelField.getType();
            if (type != null && type.equals("blob")) {
                Object obj = get(name);
                boolean b1 = obj instanceof byte [];
                if (b1) {
                    byte [] binData = (byte [])obj;
                    String strData = new String(Base64.base64Encode(binData));
                    cdataMap.put(name, strData);
                } else {
                    Debug.logWarning("Field:" + name + " is not of type 'byte[]'. obj: " + obj, module);
                }
            } else {
                String valueStr = this.getString(name);
    
                if (valueStr != null) {
                    StringBuffer value = new StringBuffer(valueStr);
                    boolean needsCdata = false;
                    
                    // check each character, if line-feed or carriage-return is found set needsCdata to true; also look for invalid characters
                    for (int i = 0; i < value.length(); i++) {
                        char curChar = value.charAt(i);
                        /* Some common character for these invalid values, have seen these are mostly from MS Word, but may be part of some standard:
                         5 = ...
                         18 = apostrophe
                         19 = left quotation mark
                         20 = right quotation mark
                         22 = –
                         23 = -
                         25 = tm
                         * 
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
                            } else if(curChar > 0x7F) {                                
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

            Iterator cdataIter = cdataMap.entrySet().iterator();

            while (cdataIter.hasNext()) {
                Map.Entry entry = (Map.Entry) cdataIter.next();

                for (int i = 0; i < (indent << 1); i++) writer.print(' ');
                writer.print('<');
                writer.print((String) entry.getKey());
                writer.print("><![CDATA[");
                writer.print((String) entry.getValue());
                writer.print("]]></");
                writer.print((String) entry.getKey());
                writer.println('>');
            }

            // don't forget to close the entity.
            for (int i = 0; i < indent; i++) writer.print(' ');
            writer.print("</");
            writer.print(this.getEntityName());
            writer.println(">");
        }
    }

    /** Determines the equality of two GenericEntity objects, overrides the default equals
     *@param  obj  The object (GenericEntity) to compare this two
     *@return      boolean stating if the two objects are equal
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;

        // from here, use the compareTo method since it is more efficient:
        try {
            return this.compareTo(obj) == 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /** Creates a hashCode for the entity, using the default String hashCode and Map hashCode, overrides the default hashCode
     *@return    Hashcode corresponding to this entity
     */
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

    /** Creates a String for the entity, overrides the default toString
     *@return    String corresponding to this entity
     */
    public String toString() {
        StringBuffer theString = new StringBuffer();

        theString.append("[GenericEntity:");
        theString.append(getEntityName());
        theString.append(']');

        Iterator keyNames = new TreeSet(fields.keySet()).iterator();
        while (keyNames.hasNext()) {
            String curKey = (String) keyNames.next();
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

    /** Compares this GenericEntity to the passed object
     *@param obj Object to compare this to
     *@return int representing the result of the comparison (-1,0, or 1)
     */
    public int compareTo(Object obj) {
        // if null, it will push to the beginning
        if (obj == null) return -1;

        // rather than doing an if instanceof, just cast it and let it throw an exception if
        // it fails, this will be faster for the expected case (that it IS a GenericEntity)
        // if not a GenericEntity throw ClassCastException, as the spec says
        GenericEntity that = (GenericEntity) obj;

        int tempResult = this.entityName.compareTo(that.entityName);

        // if they did not match, we know the order, otherwise compare the primary keys
        if (tempResult != 0) return tempResult;

        // both have same entityName, should be the same so let's compare PKs
        Iterator pkIter = getModelEntity().getPksIterator();
        while (pkIter.hasNext()) {
            ModelField curField = (ModelField) pkIter.next();
            Comparable thisVal = (Comparable) this.fields.get(curField.getName());
            Comparable thatVal = (Comparable) that.fields.get(curField.getName());

            if (thisVal == null) {
                if (thatVal == null)
                    tempResult = 0;
                // if thisVal is null, but thatVal is not, return 1 to put this earlier in the list
                else
                    tempResult = 1;
            } else {
                // if thatVal is null, put the other earlier in the list
                if (thatVal == null)
                    tempResult = -1;
                else
                    tempResult = thisVal.compareTo(thatVal);
            }
            if (tempResult != 0) return tempResult;
        }

        // okay, if we got here it means the primaryKeys are exactly the SAME, so compare the rest of the fields
        Iterator nopkIter = getModelEntity().getNopksIterator();
        while (nopkIter.hasNext()) {
            ModelField curField = (ModelField) nopkIter.next();
            if (!curField.getIsAutoCreatedInternal()) {
                Comparable thisVal = (Comparable) this.fields.get(curField.getName());
                Comparable thatVal = (Comparable) that.fields.get(curField.getName());

                if (thisVal == null) {
                    if (thatVal == null) {
                        tempResult = 0;
                    // if thisVal is null, but thatVal is not, return 1 to put this earlier in the list
                    } else {
                        tempResult = 1;
                    }
                } else {
                    // if thatVal is null, put the other earlier in the list
                    if (thatVal == null) {
                        tempResult = -1;
                    } else {
                        tempResult = thisVal.compareTo(thatVal);
                    }
                }
                if (tempResult != 0) return tempResult;
            }
        }

        // if we got here it means the two are exactly the same, so return tempResult, which should be 0
        return tempResult;
    }

    /** Clones this GenericEntity, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericEntity
     */
    public Object clone() {
        GenericEntity newEntity = new GenericEntity();
        newEntity.init(this);

        newEntity.setDelegator(internalDelegator);
        return newEntity;
    }

    // ---- Methods added to implement the Map interface: ----

    public Object remove(Object key) {
        return this.fields.remove(key);
    }

    public boolean containsKey(Object key) {
        return this.fields.containsKey(key);
    }

    public java.util.Set entrySet() {
        return Collections.unmodifiableSet(this.fields.entrySet());
    }

    public Object put(Object key, Object value) {
        return this.set((String) key, value, true);
    }

    public void putAll(java.util.Map map) {
        this.setFields(map);
    }

    public void clear() {
        this.fields.clear();
    }

    public Object get(Object key) {
        try {
            return this.get((String) key);
        } catch (IllegalArgumentException e) {
            if (Debug.verboseOn()) {
                Debug.logVerbose(e, "The field name (or key) [" + key + "] is not valid for entity [" + this.getEntityName() + "], printing IllegalArgumentException instead of throwing it because Map interface specification does not allow throwing that exception.", module);
            } else {
                Debug.logWarning("The field name (or key) [" + key + "] is not valid for entity [" + this.getEntityName() + "], printing IllegalArgumentException instead of throwing it because Map interface specification does not allow throwing that exception.", module);
            }
            return null;
        }
    }

    public java.util.Set keySet() {
        return Collections.unmodifiableSet(this.fields.keySet());
    }

    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    public java.util.Collection values() {
        return Collections.unmodifiableCollection(this.fields.values());
    }

    public boolean containsValue(Object value) {
        return this.fields.containsValue(value);
    }

    public int size() {
        return this.fields.size();
    }

    public boolean matches(EntityCondition condition) {
        return condition.entityMatches(this);
    }

    public static interface NULL {
    }

    public static class NullGenericEntity extends GenericEntity implements NULL {
        protected NullGenericEntity() { }
        
        public String toString() {
            return "[null-entity]";
        }
    }
    
    public static class NullField implements NULL {
        protected NullField() { }
    
        public String toString() {
            return "[null-field]";
        }
    }
}
