@file:JvmName("Bodies")
package net.aquadc.lychee.http

import net.aquadc.lychee.http.param.Body
import java.io.ByteArrayInputStream
import java.io.InputStream


fun Stream(mediaType: CharSequence): Body<() -> InputStream> =
    object : Body<() -> InputStream>(mediaType) {
        override fun stream(value: () -> InputStream): InputStream =
            value.invoke()
        override fun fromStream(estimateSize: Long, stream: InputStream): () -> InputStream =
            object : () -> InputStream {
                var consumed = false
                override fun invoke(): InputStream {
                    if (consumed) throw IllegalStateException("The stream is already consumed")
                    consumed = true
                    return stream
                }
            }
    }

fun Bytes(mediaType: CharSequence): Body<ByteArray> =
    object : Body<ByteArray>(mediaType) {
        override fun contentLength(value: ByteArray): Long =
            value.size.toLong()
        override fun stream(value: ByteArray): InputStream =
            ByteArrayInputStream(value)
        override fun fromStream(estimateSize: Long, stream: InputStream): ByteArray {
            val len =
                if (estimateSize > 0 && estimateSize < Int.MAX_VALUE) estimateSize.toInt()
                else DEFAULT_BUFFER_SIZE
            return stream.use { input ->
                input.readBytes(len)
            }
        }
    }
