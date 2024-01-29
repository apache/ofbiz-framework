package com.simbaquartz.xapi.connect.models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class Opportunity {

    private String accountPartyId;
    private List<String> leadPartyIds;
    private String supplierPartyId;
    private List<String> supplierPOCs;
    private String description;
    private String nextStep;
    private Timestamp nextStepDate;
    private BigDecimal estimatedAmount;
    private BigDecimal estimatedProbability;
    private String currencyUomId;
    private String dataSourceId;
    private Timestamp estimatedCloseDate;
    private String opportunityStageId;
    private String typeEnumId;
    private String opportunityName;
    private String solicitationNumber;
    private String location;
    private String agency;
    private String noticeType;
    private Timestamp originalPostedDate;
    private Timestamp postedDate;
    private Timestamp responseDate;
    private Timestamp originalResponseDate;
    private String originalSetAside;
    private String setAside;
    private List<String> classificationCodes;
    private List<String> naicsCodes;
    private String checkFollowUpTask;
    private String salesOpportunityId;
    private String marketingCampaignId;

    public String getAccountPartyId() {
        return accountPartyId;
    }

    public void setAccountPartyId(String accountPartyId) {
        this.accountPartyId = accountPartyId;
    }

    public String getSupplierPartyId() {
        return supplierPartyId;
    }

    public void setSupplierPartyId(String supplierPartyId) {
        this.supplierPartyId = supplierPartyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public Timestamp getNextStepDate() {
        return nextStepDate;
    }

    public void setNextStepDate(Timestamp nextStepDate) {
        this.nextStepDate = nextStepDate;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public BigDecimal getEstimatedProbability() {
        return estimatedProbability;
    }

    public void setEstimatedProbability(BigDecimal estimatedProbability) {
        this.estimatedProbability = estimatedProbability;
    }

    public String getCurrencyUomId() {
        return currencyUomId;
    }

    public void setCurrencyUomId(String currencyUomId) {
        this.currencyUomId = currencyUomId;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public Timestamp getEstimatedCloseDate() {
        return estimatedCloseDate;
    }

    public void setEstimatedCloseDate(Timestamp estimatedCloseDate) {
        this.estimatedCloseDate = estimatedCloseDate;
    }

    public String getOpportunityStageId() {
        return opportunityStageId;
    }

    public void setOpportunityStageId(String opportunityStageId) {
        this.opportunityStageId = opportunityStageId;
    }

    public String getTypeEnumId() {
        return typeEnumId;
    }

    public void setTypeEnumId(String typeEnumId) {
        this.typeEnumId = typeEnumId;
    }

    public String getOpportunityName() {
        return opportunityName;
    }

    public void setOpportunityName(String opportunityName) {
        this.opportunityName = opportunityName;
    }

    public String getSolicitationNumber() {
        return solicitationNumber;
    }

    public void setSolicitationNumber(String solicitationNumber) {
        this.solicitationNumber = solicitationNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public Timestamp getOriginalPostedDate() {
        return originalPostedDate;
    }

    public void setOriginalPostedDate(Timestamp originalPostedDate) {
        this.originalPostedDate = originalPostedDate;
    }

    public Timestamp getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(Timestamp postedDate) {
        this.postedDate = postedDate;
    }

    public Timestamp getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Timestamp responseDate) {
        this.responseDate = responseDate;
    }

    public Timestamp getOriginalResponseDate() {
        return originalResponseDate;
    }

    public void setOriginalResponseDate(Timestamp originalResponseDate) {
        this.originalResponseDate = originalResponseDate;
    }

    public String getOriginalSetAside() {
        return originalSetAside;
    }

    public void setOriginalSetAside(String originalSetAside) {
        this.originalSetAside = originalSetAside;
    }

    public String getSetAside() {
        return setAside;
    }

    public void setSetAside(String setAside) {
        this.setAside = setAside;
    }

    public String getCheckFollowUpTask() {
        return checkFollowUpTask;
    }

    public void setCheckFollowUpTask(String checkFollowUpTask) {
        this.checkFollowUpTask = checkFollowUpTask;
    }

    public String getSalesOpportunityId() {
        return salesOpportunityId;
    }

    public void setSalesOpportunityId(String salesOpportunityId) {
        this.salesOpportunityId = salesOpportunityId;
    }

    public List<String> getLeadPartyIds() {
        return leadPartyIds;
    }

    public void setLeadPartyIds(List<String> leadPartyIds) {
        this.leadPartyIds = leadPartyIds;
    }

    public List<String> getSupplierPOCs() {
        return supplierPOCs;
    }

    public void setSupplierPOCs(List<String> supplierPOCs) {
        this.supplierPOCs = supplierPOCs;
    }

    public List<String> getClassificationCodes() {
        return classificationCodes;
    }

    public void setClassificationCodes(List<String> classificationCodes) {
        this.classificationCodes = classificationCodes;
    }

    public List<String> getNaicsCodes() {
        return naicsCodes;
    }

    public void setNaicsCodes(List<String> naicsCodes) {
        this.naicsCodes = naicsCodes;
    }

    public String getMarketingCampaignId() {
        return marketingCampaignId;
    }

    public void setMarketingCampaignId(String marketingCampaignId) {
        this.marketingCampaignId = marketingCampaignId;
    }
}
