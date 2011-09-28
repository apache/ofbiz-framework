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
<#assign partyGroup = delegator.findOne("PartyGroup", {"partyId" : parameters.partyId?if_exists}, true)/>
<#if partyGroup?has_content>
    <#assign partyname = partyGroup.groupName?if_exists/>
</#if>
<script type="text/javascript">
    <!--
    var answer = confirm ("Are you sure you want to remove '<#if partyname?exists>${partyname}<#else>${parameters.partyId?if_exists}</#if>'?")
    if (answer)
       document.removeInternalOrg.submit();
    else
       window.close();
    // -->
</script>
<div id="rmvinternalorg" title="Remove Internal Organization">
    <form name="removeInternalOrg" method="post" action="<@ofbizUrl>removeInternalOrg</@ofbizUrl>">
        <input type="hidden" name="partyId" value="${parameters.partyId?if_exists}"/>
        <input type="hidden" name="parentpartyId" value="${parameters.parentpartyId?if_exists}"/>
    </form>
</div>