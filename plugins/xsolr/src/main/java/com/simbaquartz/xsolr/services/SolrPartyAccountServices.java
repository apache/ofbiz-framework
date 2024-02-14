package com.simbaquartz.xsolr.services;

import com.simbaquartz.xsolr.builder.SolrEntityDocumentBuilder;
import com.simbaquartz.xsolr.operations.IndexOperation;
import com.simbaquartz.xsolr.util.SolrUtils;
import com.fidelissd.zcp.xcommon.collections.FastList;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Map;
import org.apache.solr.common.SolrInputDocument;

public class SolrPartyAccountServices {
    private static final String module = SolrPartyAccountServices.class.getName();

    /**
     * Indexes the party account data in Solr
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> indexPartyAccountInSolr(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String partyId = (String) context.get("partyId");
        Debug.logInfo("Starting to index party account with " + partyId + " in Solr.", module);
        try {
            List<SolrInputDocument> documentList = FastList.newInstance();

            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            Debug.logInfo("CoreName" + solrCoreName, module);
            if (UtilValidate.isEmpty(solrCoreName)) {
                return ServiceUtil.returnError("A valid SOLR core name is not available, please add a valid core name in .properties to proceed.");
            }

            if (UtilValidate.isNotEmpty(context.get("isDataPassed")) && ((Boolean) context.get("isDataPassed"))) {
                Debug.logInfo("Starting to index party account with direct data passing in Solr.", module);
                context.put("operation", IndexOperation.SET.getSolrAtomicUpdateModifier());
                documentList.add(SolrEntityDocumentBuilder.preparePartyAccountDocument(context));
                SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
                return result;
            }

            GenericValue partyRelationship = null;

            //Will be adding more roleTypeIds to index
            EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("roleTypeIdTo", "OWNER"),
                    EntityCondition.makeCondition("partyIdTo", partyId)),
                    EntityOperator.AND);
            partyRelationship = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(conditions).orderBy("lastUpdatedStamp").filterByDate().queryFirst();

            GenericValue party = null;

            if (UtilValidate.isNotEmpty(partyRelationship)) {
                String personPartyId = partyRelationship.getString("partyIdTo");
                party = EntityQuery.use(delegator).from("Party").where("partyId", personPartyId).queryOne();
            }
            if (UtilValidate.isNotEmpty(party)) {
                context.put("operation", IndexOperation.SET.getSolrAtomicUpdateModifier());
                documentList.add(SolrEntityDocumentBuilder.preparePartyAccountDocument(UtilMisc.toMap("party", party, "partyRelationship", partyRelationship)));
                SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
            }
        } catch (Exception e) {
            Debug.logError(e,"An error occurred: ", module);
            return ServiceUtil.returnError("An error occurred while trying to index party (" + partyId + "): " + e.getMessage());
        }

        return result;
    }

}
