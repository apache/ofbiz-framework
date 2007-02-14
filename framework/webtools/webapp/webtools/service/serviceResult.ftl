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

<form method="post" action="<@ofbizUrl>saveServiceResultsToSession</@ofbizUrl>"
<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">parameter</div></td>
    <td><div class="tableheadtext">value</div></td>
    <td><div class="tableheadtext">save value?</div></td>
  </tr>

  <#list serviceResultList as srl>
  <tr>
      <#if srl.hasChild=="Y">
          <td><div class="tabletext"><a href="<@ofbizUrl>/serviceResult?servicePath=</@ofbizUrl><#if parameters.servicePath?exists>${parameters.servicePath}||</#if>${srl.key?if_exists}">${srl.key?if_exists}</a></div></td>    
      <#else>
          <td><div class="tabletext">${srl.key?if_exists}</div></td>
      </#if>
    <td><div class="tabletext">${srl.value?if_exists}</div></td>
    <td><div class="tabletext"><input type="checkbox" name="<#if parameters.servicePath?exists>${parameters.servicePath}||</#if>${srl.key?if_exists}" /></div></td>
  </tr>
  </#list>

  <tr>
    <td><div class="tabletext">&nbsp</div></td>
    <td><div class="tabletext">Clear previous params? <input type="checkbox" name="_CLEAR_PREVIOUS_PARAMS_" /></div></td>
    <td><div class="tabletext"><input type="submit" value="submit" /></div></td>
  </tr>
</table>
