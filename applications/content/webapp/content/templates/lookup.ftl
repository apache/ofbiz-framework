<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org -->
<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#assign layoutSettings = requestAttributes.layoutSettings>
<html>
<head>
    <#assign layoutSettings = requestAttributes.layoutSettings>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>${layoutSettings.companyName}: ${page.title}</title>
    <script language='javascript' src='<@ofbizContentUrl>/images/calendar1.js</@ofbizContentUrl>' type='text/javascript'></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/fieldlookup.js</@ofbizContentUrl>" type="text/javascript"></script>
    <link rel='stylesheet' href='<@ofbizContentUrl>/images/maincss.css</@ofbizContentUrl>' type='text/css'>
    <link rel='stylesheet' href='<@ofbizContentUrl>/images/tabstyles.css</@ofbizContentUrl>' type='text/css'>    



    <script language="JavaScript" type="text/javascript">
        var win_opener = window.opener;
        
        function execRemoteCall(requestName,dataResourceId, contentId, contentIdTo, mapKey) {
                if (!win_opener) return;
                var url = "<@ofbizUrl>" + requestName + "/~dataResourceId=" 
                        + dataResourceId 
                        + "/~contentId=" + contentId 
                        + "/~contentIdTo=" + contentIdTo 
                        + "/~mapKey=" + mapKey 
                        + "</@ofbizUrl>";
                window.close();
                win_opener.location.replace(url);
        }
    </script>

    <script language="JavaScript" type="text/javascript">
        // This code inserts the value lookedup by a popup window back into the associated form element
        var re_id = new RegExp('id=(\\d+)');
        var num_id = (re_id.exec(String(window.location))
                ? new Number(RegExp.$1) : 0);
        var obj_caller = (window.opener ? window.opener.lookups[num_id] : null);
        if (obj_caller == null) 
            obj_caller = window.opener;
        
        
        // function passing selected value to calling window
        function set_value(value) {
                if (!obj_caller) return;
                window.close();
                obj_caller.target.value = value;
        }
        // function refreshes caller after posting new entry
        function refresh_caller(value) {
            var str = "/postSubContent";
            <#assign separator="?"/>
            <#if requestAttributes.contentId?exists>
                str += '${separator}';
                str += "contentId=" + "${requestAttributes.contentId}";
                <#assign separator="&"/>
            </#if>
            <#if requestAttributes.mapKey?exists>
                str += '${separator}';
                str += "mapKey=" + "${requestAttributes.mapKey}";
                <#assign separator="&"/>
            </#if>
                str += '${separator}';
                str += value;
            var requestStr = '"<@ofbizUrl>"' + escape(str)</@ofbizUrl> + '"';

            window.opener.replace(requestStr);
        }
    </script>


</head>

<body  >
  ${pages.get(page.path)}
</body>
</html>
