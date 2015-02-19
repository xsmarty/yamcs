package org.yamcs.tctm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.yamcs.archive.PacketWithTime;
import org.yamcs.utils.CcsdsPacket;
import org.yamcs.utils.TimeEncoding;

public class CcsdsStreamReader implements TmPacketStreamReader {
	DataInputStream din;
	//CCSDS 102.0-B-5 page 3-1
	static public int DEFAULT_MAX_LENGTH=65542;
	int maxLength = DEFAULT_MAX_LENGTH;
	
	public CcsdsStreamReader(InputStream in) {
		din=new DataInputStream(in);
	}
	
	/**
	 * Set the maximum length of the packets. The lengths are including the 6 bytes header.
	 * According to CCSDS standard, minimum packet length is 7, maximum is 65542 bytes which is the default.
	 * @param length
	 */
	public void setMaxLength(int length) {
		if((length<7) || (length>DEFAULT_MAX_LENGTH)) throw new IllegalArgumentException("max length shall be between 7 and "+ DEFAULT_MAX_LENGTH + " bytes");
		maxLength = length;
	}
	
	public int getMaxLength() {
		return maxLength;
	}
	
	
	@Override
	public PacketWithTime readPacket() throws IOException {
		byte hdr[] = new byte[6];
		din.readFully(hdr);
		int remaining=((hdr[4]&0xFF)<<8)+(hdr[5]&0xFF)+1;
		if(remaining>maxLength-6) throw new IOException("Remaining packet length too big: "+remaining+" maximum allowed is "+(maxLength-6));
		byte[] b = new byte[6+remaining];
		System.arraycopy(hdr, 0, b, 0, 6);
		din.readFully(b, 6, remaining);
		return new PacketWithTime(TimeEncoding.currentInstant(), CcsdsPacket.getInstant(b), b);
	}
}
