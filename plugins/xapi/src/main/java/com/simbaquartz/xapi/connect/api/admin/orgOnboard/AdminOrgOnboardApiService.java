package com.simbaquartz.xapi.connect.api.admin.orgOnboard;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.fidelissd.zcp.xcommon.models.client.Onboard;

import javax.ws.rs.core.Response;

public abstract class AdminOrgOnboardApiService implements BaseApiService {
    public abstract Response onboardOrgAdmin(Onboard onboard) throws NotFoundException;
}