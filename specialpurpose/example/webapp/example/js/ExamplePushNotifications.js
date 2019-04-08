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

var webSocket;

// Create a new instance of the websocket
webSocket = new WebSocket('wss://' + window.location.host + '/example/ws/pushNotifications');

webSocket.onopen = function(event){
    // Do any operation on open
};

webSocket.onmessage = function(event) {

    // Remove already present notification
    jQuery('.pushNotification').remove();

    // Create notification on the fly, this notification markup can be added in Messages.ftl and its css can be theme based.
    jQuery("body")
        .append(jQuery('<div/>')
        .addClass('pushNotification')
        .css({'position': 'fixed', 'top': '5%', 'right': '2%', 'background': 'lightgrey', 'border-radius': '10px', 'z-index': '9999'}));

    jQuery('.pushNotification')
        .append(jQuery('<a href="javascript:void(0);" class="closeNotification"/>')
        .css({'color': 'black', 'font-size': '15px', 'position': 'fixed', 'right': '2%'}).append('close'));

    jQuery('.pushNotification')
        .append(jQuery('<p/>')
        .addClass('msg')
        .css({'font-size': '15px', 'padding': '30px 20px 30px 20px'}));

    jQuery('.pushNotification').find('.msg').append(event.data);

    // show notification
    jQuery('.pushNotification').fadeIn();

    // Remove notification after 5 seconds
    setTimeout(function() {
        jQuery('.pushNotification').remove();
    }, 5000 );

    // Added observer for close link.
    jQuery('.closeNotification').click(function() {
        jQuery(this).parent('.pushNotification').remove();
    });
};

webSocket.onerror = function(event){
    // Do any operation on error
};
