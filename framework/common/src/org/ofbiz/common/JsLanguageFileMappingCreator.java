package org.ofbiz.common;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.io.FileUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import freemarker.template.TemplateException;

public class JsLanguageFileMappingCreator {

    private static final String module = JsLanguageFileMappingCreator.class.getName();

    public static Map<String, Object> createJsLanguageFileMapping(DispatchContext ctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Locale> localeList = UtilMisc.availableLocales();
        Map<String, Object> jQueryLocaleFile = FastMap.newInstance();
        Map<String, String> dateJsLocaleFile = FastMap.newInstance();
        Map<String, String> validationLocaleFile = FastMap.newInstance();
        //Map<String, String> validationMethodsLocaleFile = FastMap.newInstance();

        // setup some variables to locate the js files
        String componentRoot = "component://images/webapp";
        String jqueryUiLocaleRelPath = "/images/jquery/ui/development-bundle/ui/i18n/";
        String dateJsLocaleRelPath = "/images/jquery/plugins/datejs/";
        String validateRelPath = "/images/jquery/plugins/validate/localization/";
        String jsFilePostFix = ".js";
        String dateJsLocalePrefix = "date-";
        String validateLocalePrefix = "messages_";
        //String validateMethLocalePrefix = "methods__";
        String jqueryUiLocalePrefix = "jquery.ui.datepicker-";
        String defaultLocaleDateJs = "en-US";
        String defaultLocaleJquery = "en";

        for (Locale locale : localeList) {
            String displayCountry = locale.toString();
            String modifiedDisplayCountry = null;
            if (displayCountry.indexOf('_') != -1) {
                modifiedDisplayCountry = displayCountry.replace("_", "-");
            } else {
                modifiedDisplayCountry = displayCountry;
            }

            String strippedLocale = locale.getLanguage();

            File file = null;
            String fileUrl = null;

            /*
             * Try to open the date-js language file
             */
            String fileName = componentRoot + dateJsLocaleRelPath + dateJsLocalePrefix + modifiedDisplayCountry + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = dateJsLocaleRelPath + dateJsLocalePrefix + modifiedDisplayCountry + jsFilePostFix;
            }

            if (fileUrl == null) {
                // Try to guess a language
                String tmpLocale = strippedLocale + "-" + strippedLocale.toUpperCase();
                fileName = componentRoot + dateJsLocaleRelPath + dateJsLocalePrefix + tmpLocale + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = dateJsLocaleRelPath + dateJsLocalePrefix + tmpLocale + jsFilePostFix;
                }
            }

            if (fileUrl == null) {
                // use default language en-US
                fileUrl = dateJsLocaleRelPath + dateJsLocalePrefix + defaultLocaleDateJs + jsFilePostFix;
            }

            dateJsLocaleFile.put(displayCountry, fileUrl);

            fileUrl = null;

            /*
             * Try to open the jquery validation language file
             */
            fileName = componentRoot + validateRelPath + validateLocalePrefix + strippedLocale + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = validateRelPath + validateLocalePrefix + strippedLocale + jsFilePostFix;
            }

            if (fileUrl == null) {
                fileUrl = validateRelPath + validateLocalePrefix + defaultLocaleJquery + jsFilePostFix;
            }
            validationLocaleFile.put(displayCountry, fileUrl);

            fileUrl = null;

            /*
             * Try to open the jquery timepicker language file
             */
            file = null;
            fileUrl = null;

            fileName = componentRoot + jqueryUiLocaleRelPath + jqueryUiLocalePrefix + strippedLocale + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = jqueryUiLocaleRelPath + jqueryUiLocalePrefix + strippedLocale + jsFilePostFix;
            } else {
                fileName = componentRoot + jqueryUiLocaleRelPath + jqueryUiLocalePrefix + locale + jsFilePostFix;
                file = FileUtil.getFile(fileName);

                if (file.exists()) {
                    fileUrl = jqueryUiLocaleRelPath + jqueryUiLocalePrefix + locale + jsFilePostFix;
                }
            }

            if (fileUrl == null) {
                fileUrl = jqueryUiLocaleRelPath + jqueryUiLocalePrefix + defaultLocaleJquery + jsFilePostFix;
            }

            jQueryLocaleFile.put(displayCountry, fileUrl);
        }

        // check the template file
        String template = "framework/common/template/JsLanguageFilesMapping.ftl";
        String output = "framework/common/src/org/ofbiz/common/JsLanguageFilesMapping.java";
        Map<String, Object> mapWrapper = new HashMap<String, Object>();
        mapWrapper.put("datejs", dateJsLocaleFile);
        mapWrapper.put("jquery", jQueryLocaleFile);
        mapWrapper.put("validation", validationLocaleFile);

        // some magic to create a new java file
        // render it as FTL
        Writer writer = new StringWriter();
        try {
            FreeMarkerWorker.renderTemplateAtLocation(template, mapWrapper, writer);
        }
        catch (MalformedURLException e) {
            Debug.logError(e, module);
            return result = ServiceUtil.returnError("The Outputfile could not be created: " + e.getMessage());
        }
        catch (TemplateException e) {
            Debug.logError(e, module);
            return result = ServiceUtil.returnError("The Outputfile could not be created: " + e.getMessage());
        }
        catch (IOException e) {
            Debug.logError(e, module);
            return result = ServiceUtil.returnError("The Outputfile could not be created: " + e.getMessage());
        }
        catch (IllegalArgumentException e) {
            Debug.logError(e, module);
            return result = ServiceUtil.returnError("The Outputfile could not be created: " + e.getMessage());
        }

        // write it as a Java file
        File file = new File(output);
        try {
            FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
        }
        catch (IOException e) {
            Debug.logError(e, module);
            return result = ServiceUtil.returnError("The Outputfile could not be created: " + e.getMessage());
        }

        return result;
    }

}
