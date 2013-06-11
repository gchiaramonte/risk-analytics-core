package org.pillarone.riskanalytics.core.parameterization.validation

import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory


@CompileStatic
class ValidatorRegistry {

    private static Log LOG = LogFactory.getLog(ValidatorRegistry)

    private static List<IParameterizationValidator> validators = new LinkedList<IParameterizationValidator>()

    static void addValidator(IParameterizationValidator validator) {
        if (!validators.contains(validator)) {
            validators.add(validator)
        } else {
            LOG.warn "Validator ${validator.class.simpleName} already exists - ignoring"
        }
    }

    static List<IParameterizationValidator> getValidators() {
        return Collections.unmodifiableList(validators)
    }

    static boolean contains(Class clazz) {
        return validators.any { it.class.name == clazz.name }
    }
}
