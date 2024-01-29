package com.simbaquartz.xparty.helpers;

import com.simbaquartz.xparty.utils.PartyTypesEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

/** Exposes party relationship management service utilities. */
public class ExtPartyRelationshipHelper {
  private static final String module = ExtPartyRelationshipHelper.class.getName();

  /**
   * Returns the list of active party relationships from the input party id in the given
   * relationshipTypeId. For example input value of PARTY_FOLLOWER will return the list of all
   * <b>active<b/> party relationships between relationshipsFromPartyId in the role of
   * PARTY_FOLLOWER
   *
   * <p>Example usage: {@code List<Map> followers = (List<Map>)
   * ExtPartyRelationshipHelper.getActivePartyRelationshipsFromParty( dispatcher, partyIdTo,
   * PARTY_FOLLOWER_ROLE_ID); }
   *
   * @param dispatcher
   * @param relationshipsFromPartyId Party id of the Person/PartyGroup against which the
   *     relationship exists.
   * @param optionalRelationshipToPartyId Party id of the Person/PartyGroup that has the
   *     relationship with fromPartyId.
   * @param partyRelationshipTypeId Type id of the relationship. Between two parties (Example Lead
   *     owner between lead and owner)
   * @return Returns the list of map records (followers) containing follower's partyId, displayName,
   *     email, photoUrl, firstName, middleName, lastName, relationshipSince
   */
  public static List<Map> getActivePartyRelationshipsFromParty(
      LocalDispatcher dispatcher,
      String relationshipsFromPartyId,
      String optionalRelationshipToPartyId,
      String partyRelationshipTypeId,
      String partyTypeId) {
    List<Map> partyFollowers = new ArrayList<>();
    Map inputFieldsMap =
        UtilMisc.toMap(
            "noConditionFind",
            "Y",
            "fromDateName",
            "fromDate",
            "thruDateName",
            "thruDate",
            "filterByDateValue",
            UtilDateTime.nowTimestamp());

    if (UtilValidate.isNotEmpty(partyRelationshipTypeId)) {
      inputFieldsMap.put("partyRelationshipTypeId", partyRelationshipTypeId);
    }
    if (UtilValidate.isNotEmpty(relationshipsFromPartyId)) {
      inputFieldsMap.put("fromPartyId", relationshipsFromPartyId);
    }
    if (UtilValidate.isNotEmpty(optionalRelationshipToPartyId)) {
      inputFieldsMap.put("partyId", optionalRelationshipToPartyId);
    }
    if (UtilValidate.isNotEmpty(partyTypeId)) {
      inputFieldsMap.put("partyTypeId", partyTypeId);
    }
    Map performFindCtx =
        UtilMisc.toMap(
            "inputFields", inputFieldsMap, "entityName", "PartyRelationshipAndPartyDetail");

    performFindCtx.put("viewSize", 100000);

    Map<String, Object> searchInvitationsResult;
    try {
      searchInvitationsResult = dispatcher.runSync("performFindList", performFindCtx);
      List<GenericValue> listOfFollowers = (List) searchInvitationsResult.get("list");

      if (UtilValidate.isNotEmpty(listOfFollowers)) {
        // simplify the list
        for (GenericValue follower : listOfFollowers) {
          Map followerDetails =
              UtilMisc.toMap(
                  "partyId", follower.getString("fromPartyId"),
                  "relationshipSince", follower.getTimestamp("fromDate"),
                  "displayName", follower.getString("fromDisplayName"),
                  "email", follower.getString("fromEmail"),
                  "photoUrl", follower.getString("fromPhotoUrl"),
                  "firstName", follower.getString("fromFirstName"),
                  "middleName", follower.getString("fromMiddleName"),
                  "lastName", follower.getString("fromLastName"),
                  "roleTypeIdTo", follower.getString("roleTypeIdTo"),
                  "fromDate", follower.getTimestamp("fromDate"),
                  "partyIdFrom", follower.getString("partyIdFrom"),
                  "partyIdTo", follower.getString("partyId"),
                  "toDisplayName", follower.getString("toDisplayName"),
                  "toEmail", follower.getString("toEmail"),
                  "toPhotoUrl", follower.getString("toPhotoUrl"),
                  "fromGroupName", follower.getString("fromGroupName"),
                  "roleTypeIdFrom", follower.getString("roleTypeIdFrom"),
                  "partyRelationshipTypeId", follower.getString("partyRelationshipTypeId"));

          partyFollowers.add(followerDetails);
        }
      }

    } catch (GenericServiceException e) {
      Debug.logError(e, module);
    }

    return partyFollowers;
  }

  /**
   * Returns all active relationships that exists for a party with a relationship
   * (partyRelationshipTypeId).
   *
   * <p>Example use case would be to get all lead assignees <code>List<Map> assignees =
   * ExtPartyRelationshipHelper.getActivePartyRelationshipsFromParty(
   * dispatcher, leadPartyId, LeadRoleTypesEnum.LEAD_ASSIGNEE.getPartyRelationshipTypeId());</code>
   *
   * @param dispatcher
   * @param relationshipsFromPartyId
   * @param partyRelationshipTypeId
   * @return
   */
  public static List<Map> getActivePartyRelationshipsFromParty(
      LocalDispatcher dispatcher, String relationshipsFromPartyId, String partyRelationshipTypeId) {
    return getActivePartyRelationshipsFromParty(
        dispatcher, relationshipsFromPartyId, null, partyRelationshipTypeId, null);
  }

  public static List<Map> getActivePartyRelationshipsToParty(
      LocalDispatcher dispatcher, String relationshipsToPartyId, String partyRelationshipTypeId) {
    return getActivePartyRelationshipsFromParty(
        dispatcher, null, relationshipsToPartyId, partyRelationshipTypeId, null);
  }

  /**
   * Returns active relationships for a party of a given type. For example to get all lead owners
   * for a given lead party.
   *
   * <p>Example use case: Fetch owners for a lead <code>
   *   List<Map> leadRecords = ExtPartyRelationshipHelper.getActivePartyRelationshipsOfType(dispatcher,LeadRoleTypesEnum.LEAD_OWNER.getPartyRelationshipTypeId())
   * </code>
   *
   * @param dispatcher
   * @param partyRelationshipTypeId
   * @return
   */
  public static List<Map> getActivePartyRelationshipsOfType(
      LocalDispatcher dispatcher, String partyRelationshipTypeId) {
    return getActivePartyRelationshipsFromParty(dispatcher, null, null, partyRelationshipTypeId, null);
  }

  /**
   * Returns all active relationships for a party. For example to get all relationships for a lead (owner, follower, assignee).
   *
   * <p>Example use case: Fetch owners for a lead <code>
   *   List<Map> leadRelationshipts = ExtPartyRelationshipHelper.getActivePartyRelationshipsOfType(dispatcher, leadPartyId)
   * </code>
   *
   * @param dispatcher
   * @param partyId Party ID for the party whose relationship needs to be fetched.
   * @return
   */
  public static List<Map> getAllActivePartyRelationshipsForParty(
      LocalDispatcher dispatcher, String partyId) {
    return getActivePartyRelationshipsFromParty(dispatcher, partyId, null, null, null);
  }

  public static List<Map> getAllActivePartyRelationshipsForPerson(
      LocalDispatcher dispatcher, String partyId) {
    return getActivePartyRelationshipsFromParty(dispatcher, partyId, null, null, PartyTypesEnum.PERSON.getPartyTypeId());
  }
}
