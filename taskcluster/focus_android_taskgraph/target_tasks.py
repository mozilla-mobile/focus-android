# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import os
from redo import retry

from taskgraph.target_tasks import _target_task
from taskgraph.util.taskcluster import find_task_id


def index_exists(index_path, reason=""):
    print(f"Looking for existing index {index_path} {reason}...")
    try:
        task_id = find_task_id(index_path)
        print(f"Index {index_path} exists: taskId {task_id}")
        return True
    except KeyError:
        print(f"Index {index_path} doesn't exist.")
        return False


@_target_task("promote")
def target_tasks_promote(full_task_graph, parameters, graph_config):
    def filter(task, parameters):
        if (
            task.attributes.get("release-type") == parameters["release_type"]
            and task.attributes.get("shipping_phase") == "promote"
        ):
            return True

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task('ship')
def target_tasks_ship(full_task_graph, parameters, graph_config):
    filtered_for_candidates = target_tasks_promote(
        full_task_graph,
        parameters,
        graph_config,
    )

    def filter(task, parameters):
        # Include promotion tasks; these will be optimized out
        if task.label in filtered_for_candidates:
            return True

        if (
            task.attributes.get("release-type") == parameters["release_type"]
            and task.attributes.get("shipping_phase") == "ship"
        ):
            return True

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task('nightly')
def target_tasks_nightly(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for a nightly build."""

    def filter(task):
        return task.attributes.get("nightly-task", False)

    index_path = (
        f"{graph_config['trust-domain']}.v2.{parameters['project']}.branch."
        f"{parameters['head_ref']}.revision.{parameters['head_rev']}.taskgraph.decision-nightly"
    )
    if os.environ.get("MOZ_AUTOMATION") and retry(
        index_exists,
        args=(index_path,),
        kwargs={
            "reason": "to avoid triggering multiple nightlies off the same revision",
        },
    ):
        return []

    return [l for l, t in full_task_graph.tasks.items() if filter(t)]


