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

<script language="JavaScript" type="text/javascript">
    function exportToEbay() {
        <#if toEbayStore??>
            document.products.action="<@ofbizUrl>prepareProductListing</@ofbizUrl>";
        <#else>
            document.products.action="<@ofbizUrl>ProductsExportToEbay</@ofbizUrl>";
        </#if>
        document.products.submit();
    }
</script>

<#if productIds?has_content>
  <table cellspacing="0" class="basic-table">
    <tr>
      <td align="center" colspan="2">
        <a href="javascript:exportToEbay();" class="buttontext">${uiLabelMap.EbayExportToEbay}</a>
      </td>
    </tr>
  </table>
</#if>