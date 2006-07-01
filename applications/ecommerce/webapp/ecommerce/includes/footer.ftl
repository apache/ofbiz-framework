<#--
 *  Copyright (c) 2001-2006 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
-->

<#assign nowTimestamp = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>

<br/>
<div align="center">
    <a href="http://jigsaw.w3.org/css-validator/"><img style="border:0;width:88px;height:31px" src="<@ofbizContentUrl>/images/vcss.gif</@ofbizContentUrl>" alt="Valid CSS!"/></a>
    <a href="http://validator.w3.org/check?uri=referer"><img style="border:0;width:88px;height:31px" src="<@ofbizContentUrl>/images/valid-xhtml10.png</@ofbizContentUrl>" alt="Valid XHTML 1.0!"/></a>
</div>
<br/>
<div class="tabletext" align="center">
    <div class="tabletext">Copyright (c) 2001-${nowTimestamp?string("yyyy")} The Open For Business Project - <a href="http://www.ofbiz.org" class="tabletext" target="_blank">www.ofbiz.org</a></div>
    <div class="tabletext">Powered By <a href="http://www.ofbiz.org" class="tabletext" target="_blank">OFBiz</a></div>
</div>
<br/>
<div class="tabletext" align="center"><a href="<@ofbizUrl>policies</@ofbizUrl>">See Store Policies Here</a></div>
</body>
</html>
