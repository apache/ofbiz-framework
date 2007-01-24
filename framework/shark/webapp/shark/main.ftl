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
<#if requestAttributes.uiLabelMap?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left" width="90%" >
            <div class="boxhead">&nbsp;${uiLabelMap.SharkMainPage}</div>
          </td>        
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width="100%">
      <p>&nbsp;</p>
      <ul>
        <li><a href="<@ofbizUrl>repository</@ofbizUrl>" class="buttontext">XPDL Repository</a>
        <li><a href="<@ofbizUrl>processes</@ofbizUrl>" class="buttontext">Process List</a>
        <li><a href="<@ofbizUrl>worklist</@ofbizUrl>" class="buttontext">Work List</a>
      </ul>
      <p>&nbsp;</p>
    </td>
  </tr>
</table>
