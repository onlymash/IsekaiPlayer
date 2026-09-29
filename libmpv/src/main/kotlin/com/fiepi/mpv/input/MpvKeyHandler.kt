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

package com.fiepi.mpv.input

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Parser and conversion utility for mapping Android keyboard/remote/scroll events to MPV key command sets
 */
object MpvKeyHandler {

    private const val TAG = "MPVKeyHandler"

    /**
     * https://github.com/mpv-player/mpv/blob/master/input/keycodes.h
     * Mapping table from Android KeyCode to mpv key names
     */
    private val KEY_MAPPING: Map<Int, String> = mapOf(
        KeyEvent.KEYCODE_SPACE to "SPACE",
        KeyEvent.KEYCODE_ENTER to "ENTER",
        KeyEvent.KEYCODE_TAB to "TAB",
        KeyEvent.KEYCODE_DEL to "BS",
        KeyEvent.KEYCODE_FORWARD_DEL to "DEL",
        KeyEvent.KEYCODE_INSERT to "INS",
        KeyEvent.KEYCODE_MOVE_HOME to "HOME",
        KeyEvent.KEYCODE_MOVE_END to "END",
        KeyEvent.KEYCODE_PAGE_UP to "PGUP",
        KeyEvent.KEYCODE_PAGE_DOWN to "PGDWN",
        KeyEvent.KEYCODE_ESCAPE to "ESC",
        KeyEvent.KEYCODE_SYSRQ to "PRINT",
        KeyEvent.KEYCODE_DPAD_RIGHT to "RIGHT",
        KeyEvent.KEYCODE_DPAD_LEFT to "LEFT",
        KeyEvent.KEYCODE_DPAD_DOWN to "DOWN",
        KeyEvent.KEYCODE_DPAD_UP to "UP",
        KeyEvent.KEYCODE_MEDIA_PLAY to "PLAYONLY",
        KeyEvent.KEYCODE_MEDIA_PAUSE to "PAUSEONLY",
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "PLAYPAUSE",
        KeyEvent.KEYCODE_HEADSETHOOK to "PLAYPAUSE",
        KeyEvent.KEYCODE_MEDIA_STOP to "STOP",
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to "FORWARD",
        KeyEvent.KEYCODE_MEDIA_REWIND to "REWIND",
        KeyEvent.KEYCODE_MEDIA_NEXT to "NEXT",
        KeyEvent.KEYCODE_MEDIA_PREVIOUS to "PREV",
        KeyEvent.KEYCODE_MEDIA_RECORD to "RECORD",
        KeyEvent.KEYCODE_CHANNEL_UP to "CHANNEL_UP",
        KeyEvent.KEYCODE_CHANNEL_DOWN to "CHANNEL_DOWN",
        KeyEvent.KEYCODE_ZOOM_IN to "ZOOMIN",
        KeyEvent.KEYCODE_ZOOM_OUT to "ZOOMOUT",
        KeyEvent.KEYCODE_F1 to "F1",
        KeyEvent.KEYCODE_F2 to "F2",
        KeyEvent.KEYCODE_F3 to "F3",
        KeyEvent.KEYCODE_F4 to "F4",
        KeyEvent.KEYCODE_F5 to "F5",
        KeyEvent.KEYCODE_F6 to "F6",
        KeyEvent.KEYCODE_F7 to "F7",
        KeyEvent.KEYCODE_F8 to "F8",
        KeyEvent.KEYCODE_F9 to "F9",
        KeyEvent.KEYCODE_F10 to "F10",
        KeyEvent.KEYCODE_F11 to "F11",
        KeyEvent.KEYCODE_F12 to "F12",
        KeyEvent.KEYCODE_NUMPAD_0 to "KP0",
        KeyEvent.KEYCODE_NUMPAD_1 to "KP1",
        KeyEvent.KEYCODE_NUMPAD_2 to "KP2",
        KeyEvent.KEYCODE_NUMPAD_3 to "KP3",
        KeyEvent.KEYCODE_NUMPAD_4 to "KP4",
        KeyEvent.KEYCODE_NUMPAD_5 to "KP5",
        KeyEvent.KEYCODE_NUMPAD_6 to "KP6",
        KeyEvent.KEYCODE_NUMPAD_7 to "KP7",
        KeyEvent.KEYCODE_NUMPAD_8 to "KP8",
        KeyEvent.KEYCODE_NUMPAD_9 to "KP9",
        KeyEvent.KEYCODE_NUMPAD_DOT to "KP_DEC",
        KeyEvent.KEYCODE_NUMPAD_ENTER to "KP_ENTER",
        KeyEvent.KEYCODE_PROG_RED to "F13",
        KeyEvent.KEYCODE_PROG_GREEN to "F14",
        KeyEvent.KEYCODE_PROG_YELLOW to "F15",
        KeyEvent.KEYCODE_PROG_BLUE to "F16"
    )

    /**
     * Converts Android KeyEvent to MPV command parameter pair
     *
     * @param event Native keyboard/remote event
     * @return Returns Pair("keydown"/"keyup", key combination string e.g. "shift+SPACE") on success
     */
    fun mapKeyEvent(event: KeyEvent): Pair<String, String>? {
        // Filter: handle only standard ACTION_DOWN and ACTION_UP, naturally excluding ACTION_MULTIPLE and other deprecated/unknown actions
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) {
            return null
        }

        // Exclude standalone modifier key presses
        if (KeyEvent.isModifierKey(event.keyCode)) {
            return null
        }

        // Prioritize matching special function keys and media keys
        var mapped = KEY_MAPPING[event.keyCode]
        if (mapped == null) {
            // If not a special key and not a printing key, discard
            if (!event.isPrintingKey) {
                return null
            }

            // Filter Dead Keys (combining accent keys)
            val ch = event.unicodeChar
            if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
                return null
            }
            mapped = ch.toChar().toString()
        }

        // When long-press triggers Android repeating events, do not dispatch repeatedly; let mpv engine manage auto-repeat
        if (event.repeatCount > 0) {
            return null
        }

        // 5. Assemble key combination prefix
        val mod = mutableListOf<String>()
        if (event.isShiftPressed) mod.add("shift")
        if (event.isCtrlPressed) mod.add("ctrl")
        if (event.isAltPressed) mod.add("alt")
        if (event.isMetaPressed) mod.add("meta")
        mod.add(mapped)

        val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
        val keyName = mod.joinToString("+")

        return Pair(action, keyName)
    }

    /**
     * Parses mouse or touchpad wheel scroll events into a list of MPV WHEEL key events
     */
    fun mapScrollEvent(event: MotionEvent): List<Pair<String, String>> {
        val commands = mutableListOf<Pair<String, String>>()
        if (event.isFromSource(InputDevice.SOURCE_CLASS_POINTER) &&
            event.actionMasked == MotionEvent.ACTION_SCROLL
        ) {
            val h = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val v = event.getAxisValue(MotionEvent.AXIS_VSCROLL)

            if (h > 0) commands.add(Pair("keypress", "WHEEL_RIGHT"))
            else if (h < 0) commands.add(Pair("keypress", "WHEEL_LEFT"))

            if (v > 0) commands.add(Pair("keypress", "WHEEL_UP"))
            else if (v < 0) commands.add(Pair("keypress", "WHEEL_DOWN"))
        }
        return commands
    }
}