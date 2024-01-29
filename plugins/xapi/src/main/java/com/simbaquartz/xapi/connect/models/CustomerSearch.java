/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@fidelissd.com>,  September, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by mande on 9/9/2017.
 */
public class CustomerSearch {

    @JsonProperty("start_index")
    private Integer startIndex = null;

    @JsonProperty("view_size")
    private Integer viewSize = null;

    @JsonProperty("facility_type_ids")
    private List facilityTypeIds = null;

    @JsonProperty("name")
    private String customerName = null;

    @JsonProperty("sort_by")
    private String sortBy = null;

    @JsonProperty("party_ids")
    private List partyIds = null;

    @JsonProperty("state_province_geo_ids")
    private List partyStateProvinceGeoIds = null;

    @JsonProperty("address")
    private String address = null;

    @JsonProperty("country_geo_ids")
    private List partyCountryGeoIds = null;

    @JsonProperty("postal_code")
    private List postalCode = null;


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public List getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(List postalCode) {
        this.postalCode = postalCode;
    }


    public List getPartyCountryGeoId() {
        return partyCountryGeoIds;
    }

    public void setPartyCountryGeoId(List partyCountryGeoId) {
        this.partyCountryGeoIds = partyCountryGeoId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List getFacilityTypeIds() {
        return facilityTypeIds;
    }

    public void setFacilityTypeIds(List facilityTypeIds) {
        this.facilityTypeIds = facilityTypeIds;
    }

    public List getPartyIds() {
        return partyIds;
    }

    public void setPartyIds(List partyIds) {
        this.partyIds = partyIds;
    }


    public List getPartyStateProvinceGeoIds() {
        return partyStateProvinceGeoIds;
    }

    public void setPartyStateProvinceGeoIds(List partyStateProvinceGeoIds) {
        this.partyStateProvinceGeoIds = partyStateProvinceGeoIds;
    }

    /**
     * Sort by Field name
     * @return
     */
    public String getSortBy() {return sortBy;}
    public void setSortBy(String sortBy) {this.sortBy = sortBy;}


    /**
     * start index
     **/
    public Integer getStartIndex() {
        return startIndex;
    }
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * Customer list size
     **/
    public Integer getViewSize() {
        return viewSize;
    }
    public void setViewSize(Integer viewSize) {
        this.viewSize = viewSize;
    }

}
