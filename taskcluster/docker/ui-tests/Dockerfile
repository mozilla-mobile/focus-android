# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

FROM $DOCKER_IMAGE_PARENT

MAINTAINER Release Engineering <releng@mozilla.com>

VOLUME /builds/worker/checkouts

# Install Google Cloud SDK for using Firebase Test Lab
RUN cd /opt && curl --location --retry 5 --output gcloud.tar.gz https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-331.0.0-linux-x86_64.tar.gz \
  && tar -xvf /opt/gcloud.tar.gz \
  && rm -f gcloud.tar.gz \
  && /opt/google-cloud-sdk/install.sh --quiet \
  && /opt/google-cloud-sdk/bin/gcloud --quiet components update
