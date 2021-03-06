package org.pillarone.riskanalytics.core.parameter.comment

import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.EnumTagType

class Tag {

    static final String LOCKED_TAG = 'LOCKED'
    static String qtrTagMatcherRegex = System.getProperty("qtrTagMatcherRegex", "^Q[1234] 20[0-9][0-9]\$" )

    String name
    EnumTagType tagType = EnumTagType.COMMENT

    String toString() {
        name
    }

    boolean equals(Object obj) {
        if (obj instanceof Tag) {
            return name.equals(obj.name)
        }
        return false
    }

    static constraints = {
        name(unique: true)
    }

    int hashCode() {
        return name.hashCode()
    }

    boolean isQuarterTag(){
        return name.matches(qtrTagMatcherRegex)
    }
}
