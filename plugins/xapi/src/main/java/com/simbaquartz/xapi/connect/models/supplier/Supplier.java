package com.simbaquartz.xapi.connect.models.supplier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.product.Product;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.client.Employee;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Supplier {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("account_lead_party_id")
    private String accountLeadPartyId = null;

    @NotEmpty(message = "Please provide a Supplier Name")
    @Size(min = 2, max = 100, message = "Supplier name should be at least 2 and at max 100 characters long.")
    @JsonProperty("name")
    private String name = null;

    @JsonProperty("phone_number")
    private Phone phoneNumber = null;

    @JsonProperty("address")
    private PostalAddress address = null;

    @JsonProperty("employee")
    private Employee employee = null;

    @JsonProperty("product")
    private Product product = null;

    @JsonProperty("default_currency")
    private String defaultCurrency = null;

    @Email(message = "Invalid format for email, please use user@example.org format.")
    @JsonProperty("email")
    private String email = null;

    @JsonProperty("web_address")
    private String webAddress = null;

    @JsonProperty("is_primary_email")
    private Boolean isPrimaryEmail = null;

    @JsonProperty("phone")
    private String phone = null;

    @JsonProperty("tin_number")
    private String tinNumber = null;

    @JsonProperty("duns_number")
    private String dunsNumber = null;

    @JsonProperty("created_at")
    private String createdAt = null;

    @JsonProperty("created_at_pretty")
    private String createdAtPretty = null;

    @JsonProperty("updated_at")
    private String updatedAt = null;

    @JsonProperty("updated_at_pretty")
    private String updatedAtPretty = null;

    @JsonProperty("product_id")
    private String productId = null;

    @JsonProperty("supplier_product_id")
    private String supplierProductId = null;

    @JsonProperty("last_price")
    private BigDecimal lastPrice = null;

    @JsonProperty("proxy_enable")
    private String proxyEnable = null;

    @JsonProperty("supplier_product_name")
    private String supplierProductName = null;

    @JsonProperty("party_id")
    private String partyId = null;

    @JsonProperty("supplier_pref_order_id")
    private String supplierPrefOrderId = null;

    @JsonProperty("product_type_id")
    private String productTypeId = null;

    @JsonProperty("contentId")
    private String contentId = null;

    @JsonProperty("attr_name")
    private String attrName  = null;

    @JsonProperty("attr_value")
    private String attrValue  = null;
}
