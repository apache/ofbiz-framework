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
package org.apache.ofbiz.base.util;

import java.util.List;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtilCodecTests {

    @Test
    public void canonicalizeRevealsEscapedXSS() {
        String xssVector = "&lt;script&gtalert(\"XSS vector\");&lt;/script&gt;";
        String canonicalizedXssVector = UtilCodec.canonicalize(xssVector, true, true);
        assertEquals("<script>alert(\"XSS vector\");</script>", canonicalizedXssVector);
    }

    @Test
    public void checkStringForHtmlStrictNoneDetectsXSS() {
        String xssVector = "&lt;script&gtalert(\"XSS vector\");&lt;/script&gt;";
        List<String> errorList = new ArrayList<>();
        String canonicalizedXssVector = UtilCodec.checkStringForHtmlStrictNone("fieldName", xssVector, errorList);
        assertEquals("<script>alert(\"XSS vector\");</script>", canonicalizedXssVector);
        assertEquals(1, errorList.size());
        assertEquals("In field [fieldName] less-than (<) and greater-than (>) symbols are not allowed.", errorList.get(0));
    }
}
