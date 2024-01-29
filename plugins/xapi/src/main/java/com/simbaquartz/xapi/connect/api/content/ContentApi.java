package com.simbaquartz.xapi.connect.api.content;

import com.simbaquartz.xapi.connect.factories.ContentApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


@Secured
@Path("/content")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public class ContentApi {
    private final ContentApiService delegate = ContentApiServiceFactory.getContentApi();


    @GET
    @Path("/{content_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getThumbNailUrl(@PathParam("content_id") String contentId, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        if (UtilValidate.isEmpty(contentId)) {
            String errorMessage = "Content id is null, please provide a valid content id to proceed.";
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, errorMessage, "name");
        }
        return delegate.getThumbNailUrl(contentId, securityContext);
    }


    @POST
    @Path("/upload/photo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadAttachment(MultipartFormDataInput attachment, @Context javax.ws.rs.core.SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.uploadAttachment(attachment, securityContext);
    }

    @POST
    @Path("/files/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(MultipartFormDataInput attachment, @Context javax.ws.rs.core.SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.uploadAttachment(attachment, securityContext);
    }

}

