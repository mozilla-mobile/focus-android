# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script will read the Screengrabfile.template from the project
root directory and created the final Screengrabfile by injecting
the locales passed to this script.
"""

import os
import sys

# Read template
template_path = os.path.join(os.path.dirname(__file__), '../../Screengrabfile.template')
template = open(template_path).read()

# Read locales from arguments
locales = sys.argv[1:]

# Generate list for config file in format: locales ['a', 'b', 'c']
locales_config = 'locales [' + ', '.join(map(lambda x: "'%s'" % x, locales[:])) + ']'

# Write configuration
config_path = os.path.join(os.path.dirname(__file__), '../../Screengrabfile')
config = open(config_path, 'w')
config.write(template)
config.write("\n")
config.write(locales_config)
config.write("\n")
config.close

print "Wrote Screengrabfile file (%s): %s" % (", ".join(locales), config_path)
