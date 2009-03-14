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
  <title>${uiLabelMap.EcommerceTellAFriend}</title>
</head>
<body class="ecbody">
  <center>
    <form name="tellafriend" action="<@ofbizUrl>emailFriend</@ofbizUrl>" method="post">
      <#if requestParameters.productId?exists>
        <input type="hidden" name="pageUrl" value="<@ofbizUrl fullPath="true" encode="false" secure="false">/product?product_id=${requestParameters.productId}</@ofbizUrl>">
      <#elseif requestParameters.categoryId?exists>
        <input type="hidden" name="pageUrl" value="<@ofbizUrl fullPath="true" encode="false" secure="false">/category?category_id=${requestParameters.categoryId}</@ofbizUrl>">
      <#else>
        <#assign cancel = "Y">
      </#if>
      <#if !cancel?exists>
        <table>
          <tr>
            <td>${uiLabelMap.CommonYouremail}:</td>
            <td><input type="text" name="sendFrom" size="30"></td>
          </tr>
          <tr>
            <td>${uiLabelMap.CommonEmailTo}:</td>
            <td><input type="text" name="sendTo" size="30"></td>
          </tr>
          <tr>
            <td colspan="2" align="center">${uiLabelMap.CommonMessage}</td>
          </tr>
          <tr>
            <td colspan="2" align="center">
              <textarea cols="40"  rows="5" name="message"></textarea>
            </td>
          </tr>
          <tr>
            <td colspan="2" align="center">
              <input type="submit" value="${uiLabelMap.CommonSend}">
            </td>
          </tr>
        </table>
      <#else>
        <script language="JavaScript" type="text/javascript">
        <!-- //
        window.close();
        // -->
        </script>
        <div class="tabletext">${uiLabelMap.EcommerceTellAFriendSorry}</div>
      </#if>
    </form>
  </center>
</body>
</html>
