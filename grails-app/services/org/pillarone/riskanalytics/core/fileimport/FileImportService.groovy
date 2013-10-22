package org.pillarone.riskanalytics.core.fileimport

import groovy.transform.CompileStatic

import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTimeZone
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.model.registry.ModelRegistry
import org.pillarone.riskanalytics.core.output.ResultConfigurationDAO
import org.pillarone.riskanalytics.core.util.ConfigObjectUtils

abstract class FileImportService {

    protected static final Log LOG = LogFactory.getLog(FileImportService)

    abstract String getFileSuffix()

    abstract public def getDaoClass()

    abstract protected boolean saveItemObject(String fileContent)

    abstract String prepare(URL file, String itemName)

    /** Setting the default time zone to UTC avoids problems in multi user context with different time zones
     *  and switches off daylight saving capabilities and possible related problems.  */
    DateTimeZone utc = DateTimeZone.setDefault(DateTimeZone.UTC)

    @CompileStatic
    public int compareFilesAndWriteToDB(List modelNames = null) {

        int recordCount = 0
        scanImportFolder(modelNames).each { URL url ->
            if (importFile(url)) {
                recordCount++
            }
        }
        return recordCount
    }

    @CompileStatic
    protected List scanImportFolder(List modelNames = null) {
        URL modelSourceFolder = searchModelImportFolder()
        return modelSourceFolder.toExternalForm().startsWith("jar") ? findURLsInJar(modelSourceFolder, modelNames) : findURLsInDirectory(modelSourceFolder, modelNames)
    }

    @CompileStatic
    protected List<URL> findURLsInDirectory(URL url, List modelNames) {
        LOG.trace "Importing from directory ${url.toExternalForm()}"

        List<URL> matchingFiles = []
        new File(url.toURI()).eachFileRecurse { File file ->
            if (file.isFile() && file.name.endsWith("${fileSuffix}.groovy") && shouldImportModel(file.parentFile.name, file.name, modelNames)) {
                matchingFiles << file.toURI().toURL()
            }
        }
        return matchingFiles
    }

    @CompileStatic
    protected List<URL> findURLsInJar(URL url, List modelNames) {
        LOG.trace "Importing from JAR file ${url.toExternalForm()}"

        List<URL> matchingFiles = []

        JarURLConnection connection
        ZipInputStream inputStream

        try {
            connection = (JarURLConnection) url.openConnection()
            URL jarUrl = connection.getJarFileURL()
            inputStream = new JarInputStream(jarUrl.openStream())
            ZipEntry entry = null
            while ((entry = inputStream.getNextEntry()) != null) {
                String entryName = entry.getName()
                String filename = entryName.substring(entryName.lastIndexOf("/") + 1)
                String folderName = entryName - "/$filename"
                folderName = folderName.substring(folderName.lastIndexOf("/") + 1)
                if (entryName.contains("models") && entryName.endsWith("${fileSuffix}.groovy") && shouldImportModel(folderName, filename, modelNames)) {
                    URL resource = getClass().getResource("/" + entryName)
                    if (resource != null) {
                        matchingFiles << resource
                    }
                }
                inputStream.closeEntry()

            }
        } finally {
            inputStream.close()
        }

        return matchingFiles
    }

    @CompileStatic
    public boolean importFile(URL url) {
        LOG.debug("importing ${url.toExternalForm()}")
        boolean success = false
        String urlString = url.toExternalForm()
        String itemName = prepare(url, urlString.substring(urlString.lastIndexOf("/") + 1))

        boolean alreadyImported = lookUpItem(getDaoClass(), itemName)
        if (!alreadyImported) {
            String fileContent = readFromURL(url)
            if (saveItemObject(fileContent)) {
                LOG.debug(">imported $itemName")
                success = true
            } else {
                LOG.error("Error importing $itemName")
            }
        } else {
            LOG.debug("omitted $itemName as it already exists.")
        }
        return success
    }

    @CompileStatic
    protected String readFromURL(URL url) {
        Scanner scanner = new Scanner(url.openStream()).useDelimiter("\\Z")
        return scanner.next()
    }

    protected boolean lookUpItem(String itemName) {
        return getDaoClass().findByName(itemName) != null
    }

    protected boolean lookUpItem(def daoClass, String itemName) {
        if (daoClass == ParameterizationDAO || daoClass == ResultConfigurationDAO) {
            return getDaoClass().findByNameAndModelClassName(itemName, getModelClassName()) != null
        } else {
            return lookUpItem(itemName)
        }
    }

    @CompileStatic
    protected boolean shouldImportModel(String folderName, String filename, List models) {
        if (!models) {
            return true
        }
        LOG.trace "filtering $filename with $models"
        models.any { String it ->
            filename.startsWith(it) && folderName.toLowerCase().equals(it.toLowerCase())
        }
    }

    @CompileStatic
    protected URL searchModelImportFolder() {
        URL modelFolder = getClass().getResource("/models")
        if (modelFolder == null) {
            throw new RuntimeException("Model folder not found")
        }
        LOG.debug "Model source URL: ${modelFolder.toExternalForm()}"
        return modelFolder
    }

    @CompileStatic
    public static void spreadRanges(ConfigObject config) {
        ConfigObjectUtils.spreadRanges config
    }


    @CompileStatic
    static void importModelsIfNeeded(List modelNames) {
        if (!Boolean.getBoolean("skipImport")) {
            String models = modelNames != null && !modelNames.empty ? modelNames.join(", ") : "all models"
            LOG.info "Importing files for ${models}"
            new ModelFileImportService().compareFilesAndWriteToDB(modelNames)
            new ModelStructureImportService().compareFilesAndWriteToDB(modelNames)
            new ParameterizationImportService().compareFilesAndWriteToDB(modelNames)
            new ResultConfigurationImportService().compareFilesAndWriteToDB(modelNames)
        }
        ModelRegistry.instance.loadFromDatabase()
    }

    @CompileStatic
    String getModelClassName() {
        return null
    }

}