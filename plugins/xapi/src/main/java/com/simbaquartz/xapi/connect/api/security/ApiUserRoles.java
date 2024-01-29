/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@fidelissd.com>,  May, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xapi.connect.api.security;

/**
 * Created by mande on 5/20/2017.
 */
public enum ApiUserRoles {
    STORE_API_CONSUMER("XCNCTROLE1002"),
    STORE_CUSTOMER("CUSTOMER");

    private String roleId;

    ApiUserRoles(String roleId) {
        this.roleId = roleId;
    }

    public String id() {
        return roleId;
    }
}
