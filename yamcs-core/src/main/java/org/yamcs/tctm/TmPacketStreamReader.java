package org.yamcs.tctm;

import java.io.IOException;

import org.yamcs.archive.PacketWithTime;

/**
 * Reads streams of packets
 */
public interface TmPacketStreamReader {
	PacketWithTime readPacket() throws IOException;
}
