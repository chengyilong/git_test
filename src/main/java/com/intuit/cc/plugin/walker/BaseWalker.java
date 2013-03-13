/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.walker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * If the goals of this session include install or deploy, invoke all dependent
 * projects that match the groupid with the same goals as this session.
 */
public class BaseWalker extends AbstractMojo {
	/**
	 * The maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

    /**
     * The user settings
     * 
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
	private Settings settings;
	
	/**
	 * The project artifacts
	 * 
	 * @parameter expression="${project.attachedArtifacts}"
	 * @required
	 * @readonly
	 */
	private List<Artifact> attachedArtifacts;

    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;
    
	/**
	 * The group we're matching
	 * 
	 * @parameter expression="${project.groupId}"
	 * @required
	 */
	private String groupId;
    
    /**
     * The beginning time of this build
     *
     * @parameter expression="${walkerStartTime}" default-value="0"
     * @required
     */
    protected long walkerStartTime;
    
    /**
     * skip getting the version
     *
     * @parameter expression="${skipWalk}" default-value="false"
     */
    private boolean skipWalk;

    private GroupMatcher matcher;
	private Invoker invoker;
	// the versions of various artifacts
    final private Map<String, String> artifactVersions= new HashMap<String, String>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().debug("running "+getClass().getName());

		if(skipWalk || isAlreadyRunning()) {
			return;
		}
        saveContext();
		
        if(walkerStartTime == 0L) {
            walkerStartTime = session.getStartTime().getTime();
        }

        try {
            matcher= new GroupMatcher(project, groupId);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

		invoker = new DefaultInvoker();
		
        checkParent();
        iterateDependencies();
        
        writeDependencies();
	}

	private final boolean isAlreadyRunning() {
        return getPluginContext().get(BaseWalker.class)!=null;
	}

	@SuppressWarnings("unchecked")
	private final void saveContext() {
        // add self to context so only one walk occurs
        getPluginContext().put(BaseWalker.class, this);
	}

	/**
	 * Call at install or site phase to write the dependencies' versions.
	 * @throws MojoExecutionException
	 */
	void writeDependencies() throws MojoExecutionException {
    	addArtifacts();
    	writeArtifactVersions();		
	}

	/**
	 * Check if parent should be walked
	 * @throws MojoExecutionException
	 */
	protected void checkParent() throws MojoExecutionException {
		MavenProject parent= project.getParent();
        if(parent!=null && matcher.isIdInGroup(parent.getGroupId())) {
            String dependencyFolderName = Helper.getProjectFolderName(parent);
			File dependencyFolder = matcher.groupFolderFromDependencyName(dependencyFolderName);
			if(dependencyFolder.exists()) { 			
				checkCoordinate(Helper.getCoordinate(project), dependencyFolder);
			} else {
				getLog().info("Checking ~/.m2 for " + parent.getArtifactId());
				if(!isArtifactInM2(parent.getGroupId(), parent.getArtifactId(), parent.getVersion())) {
					getLog().error("Error: " + parent.getArtifactId() + "-" + parent.getVersion() + " cannot be found in ~/.m2");
					System.exit(1);
				}
			}
        }
	}

	/**
	 * Check each dependency if it should be walked
	 * @throws MojoExecutionException
	 */
	@SuppressWarnings("unchecked")
    protected void iterateDependencies() throws MojoExecutionException {
		for(Dependency dependency : (List<Dependency>)project.getDependencies()) {
            if(matcher.isIdInGroup(dependency.getGroupId())) {
                String dependencyFolderName = Helper.getProjectFolderName(dependency);
				File dependencyFolder = matcher.groupFolderFromDependencyName(dependencyFolderName);
				if(dependencyFolder.exists()) { 			
					checkCoordinate(Helper.getCoordinate(project), dependencyFolder);
				} else {
					getLog().info("Checking ~/.m2 for " + dependency.getArtifactId());
					if(!isArtifactInM2(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
						getLog().error("Error: " + dependency.getArtifactId() + "-" + dependency.getVersion() + " cannot be found in ~/.m2");
						System.exit(1);
					}
				}
            }
        };
	}

    /**
     * Should the given dependent be built?
     * @param coordinate The maven coordinate
     * @param dependentFolder The dependent's folder 
     * @throws MojoExecutionException
     */
	protected void checkCoordinate(String coordinate, File dependentFolder) throws MojoExecutionException {
		// has this session already executed this dependency?
        if(hasDependency(coordinate)) {
    		getLog().debug(coordinate+" has been built by a child");
        	return;
        }
        
        File propertiesFile = getVersionFile(dependentFolder);
		if(propertiesFile.exists()) {
        	if(walkerStartTime < propertiesFile.lastModified()) {
        		getLog().debug(coordinate+" has been built by a sibling");
        		return;
        	}
        }
		invoke(dependentFolder);
        readArtifactVersions(propertiesFile);
	}
	
	/**
	 * Check the existence of jar/pom file of artifact in local maven repository
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return boolean
	 */
	protected boolean isArtifactInM2 (String groupId, String artifactId, String version) {
		String folderPath = System.getProperty("user.home") + "/.m2/repository/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version;
		File artifactJar = new File(folderPath, artifactId + "-" + version + ".jar");
		File artifactPom = new File(folderPath, artifactId + "-" + version + ".pom");
		if(artifactJar.exists() || artifactPom.exists()) {
			return true;
		}
		return false;
	}

	/**
	 * Invoke maven on a dependent project
	 * 
	 * @param dependencyFolder
	 *            The dependency folder
	 * @throws MojoExecutionException
	 */
	protected void invoke(File dependencyFolder) throws MojoExecutionException {
		List<String> args = new ArrayList<String>();

		addGoals(args);
		addLogging(args);
		addProfiles(args);
		addDefines(args);
		addSettings(args);

    	args.add("-D"); args.add("walkerStartTime="+walkerStartTime);

		if (getLog().isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("invoke ");
			sb.append(dependencyFolder).append(" with");
			for (String arg : args) {
				sb.append(' ').append(arg);
			}
			sb.append('\n');
			getLog().debug(sb);
		}

		InvocationRequest request = new DefaultInvocationRequest();
		request.setGoals(args);
		request.setBaseDirectory(dependencyFolder);

		try {
			InvocationResult result = invoker.execute(request);
			if (result.getExitCode() != 0) {
				throw new MavenInvocationException("Build failed with exit code=" + result.getExitCode());
			}
		} catch (MavenInvocationException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

    /**
	 * Add the invocation goals
	 * 
	 * @param args
	 *            The destination argument list
	 */
	@SuppressWarnings("unchecked")
	private void addGoals(List<String> args) {
		for (String goal : (List<String>)session.getGoals()) {
			args.add(goal);
		}
	}

	/**
	 * Add logging parameters
	 * 
	 * @param args The destination argument list
	 */
	private void addLogging(List<String> args) {
		if (!getLog().isInfoEnabled()) {
			args.add("--quiet");
		}
		if (getLog().isDebugEnabled()) {
			args.add("--debug");
		}
	}

	/**
	 * Add any active profiles. Filter duplicates.
	 * 
	 * @param args The destination argument list
	 */
	@SuppressWarnings("unchecked")
    private void addProfiles(List<String> args) {
	    List<String> profiles= settings.getActiveProfiles();
		if (profiles != null) {
			// remove duplicate profiles
			Set<String> profileNames = new HashSet<String>();
			// unfortunately, The project set of profiles does not include those activated on command line
	        for(String profile : profiles) {
				profileNames.add(profile);
			}
			for (String profileName : profileNames) {
				args.add("-P");
				args.add(profileName);
			}
		}
	}

	/**
	 * Add the command line property definitions. Filter any properties from
	 * System.properties or Maven embedder
	 * 
	 * @param args The destination argument list
	 */
	private void addDefines(List<String> args) {
		for (Entry<Object, Object> property : session.getExecutionProperties().entrySet()) {
			String key = (String) property.getKey();

			int idx = Arrays.binarySearch(REMOVE_PROPERTIES, key);
			if (idx >= 0) {
				continue;
			}
			int priorPoint = -Arrays.binarySearch(REMOVE_PREFIXES, key) - 2;
			if (priorPoint >= 0 && key.startsWith(REMOVE_PREFIXES[priorPoint])) {
				continue;
			}
			args.add("-D");
			args.add(key + "=" + property.getValue());
		}
	}

	/**
	 * add any settings which may have originally occurred as command line argument 
	 * @param args The destination argument list
	 */
    private void addSettings(List<String> args) {
        if(settings.isOffline()) {
            args.add("--offline");
        }        
    }

	// properties to remove
	private static String[] REMOVE_PROPERTIES = { 
		"classworlds.conf", 
		"file.separator",
		"file.encoding.pkg", 
		"line.separator",
		"path.separator",
		"awt.toolkit"
	};
	// property prefixes to remove
	private static String[] REMOVE_PREFIXES = { 
		"java.",
		"os.", 
		"sun.", 
		"env.",
		"user."
	};
	static {
		Arrays.sort(REMOVE_PROPERTIES);
		Arrays.sort(REMOVE_PREFIXES);
	}

    /**
     * Does this repository know about the given artifact?
     * @param coordinate The maven coordinate of the artifact
     * @return true if, and only if, the coordinate is in this repository.
     */
	private boolean hasDependency(String coordinate) {
		return artifactVersions.containsKey(coordinate);
	}

	/**
	 * Add the artifacts from a project into this repository
	 * @param project The project from which to extract artifact names and versions
	 */
    private void addArtifacts() {    	
    	Artifact mainArtifact = project.getArtifact();
		artifactVersions.put(Helper.getCoordinate(mainArtifact), mainArtifact.getVersion()); 
    		
    	for(Artifact artifact : attachedArtifacts) {
    		artifactVersions.put(Helper.getCoordinate(artifact), artifact.getVersion()); 
    	}
    }

    /**
     * Get the version file
     * @param projectFolder The project folder
     * @return The File to write versions to or read versions from
     */
    private static File getVersionFile(File projectFolder) {
    	File target	= new File(projectFolder, "target");
    	if(!target.exists()) {
    		target.mkdir();
    	}
    	return new File(target, "walker.versions");
    }

    /**
     * Write artifact versions
	 * @param versionsFile The file containing the artifact versions
     * @throws MojoExecutionException
     */
	private void writeArtifactVersions() throws MojoExecutionException {	
		File versionsFile= getVersionFile(project.getBasedir());
    	try {
	    	Properties properties= new Properties();
	    	for(Map.Entry<String,String> entry : artifactVersions.entrySet()) {
	    		properties.put(entry.getKey(), entry.getValue());
	    	}
	    	
	    	OutputStream os= new FileOutputStream(versionsFile);
	    	try {
		    	properties.store(os, "dependent versions");
	    	}
	    	finally {
	    		try {
	    			os.close();
	    		}
	    		catch(IOException ex) {
	    	    	getLog().error("could not close "+versionsFile, ex);    			
	    		}
	    	}
    	} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
    
	/**
	 * Read artifact versions
	 * @param versionsFile The file containing the artifact versions
	 * @throws MojoExecutionException
	 */
	private void readArtifactVersions(File versionsFile) throws MojoExecutionException {
    	if(!versionsFile.exists()) {
    		return;
    	}
    	try {
	    	InputStream is= new FileInputStream(versionsFile);
	    	try {
		    	Properties properties= new Properties();
		    	properties.load(is);
		    	for(Map.Entry<Object,Object> entry : properties.entrySet()) {
		    		artifactVersions.put((String)entry.getKey(), (String)entry.getValue());
		    	}
	    	}
	    	finally {
	    		try {
	    			is.close();
	    		}
	    		catch(IOException ex) {
	    	    	getLog().error("could not close "+versionsFile, ex);    			
	    		}
	    	}
    	} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
