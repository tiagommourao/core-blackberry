/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib 
 * File         : AcEvent.java 
 * Created      : 26-mar-2010
 * *************************************************/
package com.ht.rcs.blackberry.event;

public class AcEvent extends Event {

    public AcEvent(int actionId, byte[] confParams) {
        super(Event.EVENT_AC, actionId, confParams);
    }

    protected boolean Parse(byte[] confParams) {
        // TODO Auto-generated method stub
        return false;
    }

    protected void EventRun() {
        // TODO Auto-generated method stub

    }

}
