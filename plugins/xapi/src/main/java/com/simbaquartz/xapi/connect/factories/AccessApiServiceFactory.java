package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.access.AccessApiService;
import com.simbaquartz.xapi.connect.api.access.impl.AccessApiServiceImpl;

public class AccessApiServiceFactory {

    private final static AccessApiService service = new AccessApiServiceImpl();

    public static AccessApiService getAccessApi(){ return service; }
}

