package com.fiepi.media.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PlayerDrawerNavKey : NavKey {
    @Serializable
    data object Tracks : PlayerDrawerNavKey()

    @Serializable
    data object Playlist : PlayerDrawerNavKey()

    @Serializable
    data object Osd : PlayerDrawerNavKey()

    @Serializable
    data class Settings(
        @SerialName("key")
        val key: SettingsNavKey
    ) : PlayerDrawerNavKey()
}