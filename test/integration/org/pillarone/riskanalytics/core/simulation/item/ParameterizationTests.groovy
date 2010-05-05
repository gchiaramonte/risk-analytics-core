package org.pillarone.riskanalytics.core.simulation.item

import models.core.CoreModel
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.fileimport.ParameterizationImportService
import org.pillarone.riskanalytics.core.parameter.Parameter
import org.pillarone.riskanalytics.core.parameter.StringParameter
import org.pillarone.riskanalytics.core.example.model.EmptyModel
import org.pillarone.riskanalytics.core.example.parameter.ExampleParameterObject
import org.pillarone.riskanalytics.core.simulation.item.parameter.StringParameterHolder

class ParameterizationTests extends GroovyTestCase {

    void testLoad() {

        Parameterization unknownParameterization = new Parameterization("unknown name")
        assertEquals "Name not set", "unknown name", unknownParameterization.name
        assertNull unknownParameterization.modelClass
        unknownParameterization.load()
        assertNull unknownParameterization.modelClass
        assertEquals "Name not set after load", "unknown name", unknownParameterization.name

        ParameterizationDAO dao = createDao(EmptyModel, "myDao")
        if (!dao.save()) {
            dao.errors.each {
                println it
            }
        }

        Parameterization parameterization = new Parameterization("myDao")
        assertEquals "wrong name", "myDao", parameterization.name
        assertNull parameterization.modelClass

        parameterization.load()

        assertNotNull parameterization.dao
        assertNotNull parameterization.modelClass
        assertSame "wrong model class", EmptyModel, parameterization.modelClass
    }

    public void testLoadDoesOverwriteChanges() {
        String daoName = "myOtherDao"

        ParameterizationDAO dao = createDao(EmptyModel, daoName)
        // todo (msh) : insert parameter
        if (!dao.save()) {
            dao.errors.each {
                println it
            }
        }


        Parameterization parameterization = new Parameterization("myOtherDao")
        parameterization.load()
        // todo (msh) : modify parameter
        parameterization.load()

    }


    public void testModelClass() {
        Parameterization parameterization = new Parameterization("p")
        assertNull parameterization.getModelClass()

        parameterization.setModelClass(EmptyModel)
        assertSame EmptyModel, parameterization.getModelClass()

        parameterization.modelClass = CoreModel
        assertSame CoreModel, parameterization.modelClass
    }


    public void testSave() {
        Parameterization parameterization = new Parameterization("newParams")
        parameterization.modelClass = EmptyModel
        parameterization.periodCount = 1
        parameterization.save()

        Parameterization savedParameterization = new Parameterization("newParams")
        savedParameterization.load()
        assertEquals parameterization.name, savedParameterization.name
        assertEquals EmptyModel, parameterization.modelClass
        assertEquals 1, parameterization.periodCount
        assertEquals '1', parameterization.versionNumber.toString()

        savedParameterization.modelClass = CoreModel

        savedParameterization.save()

        Parameterization reloadedParameterization = new Parameterization("newParams")
        reloadedParameterization.load()

        assertEquals "modelClass not changed", CoreModel, reloadedParameterization.modelClass
    }


    void testAddRemoveParameter() {
        Parameterization parameterization = new Parameterization("newParams")
        parameterization.periodCount = 1
        parameterization.modelClass = EmptyModel

        int initialCount = Parameter.count()

        StringParameterHolder newHolder = new StringParameterHolder(new StringParameter(path: "path", periodIndex: 0, parameterValue: "value"))
        parameterization.addParameter(newHolder)
        parameterization.removeParameter(newHolder)

        parameterization.save()

        assertEquals 0, parameterization.parameters.size()
        assertEquals initialCount, Parameter.count()

        parameterization.addParameter(newHolder)

        parameterization.save()

        assertEquals 1, parameterization.parameters.size()
        assertEquals initialCount + 1, Parameter.count()

        parameterization.removeParameter(newHolder)

        parameterization.save()

        assertEquals 0, parameterization.parameters.size()
        assertEquals initialCount, Parameter.count()
    }

    void testSimpleParameterUpdate() {
        Parameterization parameterization = new Parameterization("testSimpleParameterUpdate")
        parameterization.periodCount = 1
        parameterization.modelClass = EmptyModel

        int initialCount = Parameter.count()

        StringParameterHolder newHolder = new StringParameterHolder(new StringParameter(path: "path", periodIndex: 0, parameterValue: "value"))
        parameterization.addParameter(newHolder)

        parameterization.save()

        assertEquals 1, parameterization.parameters.size()
        assertEquals initialCount + 1, Parameter.count()

        newHolder.value = "newValue"

        parameterization.save()

        assertEquals 1, parameterization.parameters.size()
        assertEquals initialCount + 1, Parameter.count()

        parameterization.load()
        assertEquals 1, parameterization.parameters.size()

        assertEquals "newValue", parameterization.parameters[0].businessObject
    }

    void testSaveOfParameter() {
        Parameterization parameterization = new Parameterization("newParams")
        parameterization.modelClass = EmptyModel
        StringParameter parameter = new StringParameter(path: "path", parameterValue: "value", periodIndex: 0)
        assertNull parameter.id
        parameterization.periodCount = 1
        parameterization.addParameter(new StringParameterHolder(parameter))

        parameterization.save()
        assertNotNull parameterization.id
    }


    void testGetParameters() {
        Parameterization parameterization = new Parameterization("newParams")
        parameterization.modelClass = EmptyModel
        parameterization.periodCount = 1
        StringParameterHolder parameterA = new StringParameterHolder(new StringParameter(path: "a", parameterValue: "value"))
        StringParameterHolder parameterB = new StringParameterHolder(new StringParameter(path: "a.b", parameterValue: "value"))

        parameterization.addParameter(parameterA)
        parameterization.addParameter(parameterB)

        parameterization.save()

        List parameters = parameterization.getParameters()
        assertNotNull parameters
        assertEquals 2, parameters.size()
        assertTrue parameters.contains(parameterA)
        assertTrue parameters.contains(parameterB)
    }

    void testGetParametersByPath() {
        Parameterization parameterization = new Parameterization("newParams")
        parameterization.modelClass = EmptyModel
        parameterization.periodCount = 4
        StringParameterHolder parameterD = new StringParameterHolder(new StringParameter(path: "a", parameterValue: "value", periodIndex: 3))
        StringParameterHolder parameterC = new StringParameterHolder(new StringParameter(path: "a", parameterValue: "value", periodIndex: 2))
        StringParameterHolder parameterB = new StringParameterHolder(new StringParameter(path: "a", parameterValue: "value", periodIndex: 1))
        StringParameterHolder parameterA = new StringParameterHolder(new StringParameter(path: "a", parameterValue: "value", periodIndex: 0))

        parameterization.addParameter(parameterA)
        parameterization.addParameter(parameterB)
        parameterization.addParameter(parameterC)
        parameterization.addParameter(parameterD)

        parameterization.save()


        List parameters = parameterization.getParameters('a')
        assertNotNull parameters
        assertEquals 4, parameters.size()
        assertTrue parameters.contains(parameterA)
        assertTrue parameters.contains(parameterB)
        assertTrue parameters.contains(parameterC)
        assertTrue parameters.contains(parameterD)

        assertTrue "paramaters must be sorted by period", parameters[0].periodIndex < parameters[1].periodIndex
        assertTrue "paramaters must be sorted by period", parameters[1].periodIndex < parameters[2].periodIndex
        assertTrue "paramaters must be sorted by period", parameters[2].periodIndex < parameters[3].periodIndex
    }

    void testEquals() {

        Parameterization p1 = new Parameterization('Name')
        p1.modelClass = EmptyModel

        Parameterization p2 = new Parameterization('Name')
        p2.modelClass = EmptyModel

        assertTrue p1.equals(p2)
        assertTrue p1.hashCode().equals(p2.hashCode())
    }

    void testToConfigObject() {
        new ParameterizationImportService().compareFilesAndWriteToDB(['CoreParameters'])

        Parameterization parameterization = new Parameterization('CoreParameters')

        ConfigObject configObject = parameterization.toConfigObject()


        assertEquals 6, configObject.size()
        assertTrue configObject.containsKey("applicationVersion")
        assertTrue configObject.containsKey("periodLabels")
        assertEquals CoreModel, configObject.model
        assertEquals 1, configObject.periodCount
        assertEquals 'CoreParameters', configObject.displayName

        ConfigObject components = configObject.components
        assertEquals 2, components.size()

        def distribution = components.exampleInputOutputComponent.parmParameterObject[0]
        assertTrue distribution instanceof ExampleParameterObject

        parameterization.periodLabels = ["Q1"]
        configObject = parameterization.toConfigObject()
        assertEquals 6, configObject.size()
        assertEquals "periodLabels", ["Q1"], configObject.periodLabels
    }


    private def createDao(Class modelClass, String daoName) {
        ParameterizationDAO dao = new ParameterizationDAO()
        dao.periodCount = 1
        dao.itemVersion = '1'
        dao.name = daoName
        dao.modelClassName = modelClass.name
        return dao
    }
}
