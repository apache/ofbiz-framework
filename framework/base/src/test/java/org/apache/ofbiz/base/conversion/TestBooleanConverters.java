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
package org.apache.ofbiz.base.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ofbiz.base.conversion.BooleanConverters;
import org.apache.ofbiz.base.conversion.Converter;
import org.apache.ofbiz.base.conversion.ConverterLoader;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.junit.Test;

public class TestBooleanConverters {

    public static <T> void assertFromBoolean(String label, Converter<Boolean, T> converter, T trueResult, T falseResult)
            throws Exception {
        assertTrue(label + " can convert", converter.canConvert(Boolean.class, trueResult.getClass()));
        assertEquals(label + " registered", converter.getClass(),
                Converters.getConverter(Boolean.class, trueResult.getClass()).getClass());
        assertEquals(label + " converted", trueResult, converter.convert(true));
        assertEquals(label + " converted", falseResult, converter.convert(false));
    }

    public static <S> void assertToBoolean(String label, Converter<S, Boolean> converter, S trueSource, S falseSource)
            throws Exception {
        assertTrue(label + " can convert", converter.canConvert(trueSource.getClass(), Boolean.class));
        assertEquals(label + " registered", converter.getClass(),
                Converters.getConverter(trueSource.getClass(), Boolean.class).getClass());
        assertEquals(label + " converted", Boolean.TRUE, converter.convert(trueSource));
        assertEquals(label + " converted", Boolean.FALSE, converter.convert(falseSource));
    }

    public static <S> void assertToCollection(String label, S source) throws Exception {
        Converter<S, ? extends Collection<S>> toList =
                UtilGenerics.cast(Converters.getConverter(source.getClass(), List.class));
        Collection<S> listResult = toList.convert(source);
        assertEquals(label + " converted to List", source, listResult.toArray()[0]);
        Converter<S, ? extends Collection<S>> toSet =
                UtilGenerics.cast(Converters.getConverter(source.getClass(), Set.class));
        Collection<S> setResult = toSet.convert(source);
        assertEquals(label + " converted to Set", source, setResult.toArray()[0]);
    }

    @Test
    public void testBooleanConverters() throws Exception {
        ConverterLoader loader = new BooleanConverters();
        loader.loadConverters();
        assertFromBoolean("BooleanToInteger", new BooleanConverters.BooleanToInteger(), 1, 0);
        assertFromBoolean("BooleanToString", new BooleanConverters.BooleanToString(), "true", "false");
        assertToBoolean("IntegerToBoolean", new BooleanConverters.IntegerToBoolean(), 1, 0);
        assertToBoolean("StringToBoolean", new BooleanConverters.StringToBoolean(), "true", "false");
        assertToCollection("BooleanToCollection", Boolean.TRUE);
    }
}
