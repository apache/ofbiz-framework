<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Olivier Heintz (olivier.heintz@nereide.biz)
 *@version    $Rev$
 *@since      2.1
-->

<div class="head1">${uiLabelMap.PageTitleEditBillingAccountTerms} - ${uiLabelMap.AccountingAccountId}: ${billingAccount.billingAccountId}</div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0">
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.PartyTerm}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonValue}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonUom}</div></td>
    <td>&nbsp;</td>
  </tr>
  <tr><td colspan="5"><hr class="sepbar"></td></tr>
  <#if !billingAccountTerms?exists || billingAccountTerms?size == 0>
    <tr>
      <td colspan="5"><div class="tabletext">${uiLabelMap.AccountingNoBillingAccountTerm}</div></td>
    </tr>
  <#else>
    <#list billingAccountTerms as term>
    <#assign termType = term.getRelatedOne("TermType")>
    <#if term.uomId?exists>
      <#assign uom = term.getRelatedOne("Uom")>
    </#if>
    <tr>
      <td><div class="tabletext">${(termType.get("description",locale))?if_exists}</div></td>
      <td><div class="tabletext">${term.termValue?if_exists}</div></td>
      <td><div class="tabletext"><#if uom?has_content>${uom.get("description",locale)?if_exists}<#else>&nbsp;</#if></div></td>
      <td align="right">  
        <a href="<@ofbizUrl>EditBillingAccountTerms?billingAccountId=${term.billingAccountId}&billingAccountTermId=${term.billingAccountTermId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>&nbsp;
        <a href="<@ofbizUrl>removeBillingAccountTerm?billingAccountId=${term.billingAccountId}&billingAccountTermId=${term.billingAccountTermId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a> 
      </td>
    </tr>
    </#list>
  </#if>
</table>

<br/>
<#if billingAccountTerm?has_content>
    <div class="head1">${uiLabelMap.PageTitleEditBillingAccountTerms}</div>
    <br/>
    <form name="billingform" method="post" action="<@ofbizUrl>updateBillingAccountTerm</@ofbizUrl>">
      <input type="hidden" name="billingAccountTermId" value="${billingAccountTerm.billingAccountTermId}">
<#else>
    <div class="head1">${uiLabelMap.AccountingCreateBillingAccountTerm}</div>
    <br/>
    <form name="billingform" method="post" action="<@ofbizUrl>createBillingAccountTerm</@ofbizUrl>">
</#if>
  <input type="hidden" name="billingAccountId" value="${billingAccount.billingAccountId}">
  <table width="90%" border="0" cellpadding="2" cellspacing="0"> 
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyTermType}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select class="selectBox" name="termTypeId">
          <#list termTypes as termType>
          <option value="${termType.termTypeId}" <#if termData?has_content && termData.termTypeId?default("") == termType.termTypeId>SELECTED</#if>>${(termType.get("description",locale))?if_exists}</option>
          </#list>
        </select>
      *</td>
    </tr>  
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonUom}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select class="selectBox" name="uomId">
          <option></option>
          <#list uoms as uom>
          <option value="${uom.uomId}" <#if termData?has_content && termData.uomId?default("") == uom.uomId>SELECTED</#if>>${uom.get("description",locale)?if_exists}</option>
          </#list>
        </select>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.PartyTermValue}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="10" name="termValue" value="${termData.termValue?if_exists}">
      *</td>
    </tr>         
    <tr>
      <td width="26%" align="right" valign="top">
        <input type="submit" value="${uiLabelMap.CommonSave}" class="smallSubmit">
      </td>
      <td width="5">&nbsp;</td>
      <td width="74%">&nbsp;</td>
    </tr>
  </table>
</form>

