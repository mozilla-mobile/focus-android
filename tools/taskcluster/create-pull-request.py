 # This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This scripts pushes the passed in branch (argument) to the bot's
repository and creates a pull request for it.
"""

import os
import re
import subprocess
import sys
import taskcluster

from github import Github

# Make sure we have all needed arguments
if len(sys.argv) != 2:
	print "Usage", sys.argv[0], "BRANCH"
	exit(1)

# Get token for GitHub bot account from secrets service
secrets = taskcluster.Secrets({'baseUrl': 'http://taskcluster/secrets/v1'})
data = secrets.get('project/focus/github')
token = data['secret']['botAccountToken']

BRANCH = sys.argv[1]
OWNER = 'mozilla-mobile'
USER = 'MickeyMoz'
REPO = 'focus-android'
BASE = 'master'
HEAD = "MickeyMoz:%s" % BRANCH
URL = "https://%s:%s@github.com/%s/%s/" % (USER, token, USER, REPO)

github = Github(login_or_token=token)
repo = github.get_user(OWNER).get_repo(REPO)

# Check if there's already an umerged pull request.
for request in repo.get_pulls(state='open'):
	if request.user.login == USER:
		print "There's already an unmerged pull request. Doing nothing."
		exit(0)

# Push local state to branch
print subprocess.check_output(['git', 'push', URL, BRANCH])

# Read the log file and create the pull request body from it
log_path = os.path.join(os.path.dirname(__file__), '../../import-log.txt')
with open(log_path) as log_file:
	# Remove color codes from android2po output
	ansi_escape = re.compile(r'\x1b[^m]*m')
	log = ansi_escape.sub('', log_file.read())

body = """
Automated import %s

Log:
```
%s
```
""" % (BRANCH, log)

# Create pull request
pull_request = repo.create_pull(title='String import ' + BRANCH, body=body, base=BASE, head=HEAD)
print pull_request

# Add the [needs merge] label to the pull request
issue = repo.get_issue(pull_request.number)
issue.add_to_labels("needs merge")
print issue
