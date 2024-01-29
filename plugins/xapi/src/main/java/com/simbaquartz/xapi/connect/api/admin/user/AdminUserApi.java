package com.simbaquartz.xapi.connect.api.admin.user;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.factories.AdminUserApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.AdminSecured;
import com.simbaquartz.xapi.connect.models.Userlogin;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


@AdminSecured
@Path("/admin/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminUserApi {

    private final AdminUserApiService delegate = AdminUserApiServiceFactory.getAdminUserApiService();

    /** Endpoint to create a new Admin User (ZCP Admin)
     *
     * @param userlogin
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewAdminUser(Userlogin userlogin, @Context SecurityContext securityContext) throws NotFoundException {
        return delegate.createAdminUser(userlogin,securityContext);
    }
}
