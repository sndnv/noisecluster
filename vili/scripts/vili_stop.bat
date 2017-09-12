@rem Copyright 2017 https://github.com/sndnv
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.

@rem vili stop script
@rem
@rem Example: ./vili_stop.bat

@echo off

if "%VILI_HOME%"=="" set "VILI_HOME=%~dp0\\.."
cd /d %VILI_HOME%

if exist "RUNNING_PID" (
  setlocal EnableDelayedExpansion

  set /p PID=<RUNNING_PID
  echo Stopping process [!PID!]...
  taskkill /PID !PID! /F
  del RUNNING_PID

  endlocal
) else (
  echo PID file [RUNNING_PID] not found
)
