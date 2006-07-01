<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Si Chen (schen@graciousstyle.com) 
 *@author     Tim Chen (timchen_sh@hotmail.com)
 *@version    $Rev$
 *@since      2.2
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
                <td colspan="4">
                  <a href="<@ofbizUrl>setOrderTerm?createNew=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
                </td>
               </tr>
               <tr>
                <td><div class="tabletext"><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
                <td><div class="tabletext"><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
                <td align="right">&nbsp;</td>
               </tr>
               <tr><td colspan="4"><hr class='sepbar'></td></tr>
                <#assign index=0>
                <#list orderTerms as orderTerm>
                  <tr>
                  <td><div class="tabletext">${orderTerm.getRelatedOne("TermType").get("description",locale)}</div></td>
                  <td><div class="tabletext">${orderTerm.termValue?default("")}</div></td>
                  <td><div class="tabletext">${orderTerm.termDays?default("")}</div></td>
                  <td align="right">
                    <a href="<@ofbizUrl>setOrderTerm?termIndex=${index}&createNew=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                        &nbsp;&nbsp;&nbsp;&nbsp;
                    <a href="<@ofbizUrl>removeOrderTerm?termIndex=${index}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
                  </td>
                  </tr>
                  <#if orderTerms.size()&lt;index >
                    <tr><td colspan="4"><hr class='sepbar'></td></tr>
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
