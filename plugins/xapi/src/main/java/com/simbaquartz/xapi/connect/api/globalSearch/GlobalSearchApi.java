package com.simbaquartz.xapi.connect.api.globalSearch;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.factories.GlobalSearchApiServiceFactory;
import com.simbaquartz.xapi.connect.api.globalSearch.model.GlobalSearchCriteria;
import com.simbaquartz.xapi.connect.api.security.Secured;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/globalSearch")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GlobalSearchApi {

    private final static GlobalSearchApiService delegate = GlobalSearchApiServiceFactory.getGlobalSearchApi();

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response globalSearch(@BeanParam GlobalSearchCriteria globalSearchCriteria, @Context SecurityContext securityContext) throws NotFoundException {
        return delegate.globalSearch(globalSearchCriteria,securityContext);
    }
}

