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
package org.apache.ofbiz.base.collections;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.ofbiz.base.util.collections.MultivaluedMapContext;
import org.junit.Before;
import org.junit.Test;

public class MultivaluedMapContextTests {
    private MultivaluedMapContext<String, Integer> m;

    @Before
    public void setUp() throws Exception {
        m = new MultivaluedMapContext<>();
    }

    @Test
    public void getEmpty() {
        assertThat(m.get("foo"), is(nullValue()));
    }

    @Test
    public void putSingleBasic() {
        m.putSingle("foo", 0);
        assertThat(m.get("foo"), contains(0));
        m.putSingle("foo", 1);
        assertThat(m.get("foo"), contains(1));
    }

    @Test
    public void addBasic() {
        m.add("foo", 0);
        assertThat(m.get("foo"), contains(0));
        m.add("foo", 1);
        assertThat(m.get("foo"), contains(0, 1));
    }

    @Test
    public void addWithPreviousContext() {
        m.add("foo", 0);
        m.push();
        assertThat(m.get("foo"), contains(0));
        m.add("foo", 1);
        assertThat(m.get("foo"), contains(0, 1));
    }

    @Test
    public void getFirstBasic() {
        m.add("foo", 0);
        m.add("foo", 1);
        assertThat(m.getFirst("foo"), is(0));
    }

    @Test
    public void getFirstEmpty() {
        assertThat(m.getFirst("foo"), is(nullValue()));
    }
}
