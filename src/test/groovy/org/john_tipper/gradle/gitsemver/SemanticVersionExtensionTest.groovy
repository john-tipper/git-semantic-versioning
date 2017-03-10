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

import spock.lang.Specification

/**
 * Created by admin on 09/03/2017.
 */
class SemanticVersionExtensionTest extends Specification {

    def setup() {
        GroovyMock(SimpleCommandLineExecutor, global: true)
    }

    def "evaluateGitVersion calls git describe with correct arguments"() {
        given:
            SemanticVersionExtension semanticVersionExtension = new SemanticVersionExtension()

        when:
            semanticVersionExtension.evaluateGitVersion()

        then:
            1 * SimpleCommandLineExecutor.executeCommand("git describe --always --long") >> { cmd -> "v1.2.3-0-g0259fa5" }

    }

    def "evaluateGitVersion sets semantic versions correctly when user increments versions evaluating master branch, is CI and is untagged"() {
        given:
        SemanticVersionExtension semanticVersionExtension = new SemanticVersionExtension()
        semanticVersionExtension.currentBranch = "master"
        semanticVersionExtension.isCI = true
        semanticVersionExtension.majorVersion = '1'
        semanticVersionExtension.minorVersion = '3'

        when:
        semanticVersionExtension.evaluateGitVersion()

        then:
        1 * SimpleCommandLineExecutor.executeCommand("git describe --always --long") >> { cmd -> "v1.2.3-4-g0259fa5" }
        semanticVersionExtension.gitVersionTag() == "v1.3.0"
        semanticVersionExtension.version == "1.3.0-0"
    }

    def "evaluateGitVersion keeps same version, bumps patch number and resets build number when user keeps versions the same and evaluating master branch, is CI and is untagged"() {
        given:
        SemanticVersionExtension semanticVersionExtension = new SemanticVersionExtension()
        semanticVersionExtension.currentBranch = "master"
        semanticVersionExtension.isCI = true
        semanticVersionExtension.majorVersion = '1'
        semanticVersionExtension.minorVersion = '2'

        when:
        semanticVersionExtension.evaluateGitVersion()

        then:
        1 * SimpleCommandLineExecutor.executeCommand("git describe --always --long") >> { cmd -> "v1.2.3-4-g0259fa5" }
        semanticVersionExtension.gitVersionTag() == "v1.2.4"
        semanticVersionExtension.version == "1.2.4-0"

    }

    def "evaluateGitVersion sets semantic versions correctly when user increments versions evaluating non-master branch, is CI and is untagged"() {
        given:
        SemanticVersionExtension semanticVersionExtension = new SemanticVersionExtension()
        semanticVersionExtension.currentBranch = "feature/KEY-1234-some-branch-name"
        semanticVersionExtension.isCI = true
        semanticVersionExtension.majorVersion = '1'
        semanticVersionExtension.minorVersion = '3'

        when:
        semanticVersionExtension.evaluateGitVersion()

        then:
        1 * SimpleCommandLineExecutor.executeCommand("git describe --always --long") >> { cmd -> "v1.2.3-4-g0259fa5" }
        semanticVersionExtension.gitVersionTag() == "v1.3.0"
        semanticVersionExtension.version == "1.3.0-fKEY1234-SNAPSHOT"
    }


    def "evaluateGitVersion sets semantic versions correctly when user increments versions evaluating non-master non-JIRA branch, is CI and is untagged"() {
        given:
        SemanticVersionExtension semanticVersionExtension = new SemanticVersionExtension()
        semanticVersionExtension.currentBranch = "some-branch-that-was-not-made-by-jira"
        semanticVersionExtension.isCI = true
        semanticVersionExtension.majorVersion = '1'
        semanticVersionExtension.minorVersion = '3'

        when:
        semanticVersionExtension.evaluateGitVersion()

        then:
        1 * SimpleCommandLineExecutor.executeCommand("git describe --always --long") >> { cmd -> "v1.2.3-4-g0259fa5" }
        semanticVersionExtension.gitVersionTag() == "v1.3.0"
        semanticVersionExtension.version == "1.3.0-some-branch-that-was-not-made-by-jira-SNAPSHOT"
    }

}
