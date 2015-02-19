package org.yamcs.archive;

/**
 * packet with recording and generation time
 * The packet is just a byte array.
 * 
 * It implements comparable based on generation time
 * 
 * @author mache
 *
 */
public class PacketWithTime implements Comparable<PacketWithTime>{
    private long rectime;//reception time
    private long gentime; //generation time
    private byte[] pkt;

    public PacketWithTime(long rectime, long gentime, byte[] pkt) {
        this.rectime = rectime;
        this.gentime = gentime;
        this.pkt = pkt;
    }

    public long getGenerationTime() {
        return gentime;
    }
    
    public long getReceptionTime() {
        return rectime;
    }
    
    public byte[] getPacket() {
        return pkt;
    }

	@Override
	public int compareTo(PacketWithTime p) {
		return Long.compare(this.gentime, p.gentime);
	}
    
    
}