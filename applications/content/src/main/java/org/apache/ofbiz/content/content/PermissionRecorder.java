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
package org.apache.ofbiz.content.content;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.GenericValue;

/**
 * PermissionRecorder Class
 * <p>
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class PermissionRecorder {


    private boolean isOn = false;
    private GenericValue userLogin;
    private List<Map<String, Object>> permCheckResults = new LinkedList<>();
    private boolean entityPermCheckResult = false;
    private String currentContentId = "";
    private Map<String, Object> currentContentMap;
    private String privilegeEnumId;
    private int currentCheckMode;
    private GenericValue[] contentPurposeOperations;
    private String[] statusTargets;
    private String[] targetOperations;

    private static final String MODULE = PermissionRecorder.class.getName();

    private static final String[] OP_FIELDS = {"contentPurposeTypeId", "contentOperationId", "roleTypeId", "statusId", "privilegeEnumId"};
    private static final String[] FIELD_TITLES = {"Purpose", "Operation", "Role", "Status", "Privilege"};

    public PermissionRecorder() {
        isOn = UtilProperties.propertyValueEqualsIgnoreCase("content", "permissionRecorderOn", "true");
    }

    /**
     * Gets check mode.
     * @return the check mode
     */
    public int getCheckMode() {
        return currentCheckMode;
    }

    /**
     * Sets check mode.
     * @param val the val
     */
    public void setCheckMode(int val) {
        currentCheckMode = val;
    }

    /**
     * Is on boolean.
     * @return the boolean
     */
    public boolean isOn() {
        return isOn;
    }

    /**
     * Sets on.
     * @param b the b
     */
    public void setOn(boolean b) {
        isOn = b;
    }

    /**
     * Gets user login.
     * @return the user login
     */
    public GenericValue getUserLogin() {
        return userLogin;
    }

    /**
     * Sets user login.
     * @param user the user
     */
    public void setUserLogin(GenericValue user) {
        userLogin = user;
    }

    /**
     * Gets entity perm check result.
     * @return the entity perm check result
     */
    public boolean getEntityPermCheckResult() {
        return entityPermCheckResult;
    }

    /**
     * Sets entity perm check result.
     * @param b the b
     */
    public void setEntityPermCheckResult(boolean b) {
        entityPermCheckResult = b;
    }

    /**
     * Get content purpose operations generic value [ ].
     * @return the generic value [ ]
     */
    public GenericValue[] getContentPurposeOperations() {
        return contentPurposeOperations != null ? contentPurposeOperations.clone() : null;
    }

    /**
     * Sets content purpose operations.
     * @param opList the op list
     */
    public void setContentPurposeOperations(List<GenericValue> opList) {
        contentPurposeOperations = opList.toArray(new GenericValue[opList.size()]);
    }

    /**
     * Gets privilege enum id.
     * @return the privilege enum id
     */
    public String getPrivilegeEnumId() {
        return privilegeEnumId;
    }

    /**
     * Sets privilege enum id.
     * @param id the id
     */
    public void setPrivilegeEnumId(String id) {
        privilegeEnumId = id;
    }

    /**
     * Get status targets string [ ].
     * @return the string [ ]
     */
    public String[] getStatusTargets() {
        return statusTargets != null ? statusTargets.clone() : null;
    }

    /**
     * Sets status targets.
     * @param opList the op list
     */
    public void setStatusTargets(List<String> opList) {
        statusTargets = opList.toArray(new String[opList.size()]);
    }

    /**
     * Get target operations string [ ].
     * @return the string [ ]
     */
    public String[] getTargetOperations() {
        return targetOperations != null ? targetOperations.clone() : null;
    }

    /**
     * Sets target operations.
     * @param opList the op list
     */
    public void setTargetOperations(List<String> opList) {
        targetOperations = opList.toArray(new String[opList.size()]);
    }

    /**
     * Sets current content id.
     * @param id the id
     */
    public void setCurrentContentId(String id) {
        if (!currentContentId.equals(id)) {
            currentContentMap = new HashMap<>();
            permCheckResults.add(currentContentMap);
            currentContentMap.put("contentId", id);
            currentContentMap.put("checkResults", new LinkedList<>());
        }
        currentContentId = id;
    }

    /**
     * Gets current content id.
     * @return the current content id
     */
    public String getCurrentContentId() {
        return currentContentId;
    }

    /**
     * Sets roles.
     * @param roles the roles
     */
    public void setRoles(List<String> roles) {
        if (currentContentMap != null) {
            if (roles != null) {
                currentContentMap.put("roles", roles.toArray());
            } else {
                currentContentMap.put("roles", null);
            }
        }
    }

    /**
     * Sets purposes.
     * @param purposes the purposes
     */
    public void setPurposes(List<String> purposes) {
        if (currentContentMap != null) {
            if (purposes != null) {
                currentContentMap.put("purposes", purposes.toArray());
            } else {
                currentContentMap.put("purposes", null);
            }
        }
    }

    /**
     * Start match group.
     * @param targetOperations    the target operations
     * @param purposes            the purposes
     * @param roles               the roles
     * @param targStatusList      the targ status list
     * @param targPrivilegeEnumId the targ privilege enum id
     * @param contentId           the content id
     */
    public void startMatchGroup(List<String> targetOperations, List<String> purposes, List<String> roles, List<String> targStatusList,
                                String targPrivilegeEnumId, String contentId) {
        currentContentMap = new HashMap<>();
        permCheckResults.add(currentContentMap);
        String s = null;
        if (targetOperations != null) {
            s = targetOperations.toString();
            currentContentMap.put("contentOperationId", s);
        }
        if (purposes != null) {
            s = purposes.toString();
            currentContentMap.put("contentPurposeTypeId", s);
        }
        if (roles != null) {
            s = roles.toString();
            currentContentMap.put("roleTypeId", s);
        }
        if (targStatusList != null) {
            s = targStatusList.toString();
            currentContentMap.put("statusId", s);
        }
        List<Map<String, Object>> checkResultList = new LinkedList<>();
        currentContentMap.put("privilegeEnumId", privilegeEnumId);
        currentContentMap.put("contentId", contentId);
        currentContentMap.put("checkResultList", checkResultList);
        currentContentMap.put("matches", null);
        currentContentId = contentId;
    }

    /**
     * Record.
     * @param purposeOp the purpose op
     * @param targetOpCond the target op cond
     * @param purposeCond the purpose cond
     * @param statusCond the status cond
     * @param privilegeCond the privilege cond
     * @param roleCond the role cond
     */
    public void record(GenericValue purposeOp, boolean targetOpCond, boolean purposeCond, boolean statusCond, boolean privilegeCond,
                       boolean roleCond) {
        Map<String, Object> map = UtilMisc.makeMapWritable(purposeOp);
        map.put("contentOperationIdCond", targetOpCond);
        map.put("contentPurposeTypeIdCond", purposeCond);
        map.put("statusIdCond", statusCond);
        map.put("privilegeEnumIdCond", privilegeCond);
        map.put("roleTypeIdCond", roleCond);
        map.put("contentId", currentContentId);
        List<Map<String, Object>> checkResultList = UtilGenerics.cast(currentContentMap.get("checkResultList"));
        checkResultList.add(map);
    }

    /**
     * To html string.
     * @return the string
     */
    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<style type=\"text/css\">");
        sb.append(".pass {background-color:lime; font-family:Verdana,Arial,sans-serif; font-size:10px; }");
        sb.append(".fail {background-color:red; font-family:Verdana,Arial,sans-serif; font-size:10px; }");
        sb.append(".target {background-color:lightgrey; font-family:Verdana,Arial,sans-serif; font-size:10px; }");
        sb.append(".headr {background-color:white; font-weight:bold; font-family:Verdana,Arial,sans-serif; font-size:12px; }");
        sb.append("</style>");

        sb.append("<table border=\"1\" >");
        // Do header row
        sb.append("<tr>");

        sb.append("<td class=\"headr\">");
        sb.append("Content Id");
        sb.append("</td>");

        for (String opField : FIELD_TITLES) {
            sb.append("<td class=\"headr\">");
            sb.append(opField);
            sb.append("</td>");
        }
        sb.append("<td class=\"headr\" >Pass/Fail</td>");
        sb.append("</tr>");

        for (Map<String, Object> cMap : permCheckResults) {
            sb.append(renderCurrentContentMapHtml(cMap));
        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * Render current content map html string.
     * @param cMap the c map
     * @return the string
     */
    public String renderCurrentContentMapHtml(Map<String, Object> cMap) {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> resultList = UtilGenerics.cast(cMap.get("checkResultList"));
        for (Map<String, Object> rMap : resultList) {
            sb.append(renderResultRowHtml(rMap, cMap));
        }

        return sb.toString();
    }

    /**
     * Render result row html string.
     * @param rMap                    the r map
     * @param currentContentResultMap the current content result map
     * @return the string
     */
    public String renderResultRowHtml(Map<String, Object> rMap, Map<String, Object> currentContentResultMap) {
        StringBuilder sb = new StringBuilder();

        // Do target row
        sb.append("<tr>");

        sb.append("<td class=\"target\">");
        sb.append((String) rMap.get("contentId"));
        sb.append("</td>");

        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (1):" + sb.toString(), MODULE);
        String str = null;
        String s = null;
        for (String opField : OP_FIELDS) {
            sb.append("<td class=\"target\">");
            s = (String) currentContentResultMap.get(opField);
            if (s != null) {
                str = s;
            } else {
                str = "&nbsp;";
            }
            sb.append(str);
            sb.append("</td>");
        }
        sb.append("<td class=\"target\" >&nbsp;</td>");
        sb.append("</tr>");

        // Do UUT row
        sb.append("<tr>");

        sb.append("<td class=\"target\">");
        sb.append((String) currentContentResultMap.get("contentId"));
        sb.append("</td>");

        boolean isPass = true;
        for (String opField : OP_FIELDS) {
            Boolean bool = (Boolean) rMap.get(opField + "Cond");
            String cls = (bool) ? "pass" : "fail";
            if (!bool) {
                isPass = false;
            }
            sb.append("<td class=\"" + cls + "\">");
            s = (String) rMap.get(opField);
            sb.append(s);
            sb.append("</td>");
        }
        String passFailCls = (isPass) ? "pass" : "fail";
        sb.append("<td class=\"" + passFailCls + "\">" + passFailCls.toUpperCase(Locale.getDefault()) + "</td>");
        sb.append("</tr>");

        return sb.toString();
    }
}
