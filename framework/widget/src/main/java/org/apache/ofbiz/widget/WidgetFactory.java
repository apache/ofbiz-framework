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
package org.apache.ofbiz.widget;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.widget.model.IterateSectionWidget;
import org.apache.ofbiz.widget.model.ModelScreen;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.w3c.dom.Element;

/**
 * Screen widget factory.<p>Applications can add their own widget implementations
 * to the factory by implementing the {@link org.apache.ofbiz.widget.WidgetLoader} interface.</p>
 */
public class WidgetFactory {

    public static final String module = WidgetFactory.class.getName();
    protected static final Map<String, Constructor<? extends ModelScreenWidget>> screenWidgets = new ConcurrentHashMap<>();

    static {
        loadStandardWidgets();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Iterator<WidgetLoader> widgetLoaders = ServiceLoader.load(WidgetLoader.class, loader).iterator();
        while (widgetLoaders.hasNext()) {
            try {
                WidgetLoader widgetLoader = widgetLoaders.next();
                widgetLoader.loadWidgets();
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }
    }

    /**
     * Returns a <code>ModelScreenWidget</code> instance that implements the specified
     * XML element.
     * 
     * @param modelScreen The containing screen for the widget
     * @param element The widget XML element
     * @return a <code>ModelScreenWidget</code> instance that implements the specified
     * XML element
     * @throws IllegalArgumentException
     */
    public static ModelScreenWidget getModelScreenWidget(ModelScreen modelScreen, Element element) {
        Assert.notNull("modelScreen", modelScreen, "element", element);
        String tagName = UtilXml.getTagNameIgnorePrefix(element);
        Constructor<? extends ModelScreenWidget> widgetConst = screenWidgets.get(tagName);
        if (widgetConst == null) {
            throw new IllegalArgumentException("ModelScreenWidget class not found for element " + tagName);
        }
        try {
            return widgetConst.newInstance(modelScreen, element);
        } catch (Exception e) {
            // log the original exception since the rethrown exception doesn't include much info about it and hides the cause
            Debug.logError(e, "Error getting widget for element " + element.getTagName(), module);
            throw new IllegalArgumentException(e.getMessage() + " for element " + element.getTagName());
        }
    }

    /**
     * Loads the standard OFBiz screen widgets.
     */
    protected static void loadStandardWidgets() {
        for (Class<?> clz: ModelScreenWidget.class.getClasses()) {
            try {
                // Subclass of ModelScreenWidget and non-abstract
                if (ModelScreenWidget.class.isAssignableFrom(clz) && (clz.getModifiers() & Modifier.ABSTRACT) == 0) {
                    try {
                        Field field = clz.getField("TAG_NAME");
                        Object fieldObject = field.get(null);
                        if (fieldObject != null) {
                            Class<? extends ModelScreenWidget> widgetClass = UtilGenerics.cast(clz);
                            registerScreenWidget(fieldObject.toString(), widgetClass);
                        }
                    } catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }
        try {
            registerScreenWidget("iterate-section", IterateSectionWidget.class);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    /**
     * Registers a screen sub-widget with the factory. If a tag name is already
     * registered, the new widget replaces the existing one.<p>The class supplied
     * to the method must have a public two-argument constructor that takes a
     * <code>ModelScreen</code> instance and an <code>Element</code> instance.</p>
     * 
     * @param tagName The XML element tag name for this widget
     * @param widgetClass The class that implements the widget element
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static void registerScreenWidget(String tagName, Class<? extends ModelScreenWidget> widgetClass) throws SecurityException, NoSuchMethodException {
        Assert.notNull("tagName", tagName, "widgetClass", widgetClass);
        screenWidgets.put(tagName, widgetClass.getConstructor(ModelScreen.class, Element.class));
        if (Debug.verboseOn()) {
            Debug.logVerbose("Registered " + widgetClass.getName() + " with tag name " + tagName, module);
        }
    }

    private WidgetFactory() {}
}
