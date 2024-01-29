package com.simbaquartz.xapi.connect.factories;

import com.simbaquartz.xapi.connect.api.department.DepartmentApiService;
import com.simbaquartz.xapi.connect.api.department.impl.DepartmentApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")

public class DepartmentApiServiceFactory {
    private final static DepartmentApiService service = new DepartmentApiServiceImpl();

    public static DepartmentApiService getDepartmentApi()
    {
        return service;
    }
}
