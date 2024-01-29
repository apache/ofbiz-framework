package com.simbaquartz.xapi.connect.api.access;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class AccessApiService implements BaseApiService {

  public abstract Response logoutUser(HttpHeaders httpHeaders) throws NotFoundException;

  public abstract Response createToken(HttpHeaders httpHeaders)
      throws NotFoundException;

  public abstract Response createRefreshToken(HttpHeaders httpHeaders) throws NotFoundException;

  public abstract Response verifyAccessToken(HttpHeaders httpHeaders) throws NotFoundException;

  public abstract Response verifyGoogleAccessToken(
      String tokenToVerify,
      HttpServletRequest request,
      HttpServletResponse response,
      SecurityContext securityContext)
      throws NotFoundException;
}
