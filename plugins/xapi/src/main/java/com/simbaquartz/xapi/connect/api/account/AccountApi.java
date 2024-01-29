package com.simbaquartz.xapi.connect.api.account;

import com.fidelissd.zcp.xcommon.models.account.ApplicationAccount;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUserSearchCriteria;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.account.factory.AccountApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;
import org.apache.ofbiz.base.util.UtilValidate;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AccountApi {

    private final AccountApiService delegate = AccountApiServiceFactory.getAccountApi();

    /**
     * Creates and application account for mmo application.
     *
     * @param applicationAccount
     * @return
     * @throws NotFoundException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApplicationAccount(@Valid ApplicationAccount applicationAccount)
            throws NotFoundException {
        return delegate.createApplicationAccount(applicationAccount);
    }

    @GET
    @Path("/emails/{email_id}/check-existing")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkDuplicate(@PathParam("email_id") String emailId) throws NotFoundException {
        return delegate.checkDuplicate(emailId);
    }

    /**
     * Secured end point that returns the list of account members available under the account with
     * their licensing details. Optional filters email and name available to filter the records.
     *
     * @param accountId
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @Secured
    @GET
    @Path("/{account_id}/members")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAccountMembers(
            @PathParam("account_id") String accountId,
            @BeanParam ApplicationUserSearchCriteria applicationUserSearchCriteria,
            @Context SecurityContext securityContext)
            throws NotFoundException {
        if (UtilValidate.isEmpty(applicationUserSearchCriteria)) {
            applicationUserSearchCriteria = new ApplicationUserSearchCriteria();
        }

        return delegate.listAccountMembers(accountId, applicationUserSearchCriteria, securityContext);
    }

    /**
     * To get all members.
     *
     * @return result List of employees
     */
    @GET
    @Path("/members/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMembers(@Context SecurityContext securityContext) throws NotFoundException {
        return delegate.getAllMembers(securityContext);
    }

    /**
     * Off-board account member.
     *
     * @return
     */
    @Secured
    @DELETE
    @Path("/members/{member_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAccountMember(
            @PathParam("member_id") String partyId, @Context SecurityContext securityContext)
            throws NotFoundException {
        return delegate.removeAccountMember(partyId, securityContext);
    }


}
