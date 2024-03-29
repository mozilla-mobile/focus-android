# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

FROM $DOCKER_IMAGE_PARENT

LABEL authors="Richard Pappalardo <rpappalax@gmail.com>, Aaron Train <atrain@mozilla.com>"
LABEL maintainer="Richard Pappalardo <rpappalax@gmail.com>"

#----------------------------------------------------------------------------------------------------------------------
#-- Test tools --------------------------------------------------------------------------------------------------------
#----------------------------------------------------------------------------------------------------------------------

RUN apt-get install -y jq \
    && apt-get clean

USER worker:worker

ENV GOOGLE_SDK_DOWNLOAD ./gcloud.tar.gz
ENV GOOGLE_SDK_VERSION 407

ENV TEST_TOOLS /builds/worker/test-tools
ENV PATH ${PATH}:${TEST_TOOLS}:${TEST_TOOLS}/google-cloud-sdk/bin

RUN mkdir -p ${TEST_TOOLS} && \
    mkdir -p ${HOME}/.config/gcloud

RUN curl https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${GOOGLE_SDK_VERSION}.0.0-linux-x86_64.tar.gz --output ${GOOGLE_SDK_DOWNLOAD} \
    && tar -xvf ${GOOGLE_SDK_DOWNLOAD} -C ${TEST_TOOLS} \
    && rm -f ${GOOGLE_SDK_DOWNLOAD} \
    && ${TEST_TOOLS}/google-cloud-sdk/install.sh --quiet \
    && ${TEST_TOOLS}/google-cloud-sdk/bin/gcloud --quiet components update

# Flank v21.08.1

RUN URL_FLANK_BIN="$($CURL --silent 'https://api.github.com/repos/Flank/flank/releases/48276753' | jq -r '.assets[] | select(.browser_download_url | test("flank.jar")) .browser_download_url')" \
    && $CURL --output "${TEST_TOOLS}/flank.jar" "${URL_FLANK_BIN}" \
    && chmod +x "${TEST_TOOLS}/flank.jar"

# run-task expects to run as root
USER root



# FROM $DOCKER_IMAGE_PARENT

# MAINTAINER Release Engineering <releng@mozilla.com>

# VOLUME /builds/worker/checkouts

# # Install Google Cloud SDK for using Firebase Test Lab
# RUN cd /opt && curl --location --retry 5 --output gcloud.tar.gz https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-331.0.0-linux-x86_64.tar.gz \
#   && tar -xvf /opt/gcloud.tar.gz \
#   && rm -f gcloud.tar.gz \
#   && /opt/google-cloud-sdk/install.sh --quiet \
#   && /opt/google-cloud-sdk/bin/gcloud --quiet components update
