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

        Home.toggleAudio = function (e) {
            var nodeName = $(e.currentTarget).attr("data-node-name");
            var audioState = $(e.currentTarget).attr("data-node-audio-state");
            var nodeState = $(e.currentTarget).attr("data-node-state");

            if (nodeState === "transition") {
                utils.showMessage(
                    "warning",
                    "Cannot toggle audio while the node is in transition",
                    "Transition in progress"
                );
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
                    utils.showMessage(
                        "error",
                        "Cannot toggle audio before the node has initialized",
                        "Waiting for node"
                    );
                    return;
            }

            var requestData = {"target": nodeName, "service": "audio", "action": requestedState};
            utils.postMessage(requestData);
        };

        Home.buildButton = function (nodeName, serviceStates, label, nodeState) {
            var buttonClass = nodeState;
            if (nodeName === "self" && nodeState === "active") {
                buttonClass = "important";
            }

            return $("<li></li>", {})
                .append($("<div></div>", {
                        "class": "vili-button-xlarge",
                        "click": Home.toggleAudio,
                        "data-node-name": nodeName,
                        "data-node-audio-state": serviceStates.audio,
                        "data-node-state": nodeState
                    })
                        .append($("<div></div>", {"class": "vili-button-middle"})
                            .append($("<div></div>", {"class": "vili-button-inner " + buttonClass})
                                .append($("<div></div>", {"class": "vili-button-label", "text": label}))
                            )
                        )
                );
        };

        Home.updateView = function () {
            var nodeContainer = $(".vili-home-node-container");

            utils.getStatus()
                .done(function (result) {
                    var localSourceButton = Home.buildButton(
                        "self",
                        result.state.localSource,
                        "src0",
                        utils.getClassFromState(result.state.localSource)
                    );

                    nodeContainer.empty();
                    nodeContainer.append(localSourceButton);

                    var targets = result.state.targets;
                    for (var target in targets) {
                        nodeContainer.append(
                            Home.buildButton(
                                target,
                                targets[target].state,
                                target,
                                utils.getClassFromState(targets[target].state))
                        );
                    }
                })
                .always(function () {
                    setTimeout(Home.updateView, 4000);
                });
        };

        Home.prototype.page = function () {
            Home.updateView();
        };

        return new Home();
    }
);
