/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csgp_ap.driver;

import csgp_ap.service.DataSkenarioModel;
import jist.swans.Constants;
import jist.swans.field.Fading;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.PathLoss;
import jist.swans.field.Placement;
import jist.swans.field.Spatial;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import sidnet.core.gui.PanelContext;
import sidnet.core.gui.SimGUI;
import sidnet.core.gui.TopologyGUI;
import sidnet.core.interfaces.ColorProfile;
import sidnet.core.interfaces.GPS;
import sidnet.core.misc.GPSimpl;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.LocationContext;
import sidnet.core.misc.Node;
import sidnet.core.simcontrol.SimManager;
import sidnet.models.energy.batteries.Battery;
import sidnet.models.energy.batteries.BatteryUtils;
import sidnet.models.energy.batteries.IdealBattery;
import sidnet.models.energy.energyconsumptionmodels.EnergyConsumptionModel;
import sidnet.models.energy.energyconsumptionmodels.EnergyConsumptionModelImpl;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagement;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagementImpl;
import sidnet.models.energy.energyconsumptionparameters.ElectricParameters;
import sidnet.models.energy.energyconsumptionparameters.EnergyConsumptionParameters;
import sidnet.stack.users.aggregate_route.colorprofile.ColorProfileAggregate;
import sidnet.utilityviews.statscollector.StatsCollector;

/**
 *
 * @author DTK
 */
public class ApDriver {

    public static TopologyGUI myTopology = new TopologyGUI();
    public static int nodes, fieldLength, time;
    public static DataSkenarioModel dataSkenario = new DataSkenarioModel();

    public static Battery battery = new IdealBattery(BatteryUtils.mAhToMJ(75, 3), 3);

    public static EnergyConsumptionParameters eCostParam = new EnergyConsumptionParameters(
            new ElectricParameters(8, 0.015, 27, 10, 3, 0.5, 10, 0.01),
            battery.getVoltage());

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("error");
            return;
        } else {
            System.out.println("Driver installation started ....");

//            try to print args to know its value
            nodes = Integer.parseInt(args[0]);
            fieldLength = Integer.parseInt(args[1]);
            time = Integer.parseInt(args[2]);

            float density = nodes / (float) (fieldLength / 1000.0 * fieldLength / 1000.0);
            System.out.println("nodes : " + nodes);
            System.out.println("size : " + fieldLength + " x " + fieldLength);
            System.out.println("time : " + time + "seconds");

            System.out.println("Creating simulation nodes ...");

        }
    }

    private static Field createSim(int nodes, int length) {
        System.out.println("ApDriver: Create Simulator ()");

        SimGUI simGui = new SimGUI();
        simGui.appendTitle("Reactive Cluster Aggreagate Routing for WSN");

//        SimManager.DEMO = menjalankan aplikasi dalam mode demo
        SimManager simManager = new SimManager(simGui, null, SimManager.DEMO);

        Location.Location2D bounds = new Location.Location2D(length, length);
//        penempatan node [random, grid]
        Placement placement = new Placement.Random(bounds);
//        pergerakan node [Static, RandomWalk, RandomWaypoint, Teleport]
        Mobility mobility = new Mobility.Static();
//        ?
        Spatial spatial = new Spatial.HierGrid(bounds, 5);
        Fading fading = new Fading.None();
        PathLoss pathloss = new PathLoss.FreeSpace();
        Field field = new Field(spatial, fading, pathloss, mobility, Constants.PROPAGATION_LIMIT_DEFAULT);

//        radio environment
        RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
                Constants.FREQUENCY_DEFAULT, 40000, -12,
                Constants.GAIN_DEFAULT,
                Util.fromDB(Constants.SENSITIVITY_DEFAULT),
                Util.fromDB(Constants.THRESHOLD_DEFAULT),
                Constants.TEMPERATURE_DEFAULT,
                Constants.TEMPERATURE_FACTOR_DEFAULT,
                Constants.AMBIENT_NOISE_DEFAULT);
        
        Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
        protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT); // proses maping untuk semua node
        protMap.mapToNext(Constants.NET_PROTOCOL_INDEX_1);
        
        PacketLoss pl = new PacketLoss.Zero();
        
        Node       
        return field;
    }
    
    private static Node createNode(int id,
            Field field, Placement placement,
            Mapper protMap, RadioInfo.RadioInfoShared radioInfoShared,
            PacketLoss plIn, PacketLoss plOut,
            PanelContext hostPanelContext, LocationContext fieldContext,
            SimManager simControl, StatsCollector stats,
            TopologyGUI topologyGUI){
        
        Location nextLocation = placement.getNextLocation();
        
        Battery individualBattery = new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());
        
        EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
        
        EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);
        
        Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new ColorProfileAggregate(), simControl);
        
        GPS gps = new GPSimpl(new Location2D((int) nextLocation.getX(), (int) nextLocation.getY()));
        gps.configure(new LocationContext(fieldContext));
        node.setGPS(gps);
        
        AppLa
                
        
    }

}
