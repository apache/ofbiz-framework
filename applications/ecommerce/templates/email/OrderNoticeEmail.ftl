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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title}</title>
    <#-- this needs to be fully qualified to appear in email; the server must also be available -->
    <link rel="stylesheet" href="${baseUrl}/images/maincss.css" type="text/css"/>
</head>

<body>

<#-- custom logo or text can be inserted here -->

<div class="head1">${title}</div>
<#if !isDemoStore?exists || isDemoStore><p>${uiLabelMap.OrderDemoFrontNote}.</p></#if>
<#if note?exists><p class="tabletext">${note}</p></#if>
<#if orderHeader?exists>
${screens.render("component://ecommerce/widget/OrderScreens.xml#orderheader")}
<br/>
${screens.render("component://ecommerce/widget/OrderScreens.xml#orderitems")}
<#else>
<div class="head1">Order not found with ID [${orderId?if_exists}], or not allowed to view.</div>
</#if>

</body>
</html>
