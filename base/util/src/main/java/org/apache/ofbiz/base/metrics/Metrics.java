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

/*
 Copyright (c) 2002 by Matt Welsh and The Regents of the University of California. All rights reserved.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR
 CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA
 HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE
 UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.apache.ofbiz.base.metrics;


/**
 * An object that tracks service metrics.
 * <p>This interface and its default implementation are based on the
 * <code>seda.sandstorm.internal.StageStats</code> class written by
 * Matt Welsh.
 * @see <a href="http://www.eecs.harvard.edu/~mdw/proj/seda/">SEDA</a>
 */
public interface Metrics {
    /**
     * Returns the name of the metric.
     */
    String getName();

    /**
     * Returns a moving average of the service rate in milliseconds. The default
     * implementation divides the total time by the total number of events and then
     * applies a smoothing factor.
     */
    double getServiceRate();

    /** Returns the metric threshold. The meaning of the threshold is
     * determined by client code.
     * <p>The idea is for client code to compare {@link #getServiceRate()} to
     * the threshold and perform some action based on the comparison.</p>
     */
    double getThreshold();

    /** Returns the total number of processed events. */
    long getTotalEvents();

    /**
     * Records the service time for <code>numEvents</code> taking
     * <code>time</code> milliseconds to be processed.
     */
    void recordServiceRate(int numEvents, long time);

    /** Resets all metrics. */
    void reset();
}
