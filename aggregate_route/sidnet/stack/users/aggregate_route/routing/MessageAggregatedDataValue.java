/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.aggregate_route.routing;

import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import sidnet.core.misc.NCS_Location2D;

/**
 *
 * @author invictus
 */
public class MessageAggregatedDataValue implements Message {

    public String tipeSensor;

    public double maxValue;
    public double minValue;
    public double averageValue;

    public long totalValueAggregated;

    public int priorityLevel;

    public int queryID;

    public int fromRegion;

    public int aggregatorNodeID;
    public NCS_Location2D aggregatorNodeLocation;

    public NetAddress sinkIP;
    public NCS_Location2D sinkLocation;

    public MessageAggregatedDataValue () {

    }

    public int getSize() {
        return 17;
    }

    public void getBytes(byte[] msg, int offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
