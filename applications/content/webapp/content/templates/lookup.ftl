<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org -->
<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Al Byers (byersa@automationgroups.com)
 *@version    $Rev$
 *@since      3.0
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
