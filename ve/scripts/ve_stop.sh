#!/bin/bash

# Copyright 2017 https://github.com/sndnv
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# ve stop script
#
# Example: ./ve_stop.sh

VE_HOME=${VE_HOME:-$(dirname $(readlink -f $0))}
cd ${VE_HOME}

PID_FILE="$VE_HOME/RUNNING_PID"

if [ -f ${PID_FILE} ]
then
  PID=`cat ${PID_FILE}`
  echo "Stopping process [$PID]..."
  kill ${PID}
  rm ${PID_FILE}
else
  echo "PID file [$PID_FILE] not found"
fi
