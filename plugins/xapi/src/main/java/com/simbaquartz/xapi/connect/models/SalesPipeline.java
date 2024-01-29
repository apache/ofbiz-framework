package com.simbaquartz.xapi.connect.models;


import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class SalesPipeline {

    @JsonProperty("id")
    private String id = null;

    @NotNull(message = "Please provide the estimated award month")
    @JsonProperty("estimated_award_month")
    private String estimatedAwardMonth  = null;

    @NotNull(message = "Please provide the estimated award quarter")
    @JsonProperty("estimated_award_quarter")
    private String estimatedAwardQuarter  = null;

    @NotNull(message = "Please provide the estimated award year")
    @JsonProperty("estimated_award_year")
    private String estimatedAwardYear  = null;

    @NotNull(message = "Please provide the estimated percentage confidence")
    @JsonProperty("percentage_confidence")
    private String percentageConfidence  = null;

    @JsonProperty("sales_pipeline_data_notes")
    private String salesPipelineDataNotes  = null;

    @JsonProperty("attribute_name")
    private String attrName  = null;

    @JsonProperty("attribute_value")
    private String attrValue  = null;

    public String getEstimatedAwardMonth() {
        return estimatedAwardMonth;
    }

    public void setEstimatedAwardMonth(String estimatedAwardMonth) {
        this.estimatedAwardMonth = estimatedAwardMonth;
    }

    public String getEstimatedAwardQuarter() {
        return estimatedAwardQuarter;
    }

    public void setEstimatedAwardQuarter(String estimatedAwardQuarter) {
        this.estimatedAwardQuarter = estimatedAwardQuarter;
    }

    public String getEstimatedAwardYear() {
        return estimatedAwardYear;
    }

    public void setEstimatedAwardYear(String estimatedAwardYear) {
        this.estimatedAwardYear = estimatedAwardYear;
    }

    public String getPercentageConfidence() {
        return percentageConfidence;
    }

    public void setPercentageConfidence(String percentageConfidence) {
        this.percentageConfidence = percentageConfidence;
    }

    public String getSalesPipelineDataNotes() {
        return salesPipelineDataNotes;
    }

    public void setSalesPipelineDataNotes(String salesPipelineDataNotes) {
        this.salesPipelineDataNotes = salesPipelineDataNotes.trim();
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
