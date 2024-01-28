package com.simbaquartz.xcommon.models.company.department;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.CreatedModifiedBy;
import com.simbaquartz.xcommon.models.account.User;
import com.simbaquartz.xcommon.models.client.Employee;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;


@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Department extends CreatedModifiedBy {

    /**
     * Unique id of the department
     */
    @JsonProperty("id")
    private String id = null;

    /**
     * Name of the department
     */
    @NotEmpty(message = "Please provide a department name")
    @JsonProperty("name")
    private String name = null;

    /**
     * description if any
     */
    @JsonProperty("description")
    private String description = null;

    /**
     * List of members for the department
     */
    @JsonProperty("members")
    private List<Employee> members = null;

    /**
     * parent department Id for the current department
     */
    @JsonProperty("parentDepartmentId")
    private String parentDepartmentId = null;

    /**
     * Owner/head of the department
     */
    @JsonProperty("owner")
    private User owner = null;

    /**
     * List of sub departments
     */
    @JsonProperty("subDepartments")
    private List<Department> subDepartments = null;


}
