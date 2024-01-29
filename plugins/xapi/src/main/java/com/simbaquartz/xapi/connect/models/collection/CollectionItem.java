package com.simbaquartz.xapi.connect.models.collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.people.Person;
import com.fidelissd.zcp.xcommon.models.media.File;
import com.simbaquartz.xapi.connect.models.product.Product;
import com.simbaquartz.xapi.connect.models.supplier.Supplier;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionItem {
    @JsonProperty("id")
    private String id;

    @JsonProperty("ids")
    private List<String> ids;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String itemType;

    @JsonProperty("sequence")
    private String sequenceId;

    @JsonProperty("product")
    private Product product;

    /*@JsonProperty("customer")
    private Customer customer;*/

    @JsonProperty("vendor")
    private Supplier vendor;

    @JsonProperty("documents")
    private File documents;

    //created this because Customer/Supplier object have bean validation
    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("vendor_id")
    private String vendorId;

    @JsonProperty("employee_id")
    private String employeeId;

    @JsonProperty("employee")
    private Person employee;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("created_by")
    private Person createdBy;

    @JsonProperty("updated_by")
    private Person updatedBy;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;
}
