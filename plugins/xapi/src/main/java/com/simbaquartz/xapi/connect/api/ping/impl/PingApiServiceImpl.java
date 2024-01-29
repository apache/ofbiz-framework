package com.simbaquartz.xapi.connect.api.ping.impl;


import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.ping.PingApiService;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

public class PingApiServiceImpl extends PingApiService {

    @Override
    public Response pingServer(SecurityContext securityContext) throws NotFoundException {

        Map<String, Object> pingResp = FastMap.newInstance();
        pingResp.put("response", "OK");
        return ApiResponseUtil.prepareOkResponse(pingResp);
        // return ApiResponseUtil.prepareDefaultResponse(Response.Status.OK);
    }

    @Override
    public Response verifySolr(SecurityContext securityContext) throws NotFoundException {
        Map<String, Object> pingResp = FastMap.newInstance();
        // TODO: to be implemented
        /*HttpSolrClient httpSolrClient = null;
        try {
            httpSolrClient = SolrUtils.getHttpSolrClient();
            pingResp.put("solrBaseUrl", httpSolrClient.getBaseURL());

            List<SolrInputDocument> solrInputDocuments = new ArrayList<>();
            SolrInputDocument solrInputDocument = new SolrInputDocument();
            solrInputDocument.setField("id","ping_test");
            solrInputDocuments.add(solrInputDocument);
            httpSolrClient.add(solrInputDocuments);
            httpSolrClient.commit();

            String searchText = "id:ping_test";
            SolrQuery searchQuery = new SolrQuery(searchText);
            searchQuery.setStart(0);
            searchQuery.setRows(1);

            // used for debugging
            Debug.logInfo("Invoking solr search with query: " + searchQuery, "PingApiServiceImpl");
            QueryResponse queryResponse = httpSolrClient.query(searchQuery);
            SolrDocumentList foundDocuments = queryResponse.getResults();
            pingResp.put("status", "success");
            pingResp.put("recordsFetched", foundDocuments.getNumFound());
            pingResp.put("recordsFromSolr", foundDocuments);
        } catch (IOException e) {
            e.printStackTrace();
            pingResp.put("error","Unable to connect to solr server. Error: " + e.getMessage());
        } catch (SolrServerException e) {
            e.printStackTrace();
            pingResp.put("error","There was a problem executing query on solr. Error: " + e.getMessage());
        } finally {
            if(httpSolrClient!=null) {
                try {
                    httpSolrClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    pingResp.put("error","There was a problem closing solr connection. Error: " + e.getMessage());
                }
            }
        }*/
        return ApiResponseUtil.prepareOkResponse(pingResp);
    }

}
