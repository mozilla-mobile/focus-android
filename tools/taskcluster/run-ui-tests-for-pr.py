# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/

import datetime
import json
import os
import taskcluster

from github import Github
from helper.tasks import generate_task

def generate_ui_test_task():
	return taskcluster.slugId(), generate_task(
		name = "(Focus for Android) UI tests",
		description = "Run UI tests for Focus/Klar for Android.",
		command = ('echo "--" > .adjust_token'
			' && ./gradlew clean assembleFocusWebviewDebug assembleFocusWebviewDebugAndroidTest'
			' && tools/taskcluster/execute-firebase-test.sh'),
		scopes = [ 'secrets:get:project/focus/firebase' ],
		artifacts = {
			"public": {
				"type": "directory",
				"path": "/opt/focus-android/test_artifacts",
				"expires": taskcluster.stringDate(taskcluster.fromNow('1 week'))
			}
		})

# Get token for GitHub bot account from secrets service
secrets = taskcluster.Secrets({'baseUrl': 'http://taskcluster/secrets/v1'})
data = secrets.get('project/focus/github')
token = data['secret']['botAccountToken']

LABEL = 'Run UI tests'
OWNER = 'mozilla-mobile'
REPO = 'focus-android'
TASK_ID = os.environ.get('TASK_ID')

github = Github(login_or_token=token)
repo = github.get_user(OWNER).get_repo(REPO)

issue = repo.get_issue(2199)
for label in issue.labels:
    if label.name == LABEL:
        # Queue task for running UI tests
        queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })
        taskId, task = generate_ui_test_task()        
        print json.dumps(task, indent=4, separators=(',', ': '))
        result = queue.createTask(taskId, task)
        print json.dumps(result)

        # Post comment with URL
        url = "https://tools.taskcluster.net/groups/" + result["status"]["taskGroupId"]
        issue.create_comment("Queued task for running UI tests:\n" + url + "\n\nGood luck! 😊")
        
        # Remove label from PR
        issue.remove_from_labels(LABEL)

        break

