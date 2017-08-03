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
        function Cluster() {
        }

        Cluster.unlockClusterActions = function(e) {
            var target = $(e.currentTarget).parent();
            target.parent().find(".vili-button-container").removeClass("hidden");
            target.addClass("hidden");
        };

        Cluster.triggerClusterAction = function (nodeName, action) {
            return function (e) {
                //TODO
                console.log(action + " -- " + nodeName);
            }
        };

        Cluster.buildUnlockButton = function () {
            return $("<div></div>", {"class": "vili-button-container"})
                .append($("<div></div>", { "class": "vili-button", "click": Cluster.unlockClusterActions})
                    .append($("<div></div>", {"class": "vili-button-middle"})
                        .append($("<div></div>", {"class": "vili-button-inner inactive"})
                            .append($("<div></div>", {"class": "vili-button-label"})
                                .append($("<span></span>", {"uk-icon": "icon: unlock;"}))
                            )
                        )
                    )
                );
        };

        Cluster.buildActionButton = function (nodeName, action) {
            return $("<div></div>", {"class": "vili-button-container hidden"})
                .append($("<div></div>", { "class": "vili-button", "click": Cluster.triggerClusterAction(nodeName, action.toLowerCase())})
                    .append($("<div></div>", {"class": "vili-button-middle"})
                        .append($("<div></div>", {"class": "vili-button-inner inactive"})
                            .append($("<div></div>", {"class": "vili-button-label", "text": action}))
                        )
                    )
                );
        };

        Cluster.buildMemberRow = function (name, address, roles, status, isLeader, isLocal) {
            var rolesCell = $("<th></th>");
            roles.forEach(function (role) {
                rolesCell.append($("<div></div>", {"class": "vili-role-label", "text": role}));
            });

            var buttonsCell = $("<td></td>", {"class": "vili-button-cell"})
                .append(Cluster.buildUnlockButton())
                .append(Cluster.buildActionButton(name, "Down"))
                .append(Cluster.buildActionButton(name, "Leave"));


            var statusCell = $("<th></th>").append($("<div></div>", {"class": "vili-status-label " + status.toLowerCase(), "text": status}));
            if(isLeader) {
                statusCell.append($("<div></div>", {"class": "vili-status-label leader", "text": "Leader"}));
            }

            if(isLocal) {
                statusCell.append($("<div></div>", {"class": "vili-status-label local", "text": "Local"}));
            }

            return $("<tr></tr>", {})
                .append($("<th></th>", {"text": name}))
                .append($("<th></th>", {"class": "uk-text-meta", "text": address}))
                .append(rolesCell)
                .append(statusCell)
                .append(buttonsCell);
        };

        Cluster.prototype.page = function () {
            var membersTable = $(".vili-cluster-members-table").find("tbody");

            utils.getStatus().done(function (result) {
                console.log(result);
                var memberByAddress = {};
                memberByAddress[result.state.localAddress] = "src0";
                for(var target in result.state.targetAddresses){
                    memberByAddress[result.state.targetAddresses[target]] = target;
                }

                membersTable.empty();

                result.state.members.forEach(function (member) {
                    var memberName = memberByAddress[member.address] || "unknown";

                    membersTable.append(
                        Cluster.buildMemberRow(
                            memberName,
                            member.address,
                            member.roles,
                            member.status,
                            result.state.leaderAddress === member.address,
                            result.state.localAddress === member.address
                        )
                    )
                });
            });
        };

        return new Cluster();
    }
);
