<#--
 *  Copyright (c) 2004-2006 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski
 *@version    $Rev$
 *@since      3.2
-->

<#-- Render the survey -->
<#if surveyString?has_content>
  <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>updateSurveyResponse</@ofbizUrl>" name="EditSurveyResponseForm" style="margin: 0;">
    <div class="tabletext">${uiLabelMap.PartyPartyId}: <input type="text" size="15" name="partyId" value="${userLogin.partyId}" class="inputBox"/><a href="javascript:call_fieldlookup2(document.EditSurveyResponseForm.partyId, 'LookupPartyName');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a></div>
    <#-- pass through the dataResourceId so the response can be associated with it -->
    <input type="hidden" name="dataResourceId" value="${parameters.dataResourceId?if_exists}"/>
    <input type="hidden" name="rootContentId" value="${parameters.rootContentId?if_exists}"/>
    ${surveyString}
  </form>
<#else>
  <div class="head1">Problem rendering the survey.</div>
</#if>
