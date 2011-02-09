/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;

public class EarProject extends EjbProject {

    @Override
    boolean testDependsOnMain() {
    	// there is no main source in ear, so test cannot depend on it.
		return false;
	}

    @Override
    void writeProjectBuildCommands(Writer writer) throws IOException {
        writeProjectFacetBuildCommands(writer);        
    }
    
    @Override
    void writeProjectBuildNatures(Writer writer) throws IOException {
        writer.append("\t\t<nature>org.eclipse.wst.common.project.facet.core.nature</nature>\n"
                	+"\t\t<nature>org.eclipse.wst.common.modulecore.ModuleCoreNature</nature>\n");
    }
    
    @Override
    void generateClasspath() throws IOException {
        // nothing
    }
        
    void generateComponentResources(Writer writer, Dependency dependency) throws IOException {
    	String projectName= getProjectNameFromDependency(dependency);    	
        writer.append("\t\t<dependent-module");
        
    	if("jar".equals(dependency.getType())) {
            writer.append(" archiveName=\"").append(projectName).append(".jar\"");
    	}
    	
        writer.append(" deploy-path=\"/\" handle=\"module:/resource/")
			.append(projectName).append('/').append(projectName).append("\">\n"
					+"\t\t\t<dependent-object/>\n"
					+"\t\t\t<dependency-type>uses</dependency-type>\n"
					+"\t\t</dependent-module>\n");
    }
        
    void generateComponentResources(Writer writer, Artifact artifact) throws IOException {
        File jar= artifact.getFile();
        writer.append("\t\t<dependent-module archiveName=\"").append(jar.getName())
        	.append("\" deploy-path=\"/\" handle=\"module:/classpath/lib/")
			.append(jar.getAbsolutePath().replace('\\', '/')).append("\">\n"
					+"\t\t\t<dependent-object/>\n"
					+"\t\t\t<dependency-type>uses</dependency-type>\n"
					+"\t\t</dependent-module>\n");
    }
    
    @Override
    void generateComponent(Writer writer) throws IOException {
        writer.append("\t\t<wb-resource deploy-path=\"/\" source-path=\"/java\"/>\n");        
        for(Dependency dependency : eclipseProjects) {
            generateComponentResources(writer, dependency);
        }
        for(Artifact artifact : artifacts) {
        	if(!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
                generateComponentResources(writer, artifact);
        	}
        }

    }

    @Override
    void generateFacet(Writer writer) throws IOException {
        writer.append("\t<fixed facet=\"jst.ear\"/>\n"
        			+"\t<installed facet=\"jst.ear\" version=\"5.0\"/>\n");        
    }
    
    @Override
    void generateManifest(File metaInf) throws IOException {
    	try {
			callback.generateApplicationXml(new File(metaInf, "application.xml").getAbsolutePath());
		} catch (MojoExecutionException e) {
			Throwable t= e.getCause();
			if(t instanceof IOException) {
				throw (IOException)t;
			}
			throw new IOException(e);
		}
    }

    @Override
    public String getMainScope() {
        return Artifact.SCOPE_RUNTIME;
    }
}
