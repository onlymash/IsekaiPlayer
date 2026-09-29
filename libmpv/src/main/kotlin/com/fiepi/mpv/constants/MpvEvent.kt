/*
 * IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
 * Copyright (C) 2026 onlymash
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fiepi.mpv.constants

object MpvEvent {
    const val MPV_EVENT_NONE: Int = 0
    const val MPV_EVENT_SHUTDOWN: Int = 1
    const val MPV_EVENT_LOG_MESSAGE: Int = 2
    const val MPV_EVENT_GET_PROPERTY_REPLY: Int = 3
    const val MPV_EVENT_SET_PROPERTY_REPLY: Int = 4
    const val MPV_EVENT_COMMAND_REPLY: Int = 5
    const val MPV_EVENT_START_FILE: Int = 6
    const val MPV_EVENT_END_FILE: Int = 7
    const val MPV_EVENT_FILE_LOADED: Int = 8
    const val MPV_EVENT_CLIENT_MESSAGE: Int = 16
    const val MPV_EVENT_VIDEO_RECONFIG: Int = 17
    const val MPV_EVENT_AUDIO_RECONFIG: Int = 18
    const val MPV_EVENT_SEEK: Int = 20
    const val MPV_EVENT_PLAYBACK_RESTART: Int = 21
    const val MPV_EVENT_PROPERTY_CHANGE: Int = 22
    const val MPV_EVENT_QUEUE_OVERFLOW: Int = 24
    const val MPV_EVENT_HOOK: Int = 25
}