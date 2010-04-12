<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#assign docLangAttr = locale.toString()?replace("_", "-")>
<#assign initialLocale = locale.toString()>
<#assign langDir = "ltr">
<#if "ar.iw"?contains(docLangAttr?substring(0, 2))>
    <#assign langDir = "rtl">
</#if>
<html lang="${docLangAttr}" dir="${langDir}" xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title?if_exists}</title>
    <script language="javascript" src="<@ofbizContentUrl>/images/prototypejs/prototype.js</@ofbizContentUrl>" type="text/javascript"></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/prototypejs/scriptaculous.js</@ofbizContentUrl>" type="text/javascript"></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/fieldlookup.js</@ofbizContentUrl>" type="text/javascript"></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/selectall.js</@ofbizContentUrl>" type="text/javascript"></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/calendarDateSelect/calendar_date_select.js</@ofbizContentUrl>" type="text/javascript"></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/calendarDateSelect/locale/${parameters.userLogin.lastLocale!initialLocale}.js</@ofbizContentUrl>" type="text/javascript"></script>
    <#if layoutSettings.javaScripts?has_content>
        <#--layoutSettings.javaScripts is a list of java scripts. -->
        <#-- use a Set to make sure each javascript is declared only once, but iterate the list to maintain the correct order -->
        <#assign javaScriptsSet = Static["org.ofbiz.base.util.UtilMisc"].toSet(layoutSettings.javaScripts)/>
        <#list layoutSettings.javaScripts as javaScript>
            <#if javaScriptsSet.contains(javaScript)>
                <#assign nothing = javaScriptsSet.remove(javaScript)/>
                <script src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>" type="text/javascript"></script>
            </#if>
        </#list>
    </#if>
    <#if layoutSettings.styleSheets?has_content>
        <#list layoutSettings.styleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_STYLESHEET?has_content>
        <#list layoutSettings.VT_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_HELPSTYLESHEET?has_content && lookupType?has_content>
        <#list layoutSettings.VT_HELPSTYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.rtlStyleSheets?has_content && langDir == "rtl">
        <#list layoutSettings.rtlStyleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_RTL_STYLESHEET?has_content && langDir == "rtl">
        <#list layoutSettings.VT_RTL_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>

    <script language="JavaScript" type="text/javascript">
        // This code inserts the value lookedup by a popup window back into the associated form element
        var re_id = new RegExp('id=(\\d+)');
        var num_id = (re_id.exec(String(window.location))
                ? new Number(RegExp.$1) : 0);
        var obj_caller = (window.opener ? window.opener.lookups[num_id] : null);
        if (obj_caller == null)
            obj_caller = window.opener;

        var bkColor = "yellow";
        function setSourceColor(src){
        if(src != null)
             src.style.backgroundColor = bkColor;
        }
        // function passing selected value to calling window
        function set_value(value) {
                if (!obj_caller) return;
                setSourceColor(obj_caller.target);
                obj_caller.target.value = value;
                obj_caller.target.fire("lookup:changed");
                if (obj_caller.target.onchange != null) {
                    obj_caller.target.onchange();                       
                }
                window.close();
        }
        // function passing selected value to calling window
        function set_values(value, value2) {
                set_value(value);
                if (!obj_caller.target2) return;
                if (obj_caller.target2 == null) return;
                setSourceColor(obj_caller.target2);
                obj_caller.target2.value = value2;
                obj_caller.target2.fire("lookup:changed");
                if (obj_caller.target2.onchange != null) {
                    obj_caller.target2.onchange();
                }
        }
        function set_multivalues(value) {
            obj_caller.target.value = value;
            var thisForm = obj_caller.target.form;
            var evalString = "";

            if (arguments.length > 2 ) {
                for(var i=1; i < arguments.length; i=i+2) {
                    evalString = "setSourceColor(thisForm." + arguments[i] + ")";
                    eval(evalString);
                    evalString = "thisForm." + arguments[i] + ".value='" + arguments[i+1] + "'";
                    eval(evalString);
                }
            }
            window.close();
         }
    </script>
</head>
<body style="background-color: WHITE;">
