package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.access.AccessApiService;
import com.simbaquartz.xapi.connect.api.access.impl.AccessApiServiceImpl;
import com.simbaquartz.xapi.connect.api.admin.auth.AdminAuthApiService;
import com.simbaquartz.xapi.connect.api.admin.auth.impl.AdminAuthApiServiceImpl;

public class AdminAuthApiServiceFactory {

    private final static AdminAuthApiService service = new AdminAuthApiServiceImpl();

    public static AdminAuthApiService getAdminAuthApi(){ return service; }
}

