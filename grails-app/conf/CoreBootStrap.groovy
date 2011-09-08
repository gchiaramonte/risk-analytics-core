import grails.plugins.springsecurity.SpringSecurityService
import grails.util.Environment
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.pillarone.riskanalytics.core.BatchRunSimulationRun
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.fileimport.FileImportService
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.report.IReportModel
import org.pillarone.riskanalytics.core.report.ReportRegistry
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.EnumTagType
import org.springframework.transaction.TransactionStatus
import org.pillarone.riskanalytics.core.user.*
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

class CoreBootStrap {

    private static Log LOG = LogFactory.getLog(CoreBootStrap)

    SpringSecurityService authenticateService

    def init = {servletContext ->

        authenticateService = UserManagement.getSpringSecurityService()

        Authority.withTransaction { status ->
            if (Authority.count() == 0) {

                Authority adminGroup = new Authority()
                adminGroup.authority = UserManagement.ADMIN_ROLE
                adminGroup.save()

                Authority reviewGroup = new Authority()
                reviewGroup.authority = UserManagement.REVIEWER_ROLE
                reviewGroup.save()

                Authority userGroup = new Authority()
                userGroup.authority = UserManagement.USER_ROLE
                userGroup.save()

                Person admin = new Person()
                admin.username = "admin"
                admin.password = authenticateService.encodePassword("admin")
                admin.enabled = true
                admin.settings = new UserSettings(language: "en")
                admin.save()
                PersonAuthority.create(admin, adminGroup)

                Person reviewer = new Person()
                reviewer.username = "reviewer"
                reviewer.password = authenticateService.encodePassword("reviewer")
                reviewer.enabled = true
                reviewer.settings = new UserSettings(language: "en")
                reviewer.save()
                PersonAuthority.create(reviewer, reviewGroup)

                Person actuary = new Person()
                actuary.username = "actuary"
                actuary.password = authenticateService.encodePassword("actuary")
                actuary.enabled = true
                actuary.settings = new UserSettings(language: "en")
                actuary.save()
                PersonAuthority.create(actuary, userGroup)

                Person actuaryDE = new Person()
                actuaryDE.username = "aktuar"
                actuaryDE.password = authenticateService.encodePassword("aktuar")
                actuaryDE.enabled = true
                actuaryDE.settings = new UserSettings(language: "de")
                actuaryDE.save()
                PersonAuthority.create(actuaryDE, userGroup)

                Person actuaryFR = new Person()
                actuaryFR.username = "actuaire"
                actuaryFR.password = authenticateService.encodePassword("actuaire")
                actuaryFR.enabled = true
                actuaryFR.settings = new UserSettings(language: "fr")
                actuaryFR.save()
                PersonAuthority.create(actuaryFR, userGroup)

            }
        }
        //delete all unfinished simulations
        SimulationRun.withTransaction {status ->
            def unfinishedSimulations = SimulationRun.findAllByEndTime(null)
            unfinishedSimulations.each {SimulationRun simulationRun ->
                try {
                    BatchRunSimulationRun batchRunSimulationRun = BatchRunSimulationRun.findBySimulationRun(simulationRun)
                    if (!batchRunSimulationRun)
                        simulationRun.delete()
                } catch (Exception ex) {
                    ex.printStackTrace()
                }
            }
        }
        Tag.withTransaction { status ->
            if (!Tag.findByName("LOCKED")) {
                new Tag(name: "LOCKED", tagType: EnumTagType.PARAMETERIZATION).save()
            }
        }

        if (!Boolean.getBoolean("keepCompiledJasperFiles")) {
            for (IReportModel reportModel in ReportRegistry.getAllReportModels()) {
                for (URL url in reportModel.allSourceFiles) {
                    URL jasperURL = new URL(url.toExternalForm().replace("jrxml", "jasper"))
                    File jasperFile = new File(jasperURL.toURI())
                    if(jasperFile.exists()) {
                        jasperFile.delete()
                        LOG.info("Deleting cached report file ${jasperFile.name}")
                    }
                }
            }
        }

        if (Environment.current == Environment.TEST) {
            return
        }

        //File import must be executed after the registration of all Constraints and CollectingModeStrategies
        //-> registration in the plugin descriptors / fileimport in bootstrap
        def modelFilter = ApplicationHolder.application.config?.models

        List models = null
        if (modelFilter) {
            models = modelFilter.collect {it - "Model"}
        }
        if (!Boolean.getBoolean("skipImport")) {
            ParameterizationDAO.withTransaction {TransactionStatus status ->
                FileImportService.importModelsIfNeeded(models)
            }
        }
    }

    def destroy = {
    }
}
