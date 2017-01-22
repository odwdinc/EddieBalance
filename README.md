Android Things EddieBalance
=====================================

Eddie the Balance Bot is a self blanacing robot based on the Intel Edison and Sparkfun Blocks.


This project now reflects [r3n33 EddiePlus design](https://github.com/r3n33/EddieBalance)  which incorporates encoders and some new body stylings. The new 3D models and details including a builder's guide are available on thingiverse: www.thingiverse.com/thing:694969

EddieBalance is currently running the Android Things SDK Preview image from Google and requires the installation of libmraa from Intel (instructions to come in due time).


Pre-requisites
--------------

- Android Things Intel Edison and Sparkfun Blocks
- Android Studio 2.2+


Build and install
=================

On Android Studio, click on the "Run" button.

If you prefer to run on the command line, type

```bash
./gradlew installDebug
adb shell am start com.example.androidthings.myproject/.MainActivity
```

License
-------

Copyright 2016 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
