package com.fidelissd.zcp.xcommon.models.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.enums.PostalAddressTypesEnum;
import lombok.Data;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Represents postal address
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostalAddress {
    @JsonProperty("id")
    private String id = null;

    @JsonProperty("toName")
    private String toName = null;

    @JsonProperty("attnName")
    private String attnName = null;

    @JsonProperty("addressLine1")
    private String addressLine1 = null;

    @JsonProperty("addressLine2")
    private String addressLine2 = null;

    @JsonProperty("city")
    private String city = null;

    @JsonProperty("stateName")
    private String stateName = null;

    @JsonProperty("stateCode")
    private String stateCode = null;

    /**
     * State abbreviation, example: OR for Oregon
     */
    @JsonProperty("stateAbbr")
    private String stateAbbr = null;

    @JsonProperty("countryName")
    private String countryName = null;

    /**
     * Country abbreviation, example: USA for America
     */
    @JsonProperty("countryAbbr")
    private String countryAbbr = null;

    /**
     * Two digit ISO code of the country, example US/IN
     */
    @JsonProperty("countryCode")
    private String countryCode = null;

    /**
     * Two digit country code, example IN for India, AU for Australia
     *
     * @see <a href="https://www.iso.org/iso-3166-country-codes.html">ISO Country codes</a>
     */
    @JsonProperty("countryIsoCode")
    private String countryIsoCode = null;

    @JsonProperty("postalCode")
    private String postalCode = null;

    @JsonProperty("building")
    private String building = null;

    @JsonProperty("room")
    private String room = null;

    @JsonProperty("apartment")
    private String apartment = null;

    @JsonProperty("entryCode")
    private String entryCode = null;

    @JsonProperty("directions")
    private String directions = null;

    /**
     * Purpose id for this address, defaults to primary address.
     */
    @JsonProperty("contactMechPurposeTypeId")
    private String contactMechPurposeTypeId = PostalAddressTypesEnum.PRIMARY.getTypeId();

    /**
     * Comma separated address purpose types to be used to associate a postal address for more than
     * one purpose. For example using the same address for billing and shipping.
     */
    @JsonProperty("addressPurposes")
    private String addressPurposes = null;

    /**
     * Label for the address, add yours or use one of labels defined in {@link PostalAddressTypesEnum}
     */
    @JsonProperty("label")
    private String label = PostalAddressTypesEnum.PRIMARY.getLabel();

    /**
     * Formatted address.
     */
    @JsonProperty("formattedAddress")
    private String formattedAddress;

    /**
     * Is a representation of the place's address in the adr @see <a
     * href="http://microformats.org/wiki/adr">microformat</a>. HTML supported.
     */
    @JsonProperty("adrAddress")
    private String adrAddress;

    /**
     * Google map url, based on the location and specified lat long.
     */
    @JsonProperty("googleUrl")
    private String googleUrl;

    /**
     * Google maps place ID, used to accurately identify a place.
     */
    @JsonProperty("googlePlaceId")
    private String googlePlaceId;

    /**
     * Google map url, based on the location and specified lat long.
     */
    @JsonProperty("googleData")
    private String googleData;

    /**
     * Static map url image from the cdn
     */
    @JsonProperty("staticMapUrl")
    private String staticMapUrl;

    /**
     * Static map url image from the cdn secondary.
     */
    @JsonProperty("staticMapUrl2")
    private String staticMapUrl2;

    /**
     * The latitude of this location.
     */
    @JsonProperty("latitude")
    private Double latitude;

    /**
     * The longitude of this location.
     */
    @JsonProperty("longitude")
    private Double longitude;

    /**
     * The timezoneId of this location.
     */
    @JsonProperty("timezoneId")
    private String timezoneId;

    /**
     * The timezone of this location.
     */
    @JsonProperty("timezone")
    private Timezone timezone;

    @JsonProperty("isPrimary")
    private Boolean isPrimary = null;

    @JsonProperty("isShippingAddressPrimary")
    private Boolean isShippingAddressPrimary = null;

    @JsonProperty("isBillingAddressPrimary")
    private Boolean isBillingAddressPrimary = null;

    public String getFormattedAddressString() {
        StringBuffer formattedAddress = new StringBuffer();
        if (UtilValidate.isNotEmpty(this.addressLine1)) {
            formattedAddress.append(this.addressLine1);
        }
        if (UtilValidate.isNotEmpty(this.addressLine2)) {
            formattedAddress.append(", " + this.addressLine2);
        }
        if (UtilValidate.isNotEmpty(this.city)) {
            formattedAddress.append(", " + this.city);
        }
        if (UtilValidate.isNotEmpty(this.stateCode)) {
            formattedAddress.append(", " + this.stateCode);
            if (UtilValidate.isNotEmpty(this.postalCode)) {
                formattedAddress.append(" " + this.postalCode);
            }
        }
        if (UtilValidate.isNotEmpty(this.countryCode)) {
            formattedAddress.append(", " + this.countryCode);
        }
        return formattedAddress.toString();
    }
}
