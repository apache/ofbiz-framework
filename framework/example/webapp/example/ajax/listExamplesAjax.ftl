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
    new Ajax.PeriodicalUpdater('example-list', '<@ofbizUrl>/authview/listExamples</@ofbizUrl>');
 </script>
 <table cellspacing="0" class="basic-table form-widget-table dark-grid">
    <tr class="header-row">
        <td>Example ID</td>
        <td>Name</td>
        <td>Type</td>
        <td>Status</td>
        <td>Description</td>
    </tr>
    <#list examples as example>
        <#assign type = (example.getRelatedOne("ExampleType"))?if_exists/>
        <#assign status = (example.getRelatedOne("StatusItem"))?if_exists/>
        <tr>
            <td><a class="buttontext" href="<@ofbizUrl>EditExample?exampleId=${example.exampleId}</@ofbizUrl>">${example.exampleId}</a></td>
            <td>${example.exampleName?if_exists}</td>
            <td>${(type.description)?if_exists}</td>
            <td>${(status.description)?if_exists}</td>
            <td>${example.description?if_exists}</td>    
        </tr>
    </#list>
 </table>

<br/>