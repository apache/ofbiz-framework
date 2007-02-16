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

<table border="0" width="100%" cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width="100%">
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <#if orderTerms?has_content && !requestParameters.createNew?exists>
            <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
            <input type="hidden" name="finalizeMode" value="term"/>
             <table width="100%" border="0" cellpadding="1" cellspacing="0">
               <tr>
                <td colspan="5">
                  <a href="<@ofbizUrl>setOrderTerm?createNew=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
                </td>
               </tr>
               <tr>
                <td><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
                <td align="right">&nbsp;</td>
               </tr>
               <tr><td colspan="5"><hr class='sepbar'></td></tr>
                <#assign index=0>
                <#list orderTerms as orderTerm>
                  <tr>
                  <td><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description",locale)}</div></td>
                  <td><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
                  <td><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
                  <td><div class="tabletext">${orderTerm.description?default("")}</div></td>
                  <td align="right">
                    <a href="<@ofbizUrl>setOrderTerm?termIndex=${index}&createNew=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                        &nbsp;&nbsp;&nbsp;&nbsp;
                    <a href="<@ofbizUrl>removeCartOrderTerm?termIndex=${index}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
                  </td>
                  </tr>
                  <#if orderTerms.size()&lt;index >
                    <tr><td colspan="5"><hr class='sepbar'></td></tr>
                  </#if>
                  <#assign index=index+1>
                </#list>
             </table>
            </form>
           </td>
          </tr>
          </table>
            <#else>
              <#if !orderTerms?has_content || requestParameters.createNew?exists>
               <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
                 <input type="hidden" name="finalizeMode" value="term"/>
               </form>
               <form method="post" action="<@ofbizUrl>addOrderTerm</@ofbizUrl>" name="termform">
                <input type="hidden" name="partyId" value="${cart.partyId?default("_NA_")}"/>
                <input type="hidden" name="finalizeMode" value="term"/>
                <input type="hidden" name="termIndex" value="${termIndex?default(-1)}"/>
                <table width="100%" border="0" cellpadding="1" cellspacing="0">
                  <tr>
                    <td width="26%" align="right" valign="top">
                       <div class="tabletext">${uiLabelMap.OrderOrderTermType}</div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                       <select name="termTypeId" class="selectBox">
                          <#if termTypes?has_content>
                               <option value=""/>
                             <#list termTypes as termType>
                               <option value="${termType.termTypeId}" <#if termTypeId?default("")==termType.termTypeId> selected</#if>>${termType.get("description",locale)}</option>
                             </#list>
                          </#if>
                       </select>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top">
                       <div class="tabletext">${uiLabelMap.OrderOrderTermValue}</div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" class="inputBox" size="30" maxlength="60" name="termValue" value="${termValue?if_exists}"/>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top">
                       <div class="tabletext">${uiLabelMap.OrderOrderTermDays}</div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" class="inputBox" size="30" maxlength="60" name="termDays" value="${termDays?if_exists}"/>
                    </td>
                  </tr>
                  <tr>
                    <td width="26%" align="right" valign="top">
                       <div class="tabletext">${uiLabelMap.CommonDescription}</div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td width="74%">
                      <input type="text" class="inputBox" size="30" maxlength="255" name="description" value="${description?if_exists}"/>
                    </td>
                  </tr>
                  <tr><td colspan="3" align="middle"><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"/></td></tr>
                </table>
              </form>
            </#if>
          </td>
        </tr>
      </table>
     </#if>
    </td>
  </tr>
</table>
<br/>
