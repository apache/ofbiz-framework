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

package org.apache.ofbiz.party.party;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

/**
 * Worker methods for Party Information
 */
public class PartyWorker {

    public static String module = PartyWorker.class.getName();

    private PartyWorker() {}

    public static Map<String, GenericValue> getPartyOtherValues(ServletRequest request, String partyId, String partyAttr, String personAttr, String partyGroupAttr) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, GenericValue> result = new HashMap<String, GenericValue>();
        try {
            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();

            if (party != null)
                result.put(partyAttr, party);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting Party entity", module);
        }

        try {
            GenericValue person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();

            if (person != null)
                result.put(personAttr, person);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting Person entity", module);
        }

        try {
            GenericValue partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();

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
        clubId = clubId + UtilFormatOut.formatPaddedNumber(seq.longValue(), (length - clubId.length() - 1));

        // get the check digit
        int check = UtilValidate.getLuhnCheckDigit(clubId);
        clubId = clubId + Integer.toString(check);

        return clubId;
    }

    public static GenericValue findPartyLatestContactMech(String partyId, String contactMechTypeId, Delegator delegator) {
        try {
            return EntityQuery.use(delegator).from("PartyAndContactMech")
                    .where("partyId", partyId, "contactMechTypeId", contactMechTypeId)
                    .orderBy("-fromDate")
                    .filterByDate()
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest ContactMech for party with ID [" + partyId + "] TYPE [" + contactMechTypeId + "]: " + e.toString(), module);
            return null;
        }
    }

    public static GenericValue findPartyLatestPostalAddress(String partyId, Delegator delegator) {
        GenericValue pcm = findPartyLatestContactMech(partyId, "POSTAL_ADDRESS", delegator);
        if (pcm != null) {
            try {
                return pcm.getRelatedOne("PostalAddress", false);
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
                GenericValue latestGeoPoint =  latestPostalAddress.getRelatedOne("GeoPoint", false);
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
                return pcm.getRelatedOne("TelecomNumber", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error while finding latest TelecomNumber for party with ID [" + partyId + "]: " + e.toString(), module);
            }
        }
        return null;
    }

    public static GenericValue findPartyLatestUserLogin(String partyId, Delegator delegator) {
        try {
            return EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).orderBy("-" + ModelEntity.STAMP_FIELD).queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest UserLogin for party with ID [" + partyId + "]: " + e.toString(), module);
            return null;
        }
    }

    public static Timestamp findPartyLastLoginTime(String partyId, Delegator delegator) {
        try {
            GenericValue v = EntityQuery.use(delegator).from("UserLoginHistory").where("partyId", partyId).orderBy("-fromDate").queryFirst();
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

        List<GenericValue> matching = findMatchingPersonPostalAddresses(delegator, address1, address2, city, stateProvinceGeoId, postalCode,
            postalCodeExt, countryGeoId, firstName, middleName, lastName);
        GenericValue v = EntityUtil.getFirst(matching);
        if (v != null) {
            return new String[] { v.getString("partyId"), v.getString("contactMechId") };
        }
        return null;
    }

    /** Finds all matching PartyAndPostalAddress records based on the values provided.  Excludes party records with a statusId of PARTY_DISABLED.  Results are ordered by descending PartyContactMech.fromDate.
     * The matching process is as follows:
     * 1. Calls {@link #findMatchingPartyPostalAddress(Delegator, String, String, String, String, String, String, String, String)} to retrieve a list of address matched PartyAndPostalAddress records.  Results are limited to Parties of type PERSON.
     * 2. For each matching PartyAndPostalAddress record, the Person record for the Party is then retrieved and an upper case comparison is performed against the supplied firstName, lastName and if provided, middleName.
     * 
     * @param delegator             Delegator instance
     * @param address1              PostalAddress.address1 to match against (Required).
     * @param address2              Optional PostalAddress.address2 to match against.
     * @param city                  PostalAddress.city value to match against (Required).
     * @param stateProvinceGeoId    Optional PostalAddress.stateProvinceGeoId value to match against.  If null or "**" is passed then the value will be ignored during matching.  "NA" can be passed in place of "_NA_".
     * @param postalCode            PostalAddress.postalCode value to match against.  Cannot be null but can be skipped by passing a value starting with an "*".  If the length of the supplied string is 10 characters and the string contains a "-" then the postal code will be split at the "-" and the second half will be used as the postalCodeExt.
     * @param postalCodeExt         Optional PostalAddress.postalCodeExt value to match against.  Will be overridden if a postalCodeExt value is retrieved from postalCode as described above.
     * @param countryGeoId          Optional PostalAddress.countryGeoId value to match against.
     * @param firstName             Person.firstName to match against (Required).
     * @param middleName            Optional Person.middleName to match against.
     * @param lastName              Person.lastName to match against (Required).
     * @return List of PartyAndPostalAddress GenericValue objects that match the supplied criteria.
     * @throws GeneralException
     */
    public static List<GenericValue> findMatchingPersonPostalAddresses(Delegator delegator, String address1, String address2, String city,
            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
            String firstName, String middleName, String lastName) throws GeneralException {
        // return list
        List<GenericValue> returnList = new LinkedList<GenericValue>();

        // address information
        if (firstName == null || lastName == null) {
            throw new IllegalArgumentException();
        }

        List<GenericValue> validFound = findMatchingPartyPostalAddress(delegator, address1, address2, city, stateProvinceGeoId, postalCode, postalCodeExt, countryGeoId, "PERSON");

        if (UtilValidate.isNotEmpty(validFound)) {
            for (GenericValue partyAndAddr: validFound) {
                String partyId = partyAndAddr.getString("partyId");
                if (UtilValidate.isNotEmpty(partyId)) {
                    GenericValue p = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
                    if (p != null) {
                        String fName = p.getString("firstName");
                        String lName = p.getString("lastName");
                        String mName = p.getString("middleName");
                        if (lName.toUpperCase().equals(lastName.toUpperCase())) {
                            if (fName.toUpperCase().equals(firstName.toUpperCase())) {
                                if (mName != null && middleName != null) {
                                    if (mName.toUpperCase().equals(middleName.toUpperCase())) {
                                        returnList.add(partyAndAddr);
                                    }
                                } else if (middleName == null) {
                                    returnList.add(partyAndAddr);
                                }
                            }
                        }
                    }
                }
            }
        }

        return returnList;
    }

    /**
     * @deprecated Renamed to {@link #findMatchingPersonPostalAddresses(Delegator, String, String, String, String, String, String, String, String, String, String)}
     */
    @Deprecated
    public static List<GenericValue> findMatchingPartyAndPostalAddress(Delegator delegator, String address1, String address2, String city,
                            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
                            String firstName, String middleName, String lastName) throws GeneralException {
        return PartyWorker.findMatchingPersonPostalAddresses(delegator, address1, address2, city, stateProvinceGeoId, postalCode, postalCodeExt, countryGeoId, firstName, middleName, lastName);
    }

    /**
     * Finds all matching parties based on the values provided.  Excludes party records with a statusId of PARTY_DISABLED.  Results are ordered by descending PartyContactMech.fromDate.
     * 1. Candidate addresses are found by querying PartyAndPostalAddress using the supplied city and if provided, stateProvinceGeoId, postalCode, postalCodeExt and countryGeoId
     * 2. In-memory address line comparisons are then performed against the supplied address1 and if provided, address2.  Address lines are compared after the strings have been converted using {@link #makeMatchingString(Delegator, String)}.
     * 
     * @param delegator             Delegator instance
     * @param address1              PostalAddress.address1 to match against (Required).
     * @param address2              Optional PostalAddress.address2 to match against.
     * @param city                  PostalAddress.city value to match against (Required).
     * @param stateProvinceGeoId    Optional PostalAddress.stateProvinceGeoId value to match against.  If null or "**" is passed then the value will be ignored during matching.  "NA" can be passed in place of "_NA_".
     * @param postalCode            PostalAddress.postalCode value to match against.  Cannot be null but can be skipped by passing a value starting with an "*".  If the length of the supplied string is 10 characters and the string contains a "-" then the postal code will be split at the "-" and the second half will be used as the postalCodeExt.
     * @param postalCodeExt         Optional PostalAddress.postalCodeExt value to match against.  Will be overridden if a postalCodeExt value is retrieved from postalCode as described above.
     * @param countryGeoId          Optional PostalAddress.countryGeoId value to match against.
     * @param partyTypeId           Optional Party.partyTypeId to match against.
     * @return List of PartyAndPostalAddress GenericValue objects that match the supplied criteria.
     * @throws GenericEntityException
     */
    public static List<GenericValue> findMatchingPartyPostalAddress(Delegator delegator, String address1, String address2, String city, 
                            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId, String partyTypeId) throws GenericEntityException {

        if (address1 == null || city == null || postalCode == null) {
            throw new IllegalArgumentException();
        }

        List<EntityCondition> addrExprs = new LinkedList<EntityCondition>();
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

        addrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("city"), EntityOperator.EQUALS, EntityFunction.UPPER(city)));

        if (countryGeoId != null) {
            addrExprs.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.EQUALS, countryGeoId.toUpperCase()));
        }

        // limit to only non-disabled status
        addrExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null),
                EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));

        if (partyTypeId != null) {
            addrExprs.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, partyTypeId));
        }

        List<GenericValue> addresses = EntityQuery.use(delegator).from("PartyAndPostalAddress")
                .where(EntityCondition.makeCondition(addrExprs, EntityOperator.AND))
                .orderBy("-fromDate")
                .filterByDate()
                .queryList();

        if (UtilValidate.isEmpty(addresses)) {
            // No address matches, return an empty list
            return addresses;
        }

        List<GenericValue> validFound = new LinkedList<GenericValue>();
        // check the address line
        for (GenericValue address: addresses) {
            // address 1 field
            String addr1Source = PartyWorker.makeMatchingString(delegator, address1);
            String addr1Target = PartyWorker.makeMatchingString(delegator, address.getString("address1"));

            if (addr1Target != null) {
                Debug.logInfo("Comparing address1 : " + addr1Source + " / " + addr1Target, module);
                if (addr1Target.equals(addr1Source)) {

                    // address 2 field
                    if (address2 != null) {
                        String addr2Source = PartyWorker.makeMatchingString(delegator, address2);
                        String addr2Target = PartyWorker.makeMatchingString(delegator, address.getString("address2"));
                        if (addr2Target != null) {
                            Debug.logInfo("Comparing address2 : " + addr2Source + " / " + addr2Target, module);

                            if (addr2Source.equals(addr2Target)) {
                                Debug.logInfo("Matching address2; adding valid address", module);
                                validFound.add(address);
                            }
                        }
                    } else {
                        if (address.get("address2") == null) {
                            Debug.logInfo("No address2; adding valid address", module);
                            validFound.add(address);
                        }
                    }
                }
            }
        }
        return validFound;
    }

    /**
     * Converts the supplied String into a String suitable for address line matching.
     * Performs the following transformations on the supplied String:
     * - Converts to upper case
     * - Retrieves all records from the AddressMatchMap table and replaces all occurrences of addressMatchMap.mapKey with addressMatchMap.mapValue using upper case matching.
     * - Removes all non-word characters from the String i.e. everything except A-Z, 0-9 and _
     * @param delegator     A Delegator instance
     * @param address       The address String to convert
     * @return              The converted Address
     */
    public static String makeMatchingString(Delegator delegator, String address) {
        if (address == null) {
            return null;
        }

        // upper case the address
        String str = address.trim().toUpperCase();

        // replace mapped words
        List<GenericValue> addressMap = null;
        try {
            addressMap = EntityQuery.use(delegator).from("AddressMatchMap").orderBy("sequenceNum").queryList();
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
        List<GenericValue> partyList = new LinkedList<GenericValue>();
        List<String> partyIds = null;
        try {
            EntityConditionList<EntityExpr> baseExprs = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("partyIdFrom", partyIdFrom),
                    EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId)), EntityOperator.AND);
            List<GenericValue> associatedParties = EntityQuery.use(delegator).from("PartyRelationship").where(baseExprs).cache(true).queryList();
            partyList.addAll(associatedParties);
            while (UtilValidate.isNotEmpty(associatedParties)) {
                List<GenericValue> currentAssociatedParties = new LinkedList<GenericValue>();
                for (GenericValue associatedParty : associatedParties) {
                    EntityConditionList<EntityExpr> innerExprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("partyIdFrom", associatedParty.get("partyIdTo")),
                            EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId)), EntityOperator.AND);
                    List<GenericValue> associatedPartiesChilds = EntityQuery.use(delegator).from("PartyRelationship").where(innerExprs).cache(true).queryList();
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
     * @param delegator the delegator
     * @param idToFind the party id to find
     * @param partyIdentificationTypeId the party identification type id to use
     * @param searchPartyFirst search first with party id
     * @param searchAllId search all the party ids
     * @return returns the parties founds
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
            party = EntityQuery.use(delegator).from("Party").where("partyId", idToFind).cache().queryOne();
        }

        if (searchAllId || (searchPartyFirst && UtilValidate.isEmpty(party))) {
            // 2) Retrieve party in PartyIdentification
            Map<String, String> conditions = UtilMisc.toMap("idValue", idToFind);
            if (UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
                conditions.put("partyIdentificationTypeId", partyIdentificationTypeId);
            }
            partiesFound = EntityQuery.use(delegator).from("PartyIdentificationAndParty").where(conditions).orderBy("partyId").cache(true).queryList();
        }

        if (! searchPartyFirst) {
            party = EntityQuery.use(delegator).from("Party").where("partyId", idToFind).cache().queryOne();
        }

        if (party != null) {
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
        if (party != null) {
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
                    partyToAdd = EntityQuery.use(delegator).from("Party").where("partyId", party.get("partyId")).cache().queryOne();
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
