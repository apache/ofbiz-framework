package com.simbaquartz.xapi.connect.api.department;

import com.fidelissd.zcp.xcommon.models.company.department.Department;
import com.fidelissd.zcp.xcommon.models.company.department.DepartmentSerachBean;
import com.simbaquartz.xapi.connect.factories.DepartmentApiServiceFactory;
import com.simbaquartz.xapi.connect.api.security.Secured;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/departments")
public class DepartmentAPI {
  private final DepartmentApiService delegate = DepartmentApiServiceFactory.getDepartmentApi();

  /**
   * Created API to add departments
   *
   * @param department
   * @param securityContext
   * @return
   * @throws com.simbaquartz.xapi.connect.api.NotFoundException
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createDepartment(@Valid Department department, @Context SecurityContext securityContext)
          throws com.simbaquartz.xapi.connect.api.NotFoundException {
    return delegate.createDepartment(department, securityContext);
  }

  /**
   * Created API to fetch departments
   *
   * @param securityContext
   * @return
   * @throws com.simbaquartz.xapi.connect.api.NotFoundException
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDepartments(@BeanParam DepartmentSerachBean departmentSerachBean, @Context SecurityContext securityContext)
          throws com.simbaquartz.xapi.connect.api.NotFoundException {
    return delegate.getDepartments(departmentSerachBean,securityContext);
  }

  /**
   * API to get single department details
   *
   * @param departmentId
   * @param securityContext
   * @return
   * @throws com.simbaquartz.xapi.connect.api.NotFoundException
   */
  @GET
  @Path("/{departmentId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDepartment(@PathParam("departmentId") String departmentId, @Context SecurityContext securityContext)
          throws com.simbaquartz.xapi.connect.api.NotFoundException {
    return delegate.getDepartment(departmentId,securityContext);
  }

  /**
   * API to update department details
   *
   * @param departmentId
   * @param securityContext
   * @return
   * @throws com.simbaquartz.xapi.connect.api.NotFoundException
   */
  @PUT
  @Path("/{departmentId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateDepartment(@PathParam("departmentId") String departmentId, @Valid Department department, @Context SecurityContext securityContext)
          throws com.simbaquartz.xapi.connect.api.NotFoundException {
    return delegate.updateDepartment(departmentId, department, securityContext);
  }

}
