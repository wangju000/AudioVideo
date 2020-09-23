package tinyhope.github.com.audiovideo.decoder

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 *
 * @author WangJu
 *         2020/9/17
 *
 */
class MMExtractor(path: String) {
    private var mExtractor: MediaExtractor? = null

    // 音频通道索引
    private var mAudioTrack = -1

    // 视频通道索引
    private var mVideoTrack = -1

    // 当前帧时间戳
    private var mCurSampleTime = 0L

    // 开始解码时间点
    private var mStartPos = 0L

    init {
        mExtractor = MediaExtractor()
        mExtractor?.setDataSource(path)
    }

    fun getVideoFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video/")) {
                mVideoTrack = i
                break
            }
        }
        return if (mVideoTrack >= 0) mExtractor!!.getTrackFormat(mVideoTrack) else null
    }

    fun getAudioFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("audio/")) {
                mAudioTrack = i
                break
            }
        }
        return if (mAudioTrack >= 0) mExtractor!!.getTrackFormat(mAudioTrack) else null
    }

    fun readBuffer(byteBuffer: ByteBuffer): Int {
        byteBuffer.clear()
        selectSourceTrack()
        val readSampleCount = mExtractor!!.readSampleData(byteBuffer, 0)
        if (readSampleCount < 0) {
            return -1
        }
        mCurSampleTime = mExtractor!!.sampleTime
        mExtractor!!.advance()
        return readSampleCount
    }

    private fun selectSourceTrack() {
        if (mVideoTrack >= 0) {
            mExtractor!!.selectTrack(mVideoTrack)
        } else if (mAudioTrack >= 0) {
            mExtractor!!.selectTrack(mAudioTrack)
        }
    }

    fun seek(pos: Long): Long {
        mExtractor!!.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        return mExtractor!!.sampleTime
    }

    fun stop() {
        mExtractor?.release()
        mExtractor = null
    }

    fun getVideoTrack(): Int {
        return mVideoTrack
    }

    fun getAudioTrack(): Int {
        return mAudioTrack
    }

    fun setStartPos(pos: Long) {
        mStartPos = pos
    }

    fun getCurrentTimestamp(): Long {
        return mCurSampleTime
    }
}