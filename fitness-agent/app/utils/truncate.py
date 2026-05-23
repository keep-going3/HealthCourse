from typing import Any


def truncate_context(context: dict, max_actions: int = 20) -> dict:
    """截断训练上下文，最多保留 max_actions 个动作"""
    recent_workouts = context.get("recentWorkouts", [])
    if not recent_workouts:
        return context

    total_actions = sum(
        len(w.get("exercises", [])) for w in recent_workouts
    )

    if total_actions <= max_actions:
        return context

    # 按日期倒序，丢弃最早的
    sorted_workouts = sorted(recent_workouts, key=lambda w: w.get("sessionDate", ""), reverse=True)
    truncated = []
    count = 0
    for w in sorted_workouts:
        exercises = w.get("exercises", [])
        if count + len(exercises) <= max_actions:
            truncated.append(w)
            count += len(exercises)
        else:
            remaining = max_actions - count
            w["exercises"] = exercises[:remaining]
            truncated.append(w)
            count = max_actions
            break

    context["recentWorkouts"] = truncated
    context["_truncated"] = True
    return context
