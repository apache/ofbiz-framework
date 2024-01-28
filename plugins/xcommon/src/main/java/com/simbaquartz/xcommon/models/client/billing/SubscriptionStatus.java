package com.simbaquartz.xcommon.models.client.billing;

public enum SubscriptionStatus {
    ACTIVE("ACTIVE"),
    PAST_DUE("PAST_DUE"),
    CANCELLED("CANCELLED"),
    TRIAL_ENDED("TRIAL_ENDED"),
    EXPIRED("EXPIRED");

    public String statusId;

    SubscriptionStatus(String statusId){
        this.statusId = statusId;
    }

}
