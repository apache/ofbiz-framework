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
 *
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class PermissionRecorder {


    protected boolean isOn = false;
    protected GenericValue userLogin;
    protected List<Map<String, Object>> permCheckResults = new LinkedList<>();
    protected boolean entityPermCheckResult = false;
    protected String currentContentId = "";
    protected Map<String, Object> currentContentMap;
    protected String privilegeEnumId;
    protected int currentCheckMode;
    protected GenericValue [] contentPurposeOperations;
    protected String [] statusTargets;
    protected String [] targetOperations;

    public static final String module = PermissionRecorder.class.getName();

    private static final String [] opFields = { "contentPurposeTypeId", "contentOperationId", "roleTypeId", "statusId", "privilegeEnumId"};
    private static final String [] fieldTitles = { "Purpose", "Operation", "Role", "Status", "Privilege"};

    public PermissionRecorder() {
        isOn = UtilProperties.propertyValueEqualsIgnoreCase("content", "permissionRecorderOn", "true");
    }

    public void setCheckMode(int val) {
        currentCheckMode = val;
    }

    public int getCheckMode() {
        return currentCheckMode;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean b) {
        isOn = b;
    }

    public void setUserLogin(GenericValue user) {
        userLogin = user;
    }

    public GenericValue getUserLogin() {
        return userLogin;
    }

    public boolean getEntityPermCheckResult() {
        return entityPermCheckResult;
    }

    public void setEntityPermCheckResult(boolean b) {
        entityPermCheckResult = b;
    }

    public GenericValue [] getContentPurposeOperations() {
       return contentPurposeOperations != null ? contentPurposeOperations.clone() : null;
    }

    public void setContentPurposeOperations(List<GenericValue> opList) {
       contentPurposeOperations = opList.toArray(new GenericValue[opList.size()]);
    }

    public void setPrivilegeEnumId(String id) {
        privilegeEnumId = id;
    }

    public String getPrivilegeEnumId() {
        return privilegeEnumId;
    }

    public String [] getStatusTargets() {
       return statusTargets != null ? statusTargets.clone() : null;
    }

    public void setStatusTargets(List<String> opList) {
       statusTargets = opList.toArray(new String[opList.size()]);
    }

    public String [] getTargetOperations() {
       return targetOperations != null ? targetOperations.clone() : null;
    }

    public void setTargetOperations(List<String> opList) {
       targetOperations = opList.toArray(new String[opList.size()]);
    }

    public void setCurrentContentId(String id) {
        if (!currentContentId.equals(id)) {
            currentContentMap = new HashMap<>();
            permCheckResults.add(currentContentMap);
            currentContentMap.put("contentId", id);
            currentContentMap.put("checkResults", new LinkedList<>());
        }
        currentContentId = id;
    }

    public String getCurrentContentId() {
        return currentContentId;
    }

    public void setRoles(List<String> roles) {
        if (currentContentMap != null) {
            if (roles != null)
                currentContentMap.put("roles", roles.toArray());
            else
                currentContentMap.put("roles", null);
        }
    }

    public void setPurposes(List<String> purposes) {
        if (currentContentMap != null) {
            if (purposes != null)
                currentContentMap.put("purposes", purposes.toArray());
            else
                currentContentMap.put("purposes", null);
        }
    }

    public void startMatchGroup(List<String> targetOperations, List<String> purposes, List<String> roles, List<String> targStatusList, String targPrivilegeEnumId, String contentId) {
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

    public void record(GenericValue purposeOp, boolean targetOpCond, boolean purposeCond, boolean statusCond, boolean privilegeCond, boolean roleCond) {
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

        for (int i=0; i < fieldTitles.length; i++) {
            String opField = fieldTitles[i];
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

    public String renderCurrentContentMapHtml(Map<String, Object> cMap) {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> resultList = UtilGenerics.cast(cMap.get("checkResultList"));
        for (Map<String, Object> rMap : resultList) {
            sb.append(renderResultRowHtml(rMap, cMap));
        }

        return sb.toString();
    }


    public String renderResultRowHtml(Map<String, Object> rMap, Map<String, Object> currentContentResultMap) {
        StringBuilder sb = new StringBuilder();

        // Do target row
        sb.append("<tr>");

        sb.append("<td class=\"target\">");
        sb.append((String)rMap.get("contentId"));
        sb.append("</td>");

        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (1):" + sb.toString(), module);
        String str = null;
        String s = null;
        for (int i=0; i < opFields.length; i++) {
            String opField = opFields[i];
            sb.append("<td class=\"target\">");
            s = (String)currentContentResultMap.get(opField);
            if (s != null)
                str = s;
            else
                str = "&nbsp;";
            sb.append(str);
            sb.append("</td>");
        }
        sb.append("<td class=\"target\" >&nbsp;</td>");
        sb.append("</tr>");

        // Do UUT row
        sb.append("<tr>");

        sb.append("<td class=\"target\">");
        sb.append((String)currentContentResultMap.get("contentId"));
        sb.append("</td>");

        boolean isPass = true;
        for (int i=0; i < opFields.length; i++) {
            String opField = opFields[i];
            Boolean bool = (Boolean)rMap.get(opField + "Cond");
            String cls = (bool) ? "pass" : "fail";
            if (!bool)
                isPass = false;
            sb.append("<td class=\"" + cls + "\">");
            s = (String)rMap.get(opField);
            sb.append(s);
            sb.append("</td>");
        }
        String passFailCls = (isPass) ? "pass" : "fail";
        sb.append("<td class=\"" + passFailCls +"\">" + passFailCls.toUpperCase(Locale.getDefault()) + "</td>");
        sb.append("</tr>");

        return sb.toString();
    }
}
