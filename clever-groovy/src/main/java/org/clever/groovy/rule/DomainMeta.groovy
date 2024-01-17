package org.clever.groovy.rule

import groovy.transform.Canonical
import groovy.transform.MapConstructor
import groovy.transform.ToString

@MapConstructor
@Canonical
@ToString
class DomainMeta {
    String title;
    String name;
    String category;

    DomainMetaProperty[] metaProperties
}
