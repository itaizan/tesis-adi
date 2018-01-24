package sidnet.utilityviews.statscollector;

public class IncrementableStatEntry
extends StatEntry {
	
	private double value = 0;
	
	public IncrementableStatEntry(String key, String tag) {
		super(key, tag);
	}
	public void increment(double incrementAmount){
		value += incrementAmount;
	}
	@Override
	public String getValueAsString() {
		return "" + getValue();
	}

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }
}
