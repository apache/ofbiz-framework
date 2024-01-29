package com.simbaquartz.xapi.connect.api.application;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class ApplicationApiService implements BaseApiService {

  public abstract Response getApplicationDetails(
      String applicationId, SecurityContext securityContext) throws NotFoundException;
}
