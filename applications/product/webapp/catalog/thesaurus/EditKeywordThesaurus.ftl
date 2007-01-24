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
<div class="head1">${uiLabelMap.ProductAlternateKeyWordThesaurus}</div>
<form method="post" action="<@ofbizUrl>createKeywordThesaurus</@ofbizUrl>">
    <div class="tabletext">
        ${uiLabelMap.ProductKeyword} :<input type="text" name="enteredKeyword" size="10" class="inputBox"/>
        ${uiLabelMap.ProductAlternate} :<input type="text" name="alternateKeyword" size="10" class="inputBox"/>
        ${uiLabelMap.ProductRelationship} :<select name="relationshipEnumId" class="selectBox"><#list relationshipEnums as relationshipEnum><option value="${relationshipEnum.enumId}">${relationshipEnum.get("description",locale)}</option></#list></select>
        <input type="submit" value="${uiLabelMap.CommonAdd}"/>
    </div>
</form>

<div class="tabletext">
    <#list letterList as letter>
      <#if letter == firstLetter><#assign highlight=true><#else><#assign highlight=false></#if>
      <a href="<@ofbizUrl>editKeywordThesaurus?firstLetter=${letter}</@ofbizUrl>" class="buttontext"><#if highlight>[</#if>[${letter}]<#if highlight>]</#if></a>
    </#list>
</div>
<br/>

<#assign lastkeyword = "">
<table border="1" cellpadding="2" cellspacing="0">
    <#list keywordThesauruses as keyword>
      <#assign relationship = keyword.getRelatedOneCache("RelationshipEnumeration")>
      <#if keyword.enteredKeyword == lastkeyword><#assign sameRow=true><#else><#assign lastkeyword=keyword.enteredKeyword><#assign sameRow=false></#if>
      <#if sameRow == false>
        <#if (keyword_index > 0)>
          </td>
        </tr>
        </#if>
        <tr>
          <td>
            <form method="post" action="<@ofbizUrl>createKeywordThesaurus</@ofbizUrl>">
              <div class="tabletext">
                <b>${keyword.enteredKeyword}</b>
                <a href="<@ofbizUrl>deleteKeywordThesaurus?enteredKeyword=${keyword.enteredKeyword}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDeleteAll}]</a>
              </div>
              <div class="tabletext">
                <input type="hidden" name="enteredKeyword" value=${keyword.enteredKeyword}>
                ${uiLabelMap.ProductAlternate} : <input type="text" name="alternateKeyword" size="10">
                ${uiLabelMap.ProductRelationship} :<select name="relationshipEnumId" class="selectBox"><#list relationshipEnums as relationshipEnum><option value="${relationshipEnum.enumId}">${relationshipEnum.get("description",locale)}</option></#list></select>
                <input type="submit" value="${uiLabelMap.CommonAdd}">
              </div>
            </form>
          </td>
          <td align="left">
      </#if>
      <div class="tabletext">
        <a href="<@ofbizUrl>deleteKeywordThesaurus?enteredKeyword=${keyword.enteredKeyword}&alternateKeyword=${keyword.alternateKeyword}</@ofbizUrl>" class="buttontext">[X]</a>
        <b>${keyword.alternateKeyword}</b>&nbsp;(${uiLabelMap.ProductRelationship}:${(relationship.get("description",locale))?default(keyword.relationshipEnumId?if_exists)})
      </div>
    </#list>
      </td>
    </tr>
</table>
