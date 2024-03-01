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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A basic directed graph utilitary.
 * <p>
 * A directed graph is a data structure consisting of nodes and arrows connecting those nodes
 * which are called <em>edges</em>. In a directed graph edges are ordered pairs of respectively
 * source and target nodes.
 * <p>
 * This implementation is adapted to small in-memory graphs.
 *
 * @param <T> the type of the nodes
 * @see <a href="https://www.wikipedia.org/wiki/Directed_graph">Directed graph</a>
 */
public class Digraph<T> {
    /** The map associating source nodes to their adjacent target nodes. */
    private final Map<T, Collection<T>> edges;
    /** The set of nodes */
    private final Set<T> nodes;

    /**
     * Constructs a directed graph from a specification Map.
     * @param spec  the map defining a set of source nodes (keys) that are linked to a collection
     *              of adjacent target nodes (values). Both keys and values must not be {@code null}.
     * @throws IllegalArgumentException when a target node is not present in the sources nodes.
     */
    public Digraph(Map<T, Collection<T>> spec) throws IllegalArgumentException {
        this.edges = spec;
        this.nodes = spec.keySet();
        // Check that all adjacent nodes are present as keys in the map.
        Set<T> undeclaredNodes = spec.values().stream()
                .flatMap(Collection::stream)
                .filter(child -> !nodes.contains(child))
                .collect(toSet());
        if (!undeclaredNodes.isEmpty()) {
            String msg = String.format("%s nodes are not present in the graph", undeclaredNodes);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Sort nodes in a topological ordering assuming that this graph is acyclic.
     * <p>
     * A graph without cycles is often called a <em>Directed Acyclic Graph</em> (DAG).
     * @return a linear ordering of nodes such for every edge in the graph its target node
     *         is present before its source node.
     * @throws IllegalStateException when this graph contains a cycle.
     * @see <a href="https://www.wikipedia.org/wiki/Topological_sorting">topological sorting</a>
     * @see <a href="https://www.wikipedia.org/wiki/Directed_acyclic_graph">DAG</a>
     */
    public List<T> sort() throws IllegalStateException {
        Set<T> permanents = new HashSet<>();
        Set<T> temporaries = new HashSet<>();
        List<T> result = new ArrayList<>();
        for (T node : nodes) {
            if (!permanents.contains(node)) {
                visit(result, node, permanents, temporaries);
            }
        }
        return result;
    }

    /**
     * Traverses the graph using <em>Depth First Search</em> (DFS) to construct a topological ordering.
     * @param res  the ordered list that we are building
     * @param root  the current node we are visiting
     * @param permanents  the nodes that have been successfully been visited
     * @param temporaries  the nodes that we have started to visit but might contain cycles.
     * @throws IllegalStateException when a cycle is found.
     * @see #sort
     * @see <a href="https://www.wikipedia.org/wiki/Depth-first_search">Depth Dirst Search</a>
     */
    private void visit(List<T> res, T root, Set<T> permanents, Set<T> temporaries) throws IllegalStateException {
        if (permanents.contains(root)) {
            return;
        } else if (temporaries.contains(root)) {
            throw new IllegalStateException("A cycle has been found");
        } else {
            temporaries.add(root);
            for (T next : edges.get(root)) {
                visit(res, next, permanents, temporaries);
            }
            temporaries.remove(root);
            permanents.add(root);
            res.add(root);
        }
    }
}
