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

package com.fiepi.media.data.repository.media.proxy

import com.fiepi.media.data.utils.closeSilently
import com.fiepi.media.data.utils.resolveMimeType
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.MediaStreamServer
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.hierynomus.smbj.transport.tcp.async.AsyncDirectTcpTransportFactory
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.net.ServerSocket
import java.net.URLEncoder
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Embedded local streaming proxy server implementation based on Ktor CIO
 */
class MediaStreamServerImpl : MediaStreamServer {

    private var server: EmbeddedServer<*, *>? = null
    private var boundPort: Int = 0

    private val sessionRegistry = ConcurrentHashMap<String, AutoCloseable>()

    private val releaseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val smbConfig = SmbConfig.builder()
        .withDialects(
            SMB2Dialect.SMB_3_1_1,
            SMB2Dialect.SMB_3_0_2,
            SMB2Dialect.SMB_3_0,
            SMB2Dialect.SMB_2_1,
            SMB2Dialect.SMB_2_0_2,
        )
        .withTimeout(AppConstants.REMOTE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .withSoTimeout(AppConstants.REMOTE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .withBufferSize(4 * 1024 * 1024)
        .withTransportLayerFactory(AsyncDirectTcpTransportFactory())
        .withDfsEnabled(false)
        .withMultiProtocolNegotiate(true)
        .withSigningRequired(false)
        .withEncryptData(false)
        .build()

    @Synchronized
    override fun start(port: Int): Int {
        if (server != null) return boundPort

        val targetPort = if (port == 0) findFreePort() else port
        boundPort = targetPort

        server = embeddedServer(CIO, port = targetPort, host = "127.0.0.1") {
            routing {
                get("/smb/stream") {
                    handleSmbStream(call)
                }
                get("/ftp/stream") {
                    handleFtpStream(call)
                }
            }
        }.apply {
            start(wait = false)
        }

        return boundPort
    }

    override fun stop() {
        releaseScope.launch {
            synchronized(this@MediaStreamServerImpl) {
                val s = server
                server = null
                boundPort = 0
                sessionRegistry.values.forEach { it.closeSilently() }
                sessionRegistry.clear()
                s
            }?.stop(500, 1500)
        }
    }

    override fun isRunning(): Boolean = server != null

    override fun registerMediaSession(
        sessionId: String,
        source: RemoteSource,
        filePath: String,
        knownFileSize: Long
    ): String {
        require(boundPort > 0) { "Local proxy service is not running yet, please call start() first" }

        val encodedSid = URLEncoder.encode(sessionId, "UTF-8")
        when (source.type) {
            SourceType.Smb -> {
                sessionRegistry[sessionId] = SmbStreamSession(
                    source = source,
                    filePath = filePath,
                    fileSize = knownFileSize
                )
                return "http://127.0.0.1:$boundPort/smb/stream?sessionId=$encodedSid"
            }

            SourceType.Ftp -> {
                sessionRegistry[sessionId] = FtpStreamSession(
                    source = source,
                    filePath = filePath,
                    fileSize = knownFileSize
                )
                return "http://127.0.0.1:$boundPort/ftp/stream?sessionId=$encodedSid"
            }

            else -> {
                throw IllegalArgumentException("Unsupported protocol for media stream server: ${source.type}")
            }
        }
    }

    override fun unregisterSession(sessionId: String) {
        val session = sessionRegistry.remove(sessionId)
        releaseScope.launch {
            session?.closeSilently()
        }
    }

    /**
     * Clears all registered sessions (asynchronous, non-blocking to main thread)
     */
    override fun clearAllSessions() {
        if (sessionRegistry.isEmpty()) return

        val sessions = sessionRegistry.values.toList()
        sessionRegistry.clear()

        releaseScope.launch {
            sessions.forEach { it.closeSilently() }
        }
    }

    private suspend fun handleSmbStream(call: ApplicationCall) {
        val sessionId = call.request.queryParameters["sessionId"]
        if (sessionId.isNullOrBlank()) {
            call.respondText("Missing sessionId parameter", status = HttpStatusCode.BadRequest)
            return
        }

        val session = sessionRegistry[sessionId] as? SmbStreamSession
        if (session == null) {
            call.respondText(
                "Invalid or expired playback session",
                status = HttpStatusCode.NotFound
            )
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val shareName = session.source.path?.trim('/')?.substringBefore('/') ?: ""

                // Get file size (reuse existing DiskShare)
                var totalLength = session.fileSize
                if (totalLength <= 0L) {
                    val diskShare = session.getDiskShare(smbConfig, shareName)
                    totalLength =
                        resolveRemoteFileSizeWithShare(diskShare, session.source, session.filePath)
                    session.fileSize = totalLength
                }

                val rangeHeader = call.request.headers[HttpHeaders.Range]
                val range = parseRange(rangeHeader, totalLength)

                // Normalize response headers
                call.response.header(HttpHeaders.AcceptRanges, "bytes")

                // Add Content-Type to help player identify container format
                val contentType = getMimeType(session.filePath)
                call.response.header(HttpHeaders.ContentType, contentType)

                if (range != null) {
                    val (start, end) = range
                    val contentLength = end - start + 1

                    call.response.header(
                        HttpHeaders.ContentRange,
                        "bytes $start-$end/$totalLength"
                    )
                    call.response.header(HttpHeaders.ContentLength, contentLength.toString())

                    call.respond(
                        HttpStatusCode.PartialContent,
                        ResilientSmbStreamContent(
                            session = session,
                            smbConfig = smbConfig,
                            startOffset = start,
                            endOffset = end,
                            contentSize = contentLength
                        )
                    )
                } else {
                    call.response.header(HttpHeaders.ContentLength, totalLength.toString())

                    call.respond(
                        HttpStatusCode.OK,
                        ResilientSmbStreamContent(
                            session = session,
                            smbConfig = smbConfig,
                            startOffset = 0L,
                            endOffset = totalLength - 1,
                            contentSize = totalLength
                        )
                    )
                }
            } catch (e: SMBApiException) {
                // Return more specific HTTP status codes for SMB layer errors
                val status = when (e.status.value) {
                    0xC0000034L -> HttpStatusCode.NotFound // STATUS_OBJECT_NAME_NOT_FOUND
                    0xC0000022L -> HttpStatusCode.Forbidden // STATUS_ACCESS_DENIED
                    0xC000006DL -> HttpStatusCode.Unauthorized // STATUS_LOGON_FAILURE
                    else -> HttpStatusCode.InternalServerError
                }
                call.respondText("SMB access failed: ${e.message}", status = status)
            } catch (e: Exception) {
                call.respondText(
                    "Streaming service error: ${e.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }

    private suspend fun handleFtpStream(call: ApplicationCall) {
        val sessionId = call.request.queryParameters["sessionId"]
        if (sessionId.isNullOrBlank()) {
            call.respondText("Missing sessionId parameter", status = HttpStatusCode.BadRequest)
            return
        }

        val session = sessionRegistry[sessionId] as? FtpStreamSession
        if (session == null) {
            call.respondText(
                "Invalid or expired playback session",
                status = HttpStatusCode.NotFound
            )
            return
        }

        withContext(Dispatchers.IO) {
            try {
                var totalLength = session.fileSize
                if (totalLength <= 0L) {
                    totalLength = resolveFtpFileSize(session.source, session.filePath)
                    session.fileSize = totalLength
                }

                val rangeHeader = call.request.headers[HttpHeaders.Range]
                val range = parseRange(rangeHeader, totalLength)

                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                val contentType = getMimeType(session.filePath)
                call.response.header(HttpHeaders.ContentType, contentType)

                if (range != null) {
                    val (start, end) = range
                    val contentLength = end - start + 1

                    call.response.header(
                        HttpHeaders.ContentRange,
                        "bytes $start-$end/$totalLength"
                    )
                    call.response.header(HttpHeaders.ContentLength, contentLength.toString())

                    call.respond(
                        HttpStatusCode.PartialContent,
                        ResilientFtpStreamContent(
                            session = session,
                            startOffset = start,
                            endOffset = end,
                            contentSize = contentLength
                        )
                    )
                } else {
                    call.response.header(HttpHeaders.ContentLength, totalLength.toString())

                    call.respond(
                        HttpStatusCode.OK,
                        ResilientFtpStreamContent(
                            session = session,
                            startOffset = 0L,
                            endOffset = totalLength - 1,
                            contentSize = totalLength
                        )
                    )
                }
            } catch (e: Exception) {
                call.respondText(
                    "FTP streaming service error: ${e.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }

    private suspend fun resolveFtpFileSize(source: RemoteSource, filePath: String): Long =
        withContext(Dispatchers.IO) {
            val tempClient = FTPClient().apply {
                controlEncoding = "UTF-8"
                connectTimeout = AppConstants.REMOTE_TIMEOUT_MS.toInt()
                defaultTimeout = AppConstants.REMOTE_TIMEOUT_MS.toInt()
            }
            try {
                tempClient.connect(source.host, source.port ?: 21)
                if (!FTPReply.isPositiveCompletion(tempClient.replyCode)) return@withContext -1L
                val loginSuccess = if (source.username != null) {
                    tempClient.login(source.username, source.password ?: "")
                } else {
                    tempClient.login("anonymous", "")
                }
                if (!loginSuccess) return@withContext -1L
                tempClient.setFileType(FTP.BINARY_FILE_TYPE)
                tempClient.enterLocalPassiveMode()

                if (tempClient.sendCommand("SIZE", filePath) == FTPReply.FILE_STATUS) {
                    tempClient.replyString.trim().substringAfterLast(' ').toLongOrNull() ?: -1L
                } else {
                    val parent = filePath.substringBeforeLast('/', "").ifEmpty { "/" }
                    val name = filePath.substringAfterLast('/')
                    tempClient.listFiles(parent)
                        .firstOrNull { it.name == name && !it.isDirectory }?.size
                        ?: -1L
                }
            } catch (_: Exception) {
                -1L
            } finally {
                runCatching {
                    if (tempClient.isConnected) {
                        tempClient.logout()
                        tempClient.disconnect()
                    }
                }
            }
        }

    private fun resolveRemoteFileSizeWithShare(
        diskShare: DiskShare,
        source: RemoteSource,
        filePath: String
    ): Long {
        val shareName = source.path?.trim('/')?.substringBefore('/') ?: ""
        val baseSubPath = source.path?.trim('/')?.substringAfter('/', "") ?: ""
        val cleanPath = filePath.trim('/')
        val relativePath = if (cleanPath.isEmpty()) {
            baseSubPath
        } else {
            cleanPath.removePrefix(shareName).trim('/')
        }.replace('/', '\\')

        var file: File? = null
        try {
            file = diskShare.openFile(
                relativePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            return file.fileInformation.standardInformation.endOfFile
        } finally {
            file?.closeSilently()
        }
    }

    private fun getMimeType(filePath: String): String {
        return resolveMimeType(filePath) ?: "application/octet-stream"
    }

    private fun parseRange(rangeHeader: String?, totalLength: Long): Pair<Long, Long>? {
        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) return null

        val rangeValue = rangeHeader.removePrefix("bytes=").split("-")
        if (rangeValue.size != 2) return null

        val start = rangeValue[0].toLongOrNull() ?: 0L
        val end = rangeValue[1].toLongOrNull() ?: (totalLength - 1)

        val validStart = start.coerceIn(0L, totalLength - 1)
        val validEnd = end.coerceIn(validStart, totalLength - 1)

        return Pair(validStart, validEnd)
    }

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }
}
