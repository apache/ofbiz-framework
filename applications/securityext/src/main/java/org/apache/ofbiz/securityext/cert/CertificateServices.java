/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.securityext.cert;

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.apache.ofbiz.base.util.KeyStoreUtil;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * CertificateServices
 */
public class CertificateServices {

    public static final String module = CertificateServices.class.getName();

    public static Map<String, Object> importIssuerCertificate(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String certString = (String) context.get("certString");
        String componentName = (String) context.get("componentName");
        String keystoreName = (String) context.get("keystoreName");
        String alias = (String) context.get("alias");
        String importIssuer = (String) context.get("importIssuer");

        // load the keystore
        KeyStore ks;
        try {
            ks = KeyStoreUtil.getComponentKeyStore(componentName, keystoreName);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // read the certificate
        X509Certificate cert;
        try {
            cert =  (X509Certificate) KeyStoreUtil.pemToCert(certString);
        } catch (CertificateException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
             return ServiceUtil.returnError(e.getMessage());
        }

        // store the cert
        try {
            ks.setCertificateEntry(alias, cert);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // save the keystore
        try {
            KeyStoreUtil.storeComponentKeyStore(componentName, keystoreName, ks);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // set the issuer provision
        Map<String, String> x500Map = KeyStoreUtil.getCertX500Map(cert);
        if (importIssuer != null && "Y".equalsIgnoreCase(importIssuer)) {
            GenericValue provision = delegator.makeValue("X509IssuerProvision");
            provision.set("commonName", x500Map.get("CN"));
            provision.set("organizationalUnit", x500Map.get("OU"));
            provision.set("organizationName", x500Map.get("O"));
            provision.set("cityLocality", x500Map.get("L"));
            provision.set("stateProvince", x500Map.get("ST"));
            provision.set("country", x500Map.get("C"));
            provision.set("serialNumber", cert.getSerialNumber().toString(16));

            try {
                delegator.createSetNextSeqId(provision);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
