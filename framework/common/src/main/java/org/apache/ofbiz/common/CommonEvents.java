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
package org.apache.ofbiz.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.VisualTheme;

/**
 * Common Services
 */
public class CommonEvents {

    public static final String module = CommonEvents.class.getName();

    private static final String[] ignoreAttrs = new String[] { // Attributes removed for security reason; _ERROR_MESSAGE_ and _ERROR_MESSAGE_LIST are kept
        "javax.servlet.request.key_size",
        "_CONTEXT_ROOT_",
        "_FORWARDED_FROM_SERVLET_",
        "javax.servlet.request.ssl_session",
        "javax.servlet.request.ssl_session_id",
        "multiPartMap",
        "javax.servlet.request.cipher_suite",
        "targetRequestUri",
        "_SERVER_ROOT_URL_",
        "_CONTROL_PATH_",
        "thisRequestUri",
        "org.apache.tomcat.util.net.secure_protocol_version",
        "userLogin",
        "impersonateLogin"
    };

    public static String setFollowerPage(HttpServletRequest request, HttpServletResponse response) {
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String visitId = request.getParameter("visitId");
        if (visitId != null) {
            request.setAttribute("visitId", visitId);
        }
        return "success";
    }

    /** Simple event to set the users per-session locale setting. The user's locale
     * setting should be passed as a "newLocale" request parameter. */
    public static String setSessionLocale(HttpServletRequest request, HttpServletResponse response) {
        String localeString = request.getParameter("newLocale");
        if (UtilValidate.isNotEmpty(localeString)) {
            UtilHttp.setLocale(request, localeString);

            // update the UserLogin object
            GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (GenericValue) request.getSession().getAttribute("autoUserLogin");
            }

            if (userLogin != null) {
                GenericValue ulUpdate = GenericValue.create(userLogin);
                ulUpdate.set("lastLocale", localeString);
                try {
                    ulUpdate.store();
                    userLogin.refreshFromCache();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
        return "success";
    }

    /** Simple event to set the user's per-session time zone setting. */
    public static String setSessionTimeZone(HttpServletRequest request, HttpServletResponse response) {
        String tzString = request.getParameter("tzId");
        if (UtilValidate.isNotEmpty(tzString)) {
            UtilHttp.setTimeZone(request, tzString);

            // update the UserLogin object
            GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (GenericValue) request.getSession().getAttribute("autoUserLogin");
            }

            if (userLogin != null) {
                GenericValue ulUpdate = GenericValue.create(userLogin);
                ulUpdate.set("lastTimeZone", tzString);
                try {
                    ulUpdate.store();
                    userLogin.refreshFromCache();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
        return "success";
    }

    /** Simple event to set the user's per-session theme setting. */
    public static String setSessionTheme(HttpServletRequest request, HttpServletResponse response) {
        String visualThemeId = request.getParameter("userPrefValue");
        if (UtilValidate.isNotEmpty(visualThemeId)) {
            VisualTheme visualTheme = ThemeFactory.getVisualThemeFromId(visualThemeId);
            if (visualTheme != null) {
                UtilHttp.setVisualTheme(request, visualTheme);
            }
        }
        return "success";
    }

    /** Simple event to set the users per-session currency uom value */
    public static String setSessionCurrencyUom(HttpServletRequest request, HttpServletResponse response) {
        String currencyUom = request.getParameter("currencyUom");
        if (UtilValidate.isNotEmpty(currencyUom)) {
            // update the session
            UtilHttp.setCurrencyUom(request.getSession(), currencyUom);

            // update the UserLogin object
            GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (GenericValue) request.getSession().getAttribute("autoUserLogin");
            }

            if (userLogin != null) {
                GenericValue ulUpdate = GenericValue.create(userLogin);
                ulUpdate.set("lastCurrencyUom", currencyUom);
                try {
                    ulUpdate.store();
                    userLogin.refreshFromCache();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
        return "success";
    }

    public static String jsonResponseFromRequestAttributes(HttpServletRequest request, HttpServletResponse response) {
        // pull out the service response from the request attribute

        Map<String, Object> attrMap = UtilHttp.getJSONAttributeMap(request);

        for (String ignoreAttr : ignoreAttrs) {
            if (attrMap.containsKey(ignoreAttr)) {
                attrMap.remove(ignoreAttr);
            }
        }
        try {
            JSON json = JSON.from(attrMap);
            writeJSONtoResponse(json, request, response);
        } catch (IOException e) {
            return "error";
        }
        return "success";
    }

    private static void writeJSONtoResponse(JSON json, HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        String jsonStr = json.toString();
        String httpMethod = request.getMethod();

        // This was added for security reason (OFBIZ-5409), you might need to remove the "//" prefix when handling the JSON response
        // Though normally you simply have to access the data you want, so should not be annoyed by the "//" prefix
        if ("GET".equalsIgnoreCase(httpMethod)) {
            Debug.logWarning("for security reason (OFBIZ-5409) the the '//' prefix was added handling the JSON response.  "
                    + "Normally you simply have to access the data you want, so should not be annoyed by the '//' prefix."
                    + "You might need to remove it if you use Ajax GET responses (not recommended)."
                    + "In case, the util.js scrpt is there to help you."
                    + "This can be customized in general.properties with the http.json.xssi.prefix property", module);
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            String xssiPrefix =EntityUtilProperties.getPropertyValue("general", "http.json.xssi.prefix", delegator);
            jsonStr = xssiPrefix + jsonStr;
        }

        // set the JSON content type
        response.setContentType("application/json");
        // jsonStr.length is not reliable for unicode characters
        response.setContentLength(jsonStr.getBytes("UTF8").length);

        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            Debug.logError(e, module);
        }
    }

    public static String getJSONuiLabelArray(HttpServletRequest request, HttpServletResponse response)
            throws UnsupportedEncodingException, IOException {
        // Format - {resource1 : [key1, key2 ...], resource2 : [key1, key2, ...], ...}
        String jsonString = request.getParameter("requiredLabels");
        Map<String, List<String>> uiLabelObject = null;
        if (UtilValidate.isNotEmpty(jsonString)) {
            JSON json = JSON.from(jsonString);
            uiLabelObject = UtilGenerics.<Map<String, List<String>>> cast(json.toObject(Map.class));
        }
        if (UtilValidate.isEmpty(uiLabelObject)) {
            Debug.logError("No resource and labels found in JSON string: " + jsonString, module);
            return "error";
        }
        Locale locale = UtilHttp.getLocale(request);
        Map<String, List<String>> uiLabelMap = new HashMap<>();
        Set<Map.Entry<String, List<String>>> entrySet = uiLabelObject.entrySet();
        for (Map.Entry<String, List<String>> entry : entrySet) {
            String resource = entry.getKey();
            List<String> resourceKeys = entry.getValue();
            if (resourceKeys != null) {
                List<String> labels = new ArrayList<>(resourceKeys.size());
                for (String resourceKey : resourceKeys) {
                    String label = UtilProperties.getMessage(resource, resourceKey, locale);
                    labels.add(label);
                }
                uiLabelMap.put(resource, labels);
            }
        }
        writeJSONtoResponse(JSON.from(uiLabelMap), request, response);
        return "success";
    }

    public static String getJSONuiLabel(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException, IOException {
        // Format - {resource : key}
        String jsonString = request.getParameter("requiredLabel");
        Map<String, String> uiLabelObject = null;
        if (UtilValidate.isNotEmpty(jsonString)) {
            JSON json = JSON.from(jsonString);
            uiLabelObject = UtilGenerics.<Map<String, String>>cast(json.toObject(Map.class));
        }
        if (UtilValidate.isEmpty(uiLabelObject)) {
            Debug.logError("No resource and labels found in JSON string: " + jsonString, module);
            return "error";
        } else if (uiLabelObject.size() > 1) {
            Debug.logError("More than one resource found, please use the method: getJSONuiLabelArray", module);
            return "error";
        }
        Locale locale = UtilHttp.getLocale(request);
        Map<String, String> uiLabelMap = new HashMap<>();
        Set<Map.Entry<String, String>> entrySet = uiLabelObject.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            String resource = entry.getKey();
            String resourceKey = entry.getValue();
            if (resourceKey != null) {
                String label = UtilProperties.getMessage(resource, resourceKey, locale);
                uiLabelMap.put(resource, label);
            }
        }
        writeJSONtoResponse(JSON.from(uiLabelMap), request, response);
        return "success";
    }

    public static String getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        try {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            final String captchaSizeConfigName = StringUtils.defaultIfEmpty(request.getParameter("captchaSize"), "default");
            final String captchaSizeConfig = EntityUtilProperties.getPropertyValue("captcha", "captcha." + captchaSizeConfigName, delegator);
            final String[] captchaSizeConfigs = captchaSizeConfig.split("\\|");
            final String captchaCodeId = StringUtils.defaultIfEmpty(request.getParameter("captchaCodeId"), ""); // this is used to uniquely identify in the user session the attribute where the captcha code for the last captcha for the form is stored

            final int fontSize = Integer.parseInt(captchaSizeConfigs[0]);
            final int height = Integer.parseInt(captchaSizeConfigs[1]);
            final int width = Integer.parseInt(captchaSizeConfigs[2]);
            final int charsToPrint = UtilProperties.getPropertyAsInteger("captcha", "captcha.code_length", 6);
            final char[] availableChars = EntityUtilProperties.getPropertyValue("captcha", "captcha.characters", delegator).toCharArray();

            //It is possible to pass the font size, image width and height with the request as well
            Color backgroundColor = Color.gray;
            Color borderColor = Color.DARK_GRAY;
            Color textColor = Color.ORANGE;
            Color circleColor = new Color(160, 160, 160);
            Font textFont = new Font("Arial", Font.PLAIN, fontSize);
            int circlesToDraw = 6;
            float horizMargin = 20.0f;
            double rotationRange = 0.7; // in radians
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = (Graphics2D) bufferedImage.getGraphics();

            g.setColor(backgroundColor);
            g.fillRect(0, 0, width, height);

            //Generating some circles for background noise
            g.setColor(circleColor);
            for (int i = 0; i < circlesToDraw; i++) {
                int circleRadius = (int) (Math.random() * height / 2.0);
                int circleX = (int) (Math.random() * width - circleRadius);
                int circleY = (int) (Math.random() * height - circleRadius);
                g.drawOval(circleX, circleY, circleRadius * 2, circleRadius * 2);
            }
            g.setColor(textColor);
            g.setFont(textFont);

            FontMetrics fontMetrics = g.getFontMetrics();
            int maxAdvance = fontMetrics.getMaxAdvance();
            int fontHeight = fontMetrics.getHeight();

            String captchaCode = RandomStringUtils.random(6, availableChars);

            float spaceForLetters = -horizMargin * 2 + width;
            float spacePerChar = spaceForLetters / (charsToPrint - 1.0f);

            for (int i = 0; i < captchaCode.length(); i++) {

                // this is a separate canvas used for the character so that
                // we can rotate it independently
                int charWidth = fontMetrics.charWidth(captchaCode.charAt(i));
                int charDim = Math.max(maxAdvance, fontHeight);
                int halfCharDim = (charDim / 2);

                BufferedImage charImage = new BufferedImage(charDim, charDim, BufferedImage.TYPE_INT_ARGB);
                Graphics2D charGraphics = charImage.createGraphics();
                charGraphics.translate(halfCharDim, halfCharDim);
                double angle = (Math.random() - 0.5) * rotationRange;
                charGraphics.transform(AffineTransform.getRotateInstance(angle));
                charGraphics.translate(-halfCharDim, -halfCharDim);
                charGraphics.setColor(textColor);
                charGraphics.setFont(textFont);

                int charX = (int) (0.5 * charDim - 0.5 * charWidth);
                charGraphics.drawString("" + captchaCode.charAt(i), charX,
                        ((charDim - fontMetrics.getAscent()) / 2 + fontMetrics.getAscent()));

                float x = horizMargin + spacePerChar * (i) - charDim / 2.0f;
                int y = ((height - charDim) / 2);

                g.drawImage(charImage, (int) x, y, charDim, charDim, null, null);

                charGraphics.dispose();
            }
            // Drawing the image border
            g.setColor(borderColor);
            g.drawRect(0, 0, width - 1, height - 1);
            g.dispose();
            response.setContentType("image/jpeg");
            ImageIO.write(bufferedImage, "jpg", response.getOutputStream());
            HttpSession session = request.getSession();
            Map<String, String> captchaCodeMap = UtilGenerics.checkMap(session.getAttribute("_CAPTCHA_CODE_"));
            if (captchaCodeMap == null) {
                captchaCodeMap = new HashMap<>();
                session.setAttribute("_CAPTCHA_CODE_", captchaCodeMap);
            }
            captchaCodeMap.put(captchaCodeId, captchaCode);
        } catch (IOException | IllegalArgumentException | IllegalStateException ioe) {
            Debug.logError(ioe.getMessage(), module);
        }
        return "success";
    }

}
