#!/bin/bash
#
# Copyright 2017 StreamSets Inc.
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
#

set -e

# We translate environment variables to sdc.properties and rewrite them.
set_conf() {
  if [ $# -ne 2 ]; then
    echo "set_conf requires two arguments: <key> <value>"
    exit 1
  fi

  if [ -z "$SDC_CONF" ]; then
    echo "SDC_CONF is not set."
    exit 1
  fi

  sed -i 's|^#\?\('"$1"'=\).*|\1'"$2"'|' "${SDC_CONF}/sdc.properties"
}

# In some environments such as Marathon $HOST and $PORT0 can be used to
# determine the correct external URL to reach SDC.
if [ ! -z "$HOST" ] && [ ! -z "$PORT0" ] && [ -z "$SDC_CONF_SDC_BASE_HTTP_URL" ]; then
  export SDC_CONF_SDC_BASE_HTTP_URL="http://${HOST}:${PORT0}"
fi

for e in $(env); do
  key=${e%=*}
  value=${e#*=}
  if [[ $key == SDC_CONF_* ]]; then
    lowercase=$(echo $key | tr '[:upper:]' '[:lower:]')
    key=$(echo ${lowercase#*sdc_conf_} | sed 's|_|.|g')
    set_conf $key $value
  fi
done

cp -rf /tmp/streamsets-libs /opt/streamsets-datacollector-3.8.0

# note the sigle quotes: it writes the variable as the value
sed -i 's/-Xmx1024m/-Xmx${OP_STREAMSETS_XMX}/g' "${SDC_DIST}/libexec/sdc-env.sh"
sed -i 's/-Xms1024m/-Xms${OP_STREAMSETS_XMS}/g' "${SDC_DIST}/libexec/sdc-env.sh"

# note the double quotes: it writes the variable value as the value
sed -i "s/runner.thread.pool.size=50/runner.thread.pool.size=${OP_STREAMSETS_POOL_SIZE}/g" "${SDC_CONF}/sdc.properties"

exec "${SDC_DIST}/bin/streamsets" "$@"
