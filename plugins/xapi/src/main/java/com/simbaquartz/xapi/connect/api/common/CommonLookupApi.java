package com.simbaquartz.xapi.connect.api.common;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.factories.CountryApiServiceFactory;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.ofbiz.base.util.UtilValidate;
import org.jboss.resteasy.annotations.GZIP;

@Path("/common")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-10T09:15:47.472+05:30")
public class CommonLookupApi {
    private final CommonLookupApiService delegate = CountryApiServiceFactory.getCountryApi();

    /**
     * Returns the list of available color pallete
     * @return
     * @throws NotFoundException
     */
    @GET
    @GZIP
    @Path("/colors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getColorPalette()
            throws NotFoundException {

        return delegate.getColorPalette();
    }

    /**
     * Validates a phone number.
     * @param phoneNumber
     * @param regionCode
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @GET
    @GZIP
    @Path("/phone/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validatePhoneNumber(@QueryParam("number") String phoneNumber, @QueryParam("country") String regionCode, @Context SecurityContext securityContext)
            throws NotFoundException {
        if(UtilValidate.isEmpty(phoneNumber)){
            return ApiResponseUtil.badRequestResponse("Phone number is required via number parameter.", "number");
        }
        if(UtilValidate.isEmpty(regionCode)){
            regionCode = "US";
        }

        return delegate.validatePhoneNumber(phoneNumber, regionCode, securityContext);
    }

    @POST
    @GZIP
    @Path("/address/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateAddress(@Valid PostalAddress address, @Context SecurityContext securityContext)
            throws NotFoundException {
        if(UtilValidate.isEmpty(address)){
            return ApiResponseUtil.badRequestResponse("Address is missing, please verify!");
        }
        return delegate.validateAddress(address, securityContext);
    }


    @GET
    @GZIP
    @Path("/country")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountryList(@QueryParam("store_id") String storeId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getCountryList(storeId, securityContext);
    }

    @GET
    @GZIP
    @Path("/country/{country_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountryById(@PathParam("country_id") String countryId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getCountryById(countryId, securityContext);
    }

    //gettimezones
    @GET
    @GZIP
    @Path("/timezones")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeZoneList(@QueryParam("store_id") String storeId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getTimeZoneList(storeId, securityContext);
    }

    //weight_unit
    @GET
    @GZIP
    @Path("/weight")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeightList(@QueryParam("store_id") String storeId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getWeightList(storeId, securityContext);
    }

    //currency
    @GET
    @GZIP
    @Path("/currency")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrencyList(@QueryParam("store_id") String storeId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getCurrencyList(storeId, securityContext);
    }

    //states of countries
    @GET
    @GZIP
    @Path("/country/{countryId}/provinces")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountryStateList(@PathParam("countryId") String countryId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getCountryStateList(countryId, securityContext);

    }

    @GET
    @GZIP
    @Path("/country/telephonicCode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCountryTelephonicCode(@Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.getCountryTelephonicCode(securityContext);
    }

    @GET
    @Path("/departments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDepartments(@Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getDepartments(securityContext);
    }

    @GET
    @Path("/personal/titles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPersonalTitles(@QueryParam("keyword") String keyword, @Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getPersonalTitles(keyword, securityContext);
    }

    @GET
    @Path("/email/types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmailTypes(@Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getEmailTypes(securityContext);
    }

    @GET
    @Path("/phone/types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPhoneTypes(@Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getPhoneTypes(securityContext);
    }

    /**
     * Returns the type of industries, seed data is available in
     * @see plugins/xstore/data/XstoreTypeData.xml
     *
     * @param securityContext
     * @return
     * @throws com.simbaquartz.xapi.connect.api.NotFoundException
     */
    @GET
    @Path("/industryTypes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIndustryTypes(@Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getIndustryTypes(securityContext);
    }

    @GET
    @Path("/cust-request/documents/types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustRequestDocTypes(@Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getCustRequestDocTypes(securityContext);
    }


    @GET
    @Path("/languages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLanguages(@Context SecurityContext securityContext)
            throws com.simbaquartz.xapi.connect.api.NotFoundException {
        return delegate.getAllLanguages(securityContext);
    }

}
