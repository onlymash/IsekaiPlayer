package com.fiepi.media.domain.config

object AppConstants {
    const val APP_NAME = "IsekaiPlayer"
    const val APP_LINK_REPOSITORY = "https://github.com/onlymash/IsekaiPlayer"
    const val APP_LINK_TELEGRAM_CHANNEL = "https://t.me/IsekaiPlayer"
    const val APP_LINK_DEVELOPER = "https://github.com/onlymash"

    const val MEDIA_FRAME_SCHEME = "frame"

    const val DATABASE_NAME = "media_player.db"
    const val MEDIA_SETTINGS_NAME = "media_settings.json"
    const val APPEARANCE_SETTINGS_NAME = "appearance_settings.json"
    const val GENERAL_SETTINGS_NAME = "general_settings.json"
    const val GESTURE_SETTINGS_NAME = "gesture_settings.json"
    const val PLAYER_SETTINGS_NAME = "player_settings.json"
    const val DECODER_SETTINGS_NAME = "decoder_settings.json"
    const val AUDIO_SETTINGS_NAME = "audio_settings.json"
    const val SUBTITLE_SETTINGS_NAME = "subtitle_settings.json"
    const val ADVANCED_SETTINGS_NAME = "advanced_settings.json"

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36"
    const val REMOTE_TIMEOUT_MS = 15_000L
    const val REMOTE_OPERATION_TIMEOUT_MS = 15_000L
    val VIDEO_EXTENSIONS = setOf(
        "264", "265", "3g2", "3ga", "3gp", "3gp2", "3gpp", "3gpp2", "3iv", "amr", "asf",
        "asx", "av1", "avc", "avf", "avi", "bdm", "bdmv", "clpi", "cpi", "divx", "dv", "evo",
        "evob", "f4v", "flc", "fli", "flic", "flv", "gxf", "h264", "h265", "hdmov", "hdv",
        "hevc", "lrv", "m1u", "m1v", "m2t", "m2ts", "m2v", "m4u", "m4v", "mkv", "mod", "moov",
        "mov", "mp2", "mp2v", "mp4", "mp4v", "mpe", "mpeg", "mpeg2", "mpeg4", "mpg", "mpg4",
        "mpl", "mpls", "mpv", "mpv2", "mts", "mtv", "mxf", "mxu", "nsv", "nut", "ogg", "ogm",
        "ogv", "ogx", "qt", "qtvr", "rm", "rmj", "rmm", "rms", "rmvb", "rmx", "rv", "rvx",
        "sdp", "tod", "trp", "ts", "tsa", "tsv", "tts", "vc1", "vfw", "vob", "vro", "webm",
        "wm", "wmv", "wmx", "x264", "x265", "xvid", "y4m", "yuv"
    )
    val PLAYLIST_EXTENSIONS = setOf("cue", "m3u", "m3u8", "pls", "vlc")
    val SUBTITLE_EXTENSIONS = setOf(
        "srt", "ass", "ssa", "vtt", "sub", "idx", "lrc", "smi", "sami", "rt", "sup", "ttml", "xml"
    )
}