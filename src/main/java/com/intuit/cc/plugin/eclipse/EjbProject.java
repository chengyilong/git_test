/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.eclipse;

import java.io.IOException;
import java.io.Writer;

import org.apache.maven.artifact.Artifact;

public class EjbProject extends JarProject {
    
    @Override
    void writeClasspathFacets(Writer writer) throws IOException {
        writer.append("\t\t<attributes>\n"
                +"\t\t\t<attribute name=\"owner.project.facets\" value=\"jst.java\"/>\n"
                +"\t\t</attributes>\n");
    }
    
    @Override
    void writeArtifactClasspathEntryAttributes(Writer writer, Artifact artifact) throws IOException {
    	if(!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
	    	writer.append("\t\t<attributes>\n"
	    				+"\t\t\t<attribute name=\"org.eclipse.jst.component.dependency\" value=\"../\"/>\n"
	    				+"\t\t</attributes>\n");
    	}
    }    
    
    @Override
    void writeClasspathEntries(Writer writer) throws IOException {
        super.writeClasspathEntries(writer);
        writer.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jst.j2ee.internal.module.container\"/>\n");
        writer.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jst.server.core.container/org.jboss.ide.eclipse.as.core.server.runtime.runtimeTarget/JBoss v4.2\">\n"
                +"\t\t<attributes>\n"
                +"\t\t\t<attribute name=\"owner.project.facets\" value=\"jst.ejb\"/>\n"
                +"\t\t</attributes>\n"
                +"\t</classpathentry>\n");
    }
        
    @Override
    void generateComponent(Writer writer) throws IOException {
    	super.generateComponent(writer);
        writer.append("\t\t<property name=\"java-output-path\"/>\n");        
    }
    
    @Override
    void generateFacet(Writer writer) throws IOException {
        writer.append("\t<fixed facet=\"jst.java\"/>\n"
            		+"\t<fixed facet=\"jst.ejb\"/>\n"
            		+"\t<installed facet=\"jst.java\" version=\"6.0\"/>\n"
                	+"\t<installed facet=\"jst.ejb\" version=\"3.0\"/>\n");        
    }

    public EclipseProject getTestProject() {
        return new JarProject();
    }
}
