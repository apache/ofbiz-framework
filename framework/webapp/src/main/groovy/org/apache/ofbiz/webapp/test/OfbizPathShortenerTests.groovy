/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
package org.apache.ofbiz.webapp.test

import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.webapp.OfbizPathShortener

class OfbizPathShortenerTests extends OFBizTestCase {

    OfbizPathShortenerTests(String name) {
        super(name)
    }
    void testComputeLongUrlToShortUrl() {
        String longUri = 'passwordChange?USERNAME=admin&TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxML' +
                'LL.eyJ1c2VyTG9naW5JZCI6Imx1Y2lsZS5wZWxsZXRpZXJAZWRsbi5vcmciLCJpc3MiOiJBcGFjaGVPRkJpeiIsImV4cCI6MTcyNTU' +
                '0MjM0OSwiaWF0IjoxNzI1NTQwNTQLLL.Rycl_L-u4ZeWkx82pWWGu7gycfsHQxIxE8zu1nQ5oueGDBeOXALL-SJzMuvSARbpxCwF9A' +
                'jl4rTxgoEYuRMoHg&JavaScriptEnabled=Y&Albert=Yoda'
        assert OfbizPathShortener.resolveShortenedPath(this.getDelegator(), longUri).length() < 11
    }
    void testResolveLongUrlComputedFromShort() {
        String longUri = 'passwordChange?USERNAME=admin&TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxML' +
                'LL.eyJ1c2VyTG9naW5JZCI6Imx1Y2lsZS5wZWxsZXRpZXJAZWRsbi5vcmciLCJpc3MiOiJBcGFjaGVPRkJpeiIsImV4cCI6MTcyNTU' +
                '0MjM0OSwiaWF0IjoxNzI1NTQwNTQLLL.Rycl_L-u4ZeWkx82pWWGu7gycfsHQxIxE8zu1nQ5oueGDBeOXALL-SJzMuvSARbpxCwF9A' +
                'jl4rTxgoEYuRMoHg&JavaScriptEnabled=Y'
        String shortUri = OfbizPathShortener.resolveShortenedPath(this.getDelegator(), longUri)
        assert longUri == OfbizPathShortener.resolveOriginalPathFromShortened(this.getDelegator(), shortUri)
    }
    void testResolveLongUrlComputedFromShortAlreadyStored() {
        String longUri = 'passwordChange?USERNAME=admin&TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxML' +
                'LL.eyJ1c2VyTG9naW5JZCI6Imx1Y2lsZS5wZWxsZXRpZXJAZWRsbi5vcmciLCJpc3MiOiJBcGFjaGVPRkJpeiIsImV4cCI6MTcyNTU' +
                '0MjM0OSwiaWF0IjoxNzI1NTQwNTQLLL.Rycl_L-u4ZeWkx82pWWGu7gycfsHQxIxE8zu1nQ5oueGDBeOXALL-SJzMuvSARbpxCwF9A' +
                'jl4rTxgoEYuRMoHg&JavaScriptEnabled=Y&And=Again'
        String shortUriFirst = OfbizPathShortener.resolveShortenedPath(this.getDelegator(),longUri)
        String shortUriSecond = OfbizPathShortener.resolveShortenedPath(this.getDelegator(),longUri)
        assert shortUriSecond == shortUriFirst
    }

}
