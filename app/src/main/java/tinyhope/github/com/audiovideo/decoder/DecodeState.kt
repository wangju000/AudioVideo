package tinyhope.github.com.audiovideo.decoder

/**
 *
 * @author WangJu
 *         2020/9/16
 *
 */
enum class DecodeState {
    START,
    DECODING,
    PAUSE,
    SEEKING,
    FINISH,
    STOP
}