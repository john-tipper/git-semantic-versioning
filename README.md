#Git Semantic Versioning Plugin

This Gradle plugin implements Git tagging with [semantic versioning](http://semver.org) for projects under Git version control.  To summarise:
>Given a version number MAJOR.MINOR.PATCH, increment the:
> 
> MAJOR version when you make incompatible API changes,  
> MINOR version when you add functionality in a backwards-compatible manner, and  
> PATCH version when you make backwards-compatible bug fixes.

##What it does

The plugin will set the version of the Gradle project to follow semantic versioning.  It will also tag the current branch of Git repository of the project with an appropriate label, then push to a defined remote repository.  The tagging is designed to be performed only by a continuous integration server such as Jenkins (but this can be over-ridden, see [Usage](###usage)).  The branches within the Git repository are assumed to follow [GitHub flow](http://scottchacon.com/2011/08/31/github-flow.html), i.e. only the master branch is tagged with version labels.

Two Gradle tasks are created by applying the plugin and are found within the `Git Semantic Versioning` tasks group:

 * `makeBuildGitTag`
 * `makeVersionGitTag`

Both tasks have a dependency on a task called `test`, which will exist in groovy / java projects.  This dependency will prevent the tagging of the branch for failing builds.

Additionally, the Gradle version of the project will be set according to the following patterns:

  * If being built by CI and on master branch:  
      
      **$MAJOR.$MINOR.$PATCH-$CI_BUILD_NUMBER**

  * If being built by CI and not the master branch:  
    
      **$MAJOR.$MINOR.$PATCH-$BRANCH**

  * If not being built by CI:  
    
      **$MAJOR.$MINOR.$PATCH-$BRANCH-SNAPSHOT**

    Where BRANCH is a description of the branch, based on its name.  If the branch was created using JIRA then the name may be something like `feature/KEY-1234-some-feature-details` where `KEY-1234` relates to the JIRA issue that was used to create the branch.
    
    The plugin will attempt to simplify $BRANCH to `fKEY-1234`, where `f` is simply the first character of `feature`.  If $BRANCH cannot be simplified then it will be left unchanged.  

The reasoning behind adding `-SNAPSHOT` is that an assumption is made that only the CI will push to the releases repository and to push to a SNAPSHOTS repository will require this suffix.

##Tasks
###`makeBuildGitTag`

This task will tag the git branch with a build number, where the build number is the value of the environment variable `$BUILD_NUMBER`, or 0 if this variable does not exist.  The build number variable is populated by Jenkins when it builds a job.  This tagging will only happen if the build is defined as being run as part of a CI process, which is determined by looking for the property `ci` within the Gradle project, see [Usage](###usage).  As this number is monotonically increased by Jenkins, every build by Jenkins is therefore distinguishable from all others.  The tag will be applied to the current branch, irrespective of which branch it is.

The label applied is a lightweight one (i.e. it is not annotated, c.f. [Git documentation](https://git-scm.com/book/en/v2/Git-Basics-Tagging)).
 
###`makeVersionGitTag`

This task will only run if the following conditions are met:
 * the project is being built with the `ci` property (normally to be set only by Jenkins, c.f. [Usage](###usage)).
 * the master branch is the branch being built.
 * the current branch has not already been tagged by a previous build.
 
The latter condition will occur if nothing has changed in the source, but another CI build has been triggered (e.g. nightly build). 

The label applied is an annotated one (i.e. applied using `git tag -a`, c.f. [Git documentation](https://git-scm.com/book/en/v2/Git-Basics-Tagging)) and follows the pattern:

    ${major}.${minor}.${patch}-${buildNumber}

##Usage

Within `build.gradle`:

    buildscript {
      repositories {
        mavenCentral()
      }

      dependencies {
        classpath 'org.john-tipper.gradle:git-semantic-versioning:1.1' // replace 1.1 with whatever version you need
      }
    }

    apply plugin: 'git-semantic-versioning'
    
Within the project's `gradle.properties` define the major and minor versions of the project (i.e. you need to increment these manually):

    majorVersion = 1  // major version that will be applied, this must be an integer to be compliant with semantic versioning
    minorVersion = 2  // minor version, this is a string but it may not represent an integer, to allow for things like release candidates e.g. 2.1rc2
    
Call the tasks with a `ci` property if the task is being run by Jenkins; this can be done by calling with the option `-Pci`, e.g.:

    gradlew makeBuildGitTag -Pci
    
These tasks should be called when you know that the code is ok, i.e. after testing, code quality examination etc.

##Deployment of this plugin to MavenCentral

There is an `uploadarchives` task in Gradle that will deploy the built plugin to MavenCentral.  It requires the following properties to be set (and which should normally be set in `~/.gradle/gradle.properties` in order to prevent the passwords being committed to source control):
  
    sonatypeUser=<OSSRH username>  
    sonatypePassword=<OSSRH password>  
  
    signing.keyId=<Created by gpg2>  
    signing.password=<Password used to create the key>  
    signing.secretKeyRingFile=<Path to the secret key file>
    
##To Do
* Very basic testing has been done, but not for all possible combinations of branch name, CI/not-CI, version numbers etc.
* No testing of the plugin `apply()` method has been done.
* No account taken for what happens if the user tries to lower the version numbers from what is present in the tag.

Feel free to submit a PR if you'd like.