package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.admin.user.AdminUserApiService;
import com.simbaquartz.xapi.connect.api.admin.user.impl.AdminUserApiServiceImpl;

public class AdminUserApiServiceFactory {

    private final static AdminUserApiService service = new AdminUserApiServiceImpl();

    public static AdminUserApiService getAdminUserApiService(){ return service; }
}

