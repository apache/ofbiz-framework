package com.simbaquartz.xapi.connect.api.application.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.application.ApplicationApiService;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class ApplicationApiServiceImpl extends ApplicationApiService {
  private static final String module = ApplicationApiServiceImpl.class.getName();

  @Override
  public Response getApplicationDetails(String applicationId, SecurityContext securityContext)
      throws NotFoundException {
    return ApiResponseUtil.notImplemented();
  }
}
