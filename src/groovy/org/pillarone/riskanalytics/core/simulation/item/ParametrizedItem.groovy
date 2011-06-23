package org.pillarone.riskanalytics.core.simulation.item

import org.pillarone.riskanalytics.core.parameter.Parameter
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolder
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolderFactory

abstract class ParametrizedItem extends CommentableItem {

    ParametrizedItem(String name) {
        super(name)
    }

    protected void loadParameters(List<ParameterHolder> parameterHolders, Collection<Parameter> parameters) {
        List<ParameterHolder> existingHolders = parameterHolders.clone()
        parameterHolders.clear()

        for (Parameter p in parameters) {
            final ParameterHolder existingParameterHolder = existingHolders.find { it.path == p.path && it.periodIndex == p.periodIndex}
            if (existingParameterHolder != null) {
                existingParameterHolder.setParameter(p)
                parameterHolders << existingParameterHolder
            } else {
                parameterHolders << ParameterHolderFactory.getHolder(p)
            }
        }
    }

    protected void saveParameters(List<ParameterHolder> parameterHolders, Collection<Parameter> parameters, def dao) {
        Iterator<ParameterHolder> iterator = parameterHolders.iterator()
        while (iterator.hasNext()) {
            ParameterHolder parameterHolder = iterator.next()
            if (parameterHolder.hasParameterChanged()) {
                Parameter parameter = parameters.find { it.path == parameterHolder.path && it.periodIndex == parameterHolder.periodIndex }
                parameterHolder.applyToDomainObject(parameter)
                parameterHolder.modified = false
            } else if (parameterHolder.added) {
                Parameter newParameter = parameterHolder.createEmptyParameter()
                parameterHolder.applyToDomainObject(newParameter)
                addToDao(newParameter, dao)
                parameterHolder.added = false
            } else if (parameterHolder.removed) {
                Parameter parameter = parameters.find { it.path == parameterHolder.path && it.periodIndex == parameterHolder.periodIndex }
                removeFromDao(parameter, dao)
                parameter.delete()
                iterator.remove()
            }
        }
    }

    abstract protected void addToDao(Parameter parameter, def dao)

    abstract protected void removeFromDao(Parameter parameter, def dao)
}