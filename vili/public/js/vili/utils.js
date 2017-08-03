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

        Utils.post = function (uri, data) {
            var headers = {
                "Csrf-Token": $("#csrfToken").attr("data-token-value")
            };

            return $.ajax({
                type: "POST",
                url: uri,
                headers: headers,
                data: data
            })
                .done(function (result) {
                    window.location.reload();
                })
                .fail(function (xhr, status, error) {
                    console.error("Operation failed with status [" + xhr.status + "] and message [" + error + " / " + xhr.responseText + "]");
                    Utils.prototype.showMessage("error", xhr.responseText, error);
                });
        };

        Utils.prototype.getStatus = function () {
            return $.ajax({
                type: "GET",
                url: "/status"
            });
        };

        Utils.prototype.postMessage = function (data) {
            return Utils.post("/process-message", data);
        };

        Utils.prototype.postClusterAction = function (data) {
            return Utils.post("/process-cluster-action", data);
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

        Utils.closeOverlay = function (e) {
            $(e.currentTarget).closest(".vili-message-overlay").remove();
        };

        Utils.prototype.showMessage = function (type, message, title) {
            $("body")
                .append($("<div></div>", {"class": "vili-message-overlay", "click": Utils.closeOverlay})
                    .append($("<div></div>", {"class": "vili-message-" + type})
                        .append($("<div></div>", {"class": "vili-message-content", "text": message})
                            .prepend($("<div></div>", {"class": "vili-message-title", "text": title || type}))
                        )
                    )
                );
        };

        return new Utils();
    }
);
