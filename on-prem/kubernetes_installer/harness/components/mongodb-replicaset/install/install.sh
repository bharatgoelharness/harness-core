#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

# Copyright 2016 The Kubernetes Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This volume is assumed to exist and is shared with the peer-finder
# init container. It contains on-start/change configuration scripts.
WORKDIR_VOLUME="/work-dir"

for i in "$@"; do
    case "$i" in
        -w=*|--work-dir=*)
            WORKDIR_VOLUME="${i#*=}"
            shift
            ;;
        *)
            # unknown option
            ;;
    esac
done

echo Installing config scripts into "${WORKDIR_VOLUME}"

mkdir -p "${WORKDIR_VOLUME}"
cp /peer-finder "${WORKDIR_VOLUME}"/
