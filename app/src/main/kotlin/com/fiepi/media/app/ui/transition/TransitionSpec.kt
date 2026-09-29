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

package com.fiepi.media.app.ui.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.SwipeEdge

// Transition direction definition
enum class TransitionDirection {
    RightToLeft, // Enter: -> [New], Exit: [Old] <-
    LeftToRight  // Enter: [New] <-, Exit: -> [Old]
}

sealed interface AppTransitionType {
    data class Slide(
        val direction: TransitionDirection = TransitionDirection.RightToLeft,
        val duration: Int = 400
    ) : AppTransitionType

    data class Fade(
        val duration: Int = 300,
        val keepBackground: Boolean = false
    ) : AppTransitionType

    data object None : AppTransitionType
}

object AppTransition {

    // Minimum opacity of background page.
    // 0.5f means 50% visibility is retained at darkest, equivalent to a 50% black scrim overlay.
    // Smaller values result in darker scrims; larger values result in higher transparency.
    private const val BACKGROUND_MIN_ALPHA = 0.5f

    // Uses Material Design standard deceleration curve for smoother motion
    private val EmphasisEasing = FastOutSlowInEasing

    // --- Metadata-adapted version (Scene<*>) ---
    fun enter(type: AppTransitionType): AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform =
        {
            internalEnter(type)
        }

    fun pop(type: AppTransitionType): AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform =
        {
            internalPop(type)
        }

    fun predictive(type: AppTransitionType): AnimatedContentTransitionScope<Scene<*>>.(@SwipeEdge Int) -> ContentTransform =
        { edge ->
            internalPredictive(type, edge)
        }

    // --- Generic version for global parameters (Scene<T>) ---
    // Added default parameter values
    fun <T : Any> globalEnter(type: AppTransitionType = AppTransitionType.Slide()): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        {
            internalEnter(type)
        }

    fun <T : Any> globalPop(type: AppTransitionType = AppTransitionType.Slide()): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        {
            internalPop(type)
        }

    fun <T : Any> globalPredictive(type: AppTransitionType = AppTransitionType.Slide()): AnimatedContentTransitionScope<Scene<T>>.(@SwipeEdge Int) -> ContentTransform =
        { edge ->
            internalPredictive(type, edge)
        }

    // --- Internal unified implementation (extension functions) ---

    private fun internalEnter(type: AppTransitionType): ContentTransform {
        return when (type) {
            is AppTransitionType.Slide -> {
                if (type.direction == TransitionDirection.RightToLeft) {
                    // R->L Enter: New page enters from right (foreground), old page moves left (background parallax + scrim)
                    enterFromRight(type.duration) togetherWith exitToLeft(
                        type.duration,
                        parallax = true
                    )
                } else {
                    // L->R Enter: New page enters from left (foreground), old page moves right (background parallax + scrim)
                    enterFromLeft(type.duration) togetherWith exitToRight(
                        type.duration,
                        parallax = true
                    )
                }
            }

            is AppTransitionType.Fade -> fadeEnter(type.duration, type.keepBackground)
            AppTransitionType.None -> EnterTransition.None togetherWith ExitTransition.None
        }
    }

    private fun internalPop(type: AppTransitionType): ContentTransform {
        return when (type) {
            is AppTransitionType.Slide -> {
                if (type.direction == TransitionDirection.RightToLeft) {
                    // R->L Pop (Standard back): Old page returns from left (parallax + scrim fade), current page exits right (foreground)
                    enterFromLeft(
                        type.duration,
                        parallax = true
                    ) togetherWith exitToRight(type.duration)
                } else {
                    // L->R Pop (Reverse back): Old page returns from right (parallax + scrim fade), current page exits left (foreground)
                    enterFromRight(
                        type.duration,
                        parallax = true
                    ) togetherWith exitToLeft(type.duration)
                }
            }

            is AppTransitionType.Fade -> fadePop(type.duration)
            AppTransitionType.None -> EnterTransition.None togetherWith ExitTransition.None
        }
    }

    private fun internalPredictive(
        type: AppTransitionType,
        edge: Int
    ): ContentTransform {
        return when (type) {
            is AppTransitionType.Slide -> {
                if (edge == NavigationEvent.EDGE_RIGHT) {
                    // Swipe from right edge: simulate special enter effect
                    ContentTransform(
                        targetContentEnter = enterFromRight(type.duration, parallax = true),
                        initialContentExit = exitToLeft(type.duration, parallax = false)
                    )
                } else {
                    // Standard left-swipe back
                    internalPop(type)
                }
            }

            else -> internalPop(type)
        }
    }

    // --- Atomic animation implementations ---

    // Enter from right (offset: full -> 0)
    private fun enterFromRight(duration: Int, parallax: Boolean = false): EnterTransition {
        val slide = slideInHorizontally(
            tween(
                duration,
                easing = EmphasisEasing
            )
        ) { if (parallax) it / 2 else it }
        // If entering as background layer (parallax=true), add fade-in to simulate scrim fading
        return if (parallax) slide + fadeIn(
            animationSpec = tween(
                duration,
                easing = EmphasisEasing
            ),
            initialAlpha = BACKGROUND_MIN_ALPHA
        ) else slide
    }

    // Enter from left (offset: -full -> 0)
    private fun enterFromLeft(duration: Int, parallax: Boolean = false): EnterTransition {
        val slide = slideInHorizontally(
            tween(
                duration,
                easing = EmphasisEasing
            )
        ) { if (parallax) -it / 2 else -it }
        return if (parallax) slide + fadeIn(
            animationSpec = tween(
                duration,
                easing = EmphasisEasing
            ),
            initialAlpha = BACKGROUND_MIN_ALPHA
        ) else slide
    }

    // Exit to left (offset: 0 -> -full)
    private fun exitToLeft(duration: Int, parallax: Boolean = false): ExitTransition {
        val slide = slideOutHorizontally(
            tween(
                duration,
                easing = EmphasisEasing
            )
        ) { if (parallax) -it / 2 else -it }
        // If exiting as background layer, add fade-out to simulate scrim darkening
        return if (parallax) slide + fadeOut(
            animationSpec = tween(
                duration,
                easing = EmphasisEasing
            ),
            targetAlpha = BACKGROUND_MIN_ALPHA
        ) else slide
    }

    // Exit to right (offset: 0 -> full)
    private fun exitToRight(duration: Int, parallax: Boolean = false): ExitTransition {
        val slide = slideOutHorizontally(
            tween(
                duration,
                easing = EmphasisEasing
            )
        ) { if (parallax) it / 2 else it }
        return if (parallax) slide + fadeOut(
            animationSpec = tween(
                duration,
                easing = EmphasisEasing
            ),
            targetAlpha = BACKGROUND_MIN_ALPHA
        ) else slide
    }

    private fun fadeEnter(duration: Int, keepBackground: Boolean): ContentTransform {
        val enter = fadeIn(tween(duration))
        // If background needs to be kept (for shared elements), use snap to delay disappearance until animation completes
        val exit =
            if (keepBackground) fadeOut(snap(delayMillis = duration)) else fadeOut(tween(duration))
        return enter togetherWith exit
    }

    private fun fadePop(duration: Int): ContentTransform =
        fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
}