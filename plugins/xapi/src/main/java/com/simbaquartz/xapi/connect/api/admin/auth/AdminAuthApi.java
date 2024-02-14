package com.simbaquartz.xapi.connect.api.admin.auth;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.security.Secured;
import com.simbaquartz.xapi.connect.factories.AdminAuthApiServiceFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminAuthApi {

    private final AdminAuthApiService delegate = AdminAuthApiServiceFactory.getAdminAuthApi();

    @POST
    @Path("/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticateAdmin(@Context HttpHeaders httpHeaders) throws NotFoundException {
        return delegate.authenticateAdmin(httpHeaders);
    }

    @POST
    @Secured
    @Path("/assume/{account_id}/{user_login_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAssumeToken(
            @PathParam("account_id") String accountId,
            @PathParam("user_login_id") String userLoginId,
            @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.createAssumeToken(accountId, userLoginId, securityContext);
    }
}
