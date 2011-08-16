/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.eclipse;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public abstract class EclipseProject {

    public static EclipseProject getProject(String type) {
        if(type.equals("ejb")) {
            return new EjbProject();
        }
        else if(type.equals("ear")) {
            return new EarProject();
        }
        else {
            return new JarProject();
        }
    }

    protected Log log;
    MavenProject project;
    String projectName;
    Set<Artifact> artifacts;
    Set<Dependency> eclipseProjects;
    boolean testScope;
    File projectLocation;
	File baseDir;
    List<File> linkedSources;
    CreateProject callback;
    
    public void initialize(CreateProject callback, Log log, MavenProject project, Set<Artifact> artifacts, 
    		Set<Dependency> eclipseProjects, boolean testScope) throws IOException {
    	this.callback= callback;
        this.log= log;
        this.project= project;
        this.projectName= getProjectName(project.getArtifactId(), testScope);
        this.artifacts= artifacts;
        this.eclipseProjects= eclipseProjects;
        this.testScope= testScope;
    }
        
    String getProjectName(String name, boolean isTest) {
		if(isTest) {
			return name+"-tests";
		}
		else {
			return name;
		}
    }
    
    String getProjectNameFromDependency(Dependency dependency) {
    	return getProjectName(
    			dependency.getArtifactId(), dependency.getType().equals("test-jar"));
    }
    
    public void generate() throws IOException {
    	baseDir= project.getBasedir().getCanonicalFile();
        projectLocation= new File(baseDir, "src/main");
        
        if(testScope) {
        	addTestDependencyOnMain();
            projectLocation= new File(baseDir, "src/test");
        }
        generateSet();
    }

	private void addTestDependencyOnMain() {
		if(testDependsOnMain()) {
			Dependency test= new Dependency();
			test.setScope(Artifact.SCOPE_COMPILE);
			test.setSystemPath(baseDir.getAbsolutePath());
			test.setType(project.getArtifact().getType());
			test.setArtifactId(project.getArtifactId());
			test.setVersion(project.getVersion());
		    eclipseProjects.add(test);
		}
	}
    
    boolean testDependsOnMain() {
    	// is there any source in main?
    	// when called from addTestDependencyOnMain, projectLocation still points at src/main
		return projectLocation.isDirectory();
	}

	void generateSet() throws IOException {
        log.debug("eclipse subProject:"+projectLocation.getAbsolutePath());
        if(projectExists()) {
            linkedSources= getLinkedSources();                
            generateProject();
            generateClasspath();
            generateSettings();
            generateMetaInf();
        }
    }

    boolean projectExists() {
		if(projectLocation.isDirectory()) {
			return true;
		}
    	if(testScope) {
    		return false;
    	}
    	return projectLocation.getParentFile().isDirectory() && projectLocation.mkdirs();
    }

	private static FileFilter NON_EMPTY_DIRECTORY= new FileFilter() {
        @Override
        public boolean accept(File path) {
            return path.isDirectory() && path.listFiles().length>0;
        }        
    };

    private List<File> getLinkedSources() {
        List<File> linkedSources= new ArrayList<File>();
        
        File target= new File(baseDir, "target");
        if(target.isDirectory()) {
        	if(!testScope) {
	            File generatedSources= new File(target, "generated-sources");
	            if(generatedSources.isDirectory()) {
	                for(File linkedSource : generatedSources.listFiles(NON_EMPTY_DIRECTORY)) {
	                    linkedSources.add(linkedSource);
	                }
	            }
        	}
        
	        File resources= new File(projectLocation, "resources");
	        if(resources.isDirectory()) {
	            File classes= new File(target, testScope ?"test-classes" :"classes");
	            linkedSources.add(classes);
	        }
        }

        return linkedSources.size()>0 ?linkedSources :null;
    }

    void generateProject() throws IOException {
        File project= new File(projectLocation, ".project");
        log.debug("generating :"+project.getAbsolutePath());
        
        OutputStream os= new FileOutputStream(project);
        Writer writer= new OutputStreamWriter(os, Charset.forName("UTF-8"));
        try {
            generateProject(writer);            
        }
        finally {
            try {
                writer.close();
            }
            catch(IOException ignore) {
            }
        }
    }
    
    abstract void generateProject(Writer writer) throws IOException;
    
    void generateClasspath() throws IOException {
        File classpath= new File(projectLocation, ".classpath");
        log.debug("generating :"+classpath.getAbsolutePath());
        
        OutputStream os= new FileOutputStream(classpath);
        Writer writer= new OutputStreamWriter(os, Charset.forName("UTF-8"));
        try {
            generateClasspath(writer);            
        }
        finally {
            try {
                writer.close();
            }
            catch(IOException ignore) {
            }
        }
    }
    
    abstract void generateClasspath(Writer writer) throws IOException ;
    
    void generateSettings() throws IOException {
        File settings= new File(projectLocation, ".settings");
        if(!settings.exists()) {
            String path= settings.getAbsolutePath();
            log.debug("generating :"+path);
            if(!settings.mkdir()) {
                throw new IOException("can not create "+path);
            }
        }
        generateComponent(settings);
        generateFacet(settings);
    }

    abstract void generateComponent(Writer writer) throws IOException ;
    
    void generateComponent(File settings) throws IOException {
        File component= new File(settings, "org.eclipse.wst.common.component");
        log.debug("generating :"+component.getAbsolutePath());
        
        OutputStream os= new FileOutputStream(component);
        Writer writer= new OutputStreamWriter(os, Charset.forName("UTF-8"));
        try {
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    +"<project-modules id=\"moduleCoreId\" project-version=\"1.5.0\">\n"
                    +"\t<wb-module deploy-name=\"").append(projectName).append("\">\n");
            generateComponent(writer);
            writer.append("\t</wb-module>\n"
                    +"</project-modules>\n");
        }
        finally {
            try {
                writer.close();
            }
            catch(IOException ignore) {
            }
        }
    }
    
    abstract void generateFacet(Writer writer) throws IOException;
    
    void generateFacet(File settings) throws IOException {
        File component= new File(settings, "org.eclipse.wst.common.project.facet.core.xml");
        log.debug("generating :"+component.getAbsolutePath());
        
        OutputStream os= new FileOutputStream(component);
        Writer writer= new OutputStreamWriter(os, Charset.forName("UTF-8"));
        try {
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    +"<faceted-project>\n");
            generateFacet(writer);
            writer.append("</faceted-project>\n");
        }
        finally {
            try {
                writer.close();
            }
            catch(IOException ignore) {
            }
        }
    }

    void generateMetaInf() throws IOException {
        File metaInf= new File(projectLocation, "java/META-INF");
        if(!metaInf.exists()) {
            String path= metaInf.getAbsolutePath();
            log.debug("generating :"+path);
            if(!metaInf.mkdirs()) {
                throw new IOException("can not create "+path);
            }
        }
        generateManifest(metaInf);    
    }
    
    abstract void generateManifest(Writer writer) throws IOException;

    void generateManifest(File metaInf) throws IOException {
        File classpath= new File(metaInf, "MANIFEST.MF");
        log.debug("generating :"+classpath.getAbsolutePath());
        
        OutputStream os= new FileOutputStream(classpath);
        Writer writer= new OutputStreamWriter(os, Charset.forName("UTF-8"));
        try {
            generateManifest(writer);            
        }
        finally {
            try {
                writer.close();
            }
            catch(IOException ignore) {
            }
        }
	}

	public String getMainScope() {
        return Artifact.SCOPE_COMPILE;
    }

    public EclipseProject getTestProject() {
        return this;
    }
}
