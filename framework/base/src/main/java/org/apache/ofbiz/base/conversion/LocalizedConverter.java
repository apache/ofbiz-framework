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
package org.apache.ofbiz.base.conversion;

import java.util.Locale;
import java.util.TimeZone;

/** Localized converter interface. Classes implement this interface
 * to convert one object type to another. Methods are provided to
 * localize the conversion.
 */
public interface LocalizedConverter<S, T> extends Converter<S, T> {
    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param obj The source <code>Object</code> to convert
     * @param locale The locale used for conversion - must not be <code>null</code>
     * @param timeZone The time zone used for conversion - must not be <code>null</code>
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    public T convert(S obj, Locale locale, TimeZone timeZone) throws ConversionException;

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param targetClass The <code>Class</code> to convert to
     * @param obj The source <code>Object</code> to convert
     * @param locale The locale used for conversion - must not be <code>null</code>
     * @param timeZone The time zone used for conversion - must not be <code>null</code>
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    public T convert(Class<? extends T> targetClass, S obj, Locale locale, TimeZone timeZone) throws ConversionException;

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param obj The source <code>Object</code> to convert
     * @param locale The locale used for conversion - must not be <code>null</code>
     * @param timeZone The time zone used for conversion - must not be <code>null</code>
     * @param formatString Optional formatting string
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    public T convert(S obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException;

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param targetClass The <code>Class</code> to convert to
     * @param obj The source <code>Object</code> to convert
     * @param locale The locale used for conversion - must not be <code>null</code>
     * @param timeZone The time zone used for conversion - must not be <code>null</code>
     * @param formatString Optional formatting string
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    public T convert(Class<? extends T> targetClass, S obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException;
}
