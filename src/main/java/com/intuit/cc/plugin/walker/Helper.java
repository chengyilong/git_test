package com.intuit.cc.plugin.walker;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

public abstract class Helper {
    
    /**
     * Get a maven coordinate for an Artifact, Dependency, or Project
     * 
     * @param groupId  The group name
     * @param artifactId  The artifact name
     * @param type  The artifact type
     * @return The maven coordinate
     */
    public static String getCoordinate(String groupId, String artifactId, String type) {
        return groupId + ':' + artifactId + ':' + type;
    }
    
    /**
     * Get a maven coordinate for a project
     * @param proejct the maven project
     * @return The maven coordinate
     */
    public static String getCoordinate(MavenProject project) {
        return getCoordinate(project.getGroupId(), project.getArtifactId(), project.getPackaging());
    }

    /**
     * Get a maven coordinate for an Artifact
     * @param artifact The maven artifact
     * @return The maven coordinate
     */
    public static String getCoordinate(Artifact artifact) {
        return getCoordinate(artifact.getGroupId(), artifact.getArtifactId(), artifact.getType());
    }
    
    /**
     * Get a maven coordinate for a dependency
     * @param dependency The maven dependency
     * @return The maven coordinate
     */
    public static String getCoordinate(Dependency dependency) {
        return getCoordinate(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType());
    }

    /**
     * Get the project folder name
     * @param groupId The project's group
     * @param artifactId The project' artifact
     * @return
     */
    public static String getProjectFolderName(String groupId, String artifactId) {
        return groupId.replace('.', '/')+'/'+artifactId;
    }

    /**
     * Get the project folder name from a dependency
     * @param dependency the maven dependency
     * @return The project folder name
     */
    public static String getProjectFolderName(Dependency dependency) {
        return getProjectFolderName(dependency.getGroupId(), dependency.getArtifactId());
    }

    /**
     * Get the project folder name for the project
     * @param project the maven project
     * @return The project folder name
     */
    public static String getProjectFolderName(MavenProject project) {
        return getProjectFolderName(project.getGroupId(), project.getArtifactId());
    }


}
