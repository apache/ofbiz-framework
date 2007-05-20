/*******************************************************************************
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
 *******************************************************************************/

package org.ofbiz.party.party;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.sql.Timestamp;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Worker methods for Party Information
 */
public class PartyWorker {
    
    public static String module = PartyWorker.class.getName();
    
    public static Map getPartyOtherValues(ServletRequest request, String partyId, String partyAttr, String personAttr, String partyGroupAttr) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Map result = new HashMap();
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
    
    public static void getPartyOtherValues(PageContext pageContext, String partyId, String partyAttr, String personAttr, String partyGroupAttr) {
        Map partyMap = getPartyOtherValues(pageContext.getRequest(), partyId, partyAttr, personAttr, partyGroupAttr);
        Iterator i = partyMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            pageContext.setAttribute((String) e.getKey(), e.getValue());
            
        }      
    }

    /**
     * Generate a sequenced club id using the prefix passed and a sequence value + check digit
     * @param delegator used to obtain a sequenced value
     * @param prefix prefix inserted at the beginning of the ID
     * @param length total length of the ID including prefix and check digit
     * @return Sequenced Club ID string with a length as defined starting with the prefix defined
     */
    public static String createClubId(GenericDelegator delegator, String prefix, int length) {
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

    public static GenericValue findPartyLatestContactMech(String partyId, String contactMechTypeId, GenericDelegator delegator) {
        try {
            List cmList = delegator.findByAnd("PartyAndContactMech", UtilMisc.toMap("partyId", partyId, "contactMechTypeId", contactMechTypeId), UtilMisc.toList("-fromDate"));
            cmList = EntityUtil.filterByDate(cmList);
            return EntityUtil.getFirst(cmList);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest ContactMech for party with ID [" + partyId + "] TYPE [" + contactMechTypeId + "]: " + e.toString(), module);
            return null;
        }
    }

    public static GenericValue findPartyLatestPostalAddress(String partyId, GenericDelegator delegator) {
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

    public static GenericValue findPartyLatestTelecomNumber(String partyId, GenericDelegator delegator) {
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

    public static GenericValue findPartyLatestUserLogin(String partyId, GenericDelegator delegator) {
        try {
            List userLoginList = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId), UtilMisc.toList("-" + ModelEntity.STAMP_FIELD));
            return EntityUtil.getFirst(userLoginList);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while finding latest UserLogin for party with ID [" + partyId + "]: " + e.toString(), module);
            return null;
        }
    }

    public static Timestamp findPartyLastLoginTime(String partyId, GenericDelegator delegator) {
        try {
            List loginHistory = delegator.findByAnd("UserLoginHistory", UtilMisc.toMap("partyId", partyId), UtilMisc.toList("-fromDate"));
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

    public static Locale findPartyLastLocale(String partyId, GenericDelegator delegator) {
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

    public static String findFirstMatchingPartyId(GenericDelegator delegator, String address1, String address2, String city,
            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
            String firstName, String middleName, String lastName) throws GeneralException {

        String[] info = findFirstMatchingPartyAndContactMechId(delegator, address1, address2, city, stateProvinceGeoId, postalCode,
                postalCodeExt, countryGeoId, firstName, middleName, lastName);
        if (info != null) {
            return info[0];
        }
        return null;
    }

    public static String[] findFirstMatchingPartyAndContactMechId(GenericDelegator delegator, String address1, String address2, String city,
            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
            String firstName, String middleName, String lastName) throws GeneralException {

        List matching = findMatchingPartyAndPostalAddress(delegator, address1, address2, city, stateProvinceGeoId, postalCode,
            postalCodeExt, countryGeoId, firstName, middleName, lastName);
        GenericValue v = EntityUtil.getFirst(matching);
        if (v != null) {
            return new String[] { v.getString("partyId"), v.getString("contactMechId") };
        }
        return null;
    }

    public static List findMatchingPartyAndPostalAddress(GenericDelegator delegator, String address1, String address2, String city,
                            String stateProvinceGeoId, String postalCode, String postalCodeExt, String countryGeoId,
                            String firstName, String middleName, String lastName) throws GeneralException {

        // return list
        List returnList = FastList.newInstance();

        // address information
        if (firstName == null || lastName == null || address1 == null || city == null || postalCode == null) {
            throw new IllegalArgumentException();
        }

        List addrExprs = FastList.newInstance();
        if (stateProvinceGeoId != null) {
            if ("**".equals(stateProvinceGeoId)) {
                Debug.logWarning("Illegal state code passed!", module);
            } else if ("NA".equals(stateProvinceGeoId)) {
                addrExprs.add(new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, "_NA_"));
            } else {
                addrExprs.add(new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, stateProvinceGeoId.toUpperCase()));
            }
        }

        if (!postalCode.startsWith("*")) {
            if (postalCode.length() == 10 && postalCode.indexOf("-") != -1) {
                String[] zipSplit = postalCode.split("-", 2);
                postalCode = zipSplit[0];
                postalCodeExt = zipSplit[1];
            }
            addrExprs.add(new EntityExpr("postalCode", EntityOperator.EQUALS, postalCode));
        }

        if (postalCodeExt != null) {
            addrExprs.add(new EntityExpr("postalCodeExt", EntityOperator.EQUALS, postalCodeExt));
        }

        city = city.replaceAll("'", "\\\\'");
        addrExprs.add(new EntityExpr("city", true, EntityOperator.EQUALS, city, true));

        if (countryGeoId != null) {
            addrExprs.add(new EntityExpr("countryGeoId", EntityOperator.EQUALS, countryGeoId.toUpperCase()));
        }

        // limit to only non-disabled status
        addrExprs.add(new EntityExpr(new EntityExpr("statusId", EntityOperator.EQUALS, null),
                EntityOperator.OR, new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));

        List sort = UtilMisc.toList("-fromDate");
        EntityCondition addrCond = new EntityConditionList(addrExprs, EntityOperator.AND);
        List addresses = EntityUtil.filterByDate(delegator.findByCondition("PartyAndPostalAddress", addrCond, null, sort));
        //Debug.log("Checking for matching address: " + addrCond.toString() + "[" + addresses.size() + "]", module);

        List validFound = FastList.newInstance();
        if (UtilValidate.isNotEmpty(addresses)) {
            // check the address line
            Iterator v = addresses.iterator();
            while (v.hasNext()) {
                GenericValue address = (GenericValue) v.next();

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

            if (validFound != null && validFound.size() > 0) {
                Iterator a = validFound.iterator();
                while (a.hasNext()) {
                    GenericValue partyAndAddr = (GenericValue) a.next();
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

    public static String makeMatchingString(GenericDelegator delegator, String address) {
        if (address == null) {
            return null;
        }

        // upper case the address
        String str = address.trim().toUpperCase();

        // replace mapped words
        List addressMap = null;
        try {
            addressMap = delegator.findAll("AddressMatchMap", UtilMisc.toList("sequenceNum"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (addressMap != null) {
            Iterator i = addressMap.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                str = str.replaceAll(v.getString("mapKey").toUpperCase(), v.getString("mapValue").toUpperCase());
            }
        }

        // remove all non-word characters
        return str.replaceAll("\\W", "");
    }

}
