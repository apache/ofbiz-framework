package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.tag.TagApiService;
import com.simbaquartz.xapi.connect.api.tag.impl.TagApiServiceImpl;

public class TagApiServiceFactory {

    private final static TagApiService service = new TagApiServiceImpl();

    public static TagApiService getTagApi(){
        return service;
    }
}
