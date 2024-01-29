package com.simbaquartz.xapi.connect.api.me;

import com.fidelissd.zcp.xcommon.models.account.ApplicationUser;
import com.fidelissd.zcp.xcommon.models.client.billing.Subscription;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.security.Secured;
import com.simbaquartz.xapi.connect.factories.MeApiServiceFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/me")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MeApi {
    private final MeApiService delegate = MeApiServiceFactory.getMeApi();

/*

  private final PartyTasksApiService partyTasksApiService =
      PartyApiServiceFactory.getPartyTasksApiService();
  private final PartyContactDetailsApiService partyContactDetailsApiService =
      PartyApiServiceFactory.getPartyContactDetailsApiService();
  private final PartyContentsApiService partyContentsApiService =
      PartyApiServiceFactory.getPartyContentsApi();
  private final UserApiService userApiService = UserApiServiceFactory.getUserApi();
*/

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response meGet(@Context SecurityContext securityContext) throws NotFoundException {
        return delegate.meGet(securityContext);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response meUpdate(
            ApplicationUser userDetailsToUpdate, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.meUpdate(userDetailsToUpdate, securityContext);
    }

    @POST
    @Path("/subscription")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(
            Subscription userSubscription, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.createSubscription(userSubscription, securityContext);
    }

    @POST
    @Path("/connectGmail")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response connectGoogleAccount(
            ApplicationUser user, @Context SecurityContext securityContext) throws NotFoundException {
        return delegate.connectGoogleAccount(user, securityContext);
    }

    @GET
    @Path("/isGoogleAuthorized")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response isGoogleAuthorized(@Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.isGoogleAuthorized(securityContext);
    }

    @POST
    @Path("/disconnectGoogleAuth")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnectGoogleAuth(@Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.disconnectGoogleAuth(securityContext);
    }

    @POST
    @Path("/disconnectSlack")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnectSlackAuth(@Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.disconnectSlackAuth(securityContext);
    }

    @GET
    @Path("/slack/test-message")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendSlackTestMessage(@Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.sendSlackTestMessage(securityContext);
    }
}
