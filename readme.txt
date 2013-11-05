

If you want to run the project on eclipse you need to add the file lifecycle-mapping-metadata.xml in the root of the project 
at the following location {eclipse_workspace}/.metadata/.plugins/org.eclipse.m2e.core/lifecycle-mapping-metadata.xml.

This file will ignore an error in the project pom.xml due to the appengine maven plugin goals which are not recognized by the m2 eclipse plugin.

