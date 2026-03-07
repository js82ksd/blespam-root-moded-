package com.tutozz.blespam

class AppleTileService : BaseSpammerTileService() {
    override val spammerTypes = listOf("iOS Crash", "Apple Action Modal", "Apple Device Popup", "Apple 'Not Your Device'")
    override val spammerLabel = "Apple"
}
