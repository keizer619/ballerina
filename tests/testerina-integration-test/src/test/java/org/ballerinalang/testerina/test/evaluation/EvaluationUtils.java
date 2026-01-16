/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.testerina.test.evaluation;

import org.ballerinalang.testerina.test.utils.CommonUtils;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class EvaluationUtils {
    private static final Boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.getDefault())
            .contains("win");
    private static final Path EVALUATION_OUTPUTS_DIR = Path
            .of("src", "test", "resources", "evaluation-outputs");

    private EvaluationUtils() {
    }

    public static void assertOutput(String outputFileName, String output) throws IOException {
        String regex = "(?m)^\\s*[/A-Za-z].*\\R?";
        output = CommonUtils.replaceExecutionTime(output);
        output = replaceProjectPath(output);
        if (IS_WINDOWS) {
            String fileContent = Files.readString(EVALUATION_OUTPUTS_DIR.resolve("windows").resolve(outputFileName));
            Assert.assertEquals(output.replaceAll("\r\n|\r", "\n").replaceAll(regex, "")
                            .stripTrailing(),
                    fileContent.replaceAll("\r\n|\r", "\n").replaceAll(regex, "")
                            .stripTrailing());
            return;
        }
        String fileContent = Files.readString(EVALUATION_OUTPUTS_DIR.resolve("unix").resolve(outputFileName));
        Assert.assertEquals(output.stripTrailing().replaceAll(regex, ""),
                fileContent.stripTrailing().replaceAll(regex, ""));
    }

    protected static String replaceProjectPath(String content) {
        content = CommonUtils.replaceVaryingString("Generating Test Report", "evaluation-tests", content);
        return CommonUtils.replaceVaryingString("warning: Could not find the required HTML " +
                "report tools for code coverage at", "lib", content);
    }
}
