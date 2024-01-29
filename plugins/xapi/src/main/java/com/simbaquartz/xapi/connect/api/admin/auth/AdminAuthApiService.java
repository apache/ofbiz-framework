package com.simbaquartz.xapi.connect.api.admin.auth;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.models.Activity;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class AdminAuthApiService implements BaseApiService {

  public abstract Response authenticateAdmin(HttpHeaders httpHeaders) throws NotFoundException;

  public abstract Response createAssumeToken(
      String tenantId, String userLoginId, SecurityContext securityContext)
      throws NotFoundException;
}
