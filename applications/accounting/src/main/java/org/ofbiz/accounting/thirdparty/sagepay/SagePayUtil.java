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

package org.ofbiz.accounting.thirdparty.sagepay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.ofbiz.base.util.Debug;


public final class SagePayUtil {

    public static final String module = SagePayUtil.class.getName();
    private SagePayUtil() {}

    public static Map<String, Object> buildCardAuthorisationPaymentResponse
    (Boolean authResult, String authCode, String authFlag, BigDecimal processAmount, String authRefNum, String authAltRefNum, String authMessage) {

        Map<String, Object> result = new HashMap<String, Object>();
        if(authResult != null) { result.put("authResult", authResult); }
        if(authCode != null) { result.put("authCode", authCode); }
        if(authFlag != null) { result.put("authFlag", authFlag); }
        if(processAmount != null) { result.put("processAmount", processAmount); }
        if(authRefNum != null) { result.put("authRefNum", authRefNum); }
        if(authAltRefNum != null) { result.put("authAltRefNum", authAltRefNum); }
        if(authMessage != null) { result.put("authMessage", authMessage); }
        return result;
    }

    public static Map<String, Object> buildCardCapturePaymentResponse
    (Boolean captureResult, String captureCode, String captureFlag, BigDecimal captureAmount, String captureRefNum, String captureAltRefNum, String captureMessage) {

        Map<String, Object> result = new HashMap<String, Object>();
        if(captureResult != null) { result.put("captureResult", captureResult); }
        if(captureCode != null) { result.put("captureCode", captureCode); }
        if(captureFlag != null) { result.put("captureFlag", captureFlag); }
        if(captureAmount != null) { result.put("captureAmount", captureAmount); }
        if(captureRefNum != null) { result.put("captureRefNum", captureRefNum); }
        if(captureAltRefNum != null) { result.put("captureAltRefNum", captureAltRefNum); }
        if(captureMessage != null) { result.put("captureMessage", captureMessage); }
        return result;
    }

    public static Map<String, Object> buildCardReleasePaymentResponse
    (Boolean releaseResult, String releaseCode, BigDecimal releaseAmount, String releaseRefNum, String releaseAltRefNum, String releaseMessage) {

        Map<String, Object> result = new HashMap<String, Object>();
        if(releaseResult != null) { result.put("releaseResult", releaseResult); }
        if(releaseCode != null) { result.put("releaseCode", releaseCode); }
        if(releaseAmount != null) { result.put("releaseAmount", releaseAmount); }
        if(releaseRefNum != null) { result.put("releaseRefNum", releaseRefNum); }
        if(releaseAltRefNum != null) { result.put("releaseAltRefNum", releaseAltRefNum); }
        if(releaseMessage != null) { result.put("releaseMessage", releaseMessage); }
        return result;
    }

    public static Map<String, Object> buildCardVoidPaymentResponse
    (Boolean refundResult, BigDecimal refundAmount, String refundRefNum, String refundAltRefNum, String refundMessage) {

        Map<String, Object> result = new HashMap<String, Object>();
        if(refundResult != null) { result.put("refundResult", refundResult); }
        if(refundAmount != null) { result.put("refundAmount", refundAmount); }
        if(refundRefNum != null) { result.put("refundRefNum", refundRefNum); }
        if(refundAltRefNum != null) { result.put("refundAltRefNum", refundAltRefNum); }
        if(refundMessage != null) { result.put("refundMessage", refundMessage); }
        return result;
    }

    public static Map<String, Object> buildCardRefundPaymentResponse
    (Boolean refundResult, String refundCode, BigDecimal refundAmount, String refundRefNum, String refundAltRefNum, String refundMessage) {

        Map<String, Object> result = new HashMap<String, Object>();
        if(refundResult != null) { result.put("refundResult", refundResult); }
        if(refundCode != null) { result.put("refundCode", refundCode); }
        if(refundAmount != null) { result.put("refundAmount", refundAmount); }
        if(refundRefNum != null) { result.put("refundRefNum", refundRefNum); }
        if(refundAltRefNum != null) { result.put("refundAltRefNum", refundAltRefNum); }
        if(refundMessage != null) { result.put("refundMessage", refundMessage); }
        return result;
    }

    public static HttpHost getHost(Map<String, String> props) {
        String hostUrl = null;
        if("PRODUCTION".equals(props.get("sagePayMode"))) {
            hostUrl = props.get("productionHost");
        } else if("TEST".equals(props.get("sagePayMode"))) {
            hostUrl = props.get("testingHost");
        }
        String scheme = hostUrl.substring(0, 5);
        String host = hostUrl.substring(8, hostUrl.lastIndexOf(":"));
        String port = hostUrl.substring(hostUrl.lastIndexOf(":")+1);
        return getHost(host, Integer.parseInt(port), scheme);
    }

    public static HttpHost getHost(String hostName, int port, String scheme) {
        HttpHost host = new HttpHost(hostName, port, scheme);
        return host;
    }

    public static Map<String, String> getResponseData(HttpResponse response) throws IOException {

        Map<String, String> responseData = new HashMap<String, String>();
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            InputStream inputStream = httpEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String data = null;
            while( (data = reader.readLine()) != null ) {
                if(data.indexOf("=") != -1) {
                    String name = data.substring(0, data.indexOf("="));
                    String value = data.substring(data.indexOf("=")+1);
                    responseData.put(name, value);
                }
            }
        }
        Debug.logInfo("SagePay Response Data : " + responseData, module);
        return responseData;
    }

    public static HttpPost getHttpPost(String uri, Map<String, String> parameters) throws UnsupportedEncodingException {

        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader("User-Agent", "HTTP Client");
        httpPost.addHeader("Content-type", "application/x-www-form-urlencoded");
        //postMethod.addHeader("Content-Length", "0");

        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        Set<String> keys = parameters.keySet();
        for (String key : keys) {
            String value = parameters.get(key);
            postParameters.add(new BasicNameValuePair(key, value));
        }

        Debug.logInfo("SagePay PostParameters - " + postParameters, module);

        HttpEntity postEntity = new UrlEncodedFormEntity(postParameters);
        httpPost.setEntity(postEntity);
        return httpPost;
    }

    public static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        return httpClient;
    }
}
