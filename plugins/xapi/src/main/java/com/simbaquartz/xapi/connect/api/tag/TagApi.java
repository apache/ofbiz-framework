package com.simbaquartz.xapi.connect.api.tag;

import com.simbaquartz.xapi.connect.factories.TagApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;
import com.simbaquartz.xapi.connect.models.Tag;
import org.apache.ofbiz.entity.GenericEntityException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/tags")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TagApi {

    private final TagApiService delegate = TagApiServiceFactory.getTagApi();

    /**
     * Returns the list of all tags and have option to filter it by type
     *
     * @param tagTypeId
     * @param securityContext
     * @return List<Tag>
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTags(@QueryParam("type") String tagTypeId, @Context SecurityContext securityContext){
        return delegate.getAllTags(tagTypeId, securityContext);
    }

    /**
     * Use to Create, tag name and type are compulsory inputs
     *
     * @param tag
     * @param securityContext
     * @return tag
     * @throws GenericEntityException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTag(Tag tag, @Context SecurityContext securityContext) throws GenericEntityException {
        return delegate.createTag(tag, securityContext);
    }

    /**
     * Use to update tag and id is compulsory input
     *
     * @param tag
     * @param tagId
     * @param securityContext
     * @return
     */
    @PUT
    @Path("/{tagId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTag(@PathParam("tagId") String tagId, Tag tag, @Context SecurityContext securityContext) {
        return delegate.updateTag(tagId, tag, securityContext);
    }

    /**
     * Use to delete tag and id is compulsory input, This method also clear
     * all the related entity data.
     *
     * @param tagId
     * @param securityContext
     * @return
     */
    @DELETE
    @Path("/{tagId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTag(@PathParam("tagId") String tagId, @Context SecurityContext securityContext) {
        return delegate.deleteTag(tagId, securityContext);
    }

}
