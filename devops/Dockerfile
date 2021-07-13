# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# docker file for OFBiz (framework + plugins)
# for evaluation and testing purposes

FROM ubuntu:bionic


WORKDIR /root
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_TERSE true

######
# Install some basic Apache OFBiz requirements
# some git repos need ssh-client so do it too
# Adding libffi-dev for all the programming languages
# that take advantage of it.
# Plus some application to use with the cli
######
RUN apt-get -q update && apt-get -q install --no-install-recommends -y \
    apt-transport-https \
    ca-certificates \
    curl \
    dirmngr \
    git \
    libffi-dev \
    locales \
    nano \
    pkg-config \
    rsync \
    wget \
    software-properties-common \
    ssh-client \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

###
# Set the locale
###
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

####
# Install java (first, since we want to dictate what version and form of Java is to be used)
####

####
# OpenJDK 8
####
RUN apt-get -q update && apt-get -q install --no-install-recommends -y openjdk-8-jdk-headless \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64


####
# Exposing default ports
####
EXPOSE 8009 8443

WORKDIR /opt
RUN git clone -q -o asf https://github.com/apache/ofbiz-framework.git ofbiz
RUN git clone -q -o asf https://github.com/apache/ofbiz-plugins.git ofbiz/plugins

WORKDIR /opt/ofbiz
RUN ./gradlew cleanAll loadAll
ENTRYPOINT ./gradlew ofbiz
