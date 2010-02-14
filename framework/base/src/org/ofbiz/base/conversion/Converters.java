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
    protected static final FastMap<String, Converter<?, ?>> converterMap = FastMap.newInstance();
    protected static final FastSet<String> noConversions = FastSet.newInstance();
    /** Null converter used when the source and target java object
     * types are the same. The <code>convert</code> method returns the
     * source object.
     *
     */
    public static final Converter<Object, Object> nullConverter = new NullConverter();

    static {
        converterMap.setShared(true);
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
OUTER:
        do {
            Converter<?, ?> result = converterMap.get(key);
            if (result != null) {
                return UtilGenerics.cast(result);
            }
            if (noConversions.contains(key)) {
                throw new ClassNotFoundException("No converter found for " + key);
            }
            for (Converter<?, ?> value : converterMap.values()) {
                if (value.canConvert(sourceClass, targetClass)) {
                    converterMap.putIfAbsent(key, value);
                    continue OUTER;
                }
            }
            // Null converter must be checked last
            if (nullConverter.canConvert(sourceClass, targetClass)) {
                Converter passThruConverter = new PassThruConverter<S>(sourceClass);
                converterMap.putIfAbsent(key, passThruConverter);
                continue;
            }
            if (noConversions.add(key)) {
                Debug.logWarning("*** No converter found, converting from " +
                        sourceClass.getName() + " to " + targetClass.getName() +
                        ". Please report this message to the developer community so " +
                        "a suitable converter can be created. ***", module);
            }
            throw new ClassNotFoundException("No converter found for " + key);
        } while (true);
    }

    /** Load all classes that implement <code>Converter</code> and are
     * contained in <code>containerClass</code>.
     *
     * @param containerClass
     */
    public static void loadContainedConverters(Class<?> containerClass) {
        // This only returns -public- classes and interfaces
        for (Class<?> clz: containerClass.getClasses()) {
            try {
                // non-abstract, which means no interfaces or abstract classes
                if ((clz.getModifiers() & Modifier.ABSTRACT) == 0) {
                    Object value;
                    try {
                        value = clz.getConstructor().newInstance();
                    } catch (NoSuchMethodException e) {
                        // ignore this, as this class might be some other helper class,
                        // with a non-pubilc constructor
                        continue;
                    }
                    if (value instanceof ConverterLoader) {
                        ConverterLoader loader = (ConverterLoader) value;
                        loader.loadConverters();
                    }
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
        registerConverter(converter, converter.getSourceClass(), converter.getTargetClass());
    }

    public static <S, T> void registerConverter(Converter<S, T> converter, Class<?> sourceClass, Class<?> targetClass) {
        StringBuilder sb = new StringBuilder();
        if (sourceClass != null) {
            sb.append(sourceClass.getName());
        } else {
            sb.append("<null>");
        }
        sb.append(DELIMITER);
        if (targetClass != null) {
            sb.append(targetClass.getName());
        } else {
            sb.append("<null>");
        }
        String key = sb.toString();
        if (converterMap.putIfAbsent(key, converter) == null) {
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

        public Object convert(Class<?> targetClass, Object obj) throws ConversionException {
            return obj;
        }

        public Class<?> getSourceClass() {
            return Object.class;
        }

        public Class<?> getTargetClass() {
            return Object.class;
        }
    }

    /** Pass thru converter used when the source and target java object
     * types are the same. The <code>convert</code> method returns the
     * source object.
     *
     */
    protected static class PassThruConverter<T> implements Converter<T, T> {
        private final Class<T> clz;

        public PassThruConverter(Class<T> clz) {
            this.clz = clz;
        }

        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == clz && targetClass == clz;
        }

        public T convert(T obj) throws ConversionException {
            return obj;
        }

        public T convert(Class<?> targetClass, T obj) throws ConversionException {
            return obj;
        }

        public Class<?> getSourceClass() {
            return clz;
        }

        public Class<?> getTargetClass() {
            return clz;
        }
    }
}
