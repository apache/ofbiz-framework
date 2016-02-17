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
package org.ofbiz.solr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.ofbiz.base.component.ComponentException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;

/**
 * Solr utility class.
 */
public final class SolrUtil {
    
    public static final String module = SolrUtil.class.getName();
    private static String[] solrProdAttribute = { "productId", "internalName", "manu", "size", "smallImage", "mediumImage", "largeImage", "listPrice", "defaultPrice", "inStock", "isVirtual" };

    public static final String solrConfigName = "solrconfig.properties";
    public static final String solrUrl = makeSolrWebappUrl();
    
    protected static final String socketTimeoutString = UtilProperties.getPropertyValue(solrConfigName, "solr.client.socket.timeout");
    
    protected static final String connectionTimeoutString = UtilProperties.getPropertyValue(solrConfigName, "solr.client.connection.timeout");
    
    protected static final String clientUsername = UtilProperties.getPropertyValue(solrConfigName, "solr.client.username");
    
    protected static final String clientPassword = UtilProperties.getPropertyValue(solrConfigName, "solr.client.password");

    protected static final Integer socketTimeout = getSocketTimeout();
    
    protected static final Integer connectionTimeout = getConnectionTimeout();
    
    protected static final String trustSelfSignedCertString = UtilProperties.getPropertyValue(solrConfigName, "solr.client.trust.selfsigned.cert", "false");
    
    protected static final boolean trustSelfSignedCert = getTrustSelfSignedCert();
    
    protected SolrUtil() {
        // empty constructor
    }

    public static String makeSolrWebappUrl() {
        final String solrWebappProtocol = UtilProperties.getPropertyValue(solrConfigName, "solr.webapp.protocol");
        final String solrWebappDomainName = UtilProperties.getPropertyValue(solrConfigName, "solr.webapp.domainName");
        final String solrWebappPath = UtilProperties.getPropertyValue(solrConfigName, "solr.webapp.path");
        final String solrWebappPortOverride = UtilProperties.getPropertyValue(solrConfigName, "solr.webapp.portOverride");
        
        String solrPort;
        if (UtilValidate.isNotEmpty(solrWebappPortOverride)) {
            solrPort = solrWebappPortOverride;
        } else {
            solrPort = UtilProperties.getPropertyValue("url", ("https".equals(solrWebappProtocol) ? "port.https" : "port.http"));
        }
        
        return solrWebappProtocol + "://" + solrWebappDomainName + ":" + solrPort + solrWebappPath;
    }
    
    private static Integer getSocketTimeout() {
        if (UtilValidate.isNotEmpty(socketTimeoutString)) {
            try {
                return Integer.parseInt(socketTimeoutString);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static Integer getConnectionTimeout() {
        if (UtilValidate.isNotEmpty(connectionTimeoutString)) {
            try {
                return Integer.parseInt(connectionTimeoutString);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static boolean getTrustSelfSignedCert() {
        if ("true".equals(trustSelfSignedCertString)) {
            return true;
        }
        return false;
    }

    public static boolean isSolrEcaEnabled() {
        Boolean ecaEnabled = null;
        String sysProp = System.getProperty("ofbiz.solr.eca.enabled");
        if (UtilValidate.isNotEmpty(sysProp)) {
            if ("true".equalsIgnoreCase(sysProp))  {
                ecaEnabled = Boolean.TRUE;
            }
            else if ("false".equalsIgnoreCase(sysProp)) {
                ecaEnabled = Boolean.FALSE;
            }
        }
        if (ecaEnabled == null) {
            ecaEnabled = UtilProperties.getPropertyAsBoolean(SolrUtil.solrConfigName, "solr.eca.enabled", false);
        }
        return Boolean.TRUE.equals(ecaEnabled);
    }
    
    public static WebappInfo getSolrWebappInfo() {
        WebappInfo solrApp = null;
        try {
            ComponentConfig cc = ComponentConfig.getComponentConfig("solr");
            for(WebappInfo currApp : cc.getWebappInfos()) {
                if ("solr".equals(currApp.getName())) {
                    solrApp = currApp;
                    break;
                }
            }
        }
        catch(ComponentException e) {
            throw new IllegalStateException(e);
        }
        return solrApp;
    }
    
    public static boolean isEcaTreatConnectErrorNonFatal() {
        Boolean treatConnectErrorNonFatal = UtilProperties.getPropertyAsBoolean(solrConfigName, "solr.eca.treatConnectErrorNonFatal", true);
        return Boolean.TRUE.equals(treatConnectErrorNonFatal);
    }
    
    
    public static SolrInputDocument generateSolrDocument(Map<String, Object> context) throws GenericEntityException {
        SolrInputDocument doc1 = new SolrInputDocument();

        // add defined attributes
        for (int i = 0; i < solrProdAttribute.length; i++) {
            if (context.get(solrProdAttribute[i]) != null) {
                doc1.addField(solrProdAttribute[i], context.get(solrProdAttribute[i]).toString());
            }
        }

        // add catalog
        if (context.get("catalog") != null) {
            List<String> catalog = UtilGenerics.<String>checkList(context.get("catalog"));
            for (String c : catalog) {
                doc1.addField("catalog", c);
            }
        }

        // add categories
        if (context.get("category") != null) {
            List<String> category = UtilGenerics.<String>checkList(context.get("category"));
            Iterator<String> catIter = category.iterator();
            while (catIter.hasNext()) {
                String cat = (String) catIter.next();
                doc1.addField("cat", cat);
            }
        }

        // add features
        if (context.get("features") != null) {
            Set<String> features = UtilGenerics.<String>checkSet(context.get("features"));
            Iterator<String> featIter = features.iterator();
            while (featIter.hasNext()) {
                String feat = featIter.next();
                doc1.addField("features", feat);
            }
        }

        // add attributes
        if (context.get("attributes") != null) {
            List<String> attributes = UtilGenerics.<String>checkList(context.get("attributes"));
            Iterator<String> attrIter = attributes.iterator();
            while (attrIter.hasNext()) {
                String attr = attrIter.next();
                doc1.addField("attributes", attr);
            }
        }

        // add title
        if (context.get("title") != null) {
            Map<String, String> title = UtilGenerics.<String, String>checkMap(context.get("title"));
            for (Map.Entry<String, String> entry : title.entrySet()) {
                doc1.addField("title_i18n_" + entry.getKey(), entry.getValue());
            }
        }

        // add short_description
        if (context.get("description") != null) {
            Map<String, String> description = UtilGenerics.<String, String>checkMap(context.get("description"));
            for (Map.Entry<String, String> entry : description.entrySet()) {
                doc1.addField("description_i18n_" + entry.getKey(), entry.getValue());
            }
        }

        // add short_description
        if (context.get("longDescription") != null) {
            Map<String, String> longDescription = UtilGenerics.<String, String>checkMap(context.get("longDescription"));
            for (Map.Entry<String, String> entry : longDescription.entrySet()) {
                doc1.addField("longdescription_i18n_" + entry.getKey(), entry.getValue());
            }
        }

        return doc1;
    }
    
    public static Map<String, Object> categoriesAvailable(String catalogId, String categoryId, String productId, boolean displayproducts, int viewIndex, int viewSize, String solrIndexName) {
        return categoriesAvailable(catalogId, categoryId, productId, null, displayproducts, viewIndex, viewSize, solrIndexName);
    }

    public static Map<String, Object> categoriesAvailable(String catalogId, String categoryId, String productId, String facetPrefix, boolean displayproducts, int viewIndex, int viewSize, String solrIndexName) {
        // create the data model
        Map<String, Object> result = new HashMap<String, Object>();
        HttpSolrClient client = null;
        QueryResponse returnMap = new QueryResponse();
        try {
            // do the basic query
            client = getInstance().getHttpSolrClient(solrIndexName);
            // create Query Object
            String query = "inStock[1 TO *]";
            if (categoryId != null)
                query += " +cat:"+ categoryId;
            else if (productId != null)
                query += " +productId:" + productId;
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);

            if (catalogId != null)
                solrQuery.setFilterQueries("catalog:" + catalogId);
            if (displayproducts) {
                if (viewSize > -1) {
                    solrQuery.setRows(viewSize);
                } else
                    solrQuery.setRows(50000);
                if (viewIndex > -1) {
                    solrQuery.setStart(viewIndex);
                }
            } else {
                solrQuery.setFields("cat");
                solrQuery.setRows(0);
            }
            
            if(UtilValidate.isNotEmpty(facetPrefix)){
                solrQuery.setFacetPrefix(facetPrefix);
            }
            
            solrQuery.setFacetMinCount(0);
            solrQuery.setFacet(true);
            solrQuery.addFacetField("cat");
            solrQuery.setFacetLimit(-1);
            Debug.logVerbose("solr: solrQuery: " + solrQuery, module);
            returnMap = client.query(solrQuery, METHOD.POST);
            result.put("rows", returnMap);
            result.put("numFound", returnMap.getResults().getNumFound());
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
        }
        return result;
    }

    private CloseableHttpClient getAllowAllHttpClient() {
        try {
            // Trust own CA and all self-signed certs
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(FileUtil.getFile("component://base/config/ofbizssl.jks"), "changeit".toCharArray(),
                            new TrustSelfSignedStrategy())
                    .build();
            // No host name verifier
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
            return httpClient;
        } catch (Exception e) {
            return HttpClients.createDefault();
        }
    }

    public static SolrUtil getInstance() {
        return new SolrUtil();
    }

    public HttpSolrClient getHttpSolrClient(String solrIndexName) throws ClientProtocolException, IOException {
        HttpClientContext httpContext = HttpClientContext.create();
        
        CloseableHttpClient httpClient = null;
        if (trustSelfSignedCert) {
            httpClient = getAllowAllHttpClient();
        } else {
            httpClient = HttpClients.createDefault();
        }
        
        RequestConfig requestConfig = null;
        if (UtilValidate.isNotEmpty(socketTimeout) && UtilValidate.isNotEmpty(connectionTimeout)) {
            requestConfig = RequestConfig.custom()
                  .setSocketTimeout(socketTimeout)
                  .setConnectTimeout(connectionTimeout)
                  .setRedirectsEnabled(true)
                  .build();
        } else if (UtilValidate.isNotEmpty(socketTimeout)) {
            requestConfig = RequestConfig.custom()
                    .setSocketTimeout(socketTimeout)
                    .setRedirectsEnabled(true)
                    .build();
        } else if (UtilValidate.isNotEmpty(connectionTimeout)) {
            requestConfig = RequestConfig.custom()
                    .setConnectTimeout(connectionTimeout)
                    .setRedirectsEnabled(true)
                    .build();
        } else {
            requestConfig = RequestConfig.custom()
                    .setRedirectsEnabled(true)
                    .build();
        }

        HttpGet httpLogin = new HttpGet(solrUrl + "/control/login?USERNAME=" + clientUsername + "&PASSWORD=" + clientPassword);
        httpLogin.setConfig(requestConfig);
        CloseableHttpResponse loginResponse = httpClient.execute(httpLogin, httpContext);
        loginResponse.close();
        return new HttpSolrClient(solrUrl + "/" + solrIndexName, httpClient);
    }

}
