package com.simbaquartz.xapi.connect.api.tag;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.models.Tag;
import org.apache.ofbiz.entity.GenericEntityException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class TagApiService implements BaseApiService {
    public abstract Response getAllTags(String tagTypeId, SecurityContext securityContext);

    public abstract Response createTag(Tag tag, SecurityContext securityContext) throws GenericEntityException;

    public abstract Response updateTag(String tagId, Tag tag, SecurityContext securityContext);

    public abstract Response deleteTag(String tagId, SecurityContext securityContext);

}
