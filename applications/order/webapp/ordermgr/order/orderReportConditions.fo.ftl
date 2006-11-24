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
<#escape x as x?xml>
<fo:block space-after="40pt"/>
<#if orderHeader.getString("orderTypeId") == "SALES_ORDER">
  <fo:block font-size="14pt" font-weight="bold" text-align="center">THANK YOU FOR YOUR PATRONAGE!</fo:block>
  <fo:block font-size="8pt">
    <#--    Here is a good place to put policies and return information. -->
  </fo:block>
<#elseif orderHeader.getString("orderTypeId") == "PURCHASE_ORDER">
  <fo:block font-size="8pt">
    <#-- Here is a good place to put boilerplate terms and conditions for a purchase order. -->
  </fo:block>
</#if>
