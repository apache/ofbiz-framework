package com.simbaquartz.xapi.connect.api.collection;

import com.simbaquartz.xapi.connect.factories.CollectionApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;
import com.simbaquartz.xapi.connect.models.collection.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


@Secured
@Path("/collection")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public class CollectionApi {
    private final CollectionApiService delegate = CollectionApiServiceFactory.getCollectionApi();


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCollection(Collection collection, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.createCollection(collection, securityContext);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCollection(Collection collection, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.updateCollection(collection, securityContext);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeCollection(Collection collection, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.removeCollection(collection, securityContext);
    }

    @GET
    @Path("/collectionItem")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchCollection(@PathParam("collection_type_id") String collectionTypeId, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.fetchCollection(collectionTypeId, securityContext);
    }

    @POST
    @Path("/collectionItem")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addItemsToCollection(Collection collection, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.addItemsToCollection(collection, securityContext);
    }

    @DELETE
    @Path("/collectionItem")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeItemsFromCollection(Collection collection, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.removeItemsFromCollection(collection, securityContext);
    }

}

