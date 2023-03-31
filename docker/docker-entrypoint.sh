#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
###############################################################################

###############################################################################
# OFBiz initialisation script for use as the entry point in a docker container.
#
# Triggers the loading of data and configuration of various OFBiz properties before
# executing the command given as arguments to the script.
#
#
# Behaviour controlled by environment variables:
#
# OFBIZ_SKIP_INIT
# Any non-empty value will cause this script to skip any initialisation steps.
# Default: <empty>
#
# OFBIZ_ADMIN_USER
# The username of the OFBIZ admin user.
# Default: admin
#
# OFBIZ_ADMIN_PASSWORD
# The password of the OFBIZ admin user.
# Default: ofbiz
#
# OFBIZ_DATA_LOAD
# Determine what type of data loading is required.
# Default: seed
# Values:
# - none: No data loading is performed.
# - seed: Seed data is loaded.
# - demo: Demo data is loaded.
#
# OFBIZ_HOST
# Specify the hostname used to access OFBiz.
# Used to populate the host-headers-allowed property in framework/security/config/security.properties.
# Default: default value of host-headers-allowed from framework/security/config/security.properties.
#
# OFBIZ_CONTENT_URL_PREFIX
# Used to set the content.url.prefix.secure and content.url.prefix.standard properties in
# framework/webapp/config/url.properties.
# Default: <empty>
#
# OFBIZ_ENABLE_AJP_PORT
# Enable the AJP (Apache JServe Protocol) port to allow communication with OFBiz via a reverse proxy.
# Enabled when this environment variable contains a non-empty value.
# Default value: <empty>
#
# OFBIZ_SKIP_DB_DRIVER_DOWNLOAD
# When connecting to databases other than the OFBiz embedded Derby database a suitable driver will be needed.
# This script will attempt to download a suitable driver unless the OFBIZ_SKIP_DB_DRIVER_DOWNLOAD contains a non-empty
# value.
#
# OFBIZ_POSTGRES_HOST
# Sets the name of the PostgreSQL database host.
# If OFBIZ_POSTGRES_HOST is non-empty, then the following OFBIZ_POSTGRES_* environment variables are used to configure
# access to PostgreSQL databases.
# OFBIZ_POSTGRES_OFBIZ_DB           Default: ofbiz
# OFBIZ_POSTGRES_OFBIZ_USER         Default: ofbiz
# OFBIZ_POSTGRES_OFBIZ_PASSWORD     Default: ofbiz
# OFBIZ_POSTGRES_OLAP_DB            Default: ofbizolap
# OFBIZ_POSTGRES_OLAP_USER          Default: ofbizolap
# OFBIZ_POSTGRES_OLAP_PASSWORD      Default: ofbizolap
# OFBIZ_POSTGRES_TENANT_DB          Default: ofbiztenant
# OFBIZ_POSTGRES_TENANT_USER        Default: ofbiztenant
# OFBIZ_POSTGRES_TENANT_PASSWORD    Default: ofbiztenant
#
# OFBIZ_DISABLE_COMPONENTS
# Prevents loading of ofbiz-components.
# Contains a comma separated list of relative paths from the ofbiz sources directory to the ofbiz-component.xml files
# that should be prevented from loading.
# Default: plugins/birt/ofbiz-component.xml
#
# Hooks are executed at the various stages of the initialisation process by executing scripts in the following
# directories. Scripts must be executable and have the .sh extension:
#
# /docker-entrypoint-hooks/before-config-applied.d
# Executed before any changes are applied to the OFBiz configuration files.
#
# /docker-entrypoint-hooks/after-config-applied.d
# Executed after any changes are applied to the OFBiz configuration files.
#
# /docker-entrypoint-hooks/before-data-load.d
# Executed before any data loading is about to be performed. Only executed if data loading is required.
# Example usage would be to alter the data to be loaded.
#
# /docker-entrypoint-hooks/additional-data.d
# Any data files (.xml files) in this directory are loaded after seed/demo data.
#
# /docker-entrypoint-hooks/after-data-load.d
# Executed after any data loading has been performed. Only executed if data loading was required.
#
###############################################################################
set -x
set -e

trap shutdown_ofbiz SIGTERM SIGINT

CONTAINER_STATE_DIR="/ofbiz/runtime/container_state"
CONTAINER_DATA_LOADED="$CONTAINER_STATE_DIR/data_loaded"
CONTAINER_ADMIN_LOADED="$CONTAINER_STATE_DIR/admin_loaded"
CONTAINER_CONFIG_APPLIED="$CONTAINER_STATE_DIR/config_applied"
CONTAINER_DB_CONFIG_APPLIED="$CONTAINER_STATE_DIR/db_config_applied"

POSTGRES_DRIVER_URL="https://jdbc.postgresql.org/download/postgresql-42.5.4.jar"

###############################################################################
# Validate and apply defaults to any environment variables used by this script.
# See script header for environment variable descriptions.
ofbiz_setup_env() {
  case "$OFBIZ_DATA_LOAD" in
  none | seed | demo) ;;
  *)
    OFBIZ_DATA_LOAD="seed"
    ;;
  esac

  OFBIZ_ADMIN_USER=${OFBIZ_ADMIN_USER:-admin}

  OFBIZ_ADMIN_PASSWORD=${OFBIZ_ADMIN_PASSWORD:-ofbiz}

  OFBIZ_POSTGRES_OFBIZ_DB=${OFBIZ_POSTGRES_OFBIZ_DB:-ofbiz}
  OFBIZ_POSTGRES_OFBIZ_USER=${OFBIZ_POSTGRES_OFBIZ_USER:-ofbiz}
  OFBIZ_POSTGRES_OFBIZ_PASSWORD=${OFBIZ_POSTGRES_OFBIZ_PASSWORD:-ofbiz}

  OFBIZ_POSTGRES_OLAP_DB=${OFBIZ_POSTGRES_OLAP_DB:-ofbizolap}
  OFBIZ_POSTGRES_OLAP_USER=${OFBIZ_POSTGRES_OLAP_USER:-ofbizolap}
  OFBIZ_POSTGRES_OLAP_PASSWORD=${OFBIZ_POSTGRES_OLAP_PASSWORD:-ofbizolap}

  OFBIZ_POSTGRES_TENANT_DB=${OFBIZ_POSTGRES_TENANT_DB:-ofbiztenant}
  OFBIZ_POSTGRES_TENANT_USER=${OFBIZ_POSTGRES_TENANT_USER:-ofbiztenant}
  OFBIZ_POSTGRES_TENANT_PASSWORD=${OFBIZ_POSTGRES_TENANT_PASSWORD:-ofbiztenant}

  OFBIZ_DISABLE_COMPONENTS=${OFBIZ_DISABLE_COMPONENTS-plugins/birt/ofbiz-component.xml}
}

###############################################################################
# Create the runtime container state directory used to track which initialisation
# steps have been run for the container.
# This directory should be hosted on a volume that persists for the life of the container.
create_ofbiz_runtime_directories() {
  if [ ! -d "$CONTAINER_STATE_DIR" ]; then
    mkdir --parents "$CONTAINER_STATE_DIR"
  fi
}

###############################################################################
# Execute the shell scripts at the paths passed to this function.
# Args:
# 1:  Name of the hook stage being executed. Used for logging.
# 2+: Variable number of paths to the shell scripts to be executed.
#     Only scripts with the .sh extension are executed.
#     Scripts will be sourced if they are not executable.
run_init_hooks() {
  local hookStage="$1"
  shift
  local filePath
  for filePath; do
    case "$filePath" in
    *.sh)
      if [ -x "$filePath" ]; then
        printf '%s: running %s\n' "$hookStage" "$filePath"
        "$filePath"
      else
        printf '%s: sourcing %s\n' "$hookStage" "$filePath"
        . "$filePath"
      fi
      ;;
    *)
      printf '%s: Not a script. Ignoring %s\n' "$hookStage" "$filePath"
      ;;
    esac
  done
}

###############################################################################
# If required, load data into OFBiz.
load_data() {
  if [ ! -f "$CONTAINER_DATA_LOADED" ]; then
    run_init_hooks before-data-load /docker-entrypoint-hooks/before-data-load.d/*

    case "$OFBIZ_DATA_LOAD" in
    none) ;;

    seed)
      /ofbiz/bin/ofbiz --load-data readers=seed,seed-initial
      ;;

    demo)
      /ofbiz/bin/ofbiz --load-data
      # Demo data includes the admin user so indicate that the user is already loaded.
      touch "$CONTAINER_ADMIN_LOADED"
      ;;
    esac

    # Load any additional data files provided.
    if [ -z $(find /docker-entrypoint-hooks/additional-data.d/ -prune -empty) ]; then
      /ofbiz/bin/ofbiz --load-data dir=/docker-entrypoint-hooks/additional-data.d
    fi

    touch "$CONTAINER_DATA_LOADED"

    run_init_hooks after-data-load /docker-entrypoint-hooks/after-data-load.d/*
  fi
}

###############################################################################
# Create and load the password hash for the admin user.
load_admin_user() {
  if [ ! -f "$CONTAINER_ADMIN_LOADED" ]; then
    TMPFILE=$(mktemp)

    # Concatenate a random salt and the admin password.
    SALT=$(tr --delete --complement A-Za-z0-9 </dev/urandom | head --bytes=16)
    SALT_AND_PASSWORD="${SALT}${OFBIZ_ADMIN_PASSWORD}"

    # Take a SHA-1 hash of the combined salt and password and strip off any additional output form the sha1sum utility.
    SHA1SUM_ASCII_HEX=$(printf "$SALT_AND_PASSWORD" | sha1sum | cut --delimiter=' ' --fields=1 --zero-terminated | tr --delete '\000')

    # Convert the ASCII Hex representation of the hash to raw bytes by inserting escape sequences and running
    # through the printf command. Encode the result as URL base 64 and remove padding.
    SHA1SUM_ESCAPED_STRING=$(printf "$SHA1SUM_ASCII_HEX" | sed -e 's/\(..\)\.\?/\\x\1/g')
    SHA1SUM_BASE64=$(printf "$SHA1SUM_ESCAPED_STRING" | basenc --base64url --wrap=0 | tr --delete '=')

    # Concatenate the hash type, salt and hash as the encoded password value.
    ENCODED_PASSWORD_HASH="\$SHA\$${SALT}\$${SHA1SUM_BASE64}"

    # Populate the login data template
    sed "s/@userLoginId@/$OFBIZ_ADMIN_USER/g; s/currentPassword=\".*\"/currentPassword=\"$ENCODED_PASSWORD_HASH\"/g;" framework/resources/templates/AdminUserLoginData.xml >"$TMPFILE"

    # Load data from the populated template.
    /ofbiz/bin/ofbiz --load-data "file=$TMPFILE"

    rm "$TMPFILE"

    touch "$CONTAINER_ADMIN_LOADED"
  fi
}

###############################################################################
# Modify the given ofbiz-component configuration XML file to set the root
# component's 'enabled' attribute to false.
# $1 - Path to the XML file to be modified.
disable_component() {
  XML_FILE="/ofbiz/$1"
  if [ -f "$XML_FILE" ]; then
    TMPFILE=$(mktemp)

    xsltproc /ofbiz/disable-component.xslt "$XML_FILE" > "$TMPFILE"
    mv "$TMPFILE" "$XML_FILE"
  else
    echo "Cannot find ofbiz-component configuration file. Not disabling component: $XML_FILE"
  fi
}

###############################################################################
# Modify the given ofbiz-component configuration XML files to set their root
# components' 'enabled' attribute to false.
# $1 - Comma separated list of paths to configuration XML files to be modified.
disable_components() {
  COMMA_SEPARATED_PATHS="$1"

  if [ -n "$COMMA_SEPARATED_PATHS" ]; then

    # Split the comma separated paths into separate arguments.
    IFS=,
    set "$COMMA_SEPARATED_PATHS"

    while [ -n "$1" ]; do
      disable_component "$1"
      shift
    done
  fi
}

###############################################################################
# Apply any configuration changes required.
# Changed property files need to be placed in /ofbiz/config so they appear earlier
# in the classpath and override the build-time copies of the properties in ofbiz.jar.
apply_configuration() {
  if [ ! -f "$CONTAINER_CONFIG_APPLIED" ]; then
    run_init_hooks before-config-applied /docker-entrypoint-hooks/before-config-applied.d/*

    if [ -n "$OFBIZ_ENABLE_AJP_PORT" ]; then
      # Configure tomcat to listen for AJP connections on all interfaces within the container.
      sed --in-place \
        '/<property name="ajp-connector" value="connector">/ a <property name="address" value="0.0.0.0"/>' \
        /ofbiz/framework/catalina/ofbiz-component.xml
    fi

    if [ -n "$OFBIZ_HOST" ]; then
      sed "s/host-headers-allowed=.*/host-headers-allowed=${OFBIZ_HOST}/" \
        framework/security/config/security.properties >config/security.properties
    fi

    if [ -n "$OFBIZ_CONTENT_URL_PREFIX" ]; then
      sed \
        --expression="s#content.url.prefix.secure=.*#content.url.prefix.secure=${OFBIZ_CONTENT_URL_PREFIX}#;" \
        --expression="s#content.url.prefix.standard=.*#content.url.prefix.standard=${OFBIZ_CONTENT_URL_PREFIX}#;" \
        framework/webapp/config/url.properties >config/url.properties
    fi

    if [ -n "$OFBIZ_DISABLE_COMPONENTS" ]; then
      disable_component "$OFBIZ_DISABLE_COMPONENTS"
    fi

    touch "$CONTAINER_CONFIG_APPLIED"
    run_init_hooks after-config-applied /docker-entrypoint-hooks/after-config-applied.d/*
  fi
}

###############################################################################
# Set up the connection to the OFBiz database.
configure_database() {
  if [ ! -f "$CONTAINER_DB_CONFIG_APPLIED" ]; then
    if [ -n "$OFBIZ_POSTGRES_HOST" ]; then
      sed \
        --expression="s/@HOST@/$OFBIZ_POSTGRES_HOST/;" \
        --expression="s/@OFBIZ_DB@/$OFBIZ_POSTGRES_OFBIZ_DB/;" \
        --expression="s/@OFBIZ_USERNAME@/$OFBIZ_POSTGRES_OFBIZ_USER/;" \
        --expression="s/@OFBIZ_PASSWORD@/$OFBIZ_POSTGRES_OFBIZ_PASSWORD/;" \
        --expression="s/@OLAP_DB@/$OFBIZ_POSTGRES_OLAP_DB/;" \
        --expression="s/@OLAP_USERNAME@/$OFBIZ_POSTGRES_OLAP_USER/;" \
        --expression="s/@OLAP_PASSWORD@/$OFBIZ_POSTGRES_OLAP_PASSWORD/;" \
        --expression="s/@TENANT_DB@/$OFBIZ_POSTGRES_TENANT_DB/;" \
        --expression="s/@TENANT_USERNAME@/$OFBIZ_POSTGRES_TENANT_USER/;" \
        --expression="s/@TENANT_PASSWORD@/$OFBIZ_POSTGRES_TENANT_PASSWORD/;" \
        templates/postgres-entityengine.xml > config/entityengine.xml

      if [ -z "$OFBIZ_SKIP_DB_DRIVER_DOWNLOAD" ]; then
        echo "Retrieving PostgreSQL driver from $POSTGRES_DRIVER_URL"
        wget --verbose --directory-prefix=lib-extra "$POSTGRES_DRIVER_URL"
      fi
    fi

    touch "$CONTAINER_DB_CONFIG_APPLIED"
  fi
}

###############################################################################
# Send a shutdown signal to OFBiz
shutdown_ofbiz() {
  /ofbiz/send_ofbiz_stop_signal.sh
}

_main() {
  if [ -z "$OFBIZ_SKIP_INIT" ]; then
    ofbiz_setup_env
    create_ofbiz_runtime_directories
    configure_database
    apply_configuration
    load_data
    load_admin_user
  fi

  unset OFBIZ_SKIP_INIT
  unset OFBIZ_ADMIN_USER
  unset OFBIZ_ADMIN_PASSWORD
  unset OFBIZ_DATA_LOAD
  unset OFBIZ_ENABLE_AJP_PORT
  unset OFBIZ_HOST
  unset OFBIZ_CONTENT_URL_PREFIX
  unset OFBIZ_POSTGRES_OFBIZ_DB
  unset OFBIZ_POSTGRES_OFBIZ_USER
  unset OFBIZ_POSTGRES_OFBIZ_PASSWORD
  unset OFBIZ_POSTGRES_OLAP_DB
  unset OFBIZ_POSTGRES_OLAP_USER
  unset OFBIZ_POSTGRES_OLAP_PASSWORD
  unset OFBIZ_POSTGRES_TENANT_DB
  unset OFBIZ_POSTGRES_TENANT_USER
  unset OFBIZ_POSTGRES_TENANT_PASSWORD

  # Continue loading OFBiz.
  exec "$@"
}

_main "$@"
