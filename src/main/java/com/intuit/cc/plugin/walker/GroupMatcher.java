/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.walker;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.maven.project.MavenProject;

/**
 * match projects with the same groupid
 */
public class GroupMatcher {
    
	// the base folder corresponding to groupId
	private File groupDir;
	// the number of characters to use from groupId
	private int groupMatch;
	// the pattern corresponding to groupId
	private Pattern pattern;

	public GroupMatcher(MavenProject project, String groupId) throws IOException {

        String absoluteProjectBase = project.getBasedir().getCanonicalPath().replace(File.separatorChar, '/');
        String projectFolder = Helper.getProjectFolderName(project);
        int lastSlash = getTailSegmentsMatch(absoluteProjectBase, projectFolder);
        if (lastSlash == 0)
            throw new IOException("cannot determine location of " 
                    + projectFolder + " relative to " + absoluteProjectBase);
        groupDir = new File(absoluteProjectBase.substring(0, absoluteProjectBase.length() - lastSlash));
        groupMatch = projectFolder.length() - lastSlash;

		pattern = Pattern.compile(groupId);		
	}

	/**
	 * determine how much of the tail end of two paths match
	 * 
	 * @param left
	 * @param right
	 * @return The number of characters from the tail of both paths which match.
	 *         The match will start on a path separator ('/')
	 */
	private static int getTailSegmentsMatch(String left, String right) {
		int l = left.length();
		int r = right.length();
		int lastSlash = 0;
		for (int i = 0; l > 0 && r > 0; i++) {
			char c = left.charAt(--l);
			if (c != right.charAt(--r)) {
				break;
			}
			if (c == '/') {
				lastSlash = i;
			}
		}
		return lastSlash;
	}

	/**
	 * Is the given id in the walking group?
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isIdInGroup(String groupId) {
		return pattern.matcher(groupId).matches();
	}
	
    /**
     * The root of the group
     * 
     * @param dependencyFolderName
     * @return
     */
    public File groupFolderFromDependencyName(String dependencyFolderName) {
        return new File(groupDir, dependencyFolderName.substring(groupMatch));
    }
	
}
