package com.fidelissd.zcp.xcommon.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Types of customer request documents that can be uploaded, examples include General Document or Supporting Document
 */
public enum CustomerRequestDocumentTypeEnum {
    GENERAL_DOC("GENERAL_DOC", "General document for customer request");

    private String typeId;
    private String description;

    CustomerRequestDocumentTypeEnum(String typeId, String description) {
        this.typeId = typeId;
        this.description = description;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Finds and returns a valid enum using typeId.
     *
     * @param typeId
     * @return CustomerRequestDocumentTypeEnum
     */
    public static CustomerRequestDocumentTypeEnum findByTypeId(String typeId) {
        CustomerRequestDocumentTypeEnum result = null;
        for (CustomerRequestDocumentTypeEnum membershipType : values()) {
            if (membershipType.getTypeId().equalsIgnoreCase(typeId)) {
                result = membershipType;
                break;
            }
        }

        return result;
    }

    /**
     * Returns an array of valid type ids. Can be used to display the list of valid type ids.
     *
     * @return
     */
    public static List<String> listValidTypeIds() {
        List<String> validMembershipTypeIds = new ArrayList<>();
        for (CustomerRequestDocumentTypeEnum documentType : values()) {
            validMembershipTypeIds.add(documentType.getTypeId());
        }

        return validMembershipTypeIds;
    }
}
