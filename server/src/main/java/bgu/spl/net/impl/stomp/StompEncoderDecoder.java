package bgu.spl.net.impl.stomp;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.stomp.Frame.StompFrameAbstract;

public class StompEncoderDecoder implements MessageEncoderDecoder<StompFrameAbstract> {
    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    private StompFrameAbstract currentFrame = null;

    @Override
    public StompFrameAbstract decodeNextByte(byte nextByte) {
        if (nextByte == '\0') { //if we're at the end of the frame
            if (currentFrame != null) { //and also if the frame we created isn't null
                StompFrameAbstract ansFrame = currentFrame;
                len = 0;
                currentFrame = null;
                return ansFrame;
            }
        }
        else {
            pushByte(nextByte);
        }
        return null;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            byte[] newBytes = new byte[bytes.length * 2];
            for (int i = 0; i <bytes.length; i++) {
                newBytes[i] = bytes[i];
            }
            bytes = newBytes;
        }
        bytes[len++] = nextByte;
    }

    @Override
    public byte[] encode(StompFrameAbstract message) {
        return null;
    }
}
