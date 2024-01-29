package com.simbaquartz.xapi.connect.api.adhoc;

import com.simbaquartz.xapi.connect.api.adhoc.factories.AdhocApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/adhoc")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdhocApi {
    private final AdhocApiService adhocService = AdhocApiServiceFactory.getAdhocApiService();

    /**
     * add organization Id to time-entries
     *
     * @param securityContext
     * @return
     */
    @PUT
    @Path("/timeentries/add-organization")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addOrganizationIdToTimeEntries(@Context SecurityContext securityContext) {
        return adhocService.addOrganizationIdToTimeEntries(securityContext);
    }

    @PUT
    @Path("/timeentries/bulk-update-is-synced")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkUpdateIsSyncedFlagForTimeEntries(@Context SecurityContext securityContext) {
        return adhocService.bulkUpdateIsSyncedFlagForTimeEntries(securityContext);
    }
}

