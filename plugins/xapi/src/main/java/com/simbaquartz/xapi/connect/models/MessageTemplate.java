package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Created by Admin on 10/17/17.
 */
public class MessageTemplate {

    private String subject = null;

    private String content = null;

    private String isShared = null;

    private String templateName = null;

    private String partyId = null;

    private String messageTemplateId = null;

    @JsonProperty("template_body")
    public String getTemplateBody() {
        return templateBody;
    }

    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
    }

    private String templateBody = null;

    @JsonProperty("message_contents")
    public String getMessageContents() {
        return messageContents;
    }

    public void setMessageContents(String messageContents) {
        this.messageContents = messageContents;
    }

    private String messageContents = null;

    @JsonProperty("recurrence_type_id")
    public String getRecurrenceTypeId() {
        return recurrenceTypeId;
    }

    public void setRecurrenceTypeId(String recurrenceTypeId) {
        this.recurrenceTypeId = recurrenceTypeId;
    }

    private String recurrenceTypeId = null;
    private String weeklyFrequency = null;

    @JsonProperty("weekly_frequency")
    public String getWeeklyFrequency() {
        return weeklyFrequency;
    }

    public void setWeeklyFrequency(String weeklyFrequency) {
        this.weeklyFrequency = weeklyFrequency;
    }

    @JsonProperty("weekly_frequency_time")
    public String getWeeklyFrequencyTime() {
        return weeklyFrequencyTime;
    }

    public void setWeeklyFrequencyTime(String weeklyFrequencyTime) {
        this.weeklyFrequencyTime = weeklyFrequencyTime;
    }

    @JsonProperty("weekly_custom_time")
    public String getWeeklyCustomTime() {
        return weeklyCustomTime;
    }

    public void setWeeklyCustomTime(String weeklyCustomTime) {
        this.weeklyCustomTime = weeklyCustomTime;
    }

    @JsonProperty("list_of_days")
    public String getListOfDays() {
        return listOfDays;
    }

    public void setListOfDays(String listOfDays) {
        this.listOfDays = listOfDays;
    }

    @JsonProperty("monthly_frequency_time")
    public String getMonthlyFrequencyTime() {
        return monthlyFrequencyTime;
    }

    public void setMonthlyFrequencyTime(String monthlyFrequencyTime) {
        this.monthlyFrequencyTime = monthlyFrequencyTime;
    }

    @JsonProperty("monthly_custom_time")
    public String getMonthlyCustomTime() {
        return monthlyCustomTime;
    }

    public void setMonthlyCustomTime(String monthlyCustomTime) {
        this.monthlyCustomTime = monthlyCustomTime;
    }

    private String weeklyFrequencyTime = null;
    private String weeklyCustomTime = null;
    private String listOfDays = null;
    private String monthlyFrequencyTime = null;
    private String monthlyCustomTime = null;

    @JsonProperty("supplier_party_id")
    public String getSupplierPartyId() {
        return supplierPartyId;
    }

    public void setSupplierPartyId(String supplierPartyId) {
        this.supplierPartyId = supplierPartyId;
    }

    @JsonProperty("message_templates")
    public String getMessageTemplates() {
        return messageTemplates;
    }

    public void setMessageTemplates(String messageTemplates) {
        this.messageTemplates = messageTemplates;
    }

    private String supplierPartyId = null;
    private String messageTemplates = null;

    @JsonProperty("data_resource_id")
    public String getDataResourceId() {
        return dataResourceId;
    }

    public void setDataResourceId(String dataResourceId) {
        this.dataResourceId = dataResourceId;
    }

    private String dataResourceId = null;

    @JsonProperty("messageTemplateId")
    public String getMessageTemplateId() {
        return messageTemplateId;
    }

    public void setMessageTemplateId(String messageTemplateId) {
        this.messageTemplateId = messageTemplateId;
    }

    @JsonProperty("isShared")
    public String getIsShared() {
        return isShared;
    }

    public void setIsShared(String isShared) {
        this.isShared = isShared;
    }


    @JsonProperty("partyId")
    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    @JsonProperty("subject")
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @JsonProperty("template_name")
    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
