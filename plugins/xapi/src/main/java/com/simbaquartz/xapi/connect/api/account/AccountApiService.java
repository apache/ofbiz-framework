package com.simbaquartz.xapi.connect.api.account;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.fidelissd.zcp.xcommon.models.account.ApplicationAccount;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUserSearchCriteria;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class AccountApiService implements BaseApiService {
    public abstract Response createApplicationAccount(ApplicationAccount userAccount)
            throws NotFoundException;

    public abstract Response checkDuplicate(String emailId) throws NotFoundException;

    public abstract Response listAccountMembers(
            String accountId,
            ApplicationUserSearchCriteria applicationUserSearchCriteria,
            SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response changePermissions(
            String partyId, ApplicationAccount applicationAccount, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getPermissions(String partyId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getAllMembers(SecurityContext securityContext) throws NotFoundException;

    public abstract Response removeAccountMember(String partyId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response getAccountStorageDetails(SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response bulkImportCustomerRequests(String accountId, MultipartFormDataInput attachment, SecurityContext securityContext)
            throws NotFoundException;
}
