package com.fidelissd.zcp.xcommon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * Represents a phone number object, supports international formats.
 **/
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Phone extends CreatedModifiedBy {

    @JsonProperty("id")
    private String id = null;//contactMechId

    /**
     * Two digit region code, to be used for international phone number validation and formatting.
     * http://libphonenumber.appspot.com/
     * @defaults to US
     */
    @Size(min = 2, max = 2, message = "Please enter a valid 2 letter country region code. Example, use US for USA, IN for India")
    @JsonProperty("regionCode")
    private String regionCode = "US";

    /**
     * Countr phone code example +1 for US
     */
    @JsonProperty("countryCode")
    private String countryCode = "+1";

    @JsonProperty("asYouTypeFormat")
    private String asYouTypeFormat = null;

    @NotEmpty(message = "Please provide the area code")
    @JsonProperty("areaCode")
    private String areaCode = null;

    @NotEmpty(message = "Please provide the phone number")
    @Size(min = 1, max = 10, message = "Please enter a valid 10 digit phone number, your input does not match the required length.")
    @Digits(integer = 10, fraction = 0, message = "Please enter a valid 10 digit phone number, only numbers are allowed.")
    @JsonProperty("phone")
    private String phone = null;

    @JsonProperty("extension")
    private String extension = null;

    /**
     * Whether the phone is verified via an authentication method, example text.
     */
    @JsonProperty("verified")
    private Boolean phoneVerified = null;

    /**
     * Whether the phone number is valid or not.
     */
    @JsonProperty("isValid")
    private Boolean validPhone = null;

    /**
     * Whether the phone number is a possible phone number.
     */
    @JsonProperty("isPossibleNumber")
    private Boolean possibleNumber = null;

    /**
     * Whether the phone number is valid for the input region or not.
     */
    @JsonProperty("isValidForRegion")
    private Boolean validNumberForRegion = null;

    /*@JsonProperty("phonePurposes")
    private List phonePurposes = null;*/

    @JsonProperty("phoneFormatted")
    private String phoneFormatted = null;

    /**
     * Phone number's location country name. Ex. India
     */
    @JsonProperty("location")
    private String location = null;

    /**
     * Type of phone number, mobile, one of
     * FIXED_LINE, MOBILE, FIXED_LINE_OR_MOBILE, TOLL_FREE, PREMIUM_RATE, SHARED_COST, VOIP, PERSONAL_NUMBER, PAGER, UAN, VOICEMAIL, UNKNOWN
     */
    @JsonProperty("type")
    private String phoneType = null;

    /**
     * Phone number formatted in the selected region's national format. Ex 	085569 36450
     */
    @JsonProperty("nationalFormat")
    private String nationalFormat = null;

    /**
     * Phone number formatted in the international format. Ex +91 85569 36450
     */
    @JsonProperty("internationalFormat")
    private String internationalFormat = null;

    /**
     * Phone number formatted in the e164 Format format. Ex +918556936450
     */
    @JsonProperty("e164Format")
    private String e164Format = null;

    /**
     * Time zone of the phone number, e.g. [Asia/Calcutta]
     */
    @JsonProperty("timeZone")
    private String timeZone = null;

    /**
     * ID for the label
     */
    @JsonProperty("labelId")
    private String contactMechPurposeTypeId = null;

    /***
     * The type of the phone number. The type can be custom or one of these predefined values:

     - home
     - work
     - mobile
     - other
     */
    @JsonProperty("label")
    private String label = null;

    private Boolean isDeleted = null;
    @JsonProperty("is_deleted")
    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    private String otherPurpose = null;

    @JsonProperty("other_purpose")
    public String getOtherPurpose() {
        return otherPurpose;
    }

    public void setOtherPurpose(String otherPurpose) {
        this.otherPurpose = otherPurpose;
    }

    /**
     * The phone's phone Purposes.
     **/

    @JsonProperty("phone_purposes")
    private List phonePurposes = null;
    public List getPhonePurposes() {
        return phonePurposes;
    }

    public void setPhonePurposes(List phonePurposes) {
        this.phonePurposes = phonePurposes;
    }

    private String phonePurpose = null;
    @JsonProperty("phone_purpose")
    public String getPhonePurpose() {
        return phonePurpose;
    }
    public void setPhonePurpose(String phonePurpose) {
        this.phonePurpose = phonePurpose;
    }
}