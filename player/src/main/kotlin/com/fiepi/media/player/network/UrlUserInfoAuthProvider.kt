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

package com.fiepi.media.player.network

import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import kotlin.io.encoding.Base64

class UrlUserInfoAuthProvider : AuthProvider {
    // 允许在无 401 挑战时直接发送 Authorization 头
    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean {
        return !request.url.user.isNullOrEmpty() && !request.url.password.isNullOrEmpty()
    }

    @Deprecated("Please use sendWithoutRequest function instead", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean
        get() = error("Deprecated")

    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return AuthScheme.Basic.equals(auth.authScheme, ignoreCase = true)
    }

    override suspend fun addRequestHeaders(request: HttpRequestBuilder, authHeader: HttpAuthHeader?) {
        val user = request.url.user
        val password = request.url.password

        if (!user.isNullOrEmpty() && !password.isNullOrEmpty()) {
            val authString = "$user:$password"
            val encoded = Base64.encode(authString.toByteArray(Charsets.UTF_8))
            request.headers[HttpHeaders.Authorization] = "Basic $encoded"

            // 提取后清除 URL 中的明文敏感信息
            request.url.user = null
            request.url.password = null
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean = false
}