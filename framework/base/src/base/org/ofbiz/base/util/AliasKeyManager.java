/*
 * $Id: AliasKeyManager.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.base.util;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.net.Socket;

import javax.net.ssl.X509KeyManager;

/**
 * AliasKeyManager - KeyManager used to specify a certificate alias
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class AliasKeyManager implements X509KeyManager {

    protected X509KeyManager keyManager = null;
    protected String alias = null;

    protected AliasKeyManager() {}

    public AliasKeyManager(X509KeyManager keyManager, String alias) {
        this.keyManager = keyManager;
        this.alias = alias;
    }

    // this is where the customization comes in
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
      for (int i = 0; i < keyType.length; i++) {
          String[] aliases = keyManager.getClientAliases(keyType[i], issuers);
          if (aliases != null && aliases.length > 0) {
              for (int x = 0; x < aliases.length; x++) {
                  if (alias.equals(aliases[i])) {
                      return alias;
                  }
              }
          }
      }
      return null;
    }

    // these just pass through the keyManager
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
      return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    public X509Certificate[] getCertificateChain(String alias) {
      return keyManager.getCertificateChain(alias);
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
      return keyManager.getClientAliases(keyType, issuers);
    }

    public PrivateKey getPrivateKey(String alias) {
      return keyManager.getPrivateKey(alias);
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
      return keyManager.getServerAliases(keyType, issuers);
    }
}
