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
<div>
  <div id="SearchSalesRepsList">
    <#if salesReps?? && salesReps?has_content>
      <table id="salesRepsResults" name="salesRepsResults" cellspacing="0" cellpadding="2" class="basic-table">
      <#assign alt_row = false>
        <#list salesReps as salesRep>
          <#assign salesRepInCart = false>
          <#if cartSalesReps?? && cartSalesReps?has_content>
          <#list cartSalesReps as cartSalesRep>
            <#if cartSalesRep == salesRep.partyId>
              <#assign salesRepInCart = true>
            </#if>
          </#list>
          </#if>
          <#assign person = ""/>
          <#if salesRep.lastName?has_content>
            <#assign person = person + (salesRep.lastName).trim() + " "/>
          </#if>
          <#if salesRep.firstName?has_content>
            <#assign person = person + (salesRep.firstName).trim()/>
          </#if>
          <#assign person = person + " (" + salesRep.partyId + ")"/>
          <tr <#if salesRepInCart>class="pos-cart-choose"<#else><#if alt_row>class="pos-cart-even"<#else>class="pos-cart-odd"</#if></#if>>
            <td>
              <#if salesRepInCart>
              <a href="javascript:removeSalesRep('${salesRep.partyId}');">
                &nbsp;${person}
              </a>
              <#else>
              <a href="javascript:addSalesRep('${salesRep.partyId}');">
                &nbsp;${person}
              </a>
              </#if>
            </td>
          </tr>
          <#-- toggle the row color -->
          <#assign alt_row = !alt_row>
        </#list>
      </table>
    </#if>
  </div>
</div>