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

// copied from : http://cocoon.apache.org/2.2/blocks/captcha/1.0/1436_1_1.html

package org.ofbiz.common;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilHttp;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;

public class Captcha {

    public static String ID_KEY = null;
    public static String CAPTCHA_FILE_NAME = null;
    public static String CAPTCHA_FILE_PATH = null;

    public static String getCodeCaptcha(HttpServletRequest request,HttpServletResponse response) {
        if (CAPTCHA_FILE_PATH != null) deleteFile();
        StringBuilder finalString = new StringBuilder();
        String elegibleChars = "ABCDEFGHJKLMPQRSTUVWXYabcdefhjkmnpqrstuvwxy23456789";
        int charsToPrint = 6;
        char[] chars = elegibleChars.toCharArray();

        for (int i = 0; i < charsToPrint; i++) {
            double randomValue = Math.random();
            int randomIndex = (int) Math.round(randomValue * (chars.length - 1));
            char characterToShow = chars[randomIndex];
            finalString.append(characterToShow);
        }
        ID_KEY = finalString.toString();
        if (createImageCaptcha (request,response)) return "success";
        return "error";
    }

    public static boolean createImageCaptcha (HttpServletRequest request,HttpServletResponse response) {
        try {
            //It is possible to pass the font size, image width and height with the request as well
            Color backgroundColor = Color.gray;
            Color borderColor = Color.DARK_GRAY;
            Color textColor = Color.ORANGE;
            Color circleColor = new Color(160, 160, 160);
            Font textFont = new Font("Arial", Font.PLAIN, paramInt(request, "fontSize", 22));
            int charsToPrint = 6;
            int width = paramInt(request, "width", 149);
            int height = paramInt(request, "height", 40);
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

            //We are not using certain characters, which might confuse users
            String characterToShow = ID_KEY;
            float spaceForLetters = -horizMargin * 2 + width;
            float spacePerChar = spaceForLetters / (charsToPrint - 1.0f);

            for (int i = 0; i < characterToShow.length(); i++) {

                // this is a separate canvas used for the character so that
                // we can rotate it independently
                int charWidth = fontMetrics.charWidth(characterToShow.charAt(i));
                int charDim = Math.max(maxAdvance, fontHeight);
                int halfCharDim = (int) (charDim / 2);

                BufferedImage charImage =
                        new BufferedImage(charDim, charDim, BufferedImage.TYPE_INT_ARGB);
                Graphics2D charGraphics = charImage.createGraphics();
                charGraphics.translate(halfCharDim, halfCharDim);
                double angle = (Math.random() - 0.5) * rotationRange;
                charGraphics.transform(AffineTransform.getRotateInstance(angle));
                charGraphics.translate(-halfCharDim, -halfCharDim);
                charGraphics.setColor(textColor);
                charGraphics.setFont(textFont);

                int charX = (int) (0.5 * charDim - 0.5 * charWidth);
                charGraphics.drawString("" + characterToShow.charAt(i), charX,
                        (int) ((charDim - fontMetrics.getAscent()) / 2 + fontMetrics.getAscent()));

                float x = horizMargin + spacePerChar * (i) - charDim / 2.0f;
                int y = (int) ((height - charDim) / 2);

                g.drawImage(charImage, (int) x, y, charDim, charDim, null, null);

                charGraphics.dispose();
            }
            // Drawing the image border
            g.setColor(borderColor);
            g.drawRect(0, 0, width - 1, height - 1);
            g.dispose();
            Captcha.writeImage(bufferedImage, request);

        } catch (Exception ioe) {
            return false;
        }
        //Adding this because we called response.getOutputStream() above. This will prevent and illegal state exception being thrown
        return true;
    }

    public static void writeImage(BufferedImage image, HttpServletRequest request)
    {
        try {
            String FILE_PATH = File.separator + "runtime" + File.separator + "tempfiles" + File.separator + "captcha" + File.separator;
            String URL_FILE_PATH = "/tempfiles/captcha/";
            CAPTCHA_FILE_PATH = new File(".").getCanonicalPath();
            CAPTCHA_FILE_PATH += FILE_PATH;
            File test = new File(CAPTCHA_FILE_PATH);
            if (!test.exists()) {
                test.mkdir();
            }
            CAPTCHA_FILE_NAME = UtilDateTime.nowAsString().concat(".jpg");
            request.setAttribute("captchaFileName", URL_FILE_PATH + CAPTCHA_FILE_NAME);
            request.setAttribute("ID_KEY", ID_KEY);
            ImageIO.write(image, "jpg", new File(CAPTCHA_FILE_PATH + CAPTCHA_FILE_NAME));
        } catch (IOException e) {
            return;
        }
    }

    public static void deleteFile() {
        if (CAPTCHA_FILE_PATH != null) {
               File file = new File(CAPTCHA_FILE_PATH);
               file.delete();
        }
    }

    public static String paramString(HttpServletRequest request, String paramName,
            String defaultString) {
        return request.getParameter(paramName) != null ? request.getParameter(paramName) : defaultString;
    }

    public static int paramInt(HttpServletRequest request, String paramName, int defaultInt) {
        return request.getParameter(paramName) != null ? Integer.parseInt(request.getParameter(paramName)) : defaultInt;
    }
}
