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

package com.fiepi.media.player.input

import android.view.KeyEvent
import android.view.MotionEvent
import com.fiepi.mpv.input.MpvKeyHandler

/**
 * Player-agnostic key and motion event handler.
 * Currently delegates to MpvKeyHandler, but decouples UI from specific implementation.
 */
object PlayerKeyHandler {

    /**
     * Maps an Android KeyEvent to a player command.
     * @return A pair of (action, keyName) or null if not mapped.
     */
    fun mapKeyEvent(event: KeyEvent): Pair<String, String>? {
        return MpvKeyHandler.mapKeyEvent(event)
    }

    /**
     * Maps an Android MotionEvent (scroll) to a list of player commands.
     * @return A list of (action, command) pairs.
     */
    fun mapScrollEvent(event: MotionEvent): List<Pair<String, String>> {
        return MpvKeyHandler.mapScrollEvent(event)
    }
}
