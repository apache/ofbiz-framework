package com.simbaquartz.xapi.connect.factories;


import com.simbaquartz.xapi.connect.api.content.ContentApiService;
import com.simbaquartz.xapi.connect.api.content.impl.ContentApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public class ContentApiServiceFactory {

    private final static ContentApiService service = new ContentApiServiceImpl();

    public static ContentApiService getContentApi()
    {
        return service;
    }
}
