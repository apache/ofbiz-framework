package org.apache.ofbiz.webapp.ftl;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateTransformModel;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.common.JsLanguageFilesMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * access JsLanguageFilesMapping from ftl using macro
 */
public class JsLanguageFilesMappingTransform implements TemplateTransformModel {

    private static final String MODULE = JsLanguageFilesMappingTransform.class.getName();

    @Override
    public Writer getWriter(Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        return new Writer(out) {
            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    String libraryName = buf.toString();
                    if (!libraryName.isEmpty()) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        String localeString = UtilHttp.getLocale(request).toString();
                        switch (libraryName) {
                            case "datejs":
                                out.write(JsLanguageFilesMapping.datejs.getFilePath(localeString));
                                break;
                            case "dateTime":
                                out.write(JsLanguageFilesMapping.dateTime.getFilePath(localeString));
                                break;
                            case "jquery":
                                out.write(JsLanguageFilesMapping.jquery.getFilePath(localeString));
                                break;
                            case "select2":
                                out.write(JsLanguageFilesMapping.select2.getFilePath(localeString));
                                break;
                            case "validation":
                                out.write(JsLanguageFilesMapping.validation.getFilePath(localeString));
                                break;
                            default:
                        }
                    }
                } catch (Exception e) {
                    Debug.logWarning(e, "Exception thrown while running " + MODULE, MODULE);
                    throw new IOException(e);
                }
            }
            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }
        };
    }
}
