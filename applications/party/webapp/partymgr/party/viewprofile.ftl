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

<#if party?has_content>
  <div class="align-float">
    <#if showOld>
      <a href="<@ofbizUrl>viewprofile?partyId=${party.partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.PartyHideOld}</a>
    <#else>
      <a href="<@ofbizUrl>viewprofile?partyId=${party.partyId}&SHOW_OLD=true</@ofbizUrl>" class="smallSubmit">${uiLabelMap.PartyShowOld}</a>
    </#if>
  </div>
  <br class="clear" />
  <br/>

  <#-- Party Info -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Party')}

  <#-- Contact Info -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Contact')}

  <#-- Loyalty Points -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#LoyaltyPoints')}

  <#-- Payment Info  -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#PaymentMethods')}

  <#-- AVS Strings -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#AvsSettings')}

  <#-- Financial Account Summary -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#FinAccounts')}

  <#-- UserLogins -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#UserLogin')}

  <#-- Party Attributes -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Attributes')}

  <#-- Visits -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Visits')}

  <#-- Current Cart -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Cart')}

  <#-- Serialized Inventory Summary -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#SerializedInventory')}

  <#-- Subscription Summary -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Subscriptions')}

  <#-- Party Content -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Content')}

  <#-- Party Notes -->
  ${screens.render('component://party/widget/partymgr/ProfileScreens.xml#Notes')}
<#else>
  ${uiLabelMap.PartyNoPartyFoundWithPartyId}: ${parameters.partyId?if_exists}
</#if>
