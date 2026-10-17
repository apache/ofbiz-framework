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
<#if !visualTheme?has_content>
    <#-- Try to resolve the default theme-->
    <#assign visualTheme = Static["org.apache.ofbiz.widget.model.ThemeFactory"].resolveVisualTheme(null)/>
</#if>
<#if visualTheme?has_content>
    <#assign errorPageLocation = visualTheme.modelTheme.getErrorTemplateLocation('screen')/>
</#if>
<#if errorPageLocation?has_content>
    <#include visualTheme.modelTheme.getErrorTemplateLocation('screen')/>
<#else>
<#-- Not error page found to we use a raw output -->
<html>
<head>
    <title>OFBiz Message</title><meta http-equiv="Content-Type" content="text/html">
</head>
<body bgcolor="#FFFFFF">
<div align="center">
    <h1>ERROR MESSAGE</h1>
    <hr>
    <p>${request.getAttribute('_ERROR_MESSAGE_')?replace("\n", "<br/>")}</p>
</div>
</body>
</html>
</#if>