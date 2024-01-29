package com.simbaquartz.xapi.connect.api.admin.user;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.models.Userlogin;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class AdminUserApiService implements BaseApiService {

    public abstract Response createAdminUser(Userlogin userlogin, SecurityContext securityContext) throws NotFoundException;

}
