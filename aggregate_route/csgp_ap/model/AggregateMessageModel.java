/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csgp_ap.model;

import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import sidnet.core.misc.NCS_Location2D;

/**
 *
 * @author DTK
 */
public class AggregateMessageModel implements Message{
    
    private String tipeSensor;
    
    private double maxValue;
    private double minValue;
    private double averageValue;
    
    private long totalValueAggregated;
    
    private int priorityLevel;
    
    private int queryId;
    
    private int fromRegion;
        
    private int aggregateNodeId;
    private NCS_Location2D aggregatorNodeLocation;
    
    private NetAddress sinkIp;
    private NCS_Location2D sinkLocation;

    public String getTipeSensor() {
        return tipeSensor;
    }

    public void setTipeSensor(String tipeSensor) {
        this.tipeSensor = tipeSensor;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getAverageValue() {
        return averageValue;
    }

    public void setAverageValue(double averageValue) {
        this.averageValue = averageValue;
    }

    public long getTotalValueAggregated() {
        return totalValueAggregated;
    }

    public void setTotalValueAggregated(long totalValueAggregated) {
        this.totalValueAggregated = totalValueAggregated;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public int getQueryId() {
        return queryId;
    }

    public void setQueryId(int queryId) {
        this.queryId = queryId;
    }

    public int getFromRegion() {
        return fromRegion;
    }

    public void setFromRegion(int fromRegion) {
        this.fromRegion = fromRegion;
    }

    public int getAggregateNodeId() {
        return aggregateNodeId;
    }

    public void setAggregateNodeId(int aggregateNodeId) {
        this.aggregateNodeId = aggregateNodeId;
    }

    public NCS_Location2D getAggregatorNodeLocation() {
        return aggregatorNodeLocation;
    }

    public void setAggregatorNodeLocation(NCS_Location2D aggregatorNodeLocation) {
        this.aggregatorNodeLocation = aggregatorNodeLocation;
    }

    public NetAddress getSinkIp() {
        return sinkIp;
    }

    public void setSinkIp(NetAddress sinkIp) {
        this.sinkIp = sinkIp;
    }

    public NCS_Location2D getSinkLocation() {
        return sinkLocation;
    }

    public void setSinkLocation(NCS_Location2D sinkLocation) {
        this.sinkLocation = sinkLocation;
    }
    
    

    public int getSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void getBytes(byte[] msg, int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
