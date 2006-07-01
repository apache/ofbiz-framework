<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
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
 *@version    $Rev$
 *@since      3.1
-->

<table width="100%" border="0" cellpadding="0" cellspacing="0">
  <tr>
    <#-- left side -->
    <td width="50%" valign="top" align="left">

      <#-- header box -->
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;PO For ${partyId?if_exists}</div>
            </div>
            <div class="screenlet-body">
                <table width="100%" border="0" cellpadding="1" cellspacing="0">
                    <tr><td><div class="tabletext">Supplier Information Here</div></td></tr>
                </table>
            </div>
        </div>
      <#-- end of header box -->

      <#-- payment box -->
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;Place Holder</div>
            </div>
            <div class="screenlet-body">
                  <table width="100%" border="0" cellpadding="1" cellspacing="0">

                  </table>
            </div>
        </div>
      <#-- end of payment box -->

    </td>
    <#-- end of left side -->

    <#-- left/right spacer -->
    <td width="1">&nbsp;&nbsp;</td>
    <#-- end of left/right spacer -->

    <#-- right side -->
    <td width="50%" valign="top" align="left">

      <#-- contact box -->
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;Place Holder</div>
            </div>
            <div class="screenlet-body">
                  <table width="100%" border="0" cellpadding="1" cellspacing="0">

                  </table>
            </div>
        </div>
      <#-- end of contact box -->

      <#-- shipping info box -->
        <div class="screenlet">
            <div class="screenlet-header">
                <div class="boxhead">&nbsp;Place Holder</div>
            </div>
            <div class="screenlet-body">
                  <table width="100%" border="0" cellpadding="1" cellspacing="0">

                  </table>
            </div>
        </div>
      <#-- end of shipping info box -->

    </td>
    <#-- end of right side -->
  </tr>
</table>
