package org.clever.groovy.rule

import groovy.transform.Canonical
import groovy.transform.MapConstructor
import groovy.transform.ToString

@MapConstructor
@Canonical
@ToString
public class DomainMetaProperty {
    String name
    String title
    String xtype
    String dict
    String widget
    String dataType
    String category
    String desc
    String vjsonDefine
    int order = 0
}
