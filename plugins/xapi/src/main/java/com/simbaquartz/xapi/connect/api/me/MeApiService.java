package com.simbaquartz.xapi.connect.api.me;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUser;
import com.fidelissd.zcp.xcommon.models.client.billing.Subscription;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public abstract class MeApiService {
    public abstract Response meGet(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response meUpdate(ApplicationUser userDetailsToUpdate, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response createSubscription(Subscription userSubscription, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response connectGoogleAccount(ApplicationUser user, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response isGoogleAuthorized(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response disconnectGoogleAuth(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response disconnectSlackAuth(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response sendSlackTestMessage(SecurityContext securityContext);
}
