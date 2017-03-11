/*
 * Copyright 2017 John Tipper (http://john-tipper.org)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.john_tipper.gradle.gitsemver

/**
 * Class to hold semantic version details
 */
class SemanticVersionExtension {

    /**
     * Major version as set by user
     */
    String majorVersion = "0"

    /**
     * Minor version as set by user
     */
    String minorVersion = "0"

    /**
     * Patch version
     */
    Integer patchVersion = 0

    /**
     * Major version as read from git, this may not be the same as the desired version
     */
    String gitMajorVersion = "0"

    /**
     * Minor version as read from git, this may not be the same as the desired version
     */
    String gitMinorVersion = "0"

    /**
     * Patch version as read from git
     */
    Integer gitPatchVersion = 0

    /**
     * Build number as determined by CI
     */
    int ciBuildNumber = 0

    /**
     * Is the build being performed by CI?
     */
    boolean isCI = false

    // Git Stuff
    /**
     * The current currentBranch has not yet been tagged with a version number
     */
    boolean isUntagged = true

    /**
     * The name of the current branch
     */
    String currentBranch = ""

    /*
     * Name of the master branch, should normally be "master"
     */
    String masterBranch = "master"

    /**
     * semantic version, also including details of the branch if not master and SNAPSHOT if not running as CI
     */
    String version

    /**
     * Reads information from git and sets internal variables appropriately
     */
    void readGit() {
        evaluateCurrentGitBranch()
        evaluateGitVersion()
    }

    /**
     * Get the current git branch and save it
     */
    void evaluateCurrentGitBranch() {
        currentBranch = SimpleCommandLineExecutor.executeCommand("git rev-parse --abbrev-ref HEAD")

        if (currentBranch != null) {
            currentBranch = currentBranch.substring(currentBranch.indexOf('/') + 1)  // get the bit of the branch after the '/': KEY-1234-some-branch-name
        }
    }

    /**
     * Evaluate the version from git and save it
     * The branch is assumed to be untagged if the number of additional commits on top of the tag, as returned by
     * git describe, is 0 or there are no tags matching "vXXX-XXX-XXX.  git describe returns "tag-commits-hash", where commits is a number.
     *
     * It requires the current branch to have been evaluated before calling this method.
     */
    void evaluateGitVersion() {
        String tagDescription = SimpleCommandLineExecutor.executeCommand("git describe --always --long")

        isUntagged = true
        if (tagDescription.startsWith("v") && (tagDescription =~ /-/).count == 2) {
            def tagComponents = tagDescription.tokenize('-') // split into [tag, num commits, hash]

            // if there are no commits on top of the last tag then the branch has already been tagged
            if (tagComponents[1] == "0") {
                isUntagged = false
            }

            def versionString = tagComponents[0].substring(1) // strip the v from the tag
            def versionList = versionString.tokenize('.') // create [major, minor, patch]

            // if we have 3 elements of the version [major, minor, patch] then save these
            if (versionList.size() == 3) {
                gitMajorVersion = versionList[0]
                gitMinorVersion = versionList[1]
                gitPatchVersion = versionList[2].toInteger() // this will fail if for some reason this is not a number
            }
        }

        // now determine whether we need to bump the patch number
        if (isMasterBranch() && isCI) {
            // if the minor version and major version as the same as read by git, then bump the patch version
            // but only if it untagged
            if (majorVersion == gitMajorVersion && minorVersion == gitMinorVersion) {
                if (isUntagged) {
                    patchVersion = gitPatchVersion + 1
                }
            } else {
                // the user has changed the major &/or the minor version so reset the patch version
                // @todo This doesn't take into account what happens if the user tries to lower the version numbers
                patchVersion = 0
            }

            version = "${majorVersion}.${minorVersion}.${patchVersion}-${ciBuildNumber}"

        } else {
            // either we're not on the master branch - in which case the version should include the branch details
            // and/or we're not running as CI, in which case the version should include -SNAPSHOT

            // if we're in a non-master branch, we probably have a branch name like "TYPE/BRANCHNAME"
            // If JIRA was used to create the branch, then:
            // - TYPE will be [feature, hotfix etc]
            // - BRANCHNAME will follow the format
            //     KEY-N-DESCRIPTION, where KEY is an alphanumeric key and N is a numeric value and DESCRIPTION is a hyphen
            //     or underscore separated string based on the JIRA issue name

            // let's try to simplify this to extract just the KEY-N and prefix with the first character
            if (currentBranch?.count('/') == 1) {
                def branchComponentList = currentBranch.tokenize('/') // break down the branch

                def typePrefix = branchComponentList[0].charAt(0)  // feature becomes f, etc

                // now simplify the branch name
                def branchNameList = branchComponentList[1].tokenize('-_')

                String branchDescription
                if (branchNameList.size() > 1) {
                    branchDescription = "$typePrefix" + branchNameList[0] + branchNameList[1]
                } else {
                    // don't know what we have, so just leave it
                    branchDescription = "$typePrefix" + branchComponentList[1]
                }

                version = "$majorVersion.$minorVersion.$patchVersion-$branchDescription-SNAPSHOT"

            } else {
                // perhaps the branch wasn't created by JIRA, so just leave it
                version = "$majorVersion.$minorVersion.$patchVersion-$currentBranch-SNAPSHOT"
            }
        }
    }

    /**
     * Logic for if we should do a git version tag
     *
     * Only if we are CI, we are on the master branch
     * and we haven't done the version tag yet in a
     * previous build
     *
     * @return should we do a version tag
     */
    boolean shouldVersionGitTag() {
        println "$isUntagged $isCI ${isMasterBranch()}"
        return (isUntagged && isCI && isMasterBranch())
    }

    /**
     * Logic for if we should do a git build tag
     *
     * Only if we are the CI
     *
     * @return should we do a build git tag
     */
    boolean shouldBuildGitTag() {
        return (isCI)
    }

    /**
     * Composes the String for the git tag
     * evaluateGitVersion() needs to have been called before calling this method
     * @return git version tag
     */
    String gitVersionTag() {
        return "v${majorVersion}.${minorVersion}.${patchVersion}"
    }


    boolean isMasterBranch() {
        return (currentBranch == masterBranch)
    }

}



