package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.globalSearch.GlobalSearchApiService;
import com.simbaquartz.xapi.connect.api.globalSearch.impl.GlobalSearchApiServiceImpl;

public class GlobalSearchApiServiceFactory {

    private final static GlobalSearchApiService service = new GlobalSearchApiServiceImpl();

    public static GlobalSearchApiService getGlobalSearchApi(){
        return service;
    }
}
