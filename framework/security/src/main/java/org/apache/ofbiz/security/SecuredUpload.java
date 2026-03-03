
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
package org.apache.ofbiz.security;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageParser;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.gif.GifImageParser;
import org.apache.commons.imaging.formats.png.PngImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.ContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.lowagie.text.pdf.PdfReader;

public class SecuredUpload {

    // To check if a webshell is not uploaded or a reverse shell put in the query string

    // This can be helpful:
    // https://en.wikipedia.org/wiki/File_format
    // https://en.wikipedia.org/wiki/List_of_file_signatures
    // See also information in security.properties:
    // Supported file formats are *safe* PNG, GIF, TIFF, JPEG, PDF, Audio, Video, Text, and ZIP

    private static final String MODULE = SecuredUpload.class.getName();
    private static final List<String> DENIEDFILEEXTENSIONS = getDeniedFileExtensions();
    private static final List<String> DENIEDWEBSHELLTOKENS = getDeniedWebShellTokens();
    private static final Integer MAXLINELENGTH = UtilProperties.getPropertyAsInteger("security", "maxLineLength", 0);
    private static final Boolean ALLOWSTRINGCONCATENATIONINUPLOADEDFILES =
            UtilProperties.getPropertyAsBoolean("security", "allowStringConcatenationInUploadedFiles", false);

    /**
     * Per-type extension allow-lists loaded from {@code security.properties}.
     * Keys match the {@code fileType} argument of {@link #isValidFile}.
     * "AllButCompressed" and "All" are derived automatically as unions of the others.
     */
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_TYPE = buildAllowedExtensionsMap();

    /**
     * SVG element names (lower-cased) whose presence in an SVG file is unconditionally rejected.
     * These elements either execute script, embed foreign content, or enable timing-based attacks.
     */
    private static final Set<String> DENIED_SVG_ELEMENTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "script", "foreignobject", "animate", "animatecolor",
            "animatemotion", "animatetransform", "set", "handler", "listener")));

    /**
     * Allow-list pattern for entity primary-key values (e.g. {@code contentId}).
     * Accepts ASCII letters, digits, underscores, hyphens and colons — the character
     * set used by all OFBiz auto-generated and user-supplied IDs.
     */
    private static final Pattern ENTITY_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9_:\\-]{1,255}");

    /**
     * Allow-list pattern for webapp-relative path parameters (e.g. {@code webappPath}).
     * Accepts ASCII letters, digits and the path punctuation {@code / . - _}.
     */
    private static final Pattern WEBAPP_PATH_PATTERN = Pattern.compile("[a-zA-Z0-9/_\\.\\-]{1,4096}");

    // "(" and ")" for duplicates files
    private static final String FILENAMEVALIDCHARACTERS_DUPLICATES =
            UtilProperties.getPropertyValue("security", "fileNameValidCharactersDuplicates", "[a-zA-Z0-9-_ ()]");
    private static final String FILENAMEVALIDCHARACTERS =
            UtilProperties.getPropertyValue("security", "fileNameValidCharacters", "[a-zA-Z0-9-_ ]");

    // -----------------------------------------------------------------------
    // Allow-list validators (preferred over the deny-list in isValidText)
    // -----------------------------------------------------------------------

    /**
     * Allow-list validator for entity primary-key values such as {@code contentId}.
     * <p>Only ASCII letters, digits, underscores, hyphens and colons are accepted —
     * the character set used by OFBiz auto-generated and user-supplied IDs.
     * <p>This is an <em>allow-list</em> check and does not rely on a deny-list, so it
     * cannot be bypassed by encoding tricks or token splitting.
     *
     * @param key the string to validate (must not be {@code null} or empty)
     * @return {@code true} when {@code key} conforms to the expected format
     */
    public static boolean isValidEntityKey(String key) {
        if (UtilValidate.isEmpty(key)) {
            Debug.logError("Entity key is null or empty", MODULE);
            return false;
        }
        if (!ENTITY_KEY_PATTERN.matcher(key).matches()) {
            Debug.logError("Entity key contains disallowed characters: " + key, MODULE);
            return false;
        }
        return true;
    }

    /**
     * Allow-list validator for webapp-relative path parameters such as {@code webappPath}.
     * <p>Accepts only ASCII letters, digits and the path punctuation {@code / . - _}.
     * Path-traversal sequences ({@code ..}) and null bytes are always rejected regardless
     * of encoding.
     * <p>This is an <em>allow-list</em> check and does not rely on a deny-list.
     *
     * @param path the string to validate
     * @return {@code true} when {@code path} is safe to use as a webapp path
     */
    public static boolean isValidWebAppPath(String path) {
        if (UtilValidate.isEmpty(path)) {
            Debug.logError("Webapp path is null or empty", MODULE);
            return false;
        }
        if (path.indexOf('\0') >= 0) {
            Debug.logError("Webapp path contains a null byte", MODULE);
            return false;
        }
        if (path.contains("..")) {
            Debug.logError("Path traversal sequence '..' is not allowed in webapp path: " + path, MODULE);
            return false;
        }
        if (!WEBAPP_PATH_PATTERN.matcher(path).matches()) {
            Debug.logError("Webapp path contains disallowed characters: " + path, MODULE);
            return false;
        }
        return true;
    }

    /**
     * Allow-list validator for text content (uploaded files, string parameters).
     * <p>Accepts all printable Unicode code points and the four universally legitimate
     * whitespace characters (U+0009 HT, U+000A LF, U+000D CR, U+0020 SPACE).
     * All other C0 control characters (U+0000–U+001F) and C1 control characters
     * (U+007F–U+009F) are rejected.
     * <p>This approach is significantly more robust than the token deny-list in
     * {@link #isValidText} because it operates on decoded Unicode code points.
     * It cannot be bypassed by token splitting, alternate encodings, or obfuscation
     * with whitespace.  Its intent is to guarantee that stored text can safely
     * round-trip through any character-level processing without unexpected
     * control-character effects, <em>not</em> to detect every conceivable injection
     * payload — that responsibility belongs to the surrounding context (output
     * encoding, CSP, FreeMarker SAFER_RESOLVER, etc.).
     *
     * @param content the text content to validate; {@code null} is rejected
     * @return {@code true} when all code points are within the allowed set
     */
    public static boolean isValidTextContent(String content) {
        if (content == null) {
            return false;
        }
        for (int i = 0; i < content.length();) {
            int cp = content.codePointAt(i);
            i += Character.charCount(cp);
            // Allow standard whitespace: HT, LF, CR
            if (cp == 0x09 || cp == 0x0A || cp == 0x0D) {
                continue;
            }
            // Reject all other C0 control characters, including the null byte (U+0000)
            if (cp < 0x20) {
                Debug.logInfo("Text content rejected: contains C0 control character U+"
                        + String.format("%04X", cp), MODULE);
                return false;
            }
            // Reject DEL (U+007F) and C1 control characters (U+0080–U+009F)
            if (cp >= 0x7F && cp <= 0x9F) {
                Debug.logInfo("Text content rejected: contains C1 control character U+"
                        + String.format("%04X", cp), MODULE);
                return false;
            }
            // Lone surrogate code points should not appear in valid Java strings, but
            // guard against them explicitly to prevent bypass via malformed UTF-16.
            if (cp >= 0xD800 && cp <= 0xDFFF) {
                Debug.logInfo("Text content rejected: contains surrogate code point", MODULE);
                return false;
            }
            // All other printable Unicode code points are accepted.
        }
        return true;
    }

    // Cover method of the same name below. Historically used with 84 references when below was created
    // check there is no web shell in the uploaded file
    // A file containing a reverse shell will be rejected.
    public static boolean isValidText(String content, List<String> allowed) throws IOException {
        return isValidText(content, allowed, false);
    }

    public static boolean isValidText(String content, List<String> allowed, boolean isQuery) throws IOException {
        if (content == null) {
            return false;
        }
        if (!isQuery) {
            String contentWithoutSpaces = content.replaceAll(" ", "");
            if ((contentWithoutSpaces.contains("\"+\"") || contentWithoutSpaces.contains("'+'"))
                    && !ALLOWSTRINGCONCATENATIONINUPLOADEDFILES) {
                Debug.logInfo("The uploaded file contains a string concatenation. It can't be uploaded for security reason", MODULE);
                return false;
            }
        } else {
            // Check the query string is safe, notably no reverse shell
            List<String> queryParameters = StringUtil.split(content, "&");
            return DENIEDWEBSHELLTOKENS.stream().allMatch(token -> isValid(queryParameters, token.toLowerCase(), allowed));
        }

        // Check there is no web shell in an uploaded file
        return DENIEDWEBSHELLTOKENS.stream().allMatch(token -> isValid(content.toLowerCase(), token.toLowerCase(), allowed));
    }

    public static boolean isValidFileName(String fileToCheck, Delegator delegator) throws IOException {
        // Prevents double extensions
        if (StringUtils.countMatches(fileToCheck, ".") > 1) {
            Debug.logError("Double extensions are not allowed for security reason", MODULE);
            return false;
        }

        String imageServerUrl = EntityUtilProperties.getPropertyValue("catalog", "image.management.url", delegator);
        Path p = Paths.get(fileToCheck);
        boolean wrongFile = true;

        // Check extensions
        if (p != null && p.getFileName() != null) {
            String fileName = p.getFileName().toString(); // The file name is the farthest element from the root in the directory hierarchy.
            String extension = FilenameUtils.getExtension(fileToCheck).toLowerCase();
            // Prevents null byte in filename
            if (extension.contains("%00")
                    || extension.contains("%0a")
                    || extension.contains("%20")
                    || extension.contains("%0d%0a")
                    || extension.contains("/")
                    || extension.contains("./")
                    || extension.contains(".")) {
                Debug.logError("Special bytes in extension are not allowed for security reason", MODULE);
                return false;
            }
            if (DENIEDFILEEXTENSIONS.contains(extension)) {
                Debug.logError("This file extension is not allowed for security reason", MODULE);
                deleteBadFile(fileToCheck);
                return false;
            }

            // Check the file and path names
            if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS) {
                // More about that: https://docs.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation
                if (fileToCheck.length() > 259) {
                    Debug.logError("Uploaded file name too long", MODULE);
                } else if (p.toString().contains(imageServerUrl.replace("/", "\\"))) {
                    // TODO check this is still useful in at least 1 case
                    if (fileName.matches(
                            FILENAMEVALIDCHARACTERS_DUPLICATES
                            .concat("{1,249}.")
                            .concat(FILENAMEVALIDCHARACTERS)
                            .concat("{1,10}"))) {
                        wrongFile = false;
                    }
                } else if (fileName.matches(
                        FILENAMEVALIDCHARACTERS
                        .concat("{1,249}.")
                        .concat(FILENAMEVALIDCHARACTERS)
                        .concat("{1,10}"))) {
                    wrongFile = false;
                }
            } else { // Suppose a *nix system
                if (fileToCheck.length() > 4096) {
                    Debug.logError("Uploaded file name too long", MODULE);
                } else if (p.toString().contains(imageServerUrl)) {
                    // TODO check this is still useful in at least 1 case
                    if (fileName.matches(
                            FILENAMEVALIDCHARACTERS_DUPLICATES
                            .concat("{1,4086}.")
                            .concat(FILENAMEVALIDCHARACTERS)
                            .concat("{1,10}"))) {
                        wrongFile = false;
                    }
                } else if (fileName.matches(
                        FILENAMEVALIDCHARACTERS
                        .concat("{1,4086}.")
                        .concat(FILENAMEVALIDCHARACTERS)
                        .concat("{1,10}"))) {
                    wrongFile = false;
                }
            }
        }

        if (wrongFile) {
            Debug.logError("Uploaded file "
                    + " should contain only Alpha-Numeric characters, hyphen, underscore and spaces,"
                    + " only 1 dot as an input for the file name and the extension."
                    + "The file name and extension should not be empty at all",
                    MODULE);
            deleteBadFile(fileToCheck);
            return false;
        }
        return true;
    }

    /**
     * @param fileToCheck
     * @param delegator
     * @return true if the file is valid
     * @throws IOException
     * @throws ImageReadException
     */
    public static boolean isValidAllFile(String fileToCheck, Delegator delegator) throws IOException, ImageReadException {
        return isValidFile(fileToCheck, "All", delegator);
    }

    /**
     * @param fileToCheck
     * @param fileType
     * @return true if the file is valid
     * @throws IOException
     * @throws ImageReadException
     */
    public static boolean isValidFile(String fileToCheck, String fileType, Delegator delegator) throws IOException, ImageReadException {
        // Allow all uploads w/o check
        if (("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "allowAllUploads", delegator)))) {
            return true;
        }

        // Primary gate: file extension must be in the allow-list for the declared file type.
        // This is the strongest control — only explicitly whitelisted extensions pass.
        // The deny-list in isValidFileName acts as a secondary defence.
        if (!isAllowedExtension(fileToCheck, fileType)) {
            deleteBadFile(fileToCheck);
            return false;
        }

        // Check the file name
        if (!isValidFileName(fileToCheck, delegator)) { // Useless when the file is internally generated, but not sure for all cases
            return false;
        }

        // Check the file content

        /* Check max line length, default 10000.
         PDF files are not concerned because they may contain several CharSet encodings
         hence no possibility to use Files::readAllLines that needs a sole CharSet
         MsOffice files are not accepted. This is why:
         https://www.cvedetails.com/vulnerability-list/vendor_id-26/product_id-529/Microsoft-Word.html
         https://www.cvedetails.com/version-list/26/410/1/Microsoft-Excel.html
         You name it...
         Also, the file may have been created using another charset than the one used to read it (default to OS' one).
         I remember having searched bout that. But even
         http://illegalargumentexception.blogspot.com/2009/05/java-rough-guide-to-character-encoding.html#javaencoding_autodetect
         is not a 100% solution.
         So even for text files it can be a problem and according to above there is no complete solution.
        */
        if (!isPdfFile(fileToCheck)) {
            if (getMimeTypeFromFileName(fileToCheck).equals("application/x-tika-msoffice")) {
                Debug.logError("File : " + fileToCheck + ", is a MS Office file."
                        + " It can't be uploaded for security reason. Try to transform a Word file to PDF, "
                        + "and an Excel file to CSV. For other file types try PDF.", MODULE);
                return false;
            }
            if (!isValidImageIncludingSvgFile(fileToCheck) && !checkMaxLinesLength(fileToCheck)) {
                Debug.logError("For security reason lines over " + MAXLINELENGTH.toString() + " are not allowed", MODULE);
                return false;
            }
        }

        if (isExecutable(fileToCheck)) {
            deleteBadFile(fileToCheck);
            return false;
        }

        switch (fileType) {
        case "Image":
            if (isValidImageFile(fileToCheck)) {
                return true;
            }
            break;

        case "ImageAndSvg":
            if (isValidImageIncludingSvgFile(fileToCheck)) {
                return true;
            }
            break;

        case "PDF":
            if (isValidPdfFile(fileToCheck)) {
                return true;
            }
            break;

        case "Compressed":
            if (isValidCompressedFile(fileToCheck, delegator)) {
                return true;
            }
            break;

        case "AllButCompressed":
            if (isValidTextFile(fileToCheck, true)
                    || isValidImageIncludingSvgFile(fileToCheck)
                    || isValidPdfFile(fileToCheck)) {
                return true;
            }
            break;

        case "Text":
            // The philosophy for isValidTextFile() is that
            // we can't presume of all possible text contents used for attacks with payloads
            // At least there is an easy way to prevent them in isValidTextFile
            if (isValidTextFile(fileToCheck, true)) {
                return true;
            }
            break;

        case "Audio":
            if (isValidAudioFile(fileToCheck)) {
                return true;
            }
            break;
        case "Video":
            if (isValidVideoFile(fileToCheck)) {
                return true;
            }
            break;
        case "CSV":
            if (isValidCsvFile(fileToCheck)) {
                return true;
            }
            break;

        default: // All
            if (isValidTextFile(fileToCheck, true)
                    || isValidImageIncludingSvgFile(fileToCheck)
                    || isValidCompressedFile(fileToCheck, delegator)
                    || isValidAudioFile(fileToCheck)
                    || isValidVideoFile(fileToCheck)
                    || isValidPdfFile(fileToCheck)
                    || isValidCsvFile(fileToCheck)) {
                return true;
            }
            break;
        }
        deleteBadFile(fileToCheck);
        return false;
    }

    /**
     * Is it a supported image format?
     * @param fileName
     * @return true if it's a valid image file
     * @throws IOException ImageReadException
     */
    private static boolean isValidImageFile(String fileName) throws ImageReadException, IOException {
        Path filePath = Paths.get(fileName);
        byte[] bytesFromFile = Files.readAllBytes(filePath);
        ImageFormat imageFormat = Imaging.guessFormat(bytesFromFile);
        boolean knownRasterFormat = imageFormat.equals(ImageFormats.PNG)
                || imageFormat.equals(ImageFormats.GIF)
                || imageFormat.equals(ImageFormats.TIFF)
                || imageFormat.equals(ImageFormats.JPEG);
        if (!knownRasterFormat) {
            return false;
        }
        // imageMadeSafe() re-encodes the pixel data and overwrites the file on disk,
        // stripping all metadata and steganographic payloads. Running isValidTextFile
        // on the resulting binary pixel data would be both redundant and unreliable,
        // so it is intentionally omitted here.
        return imageMadeSafe(fileName);
    }

    /**
     * Implementation based on https://github.com/righettod/document-upload-protection sanitizer for Image file. See
     * https://github.com/righettod/document-upload-protection/blob/master/src/main/java/eu/righettod/poc/sanitizer/ImageDocumentSanitizerImpl.java
     * Uses Java built-in API in complement of Apache Commons Imaging for format not supported by the built-in API. See
     * http://commons.apache.org/proper/commons-imaging/ and http://commons.apache.org/proper/commons-imaging/formatsupport.html
     */
    private static boolean imageMadeSafe(String fileName) {
        File file = new File(fileName);
        boolean fallbackOnApacheCommonsImaging;

        if (!noWebshellInMetadata(file)) {
            return false;
        }
        if (!noWebshellInPNG(file)) {
            return false;
        }

        boolean safeState = false;

        if ((file != null) && file.exists() && file.canRead() && file.canWrite()) {
            try (OutputStream fos = Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE)) {
                // Get the image format
                String formatName;
                ImageInputStream iis = ImageIO.createImageInputStream(file);
                Iterator<ImageReader> imageReaderIterator = ImageIO.getImageReaders(iis);
                // If there not ImageReader instance found so it's means that the current format is not supported by the Java built-in API
                if (!imageReaderIterator.hasNext()) {
                    ImageInfo imageInfo = Imaging.getImageInfo(file);
                    if (imageInfo != null && imageInfo.getFormat() != null && imageInfo.getFormat().getName() != null) {
                        formatName = imageInfo.getFormat().getName();
                        fallbackOnApacheCommonsImaging = true;
                    } else {
                        throw new IOException("Format of the original image " + fileName + " is not supported for read operation !");
                    }
                } else {
                    ImageReader reader = imageReaderIterator.next();
                    formatName = reader.getFormatName();
                    fallbackOnApacheCommonsImaging = false;
                    iis.close(); // This was not correctly handled in the document-upload-protection example, and I did not spot it :/
                }

                // Load the image
                BufferedImage originalImage;
                if (!fallbackOnApacheCommonsImaging) {
                    originalImage = ImageIO.read(file);
                } else {
                    originalImage = Imaging.getBufferedImage(file);
                }

                // Check that image has been successfully loaded
                if (originalImage == null) {
                    throw new IOException("Cannot load the original image " + fileName + "!");
                }

                // Get current Width and Height of the image
                int originalWidth = originalImage.getWidth(null);
                int originalHeight = originalImage.getHeight(null);

                // Resize the image by removing 1px on Width and Height
                Image resizedImage = originalImage.getScaledInstance(originalWidth - 1, originalHeight - 1, Image.SCALE_SMOOTH);

                // Resize the resized image by adding 1px on Width and Height - In fact set image to is initial size
                Image initialSizedImage = resizedImage.getScaledInstance(originalWidth, originalHeight, Image.SCALE_SMOOTH);

                // Save image by overwriting the provided source file content
                int type = originalImage.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
                BufferedImage sanitizedImage = new BufferedImage(initialSizedImage.getWidth(null), initialSizedImage.getHeight(null), type);
                Graphics bg = sanitizedImage.getGraphics();
                bg.drawImage(initialSizedImage, 0, 0, null);
                bg.dispose();

                if (!fallbackOnApacheCommonsImaging) {
                    ImageIO.write(sanitizedImage, formatName, fos);
                } else {
                    ImageParser<?> imageParser;
                    // Handle only formats for which Apache Commons Imaging can successfully write (YES in Write column of the reference link)
                    // the image format. See reference link in the class header
                    switch (formatName) {
                    case "TIFF":
                        imageParser = new TiffImageParser();
                        break;
                    case "GIF":
                        imageParser = new GifImageParser();
                        break;
                    case "PNG":
                        imageParser = new PngImageParser();
                        break;
                    // case "JPEG":
                    // imageParser = new JpegImageParser(); // Does not provide imageParser.writeImage used below
                    // break;
                    default:
                        throw new IOException("Format of the original image " + fileName + " is not supported for write operation !");
                    }
                    imageParser.writeImage(sanitizedImage, fos, null);
                }
                // Set state flag
                safeState = true;
            } catch (IOException | ImageReadException | ImageWriteException e) {
                Debug.logWarning(e, "Error during Image file " + fileName + " processing !", MODULE);
            }
        }
        return safeState;
    }

    private static boolean noWebshellInMetadata(File file) {
        com.drew.metadata.Metadata metadata = null;
        try {
            metadata = ImageMetadataReader.readMetadata(file);
        } catch (ImageProcessingException | IOException error) {
            Debug.logError("================== Not saved for security reason ==================" + error, MODULE);
        }

        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                try {
                    if (!isValidText(tag.toString(), Collections.emptyList())) {
                        Debug.logError("================== Not saved for security reason ==================", MODULE);
                        return false;
                    }
                } catch (IOException error) {
                    Debug.logError("================== Not saved for security reason ==================" + error, MODULE);
                    return false;
                }
            }
            for (String error : directory.getErrors()) {
                Debug.logError("================== Not saved for security reason ==================" + error, MODULE);
                return false;
            }
        }
        return true;
    }

    private static boolean noWebshellInPNG(File file) {
        try {
            ImageIO.read(file);
            if (!isPNG(file)) {
                return true; // Not a PNG file, it's OK so far
            }
        } catch (IOException error) {
            Debug.logError("================== Not saved for security reason ==================" + error, MODULE);
            return false;
        }

        try (DataInputStream stream = new DataInputStream(new FileInputStream(file));) {
            byte[] data = new byte[8];
            stream.readFully(data); //Read PNG Header
            while (true) {
                data = new byte[4];
                stream.readFully(data); //Read Length
                int length = ((data[0] & 0xFF) << 24)
                        | ((data[1] & 0xFF) << 16)
                        | ((data[2] & 0xFF) << 8)
                        | (data[3] & 0xFF); //Byte array to int
                stream.readFully(data); //Read Name
                String name = new String(data); //Byte array to String
                if (name.equals("IDAT")) {
                    data = new byte[length];
                    stream.readFully(data); //Read Data
                    return inflate(data);
                } else { //Don't care about other chunks
                    data = new byte[length + 4]; //Data length + 4 byte CRC
                    stream.readFully(data); //Skip Data and CRC.
                }
            }
        } catch (IOException error) {
            Debug.logError("================== Not saved for security reason, wrong PNG IDAT (weird) ==================" + error, MODULE);
            return false;
        }
    }

    private static boolean isPNG(File file) throws IOException {
        Path filePath = Paths.get(file.getPath());
        byte[] bytesFromFile = Files.readAllBytes(filePath);
        ImageFormat imageFormat = Imaging.guessFormat(bytesFromFile);
        return (imageFormat.equals(ImageFormats.PNG));
    }

    private static boolean inflate(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        byte[] result = new byte[data.length * 5]; // Inflating ratio max is 5/1
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(result);
                if (count == 0) {
                    if (!inflater.needsInput()) { // Not everything read
                        inflater.inflate(result);
                    } else if (inflater.needsDictionary()) { // Dictionary to be loaded
                        inflater.setDictionary(result);
                        inflater.getAdler();
                    } else { // nothing to inflate, avoid endless loop
                        inflater.end();
                        return true;
                    }
                }
            }
            if (inflater.getRemaining() > 0) { // There is more than image data in IDAT, check it
                byte[] remaining = Arrays.copyOfRange(data, (int) inflater.getBytesRead(), (int) inflater.getBytesRead() + inflater.getRemaining());
                String toCheck = new String(remaining, "UTF-8");
                byte[] decoded = Base64.getDecoder().decode(toCheck);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                if (!isValidText(decodedStr, Collections.emptyList())) {
                    Debug.logError("================== Not saved for security reason ==================", MODULE);
                    inflater.end();
                    return false;
                }
            }
        } catch (DataFormatException | IOException error) {
            Debug.logError("================== Not saved for security reason ==================" + error, MODULE);
            inflater.end();
            return false;
        }
        return true;
    }

    /**
     * Is it a supported image format, including SVG?
     * @param fileName
     * @return true if it's a valid image file
     * @throws IOException ImageReadException
     */
    private static boolean isValidImageIncludingSvgFile(String fileName) throws ImageReadException, IOException {
        return isValidImageFile(fileName) || isValidSvgFile(fileName);
    }

    /**
     * Is it an SVG file?
     * @param fileName
     * @return true if it's a valid SVG file
     * @throws IOException
     */
    private static boolean isValidSvgFile(String fileName) throws IOException {
        String mimeType = getMimeTypeFromFileName(fileName);
        if ("image/svg+xml".equals(mimeType)) {
            Path filePath = Paths.get(fileName);
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            try {
                f.createDocument(filePath.toUri().toString());
            } catch (IOException e) {
                return false;
            }
            // DOM-level inspection: block dangerous elements (<script>, <foreignObject>, …)
            // and unsafe attribute values (on* event handlers, javascript:/data: URIs).
            // This is stronger than the text-token deny-list which can be bypassed by
            // splitting tokens or using alternate serialisations.
            return isSafeSvgContent(fileName);
        }
        return false;
    }

    /**
     * @param fileName
     * @return true if it's a PDF file
     */
    private static boolean isPdfFile(String fileName) {
        File file = new File(fileName);
        try {
            if (Objects.isNull(file) || !file.exists()) {
                return false;
            }
            // Load stream in PDF parser — just probe whether the file is a valid PDF
            try (PdfReader ignored = new PdfReader(file.getAbsolutePath())) {
                return true;
            }
        } catch (Exception e) {
            // If it's not a PDF then exception will be thrown and return will be false
            return false;
        }
    }

    /**
     * @param fileName
     * @return true if it's a safe PDF file: is a PDF, and contains only 1 embedded readable (valid and secure) XML file (zUGFeRD)
     */
    private static boolean isValidPdfFile(String fileName) {
        if (!isPdfFile(fileName)) {
            return false;
        }
        File file = new File(fileName);
        boolean safeState = false;
        boolean canParseZUGFeRD = true;
        try {
            if (Objects.isNull(file) || !file.exists()) {
                return safeState;
            }
            // Load stream in PDF parser
            String jsCode;
            try (PdfReader reader = new PdfReader(file.getAbsolutePath())) {
                // Check 1: detect if the document contains any JavaScript code
                jsCode = reader.getJavaScript();
            }
            if (!Objects.isNull(jsCode)) {
                return safeState;
            }
            // OK no JS code, pass to check 2: detect if the document has any embedded files
            PDEmbeddedFilesNameTreeNode efTree = null;
            try (PDDocument pdDocument = Loader.loadPDF(new RandomAccessReadBufferedFile(fileName))) {
                    //PDDocument.load(file)) {
                PDDocumentNameDictionary names = new PDDocumentNameDictionary(pdDocument.getDocumentCatalog());
                efTree = names.getEmbeddedFiles();
            }
            boolean zUGFeRDCompliantUploadAllowed = UtilProperties.getPropertyAsBoolean("security", "allowZUGFeRDCompliantUpload", false);
            if (zUGFeRDCompliantUploadAllowed && !Objects.isNull(efTree)) {
                canParseZUGFeRD = false;
                Integer numberOfEmbeddedFiles = efTree.getNames().size();
                if (numberOfEmbeddedFiles.equals(1)) {
                    ZUGFeRDImporter importer = new ZUGFeRDImporter(file.getAbsolutePath());
                    boolean allowZUGFeRDnotSecure = UtilProperties.getPropertyAsBoolean("security", "allowZUGFeRDnotSecure", false);
                    if (allowZUGFeRDnotSecure) {
                        canParseZUGFeRD = importer.canParse();
                    } else {
                        try {
                            Document document = UtilXml.readXmlDocument(importer.getUTF8());
                            if (document.toString().equals("[#document: null]")) {
                                safeState = false;
                                Debug.logInfo("The file " + file.getAbsolutePath()
                                        + " is not a readable (valid and secure) PDF file. For security reason it's not accepted as a such file",
                                        MODULE);

                            }
                        } catch (SAXException | ParserConfigurationException | IOException e) {
                            safeState = false;
                            Debug.logInfo(e, "The file " + file.getAbsolutePath()
                                    + " is not a readable (valid and secure) PDF file. For security reason it's not accepted as a such file",
                                    MODULE);
                        }
                    }
                }
            }
            safeState = Objects.isNull(efTree) || canParseZUGFeRD;
        } catch (Exception e) {
            safeState = false;
            Debug.logInfo(e, "The file " + file.getAbsolutePath() + " is not a readable (valid and secure) PDF file. "
                    + "For security reason it's not accepted as a such file",
                    MODULE);
        }
        return safeState;
    }

    /**
     * Is it a CVS file?
     * @param fileName
     * @return true if it's a valid CVS file
     * @throws IOException
     */
    private static boolean isValidCsvFile(String fileName) throws IOException {
        Path filePath = Paths.get(fileName);
        String content = new String(Files.readAllBytes(filePath));
        Reader in = new StringReader(content);
        String cvsFormatString = UtilProperties.getPropertyValue("security", "csvformat");
        CSVFormat cvsFormat = CSVFormat.DEFAULT;
        switch (cvsFormatString) {
        case "EXCEL":
            cvsFormat = CSVFormat.EXCEL;
            break;
        case "MYSQL":
            cvsFormat = CSVFormat.MYSQL;
            break;
        case "ORACLE":
            cvsFormat = CSVFormat.ORACLE;
            break;
        case "POSTGRESQL_CSV":
            cvsFormat = CSVFormat.POSTGRESQL_CSV;
            break;
        default:
            cvsFormat = CSVFormat.DEFAULT;
        }

        // cf. https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html
        if (!content.contains("</svg>")) {
            try (CSVParser parser = new CSVParser(in, cvsFormat)) {
                parser.getRecords();
            }
        } else {
            Debug.logInfo("The file " + fileName + " is not a valid CSV file. For security reason it's not accepted as a such file", MODULE);
        }
        return isValidTextFile(fileName, false); // Validate content to prevent webshell
    }

    private static boolean isExecutable(String fileName) throws IOException {
        String mimeType = getMimeTypeFromFileName(fileName);
        // Check for Windows executable. Neglect .bat and .ps1: https://s.apache.org/c8sim
        if ("application/x-msdownload".equals(mimeType) || "application/x-ms-installer".equals(mimeType)) {
            Debug.logError("The file " + fileName + " is a Windows executable, for security reason it's not accepted", MODULE);
            return true;
        }
        // Check for ELF (Linux) and scripts
        if ("application/x-elf".equals(mimeType)
                || "application/x-sh".equals(mimeType)
                || "application/text/x-perl".equals(mimeType)
                || "application/text/x-ruby".equals(mimeType)
                || "application/text/x-python".equals(mimeType)) {
            Debug.logError("The file " + fileName + " is a Linux executable, for security reason it's not accepted", MODULE);
            return true;
        }
        return false;
    }

    /**
     * Check if the compressed file is valid Does not handle compressed files in sub folders of compressed files. Handles only ZIP files, if you need
     * bzip, rar, tar or/and 7z file formats they can be handled by Apache commons-compress: Types based on
     * https://developer.mozilla.org/fr/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types For code explanations see
     * http://commons.apache.org/proper/commons-compress/examples.html
     * @param fileName
     * @return true if it's a valid compressed file
     * @throws IOException ImageReadException
     */
    private static boolean isValidCompressedFile(String fileName, Delegator delegator) throws IOException, ImageReadException {
        String mimeType = getMimeTypeFromFileName(fileName);
        // I planned to handle more formats but did only ZIP
        // The code can be extended based on that
        // if ("application/octet-stream".equals(mimeType)
        // || "application/x-bzip".equals(mimeType)
        // || "application/x-bzip2".equals(mimeType)
        // || "application/java-archive".equals(mimeType)
        // || "application/x-rar-compressed".equals(mimeType)
        // || "application/x-tar".equals(mimeType)
        // || "application/zip".equals(mimeType)
        // || "application/x-zip-compressed".equals(mimeType)
        // || "multipart/x-zip".equals(mimeType)
        // || "application/x-7z-compressed".equals(mimeType)) {

        // Handles only Zip format OOTB
        File fileToCheck = new File(fileName);
        String folderName = fileToCheck.getParentFile().toString() + File.separator + UUID.randomUUID();
        if ("application/octet-stream".equals(mimeType)
                || "application/java-archive".equals(mimeType)
                || "application/zip".equals(mimeType)
                || "application/x-zip-compressed".equals(mimeType)
                || "multipart/x-zip".equals(mimeType)) {
            if (!FileUtil.unZip(fileName, folderName, "")) {
                return false;
            } else {
                // Keep it like that to allow to spot other file types which could be included...
                // try {
                // recursiveParserWrapper(fileName);
                // } catch (SAXException | TikaException e) {
                // // TODO Auto-generated catch block
                // e.printStackTrace();
                // }
                // Recursive method to check inside directories
                return isValidDirectoryInCompressedFile(folderName, delegator);
            }
        }
        return false;
    }

    /*
     * According to http://tika.apache.org/1.24.1/detection.html#The_default_Tika_Detector The simplest way to detect is through the Tika Facade
     * class, which provides methods to detect based on File, InputStream, InputStream and Filename, Filename or a few others. It works best with a
     * File or TikaInputStream.
     * @param fileName
     * @return true if the file is valid
     */
    private static String getMimeTypeFromFileName(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            Tika tika = new Tika();
            return tika.detect(file);
        }
        return null;
    }

    private static boolean isValidDirectoryInCompressedFile(String folderName, Delegator delegator) throws IOException, ImageReadException {
        File folder = new File(folderName);
        Collection<File> files = FileUtils.listFiles(folder, null, true);
        for (File f : files) {
            if (f.isDirectory()) {
                Collection<File> dirInside = FileUtils.listFiles(f, null, true);
                for (File insideFile : dirInside) {
                    if (!isValidDirectoryInCompressedFile(insideFile.getAbsolutePath(), delegator)) {
                        FileUtils.deleteDirectory(folder);
                        return false;
                    }
                }
            } else if (!isValidFile(f.getAbsolutePath(), "AllButCompressed", delegator)) {
                FileUtils.deleteDirectory(folder);
                return false;
            }
        }
        FileUtils.deleteDirectory(folder);
        return true;
    }

    /**
     * For documents that may contain embedded documents, it might be helpful to create list of metadata objects, one for the container document and
     * one for each embedded document. This allows easy access to both the extracted content and the metadata of each embedded document. Note that
     * many document formats can contain embedded documents, including traditional container formats -- zip, tar and others -- but also common office
     * document formats including: MSWord, MSExcel, MSPowerPoint, RTF, PDF, MSG and several others.
     * <p>
     * The "content" format is determined by the ContentHandlerFactory, and the content is stored in
     * {@link org.apache.tika.parser.RecursiveParserWrapper#TIKA_CONTENT}
     * <p>
     * The drawback to the RecursiveParserWrapper is that it caches metadata and contents in memory. This should not be used on files whose contents
     * are too big to be handled in memory.
     * @return a list of metadata object, one each for the container file and each embedded file
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    // This can turn to be useful, so I let it there...
    // Inspired by https://cwiki.apache.org/confluence/display/tika/RecursiveMetadata
    // And https://stackoverflow.com/questions/62132310/apache-tika-exctract-file-names-and-mime-types-from-archive
    @SuppressWarnings("unused")
    private static Set<String> recursiveParserWrapper(String fileName) throws IOException, SAXException, TikaException {
        File file = new File(fileName);
        Parser p = new AutoDetectParser();
        ContentHandlerFactory factory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.IGNORE, -1);
        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(p);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.ORIGINAL_RESOURCE_NAME, file.getName());
        ParseContext context = new ParseContext();
        RecursiveParserWrapperHandler handler = new RecursiveParserWrapperHandler(factory, -1);
        try (InputStream stream = new FileInputStream(file)) {
            wrapper.parse(stream, handler, metadata, context);
        }
        List<Metadata> metadatas = handler.getMetadataList();
        Set<String> mimeTypes = new HashSet<>();
        for (Metadata metadata1 : metadatas) {
            mimeTypes.add(metadata1.get(Metadata.CONTENT_TYPE));
        }
        return mimeTypes;
    }

    /**
     * Is this a valid Audio file?
     * @param fileName must be an UTF-8 encoded text file
     * @return true if it's a valid Audio file?
     * @throws IOException
     */
    private static boolean isValidAudioFile(String fileName) throws IOException {
        String mimeType = getMimeTypeFromFileName(fileName);
        if ("audio/basic".equals(mimeType)
                || "audio/wav".equals(mimeType)
                || "audio/x-ms-wax".equals(mimeType)
                || "audio/mpeg".equals(mimeType)
                || "audio/mp4".equals(mimeType)
                || "audio/ogg".equals(mimeType)
                || "audio/vorbis".equals(mimeType)
                || "audio/x-ogg".equals(mimeType)
                || "audio/flac".equals(mimeType)
                || "audio/x-flac".equals(mimeType)) {
            return true;
        }
        Debug.logInfo("The file " + fileName + " is not a valid audio file. For security reason it's not accepted as a such file", MODULE);
        return false;
    }

    /**
     * Is this a valid Audio file?
     * @param fileName must be an UTF-8 encoded text file
     * @return true if it's a valid Audio file?
     * @throws IOException
     */
    private static boolean isValidVideoFile(String fileName) throws IOException {
        String mimeType = getMimeTypeFromFileName(fileName);
        if ("video/avi".equals(mimeType)
                || "video/mpeg".equals(mimeType)
                || "video/mp4".equals(mimeType)
                || "video/quicktime".equals(mimeType)
                || "video/3gpp".equals(mimeType)
                || "video/x-ms-asf".equals(mimeType)
                || "video/x-flv".equals(mimeType)
                || "video/x-ms-wvx".equals(mimeType)
                || "video/x-ms-wm".equals(mimeType)
                || "video/x-ms-wmv".equals(mimeType)
                || "video/x-ms-wmx".equals(mimeType)) {
            return true;
        }
        Debug.logInfo("The file " + fileName + " is not a valid video file. For security reason it's not accepted as a such file", MODULE);
        return false;
    }

    /**
     * Validates that the file at {@code fileName} is safe plain text.
     * <p>Three checks are applied in order:
     * <ol>
     *   <li><strong>UTF-8 encoding</strong> (when {@code encodedContent} is {@code true}) — rejects
     *       binary files masquerading as text.</li>
     *   <li><strong>Dangerous XML constructs</strong> — rejects {@code xlink:href="http"} (external
     *       resource loading) and {@code <!ENTITY} (Billion-Laughs / XXE).</li>
     *   <li><strong>Character-class allow-list</strong> ({@link #isValidTextContent}) — accepts only
     *       printable Unicode code points and standard whitespace.  This is an <em>allow-list</em>
     *       strategy and cannot be bypassed by token splitting, alternate encodings, or obfuscation
     *       with whitespace, unlike the token deny-list previously used here.</li>
     * </ol>
     *
     * @param fileName       path to the file to validate
     * @param encodedContent {@code true} to additionally enforce UTF-8 validity
     * @return {@code true} when the file passes all checks
     * @throws IOException if the file cannot be read
     */
    private static boolean isValidTextFile(String fileName, Boolean encodedContent) throws IOException {
        Path filePath = Paths.get(fileName);
        byte[] bytesFromFile = Files.readAllBytes(filePath);
        if (encodedContent) {
            try {
                Charset.availableCharsets().get("UTF-8").newDecoder().decode(ByteBuffer.wrap(bytesFromFile));
            } catch (CharacterCodingException e) {
                return false;
            }
        }
        String content = new String(bytesFromFile);
        if (content.toLowerCase().contains("xlink:href=\"http")
                || content.toLowerCase().contains("<!ENTITY ")) { // Billions laugh attack
            Debug.logInfo("Linked images inside or Entity in SVG are not allowed for security reason", MODULE);
            return false;
        }
        // Use the character-class allow-list instead of the token deny-list.
        // The deny-list can be bypassed by token splitting, alternate encodings and
        // obfuscation; isValidTextContent operates at the decoded code-point level
        // and is immune to those bypass techniques.
        return isValidTextContent(content);
    }

    // Check there is no web shell
    private static boolean isValid(String content, String string, List<String> allowed) {
        boolean isOK = !content.contains(string) || allowed.contains(string);
        if (!isOK) {
            Debug.logInfo("The uploaded file contains the string '" + string + "'. It can't be uploaded for security reason", MODULE);
        }
        return isOK;
    }

    // Check there is no reverse shell in query string
    private static boolean isValid(List<String> queryParameters, String string, List<String> allowed) {
        boolean isOK = true;

        for (String parameter : queryParameters) {
            if (!parameter.contains(string)
                    || allowed.contains(HashCrypt.cryptBytes("SHA", "OFBiz", parameter.toLowerCase().getBytes(StandardCharsets.UTF_8)))) {
                continue;
            } else {
                isOK = false;
                break;
            }
        }
        if (!isOK) {
            Debug.logInfo("The HTTP query string contains the string '" + string + "'. It can't be uploaded for security reason", MODULE);
        }
        return isOK;
    }

    private static void deleteBadFile(String fileToCheck) {
        Debug.logError("File : " + fileToCheck + ", can't be uploaded for security reason", MODULE);
        File badFile = new File(fileToCheck);
        if (badFile.exists() && !badFile.delete()) {
            Debug.logError("File : " + fileToCheck + ", couldn't be deleted", MODULE);
        }
    }

    // -----------------------------------------------------------------------
    // Extension allow-list helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the per-file-type extension allow-list map from {@code security.properties}.
     * Each key matches a {@code fileType} value accepted by {@link #isValidFile}.
     * "AllButCompressed" and "All" are derived as unions of the individual sets.
     */
    private static Map<String, Set<String>> buildAllowedExtensionsMap() {
        Set<String> image = loadAllowedExtensions("allowedExtensionsImage",
                "jpg,jpeg,png,gif,tiff,tif");
        Set<String> imageAndSvg = loadAllowedExtensions("allowedExtensionsImageAndSvg",
                "jpg,jpeg,png,gif,tiff,tif,svg");
        Set<String> pdf = loadAllowedExtensions("allowedExtensionsPdf",
                "pdf");
        Set<String> audio = loadAllowedExtensions("allowedExtensionsAudio",
                "mp3,wav,ogg,flac,mp4,m4a,aac");
        Set<String> video = loadAllowedExtensions("allowedExtensionsVideo",
                "mp4,mov,avi,mkv,webm,wmv,mpg,mpeg,3gp,flv");
        Set<String> text = loadAllowedExtensions("allowedExtensionsText",
                "txt");
        Set<String> csv = loadAllowedExtensions("allowedExtensionsCsv",
                "csv");
        Set<String> compressed = loadAllowedExtensions("allowedExtensionsCompressed",
                "zip");

        Set<String> allButCompressed = new LinkedHashSet<>();
        allButCompressed.addAll(image);
        allButCompressed.addAll(imageAndSvg);
        allButCompressed.addAll(pdf);
        allButCompressed.addAll(audio);
        allButCompressed.addAll(video);
        allButCompressed.addAll(text);
        allButCompressed.addAll(csv);

        Set<String> all = new LinkedHashSet<>(allButCompressed);
        all.addAll(compressed);

        Map<String, Set<String>> map = new LinkedHashMap<>();
        map.put("Image", image);
        map.put("ImageAndSvg", imageAndSvg);
        map.put("PDF", pdf);
        map.put("Audio", audio);
        map.put("Video", video);
        map.put("Text", text);
        map.put("CSV", csv);
        map.put("Compressed", compressed);
        map.put("AllButCompressed", Collections.unmodifiableSet(allButCompressed));
        map.put("All", Collections.unmodifiableSet(all));
        return Collections.unmodifiableMap(map);
    }

    /**
     * Loads a comma-separated list of lowercase file extensions from
     * {@code security.properties} under the given key, falling back to
     * {@code defaultValue} when the property is absent or blank.
     */
    private static Set<String> loadAllowedExtensions(String propertyKey, String defaultValue) {
        String value = UtilProperties.getPropertyValue("security", propertyKey, defaultValue);
        Set<String> exts = new LinkedHashSet<>();
        for (String ext : value.split(",")) {
            String trimmed = ext.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                exts.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(exts);
    }

    /**
     * Returns {@code true} if the extension of {@code fileToCheck} is in the
     * allow-list for the given {@code fileType}.  Falls back to the "All" set
     * for unknown file-type strings so that callers are never silently blocked
     * by a misconfigured type name.
     */
    private static boolean isAllowedExtension(String fileToCheck, String fileType) {
        Set<String> allowed = ALLOWED_EXTENSIONS_BY_TYPE.getOrDefault(
                fileType, ALLOWED_EXTENSIONS_BY_TYPE.get("All"));
        String extension = FilenameUtils.getExtension(fileToCheck).toLowerCase(Locale.ROOT);
        if (allowed.contains(extension)) {
            return true;
        }
        Debug.logError("File extension [." + extension + "] is not in the allow-list for"
                + " file type [" + fileType + "]. To permit it, add it to the"
                + " allowedExtensions" + fileType + " property in security.properties.", MODULE);
        return false;
    }

    // -----------------------------------------------------------------------
    // SVG DOM safety check
    // -----------------------------------------------------------------------

    /**
     * Parses {@code fileName} as XML and walks the DOM tree looking for elements
     * or attributes that can execute code or load external resources.
     *
     * <p>This replaces the text-token deny-list ({@code isValidTextFile}) for SVG
     * files.  Token scanning can be bypassed by splitting keywords across lines,
     * using alternate encodings, or exploiting SVG/XML serialisation ambiguities.
     * DOM-level inspection operates on the fully-parsed structure and is therefore
     * immune to those bypass techniques.
     *
     * <p>The parser is hardened against XXE and DOCTYPE-injection before use.
     *
     * @return {@code true} if no unsafe element or attribute was found
     */
    private static boolean isSafeSvgContent(String fileName) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden against XXE and DOCTYPE-based injection attacks
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(fileName));
            return checkSvgNode(doc.getDocumentElement());
        } catch (Exception e) {
            Debug.logWarning(e, "SVG content safety check failed for: " + fileName, MODULE);
            return false;
        }
    }

    /**
     * Recursively walks an SVG DOM tree and returns {@code false} on the first
     * unsafe element or attribute found.
     *
     * <p>Rejected elements: anything in {@link #DENIED_SVG_ELEMENTS} (e.g.
     * {@code <script>}, {@code <foreignObject>}, animation elements).
     * Rejected attributes:
     * <ul>
     *   <li>{@code on*} event-handler attributes (e.g. {@code onclick}, {@code onload})</li>
     *   <li>{@code href}, {@code xlink:href}, {@code src}, {@code action} pointing to a
     *       {@code javascript:}, {@code data:}, or {@code vbscript:} URI scheme</li>
     * </ul>
     */
    private static boolean checkSvgNode(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String localName = node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
            if (DENIED_SVG_ELEMENTS.contains(localName.toLowerCase(Locale.ROOT))) {
                Debug.logInfo("SVG rejected: contains denied element <" + localName + ">", MODULE);
                return false;
            }
            NamedNodeMap attrs = node.getAttributes();
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    Attr attr = (Attr) attrs.item(i);
                    String attrName = attr.getName().toLowerCase(Locale.ROOT);
                    String attrValue = attr.getValue() == null ? ""
                            : attr.getValue().trim().toLowerCase(Locale.ROOT);
                    // Block on* event-handler attributes
                    if (attrName.startsWith("on")) {
                        Debug.logInfo("SVG rejected: event-handler attribute ["
                                + attr.getName() + "]", MODULE);
                        return false;
                    }
                    // Block unsafe URI schemes in link/source attributes
                    if (attrName.equals("href") || attrName.equals("xlink:href")
                            || attrName.equals("src") || attrName.equals("action")) {
                        if (attrValue.startsWith("javascript:")
                                || attrValue.startsWith("data:")
                                || attrValue.startsWith("vbscript:")) {
                            Debug.logInfo("SVG rejected: unsafe URI scheme in attribute ["
                                    + attr.getName() + "]", MODULE);
                            return false;
                        }
                    }
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!checkSvgNode(children.item(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> getDeniedFileExtensions() {
        String deniedExtensions = UtilProperties.getPropertyValue("security", "deniedFileExtensions");
        return UtilValidate.isNotEmpty(deniedExtensions) ? StringUtil.split(deniedExtensions, ",") : new ArrayList<>();
    }

    private static List<String> getDeniedWebShellTokens() {
        String deniedTokens = UtilProperties.getPropertyValue("security", "deniedWebShellTokens");
        return UtilValidate.isNotEmpty(deniedTokens) ? StringUtil.split(deniedTokens, ",") : new ArrayList<>();
    }

    private static boolean checkMaxLinesLength(String fileToCheck) {
        if (MAXLINELENGTH == 0) {
            return true;
        }
        try {
            File file = new File(fileToCheck);
            List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
            for (String line : lines) {
                if (line.length() > MAXLINELENGTH) {
                    return false;
                }
            }
        } catch (IOException e) {
            Debug.logError(e, "File : " + fileToCheck + ", can't be uploaded for security reason", MODULE);
            return false;
        }
        return true;
    }
}
