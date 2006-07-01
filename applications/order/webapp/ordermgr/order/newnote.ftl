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
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->

<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>  
  <p class="head1">${uiLabelMap.OrderAddNote}</p>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.createnoteform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>

  <form method="post" action="<@ofbizUrl>createordernote/${donePage}</@ofbizUrl>" name="createnoteform">
    <table width="90%" border="0" cellpadding="2" cellspacing="0">
      <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.OrderNote}</div></td>
        <td width="54%">
          <textarea name="note" class="textAreaBox" rows="5" cols="70"></textarea>
        </td>
      </tr> 
      <tr> 
         <td/><td class="tabletext">${uiLabelMap.OrderInternalNote} : 
	 <select class="selectBox" name="internalNote" size="1"><option value=""></option><option value="Y" selected>${uiLabelMap.CommonYes}</option><option value="N">${uiLabelMap.CommonNo}</option></select></td>
      </tr>
      <tr>
	 <td/><td class="tabletext"><i>${uiLabelMap.OrderInternalNoteMessage}</i></td>
    </table>
  </form>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.createnoteform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
  
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
