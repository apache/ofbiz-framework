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
package org.ofbiz.common;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * OFBIZ CDyne Services: for info see http://www.cdyne.com/developers/overview.aspx
 */
public class CdyneServices {

    public final static String module = CdyneServices.class.getName();
    
    public final static String licenseKey = UtilProperties.getPropertyValue("cdyne", "LicenseKey", "0");

    /**
     * CDyne ReturnCityState Service
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map cdyneReturnCityState(DispatchContext dctx, Map context) {
        String zipcode = (String) context.get("zipcode");
        
        String serviceUrl = "http://ws.cdyne.com/psaddress/addresslookup.asmx/ReturnCityState?zipcode=" + zipcode + "&LicenseKey=" + licenseKey; 
        try {
            String httpResponse = HttpClient.getUrlContent(serviceUrl);
            
            Document addressDocument = UtilXml.readXmlDocument(httpResponse);
            Element addressRootElement = addressDocument.getDocumentElement();

            Map response = ServiceUtil.returnSuccess();
            populateCdyneAddress(addressRootElement, response);
            
            if ("true".equals(response.get("ServiceError"))) {
                return ServiceUtil.returnError("Got ServiceError=true from CDyne ReturnCityState service; zipcode=" + zipcode);
            }
            if ("true".equals(response.get("AddressError"))) {
                return ServiceUtil.returnError("Got AddressError=true from CDyne ReturnCityState service; zipcode=" + zipcode);
            }
            
            return response;
        } catch (HttpClientException e) {
            String errMsg = "Error calling CDyne service at URL [" + serviceUrl + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (SAXException e) {
            String errMsg = "Error parsing XML result from CDyne service at URL [" + serviceUrl + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (ParserConfigurationException e) {
            String errMsg = "Error parsing XML result from CDyne service at URL [" + serviceUrl + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (IOException e) {
            String errMsg = "Error parsing XML result from CDyne service at URL [" + serviceUrl + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }
    
    public static void populateCdyneAddress(Element addressRootElement, Map targetContext) {
        targetContext.put("ServiceError", UtilXml.childElementValue(addressRootElement, "ServiceError"));
        targetContext.put("AddressError", UtilXml.childElementValue(addressRootElement, "AddressError"));
        targetContext.put("AddressFoundBeMoreSpecific", UtilXml.childElementValue(addressRootElement, "AddressFoundBeMoreSpecific"));
        targetContext.put("NeededCorrection", UtilXml.childElementValue(addressRootElement, "NeededCorrection"));

        targetContext.put("DeliveryAddress", UtilXml.childElementValue(addressRootElement, "DeliveryAddress"));
        targetContext.put("City", UtilXml.childElementValue(addressRootElement, "City"));
        targetContext.put("StateAbbrev", UtilXml.childElementValue(addressRootElement, "StateAbbrev"));
        targetContext.put("ZipCode", UtilXml.childElementValue(addressRootElement, "ZipCode"));
        targetContext.put("County", UtilXml.childElementValue(addressRootElement, "County"));
        targetContext.put("CountyNum", UtilXml.childElementValue(addressRootElement, "CountyNum"));
        targetContext.put("PreferredCityName", UtilXml.childElementValue(addressRootElement, "PreferredCityName"));

        targetContext.put("DeliveryPoint", UtilXml.childElementValue(addressRootElement, "DeliveryPoint"));
        targetContext.put("CheckDigit", UtilXml.childElementValue(addressRootElement, "CheckDigit"));

        targetContext.put("CSKey", UtilXml.childElementValue(addressRootElement, "CSKey"));
        targetContext.put("FIPS", UtilXml.childElementValue(addressRootElement, "FIPS"));

        targetContext.put("FromLongitude", UtilXml.childElementValue(addressRootElement, "FromLongitude"));
        targetContext.put("FromLatitude", UtilXml.childElementValue(addressRootElement, "FromLatitude"));
        targetContext.put("ToLongitude", UtilXml.childElementValue(addressRootElement, "ToLongitude"));
        targetContext.put("ToLatitude", UtilXml.childElementValue(addressRootElement, "ToLatitude"));
        targetContext.put("AvgLongitude", UtilXml.childElementValue(addressRootElement, "AvgLongitude"));
        targetContext.put("AvgLatitude", UtilXml.childElementValue(addressRootElement, "AvgLatitude"));

        targetContext.put("CMSA", UtilXml.childElementValue(addressRootElement, "CMSA"));
        targetContext.put("PMSA", UtilXml.childElementValue(addressRootElement, "PMSA"));
        targetContext.put("MSA", UtilXml.childElementValue(addressRootElement, "MSA"));
        targetContext.put("MA", UtilXml.childElementValue(addressRootElement, "MA"));

        targetContext.put("TimeZone", UtilXml.childElementValue(addressRootElement, "TimeZone"));
        targetContext.put("hasDaylightSavings", UtilXml.childElementValue(addressRootElement, "hasDaylightSavings"));
        targetContext.put("AreaCode", UtilXml.childElementValue(addressRootElement, "AreaCode"));
        targetContext.put("LLCertainty", UtilXml.childElementValue(addressRootElement, "LLCertainty"));

        targetContext.put("CensusBlockNum", UtilXml.childElementValue(addressRootElement, "CensusBlockNum"));
        targetContext.put("CensusTractNum", UtilXml.childElementValue(addressRootElement, "CensusTractNum"));
        
        /*
        Example URL: http://ws.cdyne.com/psaddress/addresslookup.asmx/ReturnCityState?zipcode=93940&LicenseKey=0
        NOTE: 0 is a test LicenseKey
         
        Example Response: 
        <?xml version="1.0" encoding="utf-8"?>
        <Address xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://ws.cdyne.com/">
            <ServiceError>false</ServiceError>
            <AddressError>false</AddressError>
            <AddressFoundBeMoreSpecific>false</AddressFoundBeMoreSpecific>
            <NeededCorrection>true</NeededCorrection>
            
            <DeliveryAddress>**UNKNOWN**</DeliveryAddress>
            <City>DEL REY OAKS</City>
            <StateAbbrev>CA</StateAbbrev>
            <ZipCode>93940</ZipCode>
            <County>MONTEREY</County>
            <CountyNum>0</CountyNum>
            <PreferredCityName>MONTEREY</PreferredCityName>
            
            <DeliveryPoint>99</DeliveryPoint>
            <CheckDigit>0</CheckDigit>
            
            <CSKey>Z20854</CSKey>
            <FIPS>06053</FIPS>
            
            <FromLongitude>-121.919965</FromLongitude>
            <FromLatitude>36.362864</FromLatitude>
            <ToLongitude>-121.647022</ToLongitude>
            <ToLatitude>36.652645</ToLatitude>
            <AvgLongitude>-121.7834935</AvgLongitude>
            <AvgLatitude>36.5077545</AvgLatitude>
            
            <CMSA>7120</CMSA>
            <PMSA />
            <MSA>7120</MSA>
            <MA>712</MA>
            
            <TimeZone>PST</TimeZone>
            <hasDaylightSavings>true</hasDaylightSavings>
            <AreaCode>831</AreaCode>
            <LLCertainty>90</LLCertainty>
            
            <CensusBlockNum>9003</CensusBlockNum>
            <CensusTractNum>0134.00</CensusTractNum>
        </Address>
        */
    }
}
