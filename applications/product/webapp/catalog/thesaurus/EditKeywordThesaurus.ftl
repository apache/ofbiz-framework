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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductAlternateKeyWordThesaurus}</h3>
    </div>
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>createKeywordThesaurus</@ofbizUrl>">
            <div>
                <span class="label">${uiLabelMap.ProductKeyword}</span><input type="text" name="enteredKeyword" size="10"/>
                <span class="label">${uiLabelMap.ProductAlternate}</span><input type="text" name="alternateKeyword" size="10"/>
                <span class="label">${uiLabelMap.ProductRelationship}</span><select name="relationshipEnumId">
                <#list relationshipEnums as relationshipEnum>
                <option value="${relationshipEnum.enumId}">${relationshipEnum.get("description",locale)}</option>
                </#list>
                </select>
                <input type="submit" value="${uiLabelMap.CommonAdd}"/>
            </div>
        </form>
        <div>
            <#list letterList as letter>
              <#if letter == firstLetter><#assign highlight=true><#else><#assign highlight=false></#if>
              <a href="<@ofbizUrl>editKeywordThesaurus?firstLetter=${letter}</@ofbizUrl>" class="buttontext"><#if highlight>[</#if>${letter}<#if highlight>]</#if></a>
            </#list>
        </div>
        <br />
        <#assign lastkeyword = "">
        <#if keywordThesauruses?has_content>
        <table cellspacing="0" class="basic-table">
            <#assign rowClass = "2">
            <#list keywordThesauruses as keyword>
              <#assign relationship = keyword.getRelatedOneCache("RelationshipEnumeration")>
              <#if keyword.enteredKeyword == lastkeyword><#assign sameRow=true><#else><#assign lastkeyword=keyword.enteredKeyword><#assign sameRow=false></#if>
              <#if sameRow == false>
                <#if (keyword_index > 0)>
                  </td>
                </tr>
                </#if>
                <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                  <td>
                    <form method="post" action="<@ofbizUrl>createKeywordThesaurus</@ofbizUrl>">
                      <div>
                        ${keyword.enteredKeyword}
                        <a href="<@ofbizUrl>deleteKeywordThesaurus?enteredKeyword=${keyword.enteredKeyword}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDeleteAll}</a>
                      </div>
                      <div>
                        <input type="hidden" name="enteredKeyword" value="${keyword.enteredKeyword}" />
                        <span class="label">${uiLabelMap.ProductAlternate}</span><input type="text" name="alternateKeyword" size="10" />
                        <span class="label">${uiLabelMap.ProductRelationship}</span><select name="relationshipEnumId"><#list relationshipEnums as relationshipEnum><option value="${relationshipEnum.enumId}">${relationshipEnum.get("description",locale)}</option></#list></select>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" />
                      </div>
                    </form>
                  </td>
                  <td>
              </#if>
              <div>
                <a href="<@ofbizUrl>deleteKeywordThesaurus?enteredKeyword=${keyword.enteredKeyword}&amp;alternateKeyword=${keyword.alternateKeyword}</@ofbizUrl>" class="buttontext">X</a>
                ${keyword.alternateKeyword}&nbsp;(${uiLabelMap.ProductRelationship}:${(relationship.get("description",locale))?default(keyword.relationshipEnumId?if_exists)})
              </div>
              <#-- toggle the row color -->
              <#if rowClass == "2">
                  <#assign rowClass = "1">
              <#else>
                  <#assign rowClass = "2">
              </#if>
            </#list>
              </td>
            </tr>
        </table>
        </#if>
    </div>
</div>