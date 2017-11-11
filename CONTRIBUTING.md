# Contributing To Firefox Focus for Android

Getting Involved
----------------

We encourage you to participate in this open source project. We love Pull Requests, Bug Reports, ideas, (security) code reviews or any kind of positive contribution. Please read the [Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* Overview to contributing (**new contributors start here!**): https://github.com/mozilla-mobile/focus-android/wiki/Contributing

* Issues: [https://github.com/mozilla-mobile/focus-android/issues](https://github.com/mozilla-mobile/focus-android/issues)

* IRC: [#focus (irc.mozilla.org)](https://wiki.mozilla.org/IRC)

* Mailing list: [firefox-focus-public@](https://mail.mozilla.org/listinfo/firefox-focus-public)

* Wiki: [https://github.com/mozilla-mobile/focus-android/wiki](https://github.com/mozilla-mobile/focus-android/wiki)

Watch out for [issues with the "good first issue" label](https://github.com/mozilla-mobile/focus-android/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22). Those are easy starter bugs that are available to work on!

Build instructions
------------------

1. Clone the repository:

  ```shell
  git clone https://github.com/mozilla-mobile/focus-android
  ```

2. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembleFocusWebviewDebug
  ```

3. Make sure to select the right build variant in Android Studio: **focusWebviewDebug**

License
-------

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
