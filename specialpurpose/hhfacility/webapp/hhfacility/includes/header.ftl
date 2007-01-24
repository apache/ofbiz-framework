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

<html>
<head>
<script language="JavaScript" type="text/javascript">
<!--
    // Setting of field focus to initialise the 
    // handheld forms - critical when scanner is being used.
    focusField = "";
    function fieldFocus() {
        if (focusField != "") {
            document.forms[0].elements[focusField].focus();
            document.forms[0].elements[focusField].select();
        }
    }

    // Function to move to next field when enter pressed
    // event is passed as firefox needs it to get keycode instead
    // of using window.event, return false will stop form submit
    function enter(e,nextfield) {
        if (e.keyCode == 13) {
            nextfield.focus();
            nextfield.select();
            return false; 
        } else {
          return true; 
        }
    }
//-->
</script>
<link rel="stylesheet" href="/images/maincss.css" type="text/css">
<title>Hand Held Facility</title>
</head>
<body onload=fieldFocus()>
<#assign facility = parameters.facility?if_exists>
<table width=240 height=250 align=top>
<tr height=24>
<td width=100% align=top class='boxbottom'>
<!--<p align="right">${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()}</p>-->
<span class="boxhead">
<a href="<@ofbizUrl>/main</@ofbizUrl>">Main</a>
<#if facility?has_content><a href="<@ofbizUrl>/menu?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>">Menu</a></#if>
<a href="<@ofbizUrl>/logout</@ofbizUrl>">Logout</a>
</span>
</td>
</tr>
<tr class="boxtop">
<td width=100%>
</td>
</tr>
<tr height=196>
<td width=100% align=top class='boxbottom'>
