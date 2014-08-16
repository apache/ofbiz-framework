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
<table cellspacing="0" width="100%">
    <tr>
      <td align="left">
                    <#if (viewIndex > 0)>
                        <#assign url='/views/'+tabButtonItem+'?'+curFindString+'&amp;VIEW_SIZE='+viewSize+'&amp;VIEW_INDEX='+viewIndexFirst>
                        <a href="javascript:nextPrevDocumentList('${url}');" class="nav-next">${uiLabelMap.CommonFirst}</a>|
                          <#assign url='/views/'+tabButtonItem+'?'+curFindString+'&amp;VIEW_SIZE='+viewSize+'&amp;VIEW_INDEX='+viewIndexPrevious>
                        <a href="javascript:nextPrevDocumentList('${url}');" class="nav-previous">${uiLabelMap.CommonPrevious}</a>|
                    </#if>
                    <#if (arraySize > 0)>
                        ${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${arraySize}
                    </#if>
                    <#if (arraySize > highIndex)>
                        <#assign url='/views/'+tabButtonItem+'?'+curFindString+'&amp;VIEW_SIZE='+viewSize+'&amp;VIEW_INDEX='+viewIndexNext>|
                        <a href="javascript:nextPrevDocumentList('${url}');" class="nav-next">${uiLabelMap.CommonNext}</a>
                        <#assign url='/views/'+tabButtonItem+'?'+curFindString+'&amp;VIEW_SIZE='+viewSize+'&amp;VIEW_INDEX='+viewIndexLast>|
                        <a href="javascript:nextPrevDocumentList('${url}');" class="nav-next">${uiLabelMap.CommonLast}</a>
                    </#if>
        </td>
        <td></td>
        <td></td>
  </tr>
   <#if (arraySize > 0)>
           <tr><td colspan="3"><hr /></td></tr>
   </#if>
</table>
<table class="basic-table hover-bar" cellspacing="0">
<#if tabButtonItem=="ListContentTree">
<#--Form ListContentTree-->
  <tr class="header-row">
    <td>${uiLabelMap.FormFieldTitle_contentId}</td>
    <td>${uiLabelMap.FormFieldTitle_coContentName}</td>
    <td>${uiLabelMap.FormFieldTitle_mimeTypeId}</td>
  </tr>
<#elseif tabButtonItem=="ListDocument">
<#--Form ListDocument-->
 <tr class="header-row">
    <td>${uiLabelMap.FormFieldTitle_contentId}</td>
    <td>${uiLabelMap.CommonView}</td>
    <td>${uiLabelMap.FormFieldTitle_contentTypeId}</td>
    <td>${uiLabelMap.FormFieldTitle_mimeTypeId}</td>
    <td>${uiLabelMap.FormFieldTitle_contentStatusId}</td>
    <td>${uiLabelMap.FormFieldTitle_caCratedDate}</td>
    <td>${uiLabelMap.CommonDelete}</td>
  </tr>
</#if>
<#if contentAssoc?has_content>  
       <#assign alt_row = false/>
       <#assign listcount=0>
      <#list contentAssoc as contentData>
      <#if tabButtonItem=="ListContentTree">
        <#--Form ListContentTree-->
          <tr <#if alt_row> class="alternate-row"</#if>> 
              <td><a class="plain" href="javascript:set_value('${contentData.contentId!}')">${contentData.contentId!}</a></td>
              <td>${contentData.contentName!}</td>
              <td>${contentData.mimeTypeId!}</td>
          </tr>
      <#elseif tabButtonItem=="ListDocument">
          <#--Form ListDocument-->
          <tr <#if alt_row> class="alternate-row"</#if>>
              <td><a class="plain" href="/content/control/editContent?contentId=${contentData.contentId!}">${contentData.contentName!}[${contentData.contentId!}]</a></td>
              <td><a class="plain" href="/content/control/showContent?contentId=${contentData.contentId!}" target="_blank">${uiLabelMap.CommonView}</a></td>
              <td>${contentData.contentTypeId!}</td>
              <td>${contentData.mimeTypeId!}</td>
              <td>${contentData.statusId!}</td>
              <#if contentData.caFromDate?has_content>
             <#assign caFromDate = Static["org.ofbiz.base.util.UtilDateTime"].toDateString(contentData.caFromDate, "dd/MM/yyyy")/>
            </#if> 
              <td>${caFromDate!}</td>
              <td><a href="javascript:document.listDocumentForm_${listcount}.submit()" >${uiLabelMap.CommonDelete}</a></td>
          </tr>
          <form action="<@ofbizUrl>removeDocumentFromTree</@ofbizUrl>" name="listDocumentForm_${listcount}" method="post">
            <input type="hidden" name="contentId" value="${contentData.contentIdStart!}"/>
            <input type="hidden" name="contentIdTo" value="${contentData.contentId!}"/>
            <input type="hidden" name="contentAssocTypeId" value="${contentData.caContentAssocTypeId!}"/>
            <input type="hidden" name="fromDate" value="${contentData.fromDate!}"/>
          </form>
     </#if>
         <#assign alt_row = !alt_row/>
         <#assign listcount=listcount+1>
      </#list>
</#if>
</table>
