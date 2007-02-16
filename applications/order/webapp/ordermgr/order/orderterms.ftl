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

<#if orderTerms?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderTerms}</div>
    </div>
    <div class="screenlet-body">
     <table border="0" width="100%" cellspacing="0" cellpadding="0">
      <tr>
        <td width="35%" align="left"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
        <td width="15%" align="center"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
        <td width="15%" align="center"><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
        <td width="35%" align="center"><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
      </tr>
      <tr><td colspan="4"><hr class='sepbar'></td></tr>
      <#list orderTerms as orderTerm>
          <tr>
            <td width="35%" align="left"><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description", locale)}</div></td>
            <td width="15%" align="center"><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
            <td width="15%" align="center"><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
            <td width="35%" align="center"><div class="tabletext">${orderTerm.description?default("")}</div></td>
          </tr>
          <tr><td colspan="4">&nbsp;</td></tr>
      </#list>
     </table>
    </div>
</div>
</#if>
