package com.simbaquartz.xapi.connect.api.application.factory;

import com.simbaquartz.xapi.connect.api.application.ApplicationApiService;
import com.simbaquartz.xapi.connect.api.application.impl.ApplicationApiServiceImpl;

public class ApplicationApiServiceFactory {
  private static final ApplicationApiService applicationApiService = new ApplicationApiServiceImpl();

  public static ApplicationApiService getApplicationApiService() {
    return applicationApiService;
  }
}
