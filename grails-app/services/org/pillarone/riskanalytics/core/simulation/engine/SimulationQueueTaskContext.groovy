package org.pillarone.riskanalytics.core.simulation.engine

import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.queue.IQueueTaskContext
import org.pillarone.riskanalytics.core.simulation.engine.grid.SimulationTask

class SimulationQueueTaskContext implements IQueueTaskContext<SimulationConfiguration> {

    final SimulationTask simulationTask
    final SimulationConfiguration configuration

    SimulationQueueTaskContext(SimulationTask simulationTask, SimulationConfiguration configuration) {
        this.simulationTask = simulationTask
        this.configuration = configuration
    }

    @Override
    DateTime getEstimatedEnd() {
        return simulationTask.estimatedSimulationEnd
    }

    @Override
    int getProgress() {
        simulationTask.progress
    }

    @Override
    String getUsername() {
        configuration.username
    }
}
