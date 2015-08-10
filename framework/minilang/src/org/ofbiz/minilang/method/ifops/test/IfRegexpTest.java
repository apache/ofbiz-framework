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
package org.ofbiz.minilang.method.ifops.test;

import java.util.Locale;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.testtools.OFBizTestCase;

public class IfRegexpTest extends OFBizTestCase {

    private static final String module = IfRegexpTest.class.getName();

    public IfRegexpTest(String name) {
        super(name);
    }

    public void testIfRegexpThreadSafety() throws Exception {
        MyThread t1 = new MyThread(null, dispatcher);
        MyThread t2 = new MyThread(t1, dispatcher);

        t1.start();
        Thread.sleep(1000);
        t2.start();

        try {
            t1.join(15000);
        } catch (InterruptedException e) {
            // if within 15 secs no problem has occurred, assume success
        }

        assertTrue(t1.success);
        assertTrue(t2.success);
    }

    private static class MyThread extends Thread {
        private boolean stopNow = false;
        private boolean success = true;
        private final MyThread friend;
        private final LocalDispatcher dispatcher;

        public MyThread(MyThread friend, LocalDispatcher dispatcher) {
            this.friend = friend;
            this.dispatcher = dispatcher;
        }

        @Override
        public void run() {
            int tryCount = 0;
            while (!stopNow) {
                MethodContext methodContext = new MethodContext(dispatcher.getDispatchContext(), UtilMisc.toMap("locale", Locale.getDefault()), null);
                try {
                    tryCount++;
                    String responseCode = SimpleMethod.runSimpleMethod("component://minilang/script/org/ofbiz/minilang/method/ifops/IfRegexpTests.xml", "testIfRegexp", methodContext);
                    if (!"success".equals(methodContext.getEnv("responseMessage"))) {
                        success = false;
                        Debug.logError("ResponseCode not success: [" + responseCode + "], tryCount: [" + tryCount + "], envMap: [" + methodContext.getEnvMap() + "]", module);
                        if (friend != null) friend.stopNow = true;
                        break;
                    }
                } catch (MiniLangException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
