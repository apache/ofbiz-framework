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

<table cellspacing="0" class="basic-table hover-bar">
    <tr class="header-row">
        <#assign firstRecord = records[0]!/>
        <#list columnNames as columnName>
        <td<#if firstRecord?? && firstRecord[columnName]?default("")?is_number> class="align-text"</#if>>
            ${columnName}
        </td>
        </#list>
    </tr>
    <#assign alt_row = false>
    <#list records as record>
    <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
        <#list columnNames as columnName>
        <#assign columnValue = record[columnName]?default("")>
        <td<#if columnValue?is_number> class="align-text"</#if>>
            ${columnValue}
        </td>
        </#list>
    </tr>
    <#-- toggle the row color -->
    <#assign alt_row = !alt_row>
    </#list>
</table>