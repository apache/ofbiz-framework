<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<span class="head1">${uiLabelMap.WebtoolsSqlProcessor}</span>

<form method="post" action="EntitySQLProcessor" name="EntitySQLCommand" style="margin: 0;">
<table border="0" cellpadding="2" cellspacing="0">
    <tr>
        <td width="20%" align="right">
            <span class="tableheadtext">${uiLabelMap.CommonGroup}</span>
        </td>
        <td>&nbsp;</td>
        <td width="80%" align="left">
            <select name="group" class="selectBox">
                <#list groups as group>
                    <option value="${group}" <#if selGroup?exists><#if group = selGroup>selected</#if></#if>>${group}</option>
                </#list>
            </select>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right">
            <span class="tableheadtext">${uiLabelMap.WebtoolsSqlCommand}</span>
        </td>
        <td>&nbsp;</td>
        <td width="80%" align="left">
            <textarea class="textAreaBox" name="sqlCommand" cols="100" rows="5">${sqlCommand?if_exists}</textarea>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right">
            <span class="tableheadtext">${uiLabelMap.WebtoolsLimitRowsTo}</span>
        </td>
        <td>&nbsp;</td>
        <td width="80%" align="left">
            <input class="tabletext" name="rowLimit" value="${rowLimit?default(200)}"/>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right">&nbsp;</td>
        <td>&nbsp;</td>
        <td width="80%" align="left" colspan="4">
            <input type="submit" class="smallSubmit" name="submitButton" value="${uiLabelMap.CommonSubmit}"/>
        </td>
    </tr>
</table>
</form>

<span class="head1">${uiLabelMap.WebtoolsResults}</span>

<div class="tabletext">${resultMessage?if_exists}</div>
<#if columns?has_content>
    <table border="1" cellpadding="2" cellspacing="0">
        <tr>
        <#list columns as column>
            <td><div class="tabletext"><b>${column}</b></div></td>
        </#list>
        </tr>
        <#if records?has_content>
        <#list records as record>
            <tr>
            <#list record as field>
                <td><div class="tabletext">${field?if_exists}</div></td>
            </#list>
            </tr>
        </#list>
        </#if>
    </table>
</#if>
