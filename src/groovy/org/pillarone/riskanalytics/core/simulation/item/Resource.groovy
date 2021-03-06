package org.pillarone.riskanalytics.core.simulation.item

import groovy.transform.CompileStatic
import org.apache.commons.lang.builder.HashCodeBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.core.ResourceDAO
import org.pillarone.riskanalytics.core.components.IResource
import org.pillarone.riskanalytics.core.components.ResourceHolder
import org.pillarone.riskanalytics.core.parameter.Parameter
import org.pillarone.riskanalytics.core.parameter.ResourceParameter
import org.pillarone.riskanalytics.core.parameter.comment.CommentDAO
import org.pillarone.riskanalytics.core.parameter.comment.ResourceCommentDAO
import org.pillarone.riskanalytics.core.parameter.comment.ResourceTag
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.parameter.comment.workflow.WorkflowResourceCommentDAO
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolder
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.Comment
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.workflow.WorkflowComment
import org.pillarone.riskanalytics.core.workflow.Status

class Resource extends ParametrizedItem {
    private static final Log LOG = LogFactory.getLog(Resource)

    String comment
    List<ParameterHolder> parameterHolders
    List<Tag> tags = []
    boolean valid
    List validationErrors
    Status status

    Resource(String name, Class resourceClass) {
        super(name)
        this.modelClass = resourceClass
        parameterHolders = []
        status = Status.NONE
        versionNumber = new VersionNumber("1")
    }

    @CompileStatic
    ResourceHolder getResourceInstance() {
        return new ResourceHolder(modelClass, name, versionNumber)
    }

    @CompileStatic
    IResource createResourceInstance() {
        IResource instance = (IResource) modelClass.newInstance()
        if (name != null) {
            if (!isLoaded()) {
                load()
            }
            for (ParameterHolder holder in parameterHolders) {
                instance[holder.path] = holder.businessObject
            }
        } else {
            instance.useDefault()
        }
        return instance
    }

    @Override
    protected void addToDao(Parameter parameter, Object dao) {
        dao = dao as ResourceDAO
        dao.addToParameters(parameter)
    }

    @Override
    protected void removeFromDao(Parameter parameter, Object dao) {
        dao = dao as ResourceDAO
        dao.removeFromParameters(parameter)
    }

    @Override
    @CompileStatic
    List<ParameterHolder> getAllParameterHolders() {
        return parameterHolders
    }

    @Override
    @CompileStatic
    protected createDao() {
        return new ResourceDAO(name: name, resourceClassName: modelClass.name)
    }

    @Override
    @CompileStatic
    def getDaoClass() {
        return ResourceDAO
    }

    @Override
    @CompileStatic
    Integer getPeriodCount() {
        return 1
    }

    @CompileStatic
    public List<String> getAllEditablePaths() {
        List result = []
        for (Comment comment in comments) {
            if (comment instanceof WorkflowComment) {
                result << comment.path
            }
        }
        return result
    }

    @Override
    protected void mapToDao(Object dao) {
        dao = dao as ResourceDAO
        dao.itemVersion = versionNumber.toString()
        dao.name = name
        dao.creationDate = creationDate
        dao.modificationDate = modificationDate
        dao.valid = valid
        dao.resourceClassName = modelClass.name
        dao.creator = creator
        dao.lastUpdater = lastUpdater
        dao.comment = comment
        dao.status = status
        saveParameters(parameterHolders, dao.parameters, dao)
        saveComments(dao)
        saveTags(dao)
    }

    @Override
    protected void mapFromDao(Object dao, boolean completeLoad) {
        dao = dao as ResourceDAO
        long time = System.currentTimeMillis()
        id = dao.id
        versionNumber = new VersionNumber(dao.itemVersion)
        name = dao.name
        creationDate = dao.creationDate
        modificationDate = dao.modificationDate
        valid = dao.valid
        modelClass = getClass().getClassLoader().loadClass(dao.resourceClassName)
        creator = dao.creator
        lastUpdater = dao.lastUpdater
        status = dao.status
        comment = dao.comment
        if (completeLoad) {
            loadParameters(parameterHolders, dao.parameters)
            loadComments(dao)
            tags = dao.tags*.tag
            if (!tags) tags = []
        }
        LOG.info("Resource $name loaded in ${System.currentTimeMillis() - time}ms")
    }

    @Override
    protected loadFromDB() {
        return ResourceDAO.findWhere([name: name, resourceClassName: modelClass.name, itemVersion: versionNumber.toString()])
    }

    protected void saveTags(ResourceDAO dao) {
        List tagsToRemove = []
        for (ResourceTag tag in dao.tags) {
            if (!tags.contains(tag.tag)) {
                tagsToRemove << tag
            }
        }
        for (ResourceTag tag in tagsToRemove) {
            dao.removeFromTags(tag)

        }
        tagsToRemove.each { it.delete() }

        for (Tag tag in tags) {
            if (!dao.tags*.tag?.contains(tag)) {
                dao.addToTags(new ResourceTag(tag: tag))
            }
        }
    }

    private void loadComments(ResourceDAO dao) {
        comments = []

        for (ResourceCommentDAO c in dao.comments) {
            comments << new Comment(c)
        }

        for (WorkflowResourceCommentDAO c in dao.issues) {
            comments << new WorkflowComment(c)
        }
    }

    @Override
    CommentDAO getItemCommentDAO(Object dao) {
        return new ResourceCommentDAO(resourceDAO: dao)
    }

    protected void commentAdded(ResourceDAO dao, WorkflowComment comment) {
        WorkflowResourceCommentDAO commentDAO = new WorkflowResourceCommentDAO(resource: dao)
        comment.applyToDomainObject(commentDAO)
        dao.addToIssues(commentDAO)
        comment.added = false
    }

    protected void commentUpdated(ResourceDAO dao, WorkflowComment comment) {
        WorkflowResourceCommentDAO commentDAO = dao.issues.find { it.path == comment.path }
        if (commentDAO) {
            comment.applyToDomainObject(commentDAO)
            comment.updated = false
        }
    }

    protected boolean commentDeleted(ResourceDAO dao, WorkflowComment comment) {
        WorkflowResourceCommentDAO commentDAO = dao.issues.find { it.path == comment.path }
        if (commentDAO) {
            dao.removeFromIssues(commentDAO)
            commentDAO.delete()
            return true
        }
        return false
    }

    @CompileStatic
    List getParameters(String path) {
        return parameterHolders.findAll { ParameterHolder parameter ->
            parameter.path == path && !parameter.removed
        }.toList()
    }

    public boolean isUsedInSimulation() {
        def criteria = ResourceParameter.createCriteria()
        return criteria.count {
            eq("name", name)
            eq("itemVersion", versionNumber.toString())
            eq("resourceClassName", modelClass.name)
        } > 0
    }

    @CompileStatic
    public boolean isEditable() {
        if (status != Status.NONE && status != Status.DATA_ENTRY) {
            return false
        }
        return !isUsedInSimulation()
    }

    @Override
    String getNameAndVersion() {
        "$name v${versionNumber.toString()}"
    }

    @CompileStatic
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
        hashCodeBuilder.append(name)
        hashCodeBuilder.append(modelClass)
        hashCodeBuilder.append(versionNumber.toString())
        return hashCodeBuilder.toHashCode()
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            return super.equals(obj) && obj.versionNumber.equals(versionNumber)
        } else {
            return false
        }
    }

}
