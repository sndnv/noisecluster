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

        Nodes.prototype.page = function () {
            var mainGrid = $("#vili-nodes-main-grid");
            utils.getStatus().done(function (result) {
                console.log(result);

                var localSourceContainer = $("<div></div>", {
                    "class": "uk-card uk-card-default uk-card-body",
                    html: JSON.stringify(result.state.localSource),
                    click: function () {
                        //TODO
                        utils.postMessage({}).done(function (clickResult) {
                            console.log(clickResult);
                        });
                    }
                });

                mainGrid.html(null);
                mainGrid.append(localSourceContainer);

                var targets = result.state.targets;
                for (var target in targets) {
                    var targetContainer = $("<div></div>", {
                        "class": "uk-card uk-card-default uk-card-body",
                        html: target + " | " + JSON.stringify(targets[target]),
                        click: function () {
                            utils.postMessage({}).done(function (clickResult) {
                                console.log(clickResult);
                            });
                        }
                    });

                    mainGrid.append(targetContainer);
                }
            });
        };

        return new Nodes();
    }
);
