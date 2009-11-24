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
package org.ofbiz.base.conversion;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.spi.ServiceRegistry;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilGenerics;

/** A <code>Converter</code> factory and repository. */
public class Converters {
    protected static final String module = Converters.class.getName();
    protected static final String DELIMITER = "->";
    protected static final Map<String, Converter<?, ?>> converterMap = FastMap.newInstance();
    protected static final Set<String> noConversions = FastSet.newInstance();
    /** Null converter used when the source and target java object
     * types are the same. The <code>convert</code> method returns the
     * source object.
     * 
     */
    public static final Converter<Object, Object> nullConverter = new NullConverter();

    static {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Iterator<ConverterLoader> converterLoaders = ServiceRegistry.lookupProviders(ConverterLoader.class, loader);
        while (converterLoaders.hasNext()) {
            try {
                ConverterLoader converterLoader = converterLoaders.next();
                converterLoader.loadConverters();
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }
    }

    private Converters() {}

    /** Returns an appropriate <code>Converter</code> instance for
     * <code>sourceClass</code> and <code>targetClass</code>. If no matching
     * <code>Converter</code> is found, the method throws
     * <code>ClassNotFoundException</code>.
     *
     * <p>This method is intended to be used when the source or
     * target <code>Object</code> types are unknown at compile time.
     * If the source and target <code>Object</code> types are known
     * at compile time, then one of the "ready made" converters should be used.</p>
     *
     * @param sourceClass The object class to convert from
     * @param targetClass The object class to convert to
     * @return A matching <code>Converter</code> instance
     * @throws ClassNotFoundException
     */
    public static <S, T> Converter<S, T> getConverter(Class<S> sourceClass, Class<T> targetClass) throws ClassNotFoundException {
        String key = sourceClass.getName().concat(DELIMITER).concat(targetClass.getName());
        if (Debug.verboseOn()) {
            Debug.logVerbose("Getting converter: " + key, module);
        }
        Converter<?, ?> result = converterMap.get(key);
        if (result == null) {
            if (!noConversions.contains(key)) {
                synchronized (converterMap) {
                    Collection<Converter<?, ?>> values = converterMap.values();
                    for (Converter<?, ?> value : values) {
                        if (value.canConvert(sourceClass, targetClass)) {
                            converterMap.put(key, value);
                            return UtilGenerics.cast(value);
                        }
                    }
                    // Null converter must be checked last
                    if (nullConverter.canConvert(sourceClass, targetClass)) {
                        converterMap.put(key, nullConverter);
                        return UtilGenerics.cast(nullConverter);
                    }
                    noConversions.add(key);
                    Debug.logWarning("*** No converter found, converting from " +
                            sourceClass.getName() + " to " + targetClass.getName() +
                            ". Please report this message to the developer community so " +
                            "a suitable converter can be created. ***", module);
                }
            }
            throw new ClassNotFoundException("No converter found for " + key);
        }
        return UtilGenerics.cast(result);
    }

    /** Load all classes that implement <code>Converter</code> and are
     * contained in <code>containerClass</code>.
     *
     * @param containerClass
     */
    public static void loadContainedConverters(Class<?> containerClass) {
        Class<?>[] classArray = containerClass.getClasses();
        for (int i = 0; i < classArray.length; i++) {
            try {
                if ((classArray[i].getModifiers() & Modifier.ABSTRACT) == 0) {
                    classArray[i].newInstance();
                }
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }
    }

    /** Registers a <code>Converter</code> instance to be used by the
     * {@link org.ofbiz.base.conversion.Converters#getConverter(Class, Class)}
     * method.
     *
     * @param <S> The source object type
     * @param <T> The target object type
     * @param converter The <code>Converter</code> instance to register
     */
    public static <S, T> void registerConverter(Converter<S, T> converter) {
        String key = converter.getSourceClass().getName().concat(DELIMITER).concat(converter.getTargetClass().getName());
        if (converterMap.get(key) == null) {
            synchronized (converterMap) {
                converterMap.put(key, converter);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Registered converter " + converter.getClass().getName(), module);
            }
        }
    }

    /** Null converter used when the source and target java object
     * types are the same. The <code>convert</code> method returns the
     * source object.
     * 
     */
    protected static class NullConverter implements Converter<Object, Object> {
        public NullConverter() {
        }

        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            if (sourceClass.getName().equals(targetClass.getName()) || "java.lang.Object".equals(targetClass.getName())) {
                return true;
            }
            return ObjectType.instanceOf(sourceClass, targetClass);
        }

        public Object convert(Object obj) throws ConversionException {
            return obj;
        }

        public Class<?> getSourceClass() {
            return Object.class;
        }

        public Class<?> getTargetClass() {
            return Object.class;
        }
    }
}
