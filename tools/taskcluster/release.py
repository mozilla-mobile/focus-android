# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script is executed on taskcluster for "release events". It will
schedule the taskcluster tasks for building, signing and uploading a
release.
"""

import json
import lib.tasks
import taskcluster

def generate_build_task():
	return taskcluster.slugId(), lib.tasks.generate_task(
		name = "(Focus for Android) Build",
		description = "Build Focus/Klar for Android from source code.",
		command = ('echo "--" > .adjust_token'
				   ' && python tools/l10n/check_translations.py'
				   ' && ./gradlew --no-daemon clean assembleRelease'))

if __name__ == "__main__":
    queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })

    task_graph = {}

    build_task_id, build_task = generate_build_task()
    lib.tasks.schedule_task(queue, build_task_id, build_task)

    task_graph[build_task_id] = {}
    task_graph[build_task_id]["task"] = queue.task(build_task_id)
    print json.dumps(task_graph, indent=4, separators=(',', ': '))








