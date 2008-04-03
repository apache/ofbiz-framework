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

<script type="text/javascript">
    function submitForm(form) {
        submitFormDisableSubmits(form);
        new Ajax.Request(form.action, { parameters: form.serialize(true) });
        new Ajax.Updater('create-form', '<@ofbizUrl>/authview/createExampleForm</@ofbizUrl>');
    }
</script>
<form id="createForm" method="post" action="<@ofbizUrl>createExample</@ofbizUrl>" class="basic-form"
      onSubmit="javascript:submitFormDisableSubmits(this)" name="EditExample">
    <table cellspacing="0">
        <tr>
            <td class="label">Type</td>
            <td>
                <select name="exampleTypeId" size="1">
                    <#list types as type>
                        <option value="${type.exampleTypeId}">${type.description}</option>
                    </#list>
                </select>
            </td>
        </tr>
        <tr>
            <td class="label">Status</td>
            <td>
                <select name="statusId" size="1">
                    <#list statusItems as statusItem>
                        <option value="${statusItem.statusId}">${statusItem.description}</option>
                    </#list>
                </select>
            </td>
        </tr>
        <tr>
            <td class="label">Example Name</td>
            <td><input type="text" name="exampleName" size="40" maxlength="60" autocomplete="off"/></td>
        </tr>
        <tr>
            <td class="label">Description</td>
            <td><input type="text" name="description" size="60" maxlength="250" autocomplete="off"/></td>
        </tr>
        <tr>
            <td class="label">Long Description</td>
            <td><textarea name="longDescription" cols="60" rows="2"></textarea></td>
        </tr>
        <tr>
            <td class="label">Comments</td>
            <td><input type="text" name="comments" size="60" maxlength="250" autocomplete="off"/></td>
        </tr>
        <tr>
            <td class="label">Example Size</td>
            <td><input type="text" name="exampleSize" size="6" autocomplete="off"/></td>
        </tr>
        <tr>
            <td class="label">Example Date</td>
            <td><input type="text" name="exampleDate" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30"/><a
                    href="javascript:call_cal(document.EditExample.exampleDate,'2008-04-03%2010:29:25.526');"><img
                    src="/images/cal.gif" width="16" height="16" border="0" alt="View Calendar" title="View Calendar"/></a>
            </td>
        </tr>
        <tr>
            <td class="label">Another Date</td>
            <td><input type="text" name="anotherDate" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30"/><a
                    href="javascript:call_cal(document.EditExample.anotherDate,'2008-04-03%2010:29:25.526');"><img
                    src="/images/cal.gif" width="16" height="16" border="0" alt="View Calendar" title="View Calendar"/></a>
            </td>
        </tr>
        <tr>
            <td class="label">Another Text</td>
            <td><select name="anotherText" size="1">
                <option value="">&nbsp;</option>
                <option value="Explicit Option">Explicit Option</option>
                <option value="Configurable Good">Configurable Good</option>
                <option value="Configurable Good Configuration">Configurable Good Configuration</option>
                <option value="Digital Good">Digital Good</option>
                <option value="Finished Good">Finished Good</option>
                <option value="Finished/Digital Good">Finished/Digital Good</option>
                <option value="Fixed Asset Usage">Fixed Asset Usage</option>
                <option value="Good">Good</option>
                <option value="Marketing Package: Auto Manufactured">Marketing Package</option>
                <option value="Marketing Package: Pick Assembly">Marketing Package: Pick Assembly</option>
                <option value="Raw Material">Raw Material</option>
                <option value="Service">Service</option>
                <option value="Subassembly">Subassembly</option>
                <option value="Work In Process">Work In Process</option>
            </select></td>
        </tr>
        <tr>
            <td class="label">&nbsp;</td>
            <td colspan="4"><input type="button" name="submitButton" value="Create" onclick="javascript:submitForm($('createForm'));"/></td>
        </tr>
    </table>
</form>

