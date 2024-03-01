package org.apache.ofbiz.base.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.ofbiz.base.util.collections.MapComparator;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.ModelService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

    /**
     * The input can be a String, Locale, or even null and a valid Locale will always be returned; if nothing else works, returns the default locale.
     * @param localeObject An Object representing the locale
     */
    public static Locale ensureLocale(Object localeObject) {
        if (localeObject instanceof String) {
            Locale locale = UtilMisc.parseLocale((String) localeObject);
            if (locale != null) {
                return locale;
            }
        } else if (localeObject instanceof Locale) {
            return (Locale) localeObject;
        }
        return Locale.getDefault();
    }

    /**
     * Returns a List of available locales sorted by display name
     */
    public static List<Locale> availableLocales() {
        return LocaleHolder.AVAIL_LOCALE_LIST;
    }

    /**
     * Sort a List of Maps by specified consistent keys.
     * @param listOfMaps List of Map objects to sort.
     * @param sortKeys List of Map keys to sort by.
     * @return a new List of sorted Maps.
     */
    public static List<Map<Object, Object>> sortMaps(List<Map<Object, Object>> listOfMaps, List<? extends String> sortKeys) {
        if (listOfMaps == null || sortKeys == null) {
            return null;
        }
        List<Map<Object, Object>> toSort = new ArrayList<>(listOfMaps.size());
        toSort.addAll(listOfMaps);
        try {
            MapComparator mc = new MapComparator(sortKeys);
            toSort.sort(mc);
        } catch (Exception e) {
            Debug.logError(e, "Problems sorting list of maps; returning null.", MODULE);
            return null;
        }
        return toSort;
    }

    /**
     * Generates a String from given values delimited by delimiter.
     * @param values
     * @param delimiter
     * @return String
     */
    public static String collectionToString(Collection<? extends Object> values, String delimiter) {
        if (UtilValidate.isEmpty(values)) {
            return null;
        }
        if (delimiter == null) {
            delimiter = "";
        }
        StringBuilder out = new StringBuilder();

        for (Object val : values) {
            out.append(UtilFormatOut.safeToString(val)).append(delimiter);
        }
        return out.toString();
    }

    // Private lazy-initializer class
    private static class LocaleHolder {
        private static final List<Locale> AVAIL_LOCALE_LIST = getAvailableLocaleList();

        private static List<Locale> getAvailableLocaleList() {
            TreeMap<String, Locale> localeMap = new TreeMap<>();
            String localesString = UtilProperties.getPropertyValue("general", "locales.available");
            if (UtilValidate.isNotEmpty(localesString)) {
                List<String> idList = StringUtil.split(localesString, ",");
                for (String id : idList) {
                    Locale curLocale = UtilMisc.parseLocale(id);
                    localeMap.put(curLocale.getDisplayName(), curLocale);
                }
            } else {
                Locale[] locales = Locale.getAvailableLocales();
                for (int i = 0; i < locales.length && locales[i] != null; i++) {
                    String displayName = locales[i].getDisplayName();
                    if (!displayName.isEmpty()) {
                        localeMap.put(displayName, locales[i]);
                    }
                }
            }
            return Collections.unmodifiableList(new ArrayList<>(localeMap.values()));
        }
    }
}
