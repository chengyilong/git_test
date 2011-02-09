/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

import com.intuit.cc.plugin.earxml.CreateApplicationXml;
import com.intuit.cc.plugin.walker.GroupMatcher;
import com.intuit.cc.plugin.walker.Helper;

/**
 * A mojo to create eclipes .project / .classpath
 * @goal generate-eclipse
 * @phase prepare-package
 */
public class CreateProject
    extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * The group we're matching
     * 
     * @parameter expression="${project.groupId}"
     * @required
     */
    private String groupId;
    private GroupMatcher matcher;

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

    
    private ScopeArtifactFilter artifactFilter;
    private ScopeFilter dependencyFilter;
    
    /**
     * Set of Artifacts
     */
    private Set<Artifact> artifacts;

    /**
     * Set of eclipse project dependencies
     */
    private Set<Dependency> eclipseProjects;
        
    public void execute() throws MojoExecutionException {
        EclipseProject eclipseProject= EclipseProject.getProject(project.getArtifact().getType());
        if(eclipseProject==null) {
            return;
        }
        try {
            matcher= new GroupMatcher(project, groupId);

            String scope= eclipseProject.getMainScope();
            artifactFilter = new ScopeArtifactFilter(scope);
            dependencyFilter= new ScopeFilter(scope);
            addGroupDependencies();
            addNonGroupDependencies();

            eclipseProject.initialize(this, getLog(), project, artifacts, eclipseProjects, false);
            eclipseProject.generate();

            eclipseProject= eclipseProject.getTestProject();
            artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_TEST);
            dependencyFilter= new ScopeFilter(Artifact.SCOPE_TEST);
            addGroupDependencies();
            addNonGroupDependencies();

            eclipseProject.initialize(this, getLog(), project, artifacts, eclipseProjects, true);
            eclipseProject.generate();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    void generateApplicationXml(String location) throws MojoExecutionException {
    	CreateApplicationXml cax= new CreateApplicationXml(location,
    			project, localRepository, artifactFactory, artifactResolver, artifactMetadataSource,
    			artifactCollector, dependencyTreeBuilder, getLog());
    	cax.execute();
    }
    
    /**
     * Check each dependency if it should be walked
     * @throws MojoExecutionException
     */
    @SuppressWarnings("unchecked")
    private void addGroupDependencies() throws MojoExecutionException {
        eclipseProjects= new HashSet<Dependency>();
        for(Dependency dependency : (List<Dependency>)project.getDependencies()) {
            if(dependencyFilter.isIncluded(dependency.getScope())
                    && matcher.isIdInGroup(dependency.getGroupId())) {
                
                String dependencyFolderName = Helper.getProjectFolderName(dependency);
                File dependencyFolder = matcher.groupFolderFromDependencyName(dependencyFolderName);
                dependency.setSystemPath(dependencyFolder.getAbsolutePath());
                if(eclipseProjects.add(dependency)) {
                    getLog().debug("addGroupDependency - "+dependency);                	
                }
            }
        };
    }
    
    private void addNonGroupDependencies() throws MojoExecutionException {
        try
        {
            DependencyNode node =
                dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                           artifactMetadataSource, artifactFilter, artifactCollector );
            
            artifacts= new HashSet<Artifact>();
            addDependencies(artifactFilter, node);
        }
        catch ( Exception exception ) {
            throw new MojoExecutionException( "Cannot build project dependency tree", exception );
        }
    }
    
    @SuppressWarnings( "unchecked" )
    private void addDependencies(ScopeArtifactFilter filter, DependencyNode node) throws ArtifactResolutionException, ArtifactNotFoundException {
        Artifact artifact = node.getArtifact();
        
        if(!artifact.isOptional()
                && filter.include( artifact ) ) {
            
        	if( !project.getArtifact().equals(artifact) ) {
        		artifactResolver.resolve(artifact, project.getRemoteArtifactRepositories(), localRepository);
        	}
            
            for ( DependencyNode child : (List<DependencyNode>)node.getChildren() ) {
                addDependencies(filter, child);
            }
            
            if ( !artifact.equals(project.getArtifact()) 
                    && !"pom".equals(artifact.getType()) ) {
                if( !matcher.isIdInGroup(artifact.getGroupId()) ) {
                    getLog().debug( "addLibraryDependency: "+artifact );
                    artifacts.add(artifact);
                }
            }
        }
    }
}
