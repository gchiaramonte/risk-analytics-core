import org.pillarone.riskanalytics.core.output.batch.results.SQLServerBulkInsert
import org.pillarone.riskanalytics.core.output.batch.calculations.SQLServerCalculationBulkInsert
import org.pillarone.riskanalytics.core.output.batch.results.MysqlBulkInsert
import org.pillarone.riskanalytics.core.output.batch.calculations.MysqlCalculationsBulkInsert
import grails.plugin.springsecurity.SecurityConfigType
import org.pillarone.riskanalytics.core.simulation.engine.grid.mapping.OneNodeStrategy

environments {

    databaseSupportClass = null
    resultBulkInsert = null
    calculationBulkInsert = null
    keyFiguresToCalculate = null
    numberOfParallelJobsPerNode = 100

    dataDirectoryIndependentOfVersion = false

    transactionServiceUrl = "rmi://localhost:1099/TransactionService"
    resultServiceRegistryPort = 1099

    development {
        log4j = {
            appenders {
                console name: 'stdout', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
            }
            root {
                error 'stdout'
                additivity = false
            }
            info 'org.pillarone.riskanalytics.core', 'org.gridgain'
        }
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'percentileProfitFunction': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'varProfitFunction': [99, 99.5],
                'tvarProfitFunction': [99, 99.5],
                'pdf': 200
        ]
    }
    test {
        models = ["CoreModel"]
        log4j = {
            info 'org.pillarone.riskanalytics.core'
        }
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'percentileProfitFunction': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'varProfitFunction': [99, 99.5],
                'tvarProfitFunction': [99, 99.5],
                'pdf': 200
        ]
        nodeMappingStrategy = OneNodeStrategy
    }
    sqlserver {
        models = ["FiniteReModel"]
        resultBulkInsert = SQLServerBulkInsert
        calculationBulkInsert = SQLServerCalculationBulkInsert
            keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'percentileProfitFunction': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'varProfitFunction': [99, 99.5],
                'tvarProfitFunction': [99, 99.5],
        ]
        log4j = {
            info 'org.pillarone.riskanalytics.core.output',
                    'org.pillarone.riskanalytics.core.components',
                    'org.pillarone.riskanalytics.core.simulation',
                    'org.pillarone.riskanalytics.core.fileimport',
                    'org.pillarone.riskanalytics.core.parameterization',
                    'org.pillarone.riskanalytics.core.jobs.JobScheduler',
                    'org.pillarone.riskanalytics.core.simulation.engine'
        }
    }
    mysql {
        resultBulkInsert = MysqlBulkInsert
        calculationBulkInsert = MysqlCalculationsBulkInsert
        log4j = {
            appenders {
                console name: 'stdout', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
                file name: 'file', file: 'RiskAnalytics.log', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
            }
            root {
                error 'stdout', 'file'
                additivity = false
            }
            info('org.pillarone.riskanalytics.core.output',
                    'org.pillarone.riskanalytics.core.components',
                    'org.pillarone.riskanalytics.core.simulation',
                    'org.pillarone.riskanalytics.core.fileimport',
                    'org.pillarone.riskanalytics.core.parameterization',
                    'org.pillarone.riskanalytics.core.jobs.JobScheduler',
                    'org.pillarone.riskanalytics.core.simulation.engine'
            )
        }
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'percentileProfitFunction': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'varProfitFunction': [99, 99.5],
                'tvarProfitFunction': [99, 99.5],
                'pdf': 200
        ]
    }


    production {
        resultBulkInsert = MysqlBulkInsert
        calculationBulkInsert = MysqlCalculationsBulkInsert
        userLogin = true
        maxIterations = 10000
        models = ["CapitalEagleModel", "DependencyModel", "DynamicCapitalEagleModel", "MultiLineReinsuranceModel", "TwoLobDependencyModel", "PodraModel"]
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'percentileProfitFunction': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'varProfitFunction': [99, 99.5],
                'tvarProfitFunction': [99, 99.5],
                'pdf': 200
        ]
    }

}

grails {
    views {
        'default' {
            codec = "none"
        }
        gsp {
            encoding = "UTF-8"
        }
    }

    plugin {
        springsecurity {
            userLookup {
                userDomainClassName = 'org.pillarone.riskanalytics.core.user.Person'
                authorityJoinClassName = 'org.pillarone.riskanalytics.core.user.PersonAuthority'
            }
            authority {
                className = 'org.pillarone.riskanalytics.core.user.Authority'
            }
            securityConfigType = SecurityConfigType.InterceptUrlMap
            interceptUrlMap = [
                    '/login/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/**/css/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/**/js/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/**/images/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/**/*.jar': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/ulcserverendpoint/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/css/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/person/**': ['ROLE_ADMIN'],
                    '/authority/**': ['ROLE_ADMIN'],
                    '/**': ['IS_AUTHENTICATED_REMEMBERED'],
            ]
        }
    }
}
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

// Uncomment and edit the following lines to start using Grails encoding & escaping improvements

/* remove this line 
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */
