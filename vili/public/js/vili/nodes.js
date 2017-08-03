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
        function Nodes() {
        }

        Nodes.sendMessage = function (e) {
            var nodeName = $(e.currentTarget).attr("data-node-name");
            var nodeState = $(e.currentTarget).attr("data-node-state");
            var buttonState = $(e.currentTarget).attr("data-node-button-state");
            var serviceName = $(e.currentTarget).attr("data-node-service-name");
            var requestedAction = $(e.currentTarget).attr("data-node-service-action");

            if (nodeState !== "active" && nodeState !== "inactive") {
                utils.showMessage(
                    "warning",
                    "Cannot perform action while the node is in transition",
                    "Transition in progress"
                );
                return;
            } else if (buttonState !== "active") {
                utils.showMessage(
                    "error",
                    "This action is not valid for the current service state",
                    "Not Available"
                );
                return;
            }

            var requestData = {"target": nodeName, "service": serviceName, "action": requestedAction};
            utils.postMessage(requestData);
        };

        Nodes.buildButton = function (nodeName, nodeState, serviceName, requestedAction, buttonState, icon) {
            return $("<div></div>", {})
                .append($("<div></div>", {
                        "class": "vili-button-large",
                        "click": Nodes.sendMessage,
                        "data-node-name": nodeName,
                        "data-node-state": nodeState,
                        "data-node-button-state": buttonState,
                        "data-node-service-name": serviceName,
                        "data-node-service-action": requestedAction
                    })
                        .append($("<div></div>", {"class": "vili-button-middle"})
                            .append($("<div></div>", {"class": "vili-button-inner " + buttonState})
                                .append($("<div></div>", {"class": "vili-button-label"})
                                    .append($("<span></span>", {"uk-icon": "icon: " + icon + "; ratio: 2"}))
                                )
                            )
                        )
                );
        };

        Nodes.buildSubContainer = function (nodeName, nodeState, serviceName, serviceState) {
            var subContainerState = "";
            var startButtonState = "";
            var stopButtonState = "";
            var restartButtonState = "";

            switch (serviceState) {
                case 'Active':
                    subContainerState = "active";
                    startButtonState = "";
                    stopButtonState = "active";
                    restartButtonState = "active";
                    break;
                case 'Stopped':
                    subContainerState = "inactive";
                    startButtonState = "active";
                    stopButtonState = "";
                    restartButtonState = "active";
                    break;
                default:
                    subContainerState = "transition";
                    break;
            }

            return $("<div></div>", {"class": "vili-sub-container-" + subContainerState})
                .append($("<div></div>", {"class": "vili-sub-header"})
                    .append($("<div></div>", {"text": serviceName}))
                )
                .append($("<div></div>", {"class": "vili-sub-content"})
                    .append($("<div></div>", {"class": "uk-grid uk-flex-center"})
                        .append(Nodes.buildButton(nodeName, nodeState, serviceName, "start", startButtonState, "upload"))
                        .append(Nodes.buildButton(nodeName, nodeState, serviceName, "stop", stopButtonState, "download"))
                        .append(Nodes.buildButton(nodeName, nodeState, serviceName, "restart", restartButtonState, "refresh"))
                    )
                    .append($("<div></div>", {"class": "vili-sub-state"})
                        .append($("<div></div>", {"text": serviceState}))
                    )
                );
        };

        Nodes.buildContainer = function (nodeName, serviceStates) {
            var containerState = "";
            var containerContent;
            if (!jQuery.isEmptyObject(serviceStates)) {
                containerState = utils.getClassFromState(serviceStates);
                containerContent =
                    $("<div></div>", {"class": "vili-container-content"})
                        .append(Nodes.buildSubContainer(nodeName, containerState, "Audio", serviceStates.audio))
                        .append(Nodes.buildSubContainer(nodeName, containerState, "Transport", serviceStates.transport))
                        .append(Nodes.buildSubContainer(nodeName, containerState, "Application", serviceStates.application))
                        .append(Nodes.buildSubContainer(nodeName, containerState, "Host", serviceStates.host));
            } else {
                containerState = "disabled";
                containerContent =
                    $("<div></div>", {"class": "vili-container-content"})
                        .append($("<div></div>", {"text": "Waiting for data..."}));
            }

            var displayName = nodeName;
            if (nodeName === "self") {
                displayName = "src0";
            }

            return $("<div></div>", {"class": "uk-width-1-3"})
                .append($("<div></div>", {"class": "vili-container-" + containerState})
                    .append($("<div></div>", {"class": "vili-container-header"})
                        .append($("<div></div>", {"class": "vili-container-header-left"}))
                        .append($("<div></div>", {"class": "vili-container-header-middle", "text": displayName}))
                        .append($("<div></div>", {"class": "vili-container-header-right"}))
                    )
                    .append(containerContent)
                    .append($("<div></div>", {"class": "vili-container-footer"}))
                );
        };

        Nodes.updateView = function () {
            var nodeContainer = $(".vili-nodes-node-container");

            nodeContainer.on("click", ".vili-sub-header", function (e) {
                var target = $(e.currentTarget);
                target.toggleClass("active");
                target.next().toggle({"duration": 0});
            });

            utils.getStatus()
                .done(function (result) {
                    var localSourceContainer = Nodes.buildContainer(
                        "self",
                        result.state.localSource
                    );

                    nodeContainer.empty();
                    nodeContainer.append(localSourceContainer);

                    var targets = result.state.targets;
                    for (var target in targets) {
                        nodeContainer.append(
                            Nodes.buildContainer(
                                target,
                                targets[target]
                            )
                        );
                    }
                });
        };

        Nodes.prototype.page = function () {
            Nodes.updateView();
        };

        return new Nodes();
    }
);
