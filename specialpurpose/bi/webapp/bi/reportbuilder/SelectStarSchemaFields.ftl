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

<form action="<@ofbizUrl>ReportBuilderRenderStarSchemaReport</@ofbizUrl>">
    <input type="hidden" name="starSchemaName" value="${starSchemaName}"/>
    <table cellspacing="0" class="basic-table form-widget-table dark-grid">
        <tr class="header-row">
            <td>
                Select
            </td>
            <td>
                Field Name
            </td>
            <td>
                Field Description
            </td>
        </tr>
        <#list starSchemaFields as starSchemaField>
        <tr>
            <td>
                <input name="selectedField" value="${starSchemaField.name}" type="checkbox"/>
            </td>
            <td>
                ${starSchemaField.name}
            </td>
            <td>
                ${starSchemaField.description?default("")}
            </td>
        </tr>
        </#list>
        <tr>
            <td colspan="3">
                <input name="submitButton" type="submit" value="Render the Report"/>
            </td>
        </tr>
    </table>
</form>
