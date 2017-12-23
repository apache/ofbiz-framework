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
package org.apache.ofbiz.base.crypto;

import org.apache.commons.codec.binary.Base64;
import org.apache.ofbiz.base.util.Debug;
import org.apache.shiro.crypto.AesCipherService;

public class Main {
    
    public static final String module = Main.class.getName(); 
    public static void main(String[] args) throws Exception {
        if ("-crypt".equals(args[0])) {
            Debug.logInfo(HashCrypt.cryptUTF8(args[1], null, args[2]), module);
        } else if ("-digest".equals(args[0])) {
            String digest = HashCrypt.digestHash("SHA", null, args[1]);
            Debug.logInfo(digest, module);
        } else if ("-kek".equals(args[0])) {
            AesCipherService cs = new AesCipherService();
            Debug.logInfo(Base64.encodeBase64String(cs.generateNewKey().getEncoded()), module);
        } else if ("-kek-old".equals(args[0])) {
            Debug.logInfo(Base64.encodeBase64String(DesCrypt.generateKey().getEncoded()), module);
        }
    }
}
