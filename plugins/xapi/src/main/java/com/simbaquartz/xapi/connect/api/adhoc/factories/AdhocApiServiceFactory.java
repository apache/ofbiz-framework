package com.simbaquartz.xapi.connect.api.adhoc.factories;

import com.simbaquartz.xapi.connect.api.adhoc.AdhocApiService;
import com.simbaquartz.xapi.connect.api.adhoc.impl.AdhocApiServiceImpl;

public class AdhocApiServiceFactory {

    private final static AdhocApiService service = new AdhocApiServiceImpl();

    public static AdhocApiService getAdhocApiService() {
        return service;
    }
}
