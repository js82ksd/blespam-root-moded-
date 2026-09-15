package com.tutozz.blespam

object HciCommands {

    private fun hex(b: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < b.size) {
            if (i > 0) sb.append(' ')
            val v = b[i].toInt() and 0xFF
            if (v < 16) sb.append('0')
            sb.append(Integer.toHexString(v).uppercase())
            i++
        }
        return sb.toString()
    }

    fun leSetAdvParams(
        minInterval: Int,
        maxInterval: Int,
        advType: Int,
        ownAddrType: Int,
        channelMap: Int,
        filterPolicy: Int
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
        val rev = ByteArray(6)
        var i = 0
        while (i < 6) {
            rev[i] = mac[5 - i]
            i++
        }
        return "0x08 0x0005 " + hex(rev)
    }

    fun leSetAdvData(data: ByteArray): String {
        val p = ByteArray(32)
        p[0] = data.size.toByte()
        System.arraycopy(data, 0, p, 1, data.size)
        return "0x08 0x0008 " + hex(p)
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
        var i = 0
        while (i < 6) {
            mac[i] = (Math.random() * 256).toInt().toByte()
            i++
        }
        mac[0] = ((mac[0].toInt() and 0xFE) or 0xC0).toByte()
        return mac
    }
}
