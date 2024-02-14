package com.simbaquartz.xsolr.services;

import com.simbaquartz.xsolr.util.SolrUtils;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SolrSearchService {
  public static final String module = SolrSearchService.class.getName();
  private static String solrSearchServer =
      UtilProperties.getPropertyValue("fsdSolrConnector.properties", "fsd.solr.instance");
  private static final String solrCoreName =
      UtilProperties.getPropertyValue("fsdSolrConnector.properties", "fsd.solr.core.name");

  /**
   * Refer to service definition for usage details.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> performSolrSearch(
      DispatchContext dctx, Map<String, ? extends Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();

    List<String> searchFields = (List) context.get("searchFields");
    List<String> filterQueryFields = (List) context.get("filterQueryFields");
    String searchText = (String) context.get("searchText");
    List<String> facetFields = (List<String>) context.get("facetFields");
    Integer startIndex = (Integer) context.get("startIndex");
    Integer viewSize = (Integer) context.get("viewSize");

    List<Map> facetResults = FastList.newInstance();

    int count = 0;
    List<Map> records = FastList.newInstance();

    HttpSolrClient httpSolrClient = null;
    try {
      SolrUtils.getInstance();
      httpSolrClient = SolrUtils.getHttpSolrClient();
      if (UtilValidate.isEmpty(searchText)) {
        searchText = "*:*";
      }

      SolrQuery searchQuery = new SolrQuery(searchText);

      String sortBy = (String) context.get("sortBy");

      if (UtilValidate.isNotEmpty(sortBy)) {
        if (sortBy.charAt(0) == '-') {
          sortBy = sortBy.substring(1, sortBy.length());
          searchQuery.setSort(sortBy, SolrQuery.ORDER.desc);
        } else {
          searchQuery.setSort(sortBy, SolrQuery.ORDER.asc);
        }
      }

      if (UtilValidate.isNotEmpty(startIndex) || UtilValidate.isNotEmpty(viewSize)) {
        searchQuery.setStart(startIndex);
        searchQuery.setRows(viewSize);
      } else {
        searchQuery.setRows(10000);
      }

      // prepare fields
      for (String searchField : searchFields) {
        searchQuery.addField(searchField);
      }

      // prepare filter query fields
      for (String filterQueryField : filterQueryFields) {
        searchQuery.addFilterQuery(filterQueryField);
      }

      // facet search
      boolean isFacetSearchEnabled = false;
      if (UtilValidate.isNotEmpty(facetFields)) {
        searchQuery.setFacet(true);
        isFacetSearchEnabled = true;
        for (String facetField : facetFields) {
          searchQuery.addFacetField(facetField);
        }
      }

      // use for debugging the generated query
      if (Debug.verboseOn())
        Debug.logVerbose("Created solr query : " + searchQuery.toString(), module);
      Debug.logInfo(
          "Invoking performSolrSearch search with query: " + searchQuery.toString(), module);

      QueryResponse queryResponse =
          httpSolrClient.query(
              searchQuery,
              SolrRequest.METHOD
                  .POST); // POST method ensures we don't get "non ok status: 414,
                          // message:Request-URI Too Long" styled messages for long queries OR
                          // conditions
      // records found
      SolrDocumentList foundDocuments = queryResponse.getResults();
      records.addAll(foundDocuments);

      if (isFacetSearchEnabled) {
        List<FacetField> facetFieldsFromSolr = queryResponse.getFacetFields();

        for (FacetField facetField : facetFieldsFromSolr) {
          List<FacetField.Count> values = facetField.getValues();
          for (FacetField.Count value : values) {
            String facetedFieldValue = value.getName();
            long facetedFieldCount = value.getCount();

            // format { facetFieldName: {"value", value, "count", count} }
            Map facetedFieldEntryMap =
                UtilMisc.toMap(
                    facetField.getName(),
                    UtilMisc.toMap("value", facetedFieldValue, "count", facetedFieldCount));
            facetResults.add(facetedFieldEntryMap);
          }
        }

        result.put("facetResults", facetResults);
      }

      // total number of records
      count = (int) foundDocuments.getNumFound();
    } catch (SolrServerException | IOException e) {
      Debug.logError(
          e,
          "An error occurred while trying to perform solr search with input searchFields: "
              + searchFields
              + " - filterQueryFields : "
              + filterQueryFields
              + " - searchText : "
              + searchText,
          module);
    } catch (Exception ex) {
      Debug.logError(
          ex,
          "An error occurred while trying to perform solr search with input searchFields: "
              + searchFields
              + " - filterQueryFields : "
              + filterQueryFields
              + " - searchText : "
              + searchText,
          module);
    } finally {
      if (httpSolrClient != null) {
        try {
          httpSolrClient.close();
        } catch (IOException e) {
          Debug.logError(e, module);
        }
      }
    }

    result.put("totalNumberOfRecords", count);
    result.put("records", records);

    return result;
  }


}
