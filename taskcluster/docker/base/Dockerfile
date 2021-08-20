# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Inspired by:
# https://hub.docker.com/r/runmymind/docker-android-sdk/~/dockerfile/

FROM ubuntu:18.04

MAINTAINER Release Engineering <releng@mozilla.com>

# Add worker user
RUN mkdir /builds && \
    useradd -d /builds/worker -s /bin/bash -m worker && \
    chown worker:worker /builds/worker && \
    mkdir /builds/worker/artifacts && \
    chown worker:worker /builds/worker/artifacts

WORKDIR /builds/worker/

# -- System -----------------------------------------------------------------------------

ENV CURL='curl --location --retry 5' \
    GRADLE_OPTS='-Xmx4096m -Dorg.gradle.daemon=false' \
    LANG='en_US.UTF-8' \
    TERM='dumb'

RUN apt-get update -qq \
    # We need to install tzdata before all of the other packages. Otherwise it will show an interactive dialog that
    # we cannot navigate while building the Docker image.
    && apt-get install -y tzdata \
    && apt-get install -y openjdk-8-jdk \
                          openjdk-11-jdk \
                          wget \
                          expect \
                          git \
                          curl \
                          python \
                          python-pip \
                          python3 \
                          python3-yaml \
                          locales \
                          unzip \
			  mercurial \
    && apt-get clean

# Today's Fastlane depends on a newer Ruby version than Ubuntu 17.10 has, so since
# fastlane is only used for screenshots (afaik) just skip it.
#RUN gem install fastlane

RUN pip install --upgrade pip
RUN pip install taskcluster

RUN locale-gen en_US.UTF-8

# %include-run-task

ENV SHELL=/bin/bash \
    HOME=/builds/worker \
    PATH="/builds/worker/.local/bin:$PATH"


VOLUME /builds/worker/checkouts
VOLUME /builds/worker/.cache


# run-task expects to run as root
USER root

# # -- Android SDK ------------------------------------------------------------------------

# RUN cd /opt && curl --location --retry 5 --output android-sdk.zip https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip \
#   && unzip -d /opt/android-sdk-linux android-sdk.zip \
#   && rm -f android-sdk.zip

# ENV ANDROID_SDK_HOME /opt/android-sdk-linux
# ENV ANDROID_HOME /opt/android-sdk-linux

# RUN yes | PATH=$JAVA8PATH "${ANDROID_SDK_HOME}/tools/bin/sdkmanager" --licenses

# # -- Project setup ----------------------------------------------------------------------

# WORKDIR /opt

# # Checkout source code
# RUN git clone https://github.com/mozilla-mobile/focus-android.git

# # Build project and run gradle tasks once to pull all dependencies
# WORKDIR /opt/focus-android
# RUN ./gradlew assembleFocusDebug \
#   && ./gradlew testFocusDebugUnitTest \
#   && ./gradlew detekt \
#   && ./gradlew ktlint \
#   && ./gradlew clean

# # -- Post setup -------------------------------------------------------------------------

# # Install taskcluster python library (used by decision tasks)
# # 5.0.0 is still incompatible with taskclusterProxy, meaning no decision task is able
# # to schedule the rest of the Taskcluster tasks. Please upgrade to taskcluster>=5 once
# # https://bugzilla.mozilla.org/show_bug.cgi?id=1460015 is fixed
# RUN pip install 'taskcluster>=4,<5'

# # Install Google Cloud SDK for using Firebase Test Lab
# RUN cd /opt && curl --location --retry 5 --output gcloud.tar.gz https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-331.0.0-linux-x86_64.tar.gz \
#   && tar -xvf /opt/gcloud.tar.gz \
#   && rm -f gcloud.tar.gz \
#   && /opt/google-cloud-sdk/install.sh --quiet \
#   && /opt/google-cloud-sdk/bin/gcloud --quiet components update
