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
package org.apache.ofbiz.webtools.print;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;

/**
 * FoPrintServerEvents
 */

public class FoPrintServerEvents {

    public static final String module = FoPrintServerEvents.class.getName();

    public static String getXslFo(HttpServletRequest req, HttpServletResponse resp) {
        LocalDispatcher dispatcher = (LocalDispatcher) req.getAttribute("dispatcher");
        Map<String, Object> reqParams = UtilHttp.getParameterMap(req);
        reqParams.put("locale", UtilHttp.getLocale(req));

        String screenUri = (String) reqParams.remove("screenUri");
        if (UtilValidate.isNotEmpty(screenUri)) {
            String base64String = null;
            try {
                byte[] bytes = FoPrintServerEvents.getXslFo(dispatcher.getDispatchContext(), screenUri, reqParams);
                base64String = new String(Base64.encodeBase64(bytes));
            } catch (GeneralException e) {
                Debug.logError(e, module);
                try {
                    resp.sendError(500);
                } catch (IOException e1) {
                    Debug.logError(e1, module);
                }
            }
            if (base64String != null) {
                try {
                    Writer out = resp.getWriter();
                    out.write(base64String);
                } catch (IOException e) {
                    try {
                        resp.sendError(500);
                    } catch (IOException e1) {
                        Debug.logError(e1, module);
                    }
                }
            }
        }

        return null;
    }

    public static byte[] getXslFo(DispatchContext dctx, String screen, Map<String, Object> parameters) throws GeneralException {
        // run as the system user
        VisualTheme visualTheme = (VisualTheme) parameters.get("visualTheme");
        if (visualTheme == null) {
            visualTheme = ThemeFactory.resolveVisualTheme(null);
        }        
        GenericValue system = null;
        try {
            system = dctx.getDelegator().findOne("UserLogin", false, "userLoginId", "system");
        } catch (GenericEntityException e) {
            throw new GeneralException(e.getMessage(), e);
        }
        parameters.put("userLogin", system);
        if (!parameters.containsKey("locale")) {
            parameters.put("locale", Locale.getDefault());
        }

        // render and obtain the XSL-FO
        Writer writer = new StringWriter();
        try {
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(visualTheme.getModelTheme().getType("screen"),
                    visualTheme.getModelTheme().getScreenRendererLocation("screen"));
            ScreenRenderer screens = new ScreenRenderer(writer, null, screenStringRenderer);
            screens.populateContextForService(dctx, parameters);
            screens.render(screen);
        } catch (Throwable t) {
            throw new GeneralException("Problems rendering FOP XSL-FO", t);
        }
        return writer.toString().getBytes();
    }
}
