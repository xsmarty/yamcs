package org.yamcs.tctm;

import org.yamcs.protobuf.Yamcs.ReplayRequest;
import org.yamcs.protobuf.Yamcs.ReplaySpeed;
import org.yamcs.protobuf.Yamcs.ReplayStatus.ReplayState;

public interface ArchiveTmPacketProvider extends TmPacketProvider {

    public abstract void seek(long time);
    public abstract void pause();
    public abstract void resume();
    
    public ReplayState getReplayState();
    
    public ReplaySpeed getSpeed();

    public abstract ReplayRequest getReplayRequest();
}