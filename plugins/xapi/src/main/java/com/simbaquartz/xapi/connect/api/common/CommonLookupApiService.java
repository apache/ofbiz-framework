package com.simbaquartz.xapi.connect.api.common;


import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-10T09:15:47.472+05:30")
public abstract class CommonLookupApiService {
    public abstract Response getColorPalette()
            throws NotFoundException;

    public abstract Response validatePhoneNumber(String countryId, String regionCode, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response validateAddress(PostalAddress address, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getCountryById(String countryId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getCountryList(String storeId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getTimeZoneList(String storeId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getCurrencyList(String storeId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getWeightList(String storeId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getCountryStateList(String countryId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getCountryTelephonicCode(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getDepartments(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getPersonalTitles(String keyword, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getEmailTypes(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getPhoneTypes(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getIndustryTypes(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getCustRequestDocTypes(SecurityContext securityContext);

    public abstract Response getAllLanguages(SecurityContext securityContext);

}
