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
 *@version    $Rev$
 *@since      3.0
-->

<#-- All fields from Person and UserLogin are available to this template -->

${person.firstName},
<br/><br/>
${uiLabelMap.EcommerceThankForRegistering} MyStore.com. <#-- MyStore.com (not a variable why?) must be adapted - JLR 1/6/5 -->
<br/><br/><br/>

${uiLabelMap.EcommerceAccountLogin} ${userLogin.userLoginId}<br/>
${uiLabelMap.EcommercePassword}<br/><br/>

${uiLabelMap.EcommerceLosePassword}<br/><br/>

${uiLabelMap.EcommerceThankYou},<br/>
MyStore.com <#-- MyStore.com (not a variable why?) must be adapted - JLR 1/6/5 -->
<br/><br/>

