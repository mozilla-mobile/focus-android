# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script is executed on taskcluster whenever we want to release builds
to Google Play. It will schedule the taskcluster tasks for building,
signing and uploading a release.
"""

import argparse
import arrow
import json
import os
import taskcluster
import lib.tasks

TASK_ID = os.environ.get('TASK_ID')
SCHEDULER_ID = os.environ.get('SCHEDULER_ID')
GITHUB_HTTP_REPOSITORY = os.environ.get('MOBILE_HEAD_REPOSITORY')
HEAD_REV = os.environ.get('MOBILE_HEAD_REV')

BUILDER = lib.tasks.TaskBuilder(
    task_id=TASK_ID,
    repo_url=os.environ.get('MOBILE_HEAD_REPOSITORY'),
    branch=os.environ.get('MOBILE_HEAD_BRANCH'),
    commit=HEAD_REV,
    owner="skaspari@mozilla.com",
    source='{}/raw/{}/.taskcluster.yml'.format(GITHUB_HTTP_REPOSITORY, HEAD_REV),
    scheduler_id=SCHEDULER_ID,
)


def generate_build_task(apks, tag, is_staging):
    artifacts = {}
    for apk in apks:
        artifact = {
            "type": 'file',
            "path": apk,
            "expires": taskcluster.stringDate(taskcluster.fromNow('1 year'))
        }
        artifacts["public/%s" % os.path.basename(apk)] = artifact

    checkout = "git clone {} repository && cd repository".format(GITHUB_HTTP_REPOSITORY)
    if tag is not None:
        checkout += " && git checkout {}".format(tag)
    assemble_task = 'assembleNightly'

    if tag:
        # Non-tagged (nightly) builds should contain all languages
        checkout = checkout + ' && python tools/l10n/filter-release-translations.py'
        assemble_task = 'assembleRelease'

    return taskcluster.slugId(), BUILDER.build_task(
        name="(Focus for Android) Build task",
        description="Build Focus/Klar from source code.",
        command=(checkout +
                 ' && python tools/taskcluster/get-adjust-token.py --staging'
                 ' && python tools/taskcluster/get-sentry-token.py --staging'
                 ' && ./gradlew --no-daemon clean test ' + assemble_task),
        features = {
            "chainOfTrust": True
        },
        artifacts = artifacts,
        worker_type='gecko-focus',
        scopes=[
            "secrets:get:garbage/staging/project/focus/tokens" if is_staging else "secrets:get:project/focus/tokens"
        ])


def generate_signing_task(build_task_id, apks, date, index_release, is_staging):
    artifacts = []
    for apk in apks:
        artifacts.append("public/" + os.path.basename(apk))

    routes = [
        "index.project.mobile.focus.{}.nightly.{}.{}.{}.latest".format(index_release, date.year, date.month, date.day),
        "index.project.mobile.focus.{}.nightly.{}.{}.{}.revision.{}".format(index_release, date.year, date.month, date.day, HEAD_REV),
        "index.project.mobile.focus.{}.nightly.latest".format(index_release),
    ]

    signing_format = "autograph_focus"
    scopes = [
        "project:mobile:focus:releng:signing:cert:{}".format("dep-signing" if is_staging else "release-signing"),
        "project:mobile:focus:releng:signing:format:{}".format(signing_format),
    ]

    return taskcluster.slugId(), BUILDER.build_signing_task(
        build_task_id,
        name="(Focus for Android) Signing task",
        description="Sign release builds of Focus/Klar",
        signing_format=signing_format,
        apks=artifacts,
        scopes=scopes,
        routes=routes,
        is_staging=is_staging,
    )


def generate_push_task(signing_task_id, apks, track, commit, is_staging):
    artifacts = []
    for apk in apks:
        artifacts.append("public/" + os.path.basename(apk))

    print artifacts

    return taskcluster.slugId(), BUILDER.build_push_task(
        signing_task_id,
        name="(Focus for Android) Push task",
        description="Upload signed release builds of Focus/Klar to Google Play",
        apks=artifacts,
        scopes=[
            "project:mobile:focus:releng:googleplay:product:focus{}".format(':dep' if is_staging else '')
        ],
        track=track,
        commit=commit,
        is_staging=is_staging,
    )

def populate_chain_of_trust_required_but_unused_files():
    # Thoses files are needed to keep chainOfTrust happy. However, they have no need for Firefox
    # Focus, at the moment. For more details, see:
    # https://github.com/mozilla-releng/scriptworker/pull/209/files#r184180585

    for file_names in ('actions.json', 'parameters.yml'):
        with open(file_names, 'w') as f:
            json.dump({}, f)    # Yaml is a super-set of JSON.


def release(apks, track, commit, tag, date_string):
    queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })
    date = arrow.get(date_string)
    is_staging = track == 'staging-nightly'
    index_release = {
        'staging-nightly': 'staging-signed-nightly',
        'nightly': 'signed-nightly',
    }.get(track, 'release')

    task_graph = {}

    build_task_id, build_task = generate_build_task(apks, tag, is_staging)
    lib.tasks.schedule_task(queue, build_task_id, build_task)

    task_graph[build_task_id] = {}
    task_graph[build_task_id]["task"] = queue.task(build_task_id)

    sign_task_id, sign_task = generate_signing_task(build_task_id, apks, date, index_release, is_staging)
    lib.tasks.schedule_task(queue, sign_task_id, sign_task)

    task_graph[sign_task_id] = {}
    task_graph[sign_task_id]["task"] = queue.task(sign_task_id)

    push_task_id, push_task = generate_push_task(sign_task_id, apks, track, commit, is_staging)
    lib.tasks.schedule_task(queue, push_task_id, push_task)

    task_graph[push_task_id] = {}
    task_graph[push_task_id]["task"] = queue.task(push_task_id)

    print json.dumps(task_graph, indent=4, separators=(',', ': '))

    task_graph_path = "task-graph.json"
    with open(task_graph_path, 'w') as f:
        json.dump(task_graph, f)

    populate_chain_of_trust_required_but_unused_files()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Create a release pipeline (build, sign, publish) on taskcluster.')

    parser.add_argument('--track', dest="track", action="store", choices=['internal', 'alpha', 'nightly',
                                                                          'staging-nightly'], help="", required=True)
    parser.add_argument('--commit', dest="commit", action="store_true", help="commit the google play transaction")
    parser.add_argument('--tag', dest="tag", action="store", help="git tag to build from")
    parser.add_argument('--apk', dest="apks", metavar="path", action="append", help="Path to APKs to sign and upload", required=True)
    parser.add_argument('--output', dest="output", metavar="path", action="store", help="Path to the build output", required=True)
    parser.add_argument('--date', dest="date", action="store", help="ISO8601 timestamp for build")

    result = parser.parse_args()

    apks = map(lambda x: result.output + '/' + x, result.apks)

    release(apks, result.track, result.commit, result.tag, result.date)
