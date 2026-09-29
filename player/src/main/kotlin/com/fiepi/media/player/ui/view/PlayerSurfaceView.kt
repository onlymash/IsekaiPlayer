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

package com.fiepi.media.player.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.fiepi.media.player.model.PlayerSurfaceEvent

/**
 * Universal SurfaceView for media engines (MPV, ExoPlayer, etc.).
 * Handles Surface lifecycle events.
 */
class PlayerSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    var onSurfaceEvent: ((PlayerSurfaceEvent) -> Unit)? = null

    init {
        // 显式指定 10-bit 色彩缓冲区格式 (RGBA_1010102)，支持宽色域/HDR 物理输出
        holder.setFormat(android.graphics.PixelFormat.RGBA_1010102)
        holder.addCallback(this)
    }

    private var isCreated = false

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Wait for surfaceChanged to provide dimensions
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (!isCreated) {
            isCreated = true
            onSurfaceEvent?.invoke(PlayerSurfaceEvent.Created(this, width, height))
        } else {
            onSurfaceEvent?.invoke(PlayerSurfaceEvent.Changed(width, height))
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isCreated = false
        onSurfaceEvent?.invoke(PlayerSurfaceEvent.Destroyed(this))
    }
}
