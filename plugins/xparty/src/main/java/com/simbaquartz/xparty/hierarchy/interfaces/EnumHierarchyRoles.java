package com.simbaquartz.xparty.hierarchy.interfaces;

public interface EnumHierarchyRoles
{
    static <T extends Enum<T>> String[] roleTypeIds(T[] values) {
        String[] names = new String[values.length];
        int index = 0;
        for (T value : values) {
            names[index++] = value.name();
        }

        return names;
    }
}
