# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script will be executed whenever a change is pushed to the
master branch. It will schedule multiple child tasks that build
the app, run tests and execute code quality tools:
"""

import datetime
import json
import taskcluster
import os

from helper.tasks import generate_task

COMMIT = os.environ.get('GITHUB_HEAD_SHA')

def generate_build_task():
	return taskcluster.slugId(), generate_task(
		name = "(Focus for Android) Build",
		description = "Build Focus/Klar for Android from source code.",
		command = ('echo "--" > .adjust_token'
				   ' && python tools/l10n/check_translations.py'
				   ' && ./gradlew --no-daemon clean assemble'))


def generate_unit_test_task(buildTaskId):
	return taskcluster.slugId(), generate_task(
		name = "(Focus for Android) Unit tests",
		description = "Run unit tests for Focus/Klar for Android.",
		command = 'echo "--" > .adjust_token && ./gradlew --no-daemon clean test',
		dependencies = [ buildTaskId ])


def generate_code_quality_task(buildTaskId):
	return taskcluster.slugId(), generate_task(
		name = "(Focus for Android) Code quality",
		description = "Run code quality tools on Focus/Klar for Android code base.",
		command = 'echo "--" > .adjust_token && ./gradlew --no-daemon clean detektCheck ktlint lint pmd checkstyle findbugs',
		dependencies = [ buildTaskId ])


def generate_ui_test_task(dependencies):
	return taskcluster.slugId(), generate_task(
		name = "(Focus for Android) UI tests",
		description = "Run UI tests for Focus/Klar for Android.",
		command = ('echo "--" > .adjust_token'
			' && ./gradlew --no-daemon clean assembleFocusWebviewDebug assembleFocusWebviewDebugAndroidTest'
			' && tools/taskcluster/execute-firebase-test.sh'),
		dependencies = dependencies,
		scopes = [ 'secrets:get:project/focus/firebase' ],
		artifacts = {
			"public": {
				"type": "directory",
				"path": "/opt/focus-android/test_artifacts",
				"expires": taskcluster.stringDate(taskcluster.fromNow('1 week'))
			}
		})


def generate_release_task(uiTestTaskId):
	return taskcluster.slugId(), generate_task(
		name = "(Focus for Android) Preview release",
		description = "Build preview versions for testing Focus/Klar for Android.",
		command = ('echo "--" > .adjust_token'
			       ' && ./gradlew --no-daemon clean assembleBeta'
			       ' && python tools/taskcluster/sign-preview-builds.py'
			       ' && touch /opt/focus-android/builds/`date +"%Y-%m-%d-%H-%M"`'
			       ' && touch /opt/focus-android/builds/' + COMMIT),
		dependencies = [ uiTestTaskId ],
		scopes = [
			"secrets:get:project/focus/preview-key-store",
			"queue:route:index.project.focus.android.preview-builds"],
		routes = [ "index.project.focus.android.preview-builds" ],
		artifacts = {
			"public": {
				"type": "directory",
				"path": "/opt/focus-android/builds",
				"expires": taskcluster.stringDate(taskcluster.fromNow('1 week'))
			}
		})

def schedule_task(queue, taskId, task):
	print "TASK", taskId
	print json.dumps(task, indent=4, separators=(',', ': '))

	result = queue.createTask(taskId, task)
	print json.dumps(result)


if __name__ == "__main__":
	queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })

	buildTaskId, buildTask = generate_build_task()
	schedule_task(queue, buildTaskId, buildTask)

	unitTestTaskId, unitTestTask = generate_unit_test_task(buildTaskId)
	schedule_task(queue, unitTestTaskId, unitTestTask)

	codeQualityTaskId, codeQualityTask = generate_code_quality_task(buildTaskId)
	schedule_task(queue, codeQualityTaskId, codeQualityTask)

	uiTestTaskId, uiTestTask = generate_ui_test_task([unitTestTaskId, codeQualityTaskId])
	schedule_task(queue, uiTestTaskId, uiTestTask)

	releaseTaskId, releaseTask = generate_release_task(uiTestTaskId)
	schedule_task(queue, releaseTaskId, releaseTask)

