package tinyhope.github.com.audiovideo.decoder

import android.media.MediaFormat

/**
 *
 * @author WangJu
 *         2020/9/16
 *
 */
interface IDecoder : Runnable {
    fun pause()
    fun goOn()
    fun stop()
    fun isDecoding(): Boolean
    fun isSeeking(): Boolean
    fun isStop(): Boolean
    fun setStateListener(l: IDecodeStateListener?)
    fun getWidth(): Int
    fun getHeight(): Int
    fun getDuration(): Int
    fun getRotationAngle(): Int
    fun getMediaFormat(): MediaFormat?
    fun getTrack(): Int
    fun getFilePath(): String
}