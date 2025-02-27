/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.isolation;

import io.ballerina.runtime.api.flags.SymbolFlags;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.TypedescType;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.internal.types.BObjectType;
import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.util.Lists;

import java.util.ArrayList;
import java.util.List;

import static org.ballerinalang.test.BAssertUtil.validateError;
import static org.ballerinalang.test.BAssertUtil.validateWarning;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test cases related to isolated workers.
 *
 * @since 2201.2.0
 */
public class IsolatedWorkerTest {
    private CompileResult startActionCompileResult;
    private CompileResult namedWorkerCompileResult;
    private CompileResult isolationInference1;
    private CompileResult isolationInference2;

    private static final String ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION =
            "invalid start action calling a non-isolated function in an 'isolated' function";
    private static final String ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT =
            "invalid start action accessing a non isolated expression in an argument of a " +
                    "async call of an 'isolated' function";
    private static final String ERROR_INVALID_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION =
            "invalid invocation of a non-isolated function in an 'isolated' function";
    private static final String ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION =
            "invalid access of mutable storage in an 'isolated' function";
    private static final String ERROR_INVALID_STRAND_ANNOTATION_IN_START_ACTION_IN_ISOLATED_FUNCTION =
            "'strand' annotation not allowed in a start action in an 'isolated' function";
    private static final String ERROR_INVALID_STRAND_ANNOTATION_IN_WORKER_IN_ISOLATED_FUNCTION =
            "'strand' annotation not allowed in a worker declaration in an 'isolated' function";
    private static final String WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED =
            "the 'strand' annotation will be deprecated";

    @BeforeClass
    public void setup() {
        startActionCompileResult = BCompileUtil.compile("test-src/isolated-workers/isolated_start_action.bal");
        namedWorkerCompileResult = BCompileUtil.compile("test-src/isolated-workers/isolated_named_worker.bal");
        isolationInference1 = BCompileUtil.compile(
                "test-src/isolated-workers/isolation_inference_with_start_action.bal");
        isolationInference2 = BCompileUtil.compile(
                "test-src/isolated-workers/isolation_inference_with_named_workers.bal");
    }

    @Test(dataProvider = "functionsToTestIsolatedStartAction")
    public void testIsolatedStartAction(String funcName) {
        BRunUtil.invoke(startActionCompileResult, funcName);
    }

    @DataProvider
    public Object[] functionsToTestIsolatedStartAction() {
        return new String[]{
                "testIsolatedStartActionWithFunctionCallExprWithNoArgs",
                "testIsolatedStartActionWithFunctionCallExprWithIsolatedArgs",
                "testIsolatedStartActionWithMethodCallExprWithNoArgs",
                "testIsolatedStartActionWithMethodCallExprWithIsolatedArgs",
                "testIsolatedStartActionWithClientRemoteMethodCallActionWithNoArgs",
                "testIsolatedStartActionWithClientRemoteMethodCallActionWithIsolatedArgs"
        };
    }

    @Test(dataProvider = "functionsToTestIsolatedNamedWorkers")
    public void testIsolatedNamedWorkers(String funcName) {
        BRunUtil.invoke(namedWorkerCompileResult, funcName);
    }

    @DataProvider
    public Object[] functionsToTestIsolatedNamedWorkers() {
        return new String[]{
                "testIsolatedWorkerInIsolatedFunction",
                "testIsolatedWorkerInIsolatedFunction2",
                "testIsolatedWorkerInIsolatedFunctionInClient",
                "testIsolatedWorkerInIsolatedFunctionInClient2",
                "testIsolatedWorkerInForkStmtInIsolatedFunction",
                "testWorkersInIsolatedResourceFunctions"
        };
    }

    @Test
    public void testNonIsolatedStartActionNegative() {
        CompileResult result = BCompileUtil.compile("test-src/isolated-workers/isolated_start_action_negative.bal");
        int i = 0;
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                18, 26);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                21, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                28, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 28, 30);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                30, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 30, 30);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                32, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 32, 30);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 32, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 73, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 82, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 94, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                105, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 114, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                114, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 126, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                126, 32);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                140, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 140, 40);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 140, 45);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 149, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                149, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 149, 38);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 149, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 161, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                161, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 161, 40);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 161, 45);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 187, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 196, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 208, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                219, 33);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 228, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                228, 33);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 240, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                240, 33);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                252, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 252, 40);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 252, 45);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 261, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                261, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 261, 38);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 261, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 273, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                273, 32);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 273, 40);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 273, 45);
        validateError(result, i++, ERROR_INVALID_STRAND_ANNOTATION_IN_START_ACTION_IN_ISOLATED_FUNCTION, 278, 21);
        validateError(result, i++, ERROR_INVALID_STRAND_ANNOTATION_IN_START_ACTION_IN_ISOLATED_FUNCTION, 287, 21);
        validateError(result, i++, ERROR_INVALID_STRAND_ANNOTATION_IN_START_ACTION_IN_ISOLATED_FUNCTION, 296, 21);
        Assert.assertEquals(result.getErrorCount(), i);
    }

    @Test
    public void testNonIsolatedNamedWorkerNegative() {
        CompileResult result = BCompileUtil.compile("test-src/isolated-workers/isolated_named_worker_negative.bal");
        int i = 0;
        validateError(result, i++, ERROR_INVALID_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION, 20, 9);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 35, 17);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 44, 9);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 52, 17);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 61, 24);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                70, 30);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 77, 33);
        validateError(result, i++, ERROR_INVALID_STRAND_ANNOTATION_IN_WORKER_IN_ISOLATED_FUNCTION, 85, 5);
        validateError(result, i++, ERROR_INVALID_STRAND_ANNOTATION_IN_WORKER_IN_ISOLATED_FUNCTION, 92, 9);
        validateError(result, i++, ERROR_INVALID_STRAND_ANNOTATION_IN_WORKER_IN_ISOLATED_FUNCTION, 96, 9);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                112, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 112, 38);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                115, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 115, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 116, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                119, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 119, 38);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                123, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 123, 46);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 124, 31);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                128, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 128, 46);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 129, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 133, 20);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                138, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 138, 38);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                141, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 141, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 142, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                145, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 145, 38);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                149, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 149, 46);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 150, 31);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                154, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 154, 46);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 155, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 159, 20);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                164, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 164, 38);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                167, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 167, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 168, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                171, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 171, 38);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                175, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 175, 46);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 176, 31);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                180, 43);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 180, 46);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 181, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 185, 20);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                195, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 195, 34);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                198, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 198, 38);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 199, 23);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                202, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 202, 34);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                206, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 206, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 207, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                211, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 211, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 212, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 216, 16);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                221, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 221, 34);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                224, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 224, 38);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 225, 23);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                228, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 228, 34);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                232, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 232, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 233, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                237, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 237, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 238, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 242, 16);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                247, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 247, 34);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                250, 35);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 250, 38);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 251, 23);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                254, 31);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 254, 34);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                258, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 258, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 259, 27);
        validateError(result, i++, ERROR_INVALID_ASYNC_INVOCATION_OF_NON_ISOLATED_FUNCTION_IN_ISOLATED_FUNCTION,
                263, 39);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_NON_ISOLATED_EXPR_IN_ARGUMENT, 263, 42);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 264, 27);
        validateError(result, i++, ERROR_INVALID_ACCESS_OF_MUTABLE_STORAGE_IN_ISOLATED_FUNCTION, 268, 16);
        Assert.assertEquals(result.getErrorCount(), i);
    }

    @Test(dataProvider = "functionsToTestIsolationInferenceWithStartAction")
    public void testIsolationInferenceWithStartAction(String funcName) {
        BRunUtil.invoke(isolationInference1, funcName);
    }

    @DataProvider
    public Object[] functionsToTestIsolationInferenceWithStartAction() {
        return new String[]{
                "testIsolationInferenceWithStartAction",
                "testNonIsolationInferenceWithStartAction",
                "testServiceClassMethodIsolationInference",
                "testClientClassMethodIsolationInference",
                "testIsolationInferenceWithStarActionInvokingPublicFunction"
        };
    }

    // This is called from the test file via the attach method of the listener.
    public static void testServiceDeclarationMethodIsolationInference(BObject listener, BObject s, Object name) {
        assertTrue(isResourceIsolated(s, "foo"));
        assertFalse(isResourceIsolated(s, "bar"));
        assertFalse(isResourceIsolated(s, "quo"));
        assertTrue(isRemoteMethodIsolated(s, "baz"));
        assertFalse(isRemoteMethodIsolated(s, "bam"));
        assertFalse(isRemoteMethodIsolated(s, "qux"));
    }

    private static boolean isResourceIsolated(Object val, String resourcePathString) {
        for (ResourceMethodType resourceMethodType : ((ServiceType) getType(val)).getResourceMethods()) {
            if (resourceMethodType.getAccessor().equals("get") &&
                    resourceMethodType.getResourcePath()[0].equals(resourcePathString)) {
                return SymbolFlags.isFlagOn(resourceMethodType.getFlags(), SymbolFlags.ISOLATED);
            }
        }

        return false;
    }

    public static boolean isRemoteMethodIsolated(Object val, String methodName) {
        ObjectType type = (ObjectType) getType(val);
        if (SymbolFlags.isFlagOn(type.getFlags(), SymbolFlags.SERVICE)) {
            return isIsolated(Lists.of(((ServiceType) type).getRemoteMethods()), methodName);
        }

        List<MethodType> remoteMethods = new ArrayList<>();
        for (MethodType method : type.getMethods()) {
            if (SymbolFlags.isFlagOn(method.getFlags(), SymbolFlags.REMOTE)) {
                remoteMethods.add(method);
            }
        }
        return isIsolated(remoteMethods, methodName);
    }

    private static boolean isIsolated(List<MethodType> methods, String methodNameString) {
        for (MethodType methodType : methods) {
            if (methodType.getName().equals(methodNameString)) {
                return SymbolFlags.isFlagOn(methodType.getFlags(), SymbolFlags.ISOLATED);
            }
        }

        throw new RuntimeException("method not found: " + methodNameString);
    }

    private static Type getType(Object val) {
        Type type = TypeUtils.getType(val);
        if (type.getTag() != TypeTags.TYPEDESC_TAG) {
            return type;
        }
        return ((TypedescType) type).getConstraint();
    }

    @Test(dataProvider = "functionsToTestIsolationInferenceWithNamedWorkers")
    public void testIsolationInferenceWithNamedWorkers(String funcName) {
        BRunUtil.invoke(isolationInference2, funcName);
    }

    @DataProvider
    public Object[] functionsToTestIsolationInferenceWithNamedWorkers() {
        return new String[]{
                "testIsolationInferenceWithNamedWorkers",
                "testNonIsolationInferenceWithNamedWorkersWithStrandAnnotation",
                "testServiceClassMethodIsolationInference",
                "testClientClassMethodIsolationInference",
                "testIsolatedInferenceWithWorkersAndStartsCallingPublicIsolatedFunctions"
        };
    }

    // This is called from the test file via the attach method of the listener.
    public static void testServiceDeclarationMethodIsolationInferenceWithWorkers(BObject listener, BObject s,
                                                                              Object name) {
        assertTrue(isResourceIsolated(s, "foo"));
        assertFalse(isResourceIsolated(s, "baz"));
        assertFalse(isResourceIsolated(s, "boo"));
        assertTrue(isRemoteMethodIsolated(s, "bar"));
        assertTrue(isMethodIsolated(s, "bam"));
    }

    public static boolean isMethodIsolated(Object val, String methodName) {
        BObjectType objectType = (BObjectType) getType(val);
        List<MethodType> methodTypes = Lists.of(objectType.getMethods());
        MethodType initMethod = objectType.getInitMethod();
        if (initMethod != null) {
            methodTypes.add(initMethod);
        }
        return isIsolated(methodTypes, methodName);
    }

    @Test
    public void testDeprecationWarningWithStrandAnnot() {
        CompileResult deprecationWarnRes = BCompileUtil.compile(
                "test-src/isolated-workers/deprecation_warning_with_strand_annot.bal");
        int i = 0;
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 18, 21);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 22, 24);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 34, 21);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 48, 21);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 59, 21);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 63, 5);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 70, 5);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 87, 5);
        validateWarning(deprecationWarnRes, i++, WARNING_USAGE_OF_STRAND_ANNOTATION_WILL_BE_DEPRECATED, 97, 5);
        Assert.assertEquals(deprecationWarnRes.getErrorCount(), 0);
        Assert.assertEquals(deprecationWarnRes.getWarnCount(), i);
    }

    @AfterClass
    public void tearDown() {
        startActionCompileResult = null;
        namedWorkerCompileResult = null;
        isolationInference1 = null;
        isolationInference2 = null;
    }
}
