package com.simbaquartz.xapi.connect.models;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class SalesQuota {

    private String supplierPartyId = null;

    private String financialYear = null;

    private String createdBy = null;

    private String lastModifiedBy = null;

    private String quotaNote = null;

    private Long units=null;

    private Double profitPercent=null;

    private BigDecimal totalQuota = null;


    @JsonProperty("supplier_party_id")
    public String getSupplierPartyId() {
        return supplierPartyId;
    }

    public void setSupplierPartyId(String supplierPartyId) {
        this.supplierPartyId = supplierPartyId;
    }

    @JsonProperty("financial_year")
    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    @JsonProperty("created_by")
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("units")
    public Long getUnits() {
        return units;
    }

    public void setUnits(Long units) {
        this.units = units;
    }

    @JsonProperty("profile_percent")
    public Double getProfitPercent() {
        return profitPercent;
    }

    public void setProfitPercent(Double profitPercent) {
        this.profitPercent = profitPercent;
    }

    @JsonProperty("total_quota")
    public BigDecimal getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(BigDecimal totalQuota) {
        this.totalQuota = totalQuota;
    }

    @JsonProperty("quota_note")
    public String getQuotaNote() {
        return quotaNote;
    }

    public void setQuotaNote(String quotaNote) {
        this.quotaNote = quotaNote;
    }

    @JsonProperty("last_modified_by")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }



}
