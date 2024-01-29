package com.simbaquartz.xapi.connect.api.collection;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.models.collection.Collection;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public abstract class CollectionApiService implements BaseApiService{

   public abstract Response createCollection(Collection collection, SecurityContext securityContext)
            throws NotFoundException;
   public abstract Response updateCollection(Collection collection, SecurityContext securityContext)
           throws NotFoundException;
   public abstract Response removeCollection(Collection collection, SecurityContext securityContext)
           throws NotFoundException;
   public abstract Response fetchCollection(String collectionTypeId, SecurityContext securityContext)
           throws NotFoundException;
   public abstract Response addItemsToCollection(Collection collection, SecurityContext securityContext)
           throws NotFoundException;
   public abstract Response removeItemsFromCollection(Collection collection, SecurityContext securityContext)
           throws NotFoundException;


}
