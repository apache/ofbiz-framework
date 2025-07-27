package org.apache.ofbiz.ws.rs.resources;

import jakarta.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;
import org.apache.ofbiz.ws.rs.security.Secured;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.ApiServiceRequest;
import org.apache.ofbiz.ws.rs.ServiceRequestProcessor;
import org.apache.ofbiz.ws.rs.annotation.ServiceRequestValidator;
import org.apache.ofbiz.ws.rs.response.Success;
import org.apache.ofbiz.ws.rs.security.Secured;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;

@Path("/entity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Provider
@Secured
public class OFBizEntityResource extends OFBizResource {

    private static final String MODULE = OFBizEntityResource.class.getName();

    @GET
    @Path("/{entityName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listOrFilterEntities(
            @PathParam("entityName") String entityName,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request) {

        try {
            Set<String> SKIP_QUERY_PARAMS = Set.of("pageSize", "pageIndex", "orderBy");

            Map<String, String> filterParams = new HashMap<>();

            List<EntityCondition> conditions = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
                String key = entry.getKey();
                if (SKIP_QUERY_PARAMS.contains(key)) continue;

                for (String rawValue : entry.getValue()) {
                    String[] parts = rawValue.split(":", 2);
                    EntityComparisonOperator<?, ?> operator = EntityComparisonOperator.EQUALS;
                    Object value;

                    /**
                    | Query                                       | Meaning                      |
                    | ------------------------------------------- | ---------------------------- |
                    | `statusId=ACTIVE`                           | Equals                       |
                    | `statusId=NEQ:CLOSED`                       | Not equal                    |
                    | `statusId=IN:ACTIVE,DRAFT`                  | IN list                      |
                    | `createdDate=BETWEEN:2024-01-01,2024-12-31` | Date range                   |
                    | `amount=GT:1000&amount=LT:5000`             | Amount between 1000 and 5000 |
                    | `orderBy=-createdDate`                      | Sort descending              |
                    | 'glAccountTypeId=LIKE:CURRENT_A%'           | Like (uses SQL wild cards)   |
                     **/

                    if (parts.length == 2) {
                        value = parts[1];
                        switch (parts[0].toUpperCase()) {
                            case "LIKE": operator = EntityComparisonOperator.LIKE; break;
                            case "GT": operator = EntityComparisonOperator.GREATER_THAN; break;
                            case "LT": operator = EntityComparisonOperator.LESS_THAN; break;
                            case "GTE": operator = EntityComparisonOperator.GREATER_THAN_EQUAL_TO; break;
                            case "LTE": operator = EntityComparisonOperator.LESS_THAN_EQUAL_TO; break;
                            case "NEQ": operator = EntityComparisonOperator.NOT_EQUAL; break;
                            case "IN":
                                operator = EntityComparisonOperator.IN;
                                value = Arrays.asList(parts[1].split(","));
                                break;
                            case "BETWEEN":
                                operator = EntityComparisonOperator.BETWEEN;
                                value = Arrays.asList(parts[1].split(",", 2));
                                break;
                            default:
                                value = rawValue;
                        }
                    } else {
                        value = rawValue;
                    }

                    conditions.add(EntityCondition.makeCondition(key, operator, value));
                }
            }

            // Parse pagination parameters
            String pageSizeStr = request.getParameter("pageSize");
            String pageIndexStr = request.getParameter("pageIndex");

            Integer pageSize = UtilValidate.isNotEmpty(pageSizeStr) ? Integer.parseInt(pageSizeStr) : null;
            Integer pageIndex = UtilValidate.isNotEmpty(pageIndexStr) ? Integer.parseInt(pageIndexStr) : null;

            int offset = (pageIndex != null && pageSize != null) ? (pageIndex * pageSize) : 0;

            // Base query
            EntityQuery query = EntityQuery
                    .use(getDelegator())
                    .from(entityName);

            if (!conditions.isEmpty()) {
                query = query.where(EntityCondition.makeCondition(conditions));
            }

            // Count total
            long totalCount = query.queryCount();

            // Apply pagination if both params are present
            if (pageSize != null && pageIndex != null) {
                query = query.offset(offset).limit(pageSize);
            }

            // Sorting support
            String orderByParam = request.getParameter("orderBy");
            List<String> orderByList = new ArrayList<>();
            if (UtilValidate.isNotEmpty(orderByParam)) {
                // Supports comma-separated fields
                for (String field : orderByParam.split(",")) {
                    field = field.trim();
                    if (field.startsWith("-")) {
                        orderByList.add(field.substring(1) + " DESC");
                    } else {
                        orderByList.add(field + " ASC");
                    }
                }
            }

            if (!orderByList.isEmpty()) {
                query = query.orderBy(orderByList);
            }

            List<GenericValue> results = query.queryList();

            Map<String, Object> response = new HashMap<>();
            response.put("entities", results);
            response.put("totalCount", totalCount);
            response.put("pageSize", pageSize);
            response.put("pageIndex", pageIndex);

            return Response.ok(response).build();

        } catch (GenericEntityException e) {
            return RestApiUtil.error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Entity Query Error",
                    "Failed to query entity: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return RestApiUtil.error(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Bad Request",
                    "Invalid request: " + e.getMessage());
        }
    }

    @GET
    @Path("/{entityName}/{pk}")
    public Response getEntity(@PathParam("entityName") String entityName,
                              @PathParam("pk") String pk) {
        try {
            ModelEntity model = getDelegator().getModelEntity(entityName);
            String pkFieldName = getPrimaryKeyField(model);
            GenericValue value = getDelegator().findOne(entityName, Map.of(pkFieldName, pk), false);
            if (value == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Entity not found").build();
            }
            return Response.ok(value).build();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error fetching entity: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/{entityName}/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response countEntities(
            @PathParam("entityName") String entityName,
            @Context UriInfo uriInfo) {

        try {
            Map<String, String> filterParams = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
                filterParams.put(entry.getKey(), entry.getValue().get(0));
            }

            EntityQuery query = EntityQuery
                    .use(getDelegator())
                    .from(entityName);

            if (!filterParams.isEmpty()) {
                query = query.where(filterParams);
            }

            long totalCount = query.queryCount();

            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", totalCount);

            return Response.ok(response).build();

        } catch (GenericEntityException e) {
            return RestApiUtil.error(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Entity Count Error",
                    "Failed to count entity: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return RestApiUtil.error(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Bad Request",
                    "Invalid request: " + e.getMessage());
        }
    }


    @POST
    @Path("/{entityName}")
    public Response createEntity(@PathParam("entityName") String entityName,
                                 Map<String, Object> payload) {
        try {
            GenericValue newValue = getDelegator().makeValue(entityName, payload);
            getDelegator().create(newValue);
            return Response.status(Response.Status.CREATED).entity(newValue).build();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error creating entity: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{entityName}/{pk}")
    public Response updateEntity(@PathParam("entityName") String entityName,
                                 @PathParam("pk") String pk,
                                 Map<String, Object> payload) {
        try {
            ModelEntity model = getDelegator().getModelEntity(entityName);
            String pkFieldName = getPrimaryKeyField(model);
            payload.put(pkFieldName, pk);

            GenericValue updated = getDelegator().makeValue(entityName, payload);
            int updatedCount = getDelegator().store(updated);
            if (updatedCount == 0) {
                return Response.status(Response.Status.NOT_FOUND).entity("Entity not found").build();
            }
            return Response.ok(updated).build();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error updating entity: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{entityName}/{pk}")
    public Response deleteEntity(@PathParam("entityName") String entityName,
                                 @PathParam("pk") String pk) {
        try {
            ModelEntity model = getDelegator().getModelEntity(entityName);
            String pkFieldName = getPrimaryKeyField(model);

            int deleted = getDelegator().removeByAnd(entityName, Map.of(pkFieldName, pk));
            if (deleted == 0) {
                return Response.status(Response.Status.NOT_FOUND).entity("Entity not found").build();
            }
            return Response.noContent().build();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error deleting entity: " + e.getMessage()).build();
        }
    }

    private String getPrimaryKeyField(ModelEntity model) {
        List<ModelField> pkFields = model.getPkFields();
        if (pkFields.size() != 1) {
            throw new WebApplicationException("Only single-field primary keys supported", 400);
        }
        return pkFields.get(0).getName();
    }
}
