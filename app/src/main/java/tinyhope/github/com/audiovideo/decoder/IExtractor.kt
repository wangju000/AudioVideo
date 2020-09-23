package tinyhope.github.com.audiovideo.decoder

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 *
 * @author WangJu
 *         2020/9/16
 *
 */
interface IExtractor {
    fun getFormat(): MediaFormat?
    fun readBuffer(byteBuffer: ByteBuffer): Int
    fun getCurrentTimestamp(): Long
    // seek 到指定位置，并返回实际帧的时间戳
    fun seek(pos: Long): Long
    fun setStartPos(pos: Long)

    fun stop()
}