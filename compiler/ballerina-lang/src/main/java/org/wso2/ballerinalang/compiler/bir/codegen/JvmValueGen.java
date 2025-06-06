/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.types.Env;
import org.ballerinalang.model.elements.PackageID;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.wso2.ballerinalang.compiler.bir.codegen.internal.AsyncDataCollector;
import org.wso2.ballerinalang.compiler.bir.codegen.methodgen.InitMethodGen;
import org.wso2.ballerinalang.compiler.bir.codegen.methodgen.MethodGen;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JFieldBIRFunction;
import org.wso2.ballerinalang.compiler.bir.codegen.model.JMethodBIRFunction;
import org.wso2.ballerinalang.compiler.bir.codegen.split.JvmConstantsGen;
import org.wso2.ballerinalang.compiler.bir.codegen.split.values.JvmObjectGen;
import org.wso2.ballerinalang.compiler.bir.codegen.split.values.JvmRecordGen;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRFunction;
import org.wso2.ballerinalang.compiler.semantics.analyzer.TypeHashVisitor;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.util.Flags;

import java.util.List;
import java.util.Map;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V21;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmCodeGenUtil.toNameString;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ABSTRACT_OBJECT_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ANNOTATIONS_FIELD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BAL_OPTIONAL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.B_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.CLASS_FILE_SUFFIX;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.CLASS_LOCK_VAR_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.INSTANTIATE_FUNCTION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JVM_INIT_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAP_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAP_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAX_METHOD_COUNT_PER_BALLERINA_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.POPULATE_INITIAL_VALUES_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.RECORD_INIT_WRAPPER_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.REENTRANT_LOCK;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.SPLIT_CLASS_SUFFIX;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPEDESC_CLASS_PREFIX;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPEDESC_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPEDESC_VALUE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPE_IMPL;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.UNSUPPORTED_OPERATION_EXCEPTION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.VALUE_CLASS_PREFIX;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmDesugarPhase.addDefaultableBooleanVarsToSignature;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.CAST_B_MAPPING_INITIAL_VALUE_ENTRY;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.GET_MAP_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INIT_TYPEDESC;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INSTANTIATE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.INSTANTIATE_WITH_INITIAL_VALUES;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.LOAD_LOCK;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.OBJECT_TYPE_IMPL_INIT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.POPULATE_INITIAL_VALUES;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.RECORD_VALUE_CLASS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.RETURN_OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.TYPE_DESC_CONSTRUCTOR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.TYPE_DESC_CONSTRUCTOR_WITH_ANNOTATIONS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.TYPE_PARAMETER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmSignatures.VOID_METHOD_DESC;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTypeGen.getTypeDesc;
import static org.wso2.ballerinalang.compiler.bir.codegen.interop.InteropMethodGen.desugarInteropFuncs;

/**
 * BIR values to JVM byte code generation class.
 *
 * @since 1.2.0
 */
public class JvmValueGen {

    private final BIRNode.BIRPackage module;
    private final JvmPackageGen jvmPackageGen;
    private final MethodGen methodGen;
    private final BType booleanType;
    private final JvmObjectGen jvmObjectGen;
    private final JvmRecordGen jvmRecordGen;
    private final TypeHashVisitor typeHashVisitor;
    private final Types types;

    JvmValueGen(BIRNode.BIRPackage module, JvmPackageGen jvmPackageGen, MethodGen methodGen,
                TypeHashVisitor typeHashVisitor, Types types) {
        this.module = module;
        this.jvmPackageGen = jvmPackageGen;
        this.methodGen = methodGen;
        this.booleanType = jvmPackageGen.symbolTable.booleanType;
        this.jvmRecordGen = new JvmRecordGen(jvmPackageGen.symbolTable);
        this.jvmObjectGen = new JvmObjectGen();
        this.typeHashVisitor = typeHashVisitor;
        this.types = types;
    }

    static void injectDefaultParamInitsToAttachedFuncs(Env env, BIRNode.BIRPackage module,
                                                       InitMethodGen initMethodGen) {
        List<BIRNode.BIRTypeDefinition> typeDefs = module.typeDefs;
        for (BIRNode.BIRTypeDefinition optionalTypeDef : typeDefs) {
            BType bType = JvmCodeGenUtil.getImpliedType(optionalTypeDef.type);
            if ((bType.tag == TypeTags.OBJECT && Symbols.isFlagOn(
                    bType.tsymbol.flags, Flags.CLASS)) || bType.tag == TypeTags.RECORD) {
                desugarObjectMethods(env, optionalTypeDef.attachedFuncs, initMethodGen);
            }
        }
    }

    private static void desugarObjectMethods(Env env, List<BIRFunction> attachedFuncs, InitMethodGen initMethodGen) {
        for (BIRNode.BIRFunction birFunc : attachedFuncs) {
            if (JvmCodeGenUtil.isExternFunc(birFunc)) {
                if (birFunc instanceof JMethodBIRFunction jMethodBIRFunction) {
                    desugarInteropFuncs(env, jMethodBIRFunction, initMethodGen);
                    initMethodGen.resetIds();
                } else if (!(birFunc instanceof JFieldBIRFunction)) {
                    initMethodGen.resetIds();
                }
            } else {
                addDefaultableBooleanVarsToSignature(env, birFunc);
                initMethodGen.resetIds();
            }
        }
    }

    public static String getTypeDescClassName(String packageName, String typeName) {
        return packageName + TYPEDESC_CLASS_PREFIX + typeName;
    }

    public static String getTypeValueClassName(String packageName, String typeName) {
        return packageName + VALUE_CLASS_PREFIX + typeName;
    }

    public static String getFieldIsPresentFlagName(String fieldName) {
        return "$" + fieldName + "$isPresent";
    }

    public static boolean isOptionalRecordField(BField field) {
        return (field.symbol.flags & BAL_OPTIONAL) == BAL_OPTIONAL;
    }

    void generateValueClasses(JarEntries jarEntries, JvmConstantsGen jvmConstantsGen, JvmTypeGen jvmTypeGen,
                              AsyncDataCollector asyncDataCollector) {
        String packageName = JvmCodeGenUtil.getPackageName(module.packageID);
        module.typeDefs.forEach(optionalTypeDef -> {
            if (optionalTypeDef.type.tag == TypeTags.TYPEREFDESC) {
                return;
            }
            BType bType = optionalTypeDef.type;
            String className = getTypeValueClassName(packageName, optionalTypeDef.internalName.value);
            String valueClass = VALUE_CLASS_PREFIX + optionalTypeDef.internalName.value;
            asyncDataCollector.setCurrentSourceFileName(valueClass);
            asyncDataCollector.setCurrentSourceFileWithoutExt(valueClass);
            if (optionalTypeDef.type.tag == TypeTags.OBJECT &&
                    Symbols.isFlagOn(optionalTypeDef.type.tsymbol.flags, Flags.CLASS)) {
                BObjectType objectType = (BObjectType) optionalTypeDef.type;
                this.createObjectValueClasses(objectType, className, optionalTypeDef, jvmConstantsGen,
                        asyncDataCollector, jarEntries);
            } else if (bType.tag == TypeTags.RECORD) {
                BRecordType recordType = (BRecordType) bType;
                byte[] bytes = this.createRecordValueClass(recordType, className, optionalTypeDef, jvmTypeGen);
                jarEntries.put(className + CLASS_FILE_SUFFIX, bytes);
                String typedescClass = getTypeDescClassName(packageName, optionalTypeDef.internalName.value);
                bytes = this.createRecordTypeDescClass(recordType, typedescClass, optionalTypeDef, jvmTypeGen);
                jarEntries.put(typedescClass + CLASS_FILE_SUFFIX, bytes);
            }
        });
    }


    private byte[] createRecordTypeDescClass(BRecordType recordType, String className,
                                             BIRNode.BIRTypeDefinition typeDef, JvmTypeGen jvmTypeGen) {

        ClassWriter cw = new BallerinaClassWriter(COMPUTE_FRAMES);
        if (typeDef.pos != null) {
            cw.visitSource(typeDef.pos.lineRange().fileName(), null);
        } else {
            cw.visitSource(className, null);
        }
        cw.visit(V21, ACC_PUBLIC + ACC_SUPER, className, null, TYPEDESC_VALUE_IMPL, new String[]{TYPEDESC_VALUE});

        FieldVisitor fv = cw.visitField(0, ANNOTATIONS_FIELD, GET_MAP_VALUE, null, null);
        fv.visitEnd();

        this.createTypeDescConstructor(cw, className);
        this.createTypeDescConstructorWithAnnotations(cw, className);
        this.createInstantiateMethod(cw, recordType, jvmTypeGen, className);
        this.createInstantiateMethodWithInitialValues(cw, recordType, typeDef, className);

        cw.visitEnd();
        return jvmPackageGen.getBytes(cw, typeDef);
    }

    private void createInstantiateMethod(ClassWriter cw, BRecordType recordType, JvmTypeGen jvmTypeGen,
                                         String className) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, INSTANTIATE_FUNCTION, INSTANTIATE, null, null);
        mv.visitCode();
        jvmTypeGen.loadType(mv, recordType);
        mv.visitTypeInsn(CHECKCAST, TYPE_IMPL);
        mv.visitMethodInsn(INVOKEVIRTUAL, TYPE_IMPL, "getZeroValue", RETURN_OBJECT, false);
        mv.visitInsn(ARETURN);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, INSTANTIATE_FUNCTION, className);
        mv.visitEnd();
    }

    private void createInstantiateMethodWithInitialValues(ClassWriter cw, BRecordType recordType,
                                         BIRNode.BIRTypeDefinition typeDef, String typeClass) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, INSTANTIATE_FUNCTION, INSTANTIATE_WITH_INITIAL_VALUES,
                null, null);
        mv.visitCode();

        String className = getTypeValueClassName(recordType.tsymbol.pkgID, toNameString(recordType));
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, className, JVM_INIT_METHOD, INIT_TYPEDESC, false);

        // Invoke the init-function of this type.
        String valueClassName;
        List<BIRFunction> attachedFuncs = typeDef.attachedFuncs;

        // Attached functions are empty for type-labeling. In such cases, call the init() of the original type value
        if (!attachedFuncs.isEmpty()) {
            valueClassName = className;
        } else {
            // record type is the original record-type of this type-label
            valueClassName = getTypeValueClassName(recordType.tsymbol.pkgID, toNameString(recordType));
        }

        mv.visitInsn(DUP);
        mv.visitTypeInsn(CHECKCAST, valueClassName);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, CAST_B_MAPPING_INITIAL_VALUE_ENTRY);
        mv.visitMethodInsn(INVOKEVIRTUAL, valueClassName, POPULATE_INITIAL_VALUES_METHOD,
                           POPULATE_INITIAL_VALUES, false);

        mv.visitInsn(ARETURN);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, INSTANTIATE_FUNCTION, typeClass);
        mv.visitEnd();
    }

    public static String getTypeValueClassName(PackageID packageID, String typeName) {
        return getTypeValueClassName(JvmCodeGenUtil.getPackageName(packageID), typeName);
    }

    private byte[] createRecordValueClass(BRecordType recordType, String className, BIRNode.BIRTypeDefinition typeDef,
                                          JvmTypeGen jvmTypeGen) {
        ClassWriter cw = new BallerinaClassWriter(COMPUTE_FRAMES);
        if (typeDef.pos != null) {
            cw.visitSource(typeDef.pos.lineRange().fileName(), null);
        } else {
            cw.visitSource(className, null);
        }
        JvmCastGen jvmCastGen = new JvmCastGen(jvmPackageGen.symbolTable, jvmTypeGen, types);
        cw.visit(V21, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, className, RECORD_VALUE_CLASS, MAP_VALUE_IMPL,
                new String[]{MAP_VALUE});

        Map<String, BField> fields = recordType.fields;
        this.createRecordFields(cw, fields);
        jvmRecordGen.createAndSplitGetMethod(cw, fields, className, jvmCastGen);
        jvmRecordGen.createAndSplitSetMethod(cw, fields, className, jvmCastGen);
        jvmRecordGen.createAndSplitEntrySetMethod(cw, fields, className, jvmCastGen);
        jvmRecordGen.createAndSplitContainsKeyMethod(cw, fields, className);
        jvmRecordGen.createAndSplitGetValuesMethod(cw, fields, className, jvmCastGen);
        this.createGetSizeMethod(cw, fields, className);
        this.createRecordClearMethod(cw, typeDef.name.value);
        jvmRecordGen.createAndSplitRemoveMethod(cw, fields, className, jvmCastGen);
        jvmRecordGen.createAndSplitGetKeysMethod(cw, fields, className);
        this.createRecordPopulateInitialValuesMethod(cw, className);

        this.createRecordConstructor(cw, INIT_TYPEDESC, className);
        this.createRecordConstructor(cw, TYPE_PARAMETER, className);
        cw.visitEnd();

        return jvmPackageGen.getBytes(cw, typeDef);
    }

    private void createTypeDescConstructor(ClassWriter cw, String className) {

        String descriptor = TYPE_DESC_CONSTRUCTOR;
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, JVM_INIT_METHOD, descriptor, null, null);
        mv.visitCode();

        // load super
        mv.visitVarInsn(ALOAD, 0);
        // load type
        mv.visitVarInsn(ALOAD, 1);

        // invoke `super(type)`;
        mv.visitMethodInsn(INVOKESPECIAL, TYPEDESC_VALUE_IMPL, JVM_INIT_METHOD, descriptor, false);

        mv.visitInsn(RETURN);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, JVM_INIT_METHOD, className);
        mv.visitEnd();
    }

    private void createTypeDescConstructorWithAnnotations(ClassWriter cw, String name) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, JVM_INIT_METHOD, TYPE_DESC_CONSTRUCTOR_WITH_ANNOTATIONS, null,
                null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitFieldInsn(PUTFIELD, name, ANNOTATIONS_FIELD, GET_MAP_VALUE);
        // load super
        mv.visitVarInsn(ALOAD, 0);
        // load type
        mv.visitVarInsn(ALOAD, 1);
        // load annotations
        mv.visitVarInsn(ALOAD, 2);
        // invoke `super(type)`;
        mv.visitMethodInsn(INVOKESPECIAL, TYPEDESC_VALUE_IMPL, JVM_INIT_METHOD, TYPE_DESC_CONSTRUCTOR_WITH_ANNOTATIONS,
                           false);

        mv.visitInsn(RETURN);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, JVM_INIT_METHOD, name);
        mv.visitEnd();
    }

    private void createRecordConstructor(ClassWriter cw, String argumentClass, String className) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, JVM_INIT_METHOD, argumentClass, null, null);
        mv.visitCode();

        // load super
        mv.visitVarInsn(ALOAD, 0);
        // load type
        mv.visitVarInsn(ALOAD, 1);
        // invoke `super(type)`;
        mv.visitMethodInsn(INVOKESPECIAL, MAP_VALUE_IMPL, JVM_INIT_METHOD, argumentClass, false);
        mv.visitInsn(RETURN);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, RECORD_INIT_WRAPPER_NAME, className);
        mv.visitEnd();
    }

    private void createRecordFields(ClassWriter cw, Map<String, BField> fields) {
        for (BField field : fields.values()) {
            if (field == null) {
                continue;
            }
            String fieldName = field.name.value;
            FieldVisitor fv = cw.visitField(0, fieldName, getTypeDesc(field.type), null, null);
            fv.visitEnd();

            if (isOptionalRecordField(field)) {
                fv = cw.visitField(0, getFieldIsPresentFlagName(fieldName), getTypeDesc(booleanType),
                        null, null);
                fv.visitEnd();
            }
        }
    }


    private void createGetSizeMethod(ClassWriter cw, Map<String, BField> fields, String className) {

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "size", "()I", null, null);
        mv.visitCode();
        int sizeVarIndex = 1;

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, MAP_VALUE_IMPL, "size", "()I", false);
        mv.visitVarInsn(ISTORE, sizeVarIndex);

        int requiredFieldsCount = 0;
        for (BField optionalField : fields.values()) {
            String fieldName = optionalField.name.value;
            if (isOptionalRecordField(optionalField)) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, getFieldIsPresentFlagName(fieldName),
                        getTypeDesc(booleanType));
                Label l3 = new Label();
                mv.visitJumpInsn(IFEQ, l3);
                mv.visitIincInsn(sizeVarIndex, 1);
                mv.visitLabel(l3);
            } else {
                requiredFieldsCount += 1;
            }
        }

        mv.visitIincInsn(sizeVarIndex, requiredFieldsCount);
        mv.visitVarInsn(ILOAD, sizeVarIndex);
        mv.visitInsn(IRETURN);

        JvmCodeGenUtil.visitMaxStackForMethod(mv, "size", className);
        mv.visitEnd();
    }

    private void createRecordPopulateInitialValuesMethod(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(ACC_PROTECTED, POPULATE_INITIAL_VALUES_METHOD,
                                          POPULATE_INITIAL_VALUES, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, MAP_VALUE_IMPL, POPULATE_INITIAL_VALUES_METHOD,
                           POPULATE_INITIAL_VALUES, false);
        mv.visitInsn(RETURN);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, POPULATE_INITIAL_VALUES_METHOD, className);
        mv.visitEnd();
    }

    private void createObjectValueClasses(BObjectType objectType, String className, BIRNode.BIRTypeDefinition typeDef,
                                          JvmConstantsGen jvmConstantsGen, AsyncDataCollector asyncDataCollector,
                                          JarEntries jarEntries) {
        ClassWriter cw = new BallerinaClassWriter(COMPUTE_FRAMES);
        cw.visitSource(typeDef.pos.lineRange().fileName(), null);

        SymbolTable symbolTable = jvmPackageGen.symbolTable;
        JvmTypeGen jvmTypeGen = new JvmTypeGen(jvmConstantsGen, module.packageID, typeHashVisitor, symbolTable);
        JvmCastGen jvmCastGen = new JvmCastGen(symbolTable, jvmTypeGen, types);
        cw.visit(V21, ACC_PUBLIC + ACC_SUPER, className, null, ABSTRACT_OBJECT_VALUE, new String[]{B_OBJECT});

        Map<String, BField> fields = objectType.fields;
        this.createObjectFields(cw, fields);

        List<BIRNode.BIRFunction> attachedFuncs = typeDef.attachedFuncs;
        if (attachedFuncs.size() > MAX_METHOD_COUNT_PER_BALLERINA_OBJECT) {
            this.createObjectMethodsWithSplitClasses(cw, attachedFuncs, className, objectType, jvmTypeGen,
                    jvmCastGen, jvmConstantsGen, asyncDataCollector, typeDef, jarEntries);
        } else {
            this.createObjectMethods(cw, attachedFuncs, className, objectType, jvmTypeGen, jvmCastGen,
                    jvmConstantsGen, asyncDataCollector);
        }

        this.createObjectInit(cw, className);
        jvmObjectGen.createAndSplitCallMethod(cw, attachedFuncs, className, jvmCastGen);
        jvmObjectGen.createAndSplitGetMethod(cw, fields, className, jvmCastGen);
        jvmObjectGen.createAndSplitSetMethod(cw, fields, className, jvmCastGen);
        jvmObjectGen.createAndSplitSetOnInitializationMethod(cw, fields, className);
        cw.visitEnd();
        jarEntries.put(className + CLASS_FILE_SUFFIX, jvmPackageGen.getBytes(cw, typeDef));
    }

    private void createObjectFields(ClassWriter cw, Map<String, BField> fields) {
        for (BField field : fields.values()) {
            if (field == null) {
                continue;
            }
            FieldVisitor fvb = cw.visitField(0, field.name.value, getTypeDesc(field.type), null, null);
            fvb.visitEnd();
        }
        // visit object self lock field.
        FieldVisitor fv = cw.visitField(ACC_PUBLIC, CLASS_LOCK_VAR_NAME, LOAD_LOCK, null, null);
        fv.visitEnd();
    }

    private void createObjectMethods(ClassWriter cw, List<BIRFunction> attachedFuncs, String moduleClassName,
                                     BObjectType currentObjectType, JvmTypeGen jvmTypeGen, JvmCastGen jvmCastGen,
                                     JvmConstantsGen jvmConstantsGen, AsyncDataCollector asyncDataCollector) {
        for (BIRNode.BIRFunction func : attachedFuncs) {
            methodGen.generateMethod(func, cw, module, currentObjectType, moduleClassName, jvmTypeGen, jvmCastGen,
                    jvmConstantsGen, asyncDataCollector);
        }
    }

    private void createObjectMethodsWithSplitClasses(ClassWriter cw, List<BIRFunction> attachedFuncs,
                                                     String moduleClassName, BObjectType currentObjectType,
                                                     JvmTypeGen jvmTypeGen, JvmCastGen jvmCastGen,
                                                     JvmConstantsGen jvmConstantsGen,
                                                     AsyncDataCollector asyncDataCollector,
                                                     BIRNode.BIRTypeDefinition typeDef,
                                                     JarEntries jarEntries) {
        int splitClassNum = 1;
        ClassWriter splitCW = new BallerinaClassWriter(COMPUTE_FRAMES);
        splitCW.visitSource(typeDef.pos.lineRange().fileName(), null);
        String splitClassName = moduleClassName + SPLIT_CLASS_SUFFIX + splitClassNum;
        splitCW.visit(V21, ACC_PUBLIC + ACC_SUPER, splitClassName, null, OBJECT, null);
        JvmCodeGenUtil.generateDefaultConstructor(splitCW, OBJECT);
        int methodCountPerSplitClass = 0;

        for (BIRNode.BIRFunction func : attachedFuncs) {
            if (func.name.value.contains("$init$")) {
                methodGen.generateMethod(func, cw, module, currentObjectType, moduleClassName,
                        jvmTypeGen, jvmCastGen, jvmConstantsGen, asyncDataCollector);
                continue;
            }
            methodGen.genJMethodWithBObjectMethodCall(func, cw, module, jvmTypeGen, jvmCastGen, jvmConstantsGen,
                    moduleClassName, asyncDataCollector, splitClassName);
            methodGen.genJMethodForBFunc(func, splitCW, module, jvmTypeGen, jvmCastGen, jvmConstantsGen,
                    moduleClassName, currentObjectType, asyncDataCollector, true);
            methodCountPerSplitClass++;
            if (methodCountPerSplitClass == MAX_METHOD_COUNT_PER_BALLERINA_OBJECT) {
                splitCW.visitEnd();
                byte[] splitBytes = jvmPackageGen.getBytes(splitCW, typeDef);
                jarEntries.put(splitClassName + CLASS_FILE_SUFFIX, splitBytes);
                splitClassNum++;
                splitCW = new BallerinaClassWriter(COMPUTE_FRAMES);
                splitCW.visitSource(typeDef.pos.lineRange().fileName(), null);
                splitClassName = moduleClassName + SPLIT_CLASS_SUFFIX + splitClassNum;
                splitCW.visit(V21, ACC_PUBLIC + ACC_SUPER, splitClassName, null, OBJECT, null);
                JvmCodeGenUtil.generateDefaultConstructor(splitCW, OBJECT);
                methodCountPerSplitClass = 0;
            }
        }
        if (methodCountPerSplitClass != 0) {
            splitCW.visitEnd();
            byte[] splitBytes = jvmPackageGen.getBytes(splitCW, typeDef);
            jarEntries.put(splitClassName + CLASS_FILE_SUFFIX, splitBytes);
        }
    }

    private void createObjectInit(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, JVM_INIT_METHOD, OBJECT_TYPE_IMPL_INIT, null,
                null);
        mv.visitCode();
        // load super
        mv.visitVarInsn(ALOAD, 0);
        // load type
        mv.visitVarInsn(ALOAD, 1);
        // invoke super(type);
        mv.visitMethodInsn(INVOKESPECIAL, ABSTRACT_OBJECT_VALUE, JVM_INIT_METHOD, OBJECT_TYPE_IMPL_INIT, false);

        // create object self lock
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, REENTRANT_LOCK);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, REENTRANT_LOCK, JVM_INIT_METHOD, VOID_METHOD_DESC, false);
        mv.visitFieldInsn(PUTFIELD, className, CLASS_LOCK_VAR_NAME, LOAD_LOCK);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void createRecordClearMethod(ClassWriter cw, String className) {
        // throw an UnsupportedOperationException, since clear is not supported by for records.
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "clear", VOID_METHOD_DESC, null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, UNSUPPORTED_OPERATION_EXCEPTION);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, UNSUPPORTED_OPERATION_EXCEPTION, JVM_INIT_METHOD, VOID_METHOD_DESC, false);
        mv.visitInsn(ATHROW);
        JvmCodeGenUtil.visitMaxStackForMethod(mv, "clear", className);
        mv.visitEnd();
    }
}
