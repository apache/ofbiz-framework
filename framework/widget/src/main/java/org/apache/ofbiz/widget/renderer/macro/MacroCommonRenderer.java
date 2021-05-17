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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels;
import org.apache.ofbiz.widget.model.ModelForm;

/**
 * TODO : Migrate to {@link org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl}
 */
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
                                                   String anchor, Map<String, ? extends Object> context) {

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        RequestHandler rh = RequestHandler.from(request);

        StringBuilder sb = new StringBuilder();
        Iterator<ModelForm.UpdateArea> updateAreaIter = updateAreas.iterator();
        while (updateAreaIter.hasNext()) {
            ModelForm.UpdateArea updateArea = updateAreaIter.next();
            sb.append(updateArea.getAreaId()).append(",");
            String ajaxTarget = updateArea.getAreaTarget(context);
            String urlPath = UtilHttp.removeQueryStringFromTarget(ajaxTarget);
            sb.append(rh.makeLink(request, response, urlPath)).append(",");
            String queryString = UtilHttp.getQueryStringFromTarget(ajaxTarget).replace("?", "");
            Map<String, Object> parameters = UtilHttp.getQueryStringOnlyParameterMap(queryString);
            Map<String, Object> ctx = UtilGenerics.cast(context);
            Map<String, Object> updateParams = UtilGenerics.cast(updateArea.getParameterMap(ctx));
            parameters.putAll(updateParams);
            UtilHttp.canonicalizeParameterMap(parameters);
            if (extraParams != null) {
                parameters.putAll(extraParams);
            }
            Iterator<Map.Entry<String, Object>> paramIter = parameters.entrySet().iterator();
            while (paramIter.hasNext()) {
                Map.Entry<String, Object> entry = paramIter.next();
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (paramIter.hasNext()) {
                    sb.append("&");
                }
            }
            if (anchor != null) {
                sb.append("#").append(anchor);
            }
            if (updateAreaIter.hasNext()) {
                sb.append(",");
            }
        }
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        return FlexibleStringExpander.expandString(sb.toString(), context, locale);
    }

    /** Create an ajaxXxxx JavaScript CSV string from a list of UpdateArea objects. See
     * <code>OfbizUtil.js</code>.
     * @param updateAreas
     * @param extraParams Renderer-supplied additional target parameters String
     * @param context
     * @return Parameter string or empty string if no UpdateArea objects were found
     */
    public static String createAjaxParamsFromUpdateAreas(List<ModelForm.UpdateArea> updateAreas, String extraParams,
                                                         Map<String, ? extends Object> context) {
        Map<String, Object> extraParamsAsMap = buildParamMapFromString(extraParams);
        return createAjaxParamsFromUpdateAreas(updateAreas, extraParamsAsMap, null, context);
    }

    private static Map<String, Object> buildParamMapFromString(String extraParams) {
        Map<String, Object> extraParamsAsMap = null;
        if (extraParams != null) {
            while (extraParams.startsWith("&")) {
                extraParams = extraParams.replaceFirst("&", "");
            }
            extraParamsAsMap = UtilGenerics.cast(StringUtil.strToMap(extraParams, "&", false));
        }
        return extraParamsAsMap;
    }

    /**
     * Analyze the context against the link type to resolve the url to call
     * @param context
     * @return
     */
    public static String getLinkUrl(CommonWidgetModels.Link link, Map<String, Object> context) {
        String linkUrl;

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        switch (link.getLinkType()) {
        case "update-area":
            ModelForm.UpdateArea resolveUpdateArea = new ModelForm.UpdateArea("onclick",
                    WidgetWorker.getScreenStack(context).resolveScreenAreaId(link.getTargetWindow(context)),
                    link.getTarget(context));
            linkUrl = createAjaxParamsFromUpdateAreas(UtilMisc.toList(resolveUpdateArea),
                    UtilHttp.urlEncodeArgs(link.getParameterMap(context)), context);
            break;
        default:
            final URI linkUri = WidgetWorker.buildHyperlinkUri(link.getTarget(context), link.getUrlMode(),
                    "layered-modal".equals(link.getLinkType()) ? null : link.getParameterMap(context), link.getPrefix(context),
                    link.getFullPath(), link.getSecure(), link.getEncode(),
                    request, response);
            linkUrl = linkUri.toString();
        }
        return linkUrl;
    }
}
