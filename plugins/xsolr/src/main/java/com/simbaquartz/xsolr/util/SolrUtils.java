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

package com.simbaquartz.xsolr.util;

import com.simbaquartz.SolrConstants;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;

public class SolrUtils {
  public static final String module = SolrUtils.class.getName();

  private SolrUtils() {}

  private static final String SOLR_CONFIG_NAME = "SolrConnector.properties";
  private static final String SOLR_URL = makeSolrWebappUrl();

  private static final String SOLR_SERVER_URL =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.instance");
  private static final String SOLR_CORE_NAME =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.core.name");
  private static final String SOLR_TENANT_CORE_NAME =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.core.tenant.name");

  private static final String SOCKET_TIMEOUT_STRING =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.client.socket.timeout");

  private static final String CON_TIMEOUT_STRING =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.client.connection.timeout");

  private static final String CLIENT_USER_NAME =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.client.username");

  private static final String CLIENT_PASSWORD =
      UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.client.password");

  private static final Integer SOCKET_TIMEOUT = getSocketTimeout();

  private static final Integer CON_TIMEOUT = getConnectionTimeout();

  private static final String TRUST_SELF_SIGN_CERT_STRING =
      UtilProperties.getPropertyValue(
          SOLR_CONFIG_NAME, "solr.client.trust.selfsigned.cert", "false");

  private static final boolean TRUST_SELF_SIGNED_CERT = getTrustSelfSignedCert();

  private static Integer getSocketTimeout() {
    if (UtilValidate.isNotEmpty(SOCKET_TIMEOUT_STRING)) {
      try {
        return Integer.parseInt(SOCKET_TIMEOUT_STRING);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private static Integer getConnectionTimeout() {
    if (UtilValidate.isNotEmpty(CON_TIMEOUT_STRING)) {
      try {
        return Integer.parseInt(CON_TIMEOUT_STRING);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private static boolean getTrustSelfSignedCert() {
    return "true".equals(TRUST_SELF_SIGN_CERT_STRING);
  }

  public static String makeSolrWebappUrl() {
    final String solrWebappProtocol =
        UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.webapp.protocol");
    final String solrWebappDomainName =
        UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.webapp.domainName");
    final String solrWebappPath =
        UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.webapp.path");
    final String solrWebappPortOverride =
        UtilProperties.getPropertyValue(SOLR_CONFIG_NAME, "solr.webapp.portOverride");

    String solrPort;
    if (UtilValidate.isNotEmpty(solrWebappPortOverride)) {
      solrPort = solrWebappPortOverride;
    } else {
      solrPort =
          UtilProperties.getPropertyValue(
              "url",
              ("https".equals(solrWebappProtocol) ? "port.https" : "port.http"),
              ("https".equals(solrWebappProtocol) ? "8443" : "8080"));
    }

    return solrWebappProtocol + "://" + solrWebappDomainName + ":" + solrPort + solrWebappPath;
  }

  public static SolrUtils getInstance() {
    return new SolrUtils();
  }

  public static HttpSolrClient getHttpSolrClient() throws IOException {
    HttpClientContext httpContext = HttpClientContext.create();

    CloseableHttpClient httpClient = null;
    if (TRUST_SELF_SIGNED_CERT) {
      httpClient = UtilHttp.getAllowAllHttpClient();
    } else {
      httpClient = HttpClients.createDefault();
    }

    RequestConfig requestConfig = null;
    if (UtilValidate.isNotEmpty(SOCKET_TIMEOUT) && UtilValidate.isNotEmpty(CON_TIMEOUT)) {
      requestConfig =
          RequestConfig.custom()
              .setSocketTimeout(SOCKET_TIMEOUT)
              .setConnectTimeout(CON_TIMEOUT)
              .setRedirectsEnabled(true)
              .build();
    } else if (UtilValidate.isNotEmpty(SOCKET_TIMEOUT)) {
      requestConfig =
          RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setRedirectsEnabled(true).build();
    } else if (UtilValidate.isNotEmpty(CON_TIMEOUT)) {
      requestConfig =
          RequestConfig.custom().setConnectTimeout(CON_TIMEOUT).setRedirectsEnabled(true).build();
    } else {
      requestConfig = RequestConfig.custom().setRedirectsEnabled(true).build();
    }

    return new HttpSolrClient.Builder(SOLR_SERVER_URL + "/" + SOLR_CORE_NAME)
        .withHttpClient(httpClient)
        .build();
  }

  public static String toSolrFormattedDateString(Timestamp toFormat) {

    if (UtilValidate.isEmpty(toFormat)) {
      return "";
    }

    Instant instant = Instant.ofEpochMilli(toFormat.getTime());
    String solrFormattedTimstamp = DateTimeFormatter.ISO_INSTANT.format(instant);

    return solrFormattedTimstamp;
    //		return solrDateFormat.format(toFormat);
  }

  /**
   * Values of "1", "t", or "T" in the first character are interpreted as true. Any other value is
   * interpreted as false
   *
   * @param bool
   * @return
   */
  public static String toSolrFormattedBoolean(boolean bool) {

    if (bool) {
      return "1"; // true
    }

    return "0"; // false
  }

  /**
   * Returns a list of maps parsed from solr indexed multivalued fields.
   *
   * @param record String value of the record, e.g. productId:FRESENIUS-190895^productName:Fresenius
   *     2008T w/o CDX w/bibag
   * @param itemDelimiter Item delimiter to use e.g. "^"
   * @param valueDelimiter Map delimiter to use e.g. ":"
   * @return Map of parsed key value. e.g. {productId=FRES ENIUS-190895, productName=Fresenius 2008T
   *     w/o CDX w/bibag}
   */
  public static Map<String, String> splitMergedRecords(
      String record, String itemDelimiter, String valueDelimiter) {
    Map<String, String> parsedMap = new HashMap<>();

    String[] items = record.split(itemDelimiter);

    for (String item : items) {
      String[] itemKeyValue = item.split(valueDelimiter);

      try {
        parsedMap.put(itemKeyValue[0], itemKeyValue[1]);
      } catch (ArrayIndexOutOfBoundsException aiobe) {
        Debug.logWarning("Unable to parse key value pairs for: " + item, module);
      }
    }

    return parsedMap;
  }

  /**
   * Returns a list of maps parsed from solr indexed multivalued fields.
   *
   * @param record String value of the record, e.g. productId:FRESENIUS-190895^productName:Fresenius
   *     2008T w/o CDX w/bibag
   * @return Map of parsed key value.
   */
  public static Map<String, String> splitMergedRecords(String record) {
    return splitMergedRecords(
        record,
        SolrConstants.SOLR_RECORD_ITEM_DELIMITER,
        SolrConstants.SOLR_RECORD_VALUE_DELIMITER);
  }

  /** Identify the solr-core-name to use based on tenant (from delegator ) */
  public static String getSolrCoreName(Delegator delegator) {
    String tenantId = delegator.getDelegatorTenantId();
    if (UtilValidate.isEmpty(tenantId)) return SOLR_CORE_NAME;
    return SOLR_TENANT_CORE_NAME + tenantId;
  }

  /**
   * Use to partially update (atomic updates) a document in SOLR. Example: <code>
   *   SolrUtils.partiallyUpdateDocumentInSolr(SolrTaskServices.getTaskDocumentId(taskTemplateId), UtilMisc.toMap("taskTemplateLastUsed", now));
   * </code> For timestamp values use <code>
   *   SolrDateTimeUtils.toSolrFormattedDateString(taskTemplateLastUsed)
   * </code>
   *
   * @param documentId
   * @param fieldsAndValuesToIndex
   */
  public static void partiallyUpdateDocumentInSolr(
      String documentId, Map<String, Object> fieldsAndValuesToIndex) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField("id", documentId);

    fieldsAndValuesToIndex
        .entrySet()
        .stream()
        .forEach(
            e ->
                solrInputDocument.addField(
                    e.getKey(), Collections.singletonMap("set", e.getValue())));

    addDocumentToSolr(solrInputDocument);
  }

  /**
   * Use this to add an additional value to the list Example add a new task follower to the list of
   * followers. Pass in keepDistinct=true to avoid duplicates
   *
   * @param documentId
   * @param fieldName
   * @param valuesToAppend
   * @param keepDistinct pass as true to keep the values unique.
   */
  public static void partiallyAppendListFieldInSolr(
      String documentId, String fieldName, List<String> valuesToAppend, boolean keepDistinct) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField("id", documentId);

    valuesToAppend.forEach(
        valueToAppend ->
            solrInputDocument.addField(
                fieldName,
                Collections.singletonMap((keepDistinct) ? "add-distinct" : "add", valuesToAppend)));

    addDocumentToSolr(solrInputDocument);
  }

  /**
   * Use this to remove items from an existing list, example removing task followers.
   *
   * @param documentId
   * @param fieldName
   * @param valuesToRemove
   */
  public static void partiallyRemoveItemFromListFieldInSolr(
      String documentId, String fieldName, List<String> valuesToRemove) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField("id", documentId);

    valuesToRemove.forEach(
        valueToAppend ->
            solrInputDocument.addField(
                fieldName, Collections.singletonMap("remove", valuesToRemove)));

    addDocumentToSolr(solrInputDocument);
  }

  public static void addDocumentToSolr(SolrInputDocument solrInputDocument) {
    HttpSolrClient client;
    try {
      SolrUtils.getInstance();
      client = SolrUtils.getHttpSolrClient();
      client.add(solrInputDocument);
      client.commit();
      client.close();
    } catch (IOException e) {
      Debug.logError(e, module);
    } catch (SolrServerException e) {
      Debug.logError(e, module);
    }
  }

  public static void addMapDocumentsToSolr(List<Map<String, Object>> solrInputDocuments) {
    addDocumentsToSolr(SolrUtils.convertMapToSolrInputDocument(solrInputDocuments));
  }

  public static void addDocumentsToSolr(List<SolrInputDocument> solrInputDocuments) {
    HttpSolrClient client;
    try {
      SolrUtils.getInstance();
      client = SolrUtils.getHttpSolrClient();
      client.add(solrInputDocuments);
      client.commit();
      client.close();
    } catch (IOException e) {
      Debug.logError(e, module);
    } catch (SolrServerException e) {
      Debug.logError(e, module);
    }
  }

  /**
   * Prepares the query string format "records:( IN 'cond1' 'cond2')" for the input list. To search
   * in solr console use following filter query - assignees: ( IN 10945 10963)
   *
   * <p>Example usage <code>
   * String taskAssigneeConstraint = SolrUtils.prepareArraySearchConstraint("assingees", assignees);
   * searchQuery.addFilterQuery(taskAssigneeConstraint);</code>
   *
   * @param solrFieldName name of the indexed field in solr, example assignees
   * @param records list of records to filter by, example a list of assignee party ids.
   * @return
   */
  public static String prepareArraySearchConstraint(String solrFieldName, List<String> records) {
    StringBuffer recordsToFilterBy = new StringBuffer();
    records.forEach(
        record -> {
          recordsToFilterBy.append("\"" + record + "\" ");
        });
    return solrFieldName + ":( IN " + recordsToFilterBy + " )";
  }

  /**
   * Prepares OR query constraint for a bunch of queries. Example: taskCreatedByPartyId:10201 OR
   * assignees:(IN "10201")
   *
   * @param queries
   * @return
   */
  public static String prepareORQueryConstraint(List<String> queries) {
    return StringUtil.join(queries, " OR ");
  }

  /**
   * Use for converting legacy map values to SolrInputDocument.
   *
   * @param documentsToConvert
   * @return
   */
  public static List<SolrInputDocument> convertMapToSolrInputDocument(
      List<Map<String, Object>> documentsToConvert) {

    List<SolrInputDocument> convertedDocuments = new ArrayList<>();
    for (Map<String, Object> mapDocumentToConvert : documentsToConvert) {
      SolrInputDocument solrInputDocument = new SolrInputDocument();
      mapDocumentToConvert.forEach((key, value) -> solrInputDocument.addField(key, value));
      convertedDocuments.add(solrInputDocument);
    }

    return convertedDocuments;
  }

  public static SolrInputDocument convertMapToSolrInputDocument(
      Map<String, Object> documentToConvert) {

    SolrInputDocument solrInputDocument = new SolrInputDocument();

    Iterator<Entry<String, Object>> itr = documentToConvert.entrySet().iterator();

    while (itr.hasNext()) {
      Map.Entry<String, Object> entry = itr.next();
      System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());

      String name = entry.getKey();
      Object value = entry.getValue();
      // XXX: Solr cannot handle BigDecimal
      if (value instanceof BigDecimal) {
        // store big decimals as their string representation
        value = value.toString();
      } else if (value instanceof Calendar) {
        value = ((Calendar) value).getTime();
      }
      solrInputDocument.addField(name, value);
    }

    return solrInputDocument;
  }

  /**
   * Returns the faceted results for the input query, additionally includes a key named
   * "totalNumOfRecords" that returns the total number of records found.
   *
   * <p>Example usage: see {@link
   * SolrContactServices#getContactStatsForAccount(DispatchContext,
   * Map)}
   *
   * @param filters
   * @param facetFieldName
   * @return
   */
  public static Map<String, Long> getFacetedStatsForQuery(
      List<String> filters, String facetFieldName) {
    Map<String, Long> facetedStats = new HashMap<String, Long>();
    long totalRecords;
    HttpSolrClient httpSolrClient = null;
    try {
      SolrUtils.getInstance();
      httpSolrClient = SolrUtils.getHttpSolrClient();

      SolrQuery solrQuery = new SolrQuery("*:*");
      QueryResponse allQueryResponse;

      solrQuery.addFilterQuery(filters.toArray(new String[0]));
      solrQuery.setFacet(true);
      solrQuery.addFacetField(facetFieldName);

      // used for debugging
      Debug.logInfo("Invoking solr search with query: " + solrQuery.toString(), module);

      allQueryResponse = httpSolrClient.query(solrQuery);
      totalRecords = (int) allQueryResponse.getResults().getNumFound();
      facetedStats.put("totalNumOfRecords", totalRecords);
      List<FacetField> allFacetFields = allQueryResponse.getFacetFields();
      for (FacetField facetField : allFacetFields) {
        List<FacetField.Count> values = facetField.getValues();
        for (FacetField.Count value : values) {
          String facetedFieldValue = value.getName();
          long facetedFieldCount = value.getCount();
          if (UtilValidate.isNotEmpty(facetedFieldValue)) {
            facetedStats.put(facetedFieldValue, facetedFieldCount);
          }
        }
      }

    } catch (SolrServerException | IOException e) {
      Debug.logError(e, module);
    } finally {
      if (httpSolrClient != null) {
        try {
          httpSolrClient.close();
        } catch (IOException e) {
          Debug.logError(e, module);
        }
      }
    }

    return facetedStats;
  }
}
