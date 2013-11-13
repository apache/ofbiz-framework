<div id="payPin" style="display:none">
  <table border="0" width="100%">
    <tr rowspan="2">
      <td colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td width="100%" align="center" colspan="2">
        <b>${uiLabelMap.WebPosTransactionTotalDue} <span id="pinTotalDue"/></b>
      </td>
    </tr>
    <tr>
      <td width="100%" align="center" colspan="2">
        <b>${uiLabelMap.WebPosPayPinTotal} <span id="pinTotalPaid"/></b>
        <a id="removePinTotalPaid" href="javascript:void(0);"><img src="/images/collapse.gif"></a>
      </td>
    </tr>
    <tr>
      <td width="50%" align="right">${uiLabelMap.WebPosPayPin}</td>
      <td width="50%" align="left"><input type="text" id="amountPin" name="amountPin" size="10" value=""/></td>
    </tr>
    <tr>
      <td width="50%" align="right">${uiLabelMap.WebPosPayPinRefNum}</td>
      <td width="50%" align="left"><input type="text" id="refNumPin" name="refNum" size="10" value=""/></td>
    </tr>
    <tr>
      <td colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="2" align="center">
        <input type="submit" value="${uiLabelMap.CommonConfirm}" id="payPinConfirm"/>
        <input type="submit" value="${uiLabelMap.CommonCancel}" id="payPinCancel"/>
      </td>
    </tr>
    <tr>
      <td colspan="2"><div class="errorPosMessage"><span id="payPinFormServerError"/></div></td>
    </tr>
  </table>
</div>