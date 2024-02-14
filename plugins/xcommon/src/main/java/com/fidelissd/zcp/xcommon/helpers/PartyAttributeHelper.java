/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@simbacart.com>,  June, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.fidelissd.zcp.xcommon.helpers;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class to easily manage Party-attributes
 * Bulk create attributes, fetch attributes etc
 */
@Builder
@Data
@AllArgsConstructor
public class PartyAttributeHelper {
    private static final String module = PartyAttributeHelper.class.getName();
    private final LocalDispatcher dispatcher;
    private final Delegator delegator;
    private final GenericValue userLogin;

    public PartyAttributeHelper(LocalDispatcher dispatcher, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        this.userLogin = userLogin;
    }

    /**
     * Create or Update PartyAttribute entries for given attributes map
     *
     * @param partyId party Id
     * @param attributes key value pair of attributes
     * @return collection of party attribute GVs
     */
    public List<GenericValue> createOrUpdateAttributes(String partyId, Map<String, String> attributes) {
        List<GenericValue> partyAttrGvs = new ArrayList<>();

        return partyAttrGvs;
    }


}