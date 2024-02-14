package com.simbaquartz.xapi.connect.api.admin.orgOnboard;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.factories.AdminOrgOnboardApiServiceFactory;
import com.simbaquartz.xapi.connect.api.admin.models.Onboard;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin/organizations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminOrgOnboardApi {

    private final AdminOrgOnboardApiService delegate = AdminOrgOnboardApiServiceFactory.getOnboardingApiService();

    /**
     * Endpoint to create a new Client
     * This does the onboarding process by creating a new Organization party and Admin for the org
     *
     * @param onboard request object
     * @return API response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response onboardNewOrg(Onboard onboard) {
        return delegate.onboardNewOrg(onboard);
    }

}
