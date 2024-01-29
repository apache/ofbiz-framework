

package com.simbaquartz.xparty.hierarchy.role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;


public abstract class HierarchyRoleUtils
{
    public static <T extends Enum<T> & HierarchyRolesEnum> List<String> roleTypeIds(Class<T> enumClass) {
        List<String> values = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            values.add(constant.name());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> List<String> roleTypeIds(Class<T> enumClass, boolean employmentRole) {
        List<String> values = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            if(constant.isEmploymentRole() == employmentRole)
                values.add(constant.name());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> List<Boolean> employmentRoles(Class<T> enumClass) {
        List<Boolean> values = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            values.add(constant.isEmploymentRole());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> List<String> partyRelationshipTypeIds(Class<T> enumClass) {
        List<String> values = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            values.add(constant.getPartyRelationshipTypeId());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> List<String> descriptions(Class<T> enumClass) {
        List<String> values = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            values.add(constant.getDescription());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> Map<String,String> getRoleTypeIdNameMap(Class<T> enumClass) {
        Map<String,String> values = new HashMap<>();
        for (T constant : enumClass.getEnumConstants()) {
            values.put(constant.name(), constant.getDescription());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> Map<String,String> getRoleTypeIdNameMap(Class<T> enumClass, boolean employmentRole) {
        Map<String,String> values = new HashMap<>();
        for (T constant : enumClass.getEnumConstants()) {
            if(constant.isEmploymentRole() == employmentRole)
                values.put(constant.name(), constant.getDescription());
        }
        return values;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> Boolean isValidPartyRole(Class<T> enumClass, String roleTypeId)
    {
        Boolean isValid = false;

        List<String> roleTypeIdConstants = roleTypeIds(enumClass);
        if(roleTypeIdConstants.contains(roleTypeId))
            isValid = true;

        return isValid;
    }
}
