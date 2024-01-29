package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.ping.PingApiService;
import com.simbaquartz.xapi.connect.api.ping.impl.PingApiServiceImpl;

public class PingApiServiceFactory {

    private final static PingApiService service = new PingApiServiceImpl();

    public static PingApiService getPingApi(){ return service; }
}

