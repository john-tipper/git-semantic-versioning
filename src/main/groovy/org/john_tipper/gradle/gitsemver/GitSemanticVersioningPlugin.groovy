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

import org.gradle.api.Plugin
import org.gradle.api.Project

class GitSemanticVersioningPlugin implements Plugin<Project> {
    def void apply(Project project) {
        def ext = project.extensions.create("semanticVersionExtension", SemanticVersionExtension)

        ext.setMajorVersion((project.findProperty('majorVersion') ?: "0") as String)
        ext.setMinorVersion((project.findProperty('minorVersion') ?: "0") as String)

        def buildNumber = System.getenv("BUILD_NUMBER") ?: "0"
        ext.setCiBuildNumber(buildNumber.toInteger())
        ext.setIsCI(project.rootProject.hasProperty('ci'))
        ext.readGit()

        project.tasks.create("makeVersionGitTag") {
            outputs.upToDateWhen { false }
            group 'Git Semantic Versioning'
            onlyIf {
                ext.shouldVersionGitTag()
            }

            dependsOn 'test'

            doLast {
                def tag = ext.gitVersionTag()
                SimpleCommandLineExecutor.executeCommand(["git", "tag", "-a", tag, "-m", "Version $tag"])
                SimpleCommandLineExecutor.executeCommand(["git", "push", "origin", tag])
            }
        }

        project.tasks.create("makeBuildGitTag") {
            outputs.upToDateWhen { false }
            group 'Git Semantic Versioning'

            onlyIf {
                ext.shouldBuildGitTag()
            }

            mustRunAfter 'makeVersionGitTag'
            dependsOn 'test'

            doLast {
                def tag = "#${ext.ciBuildNumber}"
                SimpleCommandLineExecutor.executeCommand(["git", "tag", tag])
                SimpleCommandLineExecutor.executeCommand(["git", "push", "origin", tag])
            }
        }

        project.version = ext.version
        println "Version applied to project by plugin 'git-semantic-versioning': ${project.version}"

    }
}
