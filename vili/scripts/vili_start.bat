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

@rem vili start script
@rem
@rem Example: ./vili_start.bat
@rem Example: ./vili_start.bat -Dplay.server.http.port=9001 -Dplay.server.https.port=9002

@echo off
set "JVM_OPTIONS=%*"

if "%VILI_HOME%"=="" set "VILI_HOME=%~dp0\\.."
cd /d %VILI_HOME%

java %JVM_OPTIONS% -cp "%VILI_HOME%\conf;%VILI_HOME%\lib\*" play.core.server.ProdServerStart
