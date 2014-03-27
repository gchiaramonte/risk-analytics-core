package org.pillarone.riskanalytics.core.simulation.engine

import org.pillarone.riskanalytics.core.simulation.engine.grid.SimulationTask
import org.pillarone.riskanalytics.core.user.Person

class QueueEntry implements Comparable<QueueEntry> {
    final UUID id
    final Date offeredAt
    int priority
    final SimulationTask simulationTask
    final SimulationConfiguration simulationConfiguration
    final Person offeredBy

    QueueEntry(SimulationConfiguration simulationConfiguration, int priority, Person offeredBy) {
        this.simulationTask = new SimulationTask()
        this.offeredBy = offeredBy
        this.priority = priority
        this.simulationConfiguration = simulationConfiguration
        id = UUID.randomUUID()
        offeredAt = new Date()
    }

    QueueEntry(UUID id) {
        this.id = id
        this.priority = 0
        this.simulationTask = null
        this.simulationConfiguration = null
        this.offeredBy = null
        offeredAt = null
    }

    int compareTo(QueueEntry o) {
        if (priority.equals(o.priority)) {
            offeredAt.compareTo(o.offeredAt)
        }
        return priority.compareTo(o.priority)
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        QueueEntry that = (QueueEntry) o

        if (id != that.id) return false

        return true
    }

    int hashCode() {
        return id.hashCode()
    }
}