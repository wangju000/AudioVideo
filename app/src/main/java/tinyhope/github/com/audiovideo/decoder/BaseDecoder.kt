package tinyhope.github.com.audiovideo.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 *
 * @author WangJu
 *         2020/9/16
 *
 */
abstract class BaseDecoder(private val mFilePath: String) : IDecoder {

    companion object {
        private const val TAG = "Decoder"
    }

    private var mIsRunning = true

    private var mLock = Object()

    private var mReadyForDecode = false

    //---------

    protected var mCodec: MediaCodec? = null

    protected var mExtractor: IExtractor? = null

    protected var mInputBuffers: Array<ByteBuffer>? = null

    protected var mOutputBuffers: Array<ByteBuffer>? = null

    private var mBufferInfo = MediaCodec.BufferInfo()

    private var mState = DecodeState.STOP

    protected var mStateListener: IDecodeStateListener? = null

    private var mIsEOS = false

    protected var mVideoWidth = 0

    protected var mVideoHeight = 0

    private var mDuration = 0L
    private var mEndPos = 0L

    final override fun run() {
        mState = DecodeState.START
        mStateListener?.decoderPrepare(this)

        // step 1: init and start decoder

        if (!init()) return

        while (mIsRunning) {
            if (mState != DecodeState.START &&
                mState != DecodeState.DECODING &&
                mState != DecodeState.SEEKING
            ) {
                waitDecode()
            }

            if (!mIsRunning || mState == DecodeState.STOP) {
                mIsRunning = false
                break
            }

            if (!mIsEOS) {
                // step 2: push data to buffer
                mIsEOS = pushBufferToDecoder()
            }

            // step 3: pull data from buffer
            val index = pullBufferFromDecoder()
            if (index >= 0) {
                // step 4: render
                render(mOutputBuffers!![index], mBufferInfo)

                // step 5: release buffer
                mCodec!!.releaseOutputBuffer(index, true)
                if (mState == DecodeState.START) {
                    mState = DecodeState.DECODING
                }
            }

            // step 6: 判断解码是否完成
            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                mState = DecodeState.FINISH
                mStateListener?.decoderFinish(this)
            }
        }

        doneDecode()

        // step 7: release decoder
        release()
    }

    private fun release() {
        try {
            mState = DecodeState.STOP
            mIsEOS = false
            mExtractor?.stop()
            mCodec?.release()
            mStateListener?.decoderDestroy(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pushBufferToDecoder(): Boolean {
        var inputBufferIndex = mCodec!!.dequeueInputBuffer(2000L)
        var isEndOfStream = false

        if (inputBufferIndex >= 0) {
            val inputBuffer = mInputBuffers!![inputBufferIndex]
            val sampleSize = mExtractor!!.readBuffer(inputBuffer)
            if (sampleSize < 0) {
                mCodec!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                isEndOfStream = true
            } else {
                mCodec!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    sampleSize,
                    mExtractor!!.getCurrentTimestamp(),
                    0
                )
            }
        }
        return isEndOfStream
    }

    private fun pullBufferFromDecoder(): Int {
        var index = mCodec!!.dequeueOutputBuffer(mBufferInfo, 1000L)

        when (index) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            }
            MediaCodec.INFO_TRY_AGAIN_LATER -> {
            }
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                mOutputBuffers = mCodec!!.outputBuffers
            }
            else -> {
                return index
            }
        }

        return -1
    }

    // 解码线程进入等待
    private fun waitDecode() {
        if (mState == DecodeState.PAUSE) {
            mStateListener?.decoderPause(this)
        }
        synchronized(mLock) {
            mLock.wait()
        }

    }

    protected fun notifyDecode() {
        synchronized(mLock) {
            mLock.notifyAll()
        }
        if (mState == DecodeState.DECODING) {
            mStateListener?.decoderRunning(this)
        }
    }


    private fun init(): Boolean {
        // 1. 检查参数
        if (mFilePath.isEmpty() || !File(mFilePath).exists()) {
            Log.w(TAG, "文件路径为空")
            mStateListener?.decoderError(this, "文件路径为空")
            return false
        }

        if (!check()) return false

        // 2. 初始化数据提取器
        mExtractor = initExtractor(mFilePath)
        if (mExtractor == null || mExtractor!!.getFormat() == null) return false

        // 3. 初始化参数
        if (!initParams()) return false

        // 4. 初始化渲染器
        if (!initRender()) return false

        // 5.初始化解码器
        if (!initCodec()) return false

        return true
    }

    private fun initParams(): Boolean {
        val format = mExtractor!!.getFormat()!!
        mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000
        if (mEndPos == 0L) mEndPos = mDuration

        initSpecParams(mExtractor!!.getFormat()!!)

        return true
    }

    private fun initCodec(): Boolean {
        try {// 1.初始化解码器
            val type = mExtractor!!.getFormat()!!.getString(MediaFormat.KEY_MIME)!!
            mCodec = MediaCodec.createDecoderByType(type)
            // 2.配置解码器
            if (!configCodec(mCodec!!, mExtractor!!.getFormat()!!)) {
                waitDecode()
            }

            // 3. 启动解码器
            mCodec!!.start()

            // 4.获取解码器缓冲区
            mInputBuffers = mCodec?.inputBuffers
            mOutputBuffers = mCodec?.outputBuffers
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    override fun pause() {
        mState = DecodeState.PAUSE
    }

    override fun goOn() {
        mState = DecodeState.DECODING
        notifyDecode()
    }

    override fun stop() {
        mState = DecodeState.STOP
        mIsRunning = false
        notifyDecode()
    }

    override fun isDecoding(): Boolean {
        return mState == DecodeState.DECODING
    }

    override fun isSeeking(): Boolean {
        return mState == DecodeState.SEEKING
    }

    override fun isStop(): Boolean {
        return mState == DecodeState.STOP
    }

    override fun setStateListener(l: IDecodeStateListener?) {
        this.mStateListener = l
    }

    override fun getWidth(): Int {
        return mVideoWidth
    }

    override fun getHeight(): Int {
        return mVideoHeight
    }

    override fun getDuration(): Int {
        return mDuration.toInt()
    }

    override fun getRotationAngle(): Int {
        return 0
    }

    override fun getFilePath(): String {
        return mFilePath
    }

    override fun getMediaFormat(): MediaFormat? {
        return mExtractor?.getFormat()
    }

    override fun getTrack(): Int {
        return 0
    }


    abstract fun render(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    abstract fun doneDecode()

    abstract fun check(): Boolean

    abstract fun initExtractor(path: String): IExtractor

    abstract fun initSpecParams(format: MediaFormat)

    abstract fun initRender(): Boolean

    abstract fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean
}