package com.tutozz.blespam

object HciCommands {

    private fun hex(b: ByteArray): String {
        val sb = StringBuilder()
        for (i in b.indices) {
            if (i > 0) sb.append(' ')
            val v = b[i].toInt() and 0xFF
            if (v < 16) sb.append('0')
            sb.append(Integer.toHexString(v).uppercase())
        }
        return sb.toString()
    }

    fun leSetAdvParams(
        minInterval: Int = 0x20,
        maxInterval: Int = 0x20,
        advType: Int = 0x03,
        ownAddrType: Int = 0x01,
        channelMap: Int = 0x07,
        filterPolicy: Int = 0x00
    ): String {
        val p = ByteArray(15)
        p[0] = (minInterval and 0xFF).toByte()
        p[1] = ((minInterval shr 8) and 0xFF).toByte()
        p[2] = (maxInterval and 0xFF).toByte()
        p[3] = ((maxInterval shr 8) and 0xFF).toByte()
        p[4] = advType.toByte()
        p[5] = ownAddrType.toByte()
        p[13] = channelMap.toByte()
        p[14] = filterPolicy.toByte()
        return "0x08 0x0006 " + hex(p)
    }

    fun leSetRandomAddr(mac: ByteArray): String {
        require(mac.size == 6) { "MAC must be 6 bytes" }
        val rev = ByteArray(6)
        for (i in 0..5) rev[i] = mac[5 - i]
        return "0x08 0x0005 " + hex(rev)
    }

    fun leSetAdvData(data: ByteArray): String {
        require(data.size <= 31) { "Legacy adv data max 31 bytes" }
        val p = ByteArray(32)
        p[0] = data.size.toByte()
        System.arraycopy(data, 0, p, 1, data.size)
        return "0x08 0x0008 " + hex(p)
    }

    fun leSetScanRespData(data: ByteArray): String {
        require(data.size <= 31) { "Scan response max 31 bytes" }
        val p = ByteArray(32)
        p[0] = data.size.toByte()
        System.arraycopy(data, 0, p, 1, data.size)
        return "0x08 0x0009 " + hex(p)
    }

    fun leSetAdvEnable(enable: Boolean): String {
        val flag = if (enable) "01" else "00"
        return "0x08 0x000A " + flag
    }

    fun buildManufacturerPayload(companyId: Int, data: ByteArray): ByteArray {
        val out = ByteArray(4 + data.size)
        out[0] = (out.size - 1).toByte()
        out[1] = 0xFF.toByte()
        out[2] = (companyId and 0xFF).toByte()
        out[3] = ((companyId shr 8) and 0xFF).toByte()
        System.arraycopy(data, 0, out, 4, data.size)
        return out
    }

    fun randomMac(): ByteArray {
        val mac = ByteArray(6)
        for (i in 0..5) {
            mac[i] = (Math.random() * 256).toInt().toByte()
        }
        mac[0] = (((mac[0].toInt() and 0xFE) or 0xC0)).toByte()
        return mac
    }
}package com.tutozz.blespam

/**
 * Билдеры HCI-команд для hcitool.
 * hcitool принимает: hcitool -i hciX cmd <OGF> <OCF> <params...> (все hex).
 */
object HciCommands {

    private fun hex(b: ByteArray): String =
        b.joinToString(" ") { "%02X".format(it) }

    /** LE Set Advertising Parameters (legacy). */
    fun leSetAdvParams(
        minInterval: Int = 0x20,
        maxInterval: Int = 0x20,
        advType: Int = 0x03,          // ADV_NONCONN_IND
        ownAddrType: Int = 0x01,      // random (для рандомизации MAC)
        channelMap: Int = 0x07,
        filterPolicy: Int = 0x00
    ): String {
        val p = ByteArray(15)
        p[0] = (minInterval and 0xFF).toByte(); p[1] = ((minInterval shr 8) and 0xFF).toByte()
        p[2] = (maxInterval and 0xFF).toByte(); p[3] = ((maxInterval shr 8) and 0xFF).toByte()
        p[4] = advType.toByte(); p[5] = ownAddrType.toByte()
        // peer addr + type = 7 байт нулей
        p[13] = channelMap.toByte(); p[14] = filterPolicy.toByte()
        return "0x08 0x0006 ${hex(p)}"
    }

    /** LE Set Random Address (для рандомизации MAC). */
    fun leSetRandomAddr(mac: ByteArray): String {
        require(mac.size == 6)
        val rev = ByteArray(6) { mac[5 - it] }
        return "0x08 0x0005 ${hex(rev)}"
    }

    /** LE Set Advertising Data — legacy, до 31 байта. */
    fun leSetAdvData(data: ByteArray): String {
        require(data.size <= 31) { "Legacy adv data max 31 bytes" }
        val p = ByteArray(32)
        p[0] = data.size.toByte()
        System.arraycopy(data, 0, p, 1, data.size)
        return "0x08 0x0008 ${hex(p)}"
    }

    /** LE Set Scan Response Data — legacy, до 31 байта. */
    fun leSetScanRespData(data: ByteArray): String {
        require(data.size <= 31)
        val p = ByteArray(32)
        p[0] = data.size.toByte()
        System.arraycopy(data, 0, p, 1, data.size)
        return "0x08 0x0009 ${hex(p)}"
    }

    /** LE Set Advertising Enable. */
    fun leSetAdvEnable(enable: Boolean): String =
        "0x08 0x000A ${if (enable) "01" else "00"}"

    /**
     * Формирует payload рекламы: [len][type=0xFF][companyId LE (2)][data...].
     */
    fun buildManufacturerPayload(companyId: Int, data: ByteArray): ByteArray {
        val out = ByteArray(4 + data.size)
        out[0] = (out.size - 1).toByte()
        out[1] = 0xFF.toByte()
        out[2] = (companyId and 0xFF).toByte()
        out[3] = ((companyId shr 8) and 0xFF).toByte()
        System.arraycopy(data, 0, out, 4, data.size)
        return out
    }

    fun randomMac(): ByteArray {
        val mac = ByteArray(6) { (Math.random() * 256).toInt().toByte() }
        mac[0] = ((mac[0].toInt() and 0xFE) or 0xC0).toByte() // locally-administered, not multicast
        return mac
    }
}
