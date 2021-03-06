package org.pillarone.riskanalytics.core.simulation.item.parameter.comment

import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.FileConstants
import org.pillarone.riskanalytics.core.parameter.comment.CommentDAO
import org.pillarone.riskanalytics.core.parameter.comment.CommentFileDAO
import org.pillarone.riskanalytics.core.parameter.comment.CommentTag
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.user.Person
import org.pillarone.riskanalytics.core.user.UserManagement
import org.pillarone.riskanalytics.core.util.GroovyUtils
import org.springframework.transaction.TransactionStatus

class Comment implements Cloneable {

    String path
    int period
    DateTime lastChange
    Person user
    protected String comment
    private Set<Tag> tags = new HashSet()
    Set<CommentFile> files = new HashSet<CommentFile>()

    boolean added = false
    boolean updated = false
    boolean deleted = false
    final static String POST_LOCKING = "post locking"

    protected Comment() {}

    public Comment(CommentDAO commentDAO) {
        path = commentDAO.path
        period = commentDAO.periodIndex
        lastChange = commentDAO.timeStamp
        user = commentDAO.user
        comment = commentDAO.comment
        if (commentDAO.tags?.size() > 0) {
            tags.addAll(commentDAO.tags?.tag)
        }
        attachFiles(commentDAO)
    }

    protected void attachFiles(dao) {
        dao.commentFile?.each {
            files.add(new CommentFile(it.name, it.id))
        }
    }

    public byte[] getFileContent(CommentFile file) {
        file.getContent()
    }

    public Comment(String path, int period) {
        this.path = path
        this.period = period
        updateChangeInfo()
        added = true
    }

    public Comment(Map commentMap) {
        this(commentMap['path'], commentMap['period'])

        comment = commentMap['comment']
        commentMap['tags']?.each { String tagName ->
            Tag tag = Tag.findByName(tagName)
            if (!tag) {
                Tag.withTransaction { TransactionStatus status ->
                    tag = new Tag(name: tagName)
                    tag.save()
                }
            }
            addTag(tag)
        }
        lastChange = commentMap['lastChange']
    }

    public List<Tag> getTags() {
        return tags.toList()
    }

    public void addFile(CommentFile file) {
        files.add(file)
    }

    public void removeFile(String filename) {
        files.removeAll { it.filename == filename }
    }

    public void clearFiles() {
        files.clear()
    }

    public void setTags(Set selectedTags) {
        selectedTags.each { Tag tag ->
            if (!tags.contains(tag))
                addTag(tag)
        }
        List tagsToRemove = []
        tags.each { Tag tag ->
            if (!selectedTags.contains(tag))
                tagsToRemove << tag
        }
        if (tagsToRemove.size() > 0)
            tags.removeAll(tagsToRemove)
    }

    public String getText() {
        return comment
    }

    void setText(String newText) {
        comment = newText
        updated = true
        updateChangeInfo()
    }

    void addTag(Tag tag) {
        tags << tag
        updated = true
        updateChangeInfo()
    }

    void removeTag(Tag tag) {
        tags.remove(tag)
        updated = true
        updateChangeInfo()
    }

    protected void updateChangeInfo() {
        user = UserManagement.getCurrentUser()
        lastChange = new DateTime()
    }

    void applyToDomainObject(CommentDAO dao) {
        dao.path = path
        dao.comment = comment
        dao.timeStamp = lastChange
        dao.user = user
        dao.periodIndex = period

        List tagsToRemove = []
        for (CommentTag tag in dao.tags) {
            if (!tags.contains(tag.tag)) {
                tagsToRemove << tag
            }
        }
        for (CommentTag tag in tagsToRemove) {
            dao.removeFromTags(tag)

        }
        tagsToRemove.each { it.delete() }

        for (Tag tag in tags) {
            if (!dao.tags*.tag?.contains(tag)) {
                dao.addToTags(new CommentTag(tag: tag))
            }
        }

        mapToCommentFiles(dao)
    }

    protected mapToCommentFiles(dao) {
        files.each { CommentFile file ->
            if (!(file.filename in dao?.commentFile?.name)) {
                dao.addToCommentFile(new CommentFileDAO(name: file.filename, content: file.content))
            }
        }
        List filesToRemove = []
        dao.commentFile?.each { commentFile ->
            if (!files.find { it.filename == commentFile.name }) {
                filesToRemove << commentFile
            }
        }
        filesToRemove.each {
            dao.removeFromCommentFile(it)
        }
    }

    public String toConfigObject() {
        //end line character
        char c = (char) 92
        StringBuilder sb = new StringBuilder("\"\"[")
        String newComment = replaceCharacters(comment)
        sb.append("path:'${path}', period:${period}, lastChange:new org.joda.time.DateTime(${lastChange.millis}),user:null, comment: ${c}\"${c}\"${c}\"${newComment}${c}\"${c}\"${c}\"")
        if (tags && !tags.isEmpty()) sb.append(", " + GroovyUtils.toString("tags", tags*.name - [POST_LOCKING]))
        sb.append("]\"\"")
        return sb.toString()
    }

    private String replaceCharacters(String comment) {
        char c = (char) 92
        //replace all occurrences end line Character(\n) in comment  with html code &#92;
        String newComment = comment.replace("${c}", "&#92;")
        //replace quote character by html code
        newComment = newComment.replace('"', '&rdquo;')
        return newComment
    }

    public Comment clone() {
        Comment clone = this.class.newInstance([path, period] as Object[])
        clone.user = user
        clone.comment = comment
        clone.lastChange = (DateTime) new DateTime(lastChange.millis)
        clone.tags = tags.findAll { it.name != POST_LOCKING }.clone() as Set
        clone.files = new HashSet()

        for(CommentFile file in files) {
            File newFile = file.file
            if(file.persistedId != null) {
                newFile = File.createTempFile(file.filename, '', new File(FileConstants.TEMP_FILE_DIRECTORY))
                newFile.bytes = file.content
            }
            CommentFile commentFile = new CommentFile(file.filename, newFile)
            clone.files << commentFile
        }

        clone.added = false
        clone.updated = false
        clone.deleted = false
        return clone;
    }


}
