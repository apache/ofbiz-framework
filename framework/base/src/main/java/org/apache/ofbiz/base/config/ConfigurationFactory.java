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
package org.apache.ofbiz.base.config;

import java.io.File;
import java.util.List;

/**
 * A {@link ConfigurationInterface} factory.
 */
public class ConfigurationFactory {

    private static ConfigurationInterface configInstance = null;

    /**
     * Gets {@code configInstance}. Create an empty one if needed.
     *
     * @return the instance of {@code configInstance}
     */
    public static ConfigurationInterface getInstance() {
        return getInstance(List.of());
    }

    /**
     * Gets {@code configInstance}. Create an new one if needed by loading file at {@code configPath}
     *
     * @param configPath the override configuration file
     * @return the instance of {@code configInstance}
     */
    public static ConfigurationInterface getInstance(File configPath) {
        if (configInstance == null) {
            synchronized (ConfigurationFactory.class) {
                configInstance = configPath == null
                        ? new DefaultConfiguration()
                        : new DefaultConfiguration(configPath);
            }
        }
        return configInstance;
    }

    /**
     * Gets {@code configInstance}. Create a new one if needed by loading all files in {@code configPaths}
     *
     * @param configPaths list of override configuration files
     * @return the instance of {@code configInstance}
     */
    public static ConfigurationInterface getInstance(List<File> configPaths) {
        if (configInstance == null) {
            synchronized (ConfigurationFactory.class) {
                configInstance = new DefaultConfiguration(configPaths);
            }
        }
        return configInstance;
    }

    /**
     * @return resets and returns a new empty {@code configInstance}
     */
    public static ConfigurationInterface resetAndGet() {
        return resetAndGet(List.of());
    }

    /**
     * @return resets and returns a new empty {@code configInstance}
     */
    public static ConfigurationInterface resetAndGet(List<File> configPaths) {
        configInstance = null;
        return getInstance(configPaths);
    }

}
