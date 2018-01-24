/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.aggregate_route.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author invictus
 */
public class AdaptiveAggregationPayload {
    /*
     * Level priority class
     * Range temperature level 1 adalah normal
     * diambil dari sini : http://weather-and-climate.com/average-monthly-min-max-Temperature,Jakarta,Indonesia
     * range : 20C - 30C
     *
     * Range temperature level 2 adalah suspicious
     * perkiraan sendiri
     * range : 31C - 99C
     *
     * Range temperature level 3 adalah emergency
     * perkiraan sendiri dan info dari web : http://wildfiretoday.com/2011/02/26/at-what-temperature-does-a-forest-fire-burn/
     * nyala api dimulai ketika suhu mencapai 300C
     * range : 100C - 1000C
     */

    //Atur sini variable window
    /*
    private final int WINDOW_MIN_LEVEL_1 = 30;
    private final int WINDOW_MIN_LEVEL_2 = 20;
    private final int WINDOW_MIN_LEVEL_3 = 10;

    private final int WINDOW_MAX_LEVEL_1 = 60;
    private final int WINDOW_MAX_LEVEL_2 = 30;
    private final int WINDOW_MAX_LEVEL_3 = 20;
     *
     */
    
    private final int WINDOW_MIN_LEVEL_1 = 1;
    private final int WINDOW_MIN_LEVEL_2 = 1;
    private final int WINDOW_MIN_LEVEL_3 = 1;

    private final int WINDOW_MAX_LEVEL_1 = 1;
    private final int WINDOW_MAX_LEVEL_2 = 1;
    private final int WINDOW_MAX_LEVEL_3 = 1;


    private class PriorityInfo {
        public int priorityLevel;
        public double priorityLastAggregateValue;
        public double minValue;
        public double maxValue;
        public int aggregateWindowMin;
        public int aggregateWindowMax;
    }
    private HashMap<Integer, PriorityInfo> priorityList = new HashMap<Integer, PriorityInfo>();
    private List<Double> aggregationWindow = new ArrayList<Double>();

    private int aggregateWindowMin;
    private int sizeLimitAggregateWindow; //ini yang dinamis
    private int priorityNow;

    private double lastAggregatedValue;

    public AdaptiveAggregationPayload() {
        //inisialisasi list priority
        PriorityInfo priLevel1 = new PriorityInfo();
        priLevel1.priorityLevel = 1;
        priLevel1.priorityLastAggregateValue = 0;
        priLevel1.minValue = 0;
        priLevel1.maxValue = 39;
        priLevel1.aggregateWindowMin = this.WINDOW_MIN_LEVEL_1;
        priLevel1.aggregateWindowMax = this.WINDOW_MAX_LEVEL_1;


        PriorityInfo priLevel2 = new PriorityInfo();
        priLevel2.priorityLevel = 2;
        priLevel2.priorityLastAggregateValue = 0;
        priLevel2.minValue = 40;
        priLevel2.maxValue = 48;
        priLevel2.aggregateWindowMin = this.WINDOW_MIN_LEVEL_2;
        priLevel2.aggregateWindowMax = this.WINDOW_MAX_LEVEL_2;

        PriorityInfo priLevel3 = new PriorityInfo();
        priLevel3.priorityLevel = 3;
        priLevel3.priorityLastAggregateValue = 0;
        priLevel3.minValue = 49;
        priLevel3.maxValue = 100;
        priLevel3.aggregateWindowMin = this.WINDOW_MIN_LEVEL_3;
        priLevel3.aggregateWindowMax = this.WINDOW_MAX_LEVEL_3;

        //masukan di priority list
        priorityList.put(priLevel1.priorityLevel, priLevel1);
        priorityList.put(priLevel2.priorityLevel, priLevel2);
        priorityList.put(priLevel3.priorityLevel, priLevel3);

        this.priorityNow = 1;
        this.sizeLimitAggregateWindow = priorityList.get(this.priorityNow).aggregateWindowMax;
        this.aggregateWindowMin = priorityList.get(this.priorityNow).aggregateWindowMin;
        this.aggregationWindow.clear();
        this.lastAggregatedValue = 0;

        //awalnya start dari window 60
        this.sizeLimitAggregateWindow = 60;
    }

    public void putValue(double Value) {
        this.aggregationWindow.add(Value);
    }

    public boolean isWindowFull() {
        return aggregationWindow.size() == sizeLimitAggregateWindow;
    }

    public boolean isWindowOneMoreLeft() {
        return (sizeLimitAggregateWindow - aggregationWindow.size()) == 1;
    }

    public double getAggregatedData() {
        double tmpX = 0;
        for (double x : aggregationWindow) {
            tmpX = tmpX + x;
        }
        tmpX = tmpX / aggregationWindow.size();

        this.lastAggregatedValue = tmpX;
        int priorityValue = thisValuePriority(this.lastAggregatedValue);
        if (priorityValue == -1) {
            System.out.println("Priority -1 ??");
        }
        PriorityInfo tmpPI = priorityList.get(priorityValue);
        tmpPI.priorityLastAggregateValue = this.lastAggregatedValue;
        priorityList.put(priorityValue, tmpPI);

        if (this.priorityNow != priorityValue) {
            this.priorityNow = priorityValue;
            this.sizeLimitAggregateWindow = priorityList.get(this.priorityNow).aggregateWindowMax;
            this.aggregateWindowMin = priorityList.get(this.priorityNow).aggregateWindowMin;
        }

        aggregationWindow.clear();
        return this.lastAggregatedValue;
    }

    public int thisValuePriority(double x) {
        Iterator i = priorityList.entrySet().iterator();

        while(i.hasNext()) {
            Entry e = (Entry) i.next();

            PriorityInfo pi = (PriorityInfo) e.getValue();
            if ((((int)x) >= pi.minValue) && (((int)x) <= pi.maxValue)) {
                return (Integer) e.getKey();
            }
        }

        return -1;
    }

    public void reduceWindowSize() {
        if (this.sizeLimitAggregateWindow > this.aggregateWindowMin)
            this.sizeLimitAggregateWindow--;
    }

    public void increaseWindowSize() {
            this.sizeLimitAggregateWindow++;
    }
}
