# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
Helper methods to work with taskcluster tasks
"""

import datetime
import taskcluster
import os

TASK_ID = os.environ.get('TASK_ID')
OWNER = "skaspari@mozilla.com"
SOURCE = "https://github.com/mozilla-mobile/focus-android/tree/master/tools/taskcluster"
REPO_URL = os.environ.get('GITHUB_HEAD_REPO_URL')
os.environ.get('GITHUB_HEAD_BRANCH')
BRANCH = os.environ.get('GITHUB_HEAD_BRANCH')
COMMIT = os.environ.get('GITHUB_HEAD_SHA')

def generate_task(name, description, command, dependencies = [], artifacts = {}, scopes = [], routes = []):
	"""Create a new task that depends on the current task"""
	created = datetime.datetime.now()
	expires = taskcluster.fromNow('1 week')
	deadline = taskcluster.fromNow('1 day')

	return {
	    "workerType": "github-worker",
	    "taskGroupId": TASK_ID,
	    "expires": taskcluster.stringDate(expires),
	    "retries": 5,
	    "created": taskcluster.stringDate(created),
	    "tags": {},
	    "priority": "lowest",
	    "schedulerId": "taskcluster-github",
	    "deadline": taskcluster.stringDate(deadline),
	    "dependencies": [ TASK_ID ] + dependencies,
	    "routes": routes,
	    "scopes": scopes,
	    "requires": "all-completed",
	    "payload": {
	        "features": {
	            "taskclusterProxy": True
	        },
	        "maxRunTime": 7200,
	        "image": "mozillamobile/focus-android",
	        "command": [
	            "/bin/bash",
	            "--login",
	            "-c",
				"git fetch %s %s && git config advice.detachedHead false && git checkout %s && %s" % (REPO_URL, BRANCH, COMMIT, command)
	        ],
	        "artifacts": artifacts,
	        "deadline": taskcluster.stringDate(deadline)
	    },
		"provisionerId": "aws-provisioner-v1",
		"metadata": {
			"name": name,
			"description": description,
			"owner": OWNER,
			"source": SOURCE
		}
	}
