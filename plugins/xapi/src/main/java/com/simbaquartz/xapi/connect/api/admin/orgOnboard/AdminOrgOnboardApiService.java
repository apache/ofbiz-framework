package com.simbaquartz.xapi.connect.api.admin.orgOnboard;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.admin.models.Onboard;

import javax.ws.rs.core.Response;

public abstract class AdminOrgOnboardApiService implements BaseApiService {
    public abstract Response onboardNewOrg(Onboard onboard);
}