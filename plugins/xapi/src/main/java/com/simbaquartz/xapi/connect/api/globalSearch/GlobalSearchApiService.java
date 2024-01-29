package com.simbaquartz.xapi.connect.api.globalSearch;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.globalSearch.model.GlobalSearchCriteria;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class GlobalSearchApiService implements BaseApiService {

    public abstract Response globalSearch(GlobalSearchCriteria globalSearchCriteria, @Context SecurityContext securityContext) throws NotFoundException;
}

