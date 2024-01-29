package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.simbaquartz.xapi.connect.models.note.Note;

import java.util.List;
import java.util.Objects;


/**
 * @deprecated use {@link com.simbaquartz.xapi.connect.api.customer.model.Customer} instead.
 * Represents a store customer, which can have one or more cards on file associated with it.
 **/
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class Customer {

    private String id = null;
    private String displayName = null;
    private String firstName = null;
    private String middleName = null;
    private String lastName = null;
    private String roleTypeId = null;
    private String parentPartyId = null;
    private String email = null;
    private String webAddress = null;
    private List<String> emails = null;
    private List<String> webAddresses = null;
    private List<Phone> phoneNumbers = null;
    private Boolean isPrimaryEmail = null;
    private Boolean emailVerified = null;

    private String phoneFormatted = null;
    private String phone = null;
    private String countryCode = null;
    private String areaCode = null;
    private Boolean phoneVerified = null;

    @JsonIgnore
    private String password = null;
    @JsonIgnore
    private String passwordConfirm = null;

    private PostalAddress address = null;
    private Note note = null;

    private String externalId = null;
    private Boolean acceptsMarketing = null;

    private String createdAt = null;
    private String createdAtPretty = null;
    private String updatedAt = null;
    private String updatedAtPretty = null;

    @JsonProperty("contact_first_name")
    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    private String contactFirstName = null;

    @JsonProperty("contact_last_name")
    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    private String contactLastName = null;

    private String contactEmails = null;

    private String contactPhoneNumbers = null;



    @JsonProperty("contact_email")
    public String getContactEmails() {
        return contactEmails;
    }
    public void setContactEmails(String contactEmails) {
        this.contactEmails = contactEmails;
    }

    @JsonProperty("contact_phone_number")
    public String getContactPhoneNumbers() {
        return contactPhoneNumbers;
    }
    public void setContactPhoneNumbers(String contactPhoneNumbers) {
        this.contactPhoneNumbers = contactPhoneNumbers;
    }



    /**
     * Unique identifier of a customer.
     **/

    @JsonProperty("customer_id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The customer's full name.
     **/

    @JsonProperty("display_name")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * The customer's first name.
     **/

    @JsonProperty("first_name")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * The customer's middle name.
     **/

    @JsonProperty("middle_name")
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * The customer's last name.
     **/

    @JsonProperty("last_name")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * The customer's email address.
     **/

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * The customer's roleTypeId.
     **/

    @JsonProperty("role_type_id")
    public String getRoleTypeId() {
        return roleTypeId;
    }

    public void setRoleTypeId(String roleTypeId) {
        this.roleTypeId = roleTypeId;
    }

    /**
     * The customer's agency id.
     **/

    @JsonProperty("parent_party_id")
    public String getParentPartyId() {
        return parentPartyId;
    }

    public void setParentPartyId(String parentPartyId) {
        this.parentPartyId = parentPartyId;
    }

    /**
     * The customer's webAddress address.
     **/

    @JsonProperty("web_address")
    public String getWebAddress() {
        return webAddress;
    }

    public void setWebAddress(String webAddress) {
        this.webAddress = webAddress;
    }

    /**
     * The customer's email addresses.
     **/

    @JsonProperty("emails")
    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    /**
     * The customer's web addresses.
     **/

    @JsonProperty("web_addresses")
    public List<String> getWebAddresses() {
        return webAddresses;
    }

    public void setWebAddresses(List<String> webAddresses) {
        this.webAddresses = webAddresses;
    }

    /**
     * The customer's email address, if primary.
     **/

    @JsonProperty("is_primary_email")
    public Boolean getIsPrimaryEmail() {
        return isPrimaryEmail;
    }

    public void setIsPrimaryEmail(Boolean isPrimaryEmail) {
        this.isPrimaryEmail = isPrimaryEmail;
    }

    /**
     * The customer's email address verification flag.
     **/

    @JsonProperty("email_verified")
    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    /**
     * The customer's phone number formatted.
     **/
    @JsonProperty("phone_number")
    public String getPhoneFormatted() {
        return phoneFormatted;
    }
    public void setPhoneFormatted(String phoneFormatted) {
        this.phoneFormatted = phoneFormatted;
    }

    /**
     * The customer's phone number in raw format (10 digit).
     **/
    @JsonProperty("phone")
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * The customer's phone number verification flag.
     **/
    @JsonProperty("phone_verified")
    public Boolean getPhoneVerified() {
        return phoneVerified;
    }
    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    /**
     * The customer's phone countryCode.
     **/

    @JsonProperty("country_code")
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * The customer's phone areaCode.
     **/

    @JsonProperty("area_code")
    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    /**
     * The customer's account password.
     **/
    @JsonIgnore
    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * The customer's account password confirmation.
     **/
    @JsonIgnore
    @JsonProperty("password_confirm")
    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    /**
     **/

    @JsonProperty("address")
    public PostalAddress getAddress() {
        return address;
    }

    public void setAddress(PostalAddress address) {
        this.address = address;
    }

    /**
     **/

    @JsonProperty("phone_numbers")
    public List<Phone> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<Phone> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    /**
     * A client specified identifier to associate an entity in another system with this customer.
     **/

    @JsonProperty("external_id")
    public String getExternalId() {
        return externalId;
    }
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * Indicates whether the customer has consented to be sent marketing material via email. Valid values are \"true\" and \"false.\"
     **/

    @JsonProperty("accepts_marketing")
    public Boolean getAcceptsMarketing() {
        return acceptsMarketing;
    }
    public void setAcceptsMarketing(Boolean acceptsMarketing) {
        this.acceptsMarketing = acceptsMarketing;
    }

    /**
     * The time when the customer was created, in RFC 3339 format.
     **/

    @JsonProperty("created_at")
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * The time when the customer was last updated, in RFC 3339 format.
     **/

    @JsonProperty("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Created at date in pretty format
     * @return
     */
    @JsonProperty("created_at_pretty")
    public String getCreatedAtPretty() {return createdAtPretty;}
    public void setCreatedAtPretty(String createdAtPretty) {this.createdAtPretty = createdAtPretty;}

    /**
     * Last Updated at date in pretty format
     * @return
     */
    @JsonProperty("updated_at_pretty")
    public String getUpdatedAtPretty() {return updatedAtPretty;}
    public void setUpdatedAtPretty(String updatedAtPretty) {this.updatedAtPretty = updatedAtPretty;}



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id) &&
                Objects.equals(displayName, customer.displayName) &&
                Objects.equals(firstName, customer.firstName) &&
                Objects.equals(middleName, customer.middleName) &&
                Objects.equals(lastName, customer.lastName) &&
                Objects.equals(email, customer.email) &&
                Objects.equals(emailVerified, customer.emailVerified) &&
                Objects.equals(phone, customer.phone) &&
                Objects.equals(phoneVerified, customer.phoneVerified) &&
                Objects.equals(address, customer.address) &&
                Objects.equals(externalId, customer.externalId) &&
                Objects.equals(acceptsMarketing, customer.acceptsMarketing) &&
                Objects.equals(createdAt, customer.createdAt) &&
                Objects.equals(updatedAt, customer.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, firstName, middleName, lastName, email, emailVerified, phone, phoneVerified, address, externalId, acceptsMarketing, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Customer {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    middleName: ").append(toIndentedString(middleName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    emailVerified: ").append(toIndentedString(emailVerified)).append("\n");
        sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
        sb.append("    phoneVerified: ").append(toIndentedString(phoneVerified)).append("\n");
        sb.append("    address: ").append(toIndentedString(address)).append("\n");
        sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
        sb.append("    acceptsMarketing: ").append(toIndentedString(acceptsMarketing)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
        sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}