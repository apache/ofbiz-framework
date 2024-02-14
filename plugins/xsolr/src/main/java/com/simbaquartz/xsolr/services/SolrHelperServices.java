/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@simbacart.com>,  December, 2016                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xsolr.services;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simbaquartz.SolrConstants;
import com.simbaquartz.xsolr.util.SolrUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SolrHelperServices {
    public static final String module = SolrHelperServices.class.getName();

    private static final String SOLR_DOC_TYPE_DEAL = "deal";
    private static final String SOLR_DOC_TYPE_TASK = "TASK";
    private static final String SOLR_DOC_TYPE_CONTACT = "contacts";
    private static final String SOLR_DOC_TYPE_PROJECT = "PROJECT";
    private static final String SOLR_DEFAULT_RESPONSE_TYPE = "json";
    private static String solrSearchServer = UtilProperties.getPropertyValue("SolrConnector.properties", "solr.instance");


    /**
     * Deletes docs with supplied doc id from the supplied SOLR core name
     *
     * @param dctx
     * @param context expects toBeDeletedIds (document ids, comma separated), solrCoreName in context
     *                Map
     * @return
     */
    public static Map<String, Object> deleteDocsFromSolr(DispatchContext dctx, Map context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String toBeDeletedIdsVal = (String) context.get("toBeDeletedIds");
        String solrCoreName = (String) context.get("solrCoreName");

        String[] idArray = toBeDeletedIdsVal.split(",");
        List<String> toBeDeletedIds = Arrays.asList(idArray);

        String serverUrl = solrSearchServer + "/" + solrCoreName + "/update?commit=true&optimize=true";

        StringBuffer deleteQueryString = new StringBuffer("<delete>");
        for (String toBeDeletedId : toBeDeletedIds) {
            deleteQueryString.append("<id>" + toBeDeletedId + "</id>");
        }
        deleteQueryString.append("</delete>");

        result = deleteDocFromSolr(result, serverUrl, deleteQueryString.toString());

        return result;
    }

    private static Map<String, Object> deleteDocFromSolr(
            Map<String, Object> result, String serverUrl, String deleteQueryString) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(serverUrl);
        CloseableHttpResponse response = null;

        try {
            HttpEntity entity = new StringEntity(deleteQueryString.toString(), "application/json");
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            // Execute the method.
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                String errorMessage =
                        "SOLR delete doc method failed with status : " + response.getStatusLine();
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            String solrResponse = IOUtils.toString(responseEntity.getContent());
            result.put("solrResponse", solrResponse);

            EntityUtils.consume(responseEntity);
        } catch (IOException e) {
            Debug.logError("Error Deleting documents from Solr " + e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                Debug.logError("Error closing response stream" + e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // deletes all docs from the supplied solr name
    public static Map<String, Object> deleteAllDocsInSolr(DispatchContext dctx, Map context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String solrCoreName = SolrUtils.getSolrCoreName(dctx.getDelegator());

        String deleteQuery = (String) context.get("deleteQuery");
        Debug.logInfo(
                "\n\n** WARNING ** Deleting all docs from solr for solr core " + solrCoreName, module);
        HttpSolrClient httpSolrClient = null;
        try {
            SolrUtils.getInstance();
            httpSolrClient = SolrUtils.getHttpSolrClient();
            httpSolrClient.deleteByQuery(deleteQuery);
            httpSolrClient.commit();
        } catch (SolrServerException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            if (httpSolrClient != null) {
                try {
                    httpSolrClient.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return result;
    }

    /**
     * Publishes document to SOLR
     *
     * @param dctx
     * @param context, expects docsToPublish, solrCoreName, isCommitRequired (true),
     *                 isOptimizeRequired (true) in context map
     * @return
     */
    public static Map<String, Object> publishDocsToSolr(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result;
        List<SolrInputDocument> docsToPublish = (List<SolrInputDocument>) context.get("docsToPublish");
        boolean isCommitRequired = (Boolean) context.get("isCommitRequired");
        HttpSolrClient client;

        try {
            SolrUtils.getInstance();
            client = SolrUtils.getHttpSolrClient();
            client.add(docsToPublish);
            if (isCommitRequired) client.commit();

            client.close();
            result = ServiceUtil.returnSuccess("Documents indexed in solr successfully!");
        } catch (Exception e) {
            Debug.logError(
                    e, "Error publishing documents to Solr. Please check logs for more details", module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    @Deprecated
    public static Map<String, Object> publishDocsToSolr_Depr(
            DispatchContext dctx, Map<String, Object> context) {
        List<Map<String, Object>> docsToPublish =
                (List<Map<String, Object>>) context.get("docsToPublish");
        String solrCoreName = (String) context.get("solrCoreName");
        boolean isCommitRequired = (Boolean) context.get("isCommitRequired");
        boolean isOptimizeRequired = (Boolean) context.get("isOptimizeRequired");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String responseType = SOLR_DEFAULT_RESPONSE_TYPE;

        String serverUrl = solrSearchServer + "/" + solrCoreName + "/update?wt=" + responseType;
        if (isCommitRequired) {
            serverUrl = serverUrl + "&commit=true";
        }
        if (isOptimizeRequired) {
            serverUrl = serverUrl + "&optimize=true";
        }
        Gson gson = new GsonBuilder().setDateFormat(SolrConstants.SOLR_DATE_FORMAT).create();
        String jsonText = gson.toJson(docsToPublish);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(serverUrl);
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse response;

        try {
            HttpEntity entity = new StringEntity(jsonText);
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            // Execute the method.
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                String errorMessage =
                        "SOLR publish doc method failed with status : " + response.getStatusLine();
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(errorMessage);
            }

            String solrResponse = IOUtils.toString(responseEntity.getContent());
            result.put("solrResponse", solrResponse);

            EntityUtils.consume(responseEntity);
            response.close();
        } catch (Exception e) {
            Debug.logError(
                    e, "Error publishing documents to Solr. Please check logs for more details", module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * @param dispatcher
     * @param context
     * @param documentList
     * @param solrCoreName
     * @return
     * @throws GenericServiceException
     * @deprecated use SolrCollectionServices.publishSolrDocuments( delegator,
     * UtilMisc.toList(contactSolrCollection)); instead.
     */
    @Deprecated
    public static Map<String, Object> publishDocumentList(
            LocalDispatcher dispatcher,
            Map<String, Object> context,
            List<SolrInputDocument> documentList,
            String solrCoreName)
            throws GenericServiceException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        // Index the products in "documentList"
        if (documentList != null && documentList.size() > 0) {
            // publish to solr for indexing
            Map<String, Object> solrIndexingCtx =
                    UtilMisc.toMap(
                            "userLogin",
                            context.get("userLogin"),
                            "docsToPublish",
                            documentList,
                            "solrCoreName",
                            solrCoreName,
                            "isCommitRequired",
                            true,
                            "isOptimizeRequired",
                            true);
            result = dispatcher.runSync("publishDocsToSolr", solrIndexingCtx);
        }

        return result;
    }

    /**
     * performs Solr search, make it a generic one, useful for Global Search
     */
    public static Map<String, Object> solrGlobalSearch(DispatchContext dctx, Map context)
            throws GenericServiceException, GenericEntityException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String keyword = (String) context.get("keyword");
        String docType = (String) context.get("docType");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Integer startIndex = (Integer) context.get("startIndex");
        Integer viewSize = (Integer) context.get("viewSize");
        List<FacetField> facetFields = null;
        List<FacetField> allFacetFields = null;
        Map<String, FieldStatsInfo> statsFacetFields = null;
        String solrCoreName = SolrUtils.getSolrCoreName(delegator);
        List<Map<String, Object>> resultsList = FastList.newInstance();

        SolrClient httpSolrClient =
                new HttpSolrClient.Builder(solrSearchServer + "/" + solrCoreName).build();
        String searchText = "*:*";
        if (UtilValidate.isNotEmpty(keyword)) {
            if (keyword.contains("-")) keyword = keyword.replaceAll("-", "\\\\-");
            searchText = "_text_global_:*" + keyword + "*";
        }
        SolrQuery searchQuery = new SolrQuery(searchText);
        SolrQuery allSearchQuery = new SolrQuery(searchText);

        if (startIndex == null || startIndex <= 0) {
            startIndex = 0;
        }

        if (viewSize == null || viewSize < 0) {
            viewSize = 10;
        }

        if (UtilValidate.isNotEmpty(docType)) {
            if (UtilValidate.isNotEmpty(docType)
            ) {
                searchQuery.addFilterQuery("docType:" + docType);
            }
        } else {
            // Search for Quotes, Tasks, Parties, Orders, and Opportunities
            searchQuery.addFilterQuery(
                    "docType:( IN "
                            + SOLR_DOC_TYPE_CONTACT
                            + " "
                            + SOLR_DOC_TYPE_TASK
                            + " "
                            + SOLR_DOC_TYPE_DEAL
                            + " "
                            + SOLR_DOC_TYPE_PROJECT
                            + " )");
        }
        // startIndex & viewSize filter
        searchQuery.setStart(startIndex);
        searchQuery.setRows(viewSize);

        allSearchQuery.setStart(startIndex);
        allSearchQuery.setRows(viewSize);

        searchQuery.addField("partyId");
        searchQuery.addField("partyRoles");
        searchQuery.addField("fullName");
        searchQuery.addField("email");

        searchQuery.addField("projectId");
        searchQuery.addField("projectName");
        searchQuery.addField("projectStartDate");
        searchQuery.addField("projectEndDate");
        searchQuery.addField("projectOwner");

        searchQuery.addField("dealId");
        searchQuery.addField("dealName");
        searchQuery.addField("stage");
        searchQuery.addField("estimatedAmount");

        searchQuery.addField("docType");

        // Task fields
        searchQuery.addField("taskId");
        searchQuery.addField("taskName");
        searchQuery.addField("taskDueDate");
        searchQuery.addField("taskStatusId");

        // enable faceting
        searchQuery.setFacet(true);
        searchQuery.addFacetField("docType");
        searchQuery.addFacetField("partyRoles");

        Debug.logInfo("Performing Global Search with query: " + searchQuery, module);

        // Fetching filtered docTypes count records based on the filter query
        long filteredTasksCount = 0;
        long filteredCustomersCount = 0;
        long filteredLeadCount = 0;
        long filteredProjectsCount = 0;
        long filteredDealCount = 0;
        long totalDocumentsFound = 0;
        long filteredContactsCount = 0;

        try {
            QueryResponse queryResponse = httpSolrClient.query(searchQuery);
            SolrDocumentList foundDocuments = queryResponse.getResults();
            totalDocumentsFound = queryResponse.getResults().getNumFound();
            facetFields = queryResponse.getFacetFields();

            for (SolrDocument record : foundDocuments) {
                Map<String, Object> resultEntry = FastMap.newInstance();
                String docTypeResult = (String) record.get("docType");
                resultEntry.put("docType", record.get("docType"));
                if (SOLR_DOC_TYPE_CONTACT.equalsIgnoreCase(docTypeResult)) {
                    if (UtilValidate.isNotEmpty(record.get("partyRoles"))) {
                        List<String> partyRoles = (List) record.get("partyRoles");
                        if (UtilValidate.isNotEmpty(partyRoles)) {
                            if (partyRoles.contains("CUSTOMER")) {
                                resultEntry.put("docType", "CUSTOMER");
                            } else if (partyRoles.contains("LEAD")) {
                                resultEntry.put("docType", "LEAD");
                            }
                            resultEntry.put("partyId", record.get("partyId"));
                            resultEntry.put("email", record.get("email"));
                            resultEntry.put("fullName", record.get("fullName"));
                        }
                    }
                } else if (SOLR_DOC_TYPE_PROJECT.equalsIgnoreCase(docTypeResult)) {
                    resultEntry.put("projectId", record.get("projectId"));
                    resultEntry.put("projectName", record.get("projectName"));
                    if (UtilValidate.isNotEmpty(record.get("projectStartDate")))
                        resultEntry.put(
                                "projectStartDate",
                                UtilDateTime.toTimestamp((Date) record.get("projectStartDate")));
                    if (UtilValidate.isNotEmpty(record.get("projectEndDate")))
                        resultEntry.put(
                                "projectEndDate", UtilDateTime.toTimestamp((Date) record.get("projectEndDate")));
                    resultEntry.put("projectOwner", record.get("projectOwner"));
                } else if (SOLR_DOC_TYPE_DEAL.equalsIgnoreCase(docTypeResult)) {
                    resultEntry.put("dealId", record.get("dealId"));
                    resultEntry.put("dealName", record.get("dealName"));
                    resultEntry.put("stage", record.get("stage"));
                    BigDecimal estimatedAmount = BigDecimal.ZERO;
                    if (UtilValidate.isNotEmpty(record.get("estimatedAmount")))
                        estimatedAmount = new BigDecimal((Float) record.get("estimatedAmount"));
                    resultEntry.put("estimatedAmount", estimatedAmount);
                } else if (SOLR_DOC_TYPE_TASK.equalsIgnoreCase(docTypeResult)) {
                    resultEntry.put("taskId", record.get("taskId"));
                    resultEntry.put("taskName", record.get("taskName"));
                    if (UtilValidate.isNotEmpty(record.get("taskDueDate")))
                        resultEntry.put(
                                "taskDueDate", UtilDateTime.toTimestamp((Date) record.get("taskDueDate")));
                    resultEntry.put("taskStatus", record.get("taskStatusId"));
                }

                resultsList.add(resultEntry);
            }

            for (FacetField facetField : facetFields) {
                String facetFieldName = facetField.getName();
                List<FacetField.Count> values = facetField.getValues();
                for (FacetField.Count value : values) {
                    String facetedFieldValue = value.getName();
                    long facetedFieldCount = value.getCount();
                    if (UtilValidate.isNotEmpty(facetFieldName)
                            && "partyRoles".equals(facetFieldName)
                            && UtilValidate.isNotEmpty(facetedFieldValue)
                            && "CUSTOMER".equals(facetedFieldValue)) {
                        filteredCustomersCount = facetedFieldCount;
                    } else if (UtilValidate.isNotEmpty(facetFieldName)
                            && "partyRoles".equals(facetFieldName)
                            && UtilValidate.isNotEmpty(facetedFieldValue)
                            && "LEAD".equals(facetedFieldValue)) {
                        filteredLeadCount = facetedFieldCount;
                    } else if (UtilValidate.isNotEmpty(facetedFieldValue)
                            && "PROJECT".equals(facetedFieldValue)) {
                        filteredProjectsCount = facetedFieldCount;
                    } else if (UtilValidate.isNotEmpty(facetedFieldValue)
                            && "TASK".equals(facetedFieldValue)) {
                        filteredTasksCount = facetedFieldCount;
                    } else if (UtilValidate.isNotEmpty(facetedFieldValue)
                            && "deal".equals(facetedFieldValue)) {
                        filteredDealCount = facetedFieldCount;
                    } else if (UtilValidate.isNotEmpty(facetedFieldValue)
                            && "contacts".equals(facetedFieldValue)) {
                        filteredContactsCount = facetedFieldCount;
                    }
                }
            }

        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
        result.put("searchResult", resultsList);
        result.put("totalDocumentsFound", totalDocumentsFound);
        result.put("filteredLeadCount", filteredLeadCount);
        result.put("filteredCustomersCount", filteredCustomersCount);
        result.put("filteredDealCount", filteredDealCount);
        result.put("filteredProjectsCount", filteredProjectsCount);
        result.put("filteredTasksCount", filteredTasksCount);
        result.put("filteredContactsCount", filteredContactsCount);
        return result;
    }
}
