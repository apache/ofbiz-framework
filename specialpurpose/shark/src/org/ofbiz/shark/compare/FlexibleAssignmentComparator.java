/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.shark.compare;

import java.io.Serializable;
import java.util.Comparator;

import org.enhydra.shark.api.client.wfmodel.WfAssignment;
import org.enhydra.shark.api.client.wfbase.BaseException;

/**
 * Flexible WfAssignment Comparator - Sorting assignments by common fields
 */
public class FlexibleAssignmentComparator implements Comparator, Serializable {

    public static final int ASCENDING_ORDER = 0;
    public static final int DESCENDING_ORDER = 1;

    public static final int ORDER_BY_RESOURCE = 0;
    public static final int ORDER_BY_PRIORITY = 1;
    public static final int ORDER_BY_TIME = 2;
    public static final int ORDER_BY_ACCEPT = 3;
    public static final int ORDER_BY_ACTIVITY = 4;

    protected int[] sort = { 0, 3, 1, 2, 4 };
    protected int order = 0;

    public FlexibleAssignmentComparator(int[] sort, int order) {
        this.sort = sort;
        this.order = order;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return obj.equals(this);
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object obj1, Object obj2) {
        try {
            WfAssignment as1 = (WfAssignment) obj1;
            WfAssignment as2 = (WfAssignment) obj2;
            if (as1 == null && as2 != null) {
                return (order == ASCENDING_ORDER ? -1 : 1);
            }
            if (as2 == null && as1 != null) {
                return (order == ASCENDING_ORDER ? 1 : -1);
            }
            if (as1 == null && as2 == null) {
                return 0;
            }                        

            for (int i = 0; i < sort.length; i++) {
                int compare = 0;
                switch (sort[i]) {
                    case ORDER_BY_RESOURCE :
                        compare = compareResource(as1, as2);
                        break;
                    case ORDER_BY_PRIORITY :
                        compare = comparePriority(as1, as2);
                        break;
                    case ORDER_BY_TIME :
                        compare = compareTime(as1, as2);
                        break;
                    case ORDER_BY_ACCEPT :
                        compare = compareAccepted(as1, as2);
                        break;
                }
                if (compare != 0) {
                    return compare;
                }
            }

            // some default compares
            if (!containsSort(ORDER_BY_RESOURCE)) {
                int compare = compareResource(as1, as2);
                if (compare != 0) {
                    return compare;
                }
            }
            if (!containsSort(ORDER_BY_ACTIVITY)) {
                int compare = compareActivity(as1, as2);
                if (compare != 0) {
                    return compare;
                }
            }
        } catch (Exception e) {
            return 0;
        }

        return 0;
    }

    private boolean containsSort(int field) {
        for (int i = 0; i < sort.length; i++) {
            if (sort[i] == field) return true;
        }
        return false;
    }

    private int compareActivity(WfAssignment as1, WfAssignment as2) {
        String a1 = null;
        String a2 = null;

        try {
            a1 = as1.activity().key();
            a2 = as2.activity().key();
        } catch (BaseException e) {
            throw new IllegalArgumentException("Unable to obtain activity from assignment");
        }

        if (order == ASCENDING_ORDER) {
            return a1.compareTo(a2);
        } else {
            return a2.compareTo(a1);
        }
    }

    private int compareResource(WfAssignment as1, WfAssignment as2) {
        String r1 = null;
        String r2 = null;
        try {
            r1 = as1.assignee().resource_key();
            r2 = as2.assignee().resource_key();
        } catch (BaseException e) {
            throw new IllegalArgumentException("Unable to obtain resource from assignment");
        }

        if (order == ASCENDING_ORDER) {
            return r1.compareTo(r2);
        } else {
            return r2.compareTo(r1);
        }
    }

    private int comparePriority(WfAssignment as1, WfAssignment as2) {
        short p1 = 0;
        short p2 = 0;
        try {
            p1 = as1.activity().priority();
            p2 = as2.activity().priority();
        } catch (BaseException e) {
            throw new IllegalArgumentException("Unable to obtain activity from assignment");
        }

        if (order == ASCENDING_ORDER) {
            return (p1 < p2 ? -1 : (p1 == p2 ? 0 : 1));
        } else {
            return (p2 < p1 ? -1 : (p2 == p1 ? 0 : 1));
        }
    }

    private int compareTime(WfAssignment as1, WfAssignment as2) {
        long t1 = 0;
        long t2 = 0;
        try {
            if ("open.not_running.not_started".equals(as1.activity().state())) {
                t1 = 0;
            } else {
                t1 = as1.activity().last_state_time().time;
            }
            if ("open.not_running.not_started".equals(as2.activity().state())) {
                t2 = 0;
            } else {
                t2 = as2.activity().last_state_time().time;
            }
        } catch (BaseException e) {
            throw new IllegalArgumentException("Unable to obtain activity from assignment");
        }

        if (order == ASCENDING_ORDER) {
            return (t1 < t2 ? -1 : (t1 == t2 ? 0 : 1));
        } else {
            return (t2 < t1 ? -1 : (t2 == t1 ? 0 : 1));
        }
    }

    private int compareAccepted(WfAssignment as1, WfAssignment as2) {
        boolean a1 = false;
        boolean a2 = false;
        try {
            a1 = as1.get_accepted_status();
            a2 = as2.get_accepted_status();
        } catch (BaseException e) {
            throw new IllegalArgumentException("Unable to get accepted status from assignment");
        }

        if (order == ASCENDING_ORDER) {
            return (a1 && !a2 ? -1 : (a1 && a2 ? 0 : 1));
        } else {
            return (a2 && !a1 ? -1 : (a2 && a1 ? 0 : 1));
        }
    }
}
