/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ballerinalang.compiler.bir.codegen;

import io.ballerina.identifier.Utils;
import org.ballerinalang.compiler.BLangCompilerException;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.types.SelectivelyImmutableReferenceType;
import org.ballerinalang.model.types.TypeKind;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.wso2.ballerinalang.compiler.bir.codegen.internal.AsyncDataCollector;
import org.wso2.ballerinalang.compiler.bir.codegen.internal.BIRVarToJVMIndexMap;
import org.wso2.ballerinalang.compiler.bir.codegen.internal.LambdaFunction;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JCast;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JInstruction;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JLargeArrayInstruction;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JLargeMapInstruction;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JMethodCallInstruction;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JType;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JTypeTags;
import org.wso2.ballerinalang.compiler.bir.codegen.split.JvmConstantsGen;
import org.wso2.ballerinalang.compiler.bir.model.BIRInstruction;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.FieldAccess;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewTable;
import org.wso2.ballerinalang.compiler.bir.model.BIROperand;
import org.wso2.ballerinalang.compiler.bir.model.InstructionKind;
import org.wso2.ballerinalang.compiler.bir.model.VarKind;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SchedulerPolicy;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BIntersectionType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.util.Flags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCMPL;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IOR;
import static org.objectweb.asm.Opcodes.ISHR;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.IUSHR;
import static org.objectweb.asm.Opcodes.IXOR;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmCastGen.getTargetClass;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmCodeGenUtil.toNameString;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ADD_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ANNOTATION_MAP_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ANNOTATION_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ARRAY_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ARRAY_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BAL_ENV_CLASS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BYTE_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.B_LIST_INITIAL_VALUE_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.B_MAPPING_INITIAL_VALUE_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.B_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.B_STRING_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.CURRENT_MODULE_VAR_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.DECIMAL_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.DOUBLE_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.EQUALS_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ERROR_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.FILL_AND_GET;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.FUNCTION_POINTER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_BOXED_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_ELEMENT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_ELEMENT_OR_NIL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_STRING_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_UNBOXED_BOOLEAN_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_UNBOXED_FLOAT_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_UNBOXED_INT_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.GET_VALUE_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.HANDLE_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.INSTANTIATE_FUNCTION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.INT_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JSON_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JVM_INIT_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JVM_TO_UNSIGNED_INT_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.LIST_INITIAL_EXPRESSION_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.LIST_INITIAL_SPREAD_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.LIST_INITIAL_VALUE_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.LONG_STREAM;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAPPING_INITIAL_KEY_VALUE_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAPPING_INITIAL_SPREAD_FIELD_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAP_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAP_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAP_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MATH_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MODULE_INIT_CLASS_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.OBJECT_SELF_INSTANCE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.OBJECT_TYPE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.RECORD_TYPE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.REG_EXP_FACTORY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.SHORT_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.STRING_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TABLE_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TABLE_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TABLE_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPEDESC_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPEDESC_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPE_CHECKER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.VALUE_COMPARISON_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.VALUE_OF_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.XML_FACTORY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.XML_QNAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.XML_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.ANY_TO_JBOOLEAN;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.ARRAY_ADD_BSTRING;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.ARRAY_ADD_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.BAL_ENV_PARAM;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.BOBJECT_GET;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.BSTRING_CONCAT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CHECK_IS_TYPE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.COMPARE_DECIMALS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.COMPARE_OBJECTS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_REGEXP;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_ASSERTION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_ATOM_QUANTIFIER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_CAPTURING_GROUP;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_CHAR_CLASS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_CHAR_SET;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_CHAR_SET_RANGE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_DISJUNCTION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_FLAG_EXPR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_FLAG_ON_OFF;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_LITERAL_CHAR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_QUANTIFIER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_RE_SEQUENCE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_XML_COMMENT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_XML_ELEMENT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_XML_PI;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CREATE_XML_TEXT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CRETAE_XML_SEQUENCE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.DECIMAL_NEGATE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.DOUBLE_VALUE_OF_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.FP_INIT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_ANNOTATION_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_BSTRING_FOR_ARRAY_INDEX;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_MAP_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_MODULE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_STRING_AT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_STRING_FROM_ARRAY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_TYPEDESC_OF_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.HANDLE_MAP_STORE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.HANDLE_TABLE_STORE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_ARRAY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_ARRAY_WITH_INITIAL_VALUES;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_BAL_ENV;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_ERROR_WITH_TYPE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_LIST_INITIAL_EXPRESSION_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_LIST_INITIAL_SPREAD_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_MAPPING_INITIAL_SPREAD_FIELD_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_TABLE_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_WITH_STRING;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_XML_QNAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INSTANTIATE_WITH_INITIAL_VALUES;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.JSON_GET_ELEMENT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.JSON_SET_ELEMENT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.LONG_STREAM_RANGE_CLOSED;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.OBJECT_TYPE_DUPLICATE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.OBJECT_TYPE_IMPL_INIT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PASS_B_STRING_RETURN_B_STRING;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PASS_B_STRING_RETURN_UNBOXED_BOOLEAN;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PASS_B_STRING_RETURN_UNBOXED_DOUBLE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PASS_B_STRING_RETURN_UNBOXED_LONG;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PASS_OBJECT_RETURN_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PROCESS_FP_ANNOTATIONS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.PROCESS_OBJ_CTR_ANNOTATIONS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.RETURN_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.SET_DECIMAL_RETURN_DECIMAL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.SET_DEFAULT_VALUE_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.SET_ON_INIT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.TWO_OBJECTS_ARGS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.TYPE_DESC_CONSTRUCTOR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.TYPE_DESC_CONSTRUCTOR_WITH_ANNOTATIONS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_ADD_CHILDREN;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_CHILDREN;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_CHILDREN_FROM_STRING;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_CONCAT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_GET_ATTRIBUTE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_GET_ITEM;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.XML_SET_ATTRIBUTE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTypeGen.getTypeDesc;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmValueGen.getTypeDescClassName;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmValueGen.getTypeValueClassName;

/**
 * Instruction generator helper class to hold its enclosing pkg and index map.
 *
 * @since 1.2.0
 */
public class JvmInstructionGen {

    public static final String TO_UNSIGNED_LONG = "toUnsignedLong";
    public static final String ANON_METHOD_DELEGATE = "$anon$method$delegate$";
    //this any type is currently set from package gen class
    static BType anyType;
    private final MethodVisitor mv;
    private final BIRVarToJVMIndexMap indexMap;
    private final String currentPackageName;
    private final JvmPackageGen jvmPackageGen;
    private final JvmTypeGen jvmTypeGen;
    private final JvmCastGen jvmCastGen;
    private final JvmConstantsGen jvmConstantsGen;
    private final SymbolTable symbolTable;
    private final AsyncDataCollector asyncDataCollector;
    private final JvmTypeTestGen typeTestGen;
    private final Map<String, LambdaFunction> functions;
    private final String moduleInitClass;

    public JvmInstructionGen(MethodVisitor mv, BIRVarToJVMIndexMap indexMap, PackageID currentPackage,
                             JvmPackageGen jvmPackageGen, JvmTypeGen jvmTypeGen, JvmCastGen jvmCastGen,
                             JvmConstantsGen jvmConstantsGen, AsyncDataCollector asyncDataCollector, Types types) {
        this.mv = mv;
        this.indexMap = indexMap;
        this.jvmPackageGen = jvmPackageGen;
        this.jvmTypeGen = jvmTypeGen;
        this.symbolTable = jvmPackageGen.symbolTable;
        this.currentPackageName = JvmCodeGenUtil.getPackageName(currentPackage);
        this.asyncDataCollector = asyncDataCollector;
        this.jvmCastGen = jvmCastGen;
        this.jvmConstantsGen = jvmConstantsGen;
        typeTestGen = new JvmTypeTestGen(this, types, mv, jvmTypeGen, jvmCastGen);
        this.functions = new HashMap<>();
        this.moduleInitClass = JvmCodeGenUtil.getModuleLevelClassName(currentPackage, MODULE_INIT_CLASS_NAME);
    }

    private void generateJVarLoad(MethodVisitor mv, JType jType, int valueIndex) {

        switch (jType.jTag) {
            case JTypeTags.JBYTE, JTypeTags.JSHORT, JTypeTags.JINT, JTypeTags.JBOOLEAN, JTypeTags.JCHAR ->
                    mv.visitVarInsn(ILOAD, valueIndex);
            case JTypeTags.JLONG -> mv.visitVarInsn(LLOAD, valueIndex);
            case JTypeTags.JFLOAT -> mv.visitVarInsn(FLOAD, valueIndex);
            case JTypeTags.JDOUBLE -> mv.visitVarInsn(DLOAD, valueIndex);
            case JTypeTags.JARRAY, JTypeTags.JREF -> mv.visitVarInsn(ALOAD, valueIndex);
            default -> throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE + jType);
        }
    }

    private void generateJVarStore(MethodVisitor mv, JType jType, int valueIndex) {

        switch (jType.jTag) {
            case JTypeTags.JBYTE, JTypeTags.JCHAR, JTypeTags.JSHORT, JTypeTags.JINT, JTypeTags.JBOOLEAN ->
                    mv.visitVarInsn(ISTORE, valueIndex);
            case JTypeTags.JLONG -> mv.visitVarInsn(LSTORE, valueIndex);
            case JTypeTags.JFLOAT -> mv.visitVarInsn(FSTORE, valueIndex);
            case JTypeTags.JDOUBLE -> mv.visitVarInsn(DSTORE, valueIndex);
            case JTypeTags.JARRAY, JTypeTags.JREF -> mv.visitVarInsn(ASTORE, valueIndex);
            default -> throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE + jType);
        }
    }

    private void generateIntToUnsignedIntConversion(MethodVisitor mv, BType targetType) {
        targetType = JvmCodeGenUtil.getImpliedType(targetType);
        switch (targetType.tag) { // Wouldn't reach here for int atm.
            case TypeTags.BYTE, TypeTags.UNSIGNED8_INT -> {
                mv.visitInsn(L2I);
                mv.visitInsn(I2B);
                mv.visitMethodInsn(INVOKESTATIC, BYTE_VALUE, TO_UNSIGNED_LONG, "(B)J", false);
            }
            case TypeTags.UNSIGNED16_INT -> {
                mv.visitInsn(L2I);
                mv.visitInsn(I2S);
                mv.visitMethodInsn(INVOKESTATIC, SHORT_VALUE, TO_UNSIGNED_LONG, "(S)J", false);
            }
            case TypeTags.UNSIGNED32_INT -> {
                mv.visitInsn(L2I);
                mv.visitMethodInsn(INVOKESTATIC, INT_VALUE, TO_UNSIGNED_LONG, "(I)J", false);
            }
        }
    }

    public void generateVarLoad(MethodVisitor mv, BIRNode.BIRVariableDcl varDcl, int valueIndex) {

        BType bType = JvmCodeGenUtil.getImpliedType(varDcl.type);

        switch (varDcl.kind) {
            case SELF -> {
                mv.visitVarInsn(ALOAD, this.indexMap.get(OBJECT_SELF_INSTANCE));
                return;
            }
            case CONSTANT, GLOBAL -> {
                String varName = varDcl.name.value;
                PackageID moduleId = ((BIRNode.BIRGlobalVariableDcl) varDcl).pkgId;
                String pkgName = JvmCodeGenUtil.getPackageName(moduleId);
                String className = jvmPackageGen.lookupGlobalVarClassName(pkgName, varName);
                String typeSig = getTypeDesc(bType);
                mv.visitFieldInsn(GETSTATIC, className, varName, typeSig);
                return;
            }
            default -> {
            }
        }

        generateVarLoadForType(mv, bType, valueIndex);
    }

    private void generateVarLoadForType (MethodVisitor mv, BType bType, int valueIndex) {
        bType = JvmCodeGenUtil.getImpliedType(bType);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            mv.visitVarInsn(LLOAD, valueIndex);
            return;
        }  else if (TypeTags.isXMLTypeTag(bType.tag) ||
                TypeTags.isStringTypeTag(bType.tag) || TypeTags.REGEXP == bType.tag) {
            mv.visitVarInsn(ALOAD, valueIndex);
            return;
        }

        switch (bType.tag) {
            case TypeTags.BYTE -> {
                mv.visitVarInsn(ILOAD, valueIndex);
                mv.visitInsn(I2B);
                mv.visitMethodInsn(INVOKESTATIC, BYTE_VALUE, JVM_TO_UNSIGNED_INT_METHOD, "(B)I", false);
            }
            case TypeTags.FLOAT -> mv.visitVarInsn(DLOAD, valueIndex);
            case TypeTags.BOOLEAN -> mv.visitVarInsn(ILOAD, valueIndex);
            case TypeTags.ARRAY, TypeTags.MAP, TypeTags.STREAM, TypeTags.TABLE, TypeTags.ANY, TypeTags.ANYDATA,
                    TypeTags.NIL, TypeTags.NEVER, TypeTags.UNION, TypeTags.TUPLE, TypeTags.RECORD, TypeTags.ERROR,
                    TypeTags.JSON, TypeTags.FUTURE, TypeTags.OBJECT, TypeTags.DECIMAL, TypeTags.INVOKABLE,
                    TypeTags.FINITE, TypeTags.HANDLE, TypeTags.TYPEDESC, TypeTags.READONLY ->
                    mv.visitVarInsn(ALOAD, valueIndex);
            case JTypeTags.JTYPE -> generateJVarLoad(mv, (JType) bType, valueIndex);
            default -> throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE + bType);
        }
    }

    public void generateVarStore(MethodVisitor mv, BIRNode.BIRVariableDcl varDcl, int valueIndex) {

        BType bType = JvmCodeGenUtil.getImpliedType(varDcl.type);
        if (varDcl.kind == VarKind.GLOBAL || varDcl.kind == VarKind.CONSTANT) {
            String varName = varDcl.name.value;
            PackageID moduleId = ((BIRNode.BIRGlobalVariableDcl) varDcl).pkgId;
            String pkgName = JvmCodeGenUtil.getPackageName(moduleId);
            String className = jvmPackageGen.lookupGlobalVarClassName(pkgName, varName);
            String typeSig = getTypeDesc(bType);
            mv.visitFieldInsn(PUTSTATIC, className, varName, typeSig);
            return;
        }

        generateVarStoreForType(mv, bType, valueIndex);
    }

    private void generateVarStoreForType (MethodVisitor mv, BType bType, int valueIndex) {
        bType = JvmCodeGenUtil.getImpliedType(bType);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            mv.visitVarInsn(LSTORE, valueIndex);
            return;
        } else if (TypeTags.isStringTypeTag(bType.tag) ||
                TypeTags.isXMLTypeTag(bType.tag) || bType.tag == TypeTags.REGEXP) {
            mv.visitVarInsn(ASTORE, valueIndex);
            return;
        }

        switch (bType.tag) {
            case TypeTags.BYTE, TypeTags.BOOLEAN -> mv.visitVarInsn(ISTORE, valueIndex);
            case TypeTags.FLOAT -> mv.visitVarInsn(DSTORE, valueIndex);
            case TypeTags.ARRAY, TypeTags.MAP, TypeTags.STREAM, TypeTags.TABLE, TypeTags.ANY, TypeTags.ANYDATA,
                    TypeTags.NIL, TypeTags.NEVER, TypeTags.UNION, TypeTags.TUPLE, TypeTags.DECIMAL, TypeTags.RECORD,
                    TypeTags.ERROR, TypeTags.JSON, TypeTags.FUTURE, TypeTags.OBJECT, TypeTags.INVOKABLE,
                    TypeTags.FINITE, TypeTags.HANDLE, TypeTags.TYPEDESC, TypeTags.READONLY ->
                    mv.visitVarInsn(ASTORE, valueIndex);
            case JTypeTags.JTYPE -> generateJVarStore(mv, (JType) bType, valueIndex);
            default -> throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE + bType);
        }
    }

    private BType getSmallestBuiltInUnsignedIntSubTypeContainingTypes(BType lhsType, BType rhsType) {

        if (TypeTags.isSignedIntegerTypeTag(lhsType.tag) || TypeTags.isSignedIntegerTypeTag(rhsType.tag)) {
            throw new BLangCompilerException("expected two unsigned int subtypes, found '" + lhsType + "' and '" +
                    rhsType + "'");
        }

        if (lhsType.tag == TypeTags.UNSIGNED32_INT || rhsType.tag == TypeTags.UNSIGNED32_INT) {
            return symbolTable.unsigned32IntType;
        }

        if (lhsType.tag == TypeTags.UNSIGNED16_INT || rhsType.tag == TypeTags.UNSIGNED16_INT) {
            return symbolTable.unsigned16IntType;
        }

        if (lhsType.tag == TypeTags.UNSIGNED8_INT || rhsType.tag == TypeTags.UNSIGNED8_INT) {
            return symbolTable.unsigned8IntType;
        }

        return symbolTable.byteType;
    }

    void generatePlatformIns(JInstruction ins, int localVarOffset) {
        switch (ins.jKind) {
            case J_CAST -> generateJCastIns((JCast) ins);
            case CALL -> generateJMethodCallIns(localVarOffset, (JMethodCallInstruction) ins);
            case LARGE_ARRAY -> generateJLargeArrayIns(localVarOffset, (JLargeArrayInstruction) ins);
            default -> generateJLargeMapIns(localVarOffset, (JLargeMapInstruction) ins);
        }
    }

    private void generateJLargeMapIns(int localVarOffset, JLargeMapInstruction mapNewIns) {
        this.loadVar(mapNewIns.rhsOp.variableDcl);
        this.mv.visitVarInsn(ALOAD, localVarOffset);

        // load the initial values operand
        this.loadVar(mapNewIns.initialValues.variableDcl);
        mv.visitMethodInsn(INVOKEVIRTUAL, HANDLE_VALUE, GET_VALUE_METHOD, RETURN_OBJECT, false);
        mv.visitTypeInsn(CHECKCAST, "[L" + B_MAPPING_INITIAL_VALUE_ENTRY + ";");

        this.mv.visitMethodInsn(INVOKEINTERFACE, TYPEDESC_VALUE, INSTANTIATE_FUNCTION,
                INSTANTIATE_WITH_INITIAL_VALUES, true);
        this.storeToVar(mapNewIns.lhsOp.variableDcl);
    }

    private void generateJLargeArrayIns(int localVarOffset, JLargeArrayInstruction inst) {
        BType instType = JvmCodeGenUtil.getImpliedType(inst.type);
        if (instType.tag == TypeTags.ARRAY) {
            this.mv.visitTypeInsn(NEW, ARRAY_VALUE_IMPL);
            this.mv.visitInsn(DUP);
            jvmTypeGen.loadType(this.mv, inst.type);
            loadListInitialValues(inst);
            BType elementType = JvmCodeGenUtil.getImpliedType(((BArrayType) instType).eType);
            if (elementType.tag == TypeTags.RECORD) {
                visitNewRecordArray(inst.elementTypedescOp.variableDcl);
            } else {
                this.mv.visitMethodInsn(INVOKESPECIAL, ARRAY_VALUE_IMPL, JVM_INIT_METHOD,
                        INIT_ARRAY, false);
            }
            this.storeToVar(inst.lhsOp.variableDcl);
        } else {
            this.loadVar(inst.typedescOp.variableDcl);
            this.mv.visitVarInsn(ALOAD, localVarOffset);
            loadListInitialValues(inst);
            this.mv.visitMethodInsn(INVOKEINTERFACE, TYPEDESC_VALUE, INSTANTIATE_FUNCTION,
                    INSTANTIATE_WITH_INITIAL_VALUES, true);
            this.storeToVar(inst.lhsOp.variableDcl);
        }
    }

    private void generateJMethodCallIns(int localVarOffset, JMethodCallInstruction callIns) {
        boolean isInterface = callIns.invocationType == INVOKEINTERFACE;
        int argIndex = 0;
        String jMethodVMSig = callIns.jMethodVMSig;
        boolean hasBalEnvParam = jMethodVMSig.startsWith(BAL_ENV_PARAM);
        if (hasBalEnvParam) {
            mv.visitTypeInsn(NEW, BAL_ENV_CLASS);
            mv.visitInsn(DUP);
            // load the strand
            this.mv.visitVarInsn(ALOAD, localVarOffset);
            // load the current Module
            mv.visitFieldInsn(GETSTATIC, this.moduleInitClass, CURRENT_MODULE_VAR_NAME, GET_MODULE);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL, BAL_ENV_CLASS, JVM_INIT_METHOD, INIT_BAL_ENV, false);
        }
        while (argIndex < callIns.args.size()) {
            BIROperand arg = callIns.args.get(argIndex);
            this.loadVar(arg.variableDcl);
            argIndex += 1;
        }
        this.mv.visitMethodInsn(callIns.invocationType, callIns.jClassName, callIns.name, jMethodVMSig,
                isInterface);
        if (callIns.lhsOp != null) {
            this.storeToVar(callIns.lhsOp.variableDcl);
        }
    }

    private void generateJCastIns(JCast castIns) {
        BType targetType = castIns.targetType;
        this.loadVar(castIns.rhsOp.variableDcl);
        jvmCastGen.generatePlatformCheckCast(this.mv, this.indexMap, castIns.rhsOp.variableDcl.type,
                targetType);
        this.storeToVar(castIns.lhsOp.variableDcl);
    }

    void generateMoveIns(BIRNonTerminator.Move moveIns) {
        this.loadVar(moveIns.rhsOp.variableDcl);
        this.storeToVar(moveIns.lhsOp.variableDcl);
    }

    void generateBinaryOpIns(BIRNonTerminator.BinaryOp binaryIns) {

        InstructionKind insKind = binaryIns.kind;
        switch (insKind) {
            case ADD -> this.generateAddIns(binaryIns);
            case SUB -> this.generateSubIns(binaryIns);
            case MUL -> this.generateMulIns(binaryIns);
            case DIV -> this.generateDivIns(binaryIns);
            case MOD -> this.generateRemIns(binaryIns);
            case EQUAL -> this.generateEqualIns(binaryIns);
            case NOT_EQUAL -> this.generateNotEqualIns(binaryIns);
            case GREATER_THAN -> this.generateGreaterThanIns(binaryIns);
            case GREATER_EQUAL -> this.generateGreaterEqualIns(binaryIns);
            case LESS_THAN -> this.generateLessThanIns(binaryIns);
            case LESS_EQUAL -> this.generateLessEqualIns(binaryIns);
            case REF_EQUAL -> this.generateRefEqualIns(binaryIns);
            case REF_NOT_EQUAL -> this.generateRefNotEqualIns(binaryIns);
            case CLOSED_RANGE, HALF_OPEN_RANGE -> this.generateClosedRangeIns(binaryIns);
            case ANNOT_ACCESS -> this.generateAnnotAccessIns(binaryIns);
            case BITWISE_AND -> this.generateBitwiseAndIns(binaryIns);
            case BITWISE_OR -> this.generateBitwiseOrIns(binaryIns);
            case BITWISE_XOR -> this.generateBitwiseXorIns(binaryIns);
            case BITWISE_LEFT_SHIFT -> this.generateBitwiseLeftShiftIns(binaryIns);
            case BITWISE_RIGHT_SHIFT -> this.generateBitwiseRightShiftIns(binaryIns);
            case BITWISE_UNSIGNED_RIGHT_SHIFT -> this.generateBitwiseUnsignedRightShiftIns(binaryIns);
            default -> throw new BLangCompilerException("JVM generation is not supported " +
                            "for instruction kind : " + insKind);
        }
    }

    private void generateBinaryRhsAndLhsLoad(BIRNonTerminator.BinaryOp binaryIns) {

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        this.loadVar(binaryIns.rhsOp2.variableDcl);
    }

    private void generateLessThanIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.generateBinaryCompareIns(binaryIns, IFLT);
    }

    private void generateGreaterThanIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.generateBinaryCompareIns(binaryIns, IFGT);
    }

    private void generateLessEqualIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.generateBinaryCompareIns(binaryIns, IFLE);

    }

    private void generateGreaterEqualIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.generateBinaryCompareIns(binaryIns, IFGE);
    }

    private void generateBinaryCompareIns(BIRNonTerminator.BinaryOp binaryIns, int opcode) {

        if (opcode != IFLT && opcode != IFGT && opcode != IFLE && opcode != IFGE) {
            throw new BLangCompilerException("Unsupported opcode '" + opcode + "' for binary operator.");
        }

        this.generateBinaryRhsAndLhsLoad(binaryIns);
        Label label1 = new Label();
        Label label2 = new Label();

        BType lhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType rhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(lhsOpType.tag) && TypeTags.isIntegerTypeTag(rhsOpType.tag)) {
            this.mv.visitInsn(LCMP);
            this.mv.visitJumpInsn(opcode, label1);
        } else if (lhsOpType.tag == TypeTags.BYTE && rhsOpType.tag == TypeTags.BYTE) {
            if (opcode == IFLT) {
                this.mv.visitJumpInsn(IF_ICMPLT, label1);
            } else if (opcode == IFGT) {
                this.mv.visitJumpInsn(IF_ICMPGT, label1);
            } else if (opcode == IFLE) {
                this.mv.visitJumpInsn(IF_ICMPLE, label1);
            } else {
                this.mv.visitJumpInsn(IF_ICMPGE, label1);
            }
        } else if (lhsOpType.tag == TypeTags.BOOLEAN && rhsOpType.tag == TypeTags.BOOLEAN) {
            if (opcode == IFLT) {
                this.mv.visitJumpInsn(IF_ICMPLT, label1);
            } else if (opcode == IFGT) {
                this.mv.visitJumpInsn(IF_ICMPGT, label1);
            } else if (opcode == IFLE) {
                this.mv.visitJumpInsn(IF_ICMPLE, label1);
            } else {
                this.mv.visitJumpInsn(IF_ICMPGE, label1);
            }
        } else if (lhsOpType.tag == TypeTags.FLOAT && rhsOpType.tag == TypeTags.FLOAT) {
            String compareFuncName = this.getCompareFuncName(opcode);
            this.mv.visitMethodInsn(INVOKESTATIC, VALUE_COMPARISON_UTILS, compareFuncName, "(DD)Z", false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        } else {
            String compareFuncName = this.getCompareFuncName(opcode);
            this.mv.visitMethodInsn(INVOKESTATIC, VALUE_COMPARISON_UTILS, compareFuncName,
                    COMPARE_OBJECTS, false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        }

        this.mv.visitInsn(ICONST_0);
        this.mv.visitJumpInsn(GOTO, label2);

        this.mv.visitLabel(label1);
        this.mv.visitInsn(ICONST_1);

        this.mv.visitLabel(label2);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private String getCompareFuncName(int opcode) {
        return switch (opcode) {
            case IFGT -> "compareValueGreaterThan";
            case IFGE -> "compareValueGreaterThanOrEqual";
            case IFLT -> "compareValueLessThan";
            case IFLE -> "compareValueLessThanOrEqual";
            default -> throw new BLangCompilerException("Opcode: '" + opcode + "' is not a comparison opcode.");
        };
    }

    private void generateEqualIns(BIRNonTerminator.BinaryOp binaryIns) {

        Label label1 = new Label();
        Label label2 = new Label();
        Label label3 = new Label();

        BType lhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType rhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(lhsOpType.tag) && TypeTags.isIntegerTypeTag(rhsOpType.tag)) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitInsn(LCMP);
            this.mv.visitJumpInsn(IFNE, label1);
        } else if (lhsOpType.tag == TypeTags.BYTE && rhsOpType.tag == TypeTags.BYTE) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPNE, label1);
        } else if (lhsOpType.tag == TypeTags.FLOAT && rhsOpType.tag == TypeTags.FLOAT) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, "isNaN", "(D)Z", false);
            this.mv.visitJumpInsn(IFEQ, label3);
            this.loadVar(binaryIns.rhsOp2.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, "isNaN", "(D)Z", false);
            this.mv.visitJumpInsn(IFEQ, label3);
            this.mv.visitInsn(ICONST_1);
            this.mv.visitJumpInsn(GOTO, label2);
            this.mv.visitLabel(label3);
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitInsn(DCMPL);
            this.mv.visitJumpInsn(IFNE, label1);
        } else if (lhsOpType.tag == TypeTags.BOOLEAN && rhsOpType.tag == TypeTags.BOOLEAN) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPNE, label1);
        } else if (lhsOpType.tag == TypeTags.DECIMAL && rhsOpType.tag == TypeTags.DECIMAL) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "checkDecimalEqual",
                    COMPARE_DECIMALS, false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        } else {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "isEqual",
                   COMPARE_OBJECTS, false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        }

        this.mv.visitInsn(ICONST_1);
        this.mv.visitJumpInsn(GOTO, label2);

        this.mv.visitLabel(label1);
        this.mv.visitInsn(ICONST_0);

        this.mv.visitLabel(label2);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateNotEqualIns(BIRNonTerminator.BinaryOp binaryIns) {

        Label label1 = new Label();
        Label label2 = new Label();
        Label label3 = new Label();

        // It is assumed that both operands are of same type
        BType lhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType rhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(lhsOpType.tag) && TypeTags.isIntegerTypeTag(rhsOpType.tag)) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitInsn(LCMP);
            this.mv.visitJumpInsn(IFEQ, label1);
        } else if (lhsOpType.tag == TypeTags.BYTE && rhsOpType.tag == TypeTags.BYTE) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPEQ, label1);
        } else if (lhsOpType.tag == TypeTags.FLOAT && rhsOpType.tag == TypeTags.FLOAT) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, "isNaN", "(D)Z", false);
            this.mv.visitJumpInsn(IFEQ, label3);
            this.loadVar(binaryIns.rhsOp2.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, "isNaN", "(D)Z", false);
            this.mv.visitJumpInsn(IFEQ, label3);
            this.mv.visitInsn(ICONST_0);
            this.mv.visitJumpInsn(GOTO, label2);
            this.mv.visitLabel(label3);
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitInsn(DCMPL);
            this.mv.visitJumpInsn(IFEQ, label1);
        } else if (lhsOpType.tag == TypeTags.BOOLEAN && rhsOpType.tag == TypeTags.BOOLEAN) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPEQ, label1);
        } else if (lhsOpType.tag == TypeTags.DECIMAL && rhsOpType.tag == TypeTags.DECIMAL) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "checkDecimalEqual",
                    COMPARE_DECIMALS, false);
            this.mv.visitJumpInsn(IFNE, label1);
        } else {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "isEqual",
                   COMPARE_OBJECTS, false);
            this.mv.visitJumpInsn(IFNE, label1);
        }

        this.mv.visitInsn(ICONST_1);
        this.mv.visitJumpInsn(GOTO, label2);

        this.mv.visitLabel(label1);
        this.mv.visitInsn(ICONST_0);

        this.mv.visitLabel(label2);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateRefEqualIns(BIRNonTerminator.BinaryOp binaryIns) {

        Label label1 = new Label();
        Label label2 = new Label();

        BType lhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType rhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(lhsOpType.tag) && TypeTags.isIntegerTypeTag(rhsOpType.tag)) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitInsn(LCMP);
            this.mv.visitJumpInsn(IFNE, label1);
        } else if (lhsOpType.tag == TypeTags.BYTE && rhsOpType.tag == TypeTags.BYTE) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPNE, label1);
        } else if (lhsOpType.tag == TypeTags.FLOAT && rhsOpType.tag == TypeTags.FLOAT) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, VALUE_OF_METHOD,
                    DOUBLE_VALUE_OF_METHOD, false);
            this.loadVar(binaryIns.rhsOp2.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, VALUE_OF_METHOD,
                    DOUBLE_VALUE_OF_METHOD, false);
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DOUBLE_VALUE, EQUALS_METHOD,
                    ANY_TO_JBOOLEAN, false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        } else if (lhsOpType.tag == TypeTags.BOOLEAN && rhsOpType.tag == TypeTags.BOOLEAN) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPNE, label1);
        } else if (lhsOpType.tag == TypeTags.DECIMAL && rhsOpType.tag == TypeTags.DECIMAL) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "checkDecimalExactEqual",
                    COMPARE_DECIMALS, false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        } else {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "isReferenceEqual",
                   COMPARE_OBJECTS, false);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        }

        this.mv.visitInsn(ICONST_1);
        this.mv.visitJumpInsn(GOTO, label2);

        this.mv.visitLabel(label1);
        this.mv.visitInsn(ICONST_0);

        this.mv.visitLabel(label2);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateRefNotEqualIns(BIRNonTerminator.BinaryOp binaryIns) {

        Label label1 = new Label();
        Label label2 = new Label();

        // It is assumed that both operands are of same type
        BType lhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType rhsOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(lhsOpType.tag) && TypeTags.isIntegerTypeTag(rhsOpType.tag)) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitInsn(LCMP);
            this.mv.visitJumpInsn(IFEQ, label1);
        } else if (lhsOpType.tag == TypeTags.BYTE && rhsOpType.tag == TypeTags.BYTE) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPEQ, label1);
        } else if (lhsOpType.tag == TypeTags.FLOAT && rhsOpType.tag == TypeTags.FLOAT) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, VALUE_OF_METHOD,
                    DOUBLE_VALUE_OF_METHOD, false);
            this.loadVar(binaryIns.rhsOp2.variableDcl);
            this.mv.visitMethodInsn(INVOKESTATIC, DOUBLE_VALUE, VALUE_OF_METHOD,
                    DOUBLE_VALUE_OF_METHOD, false);
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DOUBLE_VALUE, EQUALS_METHOD,
                    ANY_TO_JBOOLEAN, false);
            this.mv.visitJumpInsn(IFNE, label1);
        } else if (lhsOpType.tag == TypeTags.BOOLEAN && rhsOpType.tag == TypeTags.BOOLEAN) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitJumpInsn(IF_ICMPEQ, label1);
        } else if (lhsOpType.tag == TypeTags.DECIMAL && rhsOpType.tag == TypeTags.DECIMAL) {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "checkDecimalExactEqual",
                    COMPARE_DECIMALS, false);
            this.mv.visitJumpInsn(IFNE, label1);
        } else {
            this.generateBinaryRhsAndLhsLoad(binaryIns);
            this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "isReferenceEqual",
                   COMPARE_OBJECTS, false);
            this.mv.visitJumpInsn(IFNE, label1);
        }

        this.mv.visitInsn(ICONST_1);
        this.mv.visitJumpInsn(GOTO, label2);

        this.mv.visitLabel(label1);
        this.mv.visitInsn(ICONST_0);

        this.mv.visitLabel(label2);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateClosedRangeIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.mv.visitTypeInsn(NEW, ARRAY_VALUE_IMPL);
        this.mv.visitInsn(DUP);
        this.generateBinaryRhsAndLhsLoad(binaryIns);
        this.mv.visitMethodInsn(INVOKESTATIC, LONG_STREAM, "rangeClosed", LONG_STREAM_RANGE_CLOSED, true);
        this.mv.visitMethodInsn(INVOKEINTERFACE, LONG_STREAM, "toArray", "()[J", true);
        this.mv.visitMethodInsn(INVOKESPECIAL, ARRAY_VALUE_IMPL, JVM_INIT_METHOD, "([J)V", false);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateAnnotAccessIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        this.loadVar(binaryIns.rhsOp2.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "getAnnotValue", GET_ANNOTATION_VALUE, false);

        BType targetType = JvmCodeGenUtil.getImpliedType(binaryIns.lhsOp.variableDcl.type);
        jvmCastGen.addUnboxInsn(this.mv, targetType);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateAddIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType bType = JvmCodeGenUtil.getImpliedType(binaryIns.lhsOp.variableDcl.type);

        this.generateBinaryRhsAndLhsLoad(binaryIns);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKESTATIC, MATH_UTILS, "addExact", "(JJ)J", false);
        } else if (bType.tag == TypeTags.BYTE) {
            this.mv.visitInsn(IADD);
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
                this.mv.visitMethodInsn(INVOKEINTERFACE, B_STRING_VALUE, "concat",
                                        BSTRING_CONCAT, true);
        } else if (bType.tag == TypeTags.DECIMAL) {
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DECIMAL_VALUE, ADD_METHOD,
                    SET_DECIMAL_RETURN_DECIMAL, false);
        } else if (bType.tag == TypeTags.FLOAT) {
            this.mv.visitInsn(DADD);
        } else if (TypeTags.isXMLTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKESTATIC, XML_FACTORY, "concatenate",
                    XML_CONCAT, false);
        } else {
            throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE +
                    binaryIns.lhsOp.variableDcl.type);
        }

        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateSubIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType bType = JvmCodeGenUtil.getImpliedType(binaryIns.lhsOp.variableDcl.type);

        this.generateBinaryRhsAndLhsLoad(binaryIns);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKESTATIC, MATH_UTILS, "subtractExact", "(JJ)J", false);
        } else if (bType.tag == TypeTags.FLOAT) {
            this.mv.visitInsn(DSUB);
        } else if (bType.tag == TypeTags.DECIMAL) {
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DECIMAL_VALUE, "subtract",
                    SET_DECIMAL_RETURN_DECIMAL, false);
        } else {
            throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE +
                    binaryIns.lhsOp.variableDcl.type);
        }
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateDivIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType bType = JvmCodeGenUtil.getImpliedType(binaryIns.lhsOp.variableDcl.type);

        this.generateBinaryRhsAndLhsLoad(binaryIns);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKESTATIC, MATH_UTILS, "divide", "(JJ)J", false);
        } else if (bType.tag == TypeTags.FLOAT) {
            this.mv.visitInsn(DDIV);
        } else if (bType.tag == TypeTags.DECIMAL) {
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DECIMAL_VALUE, "divide",
                    SET_DECIMAL_RETURN_DECIMAL, false);
        } else {
            throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE +
                    binaryIns.lhsOp.variableDcl.type);
        }
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateMulIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType bType = JvmCodeGenUtil.getImpliedType(binaryIns.lhsOp.variableDcl.type);

        this.generateBinaryRhsAndLhsLoad(binaryIns);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKESTATIC, MATH_UTILS, "multiplyExact", "(JJ)J", false);
        } else if (bType.tag == TypeTags.FLOAT) {
            this.mv.visitInsn(DMUL);
        } else if (bType.tag == TypeTags.DECIMAL) {
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DECIMAL_VALUE, "multiply",
                    SET_DECIMAL_RETURN_DECIMAL, false);
        } else {
            throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE +
                    binaryIns.lhsOp.variableDcl.type);
        }
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateRemIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType bType = JvmCodeGenUtil.getImpliedType(binaryIns.lhsOp.variableDcl.type);

        this.generateBinaryRhsAndLhsLoad(binaryIns);
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKESTATIC, MATH_UTILS, "remainder", "(JJ)J", false);
        } else if (bType.tag == TypeTags.FLOAT) {
            this.mv.visitInsn(DREM);
        } else if (bType.tag == TypeTags.DECIMAL) {
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DECIMAL_VALUE, "remainder",
                    SET_DECIMAL_RETURN_DECIMAL, false);
        } else {
            throw new BLangCompilerException(JvmConstants.TYPE_NOT_SUPPORTED_MESSAGE +
                    binaryIns.lhsOp.variableDcl.type);
        }
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateBitwiseAndIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType opType1 = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType opType2 = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        int opType1Tag = opType1.tag;
        int opType2Tag = opType2.tag;

        if (opType1Tag == TypeTags.BYTE && opType2Tag == TypeTags.BYTE) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            jvmCastGen.generateCheckCastToByte(this.mv, opType1);

            this.loadVar(binaryIns.rhsOp2.variableDcl);
            jvmCastGen.generateCheckCastToByte(this.mv, opType2);

            this.mv.visitInsn(IAND);
        } else {
            boolean byteResult = false;

            this.loadVar(binaryIns.rhsOp1.variableDcl);
            if (opType1Tag == TypeTags.BYTE) {
                this.mv.visitMethodInsn(INVOKESTATIC, INT_VALUE, TO_UNSIGNED_LONG, "(I)J", false);
                byteResult = true;
            } else {
                jvmCastGen.generateCheckCast(this.mv, opType1, symbolTable.intType, this.indexMap);
            }

            this.loadVar(binaryIns.rhsOp2.variableDcl);
            if (opType2Tag == TypeTags.BYTE) {
                this.mv.visitMethodInsn(INVOKESTATIC, INT_VALUE, TO_UNSIGNED_LONG, "(I)J", false);
                byteResult = true;
            } else {
                jvmCastGen.generateCheckCast(this.mv, opType2, symbolTable.intType, this.indexMap);
            }

            this.mv.visitInsn(LAND);
            if (byteResult) {
                jvmCastGen.generateCheckCastToByte(this.mv, symbolTable.intType);
            }
        }

        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateBitwiseOrIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType opType1 = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType opType2 = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (opType1.tag == TypeTags.BYTE && opType2.tag == TypeTags.BYTE) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            this.loadVar(binaryIns.rhsOp2.variableDcl);
            this.mv.visitInsn(IOR);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        }

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        jvmCastGen.generateCheckCast(this.mv, opType1, symbolTable.intType, this.indexMap);

        this.loadVar(binaryIns.rhsOp2.variableDcl);
        jvmCastGen.generateCheckCast(this.mv, opType2, symbolTable.intType, this.indexMap);

        this.mv.visitInsn(LOR);

        if (!TypeTags.isSignedIntegerTypeTag(opType1.tag) && !TypeTags.isSignedIntegerTypeTag(opType2.tag) &&
                opType1.tag != TypeTags.UNION && opType2.tag != TypeTags.UNION) {
            generateIntToUnsignedIntConversion(this.mv,
                    getSmallestBuiltInUnsignedIntSubTypeContainingTypes(opType1, opType2));
        }

        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateBitwiseXorIns(BIRNonTerminator.BinaryOp binaryIns) {

        BType opType1 = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        BType opType2 = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (opType1.tag == TypeTags.BYTE && opType2.tag == TypeTags.BYTE) {
            this.loadVar(binaryIns.rhsOp1.variableDcl);
            this.loadVar(binaryIns.rhsOp2.variableDcl);
            this.mv.visitInsn(IXOR);
            this.storeToVar(binaryIns.lhsOp.variableDcl);
            return;
        }

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        jvmCastGen.generateCheckCast(this.mv, opType1, symbolTable.intType, this.indexMap);

        this.loadVar(binaryIns.rhsOp2.variableDcl);
        jvmCastGen.generateCheckCast(this.mv, opType2, symbolTable.intType, this.indexMap);

        this.mv.visitInsn(LXOR);

        if (!TypeTags.isSignedIntegerTypeTag(opType1.tag) && !TypeTags.isSignedIntegerTypeTag(opType2.tag) &&
            opType1.tag != TypeTags.UNION && opType2.tag != TypeTags.UNION) {
            generateIntToUnsignedIntConversion(this.mv,
                    getSmallestBuiltInUnsignedIntSubTypeContainingTypes(opType1, opType2));
        }

        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateBitwiseLeftShiftIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        BType firstOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);
        if (firstOpType.tag == TypeTags.BYTE) {
            this.mv.visitInsn(I2L);
        }

        this.loadVar(binaryIns.rhsOp2.variableDcl);
        BType secondOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);
        if (TypeTags.isIntegerTypeTag(secondOpType.tag)) {
            this.mv.visitInsn(L2I);
        }

        this.mv.visitInsn(LSHL);
        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateBitwiseRightShiftIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        this.loadVar(binaryIns.rhsOp2.variableDcl);

        BType secondOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(secondOpType.tag)) {
            this.mv.visitInsn(L2I);
        }

        BType firstOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(firstOpType.tag)) {
            this.mv.visitInsn(LSHR);
        } else {
            this.mv.visitInsn(ISHR);
        }

        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private void generateBitwiseUnsignedRightShiftIns(BIRNonTerminator.BinaryOp binaryIns) {

        this.loadVar(binaryIns.rhsOp1.variableDcl);
        this.loadVar(binaryIns.rhsOp2.variableDcl);

        BType secondOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp2.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(secondOpType.tag)) {
            this.mv.visitInsn(L2I);
        }

        BType firstOpType = JvmCodeGenUtil.getImpliedType(binaryIns.rhsOp1.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(firstOpType.tag)) {
            this.mv.visitInsn(LUSHR);
        } else {
            this.mv.visitInsn(IUSHR);
        }

        this.storeToVar(binaryIns.lhsOp.variableDcl);
    }

    private int getJVMIndexOfVarRef(BIRNode.BIRVariableDcl varDcl) {
        return this.indexMap.addIfNotExists(varDcl.name.value, varDcl.type);
    }

    void generateMapNewIns(BIRNonTerminator.NewStructure mapNewIns, int localVarOffset) {

        this.loadVar(mapNewIns.rhsOp.variableDcl);
        this.mv.visitVarInsn(ALOAD, localVarOffset);

        List<BIRNode.BIRMappingConstructorEntry> initialValues = mapNewIns.initialValues;
        mv.visitLdcInsn((long) initialValues.size());
        mv.visitInsn(L2I);
        mv.visitTypeInsn(ANEWARRAY, B_MAPPING_INITIAL_VALUE_ENTRY);

        int i = 0;
        for (BIRNode.BIRMappingConstructorEntry initialValue : initialValues) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn((long) i);
            mv.visitInsn(L2I);
            i += 1;

            if (initialValue.isKeyValuePair()) {
                createKeyValueEntry(mv, (BIRNode.BIRMappingConstructorKeyValueEntry) initialValue);
            } else {
                createSpreadFieldEntry(mv, (BIRNode.BIRMappingConstructorSpreadFieldEntry) initialValue);
            }

            mv.visitInsn(AASTORE);
        }

        this.mv.visitMethodInsn(INVOKEINTERFACE, TYPEDESC_VALUE, INSTANTIATE_FUNCTION,
                INSTANTIATE_WITH_INITIAL_VALUES, true);
        this.storeToVar(mapNewIns.lhsOp.variableDcl);
    }

    private void createKeyValueEntry(MethodVisitor mv, BIRNode.BIRMappingConstructorKeyValueEntry keyValueEntry) {

        mv.visitTypeInsn(NEW, MAPPING_INITIAL_KEY_VALUE_ENTRY);
        mv.visitInsn(DUP);

        BIRNode.BIRVariableDcl keyOpVarDecl = keyValueEntry.keyOp.variableDcl;
        this.loadVar(keyOpVarDecl);
        jvmCastGen.addBoxInsn(this.mv, keyOpVarDecl.type);

        BIRNode.BIRVariableDcl valueOpVarDecl = keyValueEntry.valueOp.variableDcl;
        this.loadVar(valueOpVarDecl);
        jvmCastGen.addBoxInsn(this.mv, valueOpVarDecl.type);

        mv.visitMethodInsn(INVOKESPECIAL, MAPPING_INITIAL_KEY_VALUE_ENTRY, JVM_INIT_METHOD,
                           TWO_OBJECTS_ARGS, false);
    }

    private void createSpreadFieldEntry(MethodVisitor mv,
                                        BIRNode.BIRMappingConstructorSpreadFieldEntry spreadFieldEntry) {

        mv.visitTypeInsn(NEW, MAPPING_INITIAL_SPREAD_FIELD_ENTRY);
        mv.visitInsn(DUP);

        BIRNode.BIRVariableDcl variableDcl = spreadFieldEntry.exprOp.variableDcl;
        this.loadVar(variableDcl);

        mv.visitMethodInsn(INVOKESPECIAL, MAPPING_INITIAL_SPREAD_FIELD_ENTRY, JVM_INIT_METHOD,
                           INIT_MAPPING_INITIAL_SPREAD_FIELD_ENTRY, false);
    }

    void generateMapStoreIns(BIRNonTerminator.FieldAccess mapStoreIns) {
        // visit map_ref
        this.loadVar(mapStoreIns.lhsOp.variableDcl);
        BType varRefType = JvmCodeGenUtil.getImpliedType(mapStoreIns.lhsOp.variableDcl.type);

        // visit key_expr
        this.loadVar(mapStoreIns.keyOp.variableDcl);

        // visit value_expr
        BType valueType = mapStoreIns.rhsOp.variableDcl.type;
        this.loadVar(mapStoreIns.rhsOp.variableDcl);
        jvmCastGen.addBoxInsn(this.mv, valueType);

        if (varRefType.tag == TypeTags.JSON) {
            this.mv.visitMethodInsn(INVOKESTATIC, JSON_UTILS, "setElement",
                                    JSON_SET_ELEMENT, false);
        } else if (mapStoreIns.onInitialization) {
            // We only reach here for stores in a record init function.
            this.mv.visitMethodInsn(INVOKEINTERFACE, MAP_VALUE, "populateInitialValue",
                                    TWO_OBJECTS_ARGS, true);
        } else {
            this.mv.visitMethodInsn(INVOKESTATIC, MAP_UTILS, "handleMapStore", HANDLE_MAP_STORE, false);
        }
    }

    void generateMapLoadIns(BIRNonTerminator.FieldAccess mapLoadIns) {
        // visit map_ref
        this.loadVar(mapLoadIns.rhsOp.variableDcl);
        BType varRefType = JvmCodeGenUtil.getImpliedType(mapLoadIns.rhsOp.variableDcl.type);
        jvmCastGen.addUnboxInsn(this.mv, varRefType);

        // visit key_expr
        this.loadVar(mapLoadIns.keyOp.variableDcl);
        BType targetType = mapLoadIns.lhsOp.variableDcl.type;
        this.mv.visitTypeInsn(CHECKCAST, B_STRING_VALUE);
        boolean shouldUnbox = true;
        if (varRefType.tag == TypeTags.JSON) {
            if (mapLoadIns.optionalFieldAccess) {
                this.mv.visitMethodInsn(INVOKESTATIC, JSON_UTILS, GET_ELEMENT_OR_NIL, JSON_GET_ELEMENT, false);
            } else {
                this.mv.visitMethodInsn(INVOKESTATIC, JSON_UTILS, GET_ELEMENT, JSON_GET_ELEMENT, false);
            }
        } else {
            if (mapLoadIns.fillingRead) {
                this.mv.visitMethodInsn(INVOKEINTERFACE, MAP_VALUE, FILL_AND_GET,
                        PASS_OBJECT_RETURN_OBJECT, true);
            } else {
                shouldUnbox = generateMapGet(varRefType, targetType);
            }
        }

        // store in the target reg
        if (shouldUnbox) {
            jvmCastGen.addUnboxInsn(this.mv, targetType);
        }
        this.storeToVar(mapLoadIns.lhsOp.variableDcl);
    }

    boolean generateMapGet(BType mapType, BType expectedType) {
        if (mapType.getKind() != TypeKind.RECORD) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, MAP_VALUE, GET_BOXED_VALUE, PASS_OBJECT_RETURN_OBJECT, true);
            return true;
        }
        return switch (expectedType.getKind()) {
            case INT -> {
                this.mv.visitMethodInsn(INVOKEVIRTUAL, MAP_VALUE_IMPL, GET_UNBOXED_INT_VALUE,
                        PASS_B_STRING_RETURN_UNBOXED_LONG, false);
                yield false;
            }
            case FLOAT -> {
                this.mv.visitMethodInsn(INVOKEVIRTUAL, MAP_VALUE_IMPL, GET_UNBOXED_FLOAT_VALUE,
                        PASS_B_STRING_RETURN_UNBOXED_DOUBLE, false);
                yield false;
            }
            case STRING -> {
                this.mv.visitMethodInsn(INVOKEVIRTUAL, MAP_VALUE_IMPL, GET_STRING_VALUE, PASS_B_STRING_RETURN_B_STRING,
                        false);
                yield true;
            }
            case BOOLEAN -> {
                this.mv.visitMethodInsn(INVOKEVIRTUAL, MAP_VALUE_IMPL, GET_UNBOXED_BOOLEAN_VALUE,
                        PASS_B_STRING_RETURN_UNBOXED_BOOLEAN, false);
                yield false;
            }
            default -> {
                this.mv.visitMethodInsn(INVOKEINTERFACE, MAP_VALUE, GET_BOXED_VALUE, PASS_OBJECT_RETURN_OBJECT, true);
                yield true;
            }
        };
    }

    void generateObjectLoadIns(BIRNonTerminator.FieldAccess objectLoadIns) {
        // visit object_ref
        this.loadVar(objectLoadIns.rhsOp.variableDcl);

        // visit key_expr
        this.loadVar(objectLoadIns.keyOp.variableDcl);

        // invoke get() method, and unbox if needed
        this.mv.visitMethodInsn(INVOKEINTERFACE, B_OBJECT, GET_BOXED_VALUE, BOBJECT_GET, true);
        BType targetType = objectLoadIns.lhsOp.variableDcl.type;
        jvmCastGen.addUnboxInsn(this.mv, targetType);

        // store in the target reg
        this.storeToVar(objectLoadIns.lhsOp.variableDcl);
    }

    void generateObjectStoreIns(BIRNonTerminator.FieldAccess objectStoreIns) {
        // visit object_ref
        this.loadVar(objectStoreIns.lhsOp.variableDcl);
        if (objectStoreIns.onInitialization) {
            BObjectType objectType = (BObjectType) objectStoreIns.lhsOp.variableDcl.type;
            String className = getTypeValueClassName(JvmCodeGenUtil.getPackageName(objectType.tsymbol.pkgID),
                    toNameString(objectType));
            // add cast to typeValueClass
            this.mv.visitTypeInsn(CHECKCAST, className);
            visitKeyValueExpressions(objectStoreIns);
            // invoke setOnInitialization() method
            this.mv.visitMethodInsn(INVOKEVIRTUAL, className, "setOnInitialization",
                    SET_ON_INIT, false);
            return;
        }
        visitKeyValueExpressions(objectStoreIns);
        // invoke set() method
        this.mv.visitMethodInsn(INVOKEINTERFACE, B_OBJECT, "set", SET_ON_INIT, true);
    }

    private void visitKeyValueExpressions(BIRNonTerminator.FieldAccess objectStoreIns) {
        // visit key_expr
        this.loadVar(objectStoreIns.keyOp.variableDcl);
        // visit value_expr
        BType valueType = objectStoreIns.rhsOp.variableDcl.type;
        this.loadVar(objectStoreIns.rhsOp.variableDcl);
        jvmCastGen.addBoxInsn(this.mv, valueType);
    }

    void generateStringLoadIns(BIRNonTerminator.FieldAccess stringLoadIns) {
        // visit the string
        this.loadVar(stringLoadIns.rhsOp.variableDcl);

        // visit the key expr
        this.loadVar(stringLoadIns.keyOp.variableDcl);

        // invoke the `getStringAt()` method
        this.mv.visitMethodInsn(INVOKESTATIC, STRING_UTILS, "getStringAt",
                                GET_STRING_AT,
                                false);

        // store in the target reg
        this.storeToVar(stringLoadIns.lhsOp.variableDcl);
    }

    void generateArrayNewIns(BIRNonTerminator.NewArray inst, int localVarOffset) {
        BType instType = JvmCodeGenUtil.getImpliedType(inst.type);
        if (instType.tag == TypeTags.ARRAY) {
            this.mv.visitTypeInsn(NEW, ARRAY_VALUE_IMPL);
            this.mv.visitInsn(DUP);
            jvmTypeGen.loadType(this.mv, inst.type);
            loadListInitialValues(inst);
            BType elementType = JvmCodeGenUtil.getImpliedType(((BArrayType) instType).eType);

            if (elementType.tag == TypeTags.RECORD) {
                visitNewRecordArray(inst.elementTypedescOp.variableDcl);
            } else {
                this.mv.visitMethodInsn(INVOKESPECIAL, ARRAY_VALUE_IMPL, JVM_INIT_METHOD,
                        INIT_ARRAY, false);
            }
            this.storeToVar(inst.lhsOp.variableDcl);
        } else {
            this.loadVar(inst.typedescOp.variableDcl);
            this.mv.visitVarInsn(ALOAD, localVarOffset);
            loadListInitialValues(inst);
            this.mv.visitMethodInsn(INVOKEINTERFACE, TYPEDESC_VALUE, INSTANTIATE_FUNCTION,
                    INSTANTIATE_WITH_INITIAL_VALUES, true);
            this.storeToVar(inst.lhsOp.variableDcl);
        }
    }

    private void visitNewRecordArray(BIRNode.BIRVariableDcl elementTypeDesc) {
        this.loadVar(elementTypeDesc);
        this.mv.visitMethodInsn(INVOKESPECIAL, ARRAY_VALUE_IMPL, JVM_INIT_METHOD,
                INIT_ARRAY_WITH_INITIAL_VALUES, false);
    }

    void generateArrayStoreIns(BIRNonTerminator.FieldAccess inst) {

        this.loadVar(inst.lhsOp.variableDcl);
        this.loadVar(inst.keyOp.variableDcl);
        this.loadVar(inst.rhsOp.variableDcl);

        BType valueType = JvmCodeGenUtil.getImpliedType(inst.rhsOp.variableDcl.type);

        if (TypeTags.isIntegerTypeTag(valueType.tag)) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, ADD_METHOD, "(JJ)V", true);
        } else if (valueType.tag == TypeTags.FLOAT) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, ADD_METHOD, "(JD)V", true);
        } else if (TypeTags.isStringTypeTag(valueType.tag)) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, ADD_METHOD, ARRAY_ADD_BSTRING, true);
        } else if (valueType.tag == TypeTags.BOOLEAN) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, ADD_METHOD, "(JZ)V", true);
        } else if (valueType.tag == TypeTags.BYTE) {
            this.mv.visitInsn(I2B);
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, ADD_METHOD, "(JB)V", true);
        } else {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, ADD_METHOD, ARRAY_ADD_OBJECT, true);
        }
    }

    void generateArrayValueLoad(BIRNonTerminator.FieldAccess inst) {
        this.loadVar(inst.rhsOp.variableDcl);
        this.mv.visitTypeInsn(CHECKCAST, ARRAY_VALUE);
        this.loadVar(inst.keyOp.variableDcl);
        BType bType = inst.lhsOp.variableDcl.type;

        BType varRefType = JvmCodeGenUtil.getImpliedType(inst.rhsOp.variableDcl.type);
        if (varRefType.tag == TypeTags.TUPLE) {
            if (inst.fillingRead) {
                this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "fillAndGetRefValue",
                        GET_STRING_FROM_ARRAY, true);
            } else {
                this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getRefValue",
                        GET_STRING_FROM_ARRAY, true);
            }
            jvmCastGen.addUnboxInsn(this.mv, bType);
        } else if (TypeTags.isIntegerTypeTag(bType.tag)) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getInt", "(J)J", true);
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
                this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getBString",
                                        GET_BSTRING_FOR_ARRAY_INDEX, true);
        } else if (bType.tag == TypeTags.BOOLEAN) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getBoolean", "(J)Z", true);
        } else if (bType.tag == TypeTags.BYTE) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getByte", "(J)B", true);
            this.mv.visitMethodInsn(INVOKESTATIC, BYTE_VALUE, JVM_TO_UNSIGNED_INT_METHOD, "(B)I", false);
        } else if (bType.tag == TypeTags.FLOAT) {
            this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getFloat", "(J)D", true);
        } else {
            if (inst.fillingRead) {
                this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "fillAndGetRefValue",
                        GET_STRING_FROM_ARRAY, true);
            } else {
                this.mv.visitMethodInsn(INVOKEINTERFACE, ARRAY_VALUE, "getRefValue",
                        GET_STRING_FROM_ARRAY, true);
            }
            String targetTypeClass = getTargetClass(bType);
            if (targetTypeClass != null) {
                this.mv.visitTypeInsn(CHECKCAST, targetTypeClass);
            } else {
                jvmCastGen.addUnboxInsn(this.mv, bType);
            }
        }
        this.storeToVar(inst.lhsOp.variableDcl);
    }

    void generateTableNewIns(NewTable inst) {

        this.mv.visitTypeInsn(NEW, TABLE_VALUE_IMPL);
        this.mv.visitInsn(DUP);
        jvmTypeGen.loadType(this.mv, inst.type);
        this.loadVar(inst.dataOp.variableDcl);
        this.loadVar(inst.keyColOp.variableDcl);
        this.mv.visitMethodInsn(INVOKESPECIAL, TABLE_VALUE_IMPL, JVM_INIT_METHOD, INIT_TABLE_VALUE_IMPL, false);

        this.storeToVar(inst.lhsOp.variableDcl);
    }

    void generateTableLoadIns(FieldAccess inst) {

        this.loadVar(inst.rhsOp.variableDcl);
        this.mv.visitTypeInsn(CHECKCAST, TABLE_VALUE);
        this.loadVar(inst.keyOp.variableDcl);
        jvmCastGen.addBoxInsn(this.mv, inst.keyOp.variableDcl.type);
        BType bType = inst.lhsOp.variableDcl.type;
        this.mv.visitMethodInsn(INVOKEINTERFACE, TABLE_VALUE, GET_BOXED_VALUE,
                PASS_OBJECT_RETURN_OBJECT, true);

        String targetTypeClass = getTargetClass(bType);
        if (targetTypeClass != null) {
            this.mv.visitTypeInsn(CHECKCAST, targetTypeClass);
        } else {
            jvmCastGen.addUnboxInsn(this.mv, bType);
        }

        this.storeToVar(inst.lhsOp.variableDcl);
    }

    void generateTableStoreIns(FieldAccess inst) {

        this.loadVar(inst.lhsOp.variableDcl);
        this.loadVar(inst.keyOp.variableDcl);
        BType keyType = inst.keyOp.variableDcl.type;
        jvmCastGen.addBoxInsn(this.mv, keyType);
        BType valueType = inst.rhsOp.variableDcl.type;
        this.loadVar(inst.rhsOp.variableDcl);
        jvmCastGen.addBoxInsn(this.mv, valueType);

        this.mv.visitMethodInsn(INVOKESTATIC, TABLE_UTILS, "handleTableStore", HANDLE_TABLE_STORE, false);
    }

    void generateNewErrorIns(BIRNonTerminator.NewError newErrorIns) {

        this.mv.visitTypeInsn(NEW, ERROR_VALUE);
        this.mv.visitInsn(DUP);
        // load errorType
        jvmTypeGen.loadType(this.mv, newErrorIns.type);
        this.loadVar(newErrorIns.messageOp.variableDcl);
        this.loadVar(newErrorIns.causeOp.variableDcl);
        this.loadVar(newErrorIns.detailOp.variableDcl);
        this.mv.visitMethodInsn(INVOKESPECIAL, ERROR_VALUE, JVM_INIT_METHOD, INIT_ERROR_WITH_TYPE, false);
        this.storeToVar(newErrorIns.lhsOp.variableDcl);
    }

    void generateCastIns(BIRNonTerminator.TypeCast typeCastIns) {
        // load source value
        this.loadVar(typeCastIns.rhsOp.variableDcl);
        if (typeCastIns.checkTypes) {
            jvmCastGen.generateCheckCast(this.mv, typeCastIns.rhsOp.variableDcl.type, typeCastIns.type, this.indexMap);
        } else {
            jvmCastGen.generateCast(this.mv, typeCastIns.rhsOp.variableDcl.type, typeCastIns.type);
        }
        this.storeToVar(typeCastIns.lhsOp.variableDcl);
    }

    void generateIsLikeIns(BIRNonTerminator.IsLike isLike) {
        // load source value
        this.loadVar(isLike.rhsOp.variableDcl);

        // load targetType
        jvmTypeGen.loadType(this.mv, isLike.type);

        this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "checkIsLikeType",
                                CHECK_IS_TYPE, false);
        this.storeToVar(isLike.lhsOp.variableDcl);
    }

    void generateObjectNewIns(BIRNonTerminator.NewInstance objectNewIns, int strandIndex) {

        BType type = jvmPackageGen.lookupTypeDef(objectNewIns);
        String className;
        if (objectNewIns.isExternalDef) {
            className = getTypeValueClassName(JvmCodeGenUtil.getPackageName(objectNewIns.externalPackageId),
                                              objectNewIns.objectName);
        } else {
            className = getTypeValueClassName(JvmCodeGenUtil.getPackageName(type.tsymbol.pkgID),
                                              objectNewIns.def.internalName.value);
        }

        this.mv.visitTypeInsn(NEW, className);
        this.mv.visitInsn(DUP);

        jvmTypeGen.loadType(mv, objectNewIns.expectedType);
        reloadObjectCtorAnnots(type, strandIndex);
        this.mv.visitMethodInsn(INVOKESPECIAL, className, JVM_INIT_METHOD, OBJECT_TYPE_IMPL_INIT, false);
        this.storeToVar(objectNewIns.lhsOp.variableDcl);
    }

    private void reloadObjectCtorAnnots(BType type, int strandIndex) {
        if ((type.getFlags() & Flags.OBJECT_CTOR) == Flags.OBJECT_CTOR) {
            this.mv.visitTypeInsn(CHECKCAST, OBJECT_TYPE_IMPL);
            mv.visitMethodInsn(INVOKEVIRTUAL, OBJECT_TYPE_IMPL, "duplicate", OBJECT_TYPE_DUPLICATE, false);
            this.mv.visitInsn(DUP);

            String pkgClassName = currentPackageName.equals(".") || currentPackageName.isEmpty() ?
                    MODULE_INIT_CLASS_NAME : jvmPackageGen.lookupGlobalVarClassName(currentPackageName,
                    ANNOTATION_MAP_NAME);

            this.mv.visitFieldInsn(GETSTATIC, pkgClassName, ANNOTATION_MAP_NAME, GET_MAP_VALUE);
            this.mv.visitVarInsn(ALOAD, strandIndex);
            this.mv.visitMethodInsn(INVOKESTATIC, ANNOTATION_UTILS, "processObjectCtorAnnotations",
                   PROCESS_OBJ_CTR_ANNOTATIONS, false);
        }
    }

    void generateFPLoadIns(BIRNonTerminator.FPLoad inst) {
        this.mv.visitTypeInsn(NEW, FUNCTION_POINTER);
        this.mv.visitInsn(DUP);
        String name = inst.funcName.value;

        String funcKey = inst.pkgId.toString() + ":" + name;
        BType type = JvmCodeGenUtil.getImpliedType(inst.type);
        if (type.tag != TypeTags.INVOKABLE) {
            throw new BLangCompilerException("Expected BInvokableType, found " + type);
        }
        for (BIROperand operand : inst.closureMaps) {
            if (operand != null) {
                this.loadVar(operand.variableDcl);
            }
        }
        LambdaFunction lambdaFunction = functions.get(funcKey);
        if (lambdaFunction == null) {
            lambdaFunction = asyncDataCollector.addAndGetLambda(name, inst, false);
            functions.put(funcKey, lambdaFunction);
        }
        JvmCodeGenUtil.visitInvokeDynamic(mv, lambdaFunction.enclosingClass, lambdaFunction.lambdaName,
                inst.closureMaps.size());
        // Need to remove once we fix #37875
        type = inst.lhsOp.variableDcl.type.tag == TypeTags.TYPEREFDESC ? inst.lhsOp.variableDcl.type : type;

        jvmTypeGen.loadType(this.mv, type);
        if (inst.strandName != null) {
            mv.visitLdcInsn(inst.strandName);
        } else {
            mv.visitInsn(ACONST_NULL);
        }

        if (inst.schedulerPolicy == SchedulerPolicy.ANY) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }
        this.mv.visitMethodInsn(INVOKESPECIAL, FUNCTION_POINTER, JVM_INIT_METHOD, FP_INIT, false);

        PackageID boundMethodPkgId = inst.boundMethodPkgId;
        String funcPkgName = JvmCodeGenUtil.getPackageName(boundMethodPkgId == null ? inst.pkgId : boundMethodPkgId);
        // Set annotations if available.
        this.mv.visitInsn(DUP);
        String pkgClassName = funcPkgName.isEmpty() ? MODULE_INIT_CLASS_NAME :
                jvmPackageGen.lookupGlobalVarClassName(funcPkgName, ANNOTATION_MAP_NAME);
        this.mv.visitFieldInsn(GETSTATIC, pkgClassName, ANNOTATION_MAP_NAME, GET_MAP_VALUE);
        // Format of name `$anon$method$delegate$Foo.func$0`.
        this.mv.visitLdcInsn(name.startsWith(ANON_METHOD_DELEGATE) ?
                name.subSequence(ANON_METHOD_DELEGATE.length(), name.lastIndexOf("$")) :
                name);
        this.mv.visitMethodInsn(INVOKESTATIC, ANNOTATION_UTILS, "processFPValueAnnotations",
                PROCESS_FP_ANNOTATIONS, false);
        this.storeToVar(inst.lhsOp.variableDcl);
    }

    private void generateRecordDefaultFPLoadIns(BIRNonTerminator.RecordDefaultFPLoad inst) {
        jvmTypeGen.loadType(this.mv, inst.enclosedType);
        this.mv.visitTypeInsn(CHECKCAST, RECORD_TYPE_IMPL);
        this.mv.visitLdcInsn(Utils.unescapeBallerina(inst.fieldName));
        this.loadVar(inst.lhsOp.variableDcl);
        this.mv.visitMethodInsn(INVOKEVIRTUAL, RECORD_TYPE_IMPL, "setDefaultValue", SET_DEFAULT_VALUE_METHOD,
                false);
        Optional<BIntersectionType> immutableType = Types.getImmutableType(symbolTable,
                inst.enclosedType.tsymbol.pkgID, (SelectivelyImmutableReferenceType) inst.enclosedType);
        if (immutableType.isEmpty()) {
            return;
        }
        BRecordType effectiveType = (BRecordType) immutableType.get().effectiveType;
        jvmTypeGen.loadType(this.mv, effectiveType);
        this.mv.visitTypeInsn(CHECKCAST, RECORD_TYPE_IMPL);
        this.mv.visitLdcInsn(Utils.unescapeBallerina(inst.fieldName));
        this.loadVar(inst.lhsOp.variableDcl);
        this.mv.visitMethodInsn(INVOKEVIRTUAL, RECORD_TYPE_IMPL, "setDefaultValue", SET_DEFAULT_VALUE_METHOD,
                false);
    }

    void generateNewXMLElementIns(BIRNonTerminator.NewXMLElement newXMLElement) {

        this.loadVar(newXMLElement.startTagOp.variableDcl);
        this.mv.visitTypeInsn(CHECKCAST, XML_QNAME);
        this.loadVar(newXMLElement.defaultNsURIOp.variableDcl);
        if (newXMLElement.readonly) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }

        this.mv.visitMethodInsn(INVOKESTATIC, XML_FACTORY, "createXMLElement",
                                CREATE_XML_ELEMENT,
                                false);
        this.storeToVar(newXMLElement.lhsOp.variableDcl);
    }

    void generateNewXMLQNameIns(BIRNonTerminator.NewXMLQName newXMLQName) {

        this.mv.visitTypeInsn(NEW, XML_QNAME);
        this.mv.visitInsn(DUP);
        this.loadVar(newXMLQName.localnameOp.variableDcl);
        this.loadVar(newXMLQName.nsURIOp.variableDcl);
        this.loadVar(newXMLQName.prefixOp.variableDcl);
        this.mv.visitMethodInsn(INVOKESPECIAL, XML_QNAME, JVM_INIT_METHOD, INIT_XML_QNAME, false);
        this.storeToVar(newXMLQName.lhsOp.variableDcl);
    }

    void generateNewStringXMLQNameIns(BIRNonTerminator.NewStringXMLQName newStringXMLQName) {

        this.mv.visitTypeInsn(NEW, XML_QNAME);
        this.mv.visitInsn(DUP);
        this.loadVar(newStringXMLQName.stringQNameOP.variableDcl);
        this.mv.visitMethodInsn(INVOKESPECIAL, XML_QNAME, JVM_INIT_METHOD,
                INIT_WITH_STRING, false);
        this.storeToVar(newStringXMLQName.lhsOp.variableDcl);
    }

    void generateNewXMLTextIns(BIRNonTerminator.NewXMLText newXMLText) {

        this.loadVar(newXMLText.textOp.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, XML_FACTORY, "createXMLText", CREATE_XML_TEXT, false);
        this.storeToVar(newXMLText.lhsOp.variableDcl);
    }

    void generateNewXMLCommentIns(BIRNonTerminator.NewXMLComment newXMLComment) {

        this.loadVar(newXMLComment.textOp.variableDcl);

        if (newXMLComment.readonly) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }

        this.mv.visitMethodInsn(INVOKESTATIC, XML_FACTORY, "createXMLComment",
                                CREATE_XML_COMMENT, false);
        this.storeToVar(newXMLComment.lhsOp.variableDcl);
    }

    void generateNewXMLProcIns(BIRNonTerminator.NewXMLProcIns newXMLPI) {

        this.loadVar(newXMLPI.targetOp.variableDcl);
        this.loadVar(newXMLPI.dataOp.variableDcl);

        if (newXMLPI.readonly) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }

        this.mv.visitMethodInsn(INVOKESTATIC, XML_FACTORY, "createXMLProcessingInstruction",
                                CREATE_XML_PI, false);
        this.storeToVar(newXMLPI.lhsOp.variableDcl);
    }

    void generateNewXMLSequenceIns(BIRNonTerminator.NewXMLSequence xmlSequenceIns) {
        this.mv.visitMethodInsn(INVOKESTATIC, XML_FACTORY, "createXmlSequence", CRETAE_XML_SEQUENCE, false);
        this.storeToVar(xmlSequenceIns.lhsOp.variableDcl);
    }

    void generateXMLStoreIns(BIRNonTerminator.XMLAccess xmlStoreIns) {

        this.loadVar(xmlStoreIns.lhsOp.variableDcl);
        this.mv.visitTypeInsn(CHECKCAST, XML_VALUE);
        this.loadVar(xmlStoreIns.rhsOp.variableDcl);
        this.mv.visitMethodInsn(INVOKEVIRTUAL, XML_VALUE, "addChildren", XML_ADD_CHILDREN,
                false);
    }

    void generateXMLLoadAllIns(BIRNonTerminator.XMLAccess xmlLoadAllIns) {

        this.loadVar(xmlLoadAllIns.rhsOp.variableDcl);
        this.mv.visitMethodInsn(INVOKEVIRTUAL, XML_VALUE, "children", XML_CHILDREN,
                false);
        this.storeToVar(xmlLoadAllIns.lhsOp.variableDcl);
    }

    void generateXMLAttrLoadIns(BIRNonTerminator.FieldAccess xmlAttrStoreIns) {
        // visit xml_ref
        this.loadVar(xmlAttrStoreIns.rhsOp.variableDcl);

        // visit attribute name expr
        this.loadVar(xmlAttrStoreIns.keyOp.variableDcl);
        this.mv.visitTypeInsn(CHECKCAST, XML_QNAME);

        // invoke getAttribute() method
        this.mv.visitMethodInsn(INVOKEVIRTUAL, XML_VALUE, "getAttribute", XML_GET_ATTRIBUTE, false);

        this.storeToVar(xmlAttrStoreIns.lhsOp.variableDcl);
    }

    void generateXMLAttrStoreIns(BIRNonTerminator.FieldAccess xmlAttrStoreIns) {
        // visit xml_ref
        this.loadVar(xmlAttrStoreIns.lhsOp.variableDcl);

        // visit attribute name expr
        this.loadVar(xmlAttrStoreIns.keyOp.variableDcl);
        this.mv.visitTypeInsn(CHECKCAST, XML_QNAME);

        // visit attribute value expr
        this.loadVar(xmlAttrStoreIns.rhsOp.variableDcl);

        // invoke setAttribute() method
        this.mv.visitMethodInsn(INVOKEVIRTUAL, XML_VALUE, "setAttribute", XML_SET_ATTRIBUTE, false);
    }

    void generateXMLLoadIns(BIRNonTerminator.FieldAccess xmlLoadIns) {
        // visit xml_ref
        this.loadVar(xmlLoadIns.rhsOp.variableDcl);

        // visit element name/index expr
        this.loadVar(xmlLoadIns.keyOp.variableDcl);

        if (TypeTags.isStringTypeTag(JvmCodeGenUtil.getImpliedType(xmlLoadIns.keyOp.variableDcl.type).tag)) {
            // invoke `children(name)` method
            this.mv.visitMethodInsn(INVOKEVIRTUAL, XML_VALUE, "children",
                    XML_CHILDREN_FROM_STRING, false);
        } else {
            // invoke `getItem(index)` method
            this.mv.visitInsn(L2I);
            this.mv.visitMethodInsn(INVOKEVIRTUAL, XML_VALUE, "getItem", XML_GET_ITEM, false);
        }

        this.storeToVar(xmlLoadIns.lhsOp.variableDcl);
    }

    void generateNewRegExpIns(BIRNonTerminator.NewRegExp newRegExp) {
        this.loadVar(newRegExp.reDisjunction.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createRegExpValue", CREATE_REGEXP, false);
        this.storeToVar(newRegExp.lhsOp.variableDcl);
    }

    void generateNewRegExpDisjunctionIns(BIRNonTerminator.NewReDisjunction newReDisjunction) {
        this.loadVar(newReDisjunction.sequences.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReDisjunction", CREATE_RE_DISJUNCTION, false);
        this.storeToVar(newReDisjunction.lhsOp.variableDcl);
    }

    void generateNewRegExpSequenceIns(BIRNonTerminator.NewReSequence newReSequence) {
        this.loadVar(newReSequence.terms.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReSequence", CREATE_RE_SEQUENCE, false);
        this.storeToVar(newReSequence.lhsOp.variableDcl);
    }

    void generateNewRegExpAssertionIns(BIRNonTerminator.NewReAssertion newReAssertion) {
        this.loadVar(newReAssertion.assertion.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReAssertion", CREATE_RE_ASSERTION, false);
        this.storeToVar(newReAssertion.lhsOp.variableDcl);
    }

    void generateNewRegExpAtomQuantifierIns(BIRNonTerminator.NewReAtomQuantifier newReAtomQuantifier) {
        this.loadVar(newReAtomQuantifier.atom.variableDcl);
        this.loadVar(newReAtomQuantifier.quantifier.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReAtomQuantifier", CREATE_RE_ATOM_QUANTIFIER,
                false);
        this.storeToVar(newReAtomQuantifier.lhsOp.variableDcl);
    }

    void generateNewRegExpLiteralCharOrEscapeIns(BIRNonTerminator.NewReLiteralCharOrEscape newReLiteralCharOrEscape) {
        this.loadVar(newReLiteralCharOrEscape.charOrEscape.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReLiteralCharOrEscape", CREATE_RE_LITERAL_CHAR,
                false);
        this.storeToVar(newReLiteralCharOrEscape.lhsOp.variableDcl);
    }

    void generateNewRegExpCharacterClassIns(BIRNonTerminator.NewReCharacterClass newReCharacterClass) {
        this.loadVar(newReCharacterClass.classStart.variableDcl);
        this.loadVar(newReCharacterClass.negation.variableDcl);
        this.loadVar(newReCharacterClass.charSet.variableDcl);
        this.loadVar(newReCharacterClass.classEnd.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReCharacterClass", CREATE_RE_CHAR_CLASS,
                false);
        this.storeToVar(newReCharacterClass.lhsOp.variableDcl);
    }

    void generateNewRegExpCharSetIns(BIRNonTerminator.NewReCharSet newReCharSet) {
        this.loadVar(newReCharSet.charSetAtoms.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReCharSet", CREATE_RE_CHAR_SET, false);
        this.storeToVar(newReCharSet.lhsOp.variableDcl);
    }

    void generateNewRegExpCharSetRangeIns(BIRNonTerminator.NewReCharSetRange newReCharSetRange) {
        this.loadVar(newReCharSetRange.lhsCharSetAtom.variableDcl);
        this.loadVar(newReCharSetRange.dash.variableDcl);
        this.loadVar(newReCharSetRange.rhsCharSetAtom.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReCharSetRange", CREATE_RE_CHAR_SET_RANGE,
                false);
        this.storeToVar(newReCharSetRange.lhsOp.variableDcl);
    }

    void generateNewRegExpCapturingGroupIns(BIRNonTerminator.NewReCapturingGroup newReCapturingGroup) {
        this.loadVar(newReCapturingGroup.openParen.variableDcl);
        this.loadVar(newReCapturingGroup.flagExpr.variableDcl);
        this.loadVar(newReCapturingGroup.reDisjunction.variableDcl);
        this.loadVar(newReCapturingGroup.closeParen.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReCapturingGroup",
                CREATE_RE_CAPTURING_GROUP, false);
        this.storeToVar(newReCapturingGroup.lhsOp.variableDcl);
    }

    void generateNewRegExpFlagExprIns(BIRNonTerminator.NewReFlagExpression newReFlagExpression) {
        this.loadVar(newReFlagExpression.questionMark.variableDcl);
        this.loadVar(newReFlagExpression.flagsOnOff.variableDcl);
        this.loadVar(newReFlagExpression.colon.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReFlagExpression", CREATE_RE_FLAG_EXPR, false);
        this.storeToVar(newReFlagExpression.lhsOp.variableDcl);
    }

    void generateNewRegExpFlagOnOffIns(BIRNonTerminator.NewReFlagOnOff newReFlagOnOff) {
        this.loadVar(newReFlagOnOff.flags.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReFlagOnOff", CREATE_RE_FLAG_ON_OFF, false);
        this.storeToVar(newReFlagOnOff.lhsOp.variableDcl);
    }

    void generateNewRegExpQuantifierIns(BIRNonTerminator.NewReQuantifier newReQuantifier) {
        this.loadVar(newReQuantifier.quantifier.variableDcl);
        this.loadVar(newReQuantifier.nonGreedyChar.variableDcl);
        this.mv.visitMethodInsn(INVOKESTATIC, REG_EXP_FACTORY, "createReQuantifier", CREATE_RE_QUANTIFIER, false);
        this.storeToVar(newReQuantifier.lhsOp.variableDcl);
    }

    void generateTypeofIns(BIRNonTerminator.UnaryOP unaryOp) {

        this.loadVar(unaryOp.rhsOp.variableDcl);
        jvmCastGen.addBoxInsn(this.mv, unaryOp.rhsOp.variableDcl.type);
        this.mv.visitMethodInsn(INVOKESTATIC, TYPE_CHECKER, "getTypedesc", GET_TYPEDESC_OF_OBJECT, false);
        this.storeToVar(unaryOp.lhsOp.variableDcl);
    }

    void generateNotIns(BIRNonTerminator.UnaryOP unaryOp) {

        this.loadVar(unaryOp.rhsOp.variableDcl);

        Label label1 = new Label();
        Label label2 = new Label();

        this.mv.visitJumpInsn(IFNE, label1);
        this.mv.visitInsn(ICONST_1);
        this.mv.visitJumpInsn(GOTO, label2);
        this.mv.visitLabel(label1);
        this.mv.visitInsn(ICONST_0);
        this.mv.visitLabel(label2);

        this.storeToVar(unaryOp.lhsOp.variableDcl);
    }

    void generateNegateIns(BIRNonTerminator.UnaryOP unaryOp) {

        this.loadVar(unaryOp.rhsOp.variableDcl);

        BType btype = JvmCodeGenUtil.getImpliedType(unaryOp.rhsOp.variableDcl.type);
        if (TypeTags.isIntegerTypeTag(btype.tag)) {
            this.mv.visitInsn(LNEG);
        } else if (btype.tag == TypeTags.BYTE) {
            this.mv.visitInsn(INEG);
        } else if (btype.tag == TypeTags.FLOAT) {
            this.mv.visitInsn(DNEG);
        } else if (btype.tag == TypeTags.DECIMAL) {
            this.mv.visitMethodInsn(INVOKEVIRTUAL, DECIMAL_VALUE, "negate", DECIMAL_NEGATE, false);
        } else {
            throw new BLangCompilerException("Negation is not supported for type: " + btype);
        }

        this.storeToVar(unaryOp.lhsOp.variableDcl);
    }

    void generateNewTypedescIns(BIRNonTerminator.NewTypeDesc newTypeDesc) {
        String className = TYPEDESC_VALUE_IMPL;
        BType type = JvmCodeGenUtil.getImpliedType(newTypeDesc.type);
        if (type.tag == TypeTags.RECORD) {
            className = getTypeDescClassName(JvmCodeGenUtil.getPackageName(type.tsymbol.pkgID), toNameString(type));
        }
        this.mv.visitTypeInsn(NEW, className);
        this.mv.visitInsn(DUP);
        jvmTypeGen.loadType(this.mv, newTypeDesc.type);
        BIROperand annotations = newTypeDesc.annotations;
        if (annotations != null) {
            this.loadVar(annotations.variableDcl);
            this.mv.visitMethodInsn(INVOKESPECIAL, className, JVM_INIT_METHOD, TYPE_DESC_CONSTRUCTOR_WITH_ANNOTATIONS,
                    false);
        } else {
            this.mv.visitMethodInsn(INVOKESPECIAL, className, JVM_INIT_METHOD, TYPE_DESC_CONSTRUCTOR, false);
        }
        this.storeToVar(newTypeDesc.lhsOp.variableDcl);
    }

    void loadVar(BIRNode.BIRVariableDcl varDcl) {
        generateVarLoad(this.mv, varDcl, this.getJVMIndexOfVarRef(varDcl));
    }

    void storeToVar(BIRNode.BIRVariableDcl varDcl) {
        generateVarStore(this.mv, varDcl, this.getJVMIndexOfVarRef(varDcl));
    }

    void generateConstantLoadIns(BIRNonTerminator.ConstantLoad loadIns) {

        JvmCodeGenUtil.loadConstantValue(loadIns.type, loadIns.value, this.mv, jvmConstantsGen);
        this.storeToVar(loadIns.lhsOp.variableDcl);
    }

    private void loadListInitialValues(BIRNonTerminator.NewArray arrayNewIns) {
        List<BIRNode.BIRListConstructorEntry> initialValues = arrayNewIns.values;
        mv.visitLdcInsn((long) initialValues.size());
        mv.visitInsn(L2I);
        mv.visitTypeInsn(ANEWARRAY, LIST_INITIAL_VALUE_ENTRY);

        int i = 0;
        for (BIRNode.BIRListConstructorEntry initialValueOp : initialValues) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn((long) i);
            mv.visitInsn(L2I);
            i += 1;

            if (initialValueOp instanceof BIRNode.BIRListConstructorExprEntry) {
                createExprEntry(initialValueOp);
            } else {
                createSpreadEntry(initialValueOp);
            }

            mv.visitInsn(AASTORE);
        }
    }

    private void loadListInitialValues(JLargeArrayInstruction largeArrayIns) {
        this.loadVar(largeArrayIns.values.variableDcl);
        mv.visitMethodInsn(INVOKEVIRTUAL, HANDLE_VALUE, GET_VALUE_METHOD, RETURN_OBJECT, false);
        mv.visitTypeInsn(CHECKCAST, "[L" + B_LIST_INITIAL_VALUE_ENTRY + ";");
    }

    private void createExprEntry(BIRNode.BIRListConstructorEntry initialValueOp) {
        mv.visitTypeInsn(NEW, LIST_INITIAL_EXPRESSION_ENTRY);
        mv.visitInsn(DUP);

        BIRNode.BIRVariableDcl varDecl = initialValueOp.exprOp.variableDcl;
        this.loadVar(varDecl);
        jvmCastGen.addBoxInsn(this.mv, varDecl.type);

        mv.visitMethodInsn(INVOKESPECIAL, LIST_INITIAL_EXPRESSION_ENTRY, JVM_INIT_METHOD,
                INIT_LIST_INITIAL_EXPRESSION_ENTRY, false);
    }

    private void createSpreadEntry(BIRNode.BIRListConstructorEntry initialValueOp) {
        mv.visitTypeInsn(NEW, LIST_INITIAL_SPREAD_ENTRY);
        mv.visitInsn(DUP);

        BIRNode.BIRVariableDcl varDecl = initialValueOp.exprOp.variableDcl;
        this.loadVar(varDecl);

        mv.visitMethodInsn(INVOKESPECIAL, LIST_INITIAL_SPREAD_ENTRY, JVM_INIT_METHOD, INIT_LIST_INITIAL_SPREAD_ENTRY,
                false);
    }

    void generateInstructions(int localVarOffset, BIRInstruction inst) {
        if (inst instanceof BIRNonTerminator.BinaryOp) {
            generateBinaryOpIns((BIRNonTerminator.BinaryOp) inst);
        } else {
            switch (inst.getKind()) {
                case MOVE -> generateMoveIns((BIRNonTerminator.Move) inst);
                case CONST_LOAD -> generateConstantLoadIns((BIRNonTerminator.ConstantLoad) inst);
                case NEW_STRUCTURE -> generateMapNewIns((BIRNonTerminator.NewStructure) inst, localVarOffset);
                case NEW_INSTANCE -> generateObjectNewIns((BIRNonTerminator.NewInstance) inst, localVarOffset);
                case MAP_STORE -> generateMapStoreIns((FieldAccess) inst);
                case NEW_TABLE -> generateTableNewIns((NewTable) inst);
                case TABLE_STORE -> generateTableStoreIns((FieldAccess) inst);
                case TABLE_LOAD -> generateTableLoadIns((FieldAccess) inst);
                case NEW_ARRAY -> generateArrayNewIns((BIRNonTerminator.NewArray) inst, localVarOffset);
                case ARRAY_STORE -> generateArrayStoreIns((FieldAccess) inst);
                case MAP_LOAD -> generateMapLoadIns((FieldAccess) inst);
                case ARRAY_LOAD -> generateArrayValueLoad((FieldAccess) inst);
                case NEW_ERROR -> generateNewErrorIns((BIRNonTerminator.NewError) inst);
                case TYPE_CAST -> generateCastIns((BIRNonTerminator.TypeCast) inst);
                case IS_LIKE -> generateIsLikeIns((BIRNonTerminator.IsLike) inst);
                case TYPE_TEST -> typeTestGen.generateTypeTestIns((BIRNonTerminator.TypeTest) inst);
                case OBJECT_STORE -> generateObjectStoreIns((FieldAccess) inst);
                case OBJECT_LOAD -> generateObjectLoadIns((FieldAccess) inst);
                case NEW_XML_ELEMENT -> generateNewXMLElementIns((BIRNonTerminator.NewXMLElement) inst);
                case NEW_XML_TEXT -> generateNewXMLTextIns((BIRNonTerminator.NewXMLText) inst);
                case NEW_XML_COMMENT -> generateNewXMLCommentIns((BIRNonTerminator.NewXMLComment) inst);
                case NEW_XML_PI -> generateNewXMLProcIns((BIRNonTerminator.NewXMLProcIns) inst);
                case NEW_XML_QNAME -> generateNewXMLQNameIns((BIRNonTerminator.NewXMLQName) inst);
                case NEW_STRING_XML_QNAME -> generateNewStringXMLQNameIns((BIRNonTerminator.NewStringXMLQName) inst);
                case NEW_XML_SEQUENCE -> generateNewXMLSequenceIns((BIRNonTerminator.NewXMLSequence) inst);
                case XML_SEQ_STORE -> generateXMLStoreIns((BIRNonTerminator.XMLAccess) inst);
                case XML_SEQ_LOAD, XML_LOAD -> generateXMLLoadIns((FieldAccess) inst);
                case XML_LOAD_ALL -> generateXMLLoadAllIns((BIRNonTerminator.XMLAccess) inst);
                case XML_ATTRIBUTE_STORE -> generateXMLAttrStoreIns((FieldAccess) inst);
                case XML_ATTRIBUTE_LOAD -> generateXMLAttrLoadIns((FieldAccess) inst);
                case NEW_REG_EXP -> generateNewRegExpIns((BIRNonTerminator.NewRegExp) inst);
                case NEW_RE_DISJUNCTION -> generateNewRegExpDisjunctionIns((BIRNonTerminator.NewReDisjunction) inst);
                case NEW_RE_SEQUENCE -> generateNewRegExpSequenceIns((BIRNonTerminator.NewReSequence) inst);
                case NEW_RE_ASSERTION -> generateNewRegExpAssertionIns((BIRNonTerminator.NewReAssertion) inst);
                case NEW_RE_ATOM_QUANTIFIER ->
                        generateNewRegExpAtomQuantifierIns((BIRNonTerminator.NewReAtomQuantifier) inst);
                case NEW_RE_LITERAL_CHAR_ESCAPE ->
                        generateNewRegExpLiteralCharOrEscapeIns((BIRNonTerminator.NewReLiteralCharOrEscape) inst);
                case NEW_RE_CHAR_CLASS ->
                        generateNewRegExpCharacterClassIns((BIRNonTerminator.NewReCharacterClass) inst);
                case NEW_RE_CHAR_SET -> generateNewRegExpCharSetIns((BIRNonTerminator.NewReCharSet) inst);
                case NEW_RE_CHAR_SET_RANGE ->
                        generateNewRegExpCharSetRangeIns((BIRNonTerminator.NewReCharSetRange) inst);
                case NEW_RE_CAPTURING_GROUP ->
                        generateNewRegExpCapturingGroupIns((BIRNonTerminator.NewReCapturingGroup) inst);
                case NEW_RE_FLAG_EXPR -> generateNewRegExpFlagExprIns((BIRNonTerminator.NewReFlagExpression) inst);
                case NEW_RE_FLAG_ON_OFF -> generateNewRegExpFlagOnOffIns((BIRNonTerminator.NewReFlagOnOff) inst);
                case NEW_RE_QUANTIFIER -> generateNewRegExpQuantifierIns((BIRNonTerminator.NewReQuantifier) inst);
                case FP_LOAD -> generateFPLoadIns((BIRNonTerminator.FPLoad) inst);
                case STRING_LOAD -> generateStringLoadIns((FieldAccess) inst);
                case TYPEOF -> generateTypeofIns((BIRNonTerminator.UnaryOP) inst);
                case NOT -> generateNotIns((BIRNonTerminator.UnaryOP) inst);
                case NEW_TYPEDESC -> generateNewTypedescIns((BIRNonTerminator.NewTypeDesc) inst);
                case NEGATE -> generateNegateIns((BIRNonTerminator.UnaryOP) inst);
                case PLATFORM -> generatePlatformIns((JInstruction) inst, localVarOffset);
                case RECORD_DEFAULT_FP_LOAD ->
                        generateRecordDefaultFPLoadIns((BIRNonTerminator.RecordDefaultFPLoad) inst);
                default -> throw new BLangCompilerException("JVM generation is not supported for operation " + inst);
            }
        }
    }
}
