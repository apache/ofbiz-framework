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
package org.apache.ofbiz.base.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.StringWriter;

import org.apache.ofbiz.base.util.IndentingWriter;
import org.junit.Test;

public class IndentingWriterTests {

    private static void doTest(String label, boolean doSpace, boolean doNewline, String wanted) throws Exception {
        StringWriter sw = new StringWriter();
        IndentingWriter iw;
        if (!doSpace || !doNewline) {
            iw = new IndentingWriter(sw, doSpace, doNewline);
        } else {
            iw = new IndentingWriter(sw);
        }
        iw.write('a');
        iw.push();
        iw.write("b\nm");
        iw.newline();
        iw.write(new char[] {'1', '\n', '2'});
        iw.space();
        iw.write('\n');
        iw.pop();
        iw.write("e");
        iw.close();
        assertEquals(label, wanted, sw.toString());
    }

    @Test
    public void testIndentingWriter() throws Exception {
        StringWriter sw = new StringWriter();
        IndentingWriter iw = IndentingWriter.makeIndentingWriter(sw);
        assertSame("makeIndentingWriter - pass-thru", iw, IndentingWriter.makeIndentingWriter(iw));
        doTest("IndentingWriter doSpace:doNewline", true, true, "ab\n m\n 1\n 2 \n e");
        doTest("IndentingWriter doNewline", false, true, "ab\nm\n1\n2\ne");
        doTest("IndentingWriter doSpace", true, false, "ab\n m 1\n 2 \n e");
        doTest("IndentingWriter", false, false, "ab\nm1\n2\ne");
    }
}
