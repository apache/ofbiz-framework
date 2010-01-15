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
/**
 * SOAPRole.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.crowd.integration.soap;

public class SOAPRole  implements java.io.Serializable {
    private java.lang.Long ID;

    private java.lang.Boolean active;

    private com.atlassian.crowd.integration.soap.SOAPAttribute[] attributes;

    private java.util.Calendar conception;

    private java.lang.String description;

    private java.lang.Long directoryID;

    private java.util.Calendar lastModified;

    private java.lang.String[] members;

    private java.lang.String name;

    public SOAPRole() {
    }

    public SOAPRole(
           java.lang.Long ID,
           java.lang.Boolean active,
           com.atlassian.crowd.integration.soap.SOAPAttribute[] attributes,
           java.util.Calendar conception,
           java.lang.String description,
           java.lang.Long directoryID,
           java.util.Calendar lastModified,
           java.lang.String[] members,
           java.lang.String name) {
           this.ID = ID;
           this.active = active;
           this.attributes = attributes;
           this.conception = conception;
           this.description = description;
           this.directoryID = directoryID;
           this.lastModified = lastModified;
           this.members = members;
           this.name = name;
    }


    /**
     * Gets the ID value for this SOAPRole.
     *
     * @return ID
     */
    public java.lang.Long getID() {
        return ID;
    }


    /**
     * Sets the ID value for this SOAPRole.
     *
     * @param ID
     */
    public void setID(java.lang.Long ID) {
        this.ID = ID;
    }


    /**
     * Gets the active value for this SOAPRole.
     *
     * @return active
     */
    public java.lang.Boolean getActive() {
        return active;
    }


    /**
     * Sets the active value for this SOAPRole.
     *
     * @param active
     */
    public void setActive(java.lang.Boolean active) {
        this.active = active;
    }


    /**
     * Gets the attributes value for this SOAPRole.
     *
     * @return attributes
     */
    public com.atlassian.crowd.integration.soap.SOAPAttribute[] getAttributes() {
        return attributes;
    }


    /**
     * Sets the attributes value for this SOAPRole.
     *
     * @param attributes
     */
    public void setAttributes(com.atlassian.crowd.integration.soap.SOAPAttribute[] attributes) {
        this.attributes = attributes;
    }


    /**
     * Gets the conception value for this SOAPRole.
     *
     * @return conception
     */
    public java.util.Calendar getConception() {
        return conception;
    }


    /**
     * Sets the conception value for this SOAPRole.
     *
     * @param conception
     */
    public void setConception(java.util.Calendar conception) {
        this.conception = conception;
    }


    /**
     * Gets the description value for this SOAPRole.
     *
     * @return description
     */
    public java.lang.String getDescription() {
        return description;
    }


    /**
     * Sets the description value for this SOAPRole.
     *
     * @param description
     */
    public void setDescription(java.lang.String description) {
        this.description = description;
    }


    /**
     * Gets the directoryID value for this SOAPRole.
     *
     * @return directoryID
     */
    public java.lang.Long getDirectoryID() {
        return directoryID;
    }


    /**
     * Sets the directoryID value for this SOAPRole.
     *
     * @param directoryID
     */
    public void setDirectoryID(java.lang.Long directoryID) {
        this.directoryID = directoryID;
    }


    /**
     * Gets the lastModified value for this SOAPRole.
     *
     * @return lastModified
     */
    public java.util.Calendar getLastModified() {
        return lastModified;
    }


    /**
     * Sets the lastModified value for this SOAPRole.
     *
     * @param lastModified
     */
    public void setLastModified(java.util.Calendar lastModified) {
        this.lastModified = lastModified;
    }


    /**
     * Gets the members value for this SOAPRole.
     *
     * @return members
     */
    public java.lang.String[] getMembers() {
        return members;
    }


    /**
     * Sets the members value for this SOAPRole.
     *
     * @param members
     */
    public void setMembers(java.lang.String[] members) {
        this.members = members;
    }


    /**
     * Gets the name value for this SOAPRole.
     *
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this SOAPRole.
     *
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SOAPRole)) return false;
        SOAPRole other = (SOAPRole) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true &&
            ((this.ID==null && other.getID()==null) ||
             (this.ID!=null &&
              this.ID.equals(other.getID()))) &&
            ((this.active==null && other.getActive()==null) ||
             (this.active!=null &&
              this.active.equals(other.getActive()))) &&
            ((this.attributes==null && other.getAttributes()==null) ||
             (this.attributes!=null &&
              java.util.Arrays.equals(this.attributes, other.getAttributes()))) &&
            ((this.conception==null && other.getConception()==null) ||
             (this.conception!=null &&
              this.conception.equals(other.getConception()))) &&
            ((this.description==null && other.getDescription()==null) ||
             (this.description!=null &&
              this.description.equals(other.getDescription()))) &&
            ((this.directoryID==null && other.getDirectoryID()==null) ||
             (this.directoryID!=null &&
              this.directoryID.equals(other.getDirectoryID()))) &&
            ((this.lastModified==null && other.getLastModified()==null) ||
             (this.lastModified!=null &&
              this.lastModified.equals(other.getLastModified()))) &&
            ((this.members==null && other.getMembers()==null) ||
             (this.members!=null &&
              java.util.Arrays.equals(this.members, other.getMembers()))) &&
            ((this.name==null && other.getName()==null) ||
             (this.name!=null &&
              this.name.equals(other.getName())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getID() != null) {
            _hashCode += getID().hashCode();
        }
        if (getActive() != null) {
            _hashCode += getActive().hashCode();
        }
        if (getAttributes() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAttributes());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAttributes(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getConception() != null) {
            _hashCode += getConception().hashCode();
        }
        if (getDescription() != null) {
            _hashCode += getDescription().hashCode();
        }
        if (getDirectoryID() != null) {
            _hashCode += getDirectoryID().hashCode();
        }
        if (getLastModified() != null) {
            _hashCode += getLastModified().hashCode();
        }
        if (getMembers() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getMembers());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getMembers(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SOAPRole.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("active");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "active"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("attributes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "attributes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("conception");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "conception"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("description");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "description"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("directoryID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "directoryID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lastModified");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "lastModified"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("members");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "members"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType,
           java.lang.Class _javaType,
           javax.xml.namespace.QName _xmlType) {
        return
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType,
           java.lang.Class _javaType,
           javax.xml.namespace.QName _xmlType) {
        return
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
