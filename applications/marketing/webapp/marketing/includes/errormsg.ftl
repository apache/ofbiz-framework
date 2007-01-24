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

<#-- display the error messages -->
<#if requestAttributes.errorMessageList?has_content>
<div class="errorMessage">The following errors occurred:</div><br/>
<ul>
  <#list requestAttributes.errorMessageList as errorMsg>
    <li class="errorMessage">${errorMsg}</li>
  </#list>
</ul>
</#if>
<#if requestAttributes.eventMessageList?has_content>
<div class="eventMessage">The following occurred:</div><br/>
<ul>
  <#list requestAttributes.eventMessageList as eventMsg>
    <li class="eventMessage">${eventMsg}</li>
  </#list>
</ul>
</#if>
