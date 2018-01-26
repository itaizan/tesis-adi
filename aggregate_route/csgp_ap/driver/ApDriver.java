/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csgp_ap.driver;

import csgp_ap.service.DataSkenarioModel;
import sidnet.core.gui.TopologyGUI;
import sidnet.models.energy.batteries.Battery;
import sidnet.models.energy.batteries.BatteryUtils;
import sidnet.models.energy.batteries.IdealBattery;

/**
 *
 * @author DTK
 */
public class ApDriver {
    public static TopologyGUI myTopology = new TopologyGUI();
    public static int nodes, fieldLength, time;
    public static DataSkenarioModel dataSkenario = new DataSkenarioModel();
    
    public static Battery battery = new IdealBattery(BatteryUtils.mAhToMJ(75, 3), 3);
    
}
