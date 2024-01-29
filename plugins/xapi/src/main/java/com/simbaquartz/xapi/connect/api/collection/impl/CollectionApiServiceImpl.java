package com.simbaquartz.xapi.connect.api.collection.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.collection.CollectionApiService;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.models.collection.Collection;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public class CollectionApiServiceImpl extends CollectionApiService {

    private static final String module = CollectionApiServiceImpl.class.getName();

    @Override
    public Response createCollection(Collection collection, SecurityContext securityContext) throws NotFoundException
    {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method createCollection", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> createCollectionContext = FastMap.newInstance();

        createCollectionContext.put("userLogin", loggedInUser.getUserLogin());
        createCollectionContext.put("name", collection.getName());
        createCollectionContext.put("description", collection.getDescription());
        createCollectionContext.put("collectionTypeId", collection.getCollectionTypeId());
        createCollectionContext.put("parentCollectionId", collection.getParentCollectionId());

        Map<String, Object> createCollectionResp = null;
        try {
            createCollectionResp = tenantDispatcher.runSync("fsdCreateCollection", createCollectionContext);
        } catch (GenericServiceException e) {
            //handle error here
            Debug.logError("An error occurred while invoking fsdCreateCollection service, details: " + e.getMessage(), "CollectionApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdCreateCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(createCollectionResp)) {
            Debug.logError("An error occurred while invoking fsdCreateCollection service, details: " + ServiceUtil.getErrorMessage(createCollectionResp), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdCreateCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,ServiceUtil.getErrorMessage(createCollectionResp));
        }

        return ApiResponseUtil.prepareOkResponse(createCollectionResp);

    }


    @Override
    public Response updateCollection(Collection collection, SecurityContext securityContext) throws NotFoundException
    {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method updateCollection", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> updateCollectionContext = FastMap.newInstance();

        updateCollectionContext.put("userLogin", loggedInUser.getUserLogin());
        updateCollectionContext.put("collectionId", collection.getCollectionId());
        updateCollectionContext.put("description", collection.getDescription());
        updateCollectionContext.put("name", collection.getName());
        updateCollectionContext.put("parentCollectionId", collection.getParentCollectionId());
        updateCollectionContext.put("updatedBy", loggedInUser.getPartyId());
        updateCollectionContext.put("parentCollectionId", collection.getParentCollectionId());

        Map<String, Object> updateCollectionResp = null;
        try {
            updateCollectionResp = tenantDispatcher.runSync("fsdUpdateCollection", updateCollectionContext);
        } catch (GenericServiceException e) {
            //handle error here
            Debug.logError("An error occurred while invoking fsdUpdateCollection service, details: " + e.getMessage(), "CollectionApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdUpdateCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(updateCollectionResp)) {
            Debug.logError("An error occurred while invoking fsdUpdateCollection service, details: " + ServiceUtil.getErrorMessage(updateCollectionResp), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdUpdateCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,ServiceUtil.getErrorMessage(updateCollectionResp));
        }

        return ApiResponseUtil.prepareOkResponse(updateCollectionResp);

    }

    @Override
    public Response removeCollection(Collection collection, SecurityContext securityContext) throws NotFoundException
    {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method removeCollection", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> removeCollectionContext = FastMap.newInstance();

        removeCollectionContext.put("userLogin", loggedInUser.getUserLogin());
        removeCollectionContext.put("collectionId", collection.getCollectionId());

        Map<String, Object> removeCollectionResp = null;
        try {
            removeCollectionResp = tenantDispatcher.runSync("fsdRemoveCollection", removeCollectionContext);
        } catch (GenericServiceException e) {
            //handle error here
            Debug.logError("An error occurred while invoking fsdRemoveCollection service, details: " + e.getMessage(), "CollectionApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdRemoveCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(removeCollectionResp)) {
            Debug.logError("An error occurred while invoking fsdUpdateCollection service, details: " + ServiceUtil.getErrorMessage(removeCollectionResp), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdUpdateCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,ServiceUtil.getErrorMessage(removeCollectionResp));
        }

        return ApiResponseUtil.prepareOkResponse(removeCollectionResp);

    }

    @Override
    public Response fetchCollection(String collectionTypeId,SecurityContext securityContext) throws NotFoundException
    {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method fetchCollection", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> fetchCollectionContext = FastMap.newInstance();

        fetchCollectionContext.put("userLogin", loggedInUser.getUserLogin());
        fetchCollectionContext.put("collectionTypeId", collectionTypeId);

        Map<String, Object> fetchCollectionResp = null;
        try {
            fetchCollectionResp = tenantDispatcher.runSync("fsdGetCollection", fetchCollectionContext);
        } catch (GenericServiceException e) {
            //handle error here
            Debug.logError("An error occurred while invoking fsdGetCollection service, details: " + e.getMessage(), "CollectionApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdGetCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(fetchCollectionResp)) {
            Debug.logError("An error occurred while invoking fsdGetCollection service, details: " + ServiceUtil.getErrorMessage(fetchCollectionResp), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdGetCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,ServiceUtil.getErrorMessage(fetchCollectionResp));
        }

        return ApiResponseUtil.prepareOkResponse(fetchCollectionResp);

    }

    @Override
    public Response addItemsToCollection(Collection collection, SecurityContext securityContext) throws NotFoundException
    {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method addItemsToCollection", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> addItemsToCollectionContext = FastMap.newInstance();

        addItemsToCollectionContext.put("userLogin", loggedInUser.getUserLogin());
        addItemsToCollectionContext.put("collectionId", collection.getCollectionId());
        addItemsToCollectionContext.put("collectionItemId", collection.getCollectionItemId());
        addItemsToCollectionContext.put("sequenceId", collection.getSequenceId());
        addItemsToCollectionContext.put("createBy", loggedInUser.getPartyId());

        Map<String, Object> fsdAddItemsToCollectionResp = null;
        try {
            fsdAddItemsToCollectionResp = tenantDispatcher.runSync("fsdAddItemsToCollection", addItemsToCollectionContext);
        } catch (GenericServiceException e) {
            //handle error here
            Debug.logError("An error occurred while invoking fsdAddItemsToCollection service, details: " + e.getMessage(), "CollectionApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdAddItemsToCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(fsdAddItemsToCollectionResp)) {
            Debug.logError("An error occurred while invoking fsdAddItemsToCollection service, details: " + ServiceUtil.getErrorMessage(fsdAddItemsToCollectionResp), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdAddItemsToCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,ServiceUtil.getErrorMessage(fsdAddItemsToCollectionResp));
        }

        return ApiResponseUtil.prepareOkResponse(fsdAddItemsToCollectionResp);

    }

    @Override
    public Response removeItemsFromCollection(Collection collection, SecurityContext securityContext) throws NotFoundException
    {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method removeItemsFromCollection", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> removeItemsFromCollectionContext = FastMap.newInstance();

        removeItemsFromCollectionContext.put("userLogin", loggedInUser.getUserLogin());
        removeItemsFromCollectionContext.put("collectionId", collection.getCollectionId());
        removeItemsFromCollectionContext.put("collectionItemId", collection.getCollectionItemId());

        Map<String, Object> removeItemsFromCollectionResp = null;
        try {
            removeItemsFromCollectionResp = tenantDispatcher.runSync("fsdRemoveItemsFromCollection", removeItemsFromCollectionContext);
        } catch (GenericServiceException e) {
            //handle error here
            Debug.logError("An error occurred while invoking fsdRemoveItemsFromCollection service, details: " + e.getMessage(), "CollectionApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdRemoveItemsFromCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(removeItemsFromCollectionResp)) {
            Debug.logError("An error occurred while invoking fsdRemoveItemsFromCollection service, details: " + ServiceUtil.getErrorMessage(removeItemsFromCollectionResp), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method fsdRemoveItemsFromCollection", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,ServiceUtil.getErrorMessage(removeItemsFromCollectionResp));
        }

        return ApiResponseUtil.prepareOkResponse(removeItemsFromCollectionResp);

    }


}

