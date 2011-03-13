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
 * PrincipalAuthenticationContext.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.crowd.integration.authentication;

@SuppressWarnings("serial")
public class PrincipalAuthenticationContext  implements java.io.Serializable {
    private java.lang.String application;

    private com.atlassian.crowd.integration.authentication.PasswordCredential credential;

    private java.lang.String name;

    private com.atlassian.crowd.integration.authentication.ValidationFactor[] validationFactors;

    public PrincipalAuthenticationContext() {
    }

    public PrincipalAuthenticationContext(
           java.lang.String application,
           com.atlassian.crowd.integration.authentication.PasswordCredential credential,
           java.lang.String name,
           com.atlassian.crowd.integration.authentication.ValidationFactor[] validationFactors) {
           this.application = application;
           this.credential = credential;
           this.name = name;
           this.validationFactors = validationFactors;
    }


    /**
     * Gets the application value for this PrincipalAuthenticationContext.
     *
     * @return application
     */
    public java.lang.String getApplication() {
        return application;
    }


    /**
     * Sets the application value for this PrincipalAuthenticationContext.
     *
     * @param application
     */
    public void setApplication(java.lang.String application) {
        this.application = application;
    }


    /**
     * Gets the credential value for this PrincipalAuthenticationContext.
     *
     * @return credential
     */
    public com.atlassian.crowd.integration.authentication.PasswordCredential getCredential() {
        return credential;
    }


    /**
     * Sets the credential value for this PrincipalAuthenticationContext.
     *
     * @param credential
     */
    public void setCredential(com.atlassian.crowd.integration.authentication.PasswordCredential credential) {
        this.credential = credential;
    }


    /**
     * Gets the name value for this PrincipalAuthenticationContext.
     *
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this PrincipalAuthenticationContext.
     *
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }


    /**
     * Gets the validationFactors value for this PrincipalAuthenticationContext.
     *
     * @return validationFactors
     */
    public com.atlassian.crowd.integration.authentication.ValidationFactor[] getValidationFactors() {
        return validationFactors;
    }


    /**
     * Sets the validationFactors value for this PrincipalAuthenticationContext.
     *
     * @param validationFactors
     */
    public void setValidationFactors(com.atlassian.crowd.integration.authentication.ValidationFactor[] validationFactors) {
        this.validationFactors = validationFactors;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof PrincipalAuthenticationContext)) return false;
        PrincipalAuthenticationContext other = (PrincipalAuthenticationContext) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true &&
            ((this.application==null && other.getApplication()==null) ||
             (this.application!=null &&
              this.application.equals(other.getApplication()))) &&
            ((this.credential==null && other.getCredential()==null) ||
             (this.credential!=null &&
              this.credential.equals(other.getCredential()))) &&
            ((this.name==null && other.getName()==null) ||
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.validationFactors==null && other.getValidationFactors()==null) ||
             (this.validationFactors!=null &&
              java.util.Arrays.equals(this.validationFactors, other.getValidationFactors())));
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
        if (getApplication() != null) {
            _hashCode += getApplication().hashCode();
        }
        if (getCredential() != null) {
            _hashCode += getCredential().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getValidationFactors() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getValidationFactors());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getValidationFactors(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(PrincipalAuthenticationContext.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PrincipalAuthenticationContext"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("application");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "application"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("credential");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "credential"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PasswordCredential"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("validationFactors");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "validationFactors"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor"));
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
           java.lang.Class<?> _javaType,
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
           java.lang.Class<?> _javaType,
           javax.xml.namespace.QName _xmlType) {
        return
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
