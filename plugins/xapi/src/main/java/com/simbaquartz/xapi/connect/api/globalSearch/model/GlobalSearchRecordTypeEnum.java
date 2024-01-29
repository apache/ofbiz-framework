
package com.simbaquartz.xapi.connect.api.globalSearch.model;

import com.simbaquartz.xparty.hierarchy.interfaces.EnumHierarchyRoles;

// Roles required to manage GlobalSearcRecordTypeEnum
public enum GlobalSearchRecordTypeEnum implements EnumHierarchyRoles {
    CUSTOMER ("CUSTOMER") ,
    LEAD ("LEAD"),
    CONTACT ("CONTACT"),
    TASK ("TASK"),
    DEAL ("DEAL"),
    PROJECT ("PROJECT") ;

    public String roleTypeId;

    GlobalSearchRecordTypeEnum(String value)
    {
        this.roleTypeId = value;
    }

   public static String[] roleTypeIds() {
        return EnumHierarchyRoles.roleTypeIds(com.simbaquartz.xapi.connect.api.globalSearch.model.GlobalSearchRecordTypeEnum.values());
    }
    public String roleTypeId() {
        return roleTypeId;
    }

}