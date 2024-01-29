package com.fidelissd.zcp.xcommon.models.company;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)

public class EmployeeLeave {
    //employee id for employee
    @NotNull(message = "employeeId is required")
    @NotBlank(message = "employeeId is required")
    @JsonProperty("employeeId")
    private String employeeId;

    //leave type for employee like INLAND_EARNED
    @NotNull(message = "leaveTypeId is required")
    @NotBlank(message = "leaveTypeId is required")
    @JsonProperty("leaveTypeId")
    private String leaveTypeId;

    //leave reason type for employee
    @JsonProperty("reasonType")
    private String reasonType;

    //leave from date for employee
    @JsonProperty("fromDate")
    private Timestamp fromDate;

    //leave through date for employee
    @JsonProperty("thruDate")
    private Timestamp thruDate;

    //leave approver id
    @NotNull(message = "leaveApprovalId is required")
    @NotBlank(message = "leaveApprovalId is required")
    @JsonProperty("leaveApprovalId")
    private String leaveApprovalId;

    //leave status id
    @JsonProperty("status")
    private String status;

    //leave description
    @JsonProperty("description")
    private String description;
}
