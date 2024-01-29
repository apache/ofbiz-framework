package com.simbaquartz.xapi.connect.api.adhoc;

import com.simbaquartz.xapi.connect.api.BaseApiService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class AdhocApiService implements BaseApiService {
    public abstract Response addOrganizationIdToTimeEntries(SecurityContext securityContext);

    public abstract Response bulkUpdateIsSyncedFlagForTimeEntries(SecurityContext securityContext);
}
