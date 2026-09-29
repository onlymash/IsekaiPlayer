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
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import com.fiepi.media.player.model.PlayerSurfaceEvent

/**
 * Universal TextureView for media engines (MPV, ExoPlayer, etc.).
 * Supports View hierarchy blending/Compose effects like Backdrop.
 */
class PlayerTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TextureView(context, attrs), TextureView.SurfaceTextureListener {

    var onSurfaceEvent: ((PlayerSurfaceEvent) -> Unit)? = null

    init {
        surfaceTextureListener = this
        isOpaque = true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        onSurfaceEvent?.invoke(PlayerSurfaceEvent.Created(this, width, height))
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        onSurfaceEvent?.invoke(PlayerSurfaceEvent.Changed(width, height))
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        onSurfaceEvent?.invoke(PlayerSurfaceEvent.Destroyed(this))
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // No-op
    }
}
