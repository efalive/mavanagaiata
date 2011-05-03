/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2011, Sebastian Staudt
 */

package com.github.koraktor.mavanagaiata;

import java.io.IOException;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * This goal provides the full ID of the current Git commit in the
 * "mavanagaiata.commit.id", "mavanagaiata.commit.sha", "mvngit.commit.id",
 * "mvngit.commit.sha" properties. The abbreviated commit ID is stored in the
 * "mavanagaiata.commit.abbrev" and "mvngit.commit.abbrev" properties.
 *
 * @author Sebastian Staudt
 * @goal commit
 * @phase initialize
 * @requiresProject
 * @since 0.1.0
 */
public class GitCommitMojo extends AbstractGitMojo {

    /**
     * The ID (full and abbreviated) of the current Git commit out Git branch
     * is retrieved using a JGit Repository instance
     *
     * @see RevCommit#getName()
     * @see org.eclipse.jgit.lib.ObjectReader#abbreviate(org.eclipse.jgit.lib.AnyObjectId, int)
     * @throws MojoExecutionException if retrieving information from the Git
     *         repository fails
     */
    public void execute() throws MojoExecutionException {
        try {
            RevCommit commit = this.getHead();
            String abbrevId = this.repository.getObjectDatabase().newReader()
                .abbreviate(commit).name();
            String shaId = commit.getName();
            Date date = new Date(new Long(commit.getCommitTime()) * 1000);

            this.addProperty("commit.abbrev", abbrevId);
            this.addProperty("commit.date", date.toString());
            this.addProperty("commit.id", shaId);
            this.addProperty("commit.sha", shaId);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read Git commit information", e);
        }
    }
}
