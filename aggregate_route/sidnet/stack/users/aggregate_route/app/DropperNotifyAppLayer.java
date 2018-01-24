/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.aggregate_route.app;

import jist.swans.misc.Message;

/**
 *
 * @author invictus
 */
public class DropperNotifyAppLayer implements Message {

    public boolean reduceWindow;
    public boolean increaseWindow;
    
    public DropperNotifyAppLayer (boolean reduceWindow, boolean increaseWindow) {
        this.increaseWindow = increaseWindow;
        this.reduceWindow = reduceWindow;
    }

    public int getSize() {
        return 2;
    }

    public void getBytes(byte[] msg, int offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
