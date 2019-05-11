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

package org.apache.ofbiz.common.authentication;

import java.util.Comparator;

import org.apache.ofbiz.common.authentication.api.Authenticator;

/**
 * AuthenticationComparator
 *
 * Used to sort Authenticators by weight
 */
public class AuthenticationComparator implements Comparator<Authenticator> {

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     * <p>
     * The implementor must ensure that {@code sgn(compare(x, y)) == -sgn(compare(y, x))}
     * for all {@code x} and {@code y}.  (This implies that {@code compare(x, y)}
     * must throw an exception if and only if {@code compare(y, x)} throws an exception.)
     * <p>
     * The implementor must also ensure that the relation is transitive:
     * {@code (compare(x, y) > 0) && (compare(y, z) > 0)} implies
     * {@code compare(x, z) > 0}.
     * <p>
     * Finally, the implementer must ensure that {@code compare(x, y) == 0}
     * implies that {@code sgn(compare(x, z)) == sgn(compare(y, z))} for all
     * {@code z}.
     * <p>
     * It is generally the case, but <i>not</i> strictly required that
     * {@code (compare(x, y) == 0) == x.equals(y)}.  Generally speaking,
     * any comparator that violates this condition should clearly indicate
     * this fact.  The recommended language is "Note: this comparator
     * imposes orderings that are inconsistent with equals."
     *
     * @param a1 the first object to be compared.
     * @param a2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     * @throws ClassCastException if the arguments' types prevent them from
     *                            being compared by this Comparator.
     */
    @Override
    public int compare(Authenticator a1, Authenticator a2) {
        int comp = Float.compare(a1.getWeight(), a2.getWeight());
        if (comp != 0) {
            return (int) Math.signum(comp);
        }
        if (!a1.getClass().equals(a2.getClass())) {
            return -1;
        }
        return a1.getClass().getName().compareTo(a2.getClass().getName());
    }
}
