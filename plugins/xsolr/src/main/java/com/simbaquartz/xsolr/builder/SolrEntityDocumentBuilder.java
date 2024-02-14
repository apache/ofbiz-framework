package com.simbaquartz.xsolr.builder;

import com.simbaquartz.xparty.helpers.PartyContactHelper;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.solr.common.SolrInputDocument;

import java.util.Map;

public class SolrEntityDocumentBuilder {
    private static final String module = SolrEntityDocumentBuilder.class.getName();
    public static final String SOLR_DOC_TYPE_PARTY_ACCOUNT = "party_account";
    private static final String SOLR_PREFIX_PROJECT = "pjt-";
    private static final String SOLR_DOC_TYPE_PROJECT = "PROJECT";

    /**
     * Party document id generator to uniquely identify party record
     *
     * @param partyId
     * @return
     */
    public static String getPartyDocumentId(String partyId) {
        return "PARTY-" + partyId;
    }

    /**
     * preparing Solr document to index party account details in Solr
     *
     * @param partyAccountDataContext
     * @return
     */
    public static SolrInputDocument preparePartyAccountDocument(Map partyAccountDataContext)
            throws GenericEntityException {

        SolrInputDocument partyDoc = new SolrInputDocument();
        Map<String, Object> partyAccountData =
                (Map<String, Object>) partyAccountDataContext.get("partyAccountData");

        partyDoc.addField("docType", SOLR_DOC_TYPE_PARTY_ACCOUNT);

        // checking for the data if passed directly do indexing
        if (UtilValidate.isNotEmpty(partyAccountDataContext.get("isDataPassed"))
                && ((Boolean) partyAccountDataContext.get("isDataPassed"))) {
            partyDoc.addField("id", getPartyDocumentId((String) partyAccountData.get("partyId")));
            partyDoc.addField("personName", partyAccountData.get("fullName"));
            partyDoc.addField("personPartyId", partyAccountData.get("partyId"));
            partyDoc.addField("companyPartyId", partyAccountData.get("accountGroupPartyId"));
            partyDoc.addField("companyName", partyAccountData.get("companyName"));
            partyDoc.addField("partyPrimaryEmail", partyAccountData.get("email"));
        }
        // Case when no data is passed in the context
        else {
            // getting party from context
            GenericValue party = (GenericValue) partyAccountDataContext.get("party");

            // getting party relation data from the context
            GenericValue partyRelationship =
                    (GenericValue) partyAccountDataContext.get("partyRelationship");
            Delegator delegator = party.getDelegator();
            String partyId = party.getString("partyId");
            partyDoc.addField("id", getPartyDocumentId(partyId));

            // Getting person details
            GenericValue person =
                    EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();

            if (UtilValidate.isNotEmpty(person)) {
                partyDoc.addField("personName", person.getString("displayName"));
                partyDoc.addField("personPartyId", person.getString("partyId"));
            }

            // getting party account i.e partyGroup based on the party id from party relationship passed
            // in context
            GenericValue partyGroup =
                    EntityQuery.use(delegator)
                            .from("PartyGroup")
                            .where("partyId", partyRelationship.getString("partyIdFrom"))
                            .queryOne();

            if (UtilValidate.isNotEmpty(partyGroup)) {
                partyDoc.addField("companyPartyId", partyGroup.getString("partyId"));
                partyDoc.addField("companyName", partyGroup.getString("groupName"));
            }

            // getting primary email
            GenericValue partyContactMech =
                    PartyContactHelper.getPartyPrimaryEmailContactMech(delegator, partyId);
            String email = (String) partyContactMech.get("infoString");

            if (UtilValidate.isNotEmpty(email)) {
                partyDoc.addField("partyPrimaryEmail", email);
            }
        }

        return partyDoc;
    }
}
