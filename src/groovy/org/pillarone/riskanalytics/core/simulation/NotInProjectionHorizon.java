package org.pillarone.riskanalytics.core.simulation;

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
public class NotInProjectionHorizon extends SimulationException {
    public NotInProjectionHorizon(String message) {
        super(message);
    }
}
