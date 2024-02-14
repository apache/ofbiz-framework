package com.simbaquartz.xapi.connect.api.access;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.factories.AccessApiServiceFactory;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import org.apache.ofbiz.base.util.UtilValidate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/access")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AccessApi {

    private final AccessApiService delegate = AccessApiServiceFactory.getAccessApi();

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response logoutUser(@Context HttpHeaders httpHeaders) throws NotFoundException {
        return delegate.logoutUser(httpHeaders);
    }

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createToken(@Context HttpHeaders httpHeaders) throws NotFoundException {
        return delegate.createToken(httpHeaders);
    }

    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRefreshToken(@Context HttpHeaders httpHeaders) throws NotFoundException {
        return delegate.createRefreshToken(httpHeaders);
    }

    @POST
    @Path("/verify/token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyAccessToken(@Context HttpHeaders httpHeaders) throws NotFoundException {
        return delegate.verifyAccessToken(httpHeaders);
    }

    @POST
    @Path("/verify/token/google")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyGoogleAccessToken(@QueryParam("token") String tokenToVerify,
                                            @Context HttpServletRequest request,
                                            @Context HttpServletResponse response, @Context SecurityContext securityContext)
            throws NotFoundException {
        if (UtilValidate.isEmpty(tokenToVerify)) {
            return ApiResponseUtil.prepareDefaultResponse(BAD_REQUEST, "Token is missing!", "token");
        }
        return delegate.verifyGoogleAccessToken(tokenToVerify, request, response, securityContext);
    }
}
