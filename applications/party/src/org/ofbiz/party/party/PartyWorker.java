/*
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
 */

package org.ofbiz.party.party;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Worker methods for Party Information
 */
public class PartyWorker {

    public static String module = PartyWorker.class.getName();

    public static Map<String, GenericValue> getPartyOtherValues(ServletRequest request, String partyId, String partyAttr, String personAttr, String partyGroupAttr) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, GenericValue> result = FastMap.newInstance();
        try {
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

            if (party != null)
                result.put(partyAttr, party);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting Party entity", module);
        }

        try {
            GenericValue person = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", partyId));

            if (person != null)
                result.put(personAttr, person);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting Person entity", module);
        }

        try {
            GenericValue partyGroup = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", partyId));

            if (partyGroup != null)
                result.put(partyGroupAttr, partyGroup);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting PartyGroup entity", module);
        }
        return result;
    }

    /**
     * Generate a sequenced club id using the prefix passed and a sequence value + check digit
     * @param delegator used to obtain a sequenced value
     * @param prefix prefix inserted at the beginning of the ID
     * @param length total length of the ID including prefix and check digit
     * @return Sequenced Club ID string with a length as defined starting with the prefix defined
     */
    public static String createClubId(Delegator delegator, String prefix, int length) {
        final String clubSeqName = "PartyClubSeq";
        String clubId = prefix != null ? prefix : "";

        // generate the sequenced number and pad
        Long seq = delegator.getNextSeqIdLong(clubSeqName);
        clubId = clubId + UtilFormatOut.formatPaddedNumber(seq.longValue(), (length - prefix.length() - 1));

        // get the check digit
        int check = UtilValidate.getLuhnCheckDigit(clubId);
        clubId = clubId + Integer.toString(check);

        return clubId;
    }

    public static GenericValue findPartyLatestContactMech(String partyId, String contactMechTypeId, Delegator delegator) {
        try {
            List<GenericValue> cmList = delegator.findByAnd("PartyAndContactMech", UtilMisc.toMap("partyId", partyId, "contactMechTypeId", contactMechTypeId), UtilMisc.toList("-fromDate"));
            cmList = EntityUtil.filterByDate(cmList);
            return EntityUtil.getFirst(cmList);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest ContactMech for party with ID [" + partyId + "] TYPE [" + contactMechTypeId + "]: " + e.toString(), module);
            return null;
        }
    }

    public static GenericValue findPartyLatestPostalAddress(String partyId, Delegator delegator) {
        GenericValue pcm = findPartyLatestContactMech(partyId, "POSTAL_ADDRESS", delegator);
        if (pcm != null) {
            try {
                return pcm.getRelatedOne("PostalAddress");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest PostalAddress for party with ID [" + partyId + "]: " + e.toString(), module);
            }
        }
        return null;
    }

    public static GenericValue findPartyLatestPostalAddressGeoPoint(String partyId, Delegator delegator) {
        GenericValue latestPostalAddress = findPartyLatestPostalAddress(partyId, delegator);
        if (latestPostalAddress  != null) {
            try {
                GenericValue latestGeoPoint =  latestPostalAddress.getRelatedOne("GeoPoint");
                if (latestGeoPoint  != null) {
                    return latestGeoPoint;
                }
                return null;
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest GeoPoint for party with ID [" + partyId + "]: " + e.toString(), module);
            }
        }
        return null;
    }

    public static GenericValue findPartyLatestTelecomNumber(String partyId, Delegator delegator) {
        GenericValue pcm = findPartyLatestContactMech(partyId, "TELECOM_NUMBER", delegator);
        if (pcm != null) {
            try {
                return pcm.getRelatedOne("TelecomNumber");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest TelecomNumber for party with ID [" + partyId + "]: " + e.toString(), module);
            }
        }
        return null;
    }

    public static GenericValue findPartyLatestUserLogin(String partyId, Delegator delegator) {
        try {
            List<GenericValue> userLoginList = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId), UtilMisc.toList("-" + ModelEntity.STAMP_FIELD));
            return EntityUtil.getFirst(userLoginList);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest UserLogin for party with ID [" + partyId + "]: " + e.toString(), module);
            return null;
        }
    }

    public static Timestamp findPartyLastLoginTime(String partyId, Delegator delegator) {
        try {
            List<GenericValue> loginHistory = delegator.findByAnd("UserLoginHistory", UtilMisc.toMap("partyId", partyId), UtilMisc.toList("-fromDate"));
            GenericValue v = EntityUtil.getFirst(loginHistory);
            if (v != null) {
                return v.getTimestamp("fromDate");
            } else {
                return null;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest login time for party with ID [" + partyId + "]: " + e.toString(), module);
            return null;
        }

    }

    public static Locale findPartyLastLocale(String partyId, Delegator delegator) {
        // just get the most recent UserLogin for this party, if there is one...
        GenericValue userLogin = findPartyLatestUserLogin(partyId, delegator);
        if (userLogin == null) {
            return null;
        }
        String localeString = userLogin.getString("lastLocale");
        if (UtilValidate.isNotEmpty(localeString)) {
            return UtilMisc.parseLocale(localeString);
        } else {
            return null;
        }
    }

    public static String findFirstMatchingPartyId(Delegator delegator, String address1, String address2, String city,
            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
            String firstName, String middleName, String lastName) throws GeneralException {

        String[] info = findFirstMatchingPartyAndContactMechId(delegator, address1, address2, city, stateProvinceGeoId, postalCode,
                postalCodeExt, countryGeoId, firstName, middleName, lastName);
        if (info != null) {
            return info[0];
        }
        return null;
    }

    public static String[] findFirstMatchingPartyAndContactMechId(Delegator delegator, String address1, String address2, String city,
            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
            String firstName, String middleName, String lastName) throws GeneralException {

        List<GenericValue> matching = findMatchingPartyAndPostalAddress(delegator, address1, address2, city, stateProvinceGeoId, postalCode,
            postalCodeExt, countryGeoId, firstName, middleName, lastName);
        GenericValue v = EntityUtil.getFirst(matching);
        if (v != null) {
            return new String[] { v.getString("partyId"), v.getString("contactMechId") };
        }
        return null;
    }

    public static List<GenericValue> findMatchingPartyAndPostalAddress(Delegator delegator, String address1, String address2, String city,
                            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
                            String firstName, String middleName, String lastName) throws GeneralException {

        // return list
        List<GenericValue> returnList = FastList.newInstance();

        // address information
        if (firstName == null || lastName == null || address1 == null || city == null || postalCode == null) {
            throw new IllegalArgumentException();
        }

        List<EntityCondition> addrExprs = FastList.newInstance();
        if (stateProvinceGeoId != null) {
            if ("**".equals(stateProvinceGeoId)) {
                Debug.logWarning("Illegal state code passed!", module);
            } else if ("NA".equals(stateProvinceGeoId)) {
                addrExprs.add(EntityCondition.makeCondition("stateProvinceGeoId", EntityOperator.EQUALS, "_NA_"));
            } else {
                addrExprs.add(EntityCondition.makeCondition("stateProvinceGeoId", EntityOperator.EQUALS, stateProvinceGeoId.toUpperCase()));
            }
        }

        if (!postalCode.startsWith("*")) {
            if (postalCode.length() == 10 && postalCode.indexOf("-") != -1) {
                String[] zipSplit = postalCode.split("-", 2);
                postalCode = zipSplit[0];
                postalCodeExt = zipSplit[1];
            }
            addrExprs.add(EntityCondition.makeCondition("postalCode", EntityOperator.EQUALS, postalCode));
        }

        if (postalCodeExt != null) {
            addrExprs.add(EntityCondition.makeCondition("postalCodeExt", EntityOperator.EQUALS, postalCodeExt));
        }

        city = city.replaceAll("'", "\\\\'");
        addrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("city"), EntityOperator.EQUALS, EntityFunction.UPPER(city)));

        if (countryGeoId != null) {
            addrExprs.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.EQUALS, countryGeoId.toUpperCase()));
        }

        // limit to only non-disabled status
        addrExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null),
                EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));

        List<String> sort = UtilMisc.toList("-fromDate");
        EntityCondition addrCond = EntityCondition.makeCondition(addrExprs, EntityOperator.AND);
        List<GenericValue> addresses = EntityUtil.filterByDate(delegator.findList("PartyAndPostalAddress", addrCond, null, sort, null, false));
        //Debug.log("Checking for matching address: " + addrCond.toString() + "[" + addresses.size() + "]", module);

        List<GenericValue> validFound = FastList.newInstance();
        if (UtilValidate.isNotEmpty(addresses)) {
            // check the address line
            for (GenericValue address: addresses) {
                // address 1 field
                String addr1Source = PartyWorker.makeMatchingString(delegator, address1);
                String addr1Target = PartyWorker.makeMatchingString(delegator, address.getString("address1"));

                if (addr1Target != null) {
                    Debug.log("Comparing address1 : " + addr1Source + " / " + addr1Target, module);
                    if (addr1Target.equals(addr1Source)) {

                        // address 2 field
                        if (address2 != null) {
                            String addr2Source = PartyWorker.makeMatchingString(delegator, address2);
                            String addr2Target = PartyWorker.makeMatchingString(delegator, address.getString("address2"));
                            if (addr2Target != null) {
                                Debug.log("Comparing address2 : " + addr2Source + " / " + addr2Target, module);

                                if (addr2Source.equals(addr2Target)) {
                                    Debug.log("Matching address2; adding valid address", module);
                                    validFound.add(address);
                                    //validParty.put(address.getString("partyId"), address.getString("contactMechId"));
                                }
                            }
                        } else {
                            if (address.get("address2") == null) {
                                Debug.log("No address2; adding valid address", module);
                                validFound.add(address);
                                //validParty.put(address.getString("partyId"), address.getString("contactMechId"));
                            }
                        }
                    }
                }
            }

            if (UtilValidate.isNotEmpty(validFound)) {
                for (GenericValue partyAndAddr: validFound) {
                    String partyId = partyAndAddr.getString("partyId");
                    String cmId = partyAndAddr.getString("contactMechId");
                    if (UtilValidate.isNotEmpty(partyId)) {
                        GenericValue p = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", partyId));
                        if (p != null) {
                            String fName = p.getString("firstName");
                            String lName = p.getString("lastName");
                            String mName = p.getString("middleName");
                            if (lName.toUpperCase().equals(lastName.toUpperCase())) {
                                if (fName.toUpperCase().equals(firstName.toUpperCase())) {
                                    if (mName != null && middleName != null) {
                                        if (mName.toUpperCase().equals(middleName.toUpperCase())) {
                                            returnList.add(partyAndAddr);
                                            //return new String[] { partyId, cmId };
                                        }
                                    } else if (middleName == null) {
                                        returnList.add(partyAndAddr);
                                        //return new String[] { partyId, cmId };
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return returnList;
    }

    public static String makeMatchingString(Delegator delegator, String address) {
        if (address == null) {
            return null;
        }

        // upper case the address
        String str = address.trim().toUpperCase();

        // replace mapped words
        List<GenericValue> addressMap = null;
        try {
            addressMap = delegator.findList("AddressMatchMap", null, null, UtilMisc.toList("sequenceNum"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (addressMap != null) {
            for (GenericValue v: addressMap) {
                str = str.replaceAll(v.getString("mapKey").toUpperCase(), v.getString("mapValue").toUpperCase());
            }
        }

        // remove all non-word characters
        return str.replaceAll("\\W", "");
    }

    public static List<String> getAssociatedPartyIdsByRelationshipType(Delegator delegator, String partyIdFrom, String partyRelationshipTypeId) {
        List<GenericValue> partyList = FastList.newInstance();
        List<String> partyIds = null;
        try {
            EntityConditionList baseExprs = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("partyIdFrom", partyIdFrom),
                    EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId)), EntityOperator.AND);
            List<GenericValue> associatedParties = delegator.findList("PartyRelationship", baseExprs, null, null, null, true);
            partyList.addAll(associatedParties);
            while (UtilValidate.isNotEmpty(associatedParties)) {
                List<GenericValue> currentAssociatedParties = FastList.newInstance();
                for (GenericValue associatedParty : associatedParties) {
                    EntityConditionList innerExprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("partyIdFrom", associatedParty.get("partyIdTo")),
                            EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId)), EntityOperator.AND);
                    List<GenericValue> associatedPartiesChilds = delegator.findList("PartyRelationship", innerExprs, null, null, null, true);
                    if (UtilValidate.isNotEmpty(associatedPartiesChilds)) {
                        currentAssociatedParties.addAll(associatedPartiesChilds);
                    }
                    partyList.add(associatedParty);
                }
                associatedParties  = currentAssociatedParties;
            }
            partyIds = EntityUtil.getFieldListFromEntityList(partyList, "partyIdTo", true);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return partyIds;
    }
    
    /**
     * Generic service to find party by id.
     * By default return the party find by partyId
     * but you can pass searchPartyFirst at false if you want search in partyIdentification before
     * or pass searchAllId at true to find apartyuct with this id (party.partyId and partyIdentification.idValue)
     * @param delegator
     * @param idToFind
     * @param partyIdentificationTypeId
     * @param searchPartyFirst
     * @param searchAllId
     * @return
     * @throws GenericEntityException
     */
    public static List<GenericValue> findPartiesById(Delegator delegator,
            String idToFind, String partyIdentificationTypeId,
            boolean searchPartyFirst, boolean searchAllId) throws GenericEntityException {

        if (Debug.verboseOn()) Debug.logVerbose("Analyze partyIdentification: entered id = " + idToFind + ", partyIdentificationTypeId = " + partyIdentificationTypeId, module);

        GenericValue party = null;
        List<GenericValue> partiesFound = null;

        // 1) look if the idToFind given is a real partyId
        if (searchPartyFirst) {
            party = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", idToFind));
        }

        if (searchAllId || (searchPartyFirst && UtilValidate.isEmpty(party))) {
            // 2) Retrieve party in PartyIdentification
            Map<String, String> conditions = UtilMisc.toMap("idValue", idToFind);
            if (UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
                conditions.put("partyIdentificationTypeId", partyIdentificationTypeId);
            }
            partiesFound = delegator.findByAndCache("PartyIdentificationAndParty", conditions, UtilMisc.toList("partyId"));
        }

        if (! searchPartyFirst) {
            party = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", idToFind));
        }

        if (UtilValidate.isNotEmpty(party)) {
            if (UtilValidate.isNotEmpty(partiesFound)) partiesFound.add(party);
            else partiesFound = UtilMisc.toList(party);
        }
        if (Debug.verboseOn()) Debug.logVerbose("Analyze partyIdentification: found party.partyId = " + party + ", and list : " + partiesFound, module);
        return partiesFound;
    }

    public static List<GenericValue> findPartiesById(Delegator delegator, String idToFind, String partyIdentificationTypeId)
    throws GenericEntityException {
        return findPartiesById(delegator, idToFind, partyIdentificationTypeId, true, false);
    }

    public static String findPartyId(Delegator delegator, String idToFind, String partyIdentificationTypeId) throws GenericEntityException {
        GenericValue party = findParty(delegator, idToFind, partyIdentificationTypeId);
        if (UtilValidate.isNotEmpty(party)) {
            return party.getString("partyId");
        } else {
            return null;
        }
    }

    public static String findPartyId(Delegator delegator, String idToFind) throws GenericEntityException {
        return findPartyId(delegator, idToFind, null);
    }

    public static GenericValue findParty(Delegator delegator, String idToFind, String partyIdentificationTypeId) throws GenericEntityException {
        List<GenericValue> parties = findPartiesById(delegator, idToFind, partyIdentificationTypeId);
        GenericValue party = EntityUtil.getFirst(parties);
        return party;
    }

    public static List<GenericValue> findParties(Delegator delegator, String idToFind, String partyIdentificationTypeId) throws GenericEntityException {
        List<GenericValue> partiesByIds = findPartiesById(delegator, idToFind, partyIdentificationTypeId);
        List<GenericValue> parties = null;
        if (UtilValidate.isNotEmpty(partiesByIds)) {
            for (GenericValue party : partiesByIds) {
                GenericValue partyToAdd = party;
                //retreive party GV if the actual genericValue came from viewEntity
                if (! "Party".equals(party.getEntityName())) {
                    partyToAdd = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", party.get("partyId")));
                }

                if (UtilValidate.isEmpty(parties)) {
                    parties = UtilMisc.toList(partyToAdd);
                }
                else {
                    parties.add(partyToAdd);
                }
            }
        }
        return parties;
    }

    public static List<GenericValue> findParties(Delegator delegator, String idToFind) throws GenericEntityException {
        return findParties(delegator, idToFind, null);
    }

    public static GenericValue findParty(Delegator delegator, String idToFind) throws GenericEntityException {
        return findParty(delegator, idToFind, null);
    }

}
