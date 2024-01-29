package com.simbaquartz.xapi.connect.factories;


import com.simbaquartz.xapi.connect.api.collection.CollectionApiService;
import com.simbaquartz.xapi.connect.api.collection.impl.CollectionApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public class CollectionApiServiceFactory {

    private final static CollectionApiService service = new CollectionApiServiceImpl();

    public static CollectionApiService getCollectionApi()
    {
        return service;
    }
}
