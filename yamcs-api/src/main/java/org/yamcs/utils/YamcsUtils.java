package org.yamcs.utils;

import java.util.List;

import org.yamcs.api.Protocol;
import org.yamcs.api.YamcsClient;
import org.yamcs.api.YamcsSession;
import org.yamcs.protobuf.Yamcs.YamcsInstance;
import org.yamcs.protobuf.Yamcs.YamcsInstances;

/**
 * Collection of utility methods 
 *
 */
public class YamcsUtils {

    public static List<YamcsInstance> getInstanceList(String url) throws Exception {
        YamcsSession ys = null;
        YamcsClient msgClient = null;
        try {
            ys = YamcsSession.newBuilder().setConnectionParams(url).build();
            msgClient = ys.newClientBuilder().setRpc(true).build();
            YamcsInstances ainst = (YamcsInstances)msgClient.executeRpc(Protocol.YAMCS_SERVER_CONTROL_ADDRESS, "getYamcsInstances", null, YamcsInstances.newBuilder());
            return ainst.getInstanceList();
        } finally {
            if (msgClient != null) {
                msgClient.close();
            }
            if (ys != null) {
                ys.close();
            }
        }
    }
	/**
	 * Get list of yamcs instances
	 * @param host Hostname of IP address
	 * @param port Port number
	 * @return List of running instances
	 * @throws Exception
	 */
	public static List<YamcsInstance> getInstanceList(String host, int port) throws Exception {
	    return getInstanceList("yamcs://"+host+":"+port);
	}
	
	public static void main(String[] args) throws Exception {
		getInstanceList("aces-eds", 5445);
	}
}
