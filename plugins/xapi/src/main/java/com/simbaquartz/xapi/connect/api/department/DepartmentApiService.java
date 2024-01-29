package com.simbaquartz.xapi.connect.api.department;

import com.simbaquartz.xapi.connect.api.BaseApiService;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.fidelissd.zcp.xcommon.models.company.department.Department;
import com.fidelissd.zcp.xcommon.models.company.department.DepartmentSerachBean;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class DepartmentApiService implements BaseApiService {

  public abstract Response createDepartment(Department department, SecurityContext securityContext)
          throws NotFoundException;

  public abstract Response getDepartments(DepartmentSerachBean departmentSerachBean, SecurityContext securityContext)
          throws NotFoundException;

  public abstract Response getDepartment( String departmentId,SecurityContext securityContext)
          throws NotFoundException;

  public abstract Response updateDepartment( String departmentId, Department department, SecurityContext securityContext)
          throws NotFoundException;
}
