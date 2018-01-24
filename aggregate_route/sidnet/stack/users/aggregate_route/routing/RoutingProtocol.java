/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.aggregate_route.routing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.net.NetMessage.Ip;
import sidnet.core.interfaces.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.route.RouteInterface;
import sidnet.colorprofiles.ColorProfileGeneric;
import sidnet.core.gui.TopologyGUI;
import sidnet.stack.users.aggregate_route.colorprofile.ColorProfileAggregate;
import sidnet.core.misc.NCS_Location2D;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.Reason;
import sidnet.stack.users.aggregate_route.app.DropperNotifyAppLayer;
import sidnet.stack.users.aggregate_route.app.MessageDataValue;
import sidnet.stack.users.aggregate_route.app.MessageQuery;
import sidnet.stack.users.aggregate_route.driver.DeliveryRatioCounter;



/**
 *
 * @author invictus
 */
public class RoutingProtocol implements RouteInterface.AggregateRoute {
    public static final byte ERROR = -1;
    public static final byte SUCCESS = 0;

    /*
     * Konstanta variable pengaturan routing
     * modifikasi value disini aja
     */
    private static final long MINIMUM_AGGREGATE_DATA_PRIORITY_1 = 5;
    private static final long MINIMUM_AGGREGATE_DATA_PRIORITY_2 = 3;
    private static final long MINIMUM_AGGREGATE_DATA_PRIORITY_3 = 1;
    private static final long INTERVAL_TIMING_SEND = 5 * Constants.SECOND;

    private static final int MAXIMUM_RETRY_SEND_MESSAGE = 0;
    private static final long INTERVAL_WAITING_BEFORE_RETRY = 10 * Constants.SECOND;

    private static final int LIMIT_PACKET_ID_SIZE = 500;

    

    private final Node myNode; // The SIDnet handle to the node representation

    private boolean netQueueFULL = false;

    //Showing topology
    public static TopologyGUI topologyGUI = null;

    // entity hook-up (network stack)
    /** Network entity. */
    private NetInterface netEntity;

    /** Self-referencing proxy entity. */
    private RouteInterface self;

    /** The proxy-entity for this application interface */
    private AppInterface appInterface;

    // DO NOT MAKE THIS STATIC
    private ColorProfileAggregate colorProfileGeneric = new ColorProfileAggregate();

    //anti-duplicate list
    ArrayList<String> receivedDataId = new ArrayList<String>();

    //hashmap hitung maksimum retry
    HashMap<Long, Integer> dataRetry = new HashMap<Long, Integer>();

    //list dari node ini proses query apa aj mana saja
    private ArrayList<Integer> queryProcessed = new ArrayList<Integer>();

    //hashmap key:queryid item:sinkIP,sinkLocation
    private class DestinationSink {
        public NetAddress sinkIP;
        public NCS_Location2D sinkLocation;
        public int regionProcessed;

        public DestinationSink (NetAddress sinkIP, NCS_Location2D sinkLocation, int regionProcessed) {
            this.sinkIP = sinkIP;
            this.sinkLocation = sinkLocation;
            this.regionProcessed = regionProcessed;
        }
    }
    private HashMap<Integer, DestinationSink> detailQueryProcessed = new HashMap<Integer, DestinationSink>();

    //node entry discover hashmap, keperluan penentuan my cluster head
    private HashMap<NetAddress, NodeEntryDiscovery> listTetangga = new HashMap<NetAddress, NodeEntryDiscovery>();

    /*Pool received, key hashmap "QUERYID-P:PRIORITY-TIPESENSOR"
     * contoh QUERYID = 10; PRIORITY=3; TIPESENSOR=SENSOR-SUHU
     * jadinya 10-P:3-SENSOR-SUHU (STRING tipe)
     */
    
    private class poolReceivedItem {
        public String tipeSensor;
        public double maxValue;
        public double minValue;
        public double averageValue;
        public long totalValueAggregated;
        public int priorityLevel;
        public int queryID;
        public int fromRegion;

        public poolReceivedItem() {
            this.tipeSensor = "UNKNOWN";
            this.maxValue = -9999;
            this.minValue = -9999;
            this.averageValue = -9999;
            this.totalValueAggregated = 0;
            this.priorityLevel = 0;
            this.queryID = 0;
            this.fromRegion = 0;
        }

        public void putMaxValue (double MaxValue) {
            if (this.maxValue < MaxValue) {
                this.maxValue = MaxValue;
                return;
            }
            if (this.maxValue == -9999) {
                this.maxValue = MaxValue;
                return;
            }
        }

        public void putMinValue (double MinValue) {
            if (this.minValue > MinValue) {
                this.minValue = MinValue;
                return;
            }

            if (this.minValue == -9999) {
                this.minValue = MinValue;
                return;
            }
        }

        public void putAverageValue (double AverageValue) {
            if (this.averageValue == -9999) {
                this.averageValue = AverageValue;
                return;
            } else {
                double tmpAvg = this.averageValue + AverageValue;
                tmpAvg = tmpAvg / 2;
                this.averageValue = tmpAvg;
            }
        }

        public void putTotalAggregatedValue(long Total) {
            this.totalValueAggregated = this.totalValueAggregated + Total;
        }
    }
    private Map<String, poolReceivedItem> rcvPool = new HashMap<String, poolReceivedItem>();
    private boolean rcvPoolIsLock = false; //jika rcvPool dikunci, tidak boleh diakses
    private ArrayList<poolReceivedItem> lstItemPool = new ArrayList<poolReceivedItem>();

    /*
     * Inisialisasi Routing protocol
     */
    public RoutingProtocol(Node myNode) {
        this.myNode = myNode;

        self = (RouteInterface.AggregateRoute)JistAPI.proxy(this, RouteInterface.AggregateRoute.class);

        this.rcvPoolIsLock = false;
        
    }


    /*
     * SWANS Network Hooks up
     *
     */
    public void setNetEntity(NetInterface netEntity)
    {
        if(!JistAPI.isEntity(netEntity)) throw new IllegalArgumentException("expected entity");
        if(this.netEntity!=null) throw new IllegalStateException("net entity already set");

        this.netEntity = netEntity;
    }
    public RouteInterface getProxy()
    {
        return self;
    }
    public void setAppInterface(AppInterface appInterface)
    {
        this.appInterface = appInterface;
    }

    /*
     * Aggregate Route Protocol Self Recursive
     * Using Jist Call, modify RouteInterface Class
     * Threading in simulator
     */
    public void timingSend(long interval) {
        JistAPI.sleepBlock(interval);

        if (!rcvPool.isEmpty()) {
            
            lstItemPool.clear();

            Comparator priorityComp = new Comparator<poolReceivedItem>(){
                public int compare(poolReceivedItem o1, poolReceivedItem o2) {
                    return o2.priorityLevel - o1.priorityLevel;
                }
            };
            
            Iterator i = rcvPool.entrySet().iterator();

            while (i.hasNext()) {
               Entry item = (Entry) i.next();
               poolReceivedItem pri = (poolReceivedItem)item.getValue();

               if ((pri.priorityLevel == 1) &&
                       (pri.totalValueAggregated >= this.MINIMUM_AGGREGATE_DATA_PRIORITY_1)) {
                   i.remove();
                   lstItemPool.add(pri);
               } else if ((pri.priorityLevel == 2) &&
                       (pri.totalValueAggregated >= this.MINIMUM_AGGREGATE_DATA_PRIORITY_2)) {
                   i.remove();
                   lstItemPool.add(pri);
               } else if ((pri.priorityLevel == 3) &&
                       (pri.totalValueAggregated >= this.MINIMUM_AGGREGATE_DATA_PRIORITY_3)) {
                   i.remove();
                   lstItemPool.add(pri);
               }
            }

            //System.out.println("UNSORTED QUEUE");

            Collections.sort(lstItemPool, priorityComp);

            //System.out.println("SORTED QUEUE");

            for (poolReceivedItem pri : lstItemPool) {
                MessageAggregatedDataValue madv = new MessageAggregatedDataValue();
                madv.averageValue = pri.averageValue;
                madv.fromRegion = pri.fromRegion;
                madv.maxValue = pri.maxValue;
                madv.minValue = pri.minValue;
                madv.priorityLevel = pri.priorityLevel;
                madv.queryID = pri.queryID;
                madv.tipeSensor = pri.tipeSensor;
                madv.totalValueAggregated = pri.totalValueAggregated;

                madv.aggregatorNodeID = myNode.getID();
                madv.aggregatorNodeLocation = myNode.getNCS_Location2D();

                madv.sinkIP = detailQueryProcessed.get(pri.queryID).sinkIP;
                madv.sinkLocation = detailQueryProcessed.get(pri.queryID).sinkLocation;

                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(madv);
                String unikID = String.valueOf(myNode.getID()) + String.valueOf(JistAPI.getTime());
                pmw.setS_seq(Long.valueOf(unikID));

                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), detailQueryProcessed.get(madv.queryID).sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                NetAddress nextHop = getNextHopToSink(madv.sinkIP, madv.sinkLocation);
                sendToLinkLayer(nmip, nextHop);

            }

        }
        
        ((RouteInterface.AggregateRoute)self).timingSend(interval);
    }
    public void selfMonitor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*
     * Abstract Implementation RouteInterface
     */
    public void peek(NetMessage msg, MacAddress lastHop) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public void send(NetMessage msg) {
        //Reject pesan jika energy yang tersisa kurang dari 2%
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;

        //Reject semua pesan yang diterima jika tipe pesan tidak dikenali
        if (!(((NetMessage.Ip)msg).getPayload() instanceof ProtocolMessageWrapper))
            return;

        //node fail system
        if (this.netQueueFULL)
            return;

        //set visual warna node
        myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,ColorProfileAggregate.RECEIVE, 2);

        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper sndMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();

        if (sndMsg.getPayload() instanceof MessageQuery) {
            //tipe pesan query, cek apakah pesan query sudah pernah diterima sebelumnya
            //jika belum berikan ke fungsi handleMessageQuery
            //jika sudah abaikan
            if (!receivedDataId.contains(String.valueOf(sndMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(sndMsg.getS_seq()));
                memoryControllerPacketID();
                handleMessageQuery(sndMsg);

            }
        } else if (sndMsg.getPayload() instanceof MessageDataValue) {
            //tipe pesan ini diteruskan ke fungsi handleMessageDataValue
            //if (!receivedDataId.contains(String.valueOf(sndMsg.getS_seq()))) {
            //    receivedDataId.add(String.valueOf(sndMsg.getS_seq()));
            //    memoryControllerPacketID();
                handleMessageDataValue((MessageDataValue)sndMsg.getPayload());
            //}
        } else if (sndMsg.getPayload() instanceof MessageAggregatedDataValue) {
            //tipe pesan aggregate diteruskan ke fungsi handleMessageAggregatedDataValue
            //if (!receivedDataId.contains(String.valueOf(sndMsg.getS_seq()))) {
            //    receivedDataId.add(String.valueOf(sndMsg.getS_seq()));
            //    memoryControllerPacketID();
                handleMessageAggregatedDataValue(msg);
            //}
        }
    }
    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl) {
        //Reject pesan jika energy yang tersisa kurang dari 2%
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;

        //Reject semua pesan yang diterima jika tipe pesan tidak dikenali
        if (!(msg instanceof ProtocolMessageWrapper))
            return;

        //node fail system
        if (this.netQueueFULL)
            return;

        //set visual warna node
        myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,ColorProfileAggregate.RECEIVE, 2);

        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper rcvMsg = (ProtocolMessageWrapper)msg;

        //Operasi sesuai dengan tipe pesan
        if (rcvMsg.getPayload() instanceof MessageNodeDiscover) {
            //tipe pesan discovery, berikan ke fungsi handle MessageNodeDiscover
            //System.out.println("Node " + myNode.getID() + " got node info from Node " + ((MessageNodeDiscover)rcvMsg.getPayload()).nodeID);
            handleMessageNodeDiscover((MessageNodeDiscover)rcvMsg.getPayload());
        } else if (rcvMsg.getPayload() instanceof MessageQuery) {
            //tipe pesan query, cek apakah pesan query sudah pernah diterima sebelumnya
            //jika belum berikan ke fungsi handleMessageQuery
            //jika sudah abaikan
            if (!receivedDataId.contains(String.valueOf(rcvMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(rcvMsg.getS_seq()));
                memoryControllerPacketID();
                handleMessageQuery(rcvMsg);
            }
        } else if (rcvMsg.getPayload() instanceof MessageAggregatedDataValue) {
            //jika pesan aggregate duplicate, abaikan
            if (this.receivedDataId.contains(String.valueOf(rcvMsg.getS_seq())))
                return;
            receivedDataId.add(String.valueOf(rcvMsg.getS_seq()));
            memoryControllerPacketID();

            //tipe pesan data value yang sudah diaggregate
            //bagian ini biasanya dipanggil jika pesan ini diterima pada sink node
            //lempar ke layer app
            //sendToAppLayer(rcvMsg.getPayload(), src);

            handleAggregatedValueOnSink((MessageAggregatedDataValue)rcvMsg.getPayload());
        }

    }
    public void dropNotify(Message msg, MacAddress nextHopMac, Reason reason) {
        
    	if (reason == Reason.PACKET_SIZE_TOO_LARGE) {
    		System.out.println("NODE:" + myNode.getID() +" WARNING: Packet size too large - unable to transmit");
    		throw new RuntimeException("Packet size too large - unable to transmit");
    	}
        if (reason == Reason.NET_QUEUE_FULL) {
            if (!this.netQueueFULL) {
                this.netQueueFULL = true;
                System.out.println("ERROR: Net Queue full node" + myNode.getID() + " TIME (SEC): " + (JistAPI.getTime() / Constants.SECOND));
                myNode.getNodeGUI().colorCode.mark(new ColorProfileGeneric(), ColorProfileGeneric.DEAD, ColorProfileGeneric.FOREVER);
                //throw new RuntimeException("Net Queue Full");
            }
        }
        if (reason == Reason.UNDELIVERABLE || reason == Reason.MAC_BUSY) {
            ProtocolMessageWrapper xMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();
            System.out.println("NODE:" + myNode.getID() + " WARNING: Cannot relay packet " + xMsg.getS_seq() + " to the destination node " + nextHopMac);
            
            //cek apakah batas retry masih bisa atau tidak
            if (isThisRetryAgain(xMsg.getS_seq())) {

                increaseThisRetry(xMsg.getS_seq());
                System.out.println("NODE:" + myNode.getID() + " Retrying(" + dataRetry.get(xMsg.getS_seq()) + ") PID:" + xMsg.getS_seq() + " send to node " + nextHopMac);

                //sleep before retry
                JistAPI.sleepBlock(this.INTERVAL_WAITING_BEFORE_RETRY);

                NetMessage.Ip nmip = (NetMessage.Ip) msg;

                if (xMsg.getPayload() instanceof MessageDataValue) {
                    //beritahu app layer agar menambah window aggregation
                    DropperNotifyAppLayer dnal = new DropperNotifyAppLayer(false, true);
                    sendToAppLayer(dnal, myNode.getIP());
                }
                NetAddress retryTo = convertMacToIP(nextHopMac);
                if (retryTo != null)
                    sendToLinkLayer(nmip, retryTo);
                else {
                    
                }
            } else {
                System.out.println("NODE:" + myNode.getID() + " Drop packet " + xMsg.getS_seq() + " after retry(" + dataRetry.get(xMsg.getS_seq()) + ") to node " + nextHopMac);
                deleteThisRetry(xMsg.getS_seq());
            }

        }

        if (reason == Reason.PACKET_DELIVERED) {
            ProtocolMessageWrapper xMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();
            if (xMsg.getPayload() instanceof MessageDataValue) {
                //beritahu app layer agar menngurangi window aggregation
                DropperNotifyAppLayer dnal = new DropperNotifyAppLayer(true, false);
                sendToAppLayer(dnal, myNode.getIP());
            }
            deleteThisRetry(xMsg.getS_seq());
        }
    }
    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*
     * Send to layer app or link
     */
    public void sendToAppLayer(Message msg, NetAddress src)
    {
    	// ignore if not enough energy
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;

        appInterface.receive(msg, src, null, (byte)-1,
        					 NetAddress.LOCAL, (byte)-1, (byte)-1);
    }
    public byte sendToLinkLayer(NetMessage.Ip ipMsg, NetAddress nextHopDestIP)
    {
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return 0;

        /*myNode.getSimManager().getSimGUI()
		  .getAnimationDrawingTool()
		 	  .animate("ExpandingFadingCircle",
				       myNode.getNCS_Location2D());*/
        ProtocolMessageWrapper pmw = (ProtocolMessageWrapper)ipMsg.getPayload();
        NetMessage.Ip copyMsg = new NetMessage.Ip(pmw,
			       ((NetMessage.Ip)ipMsg).getSrc(),
                               ((NetMessage.Ip)ipMsg).getDst(),
                               ((NetMessage.Ip)ipMsg).getProtocol(),
                               ((NetMessage.Ip)ipMsg).getPriority(),
                               ((NetMessage.Ip)ipMsg).getTTL(),
                               ((NetMessage.Ip)ipMsg).getId(),
                               ((NetMessage.Ip)ipMsg).getFragOffset());
        ipMsg = null;

        if (nextHopDestIP == null)
            System.err.println("NULL nextHopDestIP");
        if (nextHopDestIP == NetAddress.ANY) {
            netEntity.send(copyMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
            registerIDToHashmapRetry(pmw.getS_seq());
        }
        else
        {
            NodeEntry nodeEntry = myNode.neighboursList.get(nextHopDestIP);
            if (nodeEntry == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 new Exception().printStackTrace();
                 return ERROR;
            }
            MacAddress macAddress = nodeEntry.mac;
            if (macAddress == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 return ERROR;
            }
            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileAggregate.TRANSMIT, 2);

            netEntity.send(copyMsg, Constants.NET_INTERFACE_DEFAULT, macAddress);
            registerIDToHashmapRetry(pmw.getS_seq());
        }

        return SUCCESS;
    }


    /*
     * Aggregate Route Function Here
     */

    private NetAddress convertMacToIP(MacAddress macAddr) {
        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();

        for(NodeEntry nodeEntry: neighboursLinkedList) {
            if (nodeEntry.mac.hashCode() == macAddr.hashCode())
                return nodeEntry.ip;
        }

        return null;
    }

    private void handleMessageNodeDiscover(MessageNodeDiscover msg) {
        NodeEntryDiscovery ned = new NodeEntryDiscovery(msg.nodeID, msg.ipAddress, msg.totalDiscoveredNode, msg.energyLeft);
        ned.addQueryProcessed(msg.queryProcessed);
        listTetangga.put(msg.ipAddress, ned);
    }

    private void handleMessageQuery(ProtocolMessageWrapper msg) {
        /*
         * Ketika mendapatkan query message, node memeriksa apakah dia masuk
         * didalam region tersebut, jika iya maka node akan broadcast
         * ke tetangga bahwa ia masuk dalam region baru
         */

        MessageQuery query = (MessageQuery)msg.getPayload();

        if (query.getQuery().getSinkIP().hashCode() != myNode.getIP().hashCode())
            if (query.getQuery().getRegion().isInside(myNode.getNCS_Location2D())) {
                //System.out.println("Node " + myNode.getID() + " is inside region " + String.valueOf(query.getQuery().getRegion().getID()));

                //start timing send
                ((RouteInterface.AggregateRoute)self).timingSend(this.INTERVAL_TIMING_SEND);

                this.queryProcessed.add(query.getQuery().getID());
                DestinationSink ds = new DestinationSink(query.getQuery().getSinkIP(), query.getQuery().getSinkNCSLocation2D(), query.getQuery().getRegion().getID());
                this.detailQueryProcessed.put(query.getQuery().getID(), ds);

                MessageNodeDiscover mnd = new MessageNodeDiscover(myNode.getID(), myNode.getIP(), myNode.neighboursList.size(), myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel());                
                mnd.addQueryProcessed(this.queryProcessed);

                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(mnd);
                String unikID = String.valueOf(myNode.getID()) + String.valueOf(JistAPI.getTime());
                pmw.setS_seq(Long.valueOf(unikID));
                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), NetAddress.ANY, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                sendToLinkLayer(nmip, NetAddress.ANY);

                //setelah di broadcast, query diteruskan ke app layer
                sendToAppLayer(query, null);
            }
        
        //give a breath
        //JistAPI.sleepBlock(2 * Constants.SECOND);
        
        //sebarkan query
        //System.out.println("Node " + myNode.getID() + " broadcasting query.");
        NetMessage.Ip nmip = new NetMessage.Ip(msg, myNode.getIP(), NetAddress.ANY, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
        sendToLinkLayer(nmip, NetAddress.ANY);

    }

     private void handleMessageDataValue(MessageDataValue msg) {
        //mendapatkan pesan dengan jenis MessageDataValue terdapat 2 kemungkinan
        //jika pesan berasal dari layer atas, maka akan dikirimkan ke cluster head
        //jika pesan berasal dari node lain, maka data value ditampung pada pool

        if (msg.producerNodeId == myNode.getID()) {
            //tambahkan informasi region
            msg.fromRegion = detailQueryProcessed.get(msg.queryId).regionProcessed;

            //cari cluster head
            NetAddress myClusterHead = getMyClusterHead(msg.queryId);
            
            //jika cluster headnya diri sendiri maka masukan ke pool agar diteruskan
            //ke sink dengan timingSend()
            if (myClusterHead.hashCode() == myNode.getIP().hashCode()) {
                //System.out.println("Node " + myNode.getID() + " memasukan data value sendiri ke pool");
                poolHandleMessageDataValue(msg);
            }
            //jika tidak maka kirim ke cluster head
            else {
                //System.out.println("Node " + myNode.getID() + " mengirim data ke clusterhead " + myClusterHead);
                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(msg);
                String unikID = String.valueOf(myNode.getID()) + String.valueOf(JistAPI.getTime());
                pmw.setS_seq(Long.valueOf(unikID));
                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), detailQueryProcessed.get(msg.queryId).sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                sendToLinkLayer(nmip, myClusterHead);
            }

            
        } else {
            topologyGUI.addLink(msg.producerNodeId, myNode.getID(), 0, Color.RED, TopologyGUI.HeadType.LEAD_ARROW);
            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileAggregate.CLUSTERHEAD, 5000);
            
            //System.out.println("Node " + myNode.getID() + " memasukan data value node:" + msg.producerNodeId + " ke pool");

            poolHandleMessageDataValue(msg);

            
        }
    }

    private void poolHandleMessageDataValue(MessageDataValue msg) {
        //buat keyHashMap
        String hashMapKey = String.valueOf(msg.queryId) + "-P:" + String.valueOf(msg.priorityLevel) + "-" + msg.tipeSensor;

        //cek jika sudah terdapat
        if (rcvPool.containsKey(hashMapKey)) {
            //sudah terdapat key yang sama, lakukan aggregate dengan data sebelumnya

            //System.out.println("Node:" + myNode.getID() + " aggregated hashmapkey:" + hashMapKey);

            poolReceivedItem priNew = new poolReceivedItem();
            poolReceivedItem priLast = rcvPool.get(hashMapKey);

            priNew.tipeSensor = priLast.tipeSensor;
            priNew.fromRegion = priLast.fromRegion;
            priNew.priorityLevel = priLast.priorityLevel;
            priNew.queryID = priLast.queryID;

            priNew.putAverageValue(priLast.averageValue);
            priNew.putMaxValue(priLast.maxValue);
            priNew.putMinValue(priLast.minValue);
            priNew.putTotalAggregatedValue(priLast.totalValueAggregated);

            priNew.putAverageValue(msg.dataValue);
            priNew.putMaxValue(msg.dataValue);
            priNew.putMinValue(msg.dataValue);
            priNew.putTotalAggregatedValue(1);

            rcvPool.put(hashMapKey, priNew);
            return;


        } else {
            //tidak terdapat key
            //bikin entry queue langsung masukan

            //System.out.println("Node:" + myNode.getID() + " create new hashmapkey:" + hashMapKey);

            poolReceivedItem priNew = new poolReceivedItem();
            priNew.tipeSensor = msg.tipeSensor;
            priNew.fromRegion = msg.fromRegion;
            priNew.priorityLevel = msg.priorityLevel;
            priNew.queryID = msg.queryId;

            priNew.putAverageValue(msg.dataValue);
            priNew.putMaxValue(msg.dataValue);
            priNew.putMinValue(msg.dataValue);
            priNew.putTotalAggregatedValue(1);

            rcvPool.put(hashMapKey, priNew);
            return;
        }
    }

    private NetAddress getMyClusterHead(int queryID) {

        List<NetAddress> lstNetAddr = new ArrayList<NetAddress>();

        //awalnya node akan mengganggap dirinya adalah cluster head, memiliki
        //banyak tetangga
        int maxDiscoveredNode = myNode.neighboursList.size();
        NetAddress selectedClusterHead = myNode.getIP();

        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();

        //periksa setiap tetangga yang memproses queryID yang sama
        //jika ada yang sama kumpulkan di LiistNetAddress
        for(NodeEntry nodeEntry: neighboursLinkedList) {
            if (listTetangga.containsKey(nodeEntry.ip))
                if (listTetangga.get(nodeEntry.ip).queryProcessed.contains(queryID))
                    lstNetAddr.add(nodeEntry.ip);
        }

        //cari tetangga yang memiliki tetangga paling banyak
        if (!lstNetAddr.isEmpty())
            for (NetAddress na: lstNetAddr) {;
                if (maxDiscoveredNode < listTetangga.get(na).totalDiscoveredNode) {
                    maxDiscoveredNode = listTetangga.get(na).totalDiscoveredNode;
                    selectedClusterHead = na;
                }
            }

        return selectedClusterHead;
    }

    private NetAddress getNextHopToSink(NetAddress sinkIP, NCS_Location2D sinkLocation) {

        double shortestNodeDistance = -1;
        NetAddress nextHopAddress = null;
        NCS_Location2D nextHopLocation = myNode.getNCS_Location2D();

        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();

        for(NodeEntry nodeEntry: neighboursLinkedList) {
            if (shortestNodeDistance == -1) {
                shortestNodeDistance = nodeEntry.getNCS_Location2D().distanceTo(sinkLocation);
                nextHopAddress = nodeEntry.ip;
                nextHopLocation = nodeEntry.getNCS_Location2D();
            } else if (shortestNodeDistance > nodeEntry.getNCS_Location2D().distanceTo(sinkLocation)) {
                shortestNodeDistance = nodeEntry.getNCS_Location2D().distanceTo(sinkLocation);
                nextHopAddress = nodeEntry.ip;
                nextHopLocation = nodeEntry.getNCS_Location2D();
            }
        }

        topologyGUI.addLink(myNode.getNCS_Location2D(), nextHopLocation, 1, Color.BLACK, TopologyGUI.HeadType.LEAD_ARROW);

        return nextHopAddress;
    }

    private void handleMessageAggregatedDataValue(NetMessage msg) {
        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper sndMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();

        //extract ke tipe madv
        MessageAggregatedDataValue madv = (MessageAggregatedDataValue)sndMsg.getPayload();

        if (queryProcessed.contains(madv.queryID)) {
            //pesan diterima oleh node yang mengerjakan query yang sama
            //next?
            //sementara di send next hop aja

            NetMessage.Ip nmip = new NetMessage.Ip(sndMsg, myNode.getIP(), madv.sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
            NetAddress nextHop = getNextHopToSink(madv.sinkIP, madv.sinkLocation);
            sendToLinkLayer(nmip, nextHop);
        } else {
            //node tidak melakukan process query ID yang sama
            //send ke next hop
            NetMessage.Ip nmip = new NetMessage.Ip(sndMsg, myNode.getIP(), madv.sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
            NetAddress nextHop = getNextHopToSink(madv.sinkIP, madv.sinkLocation);
            sendToLinkLayer(nmip, nextHop);
        }
    }

    private void handleAggregatedValueOnSink(MessageAggregatedDataValue msg) {
        //System.out.println("SINK GOT MADV: Region=" + msg.fromRegion + " AvgVal=" + msg.averageValue
        //        + " SENSOR=" + msg.tipeSensor + " Priority=" + msg.priorityLevel + " AggTotal=" + msg.totalValueAggregated );

        sendToAppLayer(msg, null);

        
    }

    private void increaseThisRetry(long s_Seq) {
        int x = dataRetry.get(s_Seq) + 1;
        dataRetry.put(s_Seq, x);
    }

    private void deleteThisRetry(long s_Seq) {
        dataRetry.remove(s_Seq);
    }

    private boolean isThisRetryAgain(long s_Seq) {
        if (dataRetry.containsKey(s_Seq)) {
            return dataRetry.get(s_Seq) < this.MAXIMUM_RETRY_SEND_MESSAGE;
        } else {
            System.out.println("Packet " + s_Seq + " not registered, dropped!");
            return false;
        }
    }

    private void registerIDToHashmapRetry(long s_Seq) {
        if (!dataRetry.containsKey(s_Seq)) {
            dataRetry.put(s_Seq, 0);
        }
    }

    private void memoryControllerPacketID() {
        if (this.receivedDataId.size() >= LIMIT_PACKET_ID_SIZE) {
            this.receivedDataId.remove(0);
        }
    }



}
