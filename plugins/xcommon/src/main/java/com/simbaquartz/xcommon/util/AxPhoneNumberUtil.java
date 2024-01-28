package com.simbaquartz.xcommon.util;

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AxPhoneNumberUtil {
    private static final String module = AxPhoneNumberUtil.class.getName();

    /**
     * Returns a valid region code US, IN for the input country phone code 1, 91 etc.
     *
     * @param countryCode             phone country code
     * @return
     * @External References
     * http://libphonenumber.appspot.com/
     * https://github.com/googlei18n/libphonenumber/blob/master/java/demo/src/com/google/phonenumbers/PhoneNumberParserServlet.java
     */
    public static String  getPhoneRegionCodeFromCountryCode(int countryCode) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        return phoneNumberUtil.getRegionCodeForCountryCode(countryCode);
    }

    /**
     * Returns the dial code +1, +91 based on the input two digit country ISO code (US, IN).
     * @param countryIsoCode
     * @return
     */
    public static int  getPhoneCountryCodeFromRegionCode(String countryIsoCode) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        return phoneNumberUtil.getCountryCodeForRegion(countryIsoCode);
    }

    /**
     * Implementation of preparePhoneNumberInfo with input as country calling code rather then region code.
     *
     * @param phone             10 digit phone number without any spaces.
     * @param countryCode 2 digit country code, like US for USA IN for India
     * @return
     * @External References
     * http://libphonenumber.appspot.com/
     * https://github.com/googlei18n/libphonenumber/blob/master/java/demo/src/com/google/phonenumbers/PhoneNumberParserServlet.java
     */
    public static Map preparePhoneNumberInfo(String phone, int countryCode) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String countryRegionCode = phoneNumberUtil.getRegionCodeForCountryCode(countryCode);
        return preparePhoneNumberInfo(phone, countryRegionCode);
    }

    /**
     * Returns a map containing, phone's areaCode, contactNumber, countryCode, phoneNumber (Phonenumber.PhoneNumber object)
     *
     * @param phone             10 digit phone number without any spaces.
     * @param countryRegionCode 2 digit country code, like US for USA IN for India
     * @return countryCode, areaCode, contactNumber, phoneNumber(PhoneNumber), isValidNumber, location,
     *          regionCode (US, IN etc. two digit country code),
     *          type ( one of
     *              FIXED_LINE,
     *              MOBILE,
     *              FIXED_LINE_OR_MOBILE,
     *              TOLL_FREE,
     *              PREMIUM_RATE,
     *              SHARED_COST,
     *              VOIP,
     *              PERSONAL_NUMBER,
     *              PAGER,
     *              UAN,
     *              VOICEMAIL,
     *              UNKNOWN)
     *          timeZone,
     *          isPossibleNumber(true/false)
     *          e164Format Produces +16025751469
     *          nationalFormat Produces (602) 575-1469
     *          internationalFormat Produces +1 602-575-1469
     *          isValidNumberForRegion
     *
*          Example:
     *
     * @External References
     * @see <a href="http://libphonenumber.appspot.com/">link</a>
     * @see <a href="https://github.com/googlei18n/libphonenumber/blob/master/java/demo/src/com/google/phonenumbers/PhoneNumberParserServlet.java">link</a>
     */
    public static Map preparePhoneNumberInfo(String phone, String countryRegionCode) {
        Map phoneInfoMap = new HashMap<>();
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber number = null;
        try {
            number = phoneUtil.parseAndKeepRawInput(phone, countryRegionCode);
        } catch (NumberParseException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        //checks if the number is a valid one for the country, if not return error
        //TODO: Have this as a store setting, if only valid phone numbers are allowed or not
        boolean isNumberValid = phoneUtil.isValidNumber(number);
        if (!isNumberValid) {
            Debug.logWarning("Not a valid phone number for country [" + countryRegionCode + "] please provide a valid phone number and try again.", module);
            return ServiceUtil.returnError("Not a valid phone number for country [" + countryRegionCode + "] please provide a valid phone number and try again.");
        }

        //country code
        String countryCode = "+" + phoneUtil.getCountryCodeForRegion(countryRegionCode);

        //area code
        //get area code : Save in areaCode
        String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
        int nationalDestinationCodeLength = phoneUtil.getLengthOfNationalDestinationCode(number);

        String phoneAreaCode = "";
        if (nationalDestinationCodeLength > 0) {
            phoneAreaCode = nationalSignificantNumber.substring(0, nationalDestinationCodeLength);
        }

        //contact number
        String phoneContactNumber = nationalSignificantNumber.substring(nationalDestinationCodeLength, nationalSignificantNumber.length()).trim();

        String languageCode = "en";  // Default languageCode to English if nothing is entered.
        Locale geocodingLocale = new Locale(languageCode, countryRegionCode);

        phoneInfoMap.put("countryCode", countryCode);
        phoneInfoMap.put("areaCode", phoneAreaCode);
        phoneInfoMap.put("contactNumber", phoneContactNumber);
        phoneInfoMap.put("phoneNumber", number);
        phoneInfoMap.put("isValidNumber", phoneUtil.isValidNumber(number));

        String location = PhoneNumberOfflineGeocoder.getInstance().getDescriptionForNumber(
                number, geocodingLocale);
        phoneInfoMap.put("location", location);

        String regionCode = phoneUtil.getRegionCodeForNumber(number);
        phoneInfoMap.put("regionCode", regionCode);

        PhoneNumberUtil.PhoneNumberType numberType = phoneUtil.getNumberType(number);
        phoneInfoMap.put("type", numberType.toString());

        String timeZone = PhoneNumberToTimeZonesMapper.getInstance().getTimeZonesForNumber(number).toString();
        phoneInfoMap.put("timeZone", timeZone);

        boolean isPossibleNumber = phoneUtil.isPossibleNumber(number);
        phoneInfoMap.put("isPossibleNumber", isPossibleNumber);

        if(isNumberValid){
            phoneInfoMap.put("e164Format", phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
            phoneInfoMap.put("internationalFormat", phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
            phoneInfoMap.put("nationalFormat", phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
            phoneInfoMap.put("isValidNumberForRegion", phoneUtil.isValidNumberForRegion(number, countryRegionCode));

            //prepares the as you type formatter
            AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(regionCode);

            int rawNumberLength = phone.length();
            String asYouTypeFormat = phone;
            for (int i = 0; i < rawNumberLength; i++) {
                // Note this doesn't handle supplementary characters, but it shouldn't be a big deal as
                // there are no dial-pad characters in the supplementary range.
                char inputChar = phone.charAt(i);
                asYouTypeFormat = formatter.inputDigit(inputChar);
            }

            phoneInfoMap.put("asYouTypeFormat", asYouTypeFormat);
        }

        return phoneInfoMap;
    }

    /**
     * Finds phone number using provided 10 digit number, must be a valid phone number.
     *
     * @param delegator
     * @param phone
     * @return
     */
    public static GenericValue findPhone(Delegator delegator, String phone) {
        if (UtilValidate.isEmpty(phone) || !AxUtilValidate.isValidPhoneNumber(phone, delegator)) {
            return null;
        }

        Map phoneMetaData = preparePhoneNumberInfo(phone, "US");//defaults to US region
        GenericValue phoneNumber = null;
        try {
            phoneNumber = EntityQuery.use(delegator).from("TelecomNumber").where("areaCode", phoneMetaData.get("areaCode"), "contactNumber", phoneMetaData.get("areaCode")).queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return phoneNumber;
    }

}
