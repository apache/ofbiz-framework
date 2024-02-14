

package com.simbaquartz.xsolr.services;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.hierarchy.PartyGroupForPartyUtils;
import com.simbaquartz.xsolr.operations.IndexOperation;
import com.simbaquartz.xsolr.util.SolrUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.*;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class SolrPartyServices {
    public static final String module = SolrPartyServices.class.getName();
    public static String solrSearchServer =
            UtilProperties.getPropertyValue("fsdSolrConnector.properties", "fsd.solr.instance");
    // public static String solrCoreName =
    // UtilProperties.getPropertyValue("fsdSolrConnector.properties", "fsd.solr.core.name");
    public static final String SOLR_PREFIX_PARTY = "p-";
    public static final String SOLR_DOC_TYPE_PARTY = "party";
    public static final String SOLR_DOC_TYPE_QUOTE = "quote";
    public static final String GOV_CUSTOMER_FACILITY_TYPE_ID = "CUST_FCLTY_TYPE";

    private static int DEFAULT_PAGE_SIZE = 10;

    static {
        String defaultViewSizeStr =
                UtilProperties.getPropertyValue("widget", "widget.form.defaultViewSize");
        try {
            DEFAULT_PAGE_SIZE = Integer.parseInt(defaultViewSizeStr);
        } catch (Exception ignored) {
        }
    }

    public static String getPartyDocumentId(String partyId) {
        return SOLR_PREFIX_PARTY + partyId;
    }

    /**
     * Indexes Person and related artifacts
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> onPersonCreateUpdateRemove(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<SolrInputDocument> documentList = FastList.newInstance();
        GenericValue personInstance = (GenericValue) context.get("personInstance");
        IndexOperation operation = IndexOperation.getEnumByName((String) context.get("operation"));

        GenericValue party = null;
        try {
            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            if (UtilValidate.isEmpty(solrCoreName)) {
                return ServiceUtil.returnError(
                        "A valid SOLR core name is not available, please add a valid core name in fsdSolrConnector.properties to proceed.");
            }

            if (UtilValidate.isNotEmpty(personInstance)) {
                SolrInputDocument partyDocument = new SolrInputDocument();

                party = personInstance.getRelatedOne("Party", false);

                partyDocument.addField("id", SOLR_PREFIX_PARTY + party.getString("partyId"));
                partyDocument.addField("docType", SOLR_DOC_TYPE_PARTY);
                partyDocument.addField("partyId", party.getString("partyId"));
                partyDocument.addField(
                        "partyType",
                        UtilMisc.toMap(
                                operation.getSolrAtomicUpdateModifier(), party.getString("partyTypeId")));
                partyDocument.addField(
                        "partyName",
                        UtilMisc.toMap(
                                operation.getSolrAtomicUpdateModifier(),
                                AxPartyHelper.getPartyName(delegator, party.getString("partyId"))));

                Boolean isSupplierPoc = false;
                Boolean isCustomerPoc = false;
                String employer = "";
                // get employer for party
                GenericValue parentPartyGroup = PartyGroupForPartyUtils.getPartyGroupForPartyId(party);
                if (UtilValidate.isNotEmpty(parentPartyGroup)) {
                    employer = parentPartyGroup.getString("partyId");
                    isSupplierPoc = HierarchyUtils.checkPartyRole(parentPartyGroup, "SUPPLIER");
                    isCustomerPoc = HierarchyUtils.checkPartyRole(parentPartyGroup, "CUSTOMER");
                }
                partyDocument.addField("employer", employer);
                partyDocument.addField("isSupplierPoc", isSupplierPoc);
                partyDocument.addField("isCustomerPoc", isCustomerPoc);
                documentList.add(partyDocument);
            } else Debug.logWarning("Unable to find a valid party with the supplied party Id.", module);

            SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
        } catch (Exception e) {
            Debug.logError("An error occurred: " + e, module);
            return ServiceUtil.returnError(
                    "An error occurred while trying to index party ("
                            + party.getString("partyId")
                            + "): "
                            + e.getMessage());
        }

        return result;
    }

    /**
     * Indexes PartyGroup and related artifacts
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> onPartyGroupCreateUpdateRemove(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<SolrInputDocument> documentList = FastList.newInstance();
        GenericValue partyGroupInstance = (GenericValue) context.get("partyGroupInstance");
        IndexOperation operation = IndexOperation.getEnumByName((String) context.get("operation"));

        GenericValue party = null;
        try {
            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            if (UtilValidate.isEmpty(solrCoreName)) {
                return ServiceUtil.returnError(
                        "A valid SOLR core name is not available, please add a valid core name in fsdSolrConnector.properties to proceed.");
            }

            if (UtilValidate.isNotEmpty(partyGroupInstance)) {
                SolrInputDocument partyDocument = new SolrInputDocument();

                party = partyGroupInstance.getRelatedOne("Party", false);
                GenericValue partyGroup = party.getRelatedOne("PartyGroup", false);

                partyDocument.addField("id", SOLR_PREFIX_PARTY + party.getString("partyId"));
                partyDocument.addField("docType", SOLR_DOC_TYPE_PARTY);
                partyDocument.addField("partyId", party.getString("partyId"));
                partyDocument.addField(
                        "partyType",
                        UtilMisc.toMap(
                                operation.getSolrAtomicUpdateModifier(), party.getString("partyTypeId")));
                partyDocument.addField(
                        "partyName",
                        UtilMisc.toMap(
                                operation.getSolrAtomicUpdateModifier(),
                                AxPartyHelper.getPartyName(delegator, party.getString("partyId"))));
                partyDocument.addField(
                        "partyGroupNameLocal",
                        UtilMisc.toMap(
                                operation.getSolrAtomicUpdateModifier(), partyGroup.getString("groupNameLocal")));

                documentList.add(partyDocument);
            } else Debug.logWarning("Unable to find a valid party with the supplied party Id.", module);

            SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
        } catch (Exception e) {
            Debug.logError("An error occurred: " + e, module);
            return ServiceUtil.returnError(
                    "An error occurred while trying to index party ("
                            + party.getString("partyId")
                            + "): "
                            + e.getMessage());
        }

        return result;
    }

    /**
     * Indexes PartyRole and related artifacts
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> onPartyRoleCreateUpdateRemove(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<SolrInputDocument> documentList = FastList.newInstance();
        GenericValue partyRoleInstance = (GenericValue) context.get("partyRoleInstance");
        IndexOperation operation = IndexOperation.getEnumByName((String) context.get("operation"));

        GenericValue party = null;
        try {
            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            if (UtilValidate.isEmpty(solrCoreName)) {
                return ServiceUtil.returnError(
                        "A valid SOLR core name is not available, please add a valid core name in fsdSolrConnector.properties to proceed.");
            }

            if (UtilValidate.isNotEmpty(partyRoleInstance)) {
                SolrInputDocument partyDocument = new SolrInputDocument();

                party = partyRoleInstance.getRelatedOne("Party", false);
                partyDocument.addField("id", SOLR_PREFIX_PARTY + party.getString("partyId"));

                partyDocument.addField(
                        "partyRoles",
                        UtilMisc.toMap(
                                operation.getSolrAtomicUpdateModifier(),
                                partyRoleInstance.getString("roleTypeId")));
                partyDocument.addField("isSupplier", isSupplier(partyRoleInstance));
                partyDocument.addField("isPartner", isPartner(partyRoleInstance));

                documentList.add(partyDocument);
            } else Debug.logWarning("Unable to find a valid party with the supplied party Id.", module);

            SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
        } catch (Exception e) {
            Debug.logError("An error occurred: " + e, module);
            return ServiceUtil.returnError(
                    "An error occurred while trying to index party ("
                            + party.getString("partyId")
                            + "): "
                            + e.getMessage());
        }

        return result;
    }

    public static Map<String, Object> onPartyAttributeCreateUpdate(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        SolrInputDocument orderDocument = new SolrInputDocument();
        List<SolrInputDocument> documentList = FastList.newInstance();

        GenericValue partyAttribute = (GenericValue) context.get("partyAttributeInstance");
        String orderId = partyAttribute.getString("partyId");
        Debug.logInfo("Starting to index Party Attribute " + orderId + " in Solr.", module);
        IndexOperation operation = IndexOperation.getEnumByName((String) context.get("operation"));

        try {
            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            if (UtilValidate.isEmpty(solrCoreName))
                return ServiceUtil.returnError(
                        "A valid SOLR core name is not available, please add a valid core name in fsdSolrConnector.properties to proceed.");

            if (UtilValidate.isNotEmpty(partyAttribute)) {
                String attrName = partyAttribute.getString("attrName");
                String attrValue = partyAttribute.getString("attrValue");

                if (attrName.equalsIgnoreCase("Designation")) {
                    orderDocument.addField("id", getPartyDocumentId(orderId));
                    orderDocument.addField(
                            "docType",
                            UtilMisc.toMap(operation.getSolrAtomicUpdateModifier(), SOLR_DOC_TYPE_PARTY));
                    orderDocument.addField(
                            "partyDesignation",
                            UtilMisc.toMap(operation.getSolrAtomicUpdateModifier(), attrValue));
                    documentList.add(orderDocument);

                    SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
                }
            } else {
                Debug.logWarning(
                        "Unable to find a valid party attribute with the supplied order Id and attr name.",
                        module);
            }
        } catch (Exception e) {
            Debug.logError("An error occurred: " + e, module);
            return ServiceUtil.returnError(
                    "An error occurred while trying to index party with attribute: " + e.getMessage());
        }

        return result;
    }

    /**
     * Indexes Party and related artifacts
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> indexPartyInSolr(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String partyId = (String) context.get("partyId");
        Debug.logInfo("Starting to index Party " + partyId + " in Solr.", module);
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        List<SolrInputDocument> documentList = FastList.newInstance();
        try {
            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            if (UtilValidate.isEmpty(solrCoreName)) {
                return ServiceUtil.returnError("A valid SOLR core name is not available, please add a valid core name in fsdSolrConnector.properties to proceed.");
            }

            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
            if (UtilValidate.isNotEmpty(party)) {
                context.put("operation", IndexOperation.SET.getSolrAtomicUpdateModifier());

                switch (party.getString("partyTypeId")) {
                    case "PERSON":
                        GenericValue person = party.getRelatedOne("Person", false);
                        context.put("personInstance", person);
                        onPersonCreateUpdateRemove(dctx, context);
                        context.remove("personInstance");
                        break;
                    case "PARTY_GROUP":
                        GenericValue partyGroup = party.getRelatedOne("PartyGroup", false);
                        context.put("partyGroupInstance", partyGroup);
                        onPartyGroupCreateUpdateRemove(dctx, context);
                        context.remove("partyGroupInstance");
                        break;
                }

                documentList.add(preparePartySolrDocument(party, context, dispatcher));
                SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);
            }
        } catch (Exception e) {
            Debug.logError("An error occurred: " + e, module);
            return ServiceUtil.returnError(
                    "An error occurred while trying to index party (" + partyId + "): " + e.getMessage());
        }

        return result;
    }

    private static Boolean isPartner(List<GenericValue> partyRoles) {
        for (GenericValue partyRole : partyRoles) {
            if (isPartner(partyRole)) return true;
        }
        return false;
    }

    private static Boolean isSupplier(List<GenericValue> partyRoles) {
        for (GenericValue partyRole : partyRoles) {
            if (isSupplier(partyRole)) return true;
        }
        return false;
    }

    private static Boolean isPartner(GenericValue partyRole) {
        String roleTypeId = partyRole.getString("roleTypeId");
        if (roleTypeId.equalsIgnoreCase("PARTNER")) return true;
        else return false;
    }

    private static Boolean isSupplier(GenericValue partyRole) {
        String roleTypeId = partyRole.getString("roleTypeId");
        if (roleTypeId.equalsIgnoreCase("SUPPLIER")) return true;
        else return false;
    }

    private static Boolean isCustomer(List<GenericValue> partyRoles) {
        for (GenericValue partyRole : partyRoles) {
            if (isCustomer(partyRole)) return true;
        }
        return false;
    }

    private static Boolean isCustomer(GenericValue partyRole) {
        String roleTypeId = partyRole.getString("roleTypeId");
        if (roleTypeId.equalsIgnoreCase("CUSTOMER")) return true;
        else return false;
    }

    private static Boolean isGovernmentLocation(List<GenericValue> partyRoles) {
        for (GenericValue partyRole : partyRoles) {
            if (isGovernmentLocation(partyRole)) return true;
        }
        return false;
    }

    private static Boolean isGovernmentLocation(GenericValue partyRole) {
        String roleTypeId = partyRole.getString("roleTypeId");
        if (roleTypeId.equalsIgnoreCase("GOVERNMENT_LOC")) return true;
        else return false;
    }

    private static Boolean isGovernmentOrganization(List<GenericValue> partyRoles) {
        for (GenericValue partyRole : partyRoles) {
            if (isGovernmentOrganization(partyRole)) return true;
        }
        return false;
    }

    private static Boolean isGovernmentOrganization(GenericValue partyRole) {
        String roleTypeId = partyRole.getString("roleTypeId");
        if (roleTypeId.equalsIgnoreCase("GOVERNMENT_ORG")) return true;
        else return false;
    }

    public static Map<String, Object> deletePartyInSolr(
            DispatchContext dctx, Map<String, Object> context) {
        Debug.logInfo("Starting deleting of Party in Solr.", module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String partyId = (String) context.get("partyId");

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            String solrCoreName = SolrUtils.getSolrCoreName(delegator);
            String partyDocumentId = SOLR_PREFIX_PARTY + partyId;
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
            //   ctx.put("solrCoreName", solrCoreName);
            ctx.put("deleteQuery", "id:" + partyDocumentId);

            Map<String, Object> response = dispatcher.runSync("deleteAllDocsFromSolr", ctx);
            if (ServiceUtil.isSuccess(response))
                Debug.log("Successfully deleted specified party from Solr", module);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(
                    "An error occurred while trying to delete all party docs from Solr: " + e.getMessage());
        }

        return result;
    }

    public static Map<String, Object> indexAllPartiesInSolr(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String solrCoreName = SolrUtils.getSolrCoreName(delegator);

        // First delete all docs of type party from SOLR
        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
            // ctx.put("solrCoreName", solrCoreName);
            ctx.put("deleteQuery", "docType:" + SOLR_DOC_TYPE_PARTY);

            Map<String, Object> response = dispatcher.runSync("deleteAllDocsFromSolr", ctx);
            if (ServiceUtil.isSuccess(response))
                Debug.log("Successfully deleted all party docs from Solr", module);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(
                    "An error occurred while trying to delete all party docs from Solr: " + e.getMessage());
        }

        try {
            List<SolrInputDocument> documentList = FastList.newInstance();
            List<GenericValue> partyRelationships = FastList.newInstance();
            List<GenericValue> parties = delegator.findAll("Party", true);
            Debug.log("Found " + parties.size() + " parties to index.", module);
            Date thruDate = new Date();
            Date nowDate = new Date();
            for (GenericValue party : parties) {

                try {
                    partyRelationships =
                            EntityQuery.use(delegator)
                                    .from("PartyRelationship")
                                    .where("partyIdTo", party.get("partyId"))
                                    .queryList();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (UtilValidate.isNotEmpty(partyRelationships)) {
                    for (GenericValue partyRelation : partyRelationships) {

                        if (EntityUtil.isValueActive(partyRelation, UtilDateTime.nowTimestamp())) {
                            documentList.add(preparePartySolrDocument(party, context, dispatcher));
                        }
                    }
                } else {
                    List<GenericValue> agencyRecords =
                            EntityQuery.use(delegator)
                                    .from("PartyRole")
                                    .where("partyId", party.get("partyId"), "roleTypeId", "GOVERNMENT_AGENCY")
                                    .queryList();
                    if (UtilValidate.isNotEmpty(agencyRecords)) {
                        documentList.add(preparePartySolrDocument(party, context, dispatcher));
                    }
                }
            }
            SolrHelperServices.publishDocumentList(dispatcher, context, documentList, solrCoreName);

            Debug.log("Successfully indexed " + parties.size() + " parties.", module);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(
                    "An error occurred while trying to fetch all parties: " + e.getMessage());
        } catch (GenericServiceException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * * Get list of parties who has given role
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> getPartiesForRole(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<Map<String, Object>> partyList = FastList.newInstance();

        String roleTypeId = (String) context.get("roleTypeId");
        String searchKeyword = (String) context.get("keyword");

        String solrCoreName = SolrUtils.getSolrCoreName(delegator);

        String searchText = "*:*";
        if (UtilValidate.isNotEmpty(searchKeyword)) searchText = searchKeyword;
        SolrQuery searchQuery = new SolrQuery(searchText);
        searchQuery.setRows(10000); // Get max numbers
        searchQuery.addFilterQuery("docType:" + SOLR_DOC_TYPE_PARTY);
        searchQuery.addField("partyId");
        searchQuery.addField("partyName");
        searchQuery.addField("partyAddress1");
        searchQuery.addField("partyCity");
        searchQuery.addField("partyType");
        searchQuery.addField("partyPostalCode");
        searchQuery.addField("partyStateProvinceGeoId");
        searchQuery.addFilterQuery("partyRoles:" + roleTypeId);
        HttpSolrClient httpSolrClient = null;
        try {
            SolrUtils.getInstance();
            httpSolrClient = SolrUtils.getHttpSolrClient();

            QueryResponse queryResponse = httpSolrClient.query(searchQuery);
            SolrDocumentList foundDocuments = queryResponse.getResults();
            for (SolrDocument record : foundDocuments) {
                Map<String, Object> partyInfo = FastMap.newInstance();
                // GenericValue partyGv = delegator.makeValue("Person");
                partyInfo.put("partyId", record.get("partyId"));
                partyInfo.put("partyName", record.get("partyName"));
                partyInfo.put("partyAddress1", record.get("partyAddress1"));
                partyInfo.put("partyType", record.get("partyType"));
                partyInfo.put("partyCity", record.get("partyCity"));
                partyInfo.put("partyPostalCode", record.get("partyPostalCode"));
                partyInfo.put("partyStateProvinceGeoId", record.get("partyStateProvinceGeoId"));

                partyList.add(partyInfo);
            }
        } catch (IOException e) {
            Debug.logError(e, module);
        } catch (SolrServerException e) {
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
        result.put("partyList", partyList);
        return result;
    }

    private static SolrInputDocument preparePartySolrDocument(GenericValue party, Map<String, Object> context, LocalDispatcher dispatcher) throws GenericEntityException {
        SolrInputDocument partyDocument = new SolrInputDocument();
        if (UtilValidate.isNotEmpty(party)) {
            Delegator delegator = party.getDelegator();
            String partyId = party.getString("partyId");
            String partyName = AxPartyHelper.getPartyName(delegator, partyId);
            IndexOperation operation = IndexOperation.SET;

            partyDocument.addField("id", SOLR_PREFIX_PARTY + partyId);
            partyDocument.addField("docType", SOLR_DOC_TYPE_PARTY);
            partyDocument.addField("partyId", partyId);

            partyDocument.addField("partyType", UtilMisc.toMap(operation.getSolrAtomicUpdateModifier(), party.getString("partyTypeId")));
            partyDocument.addField("partyName", UtilMisc.toMap(operation.getSolrAtomicUpdateModifier(), partyName));
            String partyTypeId = party.getString("partyTypeId");
            if (UtilValidate.isNotEmpty(partyTypeId)) {
                switch (party.getString("partyTypeId")) {
                    case "PERSON":
                        GenericValue person = party.getRelatedOne("Person", false);
                        break;
                    case "PARTY_GROUP":
                        GenericValue partyGroup = party.getRelatedOne("PartyGroup", false);
                        if (UtilValidate.isNotEmpty(partyGroup)) {
                            partyDocument.addField("partyGroupNameLocal", UtilMisc.toMap(operation.getSolrAtomicUpdateModifier(), partyGroup.getString("groupNameLocal")));
                        }
                        break;
                }
            }

            List<GenericValue> partyRoles = party.getRelated("PartyRole", null, null, false);
            List<String> partyRoleNames = FastList.newInstance();
            for (GenericValue partyRole : partyRoles) {
                partyRoleNames.add(partyRole.getString("roleTypeId"));
            }
            partyDocument.addField("partyRoles", UtilMisc.toMap(IndexOperation.SET.getSolrAtomicUpdateModifier(), partyRoleNames));

            partyDocument.addField("isSupplier", isSupplier(partyRoles));
            partyDocument.addField("isPartner", isPartner(partyRoles));

            Boolean isSupplierPoc = false;
            Boolean isCustomerPoc = false;
            String employer = "";
            // get employer for party
            GenericValue parentPartyGroup = PartyGroupForPartyUtils.getPartyGroupForPartyId(party);
            if (UtilValidate.isNotEmpty(parentPartyGroup)) {
                employer = parentPartyGroup.getString("partyId");
                isSupplierPoc = HierarchyUtils.checkPartyRole(parentPartyGroup, "SUPPLIER");
                isCustomerPoc = HierarchyUtils.checkPartyRole(parentPartyGroup, "CUSTOMER");
            }
            partyDocument.addField("employer", employer);
            partyDocument.addField("isSupplierPoc", isSupplierPoc);
            partyDocument.addField("isCustomerPoc", isCustomerPoc);

            // Index owner-account-id (organization of the party)
            partyDocument.addField("ownerAccountId", identifyOwnerAccountId(delegator, partyId));

            // Index Party Postal Address
            GenericValue partyPostalAddressGv = PartyWorker.findPartyLatestPostalAddress(partyId, party.getDelegator());
            if (UtilValidate.isNotEmpty(partyPostalAddressGv)) {
                partyDocument.addField("partyAddress1", partyPostalAddressGv.getString("address1"));
                partyDocument.addField("partyAddress2", partyPostalAddressGv.getString("address2"));
                partyDocument.addField("partyCity", partyPostalAddressGv.getString("city"));
                partyDocument.addField("partyPostalCode", partyPostalAddressGv.getString("postalCode"));
                partyDocument.addField(
                        "partyStateProvinceGeoId", partyPostalAddressGv.getString("stateProvinceGeoId"));
                partyDocument.addField("partyCountryGeoId", partyPostalAddressGv.getString("countryGeoId"));
            }

            GenericValue telecomNumber = AxPartyHelper.findPartyLatestTelecomNumber(partyId, delegator);
            if (null != telecomNumber) {
                String formattedPhoneNumberInUSFormat =
                        AxPartyHelper.getFormattedPhoneNumber(
                                delegator,
                                telecomNumber.getString("contactMechId"),
                                telecomNumber.getString("tnCountryCode"),
                                telecomNumber.getString("tnAreaCode"),
                                telecomNumber.getString("tnContactNumber"),
                                telecomNumber.getString("extension"));
                partyDocument.addField("phoneNumber", formattedPhoneNumberInUSFormat);
            }

            GenericValue emailAddress = AxPartyHelper.findPartyLatestEmailAddress(partyId, delegator);
            if (null != emailAddress) {
                partyDocument.addField("emailAddress", emailAddress.getString("infoString"));

                if (UtilValidate.isNotEmpty(partyName)
                        && UtilValidate.isNotEmpty(emailAddress.getString("infoString"))) {
                    partyDocument.addField(
                            "nameAndEmailAddress",
                            partyName.concat(",").concat(emailAddress.getString("infoString")));
                }
            }

      /*if (isCustomer(partyRoles)
          || isGovernmentLocation(partyRoles)
          || isGovernmentOrganization(partyRoles)) {
        // check for party facility type if user has a customer/gov location role to index facility
        // type, e.g. VAMC, CBOC, OC etc.
        GenericValue partyFacilityType =
            EntityQuery.use(delegator)
                .from("PartyClassificationAndGroup")
                .where(
                    "partyId", partyId, "partyClassificationTypeId", GOV_CUSTOMER_FACILITY_TYPE_ID)
                .queryFirst();

        if (UtilValidate.isNotEmpty(partyFacilityType)) {
          partyDocument.addField(
              "partyFacilityTypeId", partyFacilityType.getString("partyClassificationGroupId"));
        }
      }*/

            if (UtilValidate.isNotEmpty(emailAddress)) {
                EntityConditionList<EntityExpr> ecl =
                        EntityCondition.makeCondition(
                                EntityOperator.OR,
                                EntityCondition.makeCondition(
                                        EntityFunction.UPPER_FIELD("toString"),
                                        EntityOperator.LIKE,
                                        EntityFunction.UPPER("%" + emailAddress.getString("infoString") + "%")),
                                EntityCondition.makeCondition(
                                        EntityFunction.UPPER_FIELD("ccString"),
                                        EntityOperator.LIKE,
                                        EntityFunction.UPPER("%" + emailAddress.getString("infoString") + "%")),
                                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));

                GenericValue communicationEvent =
                        EntityQuery.use(delegator)
                                .from("CommunicationEvent")
                                .where(ecl)
                                .orderBy("-lastUpdatedStamp")
                                .queryFirst();
                if (null != communicationEvent) {

                    partyDocument.addField(
                            "lastEngagedDate",
                            SolrUtils.toSolrFormattedDateString(
                                    (Timestamp) communicationEvent.get("lastUpdatedStamp")));
                }
            } else {
                GenericValue communicationEvent =
                        EntityQuery.use(delegator)
                                .from("CommunicationEvent")
                                .where("partyIdFrom", partyId)
                                .orderBy("-lastUpdatedStamp")
                                .queryFirst();
                if (null != communicationEvent) {
                    partyDocument.addField(
                            "lastEngagedDate",
                            SolrUtils.toSolrFormattedDateString(
                                    (Timestamp) communicationEvent.get("lastUpdatedStamp")));
                }
            }

            GenericValue partyAttribute =
                    EntityQuery.use(delegator)
                            .from("PartyAttribute")
                            .where(UtilMisc.toMap("partyId", partyId, "attrName", "Designation"))
                            .queryOne();
            if (UtilValidate.isNotEmpty(partyAttribute)) {
                partyDocument.addField("title", partyAttribute.getString("attrValue"));
            }

          /*GenericValue partyLogoContent =
              PartyContentWrapper.getFirstPartyContentByType(
                  party.getString("partyId"), null, CommonContentTypesEnum.PHOTO.getTypeId(), delegator);

          if (UtilValidate.isNotEmpty(partyLogoContent)) {
            String partyLogoContentId = partyLogoContent.getString("contentId");
            partyDocument.addField("avatarContentId", partyLogoContentId);
          }*/
        }

        return partyDocument;
    }

    private static String identifyOwnerAccountId(Delegator delegator, String partyId) {
        String ownerAccountId = null;
        try {
            List<GenericValue> partyRelationshipList = EntityQuery.use(delegator).from("PartyRelationship")
                    .where("partyIdTo", partyId, "partyRelationshipTypeId", "MEMBER", "roleTypeIdFrom","ORGANIZATION_ROLE")
                    .cache(true).filterByDate(UtilDateTime.nowTimestamp()).queryList();

            if(UtilValidate.isNotEmpty(partyRelationshipList)) {
                ownerAccountId = partyRelationshipList.get(0).getString("partyIdFrom");
            }
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }
        return ownerAccountId;
    }

    public static Map<String, Object> onPartyRelationshipCreateUpdate(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue partyRelationshipInstance =
                (GenericValue) context.get("partyRelationshipInstance");
        if (UtilValidate.isNotEmpty(partyRelationshipInstance)) {
            if (!EntityUtil.isValueActive(partyRelationshipInstance, UtilDateTime.nowTimestamp())) {
                // delete party from solr if thru date is set
                Map<String, Object> inputMap = new HashMap<String, Object>();
                inputMap.put("partyId", partyRelationshipInstance.getString("partyIdTo"));
                inputMap.put("userLogin", context.get("userLogin"));

                try {
                    Map<String, Object> servResult = dispatcher.runSync("deletePartyInSolr", inputMap);
                    Debug.logInfo("delete Party from Solr Service Response: " + servResult, module);
                } catch (GenericServiceException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Returns parties data fetched from Solr
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> searchParties(
            DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        List<Map> searchResults = new ArrayList<>();
        int recordsSize = 0;

        Integer startIndex = (Integer) context.get("startIndex");
        Integer viewSize = (Integer) context.get("viewSize");

        if (startIndex == null || startIndex <= 0) startIndex = 0;
        if (viewSize == null || viewSize < 0) viewSize = DEFAULT_PAGE_SIZE;
        String solrCoreName = SolrUtils.getSolrCoreName(delegator);

        try {

            SolrClient httpSolrClient =
                    new HttpSolrClient.Builder(solrSearchServer + "/" + solrCoreName).build();
            String searchText = "*:*";
            SolrQuery searchQuery = new SolrQuery(searchText);
            String sortBy = (String) context.get("sortBy");

            if (sortBy.charAt(0) == '-') {
                sortBy = sortBy.substring(1, sortBy.length());
                searchQuery.setSort(sortBy, SolrQuery.ORDER.desc);
            } else {
                searchQuery.setSort(sortBy, SolrQuery.ORDER.asc);
            }

            searchQuery.setStart(startIndex);
            searchQuery.setRows(viewSize);

            // keep minimum fields to be returned so query is fast and only returns required data.
            searchQuery.addField("partyId");
            searchQuery.addField("partyType");
            searchQuery.addField("partyName");
            searchQuery.addField("partyRoles");
            searchQuery.addField("partyAddress1");
            searchQuery.addField("partyCity");
            searchQuery.addField("partyPostalCode");
            searchQuery.addField("partyStateProvinceGeoId");
            searchQuery.addField("partyCountryGeoId");
            searchQuery.addField("emailAddress");
            searchQuery.addField("phoneNumber");
            searchQuery.addField("nameAndEmailAddress");
            // looking only for parties
            searchQuery.addFilterQuery("docType:party");

            // Party Id filter
            String partyId = (String) context.get("partyId");
            if (UtilValidate.isNotEmpty(partyId)) {
                String partyIdConstraint = "partyId:*" + partyId + "*";
                searchQuery.addFilterQuery(partyIdConstraint);
            }

            // Party Type filter
            String partyType = (String) context.get("partyType");
            if (UtilValidate.isNotEmpty(partyType)) {
                String partyTypeConstraint = "partyType:*" + partyType + "*";
                searchQuery.addFilterQuery(partyTypeConstraint);
            }

            // party name filter
            String partyName = (String) context.get("partyName");
            if (UtilValidate.isNotEmpty(partyName)) {
                String partyNameConstraint = "partyName_ci:" + "*" + partyName + "*";
                searchQuery.addFilterQuery(partyNameConstraint);
            }

            // party name filter
            String fullPartyName = (String) context.get("fullPartyName");
            if (UtilValidate.isNotEmpty(fullPartyName)) {
                String fullPartyNameConstraint = "partyName_ci:\"" + fullPartyName + "\"";
                searchQuery.addFilterQuery(fullPartyNameConstraint);
            }

            // party city filter
            String partyCity = (String) context.get("partyCity");
            if (UtilValidate.isNotEmpty(partyCity)) {
                String partyCityConstraint = "partyCity_ci:*" + partyCity + "*";
                searchQuery.addFilterQuery(partyCityConstraint);
            }

            // party state filter
            String partyStateProvinceGeoId = (String) context.get("partyStateProvinceGeoId");
            if (UtilValidate.isNotEmpty(partyStateProvinceGeoId)) {
                String partyStateProvinceGeoIdConstraint =
                        "partyStateProvinceGeoId:" + partyStateProvinceGeoId;
                searchQuery.addFilterQuery(partyStateProvinceGeoIdConstraint);
            }

            // party employer filter
            String employer = (String) context.get("employer");
            if (UtilValidate.isNotEmpty(employer)) {
                String employerConstraint = "employer:" + employer;
                searchQuery.addFilterQuery(employerConstraint);
            }

            // party country filter
            String partyCountryGeoId = (String) context.get("partyCountryGeoId");
            if (UtilValidate.isNotEmpty(partyCountryGeoId)) {
                String partyCountryGeoIdConstraint = "partyCountryGeoId:" + partyCountryGeoId;
                searchQuery.addFilterQuery(partyCountryGeoIdConstraint);
            }

            // party country filter
            String partyPostalCode = (String) context.get("partyPostalCode");
            if (UtilValidate.isNotEmpty(partyPostalCode)) {
                String partyPostalCodeConstraint = "partyPostalCode:" + partyPostalCode;
                searchQuery.addFilterQuery(partyPostalCodeConstraint);
            }

            // party role filter
            String selectedRoleType = (String) context.get("selectedRoleType");
            if (UtilValidate.isNotEmpty(selectedRoleType)) {
                String supplierPartyContactRoleConstraint = "partyRoles:" + selectedRoleType;
                searchQuery.addFilterQuery(supplierPartyContactRoleConstraint);
            }

            // party roles filter
            List selectedRoleTypes = (List) context.get("selectedRoleTypes");
            if (UtilValidate.isNotEmpty(selectedRoleTypes)) {
                String partyRolesConstraint =
                        "partyRoles: (" + SolrUtils.prepareORQueryConstraint(selectedRoleTypes) + ")";
                searchQuery.addFilterQuery(partyRolesConstraint);
            }

            // party phoneNumber filter
            String phoneNumber = (String) context.get("phoneNumber");
            if (UtilValidate.isNotEmpty(phoneNumber)) {
                String partyPhoneNumberConstraint = "phoneNumber:\"" + phoneNumber + "\"";
                searchQuery.addFilterQuery(partyPhoneNumberConstraint);
            }

            // party emailAddress filter
            String emailAddress = (String) context.get("emailAddress");
            if (UtilValidate.isNotEmpty(emailAddress)) {
                String partyEmailAddressConstraint = "emailAddress:" + emailAddress;
                searchQuery.addFilterQuery(partyEmailAddressConstraint);
            }

            // party nameAndEmailAddress filter
            String nameAndEmailAddress = (String) context.get("nameAndEmailAddress");
            if (UtilValidate.isNotEmpty(nameAndEmailAddress)) {
                String nameAndEmailAddressConstraint =
                        "nameAndEmailAddress_ci:*" + nameAndEmailAddress + "*";
                searchQuery.addFilterQuery(nameAndEmailAddressConstraint);
            }

            QueryResponse queryResponse;
            SolrDocumentList foundDocuments;

            // used for debugging
            Debug.logInfo("Invoking solr search with query: " + searchQuery.toString(), module);

            queryResponse = httpSolrClient.query(searchQuery);

            foundDocuments = queryResponse.getResults();
            recordsSize = (int) foundDocuments.getNumFound();

            // used for debugging
            Debug.logInfo("Number of parties found : " + recordsSize, module);

            for (SolrDocument record : foundDocuments) {
                Map<String, Object> partyEntry = new HashMap<>();
                GenericValue party =
                        HierarchyUtils.getPartyByPartyId(delegator, (String) record.get("partyId"));
                if (UtilValidate.isNotEmpty(party)) {
                    Boolean isInternalParty = HierarchyUtils.checkPartyRole(party, "INTERNAL_ORGANIZATIO");

                    if (!isInternalParty) {
                        partyEntry.put("partyId", record.get("partyId"));
                        partyEntry.put("partyName", record.get("partyName"));
                        partyEntry.put("partyType", record.get("partyType"));
                        partyEntry.put("partyRoles", record.get("partyRoles"));
                        partyEntry.put("partyAddress1", record.get("partyAddress1"));
                        partyEntry.put("partyCity", record.get("partyCity"));
                        partyEntry.put("partyPostalCode", record.get("partyPostalCode"));
                        partyEntry.put("partyStateProvinceGeoId", record.get("partyStateProvinceGeoId"));
                        partyEntry.put("partyCountryGeoId", record.get("partyCountryGeoId"));
                        partyEntry.put("emailAddress", record.get("emailAddress"));
                        partyEntry.put("phoneNumber", record.get("phoneNumber"));
                        partyEntry.put(
                                "nameAndEmailAddressConstraint", record.get("nameAndEmailAddressConstraint"));
                        searchResults.add(partyEntry);
                    }
                }
            } // End of Iterating SolrDocuments
        } catch (SolrServerException | IOException e) {
            Debug.logError(e, e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnFailure(e.getMessage());
        } catch (Exception ex) {
            Debug.logError(ex, ex.getMessage(), module);
            ex.printStackTrace();
            return ServiceUtil.returnFailure(ex.getMessage());
        }
        result.put("searchResults", searchResults);
        result.put("totalResultSize", recordsSize);
        return result;
    }

    /**
     * performs Solr search, make it a generic one, useful for Party Search
     */
    public static Map<String, Object> solrPartySearch(DispatchContext dctx, Map context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String keyword = (String) context.get("keyword");
        String solrCoreName = (String) context.get("solrCoreName");
        Integer startIndex = (Integer) context.get("startIndex");
        Integer viewSize = (Integer) context.get("viewSize");

        if (UtilValidate.isEmpty(solrCoreName)) {
            solrCoreName = SolrUtils.getSolrCoreName(dctx.getDelegator());
        }

        List<Map<String, Object>> resultsList = FastList.newInstance();

        SolrClient httpSolrClient =
                new HttpSolrClient.Builder(solrSearchServer + "/" + solrCoreName).build();
        String searchText = "*:*";
        if (UtilValidate.isNotEmpty(keyword)) {
            if (keyword.contains("-")) keyword = keyword.replaceAll("-", "\\\\-");
            searchText = "_text_:" + keyword;
        }
        ;
        SolrQuery searchQuery = new SolrQuery(searchText);

        if (startIndex == null || startIndex <= 0) {
            startIndex = 0;
        }

        if (viewSize == null || viewSize < 0) {
            viewSize = 10;
        }

        // startIndex & viewSize filter
        searchQuery.setStart(startIndex);
        searchQuery.setRows(viewSize);

        searchQuery.addField("partyId");
        searchQuery.addField("partyName");
        searchQuery.addField("emailAddress");
        searchQuery.addField("quoteId");
        searchQuery.addField("docType");
        searchQuery.addField("phoneNumber");
        searchQuery.addField("isSupplierPoc");
        searchQuery.addField("isCustomerPoc");
        searchQuery.addField("employer");

        // additional useful fields
        searchQuery.addField("quoteName");
        searchQuery.addField("quotePurchaseOrderNumber");
        searchQuery.addField("quoteCustomerPartyId");
        searchQuery.addField("quoteCustomerName");
        searchQuery.addField("quoteSupplierPartyId");
        searchQuery.addField("quoteSupplierPartyName");
        searchQuery.addField("quoteBillToCustomerPartyName");
        searchQuery.addField("quoteTotal");

        try {
            QueryResponse queryResponse = httpSolrClient.query(searchQuery);
            SolrDocumentList foundDocuments = queryResponse.getResults();
            for (SolrDocument record : foundDocuments) {
                Map<String, Object> resultEntry = FastMap.newInstance();
                String docType = (String) record.get("docType");
                if (SOLR_DOC_TYPE_QUOTE.equalsIgnoreCase(docType)) {
                    resultEntry.put("docType", SOLR_DOC_TYPE_QUOTE);
                    resultEntry.put("quoteId", record.get("quoteId"));
                    resultEntry.put("quoteName", record.get("quoteName"));
                    resultEntry.put("quotePurchaseOrderNumber", record.get("quotePurchaseOrderNumber"));
                    resultEntry.put("quoteCustomerPartyId", record.get("quoteCustomerPartyId"));
                    resultEntry.put("quoteCustomerName", record.get("quoteCustomerName"));
                    resultEntry.put("quoteSupplierPartyId", record.get("quoteSupplierPartyId"));
                    resultEntry.put("quoteSupplierPartyName", record.get("quoteSupplierPartyName"));
                    resultEntry.put(
                            "quoteBillToCustomerPartyName", record.get("quoteBillToCustomerPartyName"));
                    resultEntry.put("quoteTotal", record.get("quoteTotal"));
                } else if (SOLR_DOC_TYPE_PARTY.equalsIgnoreCase(docType)) {
                    resultEntry.put("docType", SOLR_DOC_TYPE_PARTY);
                    resultEntry.put("partyId", record.get("partyId"));
                    resultEntry.put("partyName", record.get("partyName"));
                    resultEntry.put("emailAddress", record.get("emailAddress"));
                    resultEntry.put("phoneNumber", record.get("phoneNumber"));
                    resultEntry.put("isSupplierPoc", record.get("isSupplierPoc"));
                    resultEntry.put("isCustomerPoc", record.get("isCustomerPoc"));
                    resultEntry.put("employer", record.get("employer"));
                }

                resultsList.add(resultEntry);
            }
        } catch (SolrServerException | IOException e) {
            Debug.logError(e, module);
        }
        result.put("searchResult", resultsList);

        return result;
    }

    /**
     * performs Solr search, make it a generic one, useful for Person Search
     */
    public static Map<String, Object> solrPersonSearch(DispatchContext dctx, Map context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String keyword = (String) context.get("keyword");
        String solrCoreName = (String) context.get("solrCoreName");
        Integer viewSize = (Integer) context.get("viewSize");
        Integer startIndex = (Integer) context.get("startIndex");
        if (UtilValidate.isEmpty(solrCoreName)) {
            solrCoreName = SolrUtils.getSolrCoreName(dctx.getDelegator());
        }
        List<Map<String, Object>> resultsList = FastList.newInstance();

        SolrClient httpSolrClient =
                new HttpSolrClient.Builder(solrSearchServer + "/" + solrCoreName).build();
        String searchText = "*:*";
        if (UtilValidate.isNotEmpty(keyword)) {
            if (keyword.contains("-")) keyword = keyword.replaceAll("-", "\\\\-");
            searchText = "_text_:" + keyword;
        }
        ;
        SolrQuery searchQuery = new SolrQuery(searchText);

        if (startIndex == null || startIndex <= 0) {
            startIndex = 0;
        }

        if (viewSize == null || viewSize < 0) {
            viewSize = 10;
        }

        // startIndex & viewSize filter
        searchQuery.setStart(startIndex);
        searchQuery.setRows(viewSize);

        searchQuery.addField("partyId");
        searchQuery.addField("partyName");
        searchQuery.addField("emailAddress");
        searchQuery.addField("docType");
        searchQuery.addField("phoneNumber");
        searchQuery.addField("isSupplierPoc");
        searchQuery.addField("isCustomerPoc");
        searchQuery.addField("employer");

        try {
            QueryResponse queryResponse = httpSolrClient.query(searchQuery);
            SolrDocumentList foundDocuments = queryResponse.getResults();
            for (SolrDocument record : foundDocuments) {
                Map<String, Object> resultEntry = FastMap.newInstance();
                String docType = (String) record.get("docType");
                if (SOLR_DOC_TYPE_PARTY.equalsIgnoreCase(docType)) {
                    resultEntry.put("docType", SOLR_DOC_TYPE_PARTY);
                    resultEntry.put("partyId", record.get("partyId"));
                    resultEntry.put("partyName", record.get("partyName"));
                    resultEntry.put("emailAddress", record.get("emailAddress"));
                    resultEntry.put("phoneNumber", record.get("phoneNumber"));
                    resultEntry.put("isSupplierPoc", record.get("isSupplierPoc"));
                    resultEntry.put("employer", record.get("employer"));
                    resultEntry.put("isCustomerPoc", record.get("isCustomerPoc"));
                }

                resultsList.add(resultEntry);
            }
        } catch (SolrServerException | IOException e) {
            Debug.logError(e, module);
        }
        result.put("searchResult", resultsList);

        return result;
    }
}
