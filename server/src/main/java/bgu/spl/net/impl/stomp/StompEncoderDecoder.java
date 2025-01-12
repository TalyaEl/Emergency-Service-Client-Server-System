package bgu.spl.net.impl.stomp;

import java.io.Serializable;
import java.nio.ByteBuffer;

import bgu.spl.net.api.MessageEncoderDecoder;

public class StompEncoderDecoder implements MessageEncoderDecoder<Serializable> {
    // private final ByteBuffer buf;

    public Serializable decodeNextByte(byte nextByte) {
        return null;
    }

    public byte[] encode(Serializable message) {
        return null;
    }
}
