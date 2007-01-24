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
<#if productId?exists && product?exists>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductIdType}</b></div></td>
        <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductIdValue}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list goodIdentifications as goodIdentification>
    <#assign line = line + 1>
    <#assign goodIdentificationType = goodIdentification.getRelatedOneCache("GoodIdentificationType")>
    <tr valign="middle">
        <td><div class="tabletext"><#if goodIdentificationType?exists>${(goodIdentificationType.get("description",locale))?if_exists}<#else>[${(goodIdentification.goodIdentificationTypeId)?if_exists}]</#if></div></td>
        <td align="center">
            <form method="post" action="<@ofbizUrl>updateGoodIdentification</@ofbizUrl>" name="lineForm${line}"/>
                <input type="hidden" name="productId" value="${(goodIdentification.productId)?if_exists}"/>
                <input type="hidden" name="goodIdentificationTypeId" value="${(goodIdentification.goodIdentificationTypeId)?if_exists}"/>
                <input type="text" size="20" name="idValue" value="${(goodIdentification.idValue)?if_exists}" class="inputBox"/>
                <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
            </form>
        </td>
        <td align="center">
        <a href="<@ofbizUrl>deleteGoodIdentification?productId=${(goodIdentification.productId)?if_exists}&goodIdentificationTypeId=${(goodIdentification.goodIdentificationTypeId)?if_exists}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
        </td>
    </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>createGoodIdentification</@ofbizUrl>" style="margin: 0;" name="createGoodIdentificationForm">
        <input type="hidden" name="productId" value="${productId}"/>
        <input type="hidden" name="useValues" value="true"/>
    
        <div class="head2">${uiLabelMap.CommonAddId} :</div>
        <div class="tabletext">
            ${uiLabelMap.ProductIdType} :
            <select name="goodIdentificationTypeId" class="selectBox">
                <#list goodIdentificationTypes as goodIdentificationType>
                    <option value="${(goodIdentificationType.goodIdentificationTypeId)?if_exists}">${(goodIdentificationType.get("description",locale))?if_exists}</option>
                </#list>
            </select>
            ${uiLabelMap.ProductIdValue} : <input type="text" size="20" name="idValue" class="inputBox"/>&nbsp;<input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;"/>
        </div>        
    </form>
</#if>    
