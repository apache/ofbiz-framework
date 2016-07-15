/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 Copyright (c) 2002 by Matt Welsh and The Regents of the University of California. All rights reserved.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR
 CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA
 HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE
 UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.ofbiz.base.metrics;

import java.util.Collection;
import java.util.TreeSet;

import org.ofbiz.base.lang.LockedBy;
import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Element;

/**
 * A {@link org.ofbiz.base.metrics.Metrics} factory.
 */
@ThreadSafe
public final class MetricsFactory {
    private static final UtilCache<String, Metrics> METRICS_CACHE = UtilCache.createUtilCache("base.metrics", 0, 0);
    /**
     * A "do-nothing" <code>Metrics</code> instance.
     */
    public static final Metrics NULL_METRICS = new NullMetrics();

    /**
     * Creates a <code>Metrics</code> instance based on <code>element</code> attributes.
     * If an instance with the same name already exists, it will be returned.
     * <p><strong>Element Attributes</strong>
     * <table border="1">
     * <tr><th>Attribute Name</th><th>Requirements</th><th>Description</th><th>Notes</th></tr>
     * <tr><td>name</td><td>Required</td><td>The metric name.</td><td>&nbsp;</td></tr>
     * <tr><td>estimation-size</td><td>Optional</td><td>Positive integer number of events to include in the metrics calculation.</td><td>Defaults to "100".</td></tr>
     * <tr><td>estimation-time</td><td>Optional</td><td>Positive integer number of milliseconds to include in the metrics calculation.</td><td>Defaults to "1000".</td></tr>
     * <tr><td>smoothing</td><td>Optional</td><td>Smoothing factor - used to smooth the differences between calculations.</td><td>A value of "1" disables smoothing. Defaults to "0.7".</td></tr>
     * <tr><td>threshold</td><td>Optional</td><td>The metric threshold. The meaning of the threshold is determined by client code.</td><td>Defaults to "0.0".</td></tr>
     * </table></p>
     * 
     * @param element The element whose attributes will be used to create the <code>Metrics</code> instance
     * @return A <code>Metrics</code> instance based on <code>element</code> attributes
     * @throws IllegalArgumentException if <code>element</code> is null or if the name attribute is empty
     * @throws NumberFormatException if any of the numeric attribute values are unparsable
     */
    public static Metrics getInstance(Element element) {
        Assert.notNull("element", element);
        String name = element.getAttribute("name");
        Assert.notEmpty("name attribute", name);
        Metrics result = METRICS_CACHE.get(name);
        if (result == null) {
            int estimationSize = UtilProperties.getPropertyAsInteger("serverstats", "metrics.estimation.size", 100); 
            String attributeValue = element.getAttribute("estimation-size");
            if (!attributeValue.isEmpty()) {
                estimationSize = Integer.parseInt(attributeValue);
            }
            long estimationTime = UtilProperties.getPropertyAsLong("serverstats", "metrics.estimation.time", 1000);
            attributeValue = element.getAttribute("estimation-time");
            if (!attributeValue.isEmpty()) {
                estimationTime = Long.parseLong(attributeValue);
            }
            double smoothing = UtilProperties.getPropertyNumber("serverstats", "metrics.smoothing.factor", 0.7);
            attributeValue = element.getAttribute("smoothing");
            if (!attributeValue.isEmpty()) {
                smoothing = Double.parseDouble(attributeValue);
            }
            double threshold = 0.0;
            attributeValue = element.getAttribute("threshold");
            if (!attributeValue.isEmpty()) {
                threshold = Double.parseDouble(attributeValue);
            }
            result = new MetricsImpl(name, estimationSize, estimationTime, smoothing, threshold);
            METRICS_CACHE.putIfAbsent(name, result);
            result = METRICS_CACHE.get(name);
        }
        return result;
    }

    /**
     * Creates a <code>Metrics</code> instance.
     * If an instance with the same name already exists, it will be returned.
     * @param name The metric name.
     * @param estimationSize Positive integer number of events to include in the metrics calculation.
     * @param estimationTime Positive integer number of milliseconds to include in the metrics calculation.
     * @param smoothing Smoothing factor - used to smooth the differences between calculations.
     * @return A <code>Metrics</code> instance
     */
    public static Metrics getInstance(String name, int estimationSize, long estimationTime, double smoothing, double threshold) {
        Assert.notNull("name", name);
        Metrics result = METRICS_CACHE.get(name);
        if (result == null) {
            result = new MetricsImpl(name, estimationSize, estimationTime, smoothing, threshold);
            METRICS_CACHE.putIfAbsent(name, result);
            result = METRICS_CACHE.get(name);
        }
        return result;
    }

    /**
     * Returns an existing <code>Metric</code> instance with the specified name.
     * Returns <code>null</code> if the metric does not exist.
     * @param name The metric name
     */
    public static Metrics getMetric(String name) {
        Assert.notNull("name", name);
        return METRICS_CACHE.get(name);
    }

    /**
     * Returns all <code>Metric</code> instances, sorted by name.
     */
    public static Collection<Metrics> getMetrics() {
        return new TreeSet<Metrics>(METRICS_CACHE.values());
    }

    private static final class MetricsImpl implements Metrics, Comparable<Metrics> {
        @LockedBy("this")
        private int count = 0;
        @LockedBy("this")
        private long lastTime = System.currentTimeMillis();
        @LockedBy("this")
        private double serviceRate = 0.0;
        @LockedBy("this")
        private long totalServiceTime = 0;
        @LockedBy("this")
        private long totalEvents = 0;
        @LockedBy("this")
        private long cumulativeEvents = 0;
        private final String name;
        private final int estimationSize;
        private final long estimationTime;
        private final double smoothing;
        private final double threshold;

        private MetricsImpl(String name, int estimationSize, long estimationTime, double smoothing, double threshold) {
            this.name = name;
            this.estimationSize = estimationSize;
            this.estimationTime = estimationTime;
            this.smoothing = smoothing;
            this.threshold = threshold;
        }

        @Override
        public int compareTo(Metrics other) {
            return this.name.compareTo(other.getName());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                MetricsImpl that = (MetricsImpl) obj;
                return this.name.equals(that.name);
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public synchronized double getServiceRate() {
            return serviceRate;
        }

        @Override
        public double getThreshold() {
            return threshold;
        }

        @Override
        public synchronized long getTotalEvents() {
            return cumulativeEvents;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public synchronized void recordServiceRate(int numEvents, long time) {
            totalEvents += numEvents;
            cumulativeEvents += numEvents;
            totalServiceTime += time;
            count++;
            long curTime = System.currentTimeMillis();
            if ((count == estimationSize) || (curTime - lastTime >= estimationTime)) {
                if (totalEvents == 0) {
                    totalEvents = 1;
                }
                double rate = totalServiceTime / totalEvents;
                serviceRate = (rate * smoothing) + (serviceRate * (1.0 - smoothing));
                count = 0;
                lastTime = curTime;
                totalEvents = totalServiceTime = 0;
            }
        }

        @Override
        public synchronized void reset() {
            serviceRate = 0.0;
            count = 0;
            lastTime = System.currentTimeMillis();
            totalEvents = totalServiceTime = cumulativeEvents = 0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class NullMetrics implements Metrics {

        @Override
        public String getName() {
            return "NULL";
        }

        @Override
        public double getServiceRate() {
            return 0;
        }

        @Override
        public double getThreshold() {
            return 0.0;
        }

        @Override
        public long getTotalEvents() {
            return 0;
        }

        @Override
        public void recordServiceRate(int numEvents, long time) {
        }

        @Override
        public void reset() {
        }
    }

    private MetricsFactory() {}
}
