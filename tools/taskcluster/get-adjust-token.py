# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script talks to the taskcluster secrets service to obtain the
Adjust token and write it to the .adjust_token file in the root
directory.
"""

import taskcluster

# Get JSON data from taskcluster secrets service
secrets = taskcluster.Secrets({'baseUrl': 'http://taskcluster/queue/v1'})
data = secrets.get('garbage/sebastian/token-test')

print "Token:", data.adjustToken
