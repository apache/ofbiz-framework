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
package org.apache.ofbiz.base.util.collections;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ControllerConfig;
import org.junit.Test;

public class MapContextTest {

    /**
     * A node containing properties and including other nodes.
     *
     * This class is simplification of the Controller configuration objects
     * useful to test {@code MapContext} objects.
     *
     * @see ControllerConfig
     */
    static class PNode {
        /** The properties of the node. */
        public Map<String, String> props;
        /** The included identifier of nodes. */
        private List<PNode> includes;

        /**
         * Constructs a node without properties.
         *
         * @param includes  the included nodes
         */
        @SafeVarargs
        public PNode(PNode... includes) {
            this(Collections.emptyMap(), includes);
        }

        /**
         * Constructs a node with some properties.
         *
         * @param props  the properties of the node
         * @param includes  the included nodes
         */
        @SafeVarargs
        public PNode(Map<String, String> props, PNode... includes) {
            this.props = props;
            this.includes = Arrays.asList(includes);
        }

        /**
         * Combines the properties of included nodes.
         *
         * @return a map context containing the properties of the tree.
         */
        public MapContext<String, String> allProps() {
            MapContext<String, String> res = new MapContext<>();
            includes.forEach(inc -> res.push(inc.allProps()));
            res.push(props);
            return res;
        }
    }

    // Checks that the order warranty of LinkedHashMap objects are preserved
    // when pushing them in a MapContext.
    @Test
    public void ControllerConfigLikeContext() {
        Map<String, String> propsA =
                UtilMisc.toMap(LinkedHashMap::new, "aa", "1", "ab", "1");
        Map<String, String> propsB =
                UtilMisc.toMap(LinkedHashMap::new, "ba", "3", "bb", "8", "bc", "1", "bd", "14");
        PNode pn = new PNode(propsA,
                new PNode(propsB, new PNode(), new PNode()),
                new PNode(new PNode()),
                new PNode());

        MapContext<String, String> mc = pn.allProps();
        assertThat("insertion order of LinkedHashMap is preserved by the 'values' method",
                mc.values(), contains("1", "1", "3", "8", "1", "14"));
    }
}
