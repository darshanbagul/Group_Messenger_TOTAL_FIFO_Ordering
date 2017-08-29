package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
/**
 * Created by darshanbagul
 */
public class Message implements Serializable {
    private String msg;
    private int priority;
    private int proposedPriority;
    private int decidedPriority;
    private int deviceId;
    private boolean is_deliver;

    public String getMessage() {
        return msg;
    }

    public void setMessage(String msg) {
        this.msg = msg;
    }

    public int getMsgPriority() {
        return priority;
    }

    public void setMsgPriority(int priority) {
        this.priority = priority;
    }

    public int getProposedPriority() {
        return proposedPriority;
    }

    public void setProposedPriority(int proposedPriority) {
        this.proposedPriority = proposedPriority;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId= deviceId;
    }

    public int getConsensusPriority() {
        return decidedPriority;
    }

    public void setConsensusPriority(int decidedPriority) {
        this.decidedPriority = decidedPriority;
    }

    public boolean getDeliveryStatus() {
        return is_deliver;
    }

    public void setDeliveryStatus(boolean is_deliver) {
        this.is_deliver = is_deliver;
    }

    public Message() {
    }

    public Message(String msg, int priority, int deviceId) {
        this.msg= msg;
        this.priority = priority;
        this.deviceId = deviceId;
        this.proposedPriority=-1;
        this.decidedPriority=-1;
        is_deliver = false;
    }

    public Message(Message msg_obj) {
        this.msg = msg_obj.getMessage();
        this.priority = msg_obj.getMsgPriority();
        this.deviceId = msg_obj.getDeviceId();
        this.proposedPriority = msg_obj.getProposedPriority();
        this.decidedPriority = msg_obj.getConsensusPriority();
        this.is_deliver = msg_obj.getDeliveryStatus();
    }
}