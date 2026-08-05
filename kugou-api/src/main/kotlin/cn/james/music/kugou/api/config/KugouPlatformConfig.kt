package cn.james.music.kugou.api.config

data class KugouPlatformConfig(
    val appId: String,
    val clientVersion: String,
    val userAgent: String,
) {
    companion object {
        // Target protocol values from KuGouMusicApi@6efe84e (MIT).
        val Standard =
            KugouPlatformConfig(
                appId = "1005",
                clientVersion = "20489",
                userAgent = "Android15-1070-11083-46-0-DiscoveryDRADProtocol-wifi",
            )
    }
}
