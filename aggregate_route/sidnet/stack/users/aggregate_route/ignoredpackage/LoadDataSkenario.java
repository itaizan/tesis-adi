/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.aggregate_route.ignoredpackage;

import java.util.HashMap;
import jist.swans.Constants;

/**
 *
 * @author invictus
 */
public class LoadDataSkenario {
    public static final int UNBOUNDED = -1;

    private class pairMinMaxTemperature{
        private double minValue;
        private double maxValue;

        public pairMinMaxTemperature(double minValue, double maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public double getRandomSensedValue() {
            double diff = maxValue - minValue;
            return minValue + Math.random() * diff;
        }

    }
    private HashMap<Integer, pairMinMaxTemperature> skenarioValue1 = new HashMap<Integer, pairMinMaxTemperature>();
    private HashMap<Integer, pairMinMaxTemperature> skenarioValue2 = new HashMap<Integer, pairMinMaxTemperature>();
    private HashMap<Integer, pairMinMaxTemperature> skenarioValue3 = new HashMap<Integer, pairMinMaxTemperature>();

    public LoadDataSkenario() {
        String filename = "csv_file\\";

        String filename1 = filename + "skenario_region_1.csv";
        String filename2 = filename + "skenario_region_2.csv";
        String filename3 = filename + "skenario_region_3.csv";

        java.io.BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new java.io.BufferedReader(new java.io.FileReader(filename1));
            while ((line = br.readLine()) != null) {
                if (!line.contains("#")) { //skip header
                    String[] data = line.split(cvsSplitBy);

                    pairMinMaxTemperature pmmt = new pairMinMaxTemperature(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
                    skenarioValue1.put(Integer.parseInt(data[0]), pmmt);
                }
            }

            br = new java.io.BufferedReader(new java.io.FileReader(filename2));
            while ((line = br.readLine()) != null) {
                if (!line.contains("#")) { //skip header
                    String[] data = line.split(cvsSplitBy);

                    pairMinMaxTemperature pmmt = new pairMinMaxTemperature(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
                    skenarioValue2.put(Integer.parseInt(data[0]), pmmt);
                }
            }

            br = new java.io.BufferedReader(new java.io.FileReader(filename3));
            while ((line = br.readLine()) != null) {
                if (!line.contains("#")) { //skip header
                    String[] data = line.split(cvsSplitBy);

                    pairMinMaxTemperature pmmt = new pairMinMaxTemperature(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
                    skenarioValue3.put(Integer.parseInt(data[0]), pmmt); 
                }
            }

            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
		if (br != null) {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    }

    public double getSensedValueBaseScene(int skenarioRegionID, long time) {
        int hour = new Long(time / Constants.HOUR).intValue();
        if (skenarioRegionID == 1)
            return skenarioValue1.get(hour).getRandomSensedValue();

        if (skenarioRegionID == 2)
            return skenarioValue2.get(hour).getRandomSensedValue();
        
        if (skenarioRegionID == 3)
            return skenarioValue3.get(hour).getRandomSensedValue();
       return 0;
    }

}
