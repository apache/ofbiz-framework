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

<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>  
  <p class="head1">${uiLabelMap.OrderAddNote}</p>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.createnoteform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>

  <form method="post" action="<@ofbizUrl>createordernote/${donePage}</@ofbizUrl>" name="createnoteform">
    <table width="90%" border="0" cellpadding="2" cellspacing="0">
      <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.OrderNote}</div></td>
        <td width="54%">
          <textarea name="note" class="textAreaBox" rows="5" cols="70"></textarea>
        </td>
      </tr> 
      <tr> 
         <td/><td class="tabletext">${uiLabelMap.OrderInternalNote} : 
	 <select class="selectBox" name="internalNote" size="1"><option value=""></option><option value="Y" selected>${uiLabelMap.CommonYes}</option><option value="N">${uiLabelMap.CommonNo}</option></select></td>
      </tr>
      <tr>
	 <td/><td class="tabletext"><i>${uiLabelMap.OrderInternalNoteMessage}</i></td>
    </table>
  </form>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.createnoteform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
  
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
