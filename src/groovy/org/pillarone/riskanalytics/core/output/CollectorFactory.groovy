package org.pillarone.riskanalytics.core.output

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.parameterization.StructureInformation
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.util.GroovyUtils

/**
 * The CollectorFactory is resonsible for the creation of PacketCollectors as they are defined in the
 * ResultConfiguration. It hands over the outputStrategy to the PacketCollector instances.
 *
 * As the ResultConfiguration can contains wildcard path information for subComponents of DynamicComposedComponents
 * a special handling of those wilcard paths has to be applied. The wildcard path is translated in a way, that the
 * wildcard element is substituted for all subComponents.
 *
 */

@CompileStatic
public class CollectorFactory {

    private final ICollectorOutputStrategy outputStrategy
    StructureInformation structureInformation

    public CollectorFactory(ICollectorOutputStrategy outputStrategy) {
        this.outputStrategy = outputStrategy
    }

    /**
     * Create all collectors defined in the ResultConfiguration.
     * Attention: For the special handling of wildcards the model has to be initialized and the parameterization for the
     * simulation has to be applied for one period. This ensures, that also dynamically composes components have their
     * subComponents initialized.
     */
    public List createCollectors(ResultConfiguration resultConfiguration, Model model) {
        return enhanceCollectorInformationSet(resultConfiguration.collectors, model).collect { PacketCollector it ->
            createCollector(it)
        }
    }




    protected PacketCollector createCollector(PacketCollector collectorInformation) {
        collectorInformation.outputStrategy = outputStrategy

        return collectorInformation
    }


    protected List<PacketCollector> enhanceCollectorInformationSet(List collectorInformations, Model model) {
        List<PacketCollector> enhancedCollectorInormation = []
        collectorInformations.each { PacketCollector collectorInformation ->
            def information = findOrCreateCollectorInformation(collectorInformation, model)
            enhancedCollectorInormation.addAll(information)
        }
        return enhancedCollectorInormation
    }

    protected List<PacketCollector> findOrCreateCollectorInformation(PacketCollector collectorInformation, Model model) {
        List<PacketCollector> resultingCollectorInformation = [collectorInformation]

        List<String> pathElements = collectorInformation.path.split("\\:").toList()

        def component = model

        for (String componentName in pathElements[1..-2]) {
            if (GroovyUtils.getProperties(component).keySet().contains(componentName)) {
                component = component[componentName]
            } else {
                if (component instanceof DynamicComposedComponent) {
                    return resolveWildcardPath(component, collectorInformation, componentName)
                } else {
                    Map pathToComponent = structureInformation.componentPaths.inverse()
                    List<String> elements = pathElements[0..-2]
                    String path = elements.join(":")
                    if (pathToComponent.containsKey(path)) {
                        component = pathToComponent.get(path)
                    } else {
                        throw new MissingPropertyException(componentName, component.class)
                    }
                }
            }
        }
        return resultingCollectorInformation
    }

    private List<PacketCollector> resolveWildcardPath(DynamicComposedComponent component, PacketCollector collectorInformation, String wildCard) {
        List<PacketCollector> result = []
        component.allSubComponents().each {Component subComponent ->
            String newPath = collectorInformation.path.replace(wildCard, subComponent.name)
            PacketCollector collector = new PacketCollector(CollectingModeFactory.getNewInstance(collectorInformation.mode))
            collector.path = new PathMapping(pathName: newPath)
            result << collector
        }
        return result
    }
}
