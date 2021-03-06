/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2011-2014, Sebastian Staudt
 */

package com.github.koraktor.mavanagaiata.mojo;

import java.io.File;
import java.text.SimpleDateFormat;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.koraktor.mavanagaiata.git.CommitWalkAction;
import com.github.koraktor.mavanagaiata.git.GitRepositoryException;
import com.github.koraktor.mavanagaiata.git.GitTag;

/**
 * This goal allows to generate a changelog of the currently checked out branch
 * of the Git repository. It will use information from tags and commit messages
 * to build a reverse chronological summary of the development. It can be
 * configured to display the changelog or save it to a file.
 *
 * @author Sebastian Staudt
 * @since 0.2.0
 */
@Mojo(name ="changelog",
      defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GitChangelogMojo extends AbstractGitOutputMojo {

    /**
     * The format for the branch line
     *
     * @since 0.7.0
     */
    @Parameter(property = "mavanagaiata.changelog.gitHubLinks",
               defaultValue = "Commits on branch \"%s\"\n")
    protected String branchFormat;

    /**
     * Whether to create links to GitHub's compare view
     */
    @Parameter(property = "mavanagaiata.changelog.gitHubLinks",
               defaultValue = "false")
    protected boolean createGitHubLinks = false;

    /**
     * The string to prepend to every commit message
     */
    @Parameter(property = "mavanagaiata.changelog.commitPrefix",
               defaultValue = " * ")
    protected String commitPrefix;

    /**
     * The project name for GitHub links
     */
    @Parameter(property = "mavanagaiata.changelog.gitHubProject")
    protected String gitHubProject;

    /**
     * The format for the link to the history from the last tag to the current
     * branch on GitHub
     *
     * @since 0.7.0
     */
    @Parameter(property = "mavanagaiata.changelog.header",
               defaultValue = "\nSee Git history for changes in the \"%s\" branch since version %s at: %s")
    protected String gitHubBranchLinkFormat;

    /**
     * The format for the link to the branch history on GitHub
     *
     * @since 0.7.0
     */
    @Parameter(property = "mavanagaiata.changelog.header",
               defaultValue = "\nSee Git history for changes in the \"%s\" branch at: %s")
    protected String gitHubBranchOnlyLinkFormat;

    /**
     * The format for the link to the tag history on GitHub
     *
     * @since 0.7.0
     */
    @Parameter(property = "mavanagaiata.changelog.header",
               defaultValue = "\nSee Git history for version %s at: %s")
    protected String gitHubTagLinkFormat;

    /**
     * The user name for GitHub links
     */
    @Parameter(property = "mavanagaiata.changelog.gitHubUser")
    protected String gitHubUser;

    /**
     * The header to print above the changelog
     */
    @Parameter(property = "mavanagaiata.changelog.header",
               defaultValue = "Changelog\\n=========\\n")
    protected String header;

    /**
     * The file to write the changelog to
     *
     * @since 0.4.1
     */
    @Parameter(property = "mavanagaiata.changelog.outputFile")
    protected File outputFile;

    /**
     * Whether to skip tagged commits' messages
     * <br>
     * This is useful when usually tagging commits like "Version bump to X.Y.Z"
     */
    @Parameter(property = "mavanagaiata.changelog.skipTagged",
               defaultValue = "false")
    protected boolean skipTagged;

    /**
     * The format for a tag line
     *
     * @since 0.7.0
     */
    @Parameter(property = "mavanagaiata.changelog.tagFormat",
               defaultValue = "\nVersion %s – %s\n")
    protected String tagFormat;

    /**
     * Walks through the history of the currently checked out branch of the
     * Git repository and builds a changelog from the commits contained in that
     * branch.
     *
     * @throws MavanagaiataMojoException if retrieving information from the Git
     *         repository fails
     */
    public void run() throws MavanagaiataMojoException {
        try {
            this.outputStream.println(this.header);

            ChangelogWalkAction walkAction = new ChangelogWalkAction();
            this.repository.walkCommits(walkAction);

            if (this.createGitHubLinks) {
                if (walkAction.getCurrentTag() == null) {
                    this.insertGitHubLink(this.repository.getBranch(), null, true);
                } else {
                    this.insertGitHubLink(walkAction.getCurrentTag(), (GitTag) null);
                }
            }

            this.insertFooter();
        } catch (GitRepositoryException e) {
            throw MavanagaiataMojoException.create("Unable to generate changelog from Git", e);
        }
    }

    /**
     * Returns the output file for the generated changelog
     *
     * @return The output file for the generated changelog
     */
    public File getOutputFile() {
        return this.outputFile;
    }

    /**
     * Initializes this mojo
     *
     * @throws MavanagaiataMojoException if an error occurs while accessing the
     *         Git repository or the changelog file
     */
    protected boolean init() throws MavanagaiataMojoException {
        this.initConfiguration();

        return super.init();
    }

    protected void initConfiguration() {
        super.initConfiguration();

        this.branchFormat               = this.branchFormat.replaceAll("(|[^\\\\])\\\\n", "$1\n");
        this.commitPrefix               = this.commitPrefix.replaceAll("(|[^\\\\])\\\\n", "$1\n");
        this.gitHubBranchLinkFormat     = this.gitHubBranchLinkFormat.replaceAll("(|[^\\\\])\\\\n", "$1\n");
        this.gitHubBranchOnlyLinkFormat = this.gitHubBranchOnlyLinkFormat.replaceAll("(|[^\\\\])\\\\n", "$1\n");
        this.gitHubTagLinkFormat        = this.gitHubTagLinkFormat.replaceAll("(|[^\\\\])\\\\n", "$1\n");
        this.header                     = this.header.replaceAll("(|[^\\\\])\\\\n", "$1\n");
        this.tagFormat                  = this.tagFormat.replaceAll("(|[^\\\\])\\\\n", "$1\n");

        if (this.gitHubProject == null || this.gitHubProject.length() == 0 ||
            this.gitHubUser == null || this.gitHubUser.length() == 0) {
            this.createGitHubLinks = false;
        }
    }

    protected void insertGitHubLink(GitTag lastTag, String branch) {
        this.insertGitHubLink(lastTag.getName(), branch, true);
    }

    protected void insertGitHubLink(GitTag lastTag, GitTag currentTag) {
        String tagName = (currentTag == null) ? null : currentTag.getName();
        this.insertGitHubLink(lastTag.getName(), tagName, false);
    }

    /**
     * Generates a link to the GitHub compare / commits view and inserts it
     * into the changelog
     * <p>
     * If no current ref is provided, the generated text will link to the
     * commits view, listing all commits of the latest tag or the whole branch.
     * Otherwise the text will link to the compare view, listing all commits
     * that are in the current ref, but not in the last one.
     *
     * @param lastRef The last tag or branch in the changelog
     * @param currentRef The current tag or branch in the changelog
     * @param isBranch Whether the link is points to a branch
     */
    protected void insertGitHubLink(String lastRef, String currentRef, boolean isBranch) {
        String url = String.format("https://github.com/%s/%s/",
            this.gitHubUser,
            this.gitHubProject);
        if(currentRef == null) {
            url += String.format("commits/%s", lastRef);
        } else {
            url += String.format("compare/%s...%s", lastRef, currentRef);
        }

        String linkText;
        if (isBranch) {
            if (currentRef == null) {
                linkText = String.format(this.gitHubBranchOnlyLinkFormat, lastRef, url);
            } else {
                linkText = String.format(this.gitHubBranchLinkFormat, currentRef, lastRef, url);
            }
        } else {
            String tagName = (currentRef == null) ? lastRef : currentRef;
            linkText = String.format(this.gitHubTagLinkFormat, tagName, url);
        }

        this.outputStream.println(linkText);
    }

    /**
     * Sets the output file for the generated changelog
     *
     * @param outputFile The output file for the generated changelog
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    class ChangelogWalkAction extends CommitWalkAction {

        private GitTag currentTag;

        private SimpleDateFormat dateFormatter;

        private boolean firstCommit = true;

        private GitTag lastTag;

        public ChangelogWalkAction() {
            this.dateFormatter = new SimpleDateFormat(dateFormat);
        }

        public GitTag getCurrentTag() {
            return this.currentTag;
        }

        protected void run() throws GitRepositoryException {
            if (repository.getTags().containsKey(this.currentCommit.getId())) {
                this.lastTag = this.currentTag;
                this.currentTag = repository.getTags().get(this.currentCommit.getId());
                if (createGitHubLinks) {
                    if (this.lastTag == null) {
                        insertGitHubLink(this.currentTag, repository.getBranch());
                    } else {
                        insertGitHubLink(this.currentTag, this.lastTag);
                    }
                }

                this.dateFormatter.setTimeZone(this.currentTag.getTimeZone());
                String dateString = this.dateFormatter.format(this.currentTag.getDate());

                String tagLine = String.format(tagFormat, this.currentTag.getName(), dateString);
                if (this.firstCommit && tagLine.startsWith("\n")) {
                    tagLine = tagLine.replaceFirst("\n", "");
                }
                outputStream.println(tagLine);

                if (skipTagged) {
                    this.firstCommit = false;
                    return;
                }
            } else if (this.firstCommit) {
                outputStream.println(String.format(branchFormat, repository.getBranch()));
            }

            outputStream.println(commitPrefix + this.currentCommit.getMessageSubject());
            this.firstCommit = false;
        }

    }

}
