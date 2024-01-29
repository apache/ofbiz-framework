package com.simbaquartz.xapi.connect;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/xapi/v1")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> resources = new HashSet<Class<?>>();
        resources.add(com.simbaquartz.xapi.connect.api.common.CommonLookupApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.me.MeApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.content.ContentApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.access.AccessApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.ping.PingApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.collection.CollectionApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.globalSearch.GlobalSearchApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.admin.orgOnboard.AdminOrgOnboardApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.admin.auth.AdminAuthApi.class);
        resources.add(com.simbaquartz.xapi.connect.api.admin.user.AdminUserApi.class);
        resources.add(io.swagger.v3.jaxrs2.integration.resources.OpenApiResource.class);
        resources.add(io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource.class);
        resources.add(io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource.class);
        return resources;
    }

}