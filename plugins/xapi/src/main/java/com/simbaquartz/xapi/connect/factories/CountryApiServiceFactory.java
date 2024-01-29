package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.common.CommonLookupApiService;
import com.simbaquartz.xapi.connect.api.common.impl.CommonLookupApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-10T09:15:47.472+05:30")
public class CountryApiServiceFactory {

   private final static CommonLookupApiService service = new CommonLookupApiServiceImpl();

   public static CommonLookupApiService getCountryApi()
   {
      return service;
   }
}
