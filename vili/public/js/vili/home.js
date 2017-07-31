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
define(["utils"],
    function (utils) {
        function Home() {
        }

        Home.buildButton = function (nodeName, serviceStates, label, nodeState) {
            return $("<li></li>", {}).append(
                $("<div></div>", {
                    "class": "vili-button-outer",
                    "click": Home.toggleAudio,
                    "data-node-name": nodeName,
                    "data-node-audio-state": serviceStates.audio,
                    "data-node-state": nodeState
                }).append(
                    $("<div></div>", {"class": "vili-button-middle"}).append(
                        $("<div></div>", {"class": "vili-button-inner " + nodeState}).append(
                            $("<div></div>", {"class": "vili-button-label", "text": label})
                        )
                    )
                )
            );
        };

        Home.getClassFromState = function (state) {
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
                return "active"
            }
        };

        Home.toggleAudio = function (e) {
            var nodeName = $(e.currentTarget).attr("data-node-name");
            var audioState = $(e.currentTarget).attr("data-node-audio-state");
            var nodeState = $(e.currentTarget).attr("data-node-state");

            if (nodeState === "transition") {
                console.log("Warn"); //TODO - display warning message
                return;
            }

            var requestedState = "restart";
            switch (audioState) {
                case 'Active':
                    requestedState = "stop";
                    break;
                case 'Stopped':
                    requestedState = "start";
                    break;
                default:
                    console.log("Err"); //TODO - display error message
                    return;
            }

            var requestData = {"target": nodeName, "service": "audio", "action": requestedState};
            console.log(requestData);
            utils.postMessage(requestData).done(function (clickResult) {
                console.log(clickResult);
            });
        };

        Home.prototype.page = function () {
            var nodeContainer = $(".vili-home-node-container");

            utils.getStatus().done(function (result) {
                var localSourceButton = Home.buildButton(
                    "self",
                    result.state.localSource,
                    "src0",
                    Home.getClassFromState(result.state.localSource)
                );

                nodeContainer.empty();
                nodeContainer.append(localSourceButton);

                var targets = result.state.targets;
                for (var target in targets) {
                    nodeContainer.append(
                        Home.buildButton(
                            target,
                            targets[target],
                            target,
                            Home.getClassFromState(targets[target]))
                    );
                }
            });
        };

        return new Home();
    }
);
