package tinyhope.github.com.audiovideo.decoder

/**
 *
 * @author WangJu
 *         2020/9/16
 *
 */
interface IDecodeStateListener {
    fun decoderPrepare(decoder: IDecoder)
    fun decoderFinish(decoder: IDecoder)
    fun decoderPause(decoder: IDecoder)
    fun decoderRunning(decoder: IDecoder)
    fun decoderError(decoder: IDecoder, error: String)
    fun decoderDestroy(decoder: IDecoder)

}