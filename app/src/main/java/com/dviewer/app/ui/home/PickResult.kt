package com.dviewer.app.ui.home

sealed interface PickResult<out T> {
    data class Ready<T>(val value: T) : PickResult<T>
    data class Error(val message: String) : PickResult<Nothing>
    data object Cancelled : PickResult<Nothing>
}

fun <T> pickResultFor(picked: T?, isReadable: (T) -> Boolean): PickResult<T> = when {
    picked == null -> PickResult.Cancelled
    isReadable(picked) -> PickResult.Ready(picked)
    else -> PickResult.Error("Couldn't open that file.")
}
