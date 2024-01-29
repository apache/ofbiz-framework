package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.me.MeApiService;
import com.simbaquartz.xapi.connect.api.me.impl.MeApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class MeApiServiceFactory {

   private final static MeApiService service = new MeApiServiceImpl();

   public static MeApiService getMeApi()
   {
      return service;
   }
}
