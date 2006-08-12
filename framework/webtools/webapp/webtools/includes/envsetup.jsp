<%--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
--%>
<%@ page import="java.util.*, java.net.*" %>
<%@ page import="org.ofbiz.security.*, org.ofbiz.entity.*, org.ofbiz.base.util.*, org.ofbiz.webapp.pseudotag.*, org.ofbiz.webapp.control.*, org.ofbiz.base.util.collections.*" %>
<%@ page import="org.ofbiz.securityext.login.*, org.ofbiz.common.*" %>

<%@ taglib uri="ofbizTags" prefix="ofbiz" %>

<jsp:useBean id="security" type="org.ofbiz.security.Security" scope="request" />
<jsp:useBean id="delegator" type="org.ofbiz.entity.GenericDelegator" scope="request" />
<%
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
    if (userLogin != null) request.setAttribute("userLogin", userLogin);

    GenericValue person = (GenericValue) session.getAttribute("_PERSON_");
    if (person == null && userLogin != null) {
        ModelEntity modelUserLogin = userLogin.getModelEntity();
        if (modelUserLogin.isField("partyId")) {
            person = userLogin.getRelatedOne("Person");
            if (person != null) session.setAttribute("_PERSON_", person);
        }
    }
    if (person != null) request.setAttribute("person", person);

    String controlPath = (String) request.getAttribute("_CONTROL_PATH_");
    String contextRoot = (String) request.getAttribute("_CONTEXT_ROOT_");
    String serverRoot = (String) request.getAttribute("_SERVER_ROOT_URL_");

    /* reading of the localization information */
    List availableLocales = UtilMisc.availableLocales();
    request.setAttribute("availableLocales",availableLocales);

    Locale locale = UtilHttp.getLocale(request);
    request.setAttribute("locale",locale);
    ResourceBundleMapWrapper uiLabelMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("WebtoolsUiLabels", locale);
    uiLabelMap.addBottomResourceBundle("CommonUiLabels");
    request.setAttribute("uiLabelMap", uiLabelMap);

    Map layoutSettings = new HashMap();
    request.setAttribute("layoutSettings", layoutSettings);
    
    layoutSettings.put("companyName", uiLabelMap.get("WebtoolsCompanyName"));
    layoutSettings.put("companySubtitle", uiLabelMap.get("WebtoolsCompanySubtitle"));
    layoutSettings.put("headerImageUrl", "/images/ofbiz_logo.jpg");
    layoutSettings.put("headerMiddleBackgroundUrl", null);
    layoutSettings.put("headerRightBackgroundUrl", null);
    
    request.setAttribute("checkLoginUrl", LoginWorker.makeLoginUrl(request, "checkLogin"));
    
    String externalLoginKey = LoginWorker.getExternalLoginKey(request);
    String externalKeyParam = externalLoginKey == null ? "" : "&externalLoginKey=" + externalLoginKey;
    request.setAttribute("externalKeyParam", externalKeyParam);
    request.setAttribute("externalLoginKey", externalLoginKey);
	request.setAttribute("activeApp", "webtools");

    List eventMessageList = (List) request.getAttribute("eventMessageList");
    if (eventMessageList == null) eventMessageList = new LinkedList();
    List errorMessageList = (List) request.getAttribute("errorMessageList");
    if (errorMessageList == null) errorMessageList = new LinkedList();

    if (request.getAttribute("_EVENT_MESSAGE_") != null) {
        eventMessageList.add(UtilFormatOut.replaceString((String) request.getAttribute("_EVENT_MESSAGE_"), "\n", "<br/>"));
        request.removeAttribute("_EVENT_MESSAGE_");
    }
    if (request.getAttribute("_EVENT_MESSAGE_LIST_") != null) {
        eventMessageList.addAll((List) request.getAttribute("_EVENT_MESSAGE_LIST_"));
        request.removeAttribute("_EVENT_MESSAGE_LIST_");
    }
    if (request.getAttribute("_ERROR_MESSAGE_") != null) {
        errorMessageList.add(UtilFormatOut.replaceString((String) request.getAttribute("_ERROR_MESSAGE_"), "\n", "<br/>"));
        request.removeAttribute("_ERROR_MESSAGE_");
    }
    if (session.getAttribute("_ERROR_MESSAGE_") != null) {
        errorMessageList.add(UtilFormatOut.replaceString((String) session.getAttribute("_ERROR_MESSAGE_"), "\n", "<br/>"));
        session.removeAttribute("_ERROR_MESSAGE_");
    }
    if (request.getAttribute("_ERROR_MESSAGE_LIST_") != null) {
        errorMessageList.addAll((List) request.getAttribute("_ERROR_MESSAGE_LIST_"));
        request.removeAttribute("_ERROR_MESSAGE_LIST_");
    }
    request.setAttribute("eventMessageList", eventMessageList);
    request.setAttribute("errorMessageList", errorMessageList);
%>
