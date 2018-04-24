import taskcluster
import os

def generate_build_task():
	return taskcluster.slugId(), taskcluster.generate_task(
		name = "(Focus for Android) Build",
		description = "Build Focus/Klar for Android from source code.",
		command = ('echo "--" > .adjust_token'
				   ' && python tools/l10n/check_translations.py'
				   ' && ./gradlew --no-daemon clean assembleRelease'))


def schedule_task(queue, taskId, task):
	print "TASK", taskId
	print "hello world"
	print json.dumps(task, indent=4, separators=(',', ': '))

	result = queue.createTask(taskId, task)
	print json.dumps(result)

if __name__ == "__main__":
	queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })

	buildTaskId, buildTask = generate_build_task()
	schedule_task(queue, buildTaskId, buildTask)
