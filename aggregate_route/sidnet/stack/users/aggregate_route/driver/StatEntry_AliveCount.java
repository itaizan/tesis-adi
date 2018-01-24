/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.aggregate_route.driver;

import sidnet.utilityviews.statscollector.ExclusionStatEntry;
import sidnet.utilityviews.statscollector.NodeBasedStatEntry;
import sidnet.core.misc.Node;

/**
 *
 * @author invictus
 */
public class StatEntry_AliveCount
        extends ExclusionStatEntry
        implements NodeBasedStatEntry  {

        private static final String TAG = "AliveNodesCount";
        private int deadNodesCount = 0;
        private int energyPercentageThreshold;

        public StatEntry_AliveCount(String key, int energyPercentageThreshold) {
            super(key, TAG);
            this.energyPercentageThreshold = energyPercentageThreshold;
        }

        /**
         * @inheridoc
         */
        public String getValueAsString() {
            return "" + deadNodesCount;
        }

        /**
         * @inheridoc
         */
       public void update(Node[] nodes) {
           super.update(nodes);
           deadNodesCount = 0;
           for (int i = 0; i < nodes.length; i++)
                 if (nodes[i].getEnergyManagement().getBattery().getPercentageEnergyLevel() >= energyPercentageThreshold)
                       deadNodesCount++;
       }

}
