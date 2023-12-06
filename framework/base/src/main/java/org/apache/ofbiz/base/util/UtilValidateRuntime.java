package org.apache.ofbiz.base.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

public class UtilValidateRuntime {
    private static final String MODULE = UtilValidate.class.getName();
    public static boolean isValidPhoneNumber(String phoneNumber, Delegator delegator) {
        String geoId = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
        return isValidPhoneNumber(phoneNumber, geoId, delegator);
    }

    public static boolean isValidPhoneNumber(String phoneNumber, String geoId, Delegator delegator) {
        boolean isValid = false;
        try {
            GenericValue geo = EntityQuery.use(delegator).from("Geo").where("geoId", geoId).cache().queryOne();
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String geoCode = geo != null ? geo.getString("geoCode") : "US";
            Phonenumber.PhoneNumber phNumber = phoneUtil.parse(phoneNumber, geoCode);
            if (phoneUtil.isValidNumber(phNumber) || phoneUtil.isPossibleNumber(phNumber)) {
                isValid = true;
            }
        } catch (GenericEntityException | NumberParseException ex) {
            Debug.logError(ex, MODULE);
        }
        return isValid;
    }

}
