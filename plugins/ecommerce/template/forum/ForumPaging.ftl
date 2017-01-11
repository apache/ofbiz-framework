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

<#assign viewIndex = viewIndex?default(0)>
<#assign lowIndex = viewIndex?int * viewSize?int + 1>
<#assign highIndex = viewIndex?int * viewSize?int + viewSize>
<#--<br />== viewIndex: ${viewIndex} ==viewSize: ${viewSize} ==lowIndex: ${lowIndex}== highIndex: ${highIndex} == ListSize: ${listSize}-->
<#if forumMessages?has_content && forumMessages?size gt 0>
  <#assign listSize = forumMessages?size/>
  <#if highIndex gt listSize><#assign highIndex = listSize></#if>
<div class="product-prevnext">
  <#assign r = listSize / viewSize />
  <#assign viewIndexMax = Static["java.lang.Math"].ceil(r)>
  <select name="pageSelect" class="selectBox" onchange="window.location=this[this.selectedIndex].value;">
    <option value="#">${uiLabelMap.CommonPage} ${viewIndex?int+1} ${uiLabelMap.CommonOf} ${viewIndexMax}</option>
      <#list 1..viewIndexMax as curViewNum>
        <option
            value="<@ofbizUrl>showforum?forumId=${parameters.forumId}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${curViewNum?int-1}</@ofbizUrl>">
          ${uiLabelMap.CommonGotoPage} ${curViewNum}
        </option>
      </#list>
  </select>
  <b>
    <#if (viewIndex?int >0)>
      <a href="<@ofbizUrl>showforum?forumId=${parameters.forumId}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex?int-1}</@ofbizUrl>"
          class="buttontext">
        ${uiLabelMap.CommonPrevious}
      </a> |
    </#if>
    <#if (listSize?int > 0)>
      <span>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
    </#if>
    <#if highIndex?int < listSize?int>
      | <a
        href="<@ofbizUrl>showforum?forumId=${parameters.forumId}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex?int+1}</@ofbizUrl>"
        class="buttontext">${uiLabelMap.CommonNext}</a>
    </#if>
  </b>
</div>
</#if>
