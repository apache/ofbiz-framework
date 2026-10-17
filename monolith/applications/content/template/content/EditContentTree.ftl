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
<form name="editContentTree" action="<#if parameters.rename?has_content><@ofbizUrl>updateDocumentTree</@ofbizUrl><#else><@ofbizUrl>addDocumentToTree</@ofbizUrl></#if>" method="post">
    <#assign content  = delegator.findOne("Content",Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("contentId",parameters.contentId), false)/>
    <#if parameters.rename?has_content>
        <div class="h3">${uiLabelMap.ContentRenameFolder}</div>
        ${uiLabelMap.ContentRenameFolder} : ${content.contentName}<br />
        <input type="text" name="contentName" value="${content.contentName}" />
        <br/> <input type="submit" value="${uiLabelMap.ContentRenameFolder}" /><a class="buttontext" href="<@ofbizUrl>navigateContent</@ofbizUrl>">${uiLabelMap.CommonCancel}</a>
    <#else>
        <div class="h3">${uiLabelMap.ContentNewFolder}</div>
        ${uiLabelMap.ContentRoot} : ${content.contentName}
        <input type="text" name="contentName" />
        <br /><input type="submit" value="${uiLabelMap.CommonCreate}"/><a class="buttontext" href="<@ofbizUrl>navigateContent</@ofbizUrl>">${uiLabelMap.CommonCancel}</a>
    </#if>
    <input type="hidden" name="contentId" value="${parameters.contentId}"/>
    <input type="hidden" name="contentAssocTypeId" value="TREE_CHILD"/>
</form>
<hr />
