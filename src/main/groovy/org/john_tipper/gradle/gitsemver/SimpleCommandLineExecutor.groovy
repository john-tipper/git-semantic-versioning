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
 * Very simple utility class to execute shell commands that is used to facilitate testing
 */
class SimpleCommandLineExecutor {

    /**
     * Execute a command and return the stdout response
     * @param command The command to execute
     * @return The output from the command
     */
    static String executeCommand(String command) {
        String output = command.execute().text.trim()
        return output;
    }

}
