/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.earxml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

/**
 * A mojo to create an application xml with ejbs in runtime dependencies order
 * @goal create-application-xml
 * @phase prepare-package
 */
public class CreateApplicationXml
    extends AbstractMojo
{
    /**
     * Directory where the deployment descriptor file(s) will be auto-generated.
     *
     * @parameter expression="${project.build.directory}/ordered-application.xml"
     */
    protected String generatedDescriptorLocation;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The artifact repository to use.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;
    
    /**
     * The artifact factory to use.
     * 
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;
    
    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression=
     *  "${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * The artifact metadata source to use.
     * 
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     * 
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     * 
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;
    
    // for maven
    public CreateApplicationXml() {
    }
    
    // for Eclipse plugin
    public CreateApplicationXml(String generatedDescriptorLocation,
			MavenProject project, ArtifactRepository localRepository,
			ArtifactFactory artifactFactory,
			ArtifactResolver artifactResolver,
			ArtifactMetadataSource artifactMetadataSource,
			ArtifactCollector artifactCollector,
			DependencyTreeBuilder dependencyTreeBuilder, Log log) {
		this.generatedDescriptorLocation = generatedDescriptorLocation;
		this.project = project;
		this.localRepository = localRepository;
		this.artifactFactory = artifactFactory;
		this.artifactResolver = artifactResolver;
		this.artifactMetadataSource = artifactMetadataSource;
		this.artifactCollector = artifactCollector;
		this.dependencyTreeBuilder = dependencyTreeBuilder;
		setLog(log);
	}

	/**
     * Map of Artifact to EarModule
     */
    private Set<Artifact> earArtifacts= new LinkedHashSet<Artifact>();

    public void execute() throws MojoExecutionException {
        addDependencies();        
        try {
            createApplicationXml();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    private void createApplicationXml() throws IOException {
        Writer ps= new OutputStreamWriter(new FileOutputStream(generatedDescriptorLocation), Charset.forName("UTF-8"));        
        ps.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")        
            .append("<!DOCTYPE application PUBLIC \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\" \"http://java.sun.com/dtd/application_1_3.dtd\">\n")
            .append("<application>\n")
            .append("<display-name>").append(project.getName()).append("</display-name>\n");
        
        for(Artifact artifact : earArtifacts) {
            ps.append("<module><ejb>")
            		.append(artifact.getFile().getName())
            		.append("</ejb></module>\n");            
        }
        ps.append("</application>");
        ps.close();        
    }

    void addDependencies() throws MojoExecutionException {
        getLog().debug( "addDependencies for project "+project);
    	
        ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
        try
        {
            DependencyNode node =
                dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                           artifactMetadataSource, filter, artifactCollector );
            addDependencies(filter, node);
        }
        catch ( Exception exception )
        {
            throw new MojoExecutionException( "Cannot build project dependency tree", exception );
        }
    }
    
    @SuppressWarnings( "unchecked" )
    void addDependencies(ScopeArtifactFilter filter, DependencyNode node) throws ArtifactResolutionException, ArtifactNotFoundException {
        Artifact artifact = node.getArtifact();
        getLog().debug( "addDependencies for artifact: "+artifact);
        if(!artifact.isOptional()
                && filter.include( artifact ) )
        {
        	if( !project.getArtifact().equals(artifact) ) {
        		artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
        	}
            
            for ( DependencyNode child : (List<DependencyNode>)node.getChildren() ) {
                addDependencies(filter, child);
            }            
            if ( "ejb".equals( artifact.getType() )
            		&& !artifact.equals( project.getArtifact() ) ) {
                earArtifacts.add(artifact);
                getLog().debug( "added dependency: "+artifact );
            }
        }
    }        
}
