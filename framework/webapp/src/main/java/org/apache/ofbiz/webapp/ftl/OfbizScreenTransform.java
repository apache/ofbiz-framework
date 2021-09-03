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
package org.apache.ofbiz.webapp.ftl;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TemplateScalarModel;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.xml.sax.SAXException;

/**
 * OfbizScreenTransform - Freemarker Transform to display a screen by is location and name
 *
 * You can call a Ofbiz screen with the ftl context with simple macro
 *    &lt;&#064;ofbizScreen&gt;component://mycomponent/widget/MyComponentScreens.xml#MyScreen&lt;/&#064;ofbizScreen&gt;
 *
 * You can also write
 *    &lt;&#064;ofbizScreen location="component://mycomponent/widget/MyComponentScreens.xml" name="MyScreen"/&gt;
 *
 * Or set a default location on your context
 *    action :
 *        context.defaultTemplateLocation = "component://mycomponent/widget/MyComponentScreens.xml"
 *    widget :
 *        &lt;&#064;ofbizScreen&gt;MyScreen&lt;/&#064;ofbizScreen&gt;
 *
 */
public class OfbizScreenTransform implements TemplateTransformModel {

    private static final String MODULE = OfbizScreenTransform.class.getName();

    private static String convertToString(Object o) {
        String result = "";
        if (o != null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Arg Object : " + o.getClass().getName(), MODULE);
            }
            if (o instanceof TemplateScalarModel) {
                TemplateScalarModel s = (TemplateScalarModel) o;
                try {
                    result = s.getAsString();
                } catch (TemplateModelException e) {
                    Debug.logError(e, "Template Exception", MODULE);
                }
            } else {
                result = o.toString();
            }
        }
        return result;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();
        final Map<String, Object> context = UtilGenerics.cast(FreeMarkerWorker.createEnvironmentMap(Environment.getCurrentEnvironment()));
        final String location = convertToString(args.get("location"));
        final String name = convertToString(args.get("name"));
        final String screenType = args.get("type") != null
                ? convertToString(args.get("type"))
                : "screen";
        return new Writer(out) {
            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    HttpServletRequest request = req == null ? null : (HttpServletRequest) req.getWrappedObject();
                    VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
                    ModelTheme modelTheme = visualTheme.getModelTheme();

                    String screenName = name.isEmpty() ? buf.toString() : name;

                    String screenMacroLibraryPath = modelTheme.getScreenRendererLocation(screenType);
                    ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(modelTheme.getType(screenType), screenMacroLibraryPath);

                    Writer writer = new StringWriter();
                    ScreenRenderer screens = new ScreenRenderer(writer, MapStack.create(context), screenStringRenderer);

                    //check if the name is combined
                    if (screenName.contains("#")) {
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Call screen with combined location" + screenName, MODULE);
                        }
                        screens.render(screenName);
                    } else {
                        String forwardLocation = !location.isEmpty()
                                ? location
                                : FreeMarkerWorker.unwrap(env.getVariable("defaultTemplateLocation"));
                        if (forwardLocation == null) {
                            forwardLocation = "component://common/widget/CommonScreens.xml";
                        }
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Call screen " + screenName + ", at location : " + forwardLocation, MODULE);
                        }
                        screens.render(forwardLocation, screenName);
                    }

                    out.write(writer.toString());
                } catch (GeneralException | SAXException | ParserConfigurationException | TemplateException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
