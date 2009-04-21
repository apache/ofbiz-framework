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
 * SecurityServerLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.ofbiz.crowd.security;

import org.ofbiz.base.util.UtilProperties;

public class SecurityServerLocator extends org.apache.axis.client.Service implements org.ofbiz.crowd.security.SecurityServer {

    public SecurityServerLocator() {
    }


    public SecurityServerLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public SecurityServerLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for SecurityServerHttpPort
    private java.lang.String SecurityServerHttpPort_address = UtilProperties.getPropertyValue("crowd.properties", "crowd.server.address");

    public java.lang.String getSecurityServerHttpPortAddress() {
        return SecurityServerHttpPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String SecurityServerHttpPortWSDDServiceName = "SecurityServerHttpPort";

    public java.lang.String getSecurityServerHttpPortWSDDServiceName() {
        return SecurityServerHttpPortWSDDServiceName;
    }

    public void setSecurityServerHttpPortWSDDServiceName(java.lang.String name) {
        SecurityServerHttpPortWSDDServiceName = name;
    }

    public org.ofbiz.crowd.security.SecurityServerPortType getSecurityServerHttpPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(SecurityServerHttpPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getSecurityServerHttpPort(endpoint);
    }

    public org.ofbiz.crowd.security.SecurityServerPortType getSecurityServerHttpPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.ofbiz.crowd.security.SecurityServerHttpBindingStub _stub = new org.ofbiz.crowd.security.SecurityServerHttpBindingStub(portAddress, this);
            _stub.setPortName(getSecurityServerHttpPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setSecurityServerHttpPortEndpointAddress(java.lang.String address) {
        SecurityServerHttpPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.ofbiz.crowd.security.SecurityServerPortType.class.isAssignableFrom(serviceEndpointInterface)) {
                org.ofbiz.crowd.security.SecurityServerHttpBindingStub _stub = new org.ofbiz.crowd.security.SecurityServerHttpBindingStub(new java.net.URL(SecurityServerHttpPort_address), this);
                _stub.setPortName(getSecurityServerHttpPortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("SecurityServerHttpPort".equals(inputPortName)) {
            return getSecurityServerHttpPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("urn:SecurityServer", "SecurityServer");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("urn:SecurityServer", "SecurityServerHttpPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("SecurityServerHttpPort".equals(portName)) {
            setSecurityServerHttpPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
