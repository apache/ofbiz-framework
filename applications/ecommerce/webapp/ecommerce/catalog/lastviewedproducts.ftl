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
<#assign uiLabelMap = requestAttributes.uiLabelMap>
<table border="0" width="100%" cellpadding="3">
  <tr>
    <td colspan="2">
      <div class="head1">
       ${uiLabelMap.ProductProductsLastViewed}
      </div>
    </td>
  </tr>
</table>

<#if sessionAttributes.lastViewedProducts?exists && sessionAttributes.lastViewedProducts?has_content>
<br/>
<center>
  <table width='100%' border='0' cellpadding='0' cellspacing='0'>        
    <#list sessionAttributes.lastViewedProducts as productId>              
      <tr><td><hr class="sepbar"/></td></tr>
      <tr>
        <td>
          ${setRequestAttribute("optProductId", productId)}
          ${setRequestAttribute("listIndex", productId_index)}
          ${pages.get("/catalog/productsummary.ftl")}
        </td>
      </tr>
    </#list>
    <tr><td colspan="2"><hr class="sepbar"/></td></tr>
  </table>
</center>
<#else>
<table border="0" width="100%" cellpadding="2">
  <tr>
    <td colspan="2"><hr class="sepbar"/></td>
  </tr>
  <tr>
    <td>
      <div class='tabletext'>${uiLabelMap.ProductNotViewedAnyProducts}.</DIV>
    </td>
  </tr>
</table>
</#if>
