package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class CustomerContract {

    @JsonProperty("contract_id")
    private String id = null;

    @JsonProperty("customer_id")
    private String customerId = null;

    @NotEmpty(message = "Please provide the contract title")
    @JsonProperty("title")
    private String title = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("contract_date")
    private Timestamp contractDate = null;

    @NotEmpty(message = "Please provide the start date")
    @JsonProperty("start_date")
    private Timestamp startDate = null;

    @NotEmpty(message = "Please provide the end date")
    @JsonProperty("end_date")
    private Timestamp endDate = null;

    @JsonProperty("ref_num")
    private String refNum = null;

    @JsonProperty("contract_amount")
    private BigDecimal amount = null;

    @JsonProperty("price_type")
    private String priceType = null;

    @JsonProperty("margin")
    private BigDecimal margin = null;

    @JsonProperty("supplier_id")
    private String supplierId = null;

    @JsonProperty("keyword")
    private String keyword = null;

    @JsonProperty("filter_by_date")
    private boolean filterByDate = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getContractDate() {
        return contractDate;
    }

    public void setContractDate(Timestamp contractDate) {
        this.contractDate = contractDate;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getRefNum() {
        return refNum;
    }

    public void setRefNum(String refNum) {
        this.refNum = refNum;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public BigDecimal getMargin() {
        return margin;
    }

    public void setMargin(BigDecimal margin) {
        this.margin = margin;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public boolean isFilterByDate() {
        return filterByDate;
    }

    public void setFilterByDate(boolean filterByDate) {
        this.filterByDate = filterByDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, title, contractDate, startDate, endDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Contract {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    customerId: ").append(toIndentedString(customerId)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    contractDate: ").append(toIndentedString(contractDate)).append("\n");
        sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
        sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
        sb.append("    refNum: ").append(toIndentedString(refNum)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
