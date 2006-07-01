<#--
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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
 * @author     Andy Zeneski
 * @created    July 12, 2002
 * @version    1.0
 */
-->

<br/>
<TABLE border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <TR>
    <TD width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">${uiLabelMap.PageTitleAddressMatchMap}</div>
          </td>
          <td valign="middle" align="right">
            <a href="<@ofbizUrl>findAddressMatch</@ofbizUrl>" class="submenutextright">${uiLabelMap.PageTitleFindMatches}</a>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width="100%" >
      <center>
      <table border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <form name="addaddrmap" method="post" action="<@ofbizUrl>createAddressMatchMap</@ofbizUrl>">
          <tr>
            <td><span class="tabletext">&nbsp;${uiLabelMap.PartyAddressMatchKey}:&nbsp;</span></td>
            <td><input type="text" class="inputBox" name="mapKey"></td>
            <td><span class="tabletext">&nbsp;${uiLabelMap.PartyAddressMatchValue}:&nbsp;</span></td>
            <td><input type="text" class="inputBox" name="mapValue"></td>
            <td><span class="tabletext">&nbsp;${uiLabelMap.CommonSequence}:&nbsp;</span></td>
            <td><input type="text" class="inputBox" size="5" name="sequenceNum" value="0"></td>
            <td>
              <a href="javascript:document.addaddrmap.submit()" class="buttontext">${uiLabelMap.CommonCreate}</a>&nbsp;&nbsp;
            </td>
          </tr>
        </form>
        <tr><td colspan="5">&nbsp;</td></tr>
        <form name="importaddrmap"method="post" enctype="multipart/form-data" action="<@ofbizUrl>importAddressMatchMapCsv</@ofbizUrl>" style="margin: 0;">
          <tr>
            <td colspan="5" align="center">
              <input type="file" name="uploadedFile" size="14" class="inputBox"/>
              <input type="submit" value="${uiLabelMap.CommonUpload} CSV" class="smallSubmit"/>
              <div class="tabletext">${uiLabelMap.PartyAddressMatchMessage1}</span>
            </td>
          </tr>
        </form>
        <tr><td colspan="5">&nbsp;</td></tr>
        <#if addressMatchMaps?has_content>
          <tr>
            <td colspan="5">
              <table border="0" cellspacing="5" cellpadding="5">
                  <tr>
                    <td><div class="tableheadtext">${uiLabelMap.PartyAddressMatchKey}</td>
                    <td><div class="tableheadtext">=></td>
                    <td><div class="tableheadtext">${uiLabelMap.PartyAddressMatchValue}</td>
                    <td>&nbsp;</td>
                    <td><a href="<@ofbizUrl>clearAddressMatchMap</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClear} ${uiLabelMap.CommonAll}</a></td>
                  </tr>
                  <tr><td colspan="5"><hr class="sepbar"/></td></tr>
                <#list addressMatchMaps as map>
                  <tr>
                    <td><div class="tabletext">${map.mapKey}</td>
                    <td><div class="tabletext">=></td>
                    <td><div class="tabletext">${map.mapValue}</td>
                    <td><div class="tabletext">[${map.sequenceNum?if_exists}]</td>
                    <td><a href="<@ofbizUrl>removeAddressMatchMap?mapKey=${map.mapKey}&mapValue=${map.mapValue}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></td>
                  </tr>
                </#list>
              </table>
            </td>
          </tr>
        </#if>
      </table>
      </center>
    </TD>
  </TR>

</TABLE>

