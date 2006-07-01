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

<br/>
<#list taskCosts as taskCost>
    <#assign task = taskCost.task>
    <#assign costsForm = taskCost.costsForm>
    <div>
        <span class="head2">${task.workEffortName?if_exists} [${task.workEffortId}]</span>
    </div>
    ${costsForm.renderFormString(context)}
    <br/>
</#list>