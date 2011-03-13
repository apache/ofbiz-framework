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
 * SecurityServerHttpBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.ofbiz.crowd.security;

public class SecurityServerHttpBindingStub extends org.apache.axis.client.Stub implements org.ofbiz.crowd.security.SecurityServerPortType {
    private java.util.Vector<Object> cachedSerClasses = new java.util.Vector<Object>();
    private java.util.Vector<Object> cachedSerQNames = new java.util.Vector<Object>();
    private java.util.Vector<Object> cachedSerFactories = new java.util.Vector<Object>();
    private java.util.Vector<Object> cachedDeserFactories = new java.util.Vector<Object>();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[42];
        _initOperationDesc1();
        _initOperationDesc2();
        _initOperationDesc3();
        _initOperationDesc4();
        _initOperationDesc5();
    }

    private static void _initOperationDesc1() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllGroupRelationships");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPNestableGroup"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPNestableGroup[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPNestableGroup"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("addGroup");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup"), com.atlassian.crowd.integration.soap.SOAPGroup.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPGroup.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidGroupException"),
                      "com.atlassian.crowd.integration.exception.InvalidGroupException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidGroupException"), 
                      true
                    ));
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("addPrincipalToRole");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findPrincipalByToken");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPPrincipal.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updatePrincipalCredential");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PasswordCredential"), com.atlassian.crowd.integration.authentication.PasswordCredential.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidCredentialException"),
                      "com.atlassian.crowd.integration.exception.InvalidCredentialException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidCredentialException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getGrantedAuthorities");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("addPrincipal");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal"), com.atlassian.crowd.integration.soap.SOAPPrincipal.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PasswordCredential"), com.atlassian.crowd.integration.authentication.PasswordCredential.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPPrincipal.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidCredentialException"),
                      "com.atlassian.crowd.integration.exception.InvalidCredentialException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidCredentialException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidPrincipalException"),
                      "com.atlassian.crowd.integration.exception.InvalidPrincipalException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidPrincipalException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("addAttributeToPrincipal");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute"), com.atlassian.crowd.integration.soap.SOAPAttribute.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("invalidatePrincipalToken");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllGroupNames");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[9] = oper;

    }

    private static void _initOperationDesc2() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findRoleMemberships");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removePrincipal");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isValidPrincipalToken");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ArrayOfValidationFactor"), com.atlassian.crowd.integration.authentication.ValidationFactor[].class, false, false);
        param.setItemQName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor"));
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationAccessDeniedException"),
                      "com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationAccessDeniedException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("authenticatePrincipalSimple");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthenticationException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthenticationException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationAccessDeniedException"),
                      "com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationAccessDeniedException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InactiveAccountException"),
                      "com.atlassian.crowd.integration.exception.InactiveAccountException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InactiveAccountException"), 
                      true
                    ));
        _operations[13] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removeRole");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[14] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getCookieInfo");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPCookieInfo"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPCookieInfo.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[15] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updatePrincipalAttribute");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute"), com.atlassian.crowd.integration.soap.SOAPAttribute.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[16] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("searchGroups");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSearchRestriction"), com.atlassian.crowd.integration.soap.SearchRestriction[].class, false, false);
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SearchRestriction"));
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPGroup"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPGroup[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[17] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getCacheTime");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        oper.setReturnClass(long.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[18] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isRoleMember");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[19] = oper;

    }

    private static void _initOperationDesc3() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateGroup");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in3"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[20] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllRoleNames");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[21] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findRoleByName");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPRole.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[22] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isCacheEnabled");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[23] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findGroupByName");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPGroup.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[24] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removePrincipalFromRole");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[25] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("authenticatePrincipal");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PrincipalAuthenticationContext"), com.atlassian.crowd.integration.authentication.PrincipalAuthenticationContext.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthenticationException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthenticationException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationAccessDeniedException"),
                      "com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationAccessDeniedException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InactiveAccountException"),
                      "com.atlassian.crowd.integration.exception.InactiveAccountException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InactiveAccountException"), 
                      true
                    ));
        _operations[26] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findGroupMemberships");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[27] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("addPrincipalToGroup");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[28] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removeGroup");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[29] = oper;

    }

    private static void _initOperationDesc4() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removeAttributeFromPrincipal");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[30] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllPrincipalNames");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString"));
        oper.setReturnClass(java.lang.String[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("urn:SecurityServer", "string"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[31] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("addRole");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole"), com.atlassian.crowd.integration.soap.SOAPRole.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPRole.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidRoleException"),
                      "com.atlassian.crowd.integration.exception.InvalidRoleException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidRoleException"), 
                      true
                    ));
        _operations[32] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("createPrincipalToken");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ArrayOfValidationFactor"), com.atlassian.crowd.integration.authentication.ValidationFactor[].class, false, false);
        param.setItemQName(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor"));
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthenticationException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthenticationException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationAccessDeniedException"),
                      "com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationAccessDeniedException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InactiveAccountException"),
                      "com.atlassian.crowd.integration.exception.InactiveAccountException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InactiveAccountException"), 
                      true
                    ));
        _operations[33] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("searchRoles");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSearchRestriction"), com.atlassian.crowd.integration.soap.SearchRestriction[].class, false, false);
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SearchRestriction"));
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPRole"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPRole[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[34] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removePrincipalFromGroup");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[35] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findPrincipalByName");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPPrincipal.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[36] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("resetPrincipalCredential");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidCredentialException"),
                      "com.atlassian.crowd.integration.exception.InvalidCredentialException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidCredentialException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ApplicationPermissionException"),
                      "com.atlassian.crowd.integration.exception.ApplicationPermissionException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "ObjectNotFoundException"),
                      "com.atlassian.crowd.integration.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException"), 
                      true
                    ));
        _operations[37] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("isGroupMember");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in2"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[38] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("searchPrincipals");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in1"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSearchRestriction"), com.atlassian.crowd.integration.soap.SearchRestriction[].class, false, false);
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SearchRestriction"));
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPPrincipal"));
        oper.setReturnClass(com.atlassian.crowd.integration.soap.SOAPPrincipal[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        param = oper.getReturnParamDesc();
        param.setItemQName(new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[39] = oper;

    }

    private static void _initOperationDesc5() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDomain");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"), com.atlassian.crowd.integration.authentication.AuthenticatedToken.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[40] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("authenticateApplication");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("urn:SecurityServer", "in0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ApplicationAuthenticationContext"), com.atlassian.crowd.integration.authentication.ApplicationAuthenticationContext.class, false, false);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken"));
        oper.setReturnClass(com.atlassian.crowd.integration.authentication.AuthenticatedToken.class);
        oper.setReturnQName(new javax.xml.namespace.QName("urn:SecurityServer", "out"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthorizationTokenException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "InvalidAuthenticationException"),
                      "com.atlassian.crowd.integration.exception.InvalidAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthenticationException"), 
                      true
                    ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("urn:SecurityServer", "RemoteException"),
                      "java.rmi.RemoteException",
                      new javax.xml.namespace.QName("http://rmi.java", "RemoteException"), 
                      true
                    ));
        _operations[41] = oper;

    }

    public SecurityServerHttpBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public SecurityServerHttpBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public SecurityServerHttpBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class<?> cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class<?> beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class<?> beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            /*
            java.lang.Class<?> enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class<?> enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class<?> arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class<?> arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class<?> simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class<?> simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class<?> simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class<?> simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            */
            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ApplicationAuthenticationContext");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.authentication.ApplicationAuthenticationContext.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ArrayOfValidationFactor");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.authentication.ValidationFactor[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor");
            qName2 = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "AuthenticatedToken");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.authentication.AuthenticatedToken.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PasswordCredential");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.authentication.PasswordCredential.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "PrincipalAuthenticationContext");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.authentication.PrincipalAuthenticationContext.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://authentication.integration.crowd.atlassian.com", "ValidationFactor");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.authentication.ValidationFactor.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationAccessDeniedException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ApplicationPermissionException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.ApplicationPermissionException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InactiveAccountException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InactiveAccountException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthenticationException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidAuthenticationException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidAuthorizationTokenException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidCredentialException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidCredentialException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidGroupException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidGroupException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidPrincipalException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidPrincipalException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidRoleException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidRoleException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "InvalidTokenException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.InvalidTokenException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.integration.crowd.atlassian.com", "ObjectNotFoundException");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.exception.ObjectNotFoundException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://lang.java", "Throwable");
            cachedSerQNames.add(qName);
            cls = java.lang.Throwable.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://rmi.java", "RemoteException");
            cachedSerQNames.add(qName);
            cls = java.rmi.RemoteException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSearchRestriction");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SearchRestriction[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SearchRestriction");
            qName2 = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SearchRestriction");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPAttribute");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPAttribute[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute");
            qName2 = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPGroup");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPGroup[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup");
            qName2 = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPNestableGroup");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPNestableGroup[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPNestableGroup");
            qName2 = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPNestableGroup");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPPrincipal");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPPrincipal[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal");
            qName2 = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "ArrayOfSOAPRole");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPRole[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole");
            qName2 = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SearchRestriction");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SearchRestriction.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPAttribute");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPAttribute.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPCookieInfo");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPCookieInfo.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPGroup");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPGroup.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPNestableGroup");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPNestableGroup.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPPrincipal");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPPrincipal.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.integration.crowd.atlassian.com", "SOAPRole");
            cachedSerQNames.add(qName);
            cls = com.atlassian.crowd.integration.soap.SOAPRole.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("urn:SecurityServer", "ArrayOfString");
            cachedSerQNames.add(qName);
            cls = java.lang.String[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string");
            qName2 = new javax.xml.namespace.QName("urn:SecurityServer", "string");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration<Object> keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setEncodingStyle(null);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class<?> cls = (java.lang.Class<?>) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class<?>) {
                            java.lang.Class<?> sf = (java.lang.Class<?>)
                                 cachedSerFactories.get(i);
                            java.lang.Class<?> df = (java.lang.Class<?>)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public com.atlassian.crowd.integration.soap.SOAPNestableGroup[] findAllGroupRelationships(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findAllGroupRelationships"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPNestableGroup[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPNestableGroup[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPNestableGroup[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPGroup addGroup(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.soap.SOAPGroup in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.InvalidGroupException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "addGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPGroup) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPGroup) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPGroup.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidGroupException) {
              throw (com.atlassian.crowd.integration.exception.InvalidGroupException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void addPrincipalToRole(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "addPrincipalToRole"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPPrincipal findPrincipalByToken(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findPrincipalByToken"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPPrincipal.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updatePrincipalCredential(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, com.atlassian.crowd.integration.authentication.PasswordCredential in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidCredentialException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "updatePrincipalCredential"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidCredentialException) {
              throw (com.atlassian.crowd.integration.exception.InvalidCredentialException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String[] getGrantedAuthorities(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "getGrantedAuthorities"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String[]) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPPrincipal addPrincipal(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.soap.SOAPPrincipal in1, com.atlassian.crowd.integration.authentication.PasswordCredential in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidCredentialException, com.atlassian.crowd.integration.exception.InvalidPrincipalException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "addPrincipal"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPPrincipal.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidCredentialException) {
              throw (com.atlassian.crowd.integration.exception.InvalidCredentialException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidPrincipalException) {
              throw (com.atlassian.crowd.integration.exception.InvalidPrincipalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void addAttributeToPrincipal(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, com.atlassian.crowd.integration.soap.SOAPAttribute in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "addAttributeToPrincipal"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void invalidatePrincipalToken(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "invalidatePrincipalToken"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String[] findAllGroupNames(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findAllGroupNames"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String[]) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String[] findRoleMemberships(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findRoleMemberships"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String[]) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removePrincipal(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "removePrincipal"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public boolean isValidPrincipalToken(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, com.atlassian.crowd.integration.authentication.ValidationFactor[] in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "isValidPrincipalToken"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String authenticatePrincipalSimple(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidAuthenticationException, com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InactiveAccountException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "authenticatePrincipalSimple"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthenticationException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthenticationException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InactiveAccountException) {
              throw (com.atlassian.crowd.integration.exception.InactiveAccountException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removeRole(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "removeRole"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPCookieInfo getCookieInfo(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[15]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "getCookieInfo"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPCookieInfo) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPCookieInfo) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPCookieInfo.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updatePrincipalAttribute(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, com.atlassian.crowd.integration.soap.SOAPAttribute in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[16]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "updatePrincipalAttribute"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPGroup[] searchGroups(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.soap.SearchRestriction[] in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[17]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "searchGroups"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPGroup[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPGroup[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPGroup[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public long getCacheTime(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[18]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "getCacheTime"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Long) _resp).longValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Long) org.apache.axis.utils.JavaUtils.convert(_resp, long.class)).longValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public boolean isRoleMember(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[19]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "isRoleMember"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateGroup(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2, boolean in3) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[20]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "updateGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2, new java.lang.Boolean(in3)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String[] findAllRoleNames(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[21]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findAllRoleNames"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String[]) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPRole findRoleByName(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[22]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findRoleByName"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPRole) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPRole) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPRole.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public boolean isCacheEnabled(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[23]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "isCacheEnabled"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPGroup findGroupByName(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[24]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findGroupByName"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPGroup) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPGroup) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPGroup.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removePrincipalFromRole(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[25]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "removePrincipalFromRole"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String authenticatePrincipal(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.authentication.PrincipalAuthenticationContext in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidAuthenticationException, com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InactiveAccountException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[26]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "authenticatePrincipal"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthenticationException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthenticationException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InactiveAccountException) {
              throw (com.atlassian.crowd.integration.exception.InactiveAccountException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String[] findGroupMemberships(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[27]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findGroupMemberships"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String[]) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void addPrincipalToGroup(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[28]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "addPrincipalToGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removeGroup(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[29]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "removeGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removeAttributeFromPrincipal(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[30]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "removeAttributeFromPrincipal"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String[] findAllPrincipalNames(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[31]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findAllPrincipalNames"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String[]) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPRole addRole(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.soap.SOAPRole in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.InvalidRoleException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[32]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "addRole"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPRole) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPRole) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPRole.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidRoleException) {
              throw (com.atlassian.crowd.integration.exception.InvalidRoleException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String createPrincipalToken(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, com.atlassian.crowd.integration.authentication.ValidationFactor[] in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidAuthenticationException, com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InactiveAccountException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[33]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "createPrincipalToken"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthenticationException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthenticationException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationAccessDeniedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InactiveAccountException) {
              throw (com.atlassian.crowd.integration.exception.InactiveAccountException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPRole[] searchRoles(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.soap.SearchRestriction[] in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[34]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "searchRoles"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPRole[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPRole[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPRole[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removePrincipalFromGroup(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[35]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "removePrincipalFromGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPPrincipal findPrincipalByName(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[36]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "findPrincipalByName"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPPrincipal.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void resetPrincipalCredential(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidCredentialException, java.rmi.RemoteException, com.atlassian.crowd.integration.exception.ApplicationPermissionException, com.atlassian.crowd.integration.exception.ObjectNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[37]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "resetPrincipalCredential"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidCredentialException) {
              throw (com.atlassian.crowd.integration.exception.InvalidCredentialException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ApplicationPermissionException) {
              throw (com.atlassian.crowd.integration.exception.ApplicationPermissionException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.ObjectNotFoundException) {
              throw (com.atlassian.crowd.integration.exception.ObjectNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public boolean isGroupMember(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[38]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "isGroupMember"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1, in2});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.soap.SOAPPrincipal[] searchPrincipals(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0, com.atlassian.crowd.integration.soap.SearchRestriction[] in1) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[39]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "searchPrincipals"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0, in1});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.soap.SOAPPrincipal[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.soap.SOAPPrincipal[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String getDomain(com.atlassian.crowd.integration.authentication.AuthenticatedToken in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[40]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "getDomain"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.atlassian.crowd.integration.authentication.AuthenticatedToken authenticateApplication(com.atlassian.crowd.integration.authentication.ApplicationAuthenticationContext in0) throws java.rmi.RemoteException, com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException, com.atlassian.crowd.integration.exception.InvalidAuthenticationException, java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[41]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("urn:SecurityServer", "authenticateApplication"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {in0});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.atlassian.crowd.integration.authentication.AuthenticatedToken) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.atlassian.crowd.integration.authentication.AuthenticatedToken) org.apache.axis.utils.JavaUtils.convert(_resp, com.atlassian.crowd.integration.authentication.AuthenticatedToken.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthorizationTokenException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.atlassian.crowd.integration.exception.InvalidAuthenticationException) {
              throw (com.atlassian.crowd.integration.exception.InvalidAuthenticationException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

}
