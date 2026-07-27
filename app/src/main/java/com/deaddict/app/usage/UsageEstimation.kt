package com.deaddict.app.usage

internal fun List<Long>.countStartsAfter(gap: Long): Int {
    if (isEmpty()) return 0
    return 1 + zipWithNext().count { (previous, next) -> next - previous >= gap }
}

internal fun List<Long>.countStartsWithin(gap: Long): Int =
    zipWithNext().count { (previous, next) -> next - previous in 1 until gap }
