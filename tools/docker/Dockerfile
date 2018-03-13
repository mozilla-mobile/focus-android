# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Inspired by:
# https://hub.docker.com/r/runmymind/docker-android-sdk/~/dockerfile/

FROM ubuntu:17.10

MAINTAINER Sebastian Kaspari "skaspari@mozilla.com"

# -- System -----------------------------------------------------------------------------

RUN apt-get update -qq

RUN apt-get install -y openjdk-8-jdk \
					   wget \
					   expect \
					   git \
					   curl \
					   ruby \
					   ruby-dev \
					   ruby-build \
					   python \
					   python-pip \
					   optipng \
					   imagemagick \
					   locales \
					   gcc \
					   python-dev \
					   python-setuptools \
					   curl

RUN gem install fastlane

# Needed for verifying cyclic redundancy check (CRC) headers (Google Cloud SDK)
RUN easy_install -U pip
RUN pip install -U crcmod

# Install taskcluster python library (used by decision tasks)
RUN pip install taskcluster

RUN locale-gen en_US.UTF-8

# -- Android SDK ------------------------------------------------------------------------

RUN mkdir -p /opt/android-sdk
WORKDIR /opt

ENV ANDROID_HOME /opt/android-sdk
ENV ANDROID_SDK_HOME /opt/android-sdk
ENV PATH ${PATH}:${ANDROID_SDK_HOME}/tools:${ANDROID_SDK_HOME}/tools/bin:${ANDROID_SDK_HOME}/platform-tools:/opt/tools:${ANDROID_SDK_HOME}/build-tools/27.0.3

RUN curl -L https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip > sdk.zip \
        && unzip sdk.zip -d /opt/android-sdk \
        && rm sdk.zip

RUN mkdir -p /root/.android
RUN touch /root/.android/repositories.cfg

RUN yes | sdkmanager --licenses

RUN sdkmanager --verbose "platform-tools" \
    "platforms;android-27" \
    "build-tools;27.0.3" \
    "extras;android;m2repository" \
    "extras;google;m2repository"

# -- Project setup ----------------------------------------------------------------------

WORKDIR /opt

# Checkout source code
RUN git clone https://github.com/mozilla-mobile/focus-android.git

# Build project and run gradle tasks once to pull all dependencies
WORKDIR /opt/focus-android
RUN ./gradlew --no-daemon assemble testFocusWebviewUniversalDebugUnitTest lint pmd checkstyle findbugs detektCheck ktlint

# -- Post setup -------------------------------------------------------------------------

# Install dependencies needed to run Android2Po
RUN pip install -r tools/l10n/android2po/requirements.txt

# Install Google Cloud SDK for using Firebase Device Lab
RUN curl https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-184.0.0-linux-x86_64.tar.gz --output /gcloud.tar.gz
RUN tar -xvf /gcloud.tar.gz -C /
RUN /google-cloud-sdk/install.sh --quiet
RUN /google-cloud-sdk/bin/gcloud --quiet components update

# -- Cleanup ----------------------------------------------------------------------------

RUN apt-get clean
