#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This script does the following:
# 1. Retrieves gcloud service account token
# 2. Activates gcloud service account
# 3. Connects to google Firebase (using TestArmada's Flank tool)
# 4. Executes UI tests
# 5. Puts test artifacts into the test_artifacts folder

# NOTE:
# Flank supports sharding across multiple devices at a time, but gcloud API
# only supports 1 defined APK per test run.


# If a command fails then do not proceed and fail this script too.
set -e

JAVA_BIN="/usr/bin/java"
FLANK_BIN="/builds/worker/test-tools/flank.jar"
ARTIFACT_DIR="/builds/worker/artifacts"
RESULTS_DIR="${ARTIFACT_DIR}/results"
# WORKDIR="/opt/focus-android"
# PATH_TOOLS="$WORKDIR/taskcluster/scripts"
FLANK_CONF="./taskcluster/scripts/flank.yml"

echo
echo "ACTIVATE SERVICE ACCT"
echo
# this is where the Google Testcloud project ID is set
gcloud config set project "$GOOGLE_PROJECT"
echo

gcloud auth activate-service-account --key-file "$GOOGLE_APPLICATION_CREDENTIALS"
echo
echo

echo
echo "FLANK VERSION"
echo
$JAVA_BIN -jar $FLANK_BIN --version
echo
echo

echo
echo "EXECUTE TEST(S)"
echo
$JAVA_BIN -jar $FLANK_BIN android run --config=$FLANK_CONF
exitcode=$?

# FLANK_BIN="$PATH_TOOLS/flank.jar"
# FLANK_CONF="$PATH_TOOLS/flank.yml"
# WORKDIR="/opt/focus-android"

# echo "home: $HOME"
# export GOOGLE_APPLICATION_CREDENTIALS="$WORKDIR/.firebase_token.json"

# rm  -f TEST_TARGETS
# rm  -f $FLANK_BIN

# echo
# echo "---------------------------------------"
# echo "FLANK FILES"
# echo "---------------------------------------"
# echo "FLANK_CONF: $FLANK_CONF"
# echo "FLANK_BIN: $FLANK_BIN"
# echo

# curl --location --retry 5 --output $FLANK_BIN $URL_FLANK_BIN

# echo
# echo "---------------------------------------"
# echo "FLANK VERSION"
# echo "---------------------------------------"
# chmod +x $FLANK_BIN
# $JAVA_BIN -jar $FLANK_BIN -v
# echo
# echo
# echo "---------------------------------------"

# $JAVA_BIN -jar $FLANK_BIN android run --config=$FLANK_CONF
# exitcode=$?

# cp -r "$WORKDIR/results" "$WORKDIR/test_artifacts"

# # Now exit the script with the exit code from the test run. (Only 0 if all test executions passed)
# exit $exitcode
