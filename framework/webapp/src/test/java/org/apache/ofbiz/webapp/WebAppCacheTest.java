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
package org.apache.ofbiz.webapp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.junit.Before;
import org.junit.Test;

public class WebAppCacheTest {
    private ArrayList<ComponentConfig> ccSource;
    private WebAppCache wac;
    private ArrayList<WebappInfo> wInfos;

    @Before
    public void setUp() {
        ccSource = new ArrayList<>();
        wac = new WebAppCache(() -> ccSource);
        wInfos = new ArrayList<>();
        ccSource.add(new ComponentConfig.Builder().webappInfos(wInfos).create());
    }

    // Checks that `getAppBarWebInfos` call retrieves the expected  `WebappInfo`
    // from the `ComponentConfig` source.
    @Test
    public void getAppBarWebInfosBasic() {
        WebappInfo wInfo0 = new WebappInfo.Builder().server("foo").title("foo").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder().server("bar").title("bar").create();
        wInfos.add(wInfo1);
        WebappInfo wInfo2 = new WebappInfo.Builder().server("baz").title("baz").create();
        wInfos.add(wInfo2);

        assertThat(wac.getAppBarWebInfos("foo"), contains(wInfo0));
        assertThat(wac.getAppBarWebInfos("bar"), contains(wInfo1));
        assertThat(wac.getAppBarWebInfos("baz"), contains(wInfo2));
    }

    // Checks that `getAppBarWebInfos` call retrieves the expected  `WebappInfo`
    // from the `ComponentConfig` source when `menuName` is specified.
    @Test
    public void getAppBarWebInfosBasicWithMenu() {
        WebappInfo wInfo0 = new WebappInfo.Builder().server("foo").title("foo").menuName("a").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder().server("bar").title("bar").menuName("b").create();
        wInfos.add(wInfo1);
        WebappInfo wInfo2 = new WebappInfo.Builder().server("baz").title("baz").menuName("c").create();
        wInfos.add(wInfo2);

        assertThat(wac.getAppBarWebInfos("foo", "a"), contains(wInfo0));
        assertThat(wac.getAppBarWebInfos("foo", "none"), is(empty()));
        assertThat(wac.getAppBarWebInfos("bar", "b"), contains(wInfo1));
        assertThat(wac.getAppBarWebInfos("bar", "none"), is(empty()));
        assertThat(wac.getAppBarWebInfos("baz", "c"), contains(wInfo2));
        assertThat(wac.getAppBarWebInfos("baz", "none"), is(empty()));
    }

    // Checks that once a `getAppBarWebInfos` is called on a server name,
    // modifying the `ComponentConfig` source doesn't impact further calls.
    @Test
    public void getAppBarWebInfosMemoization() {
        assertThat(wac.getAppBarWebInfos("foo"), is(empty()));

        WebappInfo wInfo0 = new WebappInfo.Builder().server("foo").title("foo").create();
        wInfos.add(wInfo0);

        assertThat(wac.getAppBarWebInfos("foo"), is(empty()));
    }

    // Checks that when the same position is provided by the `WebappInfo` instance
    // only one is retrieved
    @Test
    public void getAppBarWebInfosSamePosition() {
        WebappInfo wInfo0 = new WebappInfo.Builder().server("foo").title("foo").position("1").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder().server("foo").title("foo").position("1").create();
        wInfos.add(wInfo1);

        // Ensure that there is a collision between `wInfo0` and `wInfo1`
        // and only one of them are retrieved.
        assertThat(wac.getAppBarWebInfos("foo").size(), is(1));
    }

    // Checks that when the same title with no position is provided by the `WebappInfo` instance
    // 2 instances are retrieved
    @Test
    public void getAppBarWebInfosSameTitle() {
        WebappInfo wInfo0 = new WebappInfo.Builder().server("foo").title("foo").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder().server("foo").title("foo").create();
        wInfos.add(wInfo1);

        // Ensure that there is no collision between `wInfo0` and `wInfo1`
        // though they use the same title only position allows collision
        assertThat(wac.getAppBarWebInfos("foo").size(), is(2));
    }

    // Checks that when a position is provided by the `WebappInfo` instance
    // it is used instead of its title.
    @Test
    public void getAppBarWebInfosPositionTitle() {
        WebappInfo wInfo0 = new WebappInfo.Builder().server("foo").title("foo").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder().server("foo").title("foo").position("14").create();
        wInfos.add(wInfo1);

        // Ensure that there is no collision.
        assertThat(wac.getAppBarWebInfos("foo"), containsInAnyOrder(wInfo0, wInfo1));
    }

    // Checks that when a `appBarDisplay` is false the corresponding `WebappInfo` instance is ignored
    // only when the menu name argument is empty.
    @Test
    public void getAppBarWebInfosDisplayFalse() {
        WebappInfo wInfo0 =
                new WebappInfo.Builder().server("foo").title("bar").appBarDisplay(false).menuName("m").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder().server("foo").title("baz").menuName("m").create();
        wInfos.add(wInfo1);

        assertThat(wac.getAppBarWebInfos("foo", "m"), containsInAnyOrder(wInfo1));
        assertThat(wac.getAppBarWebInfos("foo"), containsInAnyOrder(wInfo0, wInfo1));
    }

    // Checks that `mountPoint` are properly handled.
    @Test
    public void getWebappInfoBasic() {
        WebappInfo wInfo0 = new WebappInfo.Builder()
                .server("foo").position("7").mountPoint("/bar/*").create();
        wInfos.add(wInfo0);
        WebappInfo wInfo1 = new WebappInfo.Builder()
                .server("foo").position("14").mountPoint("/bar/baz/*").create();
        wInfos.add(wInfo1);

        assertThat(wac.getWebappInfo("foo", "bar").get(), is(wInfo0));
        assertThat(wac.getWebappInfo("foo", "barbaz").get(), is(wInfo1));
        assertFalse(wac.getWebappInfo("foo", "bazbaz").isPresent());
    }
}
