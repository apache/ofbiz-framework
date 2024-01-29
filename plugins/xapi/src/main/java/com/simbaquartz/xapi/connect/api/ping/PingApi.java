package com.simbaquartz.xapi.connect.api.ping;

import com.simbaquartz.xapi.connect.factories.PingApiServiceFactory;
import com.simbaquartz.xapi.connect.api.NotFoundException;

import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/ping")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PingApi {

    private final PingApiService delegate = PingApiServiceFactory.getPingApi();

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pingServer(@Context SecurityContext securityContext) throws NotFoundException {
        return delegate.pingServer(securityContext);
    }

    @GET
    @Path("/400")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnErrorGet(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.badRequestResponse("Please check your input and try again");
    }

    @POST
    @Path("/400")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnErrorPost(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.badRequestResponse("Please check your input and try again");
    }

    @PUT
    @Path("/400")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnErrorPut(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.badRequestResponse("Please check your input and try again");
    }

    @Path("/400")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnErrorDelete(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.badRequestResponse("Please check your input and try again");
    }

    @GET
    @Path("/500")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnServerErrorGet(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.serverErrorResponse("Please check your input and try again");
    }

    @POST
    @Path("/500")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnServerErrorPost(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.serverErrorResponse("Please check your input and try again");
    }

    @PUT
    @Path("/500")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnServerErrorPut(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.serverErrorResponse("Please check your input and try again");
    }

    @Path("/500")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnServerErrorDelete(@Context SecurityContext securityContext) throws NotFoundException {
        return ApiResponseUtil.serverErrorResponse("Please check your input and try again");
    }

    @Path("/solr")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response verifySolrConnection(@Context SecurityContext securityContext) throws NotFoundException {
        return delegate.verifySolr(securityContext);
    }
}
