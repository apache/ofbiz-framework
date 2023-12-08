package org.apache.ofbiz.base.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.ModelService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilMiscRuntime {
    private static final String MODULE = UtilMiscRuntime.class.getName();

    /**
     * List of domains or IP addresses to be checked to prevent Host Header Injection,
     * no spaces after commas, no wildcard, can be extended of course...
     * @return List of domains or IP addresses to be checked to prevent Host Header Injection,
     */
    public static List<String> getHostHeadersAllowed() {
        String hostHeadersAllowedString = UtilProperties.getPropertyValue("security", "host-headers-allowed", "localhost");
        List<String> hostHeadersAllowed = null;
        if (UtilValidate.isNotEmpty(hostHeadersAllowedString)) {
            hostHeadersAllowed = StringUtil.split(hostHeadersAllowedString, ",");
            hostHeadersAllowed = Collections.unmodifiableList(hostHeadersAllowed);
        }
        return hostHeadersAllowed;
    }

    public static Map<String, String> splitPhoneNumber(String phoneNumber, Delegator delegator) {
        Map<String, String> result = new HashMap<>();
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String defaultCountry = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
            GenericValue defaultGeo = EntityQuery.use(delegator).from("Geo").where("geoId", defaultCountry).cache().queryOne();
            String defaultGeoCode = defaultGeo != null ? defaultGeo.getString("geoCode") : "US";
            Phonenumber.PhoneNumber phNumber = phoneUtil.parse(phoneNumber, defaultGeoCode);
            if (phoneUtil.isValidNumber(phNumber) || phoneUtil.isPossibleNumber(phNumber)) {
                String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(phNumber);
                int areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(phNumber);
                result.put("countryCode", Integer.toString(phNumber.getCountryCode()));
                if (areaCodeLength > 0) {
                    result.put("areaCode", nationalSignificantNumber.substring(0, areaCodeLength));
                    result.put("contactNumber", nationalSignificantNumber.substring(areaCodeLength));
                } else {
                    result.put("areaCode", "");
                    result.put("contactNumber", nationalSignificantNumber);
                }
            } else {
                Debug.logError("Invalid phone number " + phoneNumber, MODULE);
                result.put(ModelService.ERROR_MESSAGE, "Invalid phone number");
            }
        } catch (GenericEntityException | NumberParseException ex) {
            Debug.logError(ex, MODULE);
            result.put(ModelService.ERROR_MESSAGE, ex.getMessage());
        }
        return result;
    }

}
