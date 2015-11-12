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

package org.ofbiz.sfa.vcard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import net.wimpi.pim.Pim;
import net.wimpi.pim.contact.basicimpl.AddressImpl;
import net.wimpi.pim.contact.basicimpl.EmailAddressImpl;
import net.wimpi.pim.contact.basicimpl.PhoneNumberImpl;
import net.wimpi.pim.contact.io.ContactMarshaller;
import net.wimpi.pim.contact.io.ContactUnmarshaller;
import net.wimpi.pim.contact.model.Address;
import net.wimpi.pim.contact.model.Communications;
import net.wimpi.pim.contact.model.Contact;
import net.wimpi.pim.contact.model.EmailAddress;
import net.wimpi.pim.contact.model.Organization;
import net.wimpi.pim.contact.model.OrganizationalIdentity;
import net.wimpi.pim.contact.model.PersonalIdentity;
import net.wimpi.pim.contact.model.PhoneNumber;
import net.wimpi.pim.factory.ContactIOFactory;
import net.wimpi.pim.factory.ContactModelFactory;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class VCard {
    public static final String module = VCard.class.getName();
    public static final String resourceError = "MarketingUiLabels";

    public static Map<String, Object> importVCard(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Address workAddress = null;
        String email = null;
        String phone = null;
        ByteBuffer byteBuffer = (ByteBuffer) context.get("infile");
        byte[] inputByteArray = byteBuffer.array();
        InputStream in = new ByteArrayInputStream(inputByteArray);
        String partyType = (String) context.get("partyType");
        Boolean isGroup =  "PartyGroup".equals(partyType); // By default we import a Person.
        Map<String, Object> serviceCtx = new HashMap<String, Object>();

        try {
            ContactIOFactory ciof = Pim.getContactIOFactory();
            ContactUnmarshaller unmarshaller = ciof.createContactUnmarshaller();
            Contact[] contacts = unmarshaller.unmarshallContacts(in);

            for (Contact contact: contacts) {
                PersonalIdentity pid = contact.getPersonalIdentity();
                if (!isGroup) {
                    serviceCtx.put("firstName", pid.getFirstname());
                    serviceCtx.put("lastName", pid.getLastname());
                }
                for (Iterator<?> iter = contact.getAddresses(); iter.hasNext();) {
                    Address address = (AddressImpl) iter.next();
                    if (contact.isPreferredAddress(address)) {
                        workAddress = address;
                        break;
                    } else if (address.isWork()) {
                        workAddress = address;
                        break;
                    } else { // for now use preferred/work address only
                        continue;
                    }
                }
                if (UtilValidate.isNotEmpty(workAddress)) {
                    serviceCtx.put("address1", workAddress.getStreet());
                    serviceCtx.put("city", workAddress.getCity());
                    serviceCtx.put("postalCode", workAddress.getPostalCode());

                    GenericValue countryGeo = EntityQuery.use(delegator).from("Geo")
                            .where(EntityCondition.makeCondition("geoTypeId", EntityOperator.EQUALS, "COUNTRY"),
                                    EntityCondition.makeCondition("geoName", EntityOperator.LIKE, workAddress.getCountry()))
                            .cache().queryFirst();
                    if (countryGeo != null) {
                        serviceCtx.put("countryGeoId", countryGeo.get("geoId"));
                    }

                    GenericValue stateGeo = EntityQuery.use(delegator).from("Geo")
                            .where(EntityCondition.makeCondition("geoTypeId", EntityOperator.EQUALS, "STATE"),
                            EntityCondition.makeCondition("geoName", EntityOperator.LIKE, workAddress.getRegion()))
                            .cache().queryFirst();
                    if (stateGeo != null) {
                        serviceCtx.put("stateProvinceGeoId", stateGeo.get("geoId"));
                    }
                }

                if (!isGroup) {
                    Communications communications = contact.getCommunications();
                    if (UtilValidate.isNotEmpty(communications)) {
                        for (Iterator<?> iter = communications.getEmailAddresses(); iter.hasNext();) {
                            EmailAddress emailAddress = (EmailAddressImpl) iter.next();
                            if (communications.isPreferredEmailAddress(emailAddress)) {
                                email = emailAddress.getAddress();
                                break;
                            } else {
                                email = emailAddress.getAddress();
                                break;
                            }
                        }
                        if (UtilValidate.isNotEmpty(email)) {
                            serviceCtx.put("emailAddress", email);
                        }
                        for (Iterator<?> iter = communications.getPhoneNumbers(); iter.hasNext();) {
                            PhoneNumber phoneNumber = (PhoneNumberImpl) iter.next();
                            if (phoneNumber.isPreferred()) {
                                phone = phoneNumber.getNumber();
                                break;
                            } else if (phoneNumber.isWork()) {
                                phone = phoneNumber.getNumber();
                                break;
                            } else { // for now use only preferred/work phone numbers
                                continue;
                            }
                        }
                        if (UtilValidate.isNotEmpty(phone)) {
                            String[] numberParts = phone.split("\\D");
                            StringBuilder telNumber = new StringBuilder("");
                            for (String number: numberParts) {
                                if (number != "") {
                                    telNumber.append(number);
                                }
                            }
                            serviceCtx.put("areaCode", telNumber.substring(0, 3));
                            serviceCtx.put("contactNumber", telNumber.substring(3));
                        }
                    }
                }
                OrganizationalIdentity  oid = contact.getOrganizationalIdentity();
                // Useful when creating a contact with more than OOTB
                if (!isGroup) {
                    serviceCtx.put("personalTitle", oid.getTitle());
                }

                // Needed when creating an account (a PartyGroup)
                if (isGroup) {
                    //serviceCtx.put("partyRole", oid.getRole()); // not used yet,maybe useful later
                    if (oid.hasOrganization()) {
                        Organization org = oid.getOrganization();
                        serviceCtx.put("groupName", org.getName());
                    }
                }

                GenericValue userLogin = (GenericValue) context.get("userLogin");
                serviceCtx.put("userLogin", userLogin);
                String serviceName = (String) context.get("serviceName");
                Map<String, Object> serviceContext = UtilGenerics.cast(context.get("serviceContext"));
                if(UtilValidate.isNotEmpty(serviceContext)) {
                    for (Map.Entry<String, Object> entry : serviceContext.entrySet()) {
                        serviceCtx.put(entry.getKey(), entry.getValue());
                    }
                }
                Map<String, Object> resp = dispatcher.runSync(serviceName, serviceCtx);
                result.put("partyId", resp.get("partyId"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "SfaImportVCardError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "SfaImportVCardError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    public static Map<String, Object> exportVCard(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        File file = null;
        try {
            ContactModelFactory cmf = Pim.getContactModelFactory();
            Contact contact = cmf.createContact();

            PersonalIdentity pid = cmf.createPersonalIdentity();
            String fullName = PartyHelper.getPartyName(delegator, partyId, false);
            String[] name = fullName.split("\\s");
            pid.setFirstname(name[0]);
            pid.setLastname(name[1]);
            contact.setPersonalIdentity(pid);

            GenericValue postalAddress = PartyWorker.findPartyLatestPostalAddress(partyId, delegator);
            Address address = cmf.createAddress();
            address.setStreet(postalAddress.getString("address1"));
            address.setCity(postalAddress.getString("city"));

            address.setPostalCode(postalAddress.getString("postalCode"));
            GenericValue state = postalAddress.getRelatedOne("StateProvinceGeo", false);
            if (UtilValidate.isNotEmpty(state)) {
                address.setRegion(state.getString("geoName"));
            }
            GenericValue countryGeo = postalAddress.getRelatedOne("CountryGeo", false);
            if (UtilValidate.isNotEmpty(countryGeo)) {
                String country = postalAddress.getRelatedOne("CountryGeo", false).getString("geoName");
                address.setCountry(country);
                address.setWork(true); // this can be better set by checking contactMechPurposeTypeId
            }
            contact.addAddress(address);

            Communications communication = cmf.createCommunications();
            contact.setCommunications(communication);

            PhoneNumber number = cmf.createPhoneNumber();
            GenericValue telecomNumber = PartyWorker.findPartyLatestTelecomNumber(partyId, delegator);
            if (UtilValidate.isNotEmpty(telecomNumber)) {
                number.setNumber(telecomNumber.getString("areaCode") + telecomNumber.getString("contactNumber"));
                number.setWork(true); // this can be better set by checking contactMechPurposeTypeId
                communication.addPhoneNumber(number);
            }
            EmailAddress email = cmf.createEmailAddress();
            GenericValue emailAddress = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator);
            if (UtilValidate.isNotEmpty(emailAddress.getString("infoString"))) {
                email.setAddress(emailAddress.getString("infoString"));
                communication.addEmailAddress(email);
            }
            ContactIOFactory ciof = Pim.getContactIOFactory();
            ContactMarshaller marshaller = ciof.createContactMarshaller();
            String saveToDirectory = EntityUtilProperties.getPropertyValue("sfa", "save.outgoing.directory", "", delegator);
            if (UtilValidate.isEmpty(saveToDirectory)) {
                saveToDirectory = System.getProperty("ofbiz.home");
            }
            String saveToFilename = fullName + ".vcf";
            file = FileUtil.getFile(saveToDirectory + "/" + saveToFilename);
            FileOutputStream outputStream = new FileOutputStream(file);
            marshaller.marshallContact(outputStream, contact);
            outputStream.close();
        } catch (FileNotFoundException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "SfaExportVCardErrorOpeningFile", UtilMisc.toMap("errorString", file.getAbsolutePath()), locale));
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "SfaExportVCardErrorWritingFile", UtilMisc.toMap("errorString", file.getAbsolutePath()), locale));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "SfaExportVCardError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }
}
