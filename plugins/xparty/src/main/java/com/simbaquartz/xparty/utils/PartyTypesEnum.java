package com.simbaquartz.xparty.utils;

/**
 * Represents the types of Parties available in the system.
 */
public enum PartyTypesEnum {
    PERSON ("A person", "PERSON"),
    COMPANY ("A company", "PARTY_GROUP"),
    APP_ACCOUNT ("An application account", "APP_ACCOUNT"),
    TEAM ("A team", "TEAM");

    private final String description;
    private final String partyTypeId;

    PartyTypesEnum(String value, String partyTypeId)
    {
        this.description = value;
        this.partyTypeId = partyTypeId;
    }

    public String getDescription() {
        return description;
    }
    public String getPartyTypeId()
    {
        return partyTypeId;
    }
}
