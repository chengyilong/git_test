/*
 *2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

public class JarProject extends EclipseProject {
    
    void writeProjectFacetBuildCommands(Writer writer) throws IOException {
        writer.append("\t\t<buildCommand>\n"
                    +"\t\t\t<name>org.eclipse.wst.common.project.facet.core.builder</name>\n"
                    +"\t\t\t<arguments>\n"
                    +"\t\t\t</arguments>\n"
                    +"\t\t</buildCommand>\n"
                    +"\t\t<buildCommand>\n"
                    +"\t\t\t<name>org.eclipse.wst.validation.validationbuilder</name>\n"
                    +"\t\t\t<arguments>\n"
                    +"\t\t\t</arguments>\n"
                    +"\t\t</buildCommand>\n");
    }
    
    void writeProjectBuildCommands(Writer writer) throws IOException {
        writer.append("\t\t<buildCommand>\n"
	                +"\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>\n"
	                +"\t\t\t<arguments>\n"
	                +"\t\t\t</arguments>\n"
	                +"\t\t</buildCommand>\n");        
        writeProjectFacetBuildCommands(writer);        
    }

    void writeProjectBuildNatures(Writer writer) throws IOException {
        writer.append("\t\t<nature>org.eclipse.jem.workbench.JavaEMFNature</nature>\n"
        			+"\t\t<nature>org.eclipse.wst.common.modulecore.ModuleCoreNature</nature>\n"
        			+"\t\t<nature>org.eclipse.jdt.core.javanature</nature>\n"
        			+"\t\t<nature>org.eclipse.wst.common.project.facet.core.nature</nature>\n");
    }

    @Override
    void generateProject(Writer writer) throws IOException {
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
	                +"<projectDescription>\n"
	                +"\t<name>").append(projectName).append("</name>\n"
	                +"\t<comment></comment>\n"
	                
	                +"\t<projects>\n");
        for(Dependency dependency : eclipseProjects) {
            writer.append("\t\t<project>")
            		.append(getProjectNameFromDependency(dependency))
            		.append("</project>\n");
        }
        writer.append("\t</projects>\n"
                
                	+"\t<buildSpec>\n");
        writeProjectBuildCommands(writer);
        writer.append("\t</buildSpec>\n"
                
                	+"\t<natures>\n");
        writeProjectBuildNatures(writer);
        writer.append("\t</natures>\n");
        
        if(linkedSources!=null) {
            writer.append("\t<linkedResources>\n");
            for(File linkedSource : linkedSources) {
                writer.append("\t\t<link>\n"
	                        +"\t\t\t<name>").append(linkedSource.getName()).append("</name>\n"
	                        +"\t\t\t<type>2</type>\n"
	                        +"\t\t\t<location>").append(linkedSource.getAbsolutePath().replace('\\', '/')).append("</location>\n"
	                        +"\t\t</link>\n");
            }
            writer.append("\t</linkedResources>\n");           
        }
        
        writer.append("</projectDescription>\n");
    }

    void writeClasspathFacets(Writer writer) throws IOException {
    }
    
    void writeArtifactClasspathEntryAttributes(Writer writer, Artifact artifact) throws IOException {
    }
    
    void writeArtifactClasspathEntry(Writer writer, Artifact artifact) throws IOException {
        File jar= artifact.getFile();
        writer.append("\t<classpathentry kind=\"lib\" path=\"")
            .append(jar.getAbsolutePath().replace('\\', '/'));
        
        File sources= getSourcesFile(jar);
        if(sources.exists()) {
            writer.append("\" sourcepath=\"")
                    .append(sources.getAbsolutePath().replace('\\', '/'));
        }
        writer.append("\">\n");
        
        writeArtifactClasspathEntryAttributes(writer, artifact);
        writer.append("\t</classpathentry>\n");
    }
    
    private File getSourcesFile(File jar) {
        String name= jar.getName();
        String stub= name.substring(0, name.length()-4);
        return new File(jar.getParentFile(), stub+"-sources.jar");
    }

    void writeClasspathEntry(Writer writer, File location) throws IOException {
        if(location.isDirectory()) {
            writer.append("\t<classpathentry");
        	String name= location.getName();
        	if(name.endsWith("classes")) {
        		writer.append(" excluding=\"**/*.class\"");
        	}
            writer.append(" kind=\"src\" path=\"").append(name).append("\"/>\n");
        }
    }
    
    void writeClasspathEntry(Writer writer, Dependency dependency) throws IOException {
    	if(dependency.getType().equals("ear") ) {
    		// no dependency on the main ear project 
    		return;
    	}
    	String location= getProjectNameFromDependency(dependency);
        writer.append("\t<classpathentry combineaccessrules=\"false\" exported=\"true\" kind=\"src\" path=\"/")
                .append(location).append("\"/>\n");
    }
    
    void writeClasspathEntries(Writer writer) throws IOException {
        writeClasspathEntry(writer, new File(projectLocation, "java"));
        if(linkedSources!=null) {
            for(File linkedSource : linkedSources) {
                writeClasspathEntry(writer, linkedSource);
            }
        }
        writer.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\">\n");
        writeClasspathFacets(writer);       
        writer.append("\t</classpathentry>\n");
        for(Dependency dependency : eclipseProjects) {
       		writeClasspathEntry(writer, dependency);
        }
        
        for(Artifact artifact : artifacts) {
            writeArtifactClasspathEntry(writer, artifact);
        }

        writer.append("\t<classpathentry kind=\"output\" path=\".bin\"/>\n");
    }

    @Override
    void generateClasspath(Writer writer) throws IOException {
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                +"<classpath>\n");
        writeClasspathEntries(writer);
        writer.append("</classpath>");
    }

	@Override
	void generateComponent(Writer writer) throws IOException {
        writer.append("\t\t<wb-resource deploy-path=\"/\" source-path=\"/java\"/>\n");
	}

	@Override
	void generateFacet(Writer writer) throws IOException {
        writer.append("\t<fixed facet=\"jst.java\"/>\n"
    				+"\t<fixed facet=\"jst.utility\"/>\n"
    				+"\t<installed facet=\"jst.java\" version=\"1.7\"/>\n"
        			+"\t<installed facet=\"jst.utility\" version=\"1.0\"/>\n");
	}

	@Override
	void generateManifest(Writer writer) throws IOException {
        writer.append("Manifest-Version: 1.0\n"
        			+"Class-Path:"); 

        for(Dependency dependency : eclipseProjects) {
        	if(!Artifact.SCOPE_PROVIDED.equals(dependency.getScope())) {
        		writer.append("  ").append(getProjectNameFromDependency(dependency)).append(".jar\n");
        	}
        }
        for(Artifact artifact : artifacts) {
        	if(!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
        		writer.append("  ").append(artifact.getArtifactId()).append(".jar\n");
        	}
        }
        
        writer.append('\n');
	}

}
