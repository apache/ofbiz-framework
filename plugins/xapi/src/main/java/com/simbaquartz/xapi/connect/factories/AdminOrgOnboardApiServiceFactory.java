package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.admin.orgOnboard.AdminOrgOnboardApiService;
import com.simbaquartz.xapi.connect.api.admin.orgOnboard.impl.AdminOrgOnboardApiServiceImpl;

public class AdminOrgOnboardApiServiceFactory {

    private final static AdminOrgOnboardApiService service = new AdminOrgOnboardApiServiceImpl();

    public static AdminOrgOnboardApiService getOnboardingApiService(){ return service; }
}

