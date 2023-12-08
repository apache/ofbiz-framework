/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.widget.renderer.macro;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels;
import org.apache.ofbiz.widget.model.ModelForm;

public class MacroCommonRenderer {

    /**
     * Create an ajaxXxxx JavaScript CSV string from a list of UpdateArea objects. See
     * <code>OfbizUtil.js</code>.
     * @param updateAreas
     * @param extraParams Renderer-supplied additional target parameters Map
     * @param anchor
     * @param context
     * @return Parameter string or empty string if no UpdateArea objects were found
     */
    public static String createAjaxParamsFromUpdateAreas(List<ModelForm.UpdateArea> updateAreas, Map<String, Object> extraParams,
                                                         ModelForm parentModelForm, String anchor, Map<String, ? extends Object> context) {

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        Map<String, Object> ctx = UtilGenerics.cast(context);
        RequestHandler rh = RequestHandler.from(request);

        StringBuilder sb = new StringBuilder();
        Iterator<ModelForm.UpdateArea> updateAreaIter = updateAreas.iterator();
        while (updateAreaIter.hasNext()) {
            ModelForm.UpdateArea updateArea = updateAreaIter.next();

            //For each update area we need to resolve three information below
            String areaIdToUpdate;
            String targetToCall;
            String parametersToForward;

            // 1. areaId to update, use the screen stack with the potential area given by the update area element
            areaIdToUpdate = WidgetWorker.getScreenStack(ctx).resolveScreenAreaId(updateArea.getAreaId());

            // 2. the target, if the updateArea haven't the information and we are on event link to the pagination,
            //     will ask to the parent model the pagination target
            String ajaxTarget = updateArea.getAreaTarget(context);
            if (UtilValidate.isEmpty(ajaxTarget)
                    && parentModelForm != null) {
                if (UtilMisc.toList("multi", "list").contains(parentModelForm.getType())) {
                    ajaxTarget = parentModelForm.getPaginateTarget(ctx);
                } else {
                    ajaxTarget = parentModelForm.getTarget(ctx, parentModelForm.getTargetType());
                }
            }
            targetToCall = rh.makeLink(request, response, UtilHttp.removeQueryStringFromTarget(ajaxTarget));

            // 3. Build parameters to forward
            String queryString = UtilHttp.getQueryStringFromTarget(ajaxTarget).replace("?", "");
            Map<String, Object> parameters = UtilHttp.getQueryStringOnlyParameterMap(queryString);
            if (extraParams != null) {
                parameters.putAll(UtilGenerics.cast(extraParams));
            }
            parameters.putAll(UtilGenerics.cast(updateArea.getParameterMap(ctx)));
            UtilHttp.canonicalizeParameterMap(parameters);
            parametersToForward = UtilHttp.urlEncodeArgs(parameters, false);

            // 4. build the final string
            sb.append(areaIdToUpdate).append(",")
                    .append(targetToCall).append(",")
                    .append(parametersToForward);
            if (UtilValidate.isNotEmpty(anchor)) {
                sb.append("#").append(anchor);
            }
            if (updateAreaIter.hasNext()) {
                sb.append(",");
            }
        }
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        return FlexibleStringExpander.expandString(sb.toString(), context, locale);
    }

    private static String buildParamStringFromMap(Map<String, Object> extraParamsAsMap) {
        if (extraParamsAsMap == null) return "";
        final Map<String, Object> extraParamsAsMapConverted = extraParamsAsMap;
        return extraParamsAsMap.keySet().stream()
                .map(key -> key + "=" + extraParamsAsMapConverted.get(key))
                .collect(Collectors.joining("&"));
    }

    /**
     * Analyze the context against the link type to resolve the url to call
     * @param link generic link object
     * @param linkType link type is resolved from execution context and not directly on the link object
     * @param context
     * @return
     */
    public static String getLinkUrl(CommonWidgetModels.Link link, String linkType, Map<String, Object> context) {
        String linkUrl = "";

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        switch (linkType) {
        case "update-area":
            ModelForm.UpdateArea resolveUpdateArea = new ModelForm.UpdateArea("onclick",
                    WidgetWorker.getScreenStack(context).resolveScreenAreaId(link.getTargetWindow(context)),
                    link.getTarget(context));
            linkUrl = createAjaxParamsFromUpdateAreas(UtilMisc.toList(resolveUpdateArea),
                    UtilGenerics.cast(link.getParameterMap(context)), null, null, context);
            break;
        case "hidden-form":
            if (link.getCallback() != null) {
                // we assume that the request post
                // wait an immediate response, so we execute the callback directly
                linkUrl = createAjaxParamsFromUpdateAreas(UtilMisc.toList(link.getCallback()),
                        WidgetWorker.resolveParametersMapFromQueryString(context),
                        null, null, context);
            }
            break;
        default:
            String target = link.getTarget(context);
            if (UtilValidate.isNotEmpty(target)) {
                final URI linkUri = WidgetWorker.buildHyperlinkUri(target, link.getUrlMode(),
                        "layered-modal".equals(link.getLinkType()) ? null : link.getParameterMap(context), link.getPrefix(context),
                        link.getFullPath(), link.getSecure(), link.getEncode(),
                        request, response);
                linkUrl = linkUri.toString();
            }
        }
        return linkUrl;
    }
}
