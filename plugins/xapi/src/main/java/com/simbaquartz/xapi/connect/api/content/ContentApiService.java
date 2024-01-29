package com.simbaquartz.xapi.connect.api.content;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

public abstract class ContentApiService implements BaseApiService {

  public abstract Response getThumbNailUrl(String contentId, SecurityContext securityContext)
      throws NotFoundException;

  public abstract Response uploadAttachment(
      MultipartFormDataInput attachment, javax.ws.rs.core.SecurityContext securityContext)
      throws NotFoundException;
}
