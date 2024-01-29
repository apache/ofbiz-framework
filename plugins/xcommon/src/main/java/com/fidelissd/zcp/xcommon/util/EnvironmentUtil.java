package com.fidelissd.zcp.xcommon.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.LocalDispatcher;

/** Helps detect the runtime environment of the application, dev/test/prod. */
public enum EnvironmentUtil {
  PROD("prod"),
  TEST("test"),
  DEV("dev");
  private static GenericDelegator delegator =
      (GenericDelegator) DelegatorFactory.getDelegator("default");
  private static LocalDispatcher dispatcher =
      new GenericDispatcherFactory().createLocalDispatcher("default", delegator);

  private String hostname;
  public static final String module = EnvironmentUtil.class.getName();

  EnvironmentUtil(String s) {
    hostname = s;
  }

  public static EnvironmentUtil detectEnvironment() throws UnknownHostException {
    //String environmentValue = getEnvironmentVariable("OFBIZ_ENV");
    String environmentValue = EntityUtilProperties.getPropertyValue("appconfig", "app.system.env", delegator);
    if(UtilValidate.isEmpty(environmentValue)) {
      environmentValue = "dev";
    }

    Debug.logInfo(
        "******* Environment Variable OFBIZ_ENV value is ******** " + environmentValue, module);
    switch (environmentValue) {
      case "test":
        return EnvironmentUtil.TEST;
      case "prod":
        return EnvironmentUtil.PROD;
      case "ofbiz":
        return EnvironmentUtil.PROD;
        // Add local host names here:
      case "dev":
      case "localhost":
      default:
        return EnvironmentUtil.DEV;
    }
  }

  public static EnvironmentUtil detectEnvironmentUsingHostName() throws UnknownHostException {
    InetAddress addr;
    addr = InetAddress.getLocalHost();
    Debug.logInfo("******* Host Address ******** " + addr, module);
    switch (addr.getHostName()) {
      case "test":
        return EnvironmentUtil.TEST;
      case "prod":
        return EnvironmentUtil.PROD;
      case "ofbiz":
        return EnvironmentUtil.PROD;
        // Add local host names here:
      case "localhost":
      default:
        return EnvironmentUtil.DEV;
    }
  }

  public static String getEnvironmentKey() throws Exception {
    EnvironmentUtil environment;
    try {
      environment = EnvironmentUtil.detectEnvironment();
      switch (environment) {
        case TEST:
          return "test";
        case DEV:
          return "dev";
        case PROD:
          // We're in a PROD environment
          return "prod";
        default:
          // default to Dev
          return "dev";
      }
    } catch (UnknownHostException e) {
      Debug.logError(e, e.getMessage(), module);
      return "dev";
    }
  }

  public static Boolean isDevelopmentMode() {
    try {
      return DEV.equals(EnvironmentUtil.detectEnvironment());
    } catch (UnknownHostException e) {
      Debug.logError(e, e.getMessage(), module);
    }
    return false;
  }

  public static Boolean isTestingMode() {
    try {
      return TEST.equals(EnvironmentUtil.detectEnvironment());
    } catch (UnknownHostException e) {
      Debug.logError(e, e.getMessage(), module);
    }
    return false;
  }

  public static Boolean isProductionMode() {
    try {
      return PROD.equals(EnvironmentUtil.detectEnvironment());
    } catch (UnknownHostException e) {
      Debug.logError(e, e.getMessage(), module);
    }
    return false;
  }

  public static String getEnvironmentVariable(String key) {
    String envHostType = System.getenv(key);
    if (UtilValidate.isNotEmpty(envHostType)) {
      return envHostType;
    }
    return "dev";
  }

  public static String getServerRootUrl() {
    String serverRootUrl = "";
    Delegator delegator = DelegatorFactory.getDelegator("default");
    try {
      String environment = getEnvironmentVariable("ENV");
      if (UtilValidate.isEmpty(environment)) {
        serverRootUrl =
            EntityUtilProperties.getPropertyValue("appconfig", "app.domain.url", delegator);
      } else {
        serverRootUrl =
            EntityUtilProperties.getPropertyValue(
                "appconfig", "app.domain.url." + environment, delegator);
      }
    } catch (Exception e) {
      Debug.logError(e, module);
    }

    return serverRootUrl;
  }
}
