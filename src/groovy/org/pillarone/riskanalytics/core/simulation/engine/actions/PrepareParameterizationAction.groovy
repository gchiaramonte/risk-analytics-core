package org.pillarone.riskanalytics.core.simulation.engine.actions

import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.core.RiskAnalyticsInconsistencyException
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.parameterization.ParameterApplicator
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope
import org.pillarone.riskanalytics.core.simulation.engine.SimulationScope

/**
 * Prepares the ParameterApplicator and applies the parameters of the first period. The later is required as following
 * actions like  {@code ApplyGlobalParametersAction} ,  {@code PrepareResourceParameterizationAction}  and
 * {@code WireModelAction}  depend on parameters.
 */

@CompileStatic
public class PrepareParameterizationAction implements Action {

    private static Log LOG = LogFactory.getLog(PrepareParameterizationAction)

    SimulationScope simulationScope
    PeriodScope periodScope

    public void perform() {
        if(simulationScope.parameters.parameterHolders.empty) {
            throw new RiskAnalyticsInconsistencyException("Parameterization does not contain any parameters.")
        }

        Model model = simulationScope.model
        ParameterApplicator applicator = new ParameterApplicator(model: model, parameterization: simulationScope.parameters)
        applicator.init()
        simulationScope.parameterApplicator = applicator
        periodScope.parameterApplicator = applicator
        // PMO-758: Applying parameters before wiring is necessary,
        // similarly ApplyGlobalParameters and PrepareResourcesParameterizationAction depend on the following line
        simulationScope.parameterApplicator.applyParameterForPeriod(0)
    }



}