package com.simbaquartz.xapi.connect.api.ping;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class PingApiService implements BaseApiService {

    public abstract Response pingServer(SecurityContext securityContext) throws NotFoundException;

    public abstract Response verifySolr(SecurityContext securityContext) throws NotFoundException;
}
