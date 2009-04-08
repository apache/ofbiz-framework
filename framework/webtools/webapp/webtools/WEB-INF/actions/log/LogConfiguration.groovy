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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityUtil;


rootLogger = Logger.getRootLogger();
loggerRepository = rootLogger.getLoggerRepository();

loggerList = [];
for (Enumeration enumeration = loggerRepository.getCurrentLoggers(); enumeration.hasMoreElements();) {
    logger = enumeration.nextElement();

    if (logger.getLevel() != null) {
        loggerMap = [name : logger.getName(), level : logger.getLevel(), additivity : logger.getAdditivity() ? "Y" : "N", logger : logger];
        loggerList.add(loggerMap);
    }
}

Collections.sort(loggerList, [compare: {l1, l2 -> l1.name.compareTo(l2.name)}] as Comparator);

loggerList.add(0, [name : rootLogger.getName(), level : rootLogger.getLevel(), additivity : rootLogger.getAdditivity() ? "Y" : "N", logger : rootLogger]);
context.loggerList = loggerList;

context.defaultLogger = [name : "org.ofbiz.", level : "INFO", additivity : "Y"];
context.activeDebugLevel = [fatal : Debug.fatalOn() ? "Y" : "N",
                            error : Debug.errorOn() ? "Y" : "N",
                            warning : Debug.warningOn() ? "Y" : "N",
                            important : Debug.importantOn() ? "Y" : "N",
                            info : Debug.infoOn() ? "Y" : "N",
                            timing : Debug.timingOn() ? "Y" : "N",
                            verbose : Debug.verboseOn() ? "Y" : "N"];
