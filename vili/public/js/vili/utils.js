/**
 * Copyright 2017 https://github.com/sndnv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define([],
    function () {
        function Utils() {
        }

        Utils.prototype.getStatus = function () {
            return $.ajax({
                type: "GET",
                url: "/status"
            });
        };

        Utils.prototype.postMessage = function (data) {
            var headers = {
                "Csrf-Token": $("#csrfToken").attr("data-token-value")
            };

            return $.ajax({
                type: "POST",
                url: "/process",
                headers: headers,
                data: data
            });
        };

        Utils.prototype.getClassFromState = function (state) {
            var states = Object.values(state);
            var isActive = $.inArray('Active', states) > -1;
            var isStopped = $.inArray('Stopped', states) > -1;
            var isInTransition = (
                $.inArray('Starting', states) > -1
                || $.inArray('Stopping', states) > -1
                || $.inArray('Restarting', states) > -1
            );

            if (isInTransition) {
                return "transition";
            } else if (isStopped) {
                return "inactive";
            } else if (isActive) {
                return "active";
            } else {
                return "";
            }
        };

        return new Utils();
    }
);