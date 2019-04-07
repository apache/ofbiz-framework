/*
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
 */
package org.apache.ofbiz.common.qrcode;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.image.ImageTransform;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.qrcode.detector.Detector;

import freemarker.template.utility.StringUtil;

/**
 * Services for QRCode.
 */
public class QRCodeServices {

    public static final String module = QRCodeServices.class.getName();

    public static final int MIN_SIZE = 20;

    public static final int MAX_SIZE = 500;

    private static final int BLACK = 0xFF000000;

    private static final int WHITE = 0xFFFFFFFF;

    /** Streams QR Code to the result. */
    public static Map<String, Object> generateQRCodeImage(DispatchContext ctx,Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        String message = (String) context.get("message");
        Integer width = (Integer) context.get("width");
        Integer height = (Integer) context.get("height");
        String format = (String) context.get("format");
        String encoding = (String) context.get("encoding");
        Boolean verifyOutput = (Boolean) context.get("verifyOutput");
        String logoImage = (String) context.get("logoImage");
        Integer logoImageMaxWidth = (Integer) context.get("logoImageMaxWidth");
        Integer logoImageMaxHeight = (Integer) context.get("logoImageMaxHeight");
        Delegator delegator = ctx.getDelegator();

        if (UtilValidate.isEmpty(message)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "ParameterCannotEmpty", new Object[] { "message" }, locale));
        }
        if (width == null) {
            width = Integer.parseInt(EntityUtilProperties.getPropertyValue("qrcode", "qrcode.default.width", "200", delegator));
        }
        if (width < MIN_SIZE || width > MAX_SIZE) {
            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "SizeOutOfBorderError", new Object[] {"width", String.valueOf(width), String.valueOf(MIN_SIZE), String.valueOf(MAX_SIZE)}, locale));
        }
        if (height == null) {
            height = Integer.parseInt(EntityUtilProperties.getPropertyValue("qrcode", "qrcode.default.height", "200", delegator));
        }
        if (height < MIN_SIZE || height > MAX_SIZE) {
            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "SizeOutOfBorderError", 
                    new Object[] { "height", String.valueOf(height), String.valueOf(MIN_SIZE), String.valueOf(MAX_SIZE) }, locale));
        }
        if (UtilValidate.isEmpty(format)) {
            format = EntityUtilProperties.getPropertyValue("qrcode", "qrcode.default.format", "jpg", delegator);
        }
        String qrCodeFormatSupported = EntityUtilProperties.getPropertyValue("qrcode", "qrcode.format.supported", "jpg|png|bmp", delegator);
        String[] formatNames = StringUtil.split(qrCodeFormatSupported, '|');
        List<String> formatsSupported = Arrays.asList(formatNames);
        if (!formatsSupported.contains(format)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "ErrorFormatNotSupported", new Object[] { format }, locale));
        }
        Map<EncodeHintType, Object> encodeHints = null;
        if (UtilValidate.isNotEmpty(encoding)) {
            encodeHints = new EnumMap<>(EncodeHintType.class);
            encodeHints.put(EncodeHintType.CHARACTER_SET, encoding);
        }

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, width, height, encodeHints);
            BufferedImage bufferedImage = toBufferedImage(bitMatrix, format, locale);
            BufferedImage logoBufferedImage = null;
            if (UtilValidate.isNotEmpty(logoImage)) {
                Map<String, Object> logoImageResult;
                try {
                    logoImageResult = ImageTransform.getBufferedImage(FileUtil.getFile(logoImage).getAbsolutePath(), locale);
                    logoBufferedImage = (BufferedImage) logoImageResult.get("bufferedImage");
                } catch (IllegalArgumentException | IOException e) {
                    Debug.logError(e, module);
                }
            }
            if (UtilValidate.isEmpty(logoBufferedImage)) {
                String qrCodeDefaultLogoImage = EntityUtilProperties.getPropertyValue("qrcode", "qrcode.default.logoimage", delegator);
                BufferedImage defaultLogoImage = null;
                if (UtilValidate.isNotEmpty(qrCodeDefaultLogoImage)) {
                    try {
                        Map<String, Object> logoImageResult = ImageTransform.getBufferedImage(FileUtil.getFile(qrCodeDefaultLogoImage).getAbsolutePath(), locale);
                        defaultLogoImage = (BufferedImage) logoImageResult.get("bufferedImage");
                        if (UtilValidate.isEmpty(defaultLogoImage)) {
                            Debug.logError("Your logo image file(" + qrCodeDefaultLogoImage + ") cannot be read by javax.imageio.ImageIO. Please use png, jpeg formats instead of ico and etc.", module);
                        }
                    } catch (IllegalArgumentException | IOException e) {
                        defaultLogoImage = null;
                    }
                }
                logoBufferedImage = defaultLogoImage;
            }
            
            BufferedImage newBufferedImage = null;
            if (UtilValidate.isNotEmpty(logoBufferedImage)) {
                if (UtilValidate.isNotEmpty(logoImageMaxWidth) && UtilValidate.isNotEmpty(logoImageMaxHeight) && (logoBufferedImage.getWidth() > logoImageMaxWidth || logoBufferedImage.getHeight() > logoImageMaxHeight)) {
                    Map<String, String> typeMap = new HashMap<>();
                    typeMap.put("width", logoImageMaxWidth.toString());
                    typeMap.put("height", logoImageMaxHeight.toString());
                    Map<String, Map<String, String>> dimensionMap = new HashMap<>();
                    dimensionMap.put("QRCode", typeMap);
                    Map<String, Object> logoImageResult =
                            ImageTransform.scaleImage(logoBufferedImage, logoBufferedImage.getWidth(),
                                    logoBufferedImage.getHeight(), dimensionMap, "QRCode", locale);
                    logoBufferedImage = (BufferedImage) logoImageResult.get("bufferedImage");
                }
                BitMatrix newBitMatrix = bitMatrix.clone();
                newBufferedImage = toBufferedImage(newBitMatrix, format, locale);
                Graphics2D graphics = newBufferedImage.createGraphics();
                graphics.drawImage(logoBufferedImage, new AffineTransformOp(AffineTransform.getTranslateInstance(1, 1), null), (newBufferedImage.getWidth() - logoBufferedImage.getWidth())/2, (newBufferedImage.getHeight() - logoBufferedImage.getHeight())/2);
                graphics.dispose();
            }
            
            if (UtilValidate.isNotEmpty(verifyOutput) && verifyOutput) {
                Decoder decoder = new Decoder();
                Map<DecodeHintType, Object> decodeHints = new EnumMap<>(DecodeHintType.class);
                decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                if (UtilValidate.isNotEmpty(encoding)) {
                    decodeHints.put(DecodeHintType.CHARACTER_SET, encoding);
                }
                DetectorResult detectorResult = null;
                if (UtilValidate.isNotEmpty(newBufferedImage)) {
                    BitMatrix newBitMatrix = createMatrixFromImage(newBufferedImage);
                    DecoderResult result = null;
                    try {
                        detectorResult = new Detector(newBitMatrix).detect(decodeHints);
                        result = decoder.decode(detectorResult.getBits(), decodeHints);
                    } catch (ChecksumException | FormatException | NotFoundException e) {
                        Debug.logError(e, module);
                    }
                    if (UtilValidate.isNotEmpty(result) && !result.getText().equals(message)) {
                        detectorResult = new Detector(bitMatrix).detect(decodeHints);
                        result = decoder.decode(detectorResult.getBits(), decodeHints);
                        if (!result.getText().equals(message)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "GeneratedTextNotMatchOriginal", new Object[]{result.getText(), message}, locale));
                        }
                    } else {
                        bufferedImage = newBufferedImage;
                    }
                } else {
                    detectorResult = new Detector(bitMatrix).detect(decodeHints);
                    DecoderResult result = decoder.decode(detectorResult.getBits(), decodeHints);
                    if (!result.getText().equals(message)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "GeneratedTextNotMatchOriginal", new Object[]{result.getText(), message}, locale));
                    }
                }
            } else if (UtilValidate.isNotEmpty(newBufferedImage)) {
                bufferedImage = newBufferedImage;
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("bufferedImage", bufferedImage);
            return result;
        } catch (WriterException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "ErrorGenerateQRCode", new Object[] { e.toString() }, locale));
        } catch (ChecksumException | FormatException | NotFoundException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage("QRCodeUiLabels", "ErrorVerifyQRCode", new Object[] { e.toString() }, locale));
        }
    }

    /**
     * Renders a {@link BitMatrix} as an image, where "false" bits are rendered
     * as white, and "true" bits are rendered as black.
     * 
     * This is to replace MatrixToImageWriter.toBufferedImage(bitMatrix) if you
     * find the output image is not right, you can change BufferedImage image =
     * new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); to
     * BufferedImage image = new BufferedImage(width, height,
     * BufferedImage.TYPE_INT_RGB); or others to make it work correctly.
     */
    private static BufferedImage toBufferedImage(BitMatrix matrix, String format, Locale locale) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = null;
        String osName = System.getProperty("os.name").toLowerCase(locale);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        if (osName.startsWith("mac os") && format.equals("png")) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }
        return image;
    }

    private static BitMatrix createMatrixFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        BitMatrix matrix = new BitMatrix(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int luminance = (306 * ((pixel >> 16) & 0xFF) +
                    601 * ((pixel >> 8) & 0xFF) +
                    117 * (pixel & 0xFF)) >> 10;
                if (luminance <= 0x7F) {
                    matrix.set(x, y);
                }
            }
        }
        return matrix;
    }
}
