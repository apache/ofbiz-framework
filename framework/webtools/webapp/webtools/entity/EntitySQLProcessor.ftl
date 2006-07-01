<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Jacopo Cappellato (tiz@sastau.it)
-->
<span class="head1">SQL Processor</span>

<form method="post" action="EntitySQLProcessor" name="EntitySQLCommand" style="margin: 0;">
<table border="0" cellpadding="2" cellspacing="0">
    <tr>
        <td width="20%" align="right">
            <span class="tableheadtext">Group</span>
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
            <span class="tableheadtext">SQL Command</span>
        </td>
        <td>&nbsp;</td>
        <td width="80%" align="left">
            <textarea class="textAreaBox" name="sqlCommand" cols="100" rows="5">${sqlCommand?if_exists}</textarea>
        </td>
    </tr>
    <tr>
        <td width="20%" align="right">
            <span class="tableheadtext">Limit Rows To</span>
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
            <input type="submit" class="smallSubmit" name="submitButton" value="Submit"/>
        </td>
    </tr>
</table>
</form>

<span class="head1">Results</span>

<br/>
${resultMessage?if_exists}
<br/>
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