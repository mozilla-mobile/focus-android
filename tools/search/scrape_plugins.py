#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from lxml import etree
import copy
import json
import os
import requests
import shutil
import urllib

# Path for list file
LIST_URL = "https://hg.mozilla.org/mozilla-central/raw-file/default/mobile/locales/search/list.json"
# Path for search plugins
SEARCH_PLUGINS_URL = "https://hg.mozilla.org/mozilla-central/raw-file/default/mobile/locales/searchplugins/{plugin}"

ns = {"search": "http://www.mozilla.org/2006/browser/search/"}


def import_plugins():
    # Remove and recreate the SearchPlugins directory.
    if os.path.exists("SearchPlugins"):
        shutil.rmtree("SearchPlugins")
    os.makedirs("SearchPlugins")

    r = requests.get(LIST_URL)
    plugins = r.json()

    engines = {}

    # Import engines from the l10n repos.
    locales = plugins["locales"]
    for locale in locales:
        regions = locales[locale]
        for region in regions:
            if region == "default":
                code = locale
            elif region == "experimental-hidden":
                continue
            else:
                language = locale.split("-")[0]
                code = ("%s-%s" % (language, region))

            print("adding %s..." % code)

            visibleEngines = regions[region]["visibleDefaultEngines"]
            downloadEngines(code, Scraper(locale), visibleEngines)
            engines[code] = visibleEngines

    # Import default engines from the core repo.
    print("adding defaults...")
    defaultEngines = plugins['default']['visibleDefaultEngines']
    downloadEngines("default", Scraper(), defaultEngines)
    engines['default'] = defaultEngines

    # Remove Bing.
    if "bing" in engines['default']:
        engines['default'].remove('bing')

    # Make sure fallback directories contain any skipped engines.
    verifyEngines(engines)

    # Write the list of engine names for each locale.
    writeList(engines)


def downloadEngines(locale, scraper, engines):
    directory = os.path.join("SearchPlugins", locale)
    if not os.path.exists(directory):
        os.makedirs(directory)

    # Remove Bing.
    if 'bing' in engines:
        engines.remove('bing')

    # Always include DuckDuckGo.
    if "duckduckgo" not in engines:
        lastEngine = '~'
        for i, engine in reversed(list(enumerate(engines))):
            if i > 0 and "duckduckgo" < engine and engine < lastEngine and not engine.startswith("google"):
                lastEngine = engine
                continue
            engines.insert(i + 1, "duckduckgo")
            break

    for engine in engines:
        engine_file = engine + ".xml"
        path = os.path.join(directory, engine_file)
        downloadedFile = scraper.getFile(engine_file)
        if downloadedFile is None:
            print("  skipping: %s..." % engine_file)
            continue

        print("  downloading: %s..." % engine_file)
        name, extension = os.path.splitext(engine_file)

        # Apply iOS-specific overlays for this engine if they are defined.
        if extension == ".xml":
            engine = name.split("-")[0]
            overlay = overlayForEngine(engine)
            if overlay:
                plugin = etree.parse(downloadedFile)
                overlay.apply(plugin)
                contents = etree.tostring(plugin.getroot(), encoding="utf-8", pretty_print=True)
                with open(path, "w") as outfile:
                    outfile.write(contents)
                continue

        # Otherwise, just use the downloaded file as is.
        shutil.move(downloadedFile, path)


def verifyEngines(engines):
    print("verifying engines...")
    error = False
    for locale in engines:
        dirs = [locale, locale.split('-')[0], 'default']
        dirs = map(lambda dir: os.path.join('SearchPlugins', dir), dirs)
        for engine in engines[locale]:
            file = engine + '.xml'
            if not any(os.path.exists(os.path.join(dir, file)) for dir in dirs):
                error = True
                print("  ERROR: missing engine %s for locale %s" % (engine, locale))
    if not error:
        print("  OK!")


def overlayForEngine(engine):
    path = os.path.join("SearchOverlays", "%s.xml" % engine)
    if not os.path.exists(path):
        return None
    return Overlay(path)


def writeList(engines):
    with open("search_configuration.json", "w") as outfile:
        json.dump(engines, outfile, indent=2)


class Scraper:
    def __init__(self, locale=None):
        self.plugins_url = SEARCH_PLUGINS_URL
        self.locale = locale

    def getFile(self, plugin_file):
        path = self.plugins_url.format(plugin=plugin_file)
        handle = urllib.urlopen(path)
        if handle.code != 200:
            return None

        result = urllib.urlretrieve(path)
        return result[0]


class Overlay:
    def __init__(self, path):
        overlay = etree.parse(path)
        self.actions = overlay.getroot().getchildren()

    def apply(self, doc):
        for action in self.actions:
            if action.tag == "replace":
                self.replace(target=action.get("target"), replacement=action[0], doc=doc)
            elif action.tag == "append":
                self.append(parent=action.get("parent"), child=action[0], doc=doc)

    def replace(self, target, replacement, doc):
        for element in doc.xpath(target, namespaces=ns):
            replacementCopy = copy.deepcopy(replacement)
            element.getparent().replace(element, replacementCopy)

            # Try to preserve indentation.
            replacementCopy.tail = element.tail

    def append(self, parent, child, doc):
        for element in doc.xpath(parent, namespaces=ns):
            childCopy = copy.deepcopy(child)
            element.append(childCopy)

            # Try to preserve indentation.
            childCopy.tail = "\n"
            previous = childCopy.getprevious()
            if previous is not None:
                childCopy.tail = previous.tail
                prevPrevious = previous.getprevious()
                if prevPrevious is not None:
                    previous.tail = prevPrevious.tail


if __name__ == "__main__":
    import_plugins()
