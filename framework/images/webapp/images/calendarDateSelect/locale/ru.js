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
Date.weekdays = $w('Пн Вт Ср Чт Пт Сб В�?');
Date.months = $w('Январь Февраль Март �?прель Май Июнь Июль �?вгу�?т Сент�?брь Окт�?брь �?о�?брь Декабрь');

Date.first_day_of_week = 1

_translations = {
  "OK": "OK",
  "Now": "Сейча�?",
  "Today": "Сегодн�?"
}

//load the data format
var dataFormatJs = "format_euro_24hr.js" // Not sure

var e = document.createElement("script");
e.src = "/images/calendarDateSelect/format/" + dataFormatJs;
e.type="text/javascript";
document.getElementsByTagName("head")[0].appendChild(e);
