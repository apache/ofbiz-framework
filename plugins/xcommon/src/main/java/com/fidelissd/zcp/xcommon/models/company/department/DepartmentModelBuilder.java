package com.fidelissd.zcp.xcommon.models.company.department;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.models.account.User;
import com.fidelissd.zcp.xcommon.models.client.Employee;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.UtilValidate;

public class DepartmentModelBuilder {
  public static List<Department> build(List<Map> departments) {

    List<Department> departmentList = FastList.newInstance();
    for (Map departmentMap : departments) {
      Department department = build(departmentMap);
      departmentList.add(department);
    }
    return departmentList;
  }

  public static Department build(Map departmentMap) {
    Department department = new Department();
    department.setId((String) departmentMap.get("id"));
    department.setName((String) departmentMap.get("name"));
    department.setDescription((String) departmentMap.get("description"));

    Map ownerDetails = (Map) departmentMap.get("owner");
    if (UtilValidate.isNotEmpty(ownerDetails)) {

      User owner = new User();
      owner.setId((String) ownerDetails.get("id"));
      owner.setDisplayName((String) ownerDetails.get("displayName"));
      owner.setEmail((String) ownerDetails.get("email"));
      owner.setPhotoUrl((String) ownerDetails.get("photoUrl"));
      department.setOwner(owner);
    }

    if (UtilValidate.isNotEmpty(departmentMap.get("members"))) {
      List<Employee> membersList = FastList.newInstance();
      List<Map> members = (List) departmentMap.get("members");
      for (Map member : members) {
        Employee empMember = new Employee();
        empMember.setId((String) member.get("id"));
        empMember.setDisplayName((String) member.get("displayName"));
        empMember.setEmail((String) member.get("email"));
        empMember.setPhotoUrl((String) member.get("photoUrl"));
        membersList.add(empMember);
      }
      department.setMembers(membersList);
    }

    List<Map> subDepartments = (List) departmentMap.get("subDepartments");
    List<Department> subDepartmentList = FastList.newInstance();
    for (Map subDepartment : CollectionUtils.emptyIfNull(subDepartments)) {
      subDepartmentList.add(build(subDepartment));
    }

    if (UtilValidate.isNotEmpty(subDepartmentList)) {
      department.setSubDepartments(subDepartmentList);
    }

    return department;
  }
}
