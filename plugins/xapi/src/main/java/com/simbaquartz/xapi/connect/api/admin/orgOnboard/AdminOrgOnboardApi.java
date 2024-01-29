package com.simbaquartz.xapi.connect.api.admin.orgOnboard;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.factories.AdminOrgOnboardApiServiceFactory;
import com.fidelissd.zcp.xcommon.models.client.Onboard;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin/org")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminOrgOnboardApi {

    private final AdminOrgOnboardApiService delegate = AdminOrgOnboardApiServiceFactory.getOnboardingApiService();

    /**
     * Endpoint to create a new Client
     * This does the onboarding process by creating a new admin and org
     *
     * @param onboard
     * @return
     * @throws NotFoundException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response onboardOrgAdmin(Onboard onboard) throws NotFoundException {
        return delegate.onboardOrgAdmin(onboard);
    }

}
