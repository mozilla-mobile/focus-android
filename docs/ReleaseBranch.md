### Creating a new Release Branch

1. Create a branch name with the format `releases_v[version]` (for example: `releases_v87.0.0`).
2. Pin the Android Components version to the final release version with the format `[version].0.0`.

   For example:

   | FILE               | CURRENT VERSION    | RELEASE VERSION |
   |--------------------|--------------------|-----------------|
   | AndroidComponents  | 73.0.20210223143117| 73.0.0          |

    For reference, the AC release checklist can be found at https://mozac.org/contributing/release-checklist.
    Instead of updating the AC version manually, you could also wait for automation to update the [ac version](https://github.com/mozilla-mobile/focus-android/actions/workflows/update-ac.yml).

3. In the GitHub UI, create a branch on the upstream with the format `releases_v[version]`.
4. Push the `releases_v[version]` branch as a PR with the commit message "Set version to [version]" and commit into the upstream `releases_v[version]` branch.
6. Create a new [milestone](https://github.com/mozilla-mobile/focus-android/milestones) for the next release and close the existing milestone.
7. Announce the new `releases_v[version]` branch on Slack in #releaseduty-mobile.

Automation should take over after you land your PR into the upstream `releases_v[version]` branch. You can verify by clicking on the branch in the UI, and looking for the green/yellow dot that will list links to the running build tasks.

### After the Beta release

   Update the `versionName` from `build.gradle(Module:focus-android.app)` to the next Focus beta release version.This should be the `[version + 1]`.
   For example, if the release cut was for `90`, we're looking to prepare the latest nightly for `91`.
   For example:

   | FILE               | CURRENT VERSION    | RELEASE VERSION |
   |--------------------|--------------------|-----------------|
   | build.gradle       | 90                 | 91              |

### Ask for Help

If you run into any problems, please ask any questions on Slack in #releaseduty-mobile.

References:
- https://github.com/mozilla-mobile/focus-android/tree/releases_v96.0
- https://github.com/mozilla-mobile/focus-android/pull/6012
