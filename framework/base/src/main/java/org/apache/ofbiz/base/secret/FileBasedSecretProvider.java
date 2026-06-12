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
package org.apache.ofbiz.base.secret;

import org.apache.ofbiz.base.crypto.ConfigCryptoUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Default {@link SecretProvider} implementation that resolves secrets from
 * {@code framework/base/config/passwords.properties}.
 *
 * <p>This preserves full backward compatibility with the existing
 * {@code jdbc-password-lookup} mechanism in {@code entityengine.xml}.
 * No configuration is required to use this implementation.</p>
 *
 * <p>Values may optionally be stored encrypted, wrapped as {@code ENC(<base64>)},
 * e.g. {@code jdbc-password.mysql-ofbiz=ENC(AbCd123...)}. Encrypted values are
 * decrypted in-memory with {@link ConfigCryptoUtil} using the master key supplied
 * via the {@code OFBIZ_MASTER_KEY} environment variable.</p>
 */
public final class FileBasedSecretProvider implements SecretProvider {

    @Override
    public String getSecret(String key) throws GeneralException {
        String value = UtilProperties.getPropertyValue("passwords", key);
        if (UtilValidate.isEmpty(value)) {
            throw new GeneralException("Secret key '" + key + "' not found in passwords.properties");
        }
        return ConfigCryptoUtil.decryptIfEncrypted(value, key);
    }
}
