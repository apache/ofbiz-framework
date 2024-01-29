package com.simbaquartz.xapi.connect.api.application;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.application.factory.ApplicationApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/applications")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationApi {
  private static final String module = ApplicationApi.class.getName();
  private final ApplicationApiService delegate = ApplicationApiServiceFactory.getApplicationApiService();
  /**
   * API to get a application details with application id
   *
   * @param applicationId
   * @param securityContext
   * @return
   * @throws NotFoundException
   */
  @GET
  @Path("/{application_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApplicationDetails(
      @PathParam("application_id") String applicationId, @Context SecurityContext securityContext)
      throws NotFoundException {
    return delegate.getApplicationDetails(applicationId, securityContext);
  }


}
