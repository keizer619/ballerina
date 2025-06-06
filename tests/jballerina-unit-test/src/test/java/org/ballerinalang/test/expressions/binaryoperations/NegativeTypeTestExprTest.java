/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.expressions.binaryoperations;

import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.test.BAssertUtil;
import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @since 2.0.0
 * Class to test functionality of negative type test expressions.
 */
public class NegativeTypeTestExprTest {

    CompileResult result;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/expressions/binaryoperations/negative-type-test-expr.bal");
    }

    @Test
    public void testNegativeTypeTestExprNegative() {
        CompileResult negativeResult =
                BCompileUtil.compile("test-src/expressions/binaryoperations/negative-type-test-expr-negative.bal");
        int i = 0;
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 19, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 20, 9);
        BAssertUtil.validateError(negativeResult, i++, "incompatible types: 'int' will not be matched to 'float'", 28,
                9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 37, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 38, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'int' will not be matched to '(string|float)'", 46, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 55, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 56, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 64, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 65, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: '(int|string)' will not be matched to '(boolean|float)'", 73, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 91, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 92, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 93, 16);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 94, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 118, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 119, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 120, 16);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 121, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 131, 17);
        BAssertUtil.validateHint(negativeResult, i++,
                "unnecessary condition: expression will always evaluate to 'true'", 131, 32);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 133, 17);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 134, 17);
        BAssertUtil.validateWarning(negativeResult, i++, "unused variable 'b4'", 135, 5);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 135, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 141, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: '[int,string]' will not be matched to '[float,boolean]'", 142, 17);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 143, 17);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 144, 17);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 150, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 151, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 157, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 158, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 159, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 160, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 161, 18);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 177, 16);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 178, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 187, 13);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 187, 24);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 225, 8);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 226, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 229, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 230, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 241, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 242, 9);
        BAssertUtil.validateHint(negativeResult, i++,
                "expression will always evaluate to 'false'", 246, 9);
        BAssertUtil.validateError(negativeResult, i++, "unreachable code", 247, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'FooBar' will not be matched to 'BazTwo'", 255, 10);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: '(string|int)' will not be matched to '(float|boolean)'",
                262, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'xml<never>' will not be matched to 'string'",
                271, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'xml:Text' will not be matched to 'string'",
                272, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'xml<xml<xml:Text>>' will not be matched to 'string'",
                273, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'xml<xml<never>>' will not be matched to 'string'",
                274, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'xml' will not be matched to 'string'",
                275, 9);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: '(Baz|int)' will not be matched to 'Bar'", 280, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: '(Baz|int)' will not be matched to 'Qux'", 281, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Bar' will not be matched to 'Baz'", 284, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Bar' will not be matched to 'Quux'", 285, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Qux' will not be matched to 'Baz'", 288, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Qux' will not be matched to 'Quux'", 289, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Quux' will not be matched to 'Bar'", 292, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Quux' will not be matched to 'Qux'", 293, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'Quux' will not be matched to 'record {| int i; boolean b; |}'", 294, 17);
        BAssertUtil.validateError(negativeResult, i++,
                "incompatible types: 'ClosedRecordWithIntField' will not be matched to " +
                        "'record {| int i; string s; |}'", 297, 17);
        BAssertUtil.validateWarning(negativeResult, i++, "unused variable 'p'", 331, 9);
        BAssertUtil.validateError(negativeResult, i++, "incompatible types: 'Record' will not be matched to " +
                "'RecordWithIntFieldAndNeverRestField'", 358, 17);
        BAssertUtil.validateError(negativeResult, i++, "incompatible types: 'Record' will not be matched to " +
                "'RecordWithIntFieldAndEffectivelyNeverRestField'", 359, 17);
        Assert.assertEquals(negativeResult.getErrorCount(), i - 30 - 2);
        Assert.assertEquals(negativeResult.getHintCount(), 30);
        Assert.assertEquals(negativeResult.getWarnCount(), 2);
    }

    @Test
    public void testValueTypeInUnion() {
        Object returns = BRunUtil.invoke(result, "testValueTypeInUnion");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "string");
    }

    @Test
    public void testUnionTypeInUnion() {
        Object returns = BRunUtil.invoke(result, "testUnionTypeInUnion");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "numeric");
    }

    @Test
    public void testNestedTypeCheck() {
        Object arr = BRunUtil.invoke(result, "testNestedTypeCheck");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "boolean");
        Assert.assertEquals(returns.get(1).toString(), "int");
        Assert.assertEquals(returns.get(2).toString(), "string");
    }

    @Test
    public void testTypeInAny() {
        Object returns = BRunUtil.invoke(result, "testTypeInAny");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "string value: This is working");
    }

    @Test
    public void testNilType() {
        Object returns = BRunUtil.invoke(result, "testNilType");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "nil");
    }

    @Test
    public void testTypeChecksWithLogicalAnd() {
        Object returns = BRunUtil.invoke(result, "testTypeChecksWithLogicalAnd");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "string and boolean");
    }

    @Test
    public void testTypeCheckInTernary() {
        Object returns = BRunUtil.invoke(result, "testTypeCheckInTernary");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "A string");
    }

    @Test
    public void testSimpleRecordTypes_1() {
        Object returns = BRunUtil.invoke(result, "testSimpleRecordTypes_1");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "a is A1");
    }

    @Test
    public void testSimpleRecordTypes_2() {
        Object arr = BRunUtil.invoke(result, "testSimpleRecordTypes_2");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
    }

    @Test
    public void testSimpleRecordTypes_3() {
        Object arr = BRunUtil.invoke(result, "testSimpleRecordTypes_3");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
    }

    @Test
    public void testRecordsWithFunctionType_1() {
        Object arr = BRunUtil.invoke(result, "testRecordsWithFunctionType_1");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "a is not a man");
        Assert.assertEquals(returns.get(1).toString(), "Human: Piyal");
    }

    @Test
    public void testRecordsWithFunctionType_2() {
        Object arr = BRunUtil.invoke(result, "testRecordsWithFunctionType_2");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "Man: Piyal");
        Assert.assertEquals(returns.get(1).toString(), "Human: Piyal");
    }

    @Test
    public void testSealedRecordTypes() {
        Object arr = BRunUtil.invoke(result, "testSealedRecordTypes");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue((Boolean) returns.get(1));
    }

    @Test
    public void testObjectWithSameMembersButDifferentAlias() {
        Object arr = BRunUtil.invoke(result, "testObjectWithSameMembersButDifferentAlias");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertTrue(returns.get(3) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "I am same as person: John");
        Assert.assertEquals(returns.get(1).toString(), "I am a person: John");
        Assert.assertEquals(returns.get(2).toString(), "I am same as person: Doe");
        Assert.assertEquals(returns.get(3).toString(), "I am a person: Doe");
    }

    @Test
    public void testObjectWithUnorderedFields() {
        Object arr = BRunUtil.invoke(result, "testObjectWithUnorderedFields");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertTrue(returns.get(3) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "I am a person in order: John");
        Assert.assertEquals(returns.get(1).toString(), "I am a person not in order: John");
        Assert.assertEquals(returns.get(2).toString(), "I am a person in order: Doe");
        Assert.assertEquals(returns.get(3).toString(), "I am a person not in order: Doe");
    }

    @Test
    public void testPublicObjectEquivalency() {
        Object arr = BRunUtil.invoke(result, "testPublicObjectEquivalency");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "values: 5, foo");
        Assert.assertEquals(returns.get(1).toString(), "values: 5, foo, 6.7");
        Assert.assertEquals(returns.get(2).toString(), "n/a");
    }

    @Test
    public void testPrivateObjectEquivalency() {
        Object arr = BRunUtil.invoke(result, "testPrivateObjectEquivalency");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "values: 5, foo");
        Assert.assertEquals(returns.get(1).toString(), "values: 5, foo, 6.7");
        Assert.assertEquals(returns.get(2).toString(), "n/a");
    }

    @Test
    public void testAnonymousObjectEquivalency() {
        Object arr = BRunUtil.invoke(result, "testAnonymousObjectEquivalency");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "values: 5, foo, 6.7");
        Assert.assertEquals(returns.get(1).toString(), "values: 5, foo, 6.7, true");
        Assert.assertEquals(returns.get(2).toString(), "n/a");
    }

    @Test
    public void testObjectIsCheckWithCycles() {
        BRunUtil.invoke(result, "testObjectIsCheckWithCycles");
    }

    @Test
    public void testSimpleArrays() {
        Object arr = BRunUtil.invoke(result, "testSimpleArrays");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 5);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertTrue(returns.get(4) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue((Boolean) returns.get(1));
        Assert.assertFalse((Boolean) returns.get(2));
        Assert.assertFalse((Boolean) returns.get(3));
        Assert.assertFalse((Boolean) returns.get(4));
    }

    @Test
    public void testRecordArrays() {
        Object arr = BRunUtil.invoke(result, "testRecordArrays");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertTrue((Boolean) returns.get(2));
        Assert.assertTrue((Boolean) returns.get(3));
    }

    @Test
    public void testUnionType() {
        BRunUtil.invoke(result, "testUnionType");
    }

    @Test
    public void testClosedArrayType() {
        BRunUtil.invoke(result, "testClosedArrayType");
    }

    @Test
    public void testInferredArrayType() {
        BRunUtil.invoke(result, "testInferredArrayType");
    }

    @Test
    public void testEmptyArrayType() {
        BRunUtil.invoke(result, "testEmptyArrayType");
    }

    @Test
    public void testSimpleTuples() {
        Object arr = BRunUtil.invoke(result, "testSimpleTuples");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 5);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertTrue(returns.get(4) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue((Boolean) returns.get(1));
        Assert.assertTrue((Boolean) returns.get(2));
        Assert.assertFalse((Boolean) returns.get(3));
        Assert.assertFalse((Boolean) returns.get(4));
    }

    @Test
    public void testTupleWithAssignableTypes_1() {
        Object arr = BRunUtil.invoke(result, "testTupleWithAssignableTypes_1");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertTrue((Boolean) returns.get(2));
        Assert.assertTrue((Boolean) returns.get(3));
    }

    @Test
    public void testTupleWithAssignableTypes_2() {
        Object returns = BRunUtil.invoke(result, "testTupleWithAssignableTypes_2");
        Assert.assertTrue(returns instanceof Boolean);
        Assert.assertFalse((Boolean) returns);
    }

    @Test
    public void testRestType() {
        BRunUtil.invoke(result, "testRestType");
    }

    @Test
    public void testSimpleUnconstrainedMap_1() {
        Object arr = BRunUtil.invoke(result, "testSimpleUnconstrainedMap_1");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(0));
        Assert.assertTrue((Boolean) returns.get(1));
    }

    @Test
    public void testSimpleUnconstrainedMap_2() {
        Object arr = BRunUtil.invoke(result, "testSimpleUnconstrainedMap_2");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 5);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertTrue(returns.get(4) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertTrue((Boolean) returns.get(2));
        Assert.assertTrue((Boolean) returns.get(3));
        Assert.assertTrue((Boolean) returns.get(4));
    }

    @Test
    public void testSimpleConstrainedMap() {
        Object arr = BRunUtil.invoke(result, "testSimpleConstrainedMap");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertFalse((Boolean) returns.get(2));
        Assert.assertFalse((Boolean) returns.get(3));
    }

    @Test
    public void testJSONTypeCheck() {
        Object arr = BRunUtil.invoke(result, "testJSONTypeCheck");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 7);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertTrue(returns.get(2) instanceof BString);
        Assert.assertTrue(returns.get(3) instanceof BString);
        Assert.assertTrue(returns.get(4) instanceof BString);
        Assert.assertTrue(returns.get(5) instanceof BString);
        Assert.assertTrue(returns.get(6) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "json int");
        Assert.assertEquals(returns.get(1).toString(), "json float");
        Assert.assertEquals(returns.get(2).toString(), "json string: hello");
        Assert.assertEquals(returns.get(3).toString(), "json boolean");
        Assert.assertEquals(returns.get(4).toString(), "json array");
        Assert.assertEquals(returns.get(5).toString(), "json object");
        Assert.assertEquals(returns.get(6).toString(), "json null");
    }

    @Test
    public void testJsonArrays() {
        Object arr = BRunUtil.invoke(result, "testJsonArrays");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(0));
        Assert.assertTrue((Boolean) returns.get(1));
        Assert.assertFalse((Boolean) returns.get(2));
    }

    @Test
    public void testFiniteType() {
        Object arr = BRunUtil.invoke(result, "testFiniteType");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertTrue((Boolean) returns.get(2));
    }

    @Test
    public void testFiniteTypeInTuple() {
        Object arr = BRunUtil.invoke(result, "testFiniteTypeInTuple");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertTrue((Boolean) returns.get(2));
        Assert.assertFalse((Boolean) returns.get(3));
    }

    @Test
    public void testFiniteTypeInTuplePoisoning() {
        Object arr = BRunUtil.invoke(result, "testFiniteTypeInTuplePoisoning");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "on");
        Assert.assertEquals(returns.get(1).toString(), "on");
    }

    @Test
    public void testFiniteType_1() {
        Object returns = BRunUtil.invoke(result, "testFiniteType_1");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "a is a fruit");
    }

    @Test
    public void testFiniteType_2() {
        Object returns = BRunUtil.invoke(result, "testFiniteType_2");
        Assert.assertTrue(returns instanceof BString);
        Assert.assertEquals(returns.toString(), "a is an Apple");
    }

    @Test
    public void testFiniteTypeAsBroaderType_1() {
        Object returns = BRunUtil.invoke(result, "testFiniteTypeAsBroaderType_1");
        Assert.assertTrue(returns instanceof Boolean);
        Assert.assertFalse((Boolean) returns);
    }

    @Test
    public void testFiniteTypeAsBroaderType_2() {
        Object arr = BRunUtil.invoke(result, "testFiniteTypeAsBroaderType_2");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(1));
    }

    @Test
    public void testUnionWithFiniteTypeAsFiniteTypeTrue() {
        Object arr = BRunUtil.invoke(result, "testUnionWithFiniteTypeAsFiniteTypeTrue");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(1));
    }

    @Test
    public void testUnionWithFiniteTypeAsFiniteTypeFalse() {
        Object arr = BRunUtil.invoke(result, "testUnionWithFiniteTypeAsFiniteTypeFalse");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(1));
    }

    @Test
    public void testFiniteTypeAsFiniteTypeTrue() {
        Object returns = BRunUtil.invoke(result, "testFiniteTypeAsFiniteTypeTrue");
        Assert.assertTrue(returns instanceof Boolean);
        Assert.assertFalse((Boolean) returns);
    }

    @Test
    public void testIntersectingUnionTrue() {
        Object arr = BRunUtil.invoke(result, "testIntersectingUnionTrue");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(1));
    }

    @Test
    public void testIntersectingUnionFalse() {
        Object arr = BRunUtil.invoke(result, "testIntersectingUnionFalse");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(1));
    }

    @Test
    public void testValueTypeAsFiniteTypeTrue() {
        Object arr = BRunUtil.invoke(result, "testValueTypeAsFiniteTypeTrue");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(1));
    }

    @Test
    public void testValueTypeAsFiniteTypeFalse() {
        Object arr = BRunUtil.invoke(result, "testValueTypeAsFiniteTypeFalse");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(1));
    }

    @Test
    public void testError_1() {
        Object arr = BRunUtil.invoke(result, "testError_1");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 4);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(1));
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(2));
        Assert.assertTrue(returns.get(3) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(3));
    }

    @Test
    public void testError_2() {
        Object arr = BRunUtil.invoke(result, "testError_2");
        BArray returns = (BArray) arr;
        Assert.assertEquals(returns.size(), 3);
        Assert.assertTrue(returns.get(0) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue(returns.get(1) instanceof Boolean);
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertTrue(returns.get(2) instanceof Boolean);
        Assert.assertTrue((Boolean) returns.get(2));
    }

    @Test
    public void testClosedArrayAsOpenArray() {
        Object returns = BRunUtil.invoke(result, "testClosedArrayAsOpenArray");
        Assert.assertFalse((Boolean) returns);
    }

    @Test
    public void testFunctions1() {
        Object arr = BRunUtil.invoke(result, "testFunctions1");
        BArray returns = (BArray) arr;
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertFalse((Boolean) returns.get(1));
        Assert.assertFalse((Boolean) returns.get(2));
        Assert.assertTrue((Boolean) returns.get(3));
    }

    @Test
    public void testFunctions2() {
        Object arr = BRunUtil.invoke(result, "testFunctions2");
        BArray returns = (BArray) arr;
        Assert.assertFalse((Boolean) returns.get(0));
        Assert.assertTrue((Boolean) returns.get(1));
        Assert.assertFalse((Boolean) returns.get(2));
        Assert.assertTrue((Boolean) returns.get(3));
    }

    @Test
    public void testFuture() {
        Object returns = BRunUtil.invoke(result, "testFutureTrue");
        Assert.assertTrue((Boolean) returns);
        returns = BRunUtil.invoke(result, "testFutureFalse");
        Assert.assertFalse((Boolean) returns);
    }

    @Test
    public void testMapAsRecord() {
        BRunUtil.invoke(result, "testMapAsRecord");
    }

    @Test
    public void testXMLNeverType() {
        BRunUtil.invoke(result, "testXMLNeverType");
    }

    @Test
    public void testXMLTextType() {
        BRunUtil.invoke(result, "testXMLTextType");
    }

    @Test
    public void testRecordIntersections() {
        BRunUtil.invoke(result, "testRecordIntersections");
    }

    @Test
    public void testRecordIntersectionWithEffectivelyNeverFields() {
        BRunUtil.invoke(result, "testRecordIntersectionWithEffectivelyNeverFields");
    }

    @Test
    public void testRecordIntersectionWithFunctionFields() {
        BRunUtil.invoke(result, "testRecordIntersectionWithFunctionFields");
    }

    @AfterClass
    public void tearDown() {
        result = null;
    }
}
