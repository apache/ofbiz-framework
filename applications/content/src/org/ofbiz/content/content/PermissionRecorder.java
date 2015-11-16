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
package org.ofbiz.content.content;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericValue;

/**
 * PermissionRecorder Class
 *
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class PermissionRecorder {

    public static final int PRE_PURPOSE = 0;
    public static final int PRE_ROLE = 1;
    public static final int WITH_ROLES = 2;

    protected boolean isOn = false;
    protected GenericValue userLogin;
    protected List<Map<String, Object>> permCheckResults = new LinkedList<Map<String,Object>>();
    protected boolean entityPermCheckResult = false;
    protected String currentContentId = "";
    protected Map<String, Object> currentContentMap;
    protected String privilegeEnumId;
    protected int currentCheckMode;
    protected GenericValue [] contentPurposeOperations;
    protected String [] statusTargets;
    protected String [] targetOperations;

    public static final String module = PermissionRecorder.class.getName();

    public static final String [] opFields = { "contentPurposeTypeId", "contentOperationId", "roleTypeId", "statusId", "privilegeEnumId"};
    public static final String [] fieldTitles = { "Purpose", "Operation", "Role", "Status", "Privilege"};

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
       return contentPurposeOperations;
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
       return statusTargets;
    }

    public void setStatusTargets(List<String> opList) {
       statusTargets = opList.toArray(new String[opList.size()]);
    }

    public String [] getTargetOperations() {
       return targetOperations;
    }

    public void setTargetOperations(List<String> opList) {
       targetOperations = opList.toArray(new String[opList.size()]);
    }

    public void setCurrentContentId(String id) {
        if (!currentContentId.equals(id)) {
            currentContentMap = new HashMap<String, Object>();
            permCheckResults.add(currentContentMap);
            currentContentMap.put("contentId", id);
            currentContentMap.put("checkResults", new LinkedList());
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
        currentContentMap = new HashMap<String, Object>();
        permCheckResults.add(currentContentMap);
        String s = null;
        if (targetOperations != null) {
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, targetOperations:" + targetOperations, module);
            s = targetOperations.toString();
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, targetOperations(string):" + s, module);
            currentContentMap.put("contentOperationId", s);
        }
        if (purposes != null) {
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, purposes:" + purposes, module);
            s = purposes.toString();
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, purposes(string):" + s, module);
            currentContentMap.put("contentPurposeTypeId", s);
        }
        if (roles != null) {
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, roles:" + roles, module);
            s = roles.toString();
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, roles(string):" + s, module);
            currentContentMap.put("roleTypeId", s);
        }
        if (targStatusList != null) {
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, targStatusList:" + targStatusList, module);
            s = targStatusList.toString();
            //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, targStatusList(string):" + s, module);
            currentContentMap.put("statusId", s);
        }
        List<Map<String, Object>> checkResultList = new LinkedList<Map<String,Object>>();
        currentContentMap.put("privilegeEnumId", privilegeEnumId);
        currentContentMap.put("contentId", contentId);
        currentContentMap.put("checkResultList", checkResultList);
        currentContentMap.put("matches", null);
        currentContentId = contentId;
        //if (Debug.infoOn()) Debug.logInfo("startMatchGroup, currentContentMap:" + currentContentMap, module);
    }

    public void record(GenericValue purposeOp, boolean targetOpCond, boolean purposeCond, boolean statusCond, boolean privilegeCond, boolean roleCond) {
        Map<String, Object> map = UtilMisc.makeMapWritable(purposeOp);
        map.put("contentOperationIdCond", Boolean.valueOf(targetOpCond));
        map.put("contentPurposeTypeIdCond", Boolean.valueOf(purposeCond));
        map.put("statusIdCond", Boolean.valueOf(statusCond));
        map.put("privilegeEnumIdCond", Boolean.valueOf(privilegeCond));
        map.put("roleTypeIdCond", Boolean.valueOf(roleCond));
        map.put("contentId", currentContentId);
        List<Map<String, Object>> checkResultList = UtilGenerics.checkList(currentContentMap.get("checkResultList"));
        checkResultList.add(map);
        //if (Debug.infoOn()) Debug.logInfo("record, map:" + map, module);
    }

    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<style type=\"text/css\">");
        sb.append(".pass {background-color:lime; font-family:Verdana,Arial,sans-serif; font-size:10px; }");
        sb.append(".fail {background-color:red; font-family:Verdana,Arial,sans-serif; font-size:10px; }");
        sb.append(".target {background-color:lightgrey; font-family:Verdana,Arial,sans-serif; font-size:10px; }");
        sb.append(".headr {background-color:white; font-weight:bold; font-family:Verdana,Arial,sans-serif; font-size:12px; }");
        sb.append("</style>");

        //if (Debug.infoOn()) Debug.logInfo("toHtml, style:" + sb.toString(), module);
        sb.append("<table border=\"1\" >");
        // Do header row
        sb.append("<tr>");

        sb.append("<td class=\"headr\">");
        sb.append("Content Id");
        sb.append("</td>");

        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (1):" + sb.toString(), module);
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
        List<Map<String, Object>> resultList = UtilGenerics.checkList(cMap.get("checkResultList"));
        for (Map<String, Object> rMap : resultList) {
            //if (Debug.infoOn()) Debug.logInfo("renderCCMapHtml, (1):" + rMap, module);
            sb.append(renderResultRowHtml(rMap, cMap));
        }

        return sb.toString();
    }

    //public static final String [] opFields = { "contentPurposeTypeId", "contentOperationId", "roleTypeId", "statusId", "privilegeEnumId"};

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

        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (2):" + sb.toString(), module);
        // Do UUT row
        sb.append("<tr>");

        sb.append("<td class=\"target\">");
        sb.append((String)currentContentResultMap.get("contentId"));
        sb.append("</td>");

        boolean isPass = true;
        for (int i=0; i < opFields.length; i++) {
            String opField = opFields[i];
            Boolean bool = (Boolean)rMap.get(opField + "Cond");
            String cls = (bool.booleanValue()) ? "pass" : "fail";
            if (!bool.booleanValue())
                isPass = false;
            sb.append("<td class=\"" + cls + "\">");
        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (2b):" + sb.toString(), module);
            s = (String)rMap.get(opField);
        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (s):" + s,  module);
            sb.append(s);
            sb.append("</td>");
        }
        String passFailCls = (isPass) ? "pass" : "fail";
        sb.append("<td class=\"" + passFailCls +"\">" + passFailCls.toUpperCase() + "</td>");
        sb.append("</tr>");
        //if (Debug.infoOn()) Debug.logInfo("renderResultRowHtml, (3):" + sb.toString(), module);

        return sb.toString();
    }
}
