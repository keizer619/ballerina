/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package org.wso2.ballerinalang.compiler.desugar;

import io.ballerina.identifier.Utils;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.TopLevelNode;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SchedulerPolicy;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotation;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangClassDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangErrorVariable;
import org.wso2.ballerinalang.compiler.tree.BLangExprFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangExternalFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangInvokableNode;
import org.wso2.ballerinalang.compiler.tree.BLangMarkdownDocumentation;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangRecordVariable;
import org.wso2.ballerinalang.compiler.tree.BLangResourceFunction;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTableKeyTypeConstraint;
import org.wso2.ballerinalang.compiler.tree.BLangTupleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangCollectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangFromClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangGroupByClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangGroupingKey;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangJoinClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangLetClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangLimitClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOnClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOnConflictClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOnFailClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOrderByClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOrderKey;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangSelectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangWhereClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAlternateWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrowFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckPanickedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCollectContextInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCommitExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConstRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConstant;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangDynamicArgExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangElvisExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangErrorConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangErrorVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExtendedXMLNavigationAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangGroupExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIgnoreExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInferredTypedescDefaultNode;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIsAssignableExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIsLikeExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLetExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMarkdownDocumentationLine;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMarkdownParameterDocumentation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMarkdownReturnParameterDocumentation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMultipleWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNaturalExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNumericLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangObjectConstructorExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryAction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRawTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRegExpTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRestArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangServiceConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStatementExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStringTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTableConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTernaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTransactionalExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTrapExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTupleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeTestExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypedescExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangUnaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWaitExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWaitForAllExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerAsyncSendExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerFlushExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerSyncSendExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLCommentLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLFilterStepExtend;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLIndexedStepExtend;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLMethodCallStepExtend;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLNavigationAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLProcInsLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQName;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQuotedString;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLSequenceLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLStepExtend;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBreak;
import org.wso2.ballerinalang.compiler.tree.statements.BLangCompoundAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangContinue;
import org.wso2.ballerinalang.compiler.tree.statements.BLangDo;
import org.wso2.ballerinalang.compiler.tree.statements.BLangErrorDestructure;
import org.wso2.ballerinalang.compiler.tree.statements.BLangErrorVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangFail;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForeach;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForkJoin;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangLock;
import org.wso2.ballerinalang.compiler.tree.statements.BLangMatchStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangPanic;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRecordDestructure;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRecordVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRetry;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRetryTransaction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRollback;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTransaction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTupleDestructure;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTupleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.statements.BLangXMLNSStatement;
import org.wso2.ballerinalang.compiler.tree.types.BLangArrayType;
import org.wso2.ballerinalang.compiler.tree.types.BLangBuiltInRefTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangConstrainedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangErrorType;
import org.wso2.ballerinalang.compiler.tree.types.BLangFiniteTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangFunctionTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangIntersectionTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangLetVariable;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangStreamType;
import org.wso2.ballerinalang.compiler.tree.types.BLangTableTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangTupleTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUnionTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.ClosureVarSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.ballerinalang.model.symbols.SymbolOrigin.VIRTUAL;
import static org.wso2.ballerinalang.compiler.util.CompilerUtils.isInParameterList;
import static org.wso2.ballerinalang.compiler.util.Constants.DOLLAR;
import static org.wso2.ballerinalang.compiler.util.Constants.RECORD_DELIMITER;
import static org.wso2.ballerinalang.compiler.util.Constants.UNDERSCORE;

/**
 * ClosureGenerator for creating closures for default values.
 *
 * @since 2201.3.0
 */
public class ClosureGenerator extends BLangNodeVisitor {
    private static final CompilerContext.Key<ClosureGenerator> CLOSURE_GENERATOR_KEY = new CompilerContext.Key<>();
    private Queue<BLangSimpleVariableDef> queue;
    private Queue<BLangSimpleVariableDef> annotationClosureReferences;
    private final SymbolTable symTable;
    private SymbolEnv env;
    private BLangNode result;
    private final SymbolResolver symResolver;
    private final AnnotationDesugar annotationDesugar;
    private final Types types;

    public static ClosureGenerator getInstance(CompilerContext context) {
        ClosureGenerator closureGenerator = context.get(CLOSURE_GENERATOR_KEY);
        if (closureGenerator == null) {
            closureGenerator = new ClosureGenerator(context);
        }

        return closureGenerator;
    }

    private ClosureGenerator(CompilerContext context) {
        context.put(CLOSURE_GENERATOR_KEY, this);
        this.symTable = SymbolTable.getInstance(context);
        this.queue = new LinkedList<>();
        this.annotationClosureReferences = new LinkedList<>();
        this.types = Types.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.annotationDesugar = AnnotationDesugar.getInstance(context);
    }

    @Override
    public void visit(BLangPackage pkgNode) {
        SymbolEnv pkgEnv = this.symTable.pkgEnvMap.get(pkgNode.symbol);

        for (int i = 0; i < pkgNode.functions.size(); i++) {
            BLangFunction bLangFunction = pkgNode.functions.get(i);
            if (!bLangFunction.flagSet.contains(Flag.LAMBDA)) {
                SymbolEnv funcEnv = SymbolEnv.createFunctionEnv(bLangFunction, bLangFunction.symbol.scope, pkgEnv);
                rewriteParamsAndReturnTypeOfFunction(bLangFunction, funcEnv);
            }
        }
        pkgNode.services.forEach(service -> rewrite(service, pkgEnv));
        pkgNode.typeDefinitions.forEach(typeDefinition -> rewrite(typeDefinition, pkgEnv));
        pkgNode.xmlnsList.forEach(xmlns -> rewrite(xmlns, pkgEnv));
        pkgNode.constants.forEach(constant -> rewrite(constant, pkgEnv));
        pkgNode.annotations.forEach(annotation -> rewrite(annotation, pkgEnv));
        pkgNode.initFunction = rewrite(pkgNode.initFunction, pkgEnv);
        rewrite(pkgNode.classDefinitions, pkgEnv);
        rewrite(pkgNode.globalVars, pkgEnv);
        addClosuresToGlobalVariableList(pkgEnv);
        for (int i = 0; i < pkgNode.functions.size(); i++) {
            BLangFunction bLangFunction = pkgNode.functions.get(i);
            if (!bLangFunction.flagSet.contains(Flag.LAMBDA)) {
                rewrite(bLangFunction, pkgEnv);
            }
        }

        result = pkgNode;
    }

    private void addClosuresToGlobalVariableList(SymbolEnv pkgEnv) {
        Iterator<BLangSimpleVariableDef> iterator = queue.iterator();
        while (iterator.hasNext()) {
            BLangSimpleVariable simpleVariable = queue.poll().var;
            simpleVariable.flagSet.add(Flag.PUBLIC);
            simpleVariable.symbol.flags |= Flags.PUBLIC;
            pkgEnv.enclPkg.globalVars.add(0, simpleVariable);
            pkgEnv.enclPkg.topLevelNodes.add(0, simpleVariable);
        }
        for (BLangSimpleVariableDef closureReference : annotationClosureReferences) {
            BLangSimpleVariable simpleVariable = rewrite(closureReference.var, pkgEnv);
            pkgEnv.enclPkg.globalVars.add(simpleVariable);
            pkgEnv.enclPkg.topLevelNodes.add(simpleVariable);
        }
    }

    @Override
    public void visit(BLangFunction funcNode) {
        SymbolEnv funcEnv = SymbolEnv.createFunctionEnv(funcNode, funcNode.symbol.scope, env);
        
        if (funcNode.flagSet.contains(Flag.LAMBDA)) {
            rewriteParamsAndReturnTypeOfFunction(funcNode, funcEnv);
        }
        funcNode.body = rewrite(funcNode.body, funcEnv);
        result = funcNode;
    }

    public void rewriteParamsAndReturnTypeOfFunction(BLangFunction funcNode, SymbolEnv funcEnv) {
        for (BLangSimpleVariable bLangSimpleVariable : funcNode.requiredParams) {
            rewrite(bLangSimpleVariable, funcEnv);
        }

        if (funcNode.restParam != null) {
            funcNode.restParam = rewrite(funcNode.restParam, funcEnv);
        }

        if (funcNode.returnTypeNode != null && funcNode.returnTypeNode.getKind() != null) {
            funcNode.returnTypeNode = rewrite(funcNode.returnTypeNode, funcEnv);
        }
    }

    @Override
    public void visit(BLangBlockFunctionBody body) {
        SymbolEnv blockEnv = SymbolEnv.createFuncBodyEnv(body, env);
        body.stmts = rewriteStmt(body.stmts, blockEnv);
        result = body;
    }

    @Override
    public void visit(BLangRawTemplateLiteral rawTemplateLiteral) {
        rewriteExprs(rawTemplateLiteral.strings);
        rewriteExprs(rawTemplateLiteral.insertions);
        result = rawTemplateLiteral;
    }

    @Override
    public void visit(BLangExprFunctionBody exprBody) {
        exprBody.expr = rewriteExpr(exprBody.expr);
        result = exprBody;
    }

    @Override
    public void visit(BLangResourceFunction resourceFunction) {
        visit((BLangFunction) resourceFunction);
    }

    @Override
    public void visit(BLangExternalFunctionBody body) {
        result = body;
    }

    @Override
    public void visit(BLangBlockStmt blockNode) {
        SymbolEnv blockEnv = SymbolEnv.createBlockEnv(blockNode, env);
        blockNode.stmts = rewriteStmt(blockNode.stmts, blockEnv);

        result = blockNode;
    }

    @Override
    public void visit(BLangService serviceNode) {
        result = serviceNode;
    }

    @Override
    public void visit(BLangSimpleVariableDef varDefNode) {
        varDefNode.var = rewrite(varDefNode.var, env);
        result = varDefNode;
    }

    @Override
    public void visit(BLangReturn returnNode) {
        if (returnNode.expr != null) {
            returnNode.expr = rewriteExpr(returnNode.expr);
        }
        result = returnNode;
    }

    @Override
    public void visit(BLangTypeDefinition typeDef) {
        typeDef.typeNode = rewrite(typeDef.typeNode, env);
        result = typeDef;
    }

    @Override
    public void visit(BLangIntersectionTypeNode intersectionTypeNode) {
        List<BLangType> rewrittenConstituents = new ArrayList<>();
        for (BLangType constituentTypeNode : intersectionTypeNode.constituentTypeNodes) {
            rewrittenConstituents.add(rewrite(constituentTypeNode, env));
        }
        intersectionTypeNode.constituentTypeNodes = rewrittenConstituents;
        result = intersectionTypeNode;
    }

    @Override
    public void visit(BLangClassDefinition classDefinition) {
        SymbolEnv classEnv = SymbolEnv.createClassEnv(classDefinition, classDefinition.symbol.scope, env);
        for (BLangSimpleVariable bLangSimpleVariable : classDefinition.fields) {
            bLangSimpleVariable.typeNode = rewrite(bLangSimpleVariable.typeNode, classEnv);
            bLangSimpleVariable.expr = rewrite(bLangSimpleVariable.expr, classEnv);
        }
        result = classDefinition;
    }

    @Override
    public void visit(BLangObjectTypeNode objectTypeNode) {
        for (BLangSimpleVariable field : objectTypeNode.fields) {
            rewrite(field, env);
        }
        result = objectTypeNode;
    }

    @Override
    public void visit(BLangObjectConstructorExpression objectConstructorExpression) {
        objectConstructorExpression.typeInit = rewriteExpr(objectConstructorExpression.typeInit);
        result = objectConstructorExpression;
    }

    @Override
    public void visit(BLangRecordTypeNode recordTypeNode) {
        BTypeSymbol typeSymbol = recordTypeNode.getBType().tsymbol;
        BSymbol owner = typeSymbol.owner;
        desugarFieldAnnotations(owner, typeSymbol, recordTypeNode.fields, recordTypeNode.pos);
        for (BLangSimpleVariable field : recordTypeNode.fields) {
            rewrite(field, recordTypeNode.typeDefEnv);
        }
        recordTypeNode.restFieldType = rewrite(recordTypeNode.restFieldType, env);
        generateClosuresForNonOverriddenFields(recordTypeNode);
        result = recordTypeNode;
    }

    private void generateClosuresForNonOverriddenFields(BLangRecordTypeNode recordTypeNode) {
        if (recordTypeNode.typeRefs.isEmpty()) {
            return;
        }
        List<String> fieldNames = getFieldNames(recordTypeNode.fields);
        BTypeSymbol typeSymbol = recordTypeNode.getBType().tsymbol;
        String typeName = recordTypeNode.symbol.name.value;
        for (BLangType type : recordTypeNode.typeRefs) {
            BType bType = type.getBType();
            BRecordType recordType = (BRecordType) Types.getReferredType(bType);
            Map<String, BInvokableSymbol> defaultValuesOfTypeRef =
                    ((BRecordTypeSymbol) recordType.tsymbol).defaultValues;
            for (Map.Entry<String, BInvokableSymbol> defaultValue : defaultValuesOfTypeRef.entrySet()) {
                String name = defaultValue.getKey();
                if (fieldNames.contains(name)) {
                    continue;
                }
                BInvokableSymbol symbol = defaultValue.getValue();
                BLangInvocation invocation = getInvocation(symbol);
                String closureName = RECORD_DELIMITER + typeName + RECORD_DELIMITER + name;
                generateClosureForDefaultValues(closureName, name, invocation, symbol.retType, typeSymbol);
            }
        }
    }

    private List<String> getFieldNames(List<BLangSimpleVariable> fields) {
        List<String> fieldNames = new ArrayList<>();
        for (BLangSimpleVariable field : fields) {
            fieldNames.add(field.name.getValue());
        }
        return fieldNames;
    }

    @Override
    public void visit(BLangTupleTypeNode tupleTypeNode) {
        BTypeSymbol typeSymbol = tupleTypeNode.getBType().tsymbol;
        BSymbol owner = typeSymbol.owner;
        desugarFieldAnnotations(owner, typeSymbol, tupleTypeNode.members, tupleTypeNode.pos);
        List<BLangSimpleVariable> rewrittenMembers = new ArrayList<>();
        tupleTypeNode.members.forEach(member -> rewrittenMembers.add(rewrite(member, env)));
        tupleTypeNode.members = rewrittenMembers;
        tupleTypeNode.restParamType = rewrite(tupleTypeNode.restParamType, env);
        result = tupleTypeNode;
    }

    private void desugarFieldAnnotations(BSymbol owner, BTypeSymbol typeSymbol, List<BLangSimpleVariable> fields,
                                         Location pos) {
        if (owner.getKind() == SymbolKind.PACKAGE && typeSymbol.name != Names.EMPTY) {
            return;
        }
        owner = getOwner(env);
        BLangLambdaFunction lambdaFunction = annotationDesugar.defineFieldAnnotations(fields, pos, env.enclPkg, env,
                                                                                      typeSymbol.pkgID, owner);
        if (lambdaFunction != null) {
            boolean isPackageLevelAnnotationClosure = owner.getKind() == SymbolKind.PACKAGE;
            BInvokableSymbol invokableSymbol = createSimpleVariable(lambdaFunction.function, lambdaFunction,
                                                                    isPackageLevelAnnotationClosure);
            typeSymbol.annotations = createSimpleVariable(invokableSymbol, isPackageLevelAnnotationClosure);
        }
    }

    @Override
    public void visit(BLangFiniteTypeNode finiteTypeNode) {
        finiteTypeNode.valueSpace.forEach(param -> rewrite(param, env));
        result = finiteTypeNode;
    }

    @Override
    public void visit(BLangArrayType arrayType) {
        arrayType.elemtype = rewrite(arrayType.elemtype, env);
        result = arrayType;
    }

    @Override
    public void visit(BLangUserDefinedType userDefinedType) {
        result = userDefinedType;
    }

    @Override
    public void visit(BLangUnionTypeNode unionTypeNode) {
        List<BLangType> rewrittenMembers = new ArrayList<>();
        unionTypeNode.memberTypeNodes.forEach(typeNode -> rewrittenMembers.add(rewrite(typeNode, env)));
        unionTypeNode.memberTypeNodes = rewrittenMembers;
        result = unionTypeNode;
    }

    @Override
    public void visit(BLangValueType valueType) {
        result = valueType;
    }

    @Override
    public void visit(BLangBuiltInRefTypeNode builtInRefTypeNode) {
        result = builtInRefTypeNode;
    }

    @Override
    public void visit(BLangStreamType streamType) {
        streamType.constraint = rewrite(streamType.constraint, env);
        streamType.error = rewrite(streamType.error, env);
        result = streamType;
    }

    @Override
    public void visit(BLangConstrainedType constrainedType) {
        constrainedType.constraint = rewrite(constrainedType.constraint, env);
        result = constrainedType;
    }

    @Override
    public void visit(BLangErrorType errorType) {
        errorType.detailType = rewrite(errorType.detailType, env);
        result = errorType;
    }

    @Override
    public void visit(BLangTableTypeNode tableTypeNode) {
        tableTypeNode.constraint = rewrite(tableTypeNode.constraint, env);
        tableTypeNode.tableKeyTypeConstraint = rewrite(tableTypeNode.tableKeyTypeConstraint, env);
        result = tableTypeNode;
    }

    @Override
    public void visit(BLangInvocation.BLangResourceAccessInvocation resourceAccessInvocation) {
        result = resourceAccessInvocation;
    }

    @Override
    public void visit(BLangTableKeyTypeConstraint keyTypeConstraint) {
        keyTypeConstraint.keyType = rewrite(keyTypeConstraint.keyType, env);
        result = keyTypeConstraint;
    }

    @Override
    public void visit(BLangFunctionTypeNode functionTypeNode) {
        SymbolEnv funcEnv = SymbolEnv.createTypeEnv(functionTypeNode, functionTypeNode.getBType().tsymbol.scope, env);
        for (BLangSimpleVariable param : functionTypeNode.params) {
            rewrite(param, funcEnv);
        }
        if (functionTypeNode.restParam != null) {
            functionTypeNode.restParam.typeNode = rewrite(functionTypeNode.restParam.typeNode, env);
        }
        if (functionTypeNode.returnTypeNode != null) {
            functionTypeNode.returnTypeNode = rewrite(functionTypeNode.returnTypeNode, env);
        }

        result = functionTypeNode;
    }

    @Override
    public void visit(BLangSimpleVariable varNode) {
        if (varNode.typeNode != null && varNode.typeNode.getKind() != null) {
            varNode.typeNode = rewrite(varNode.typeNode, env);
        }
        if (Symbols.isFlagOn(varNode.symbol.flags, Flags.FIELD) && varNode.symbol.isDefaultable) {
            String closureName = generateName(varNode.symbol.name.value, env.node);
            varNode.pos = null;
            varNode.expr.pos = null;
            generateClosureForDefaultValues(closureName, varNode.name.value, varNode);
            result = varNode;
            return;
        }

        if (Symbols.isFlagOn(varNode.symbol.flags, Flags.DEFAULTABLE_PARAM)) {
            String closureName = generateName(varNode.symbol.name.value, env.node);
            generateClosureForDefaultValues(closureName, varNode.name.value, varNode);
        } else {
            rewriteExpr(varNode.expr);
        }
        result = varNode;
    }

    private BSymbol getOwner(SymbolEnv symbolEnv) {
        while (symbolEnv.node.getKind() != NodeKind.PACKAGE) {
            NodeKind kind = symbolEnv.node.getKind();
            if (kind != NodeKind.BLOCK_FUNCTION_BODY && kind != NodeKind.BLOCK) {
                symbolEnv = symbolEnv.enclEnv;
                continue;
            }
            return symbolEnv.enclInvokable.symbol;
        }
        return symbolEnv.enclPkg.symbol;
    }

    private void generateClosureForDefaultValues(String closureName, String paramName, BLangSimpleVariable varNode) {
        generateClosureForDefaultValues(closureName, paramName, varNode.expr, varNode.getBType(),
                                        env.node.getBType().tsymbol);
    }

    private void generateClosureForDefaultValues(String closureName, String paramName, BLangExpression expr,
                                                 BType returnType, BTypeSymbol symbol) {
        BSymbol owner = getOwner(env);
        BLangFunction function = createFunction(closureName, expr.pos, owner.pkgID, owner, returnType);
        BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(function.pos, (BLangBlockFunctionBody) function.body);
        returnStmt.expr = types.addConversionExprIfRequired(expr, function.returnTypeNode.getBType());
        BLangLambdaFunction lambdaFunction = createLambdaFunction(function);
        BInvokableSymbol varSymbol = createSimpleVariable(function, lambdaFunction, false);
        if (symbol.getKind() == SymbolKind.INVOKABLE_TYPE) {
            BInvokableTypeSymbol invokableTypeSymbol = (BInvokableTypeSymbol) symbol;
            updateFunctionParams(function, invokableTypeSymbol.params, paramName);
            invokableTypeSymbol.defaultValues.put(Utils.unescapeBallerina(paramName), varSymbol);
        } else {
            ((BRecordTypeSymbol) symbol).defaultValues.put(Utils.unescapeBallerina(paramName), varSymbol);
            lambdaFunction.function.flagSet.add(Flag.RECORD);
        }
        env.enclPkg.symbol.scope.define(function.symbol.name, function.symbol);
        env.enclPkg.functions.add(function);
        env.enclPkg.topLevelNodes.add(function);
        rewrite(lambdaFunction, env);
    }

    private void updateFunctionParams(BLangFunction funcNode, List<BVarSymbol> params, String paramName) {
        // Add params to the required param list if there are any.
        BInvokableSymbol funcSymbol = funcNode.symbol;
        Location pos = funcSymbol.pos;
        for (BVarSymbol symbol : params) {
            Name symbolName = symbol.name;
            if (paramName.equals(symbolName.value)) {
                break;
            }
            BType type = symbol.type;
            BVarSymbol varSymbol = new BVarSymbol(Flags.REQUIRED_PARAM, symbolName, symbol.pkgID, type, funcSymbol, pos,
                                                  VIRTUAL);
            funcSymbol.scope.define(symbolName, varSymbol);
            funcSymbol.params.add(varSymbol);
            BInvokableType funcType = (BInvokableType) funcSymbol.type;
            funcType.addParamType(varSymbol.type);
            funcNode.requiredParams.add(ASTBuilderUtil.createVariable(pos, symbolName.value, type, null,
                    varSymbol));
        }
    }

    BLangLambdaFunction createLambdaFunction(BLangFunction function) {
        BLangLambdaFunction lambdaFunction = (BLangLambdaFunction) TreeBuilder.createLambdaFunctionNode();
        lambdaFunction.function = function;
        lambdaFunction.setBType(function.getBType());
        lambdaFunction.pos = function.pos;
        return lambdaFunction;
    }

    public BInvokableSymbol createSimpleVariable(BLangFunction function, BLangLambdaFunction lambdaFunction,
                                                 boolean isAnnotationClosure) {
        BInvokableSymbol invokableSymbol = function.symbol;
        BType type = function.getBType();
        Location pos = function.pos;
        Name name = invokableSymbol.name;
        BInvokableSymbol varSymbol = new BInvokableSymbol(SymTag.VARIABLE, 0, name, invokableSymbol.pkgID, type,
                                                          invokableSymbol.owner, pos, VIRTUAL);
        varSymbol.params = invokableSymbol.params;
        varSymbol.restParam = invokableSymbol.restParam;
        varSymbol.retType = invokableSymbol.retType;
        BLangSimpleVariableDef variableDef = createSimpleVariableDef(pos, name.value, type, lambdaFunction, varSymbol);
        addToQueue(variableDef, isAnnotationClosure);
        return varSymbol;
    }

    private BVarSymbol createSimpleVariable(BInvokableSymbol invokableSymbol, boolean isAnnotationClosure) {
        BType type = invokableSymbol.retType;
        Location pos = invokableSymbol.pos;
        Name name = invokableSymbol.name;
        BVarSymbol varSymbol = new BVarSymbol(0, name, invokableSymbol.originalName, invokableSymbol.pkgID, type,
                                              invokableSymbol.owner, pos, VIRTUAL);
        BLangSimpleVariableDef variableDef = createSimpleVariableDef(pos, name.value, type,
                                                                     getInvocation(invokableSymbol), varSymbol);
        addToQueue(variableDef, isAnnotationClosure);
        return varSymbol;
    }

    private BLangSimpleVariableDef createSimpleVariableDef(Location pos, String name, BType type, BLangExpression expr,
                                                           BVarSymbol varSymbol) {
        BLangSimpleVariable simpleVariable = ASTBuilderUtil.createVariable(pos, name, type, expr, varSymbol);
        BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDef(pos);
        variableDef.var = simpleVariable;
        variableDef.setBType(type);
        return variableDef;
    }

    private void addToQueue(BLangSimpleVariableDef variableDef, boolean isAnnotationClosure) {
        if (isAnnotationClosure) {
            annotationClosureReferences.add(variableDef);
            return;
        }
        queue.add(variableDef);
    }

    private BLangInvocation getInvocation(BInvokableSymbol symbol) {
        BLangInvocation funcInvocation = (BLangInvocation) TreeBuilder.createInvocationNode();
        funcInvocation.setBType(symbol.retType);
        funcInvocation.symbol = symbol;
        funcInvocation.name = ASTBuilderUtil.createIdentifier(symbol.pos, symbol.name.value);
        funcInvocation.functionPointerInvocation = true;
        return funcInvocation;
    }

    private BLangFunction createFunction(String funcName, Location pos, PackageID pkgID, BSymbol owner, BType bType) {
        BLangFunction function = ASTBuilderUtil.createFunction(pos, funcName);
        function.flagSet.add(Flag.PUBLIC);
        BInvokableTypeSymbol invokableTypeSymbol = Symbols.createInvokableTypeSymbol(SymTag.FUNCTION_TYPE, Flags.PUBLIC,
                                                                                     pkgID, bType, owner, pos, VIRTUAL);
        function.setBType(new BInvokableType(symTable.typeEnv(), List.of(), bType, invokableTypeSymbol));

        BLangBuiltInRefTypeNode typeNode = (BLangBuiltInRefTypeNode) TreeBuilder.createBuiltInReferenceTypeNode();
        typeNode.setBType(bType);
        typeNode.typeKind = bType.getKind();
        typeNode.pos = pos;
        function.returnTypeNode = typeNode;

        BInvokableSymbol functionSymbol = new BInvokableSymbol(SymTag.FUNCTION, Flags.PUBLIC, new Name(funcName), pkgID,
                                                               function.getBType(), owner, pos, VIRTUAL);
        functionSymbol.bodyExist = true;
        functionSymbol.kind = SymbolKind.FUNCTION;
        functionSymbol.retType = function.returnTypeNode.getBType();
        functionSymbol.scope = new Scope(functionSymbol);
        functionSymbol.schedulerPolicy = SchedulerPolicy.ANY;
        function.symbol = functionSymbol;

        return function;
    }

    private String generateName(String name, BLangNode parent) {
        if (parent == null) {
            return DOLLAR + name;
        }
        return switch (parent.getKind()) {
            case CLASS_DEFN ->
                    generateName(((BLangClassDefinition) parent).name.getValue() + UNDERSCORE + name, parent.parent);
            case FUNCTION -> generateName(((BLangFunction) parent).symbol.name.value.replace(".", UNDERSCORE)
                    + UNDERSCORE + name, parent.parent);
            case RESOURCE_FUNC ->
                    generateName(((BLangResourceFunction) parent).name.value + UNDERSCORE + name, parent.parent);
            case VARIABLE ->
                    generateName(((BLangSimpleVariable) parent).name.getValue() + UNDERSCORE + name, parent.parent);
            case TYPE_DEFINITION ->
                    generateName(((BLangTypeDefinition) parent).name.getValue() + UNDERSCORE + name, parent.parent);
            case RECORD_TYPE -> generateName(RECORD_DELIMITER + ((BLangRecordTypeNode) parent).symbol.name.getValue()
                    + RECORD_DELIMITER + name, parent.parent);
            default -> generateName(name, parent.parent);
        };
    }
    @Override
    public void visit(BLangTupleVariable varNode) {
        rewrite(varNode.restVariable, env);
        result = varNode;
    }

    @Override
    public void visit(BLangRecordVariable varNode) {
        varNode.expr = rewriteExpr(varNode.expr);
        result = varNode;
    }

    @Override
    public void visit(BLangErrorVariable varNode) {
        varNode.expr = rewriteExpr(varNode.expr);
        result = varNode;
    }

    @Override
    public void visit(BLangTupleVariableDef varDefNode) {
        varDefNode.var = rewrite(varDefNode.var, env);
        result = varDefNode;
    }

    @Override
    public void visit(BLangRecordVariableDef varDefNode) {
        varDefNode.var = rewrite(varDefNode.var, env);
        result = varDefNode;
    }

    @Override
    public void visit(BLangErrorVariableDef varDefNode) {
        varDefNode.errorVariable = rewrite(varDefNode.errorVariable, env);
        result = varDefNode;
    }

    @Override
    public void visit(BLangAssignment assignNode) {
        assignNode.varRef = rewriteExpr(assignNode.varRef);
        assignNode.expr = rewriteExpr(assignNode.expr);
        result = assignNode;
    }

    @Override
    public void visit(BLangTupleDestructure tupleDestructure) {
        result = tupleDestructure;
    }

    @Override
    public void visit(BLangRecordDestructure recordDestructure) {
        result = recordDestructure;
    }

    @Override
    public void visit(BLangErrorDestructure errorDestructure) {
        result = errorDestructure;
    }

    @Override
    public void visit(BLangRetry retryNode) {
        retryNode.retryBody = rewrite(retryNode.retryBody, env);
        result = retryNode;
    }

    @Override
    public void visit(BLangRetryTransaction retryTransaction) {
        retryTransaction.transaction = rewrite(retryTransaction.transaction, env);
        result = retryTransaction;
    }

    @Override
    public void visit(BLangContinue nextNode) {
        result = nextNode;
    }

    @Override
    public void visit(BLangBreak breakNode) {
        result = breakNode;
    }

    @Override
    public void visit(BLangPanic panicNode) {
        panicNode.expr = rewriteExpr(panicNode.expr);
        result = panicNode;
    }

    @Override
    public void visit(BLangDo doNode) {
        doNode.body = rewrite(doNode.body, env);
        result = doNode;
    }

    @Override
    public void visit(BLangXMLNSStatement xmlnsStmtNode) {
        xmlnsStmtNode.xmlnsDecl = rewrite(xmlnsStmtNode.xmlnsDecl, env);
        result = xmlnsStmtNode;
    }

    @Override
    public void visit(BLangXMLNS xmlnsNode) {
        xmlnsNode.namespaceURI = rewriteExpr(xmlnsNode.namespaceURI);
        result = xmlnsNode;
    }

    @Override
    public void visit(BLangExpressionStmt exprStmtNode) {
        exprStmtNode.expr = rewriteExpr(exprStmtNode.expr);
        result = exprStmtNode;
    }

    @Override
    public void visit(BLangFail failNode) {
        if (failNode.exprStmt != null) {
            failNode.exprStmt = rewrite(failNode.exprStmt, env);
        }
        result = failNode;
    }

    @Override
    public void visit(BLangIf ifNode) {
        ifNode.expr = rewriteExpr(ifNode.expr);
        ifNode.body = rewrite(ifNode.body, env);
        ifNode.elseStmt = rewrite(ifNode.elseStmt, env);
        result = ifNode;
    }

    @Override
    public void visit(BLangForeach foreach) {
        result = foreach;
    }

    @Override
    public void visit(BLangWhile whileNode) {
        whileNode.expr = rewriteExpr(whileNode.expr);
        whileNode.body = rewrite(whileNode.body, env);
        result = whileNode;
    }

    @Override
    public void visit(BLangLock lockNode) {
        lockNode.body = rewrite(lockNode.body, env);
        result = lockNode;
    }

    @Override
    public void visit(BLangLock.BLangLockStmt lockNode) {
        result = lockNode;
    }

    @Override
    public void visit(BLangLock.BLangUnLockStmt unLockNode) {
        result = unLockNode;
    }

    @Override
    public void visit(BLangTransaction transactionNode) {
        transactionNode.transactionBody = rewrite(transactionNode.transactionBody, env);
        result = transactionNode;
    }

    @Override
    public void visit(BLangRollback rollbackNode) {
        rollbackNode.expr = rewriteExpr(rollbackNode.expr);
        result = rollbackNode;
    }

    @Override
    public void visit(BLangTransactionalExpr transactionalExpr) {
        result = transactionalExpr;
    }

    @Override
    public void visit(BLangCommitExpr commitExpr) {
        result = commitExpr;
    }

    @Override
    public void visit(BLangForkJoin forkJoin) {
        result = forkJoin;
    }

    @Override
    public void visit(BLangLiteral literalExpr) {
        result = literalExpr;
    }

    @Override
    public void visit(BLangListConstructorExpr listConstructorExpr) {
        listConstructorExpr.exprs = rewriteExprs(listConstructorExpr.exprs);
        result = listConstructorExpr;
    }

    @Override
    public void visit(BLangTableConstructorExpr tableConstructorExpr) {
        rewriteExprs(tableConstructorExpr.recordLiteralList);
        result = tableConstructorExpr;
    }

    @Override
    public void visit(BLangListConstructorExpr.BLangJSONArrayLiteral jsonArrayLiteral) {
        jsonArrayLiteral.exprs = rewriteExprs(jsonArrayLiteral.exprs);
        result = jsonArrayLiteral;
    }

    @Override
    public void visit(BLangListConstructorExpr.BLangTupleLiteral tupleLiteral) {
        tupleLiteral.exprs = rewriteExprs(tupleLiteral.exprs);
        result = tupleLiteral;
    }

    @Override
    public void visit(BLangListConstructorExpr.BLangArrayLiteral arrayLiteral) {
        arrayLiteral.exprs = rewriteExprs(arrayLiteral.exprs);
        result = arrayLiteral;
    }

    @Override
    public void visit(BLangRecordLiteral recordLiteral) {
        for (RecordLiteralNode.RecordField field : recordLiteral.fields) {
            if (field.isKeyValueField()) {
                BLangRecordLiteral.BLangRecordKeyValueField keyValueField =
                                                                    (BLangRecordLiteral.BLangRecordKeyValueField) field;
                keyValueField.key.expr = rewriteExpr(keyValueField.key.expr);
                keyValueField.valueExpr = rewriteExpr(keyValueField.valueExpr);
            } else if (field.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                BLangSimpleVarRef varRefField = (BLangSimpleVarRef) field;
                rewriteExpr(varRefField);
            } else {
                BLangRecordLiteral.BLangRecordSpreadOperatorField spreadOpField =
                                                              (BLangRecordLiteral.BLangRecordSpreadOperatorField) field;
                spreadOpField.expr = rewriteExpr(spreadOpField.expr);
            }
        }
        result = recordLiteral;
    }

    @Override
    public void visit(BLangSimpleVarRef varRefExpr) {
        BSymbol varRefSym = varRefExpr.symbol;
        if (varRefSym != null) {
            boolean isMemberOfFunction = Symbols.isFlagOn(varRefSym.flags, Flags.REQUIRED_PARAM) ||
                                         Symbols.isFlagOn(varRefSym.flags, Flags.DEFAULTABLE_PARAM);
            if (isMemberOfFunction) {
                updateFunctionParamsOfClosures(env, varRefExpr);
            }
        }
        BLangInvokableNode encInvokable = env.enclInvokable;
        BSymbol symbol = varRefExpr.symbol;
        if (varRefSym == null || encInvokable == null || (symbol.tag & SymTag.VARIABLE) != SymTag.VARIABLE) {
            result = varRefExpr;
            return;
        }
        updateClosureVariable((BVarSymbol) symbol, encInvokable, varRefExpr.pos);
        result = varRefExpr;
    }

    private void updateFunctionParamsOfClosures(SymbolEnv symbolEnv, BLangSimpleVarRef varRefExpr) {
        BLangFunction closure = null;
        while (symbolEnv != null && symbolEnv.node.getKind() != NodeKind.PACKAGE) {

            if (symbolEnv.node.getKind() != NodeKind.FUNCTION) {
                symbolEnv = symbolEnv.enclEnv;
                continue;
            }

            BLangFunction bLangFunction = (BLangFunction) symbolEnv.node;

            BLangInvokableNode enclInvokable = symbolEnv.enclInvokable;
            if (enclInvokable.flagSet.contains(Flag.LAMBDA) && !enclInvokable.flagSet.contains(Flag.QUERY_LAMBDA) &&
                                                                !enclInvokable.flagSet.contains(Flag.ANONYMOUS)) {
                closure = bLangFunction;
            }

            symbolEnv = symbolEnv.enclEnv;
        }
        if (closure != null) {
            updateFunctionParams(closure, varRefExpr);
        }
    }

    private void updateFunctionParams(BLangFunction funcNode, BLangSimpleVarRef varRefExpr) {
        BInvokableSymbol funcSymbol = funcNode.symbol;
        for (BVarSymbol varSymbol : funcSymbol.params) {
            if (varSymbol.name.value.equals(varRefExpr.symbol.name.value)) {
                varRefExpr.symbol = varSymbol;
                return;
            }
        }
    }

    private SymbolEnv findEnclosingInvokableEnv(SymbolEnv env, BLangInvokableNode encInvokable) {
        if (env.enclEnv.node != null && env.enclEnv.node.getKind() == NodeKind.ARROW_EXPR) {
            return env.enclEnv;
        }

        if (env.enclEnv.node != null && (env.enclEnv.node.getKind() == NodeKind.ON_FAIL)) {
            return env.enclEnv;
        }

        if (env.enclInvokable != null && env.enclInvokable == encInvokable) {
            return findEnclosingInvokableEnv(env.enclEnv, encInvokable);
        }
        return env;
    }

    @Override
    public void visit(BLangFieldBasedAccess fieldAccessExpr) {
        fieldAccessExpr.expr = rewriteExpr(fieldAccessExpr.expr);

        result = fieldAccessExpr;
    }

    @Override
    public void visit(BLangIndexBasedAccess indexAccessExpr) {
        indexAccessExpr.indexExpr = rewriteExpr(indexAccessExpr.indexExpr);
        indexAccessExpr.expr = rewriteExpr(indexAccessExpr.expr);
        result = indexAccessExpr;
    }

    @Override
    public void visit(BLangCompoundAssignment compoundAssignment) {
        result = compoundAssignment;
    }

    @Override
    public void visit(BLangInvocation invocation) {
        rewriteInvocationExpr(invocation);
        BLangInvokableNode encInvokable = env.enclInvokable;
        if (encInvokable == null || !invocation.functionPointerInvocation ||
                !env.enclPkg.packageID.equals(invocation.symbol.pkgID)) {
            return;
        }
        updateClosureVariable((BVarSymbol) invocation.symbol, encInvokable, invocation.pos);
    }

    public void rewriteInvocationExpr(BLangInvocation invocation) {
        invocation.requiredArgs = rewriteExprs(invocation.requiredArgs);
        result = invocation;

    }

    @Override
    public void visit(BLangQueryAction queryAction) {
        result = queryAction;
    }

    @Override
    public void visit(BLangCheckPanickedExpr checkedExpr) {
        result = checkedExpr;
    }

    @Override
    public void visit(BLangErrorConstructorExpr errorConstructorExpr) {
        rewriteExprs(errorConstructorExpr.positionalArgs);
        errorConstructorExpr.errorDetail = rewriteExpr(errorConstructorExpr.errorDetail);
        result = errorConstructorExpr;
    }

    @Override
    public void visit(BLangTypeInit typeInitExpr) {
        typeInitExpr.initInvocation = rewriteExpr(typeInitExpr.initInvocation);
        result = typeInitExpr;
    }

    @Override
    public void visit(BLangTernaryExpr ternaryExpr) {
        ternaryExpr.expr = rewriteExpr(ternaryExpr.expr);
        ternaryExpr.thenExpr = rewriteExpr(ternaryExpr.thenExpr);
        ternaryExpr.elseExpr = rewriteExpr(ternaryExpr.elseExpr);
        result = ternaryExpr;
    }

    @Override
    public void visit(BLangWaitExpr waitExpr) {
        List<BLangExpression> exprList = new ArrayList<>();
        waitExpr.exprList.forEach(expression -> exprList.add(rewriteExpr(expression)));
        waitExpr.exprList = exprList;
        result = waitExpr;
    }

    @Override
    public void visit(BLangWaitForAllExpr waitExpr) {
        result = waitExpr;
    }

    @Override
    public void visit(BLangTrapExpr trapExpr) {
        trapExpr.expr = rewriteExpr(trapExpr.expr);
        result = trapExpr;
    }

    @Override
    public void visit(BLangBinaryExpr binaryExpr) {
        binaryExpr.lhsExpr = rewriteExpr(binaryExpr.lhsExpr);
        binaryExpr.rhsExpr = rewriteExpr(binaryExpr.rhsExpr);
        result = binaryExpr;
    }

    @Override
    public void visit(BLangElvisExpr elvisExpr) {
        result = elvisExpr;
    }

    @Override
    public void visit(BLangGroupExpr groupExpr) {
        groupExpr.expression = rewriteExpr(groupExpr.expression);
        result = groupExpr;
    }

    @Override
    public void visit(BLangUnaryExpr unaryExpr) {
        unaryExpr.expr = rewriteExpr(unaryExpr.expr);
        result = unaryExpr;
    }

    @Override
    public void visit(BLangTypeConversionExpr conversionExpr) {
        conversionExpr.expr = rewriteExpr(conversionExpr.expr);
        conversionExpr.typeNode = rewrite(conversionExpr.typeNode, env);
        result = conversionExpr;
    }

    @Override
    public void visit(BLangLambdaFunction bLangLambdaFunction) {
        bLangLambdaFunction.capturedClosureEnv = env;
        bLangLambdaFunction.function = rewrite(bLangLambdaFunction.function, env);
        result = bLangLambdaFunction;
    }

    @Override
    public void visit(BLangArrowFunction bLangArrowFunction) {
        result = bLangArrowFunction;
    }

    @Override
    public void visit(BLangXMLQName xmlQName) {
        result = xmlQName;
    }

    @Override
    public void visit(BLangXMLAttribute xmlAttribute) {
        xmlAttribute.name = rewriteExpr(xmlAttribute.name);
        xmlAttribute.value = rewriteExpr(xmlAttribute.value);
        result = xmlAttribute;
    }

    @Override
    public void visit(BLangXMLElementLiteral xmlElementLiteral) {
        xmlElementLiteral.startTagName = rewriteExpr(xmlElementLiteral.startTagName);
        xmlElementLiteral.endTagName = rewriteExpr(xmlElementLiteral.endTagName);
        xmlElementLiteral.modifiedChildren = rewriteExprs(xmlElementLiteral.modifiedChildren);
        xmlElementLiteral.attributes = rewriteExprs(xmlElementLiteral.attributes);
        result = xmlElementLiteral;
    }

    @Override
    public void visit(BLangXMLTextLiteral xmlTextLiteral) {
        xmlTextLiteral.textFragments.forEach(this::rewriteExpr);
        xmlTextLiteral.concatExpr = rewriteExpr(xmlTextLiteral.concatExpr);
        result = xmlTextLiteral;
    }

    @Override
    public void visit(BLangXMLCommentLiteral xmlCommentLiteral) {
        xmlCommentLiteral.textFragments.forEach(this::rewriteExpr);
        result = xmlCommentLiteral;
    }

    @Override
    public void visit(BLangXMLProcInsLiteral xmlProcInsLiteral) {
        xmlProcInsLiteral.target = rewriteExpr(xmlProcInsLiteral.target);
        xmlProcInsLiteral.dataFragments.forEach(this::rewriteExpr);
        result = xmlProcInsLiteral;
    }

    @Override
    public void visit(BLangXMLQuotedString xmlQuotedString) {
        xmlQuotedString.textFragments.forEach(this::rewriteExpr);
        result = xmlQuotedString;
    }

    @Override
    public void visit(BLangStringTemplateLiteral stringTemplateLiteral) {
        stringTemplateLiteral.exprs.forEach(this::rewriteExpr);
        result = stringTemplateLiteral;
    }

    @Override
    public void visit(BLangWorkerAsyncSendExpr asyncSendExpr) {
        asyncSendExpr.expr = rewriteExpr(asyncSendExpr.expr);
        result = asyncSendExpr;
    }

    @Override
    public void visit(BLangWorkerSyncSendExpr syncSendExpr) {
        syncSendExpr.expr = rewriteExpr(syncSendExpr.expr);
        result = syncSendExpr;
    }

    @Override
    public void visit(BLangAlternateWorkerReceive alternateWorkerReceive) {
        result = alternateWorkerReceive;
    }

    @Override
    public void visit(BLangMultipleWorkerReceive multipleWorkerReceive) {
        result = multipleWorkerReceive;
    }

    @Override
    public void visit(BLangWorkerReceive workerReceiveNode) {
        result = workerReceiveNode;
    }

    @Override
    public void visit(BLangWorkerFlushExpr workerFlushExpr) {
        result = workerFlushExpr;
    }

    @Override
    public void visit(BLangSimpleVarRef.BLangLocalVarRef localVarRef) {
        BLangInvokableNode encInvokable = env.enclInvokable;
        BSymbol symbol = localVarRef.symbol;
        updateClosureVariable((BVarSymbol) symbol, encInvokable, localVarRef.pos);
        result = localVarRef;
    }

    private void updateClosureVariable(BVarSymbol varSymbol, BLangInvokableNode encInvokable, Location pos) {
        Set<Flag> flagSet = encInvokable.flagSet;
        boolean isClosure = !flagSet.contains(Flag.QUERY_LAMBDA) && flagSet.contains(Flag.LAMBDA) &&
                            !flagSet.contains(Flag.ATTACHED) && varSymbol.owner.tag != SymTag.PACKAGE &&
                            !isInParameterList(varSymbol, encInvokable.requiredParams);
        if (!varSymbol.closure && isClosure) {
            SymbolEnv encInvokableEnv = findEnclosingInvokableEnv(env, encInvokable);
            BSymbol resolvedSymbol = symResolver.lookupClosureVarSymbol(encInvokableEnv, varSymbol);
            if (resolvedSymbol != symTable.notFoundSymbol) {
                varSymbol.closure = true;
                ((BLangFunction) encInvokable).closureVarSymbols.add(new ClosureVarSymbol(varSymbol, pos));
            }
        }
    }

    @Override
    public void visit(BLangIgnoreExpr ignoreExpr) {
        result = ignoreExpr;
    }

    @Override
    public void visit(BLangDynamicArgExpr dynamicParamExpr) {
        dynamicParamExpr.condition = rewriteExpr(dynamicParamExpr.condition);
        dynamicParamExpr.conditionalArgument = rewriteExpr(dynamicParamExpr.conditionalArgument);
        result = dynamicParamExpr;
    }

    @Override
    public void visit(BLangSimpleVarRef.BLangPackageVarRef packageVarRef) {
        result = packageVarRef;
    }

    @Override
    public void visit(BLangConstRef constRef) {
        result = constRef;
    }

    @Override
    public void visit(BLangSimpleVarRef.BLangFunctionVarRef functionVarRef) {
        result = functionVarRef;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangStructFieldAccessExpr fieldAccessExpr) {
        fieldAccessExpr.indexExpr = rewriteExpr(fieldAccessExpr.indexExpr);
        fieldAccessExpr.expr = rewriteExpr(fieldAccessExpr.expr);
        result = fieldAccessExpr;
    }

    @Override
    public void visit(BLangFieldBasedAccess.BLangStructFunctionVarRef functionVarRef) {
        functionVarRef.expr = rewriteExpr(functionVarRef.expr);
        result = functionVarRef;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangMapAccessExpr mapKeyAccessExpr) {
        mapKeyAccessExpr.indexExpr = rewriteExpr(mapKeyAccessExpr.indexExpr);
        mapKeyAccessExpr.expr = rewriteExpr(mapKeyAccessExpr.expr);
        result = mapKeyAccessExpr;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangTableAccessExpr tableKeyAccessExpr) {
        tableKeyAccessExpr.indexExpr = rewriteExpr(tableKeyAccessExpr.indexExpr);
        tableKeyAccessExpr.expr = rewriteExpr(tableKeyAccessExpr.expr);
        result = tableKeyAccessExpr;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangArrayAccessExpr arrayIndexAccessExpr) {
        arrayIndexAccessExpr.indexExpr = rewriteExpr(arrayIndexAccessExpr.indexExpr);
        arrayIndexAccessExpr.expr = rewriteExpr(arrayIndexAccessExpr.expr);
        result = arrayIndexAccessExpr;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangTupleAccessExpr arrayIndexAccessExpr) {
        arrayIndexAccessExpr.indexExpr = rewriteExpr(arrayIndexAccessExpr.indexExpr);
        arrayIndexAccessExpr.expr = rewriteExpr(arrayIndexAccessExpr.expr);
        result = arrayIndexAccessExpr;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangXMLAccessExpr xmlIndexAccessExpr) {
        xmlIndexAccessExpr.indexExpr = rewriteExpr(xmlIndexAccessExpr.indexExpr);
        xmlIndexAccessExpr.expr = rewriteExpr(xmlIndexAccessExpr.expr);
        result = xmlIndexAccessExpr;
    }

    @Override
    public void visit(BLangXMLElementAccess xmlElementAccess) {
        xmlElementAccess.expr = rewriteExpr(xmlElementAccess.expr);
        result = xmlElementAccess;
    }

    @Override
    public void visit(BLangXMLNavigationAccess xmlNavigation) {
        xmlNavigation.expr = rewriteExpr(xmlNavigation.expr);
        result = xmlNavigation;
    }

    @Override
    public void visit(BLangExtendedXMLNavigationAccess extendedXmlNavigationAccess) {
        extendedXmlNavigationAccess.stepExpr = rewriteExpr(extendedXmlNavigationAccess.stepExpr);
        List<BLangXMLStepExtend> extensions = extendedXmlNavigationAccess.extensions;
        for (int i = 0; i < extensions.size(); i++) {
            extensions.set(i, rewrite(extensions.get(i), env));
        }
        result = extendedXmlNavigationAccess;
    }

    @Override
    public void visit(BLangXMLIndexedStepExtend xmlIndexedStepExtend) {
        xmlIndexedStepExtend.indexExpr = rewriteExpr(xmlIndexedStepExtend.indexExpr);
        result = xmlIndexedStepExtend;
    }

    @Override
    public void visit(BLangXMLFilterStepExtend xmlFilterStepExtend) {
        result = xmlFilterStepExtend;
    }
   
    @Override
    public void visit(BLangXMLMethodCallStepExtend xmlMethodCallStepExtend) {
        xmlMethodCallStepExtend.invocation = rewriteExpr(xmlMethodCallStepExtend.invocation);
        result = xmlMethodCallStepExtend;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangJSONAccessExpr jsonAccessExpr) {
        jsonAccessExpr.indexExpr = rewriteExpr(jsonAccessExpr.indexExpr);
        jsonAccessExpr.expr = rewriteExpr(jsonAccessExpr.expr);
        result = jsonAccessExpr;
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangStringAccessExpr stringAccessExpr) {
        stringAccessExpr.indexExpr = rewriteExpr(stringAccessExpr.indexExpr);
        stringAccessExpr.expr = rewriteExpr(stringAccessExpr.expr);
        result = stringAccessExpr;
    }

    @Override
    public void visit(BLangRecordLiteral.BLangMapLiteral mapLiteral) {
        for (RecordLiteralNode.RecordField field : mapLiteral.fields) {
            if (field.isKeyValueField()) {
                BLangRecordLiteral.BLangRecordKeyValueField keyValueField =
                        (BLangRecordLiteral.BLangRecordKeyValueField) field;
                keyValueField.key.expr = rewriteExpr(keyValueField.key.expr);
                keyValueField.valueExpr = rewriteExpr(keyValueField.valueExpr);
                continue;
            }

            BLangRecordLiteral.BLangRecordSpreadOperatorField spreadField =
                    (BLangRecordLiteral.BLangRecordSpreadOperatorField) field;
            spreadField.expr = rewriteExpr(spreadField.expr);
        }
        result = mapLiteral;
    }

    @Override
    public void visit(BLangRecordLiteral.BLangStructLiteral structLiteral) {
        for (RecordLiteralNode.RecordField field : structLiteral.fields) {
            if (field.isKeyValueField()) {
                BLangRecordLiteral.BLangRecordKeyValueField keyValueField =
                        (BLangRecordLiteral.BLangRecordKeyValueField) field;
                keyValueField.key.expr = rewriteExpr(keyValueField.key.expr);
                keyValueField.valueExpr = rewriteExpr(keyValueField.valueExpr);
                continue;
            }

            BLangRecordLiteral.BLangRecordSpreadOperatorField spreadField =
                    (BLangRecordLiteral.BLangRecordSpreadOperatorField) field;
            spreadField.expr = rewriteExpr(spreadField.expr);
        }

        result = structLiteral;
    }

    @Override
    public void visit(BLangWaitForAllExpr.BLangWaitLiteral waitLiteral) {
        waitLiteral.keyValuePairs.forEach(keyValue -> {
            if (keyValue.valueExpr != null) {
                keyValue.valueExpr = rewriteExpr(keyValue.valueExpr);
            } else {
                keyValue.keyExpr = rewriteExpr(keyValue.keyExpr);
            }
        });
        result = waitLiteral;
    }

    @Override
    public void visit(BLangIsAssignableExpr assignableExpr) {
        assignableExpr.lhsExpr = rewriteExpr(assignableExpr.lhsExpr);
        result = assignableExpr;
    }

    @Override
    public void visit(BLangInvocation.BFunctionPointerInvocation fpInvocation) {
        fpInvocation.expr = rewriteExpr(fpInvocation.expr);
        rewriteInvocationExpr(fpInvocation);
    }

    @Override
    public void visit(BLangTypedescExpr accessExpr) {
        result = accessExpr;
    }

    @Override
    public void visit(BLangRestArgsExpression bLangVarArgsExpression) {
        result = rewriteExpr(bLangVarArgsExpression.expr);
    }

    @Override
    public void visit(BLangNamedArgsExpression bLangNamedArgsExpression) {
        bLangNamedArgsExpression.expr = rewriteExpr(bLangNamedArgsExpression.expr);
        result = bLangNamedArgsExpression;
    }

    @Override
    public void visit(BLangCheckedExpr checkedExpr) {
        result = checkedExpr;
    }

    @Override
    public void visit(BLangServiceConstructorExpr serviceConstructorExpr) {
        result = serviceConstructorExpr;
    }

    @Override
    public void visit(BLangTypeTestExpr typeTestExpr) {
        typeTestExpr.typeNode = rewrite(typeTestExpr.typeNode, env);
        typeTestExpr.expr = rewriteExpr(typeTestExpr.expr);
        result = typeTestExpr;
    }

    @Override
    public void visit(BLangIsLikeExpr isLikeExpr) {
        isLikeExpr.expr = rewriteExpr(isLikeExpr.expr);
        result = isLikeExpr;
    }

    public void visit(BLangFieldBasedAccess.BLangPrefixedFieldBasedAccess prefixedFieldBasedAccess) {
        result = prefixedFieldBasedAccess;
    }

    @Override
    public void visit(BLangLetExpression letExpression) {
        for (BLangLetVariable letVariable : letExpression.letVarDeclarations) {
            rewrite((BLangNode) letVariable.definitionNode, env);
        }
        letExpression.expr = rewriteExpr(letExpression.expr);
        result = letExpression;
    }

    @Override
    public void visit(BLangAnnotAccessExpr annotAccessExpr) {
        annotAccessExpr.expr = rewriteExpr(annotAccessExpr.expr);
        result = annotAccessExpr;
    }

    @Override
    public void visit(BLangStatementExpression bLangStatementExpression) {
        if (bLangStatementExpression.stmt.getKind() == NodeKind.BLOCK) {
            BLangBlockStmt bLangBlockStmt = (BLangBlockStmt) bLangStatementExpression.stmt;
            for (int i = 0; i < bLangBlockStmt.stmts.size(); i++) {
                BLangStatement stmt = bLangBlockStmt.stmts.remove(i);
                bLangBlockStmt.stmts.add(i, rewrite(stmt, env));
            }
        } else {
            bLangStatementExpression.stmt = rewrite(bLangStatementExpression.stmt, env);
        }
        bLangStatementExpression.expr = rewriteExpr(bLangStatementExpression.expr);
        result = bLangStatementExpression;
    }

    @Override
    public void visit(BLangInvocation.BLangActionInvocation invocation) {
        rewriteInvocationExpr(invocation);
    }

    @Override
    public void visit(BLangIdentifier identifierNode) {
        /* ignore */
    }

    @Override
    public void visit(BLangAnnotation annotationNode) {
        /* ignore */
    }

    @Override
    public void visit(BLangAnnotationAttachment annAttachmentNode) {
        /* ignore */
    }

    @Override
    public void visit(BLangConstant constant) {
        result = constant;
    }

    @Override
    public void visit(BLangNumericLiteral literalExpr) {
        result = literalExpr;
    }

    @Override
    public void visit(BLangTupleVarRef varRefExpr) {
        result = varRefExpr;
    }

    @Override
    public void visit(BLangRecordVarRef varRefExpr) {
        result = varRefExpr;
    }

    @Override
    public void visit(BLangErrorVarRef varRefExpr) {
        result = varRefExpr;
    }

    @Override
    public void visit(BLangSimpleVarRef.BLangTypeLoad typeLoad) {
        result = typeLoad;
    }

    @Override
    public void visit(BLangXMLNS.BLangLocalXMLNS xmlnsNode) {
        xmlnsNode.namespaceURI = rewriteExpr(xmlnsNode.namespaceURI);
        result = xmlnsNode;
    }

    @Override
    public void visit(BLangXMLNS.BLangPackageXMLNS xmlnsNode) {
        xmlnsNode.namespaceURI = rewriteExpr(xmlnsNode.namespaceURI);
        result = xmlnsNode;
    }

    @Override
    public void visit(BLangListConstructorExpr.BLangListConstructorSpreadOpExpr listConstructorSpreadOpExpr) {
        listConstructorSpreadOpExpr.expr = rewriteExpr(listConstructorSpreadOpExpr.expr);
        result = listConstructorSpreadOpExpr;
    }

    @Override
    public void visit(BLangQueryExpr queryExpr) {
        for (BLangNode clause : queryExpr.getQueryClauses()) {
            rewrite(clause, env);
        }
        result = queryExpr;
    }

    @Override
    public void visit(BLangFromClause fromClause) {
        BLangExpression collection = fromClause.collection;
        rewrite(collection, env);
        result = fromClause;
    }

    @Override
    public void visit(BLangJoinClause joinClause) {
        rewrite(joinClause.collection, env);
        if (joinClause.onClause != null) {
            rewrite((BLangNode) joinClause.onClause, env);
        }
        result = joinClause;
    }

    @Override
    public void visit(BLangLetClause letClause) {
        for (BLangLetVariable letVariable : letClause.letVarDeclarations) {
            rewrite((BLangNode) letVariable.definitionNode, env);
        }
        result = letClause;
    }

    @Override
    public void visit(BLangWhereClause whereClause) {
        rewrite(whereClause.expression, env);
        result = whereClause;
    }

    @Override
    public void visit(BLangOnClause onClause) {
        rewrite(onClause.lhsExpr, env);
        rewrite(onClause.rhsExpr, env);
        result = onClause;
    }

    @Override
    public void visit(BLangOrderKey orderKeyClause) {
        rewrite(orderKeyClause.expression, env);
        result = orderKeyClause;
    }

    @Override
    public void visit(BLangOrderByClause orderByClause) {
        orderByClause.orderByKeyList.forEach(value -> rewrite((BLangNode) value, env));
        result = orderByClause;
    }

    @Override
    public void visit(BLangGroupByClause groupByClause) {
        groupByClause.groupingKeyList.forEach(value -> rewrite(value, env));
        result = groupByClause;
    }

    @Override
    public void visit(BLangGroupingKey groupingKey) {
        rewrite((BLangNode) groupingKey.getGroupingKey(), env);
        result = groupingKey;
    }

    @Override
    public void visit(BLangSelectClause selectClause) {
        rewrite(selectClause.expression, env);
        result = selectClause;
    }

    @Override
    public void visit(BLangCollectClause bLangCollectClause) {
        rewrite(bLangCollectClause.expression, env);
        result = bLangCollectClause;
    }

    @Override
    public void visit(BLangOnConflictClause onConflictClause) {
        rewrite(onConflictClause.expression, env);
        result = onConflictClause;
    }

    @Override
    public void visit(BLangLimitClause limitClause) {
        rewrite(limitClause.expression, env);
        result = limitClause;
    }

    @Override
    public void visit(BLangOnFailClause onFailClause) {
        rewrite(onFailClause.body, env);
        result = onFailClause;
    }

    @Override
    public void visit(BLangCollectContextInvocation collectContextInvocation) {
        rewrite(collectContextInvocation.invocation, env);
        result = collectContextInvocation;
    }

    @Override
    public void visit(BLangMatchStatement matchStatement) {
        result = matchStatement;
    }

    @Override
    public void visit(BLangXMLSequenceLiteral xmlSequenceLiteral) {
        xmlSequenceLiteral.xmlItems.forEach(this::rewriteExpr);
        result = xmlSequenceLiteral;
    }

    @Override
    public void visit(BLangRegExpTemplateLiteral regExpTemplateLiteral) {
        List<BLangExpression> interpolationsList =
                symResolver.getListOfInterpolations(regExpTemplateLiteral.reDisjunction.sequenceList);
        interpolationsList.forEach(this::rewriteExpr);
        result = regExpTemplateLiteral;
    }

    @Override
    public void visit(BLangMarkdownDocumentationLine bLangMarkdownDocumentationLine) {
        /* Ignore */
    }

    @Override
    public void visit(BLangMarkdownParameterDocumentation bLangDocumentationParameter) {
        /* Ignore */
    }

    @Override
    public void visit(BLangMarkdownReturnParameterDocumentation bLangMarkdownReturnParameterDocumentation) {
        /* Ignore */
    }

    @Override
    public void visit(BLangInferredTypedescDefaultNode inferTypedescExpr) {
        result = inferTypedescExpr;
    }
    @Override
    public void visit(BLangMarkdownDocumentation bLangMarkdownDocumentation) {
        /* Ignore */
    }

    @Override
    public void visit(BLangNaturalExpression naturalExpression) {
        rewriteExprs(naturalExpression.arguments);
        rewriteExprs(naturalExpression.strings);
        rewriteExprs(naturalExpression.insertions);
        result = naturalExpression;
    }

    // Rewrite methods

    @SuppressWarnings("unchecked")
    private <E extends BLangNode> E rewrite(E node, SymbolEnv env) {
        if (node == null) {
            return null;
        }

        SymbolEnv previousEnv = this.env;
        this.env = env;

        node.accept(this);
        BLangNode resultNode = this.result;
        this.result = null;

        this.env = previousEnv;
        return (E) resultNode;
    }

    private <E extends BLangNode> List<E> rewrite(List<E> nodeList, SymbolEnv env) {
        Queue<BLangSimpleVariableDef> previousQueue = this.annotationClosureReferences;
        this.annotationClosureReferences = new LinkedList<>();
        int size = nodeList.size();
        for (int i = 0; i < size; i++) {
            E node = rewrite(nodeList.remove(0), env);
            Iterator<BLangSimpleVariableDef> iterator = annotationClosureReferences.iterator();
            List<E> closureList = new ArrayList<>();
            while (iterator.hasNext()) {
                E simpleVariable = rewrite((E) annotationClosureReferences.poll().var, env);
                closureList.add(simpleVariable);
            }
            // Add closures before the dependent node in the top-level node list
            int indexAtTopLevel = env.enclPkg.topLevelNodes.indexOf(node);
            env.enclPkg.topLevelNodes.addAll(indexAtTopLevel, (Collection<? extends TopLevelNode>) closureList);
            nodeList.addAll(closureList);
            nodeList.add(node);
        }
        this.annotationClosureReferences = previousQueue;
        return nodeList;
    }

    @SuppressWarnings("unchecked")
    private <E extends BLangExpression> E rewriteExpr(E node) {
        if (node == null) {
            return null;
        }

        node.accept(this);
        BLangNode resultNode = this.result;
        this.result = null;
        return (E) resultNode;
    }

    @SuppressWarnings("unchecked")
    private <E extends BLangStatement> List<E> rewriteStmt(List<E> nodeList, SymbolEnv env) {
        Queue<BLangSimpleVariableDef> previousQueue = this.queue;
        this.queue = new LinkedList<>();
        int size = nodeList.size();
        for (int i = 0; i < size; i++) {
            E node = rewrite(nodeList.remove(0), env);
            Iterator<BLangSimpleVariableDef> iterator = queue.iterator();
            while (iterator.hasNext()) {
                BLangSimpleVariableDef bLangSimpleVariableDef = queue.poll();
                nodeList.add(rewrite((E) bLangSimpleVariableDef, env));
                BSymbol varSymbol = bLangSimpleVariableDef.var.symbol;
                env.scope.define(varSymbol.name, varSymbol);
            }
            nodeList.add(node);
        }
        this.queue = previousQueue;
        return nodeList;
    }

    @SuppressWarnings("unchecked")
    private <E extends BLangExpression> List<E> rewriteExprs(List<E> nodeList) {
        for (int i = 0; i < nodeList.size(); i++) {
            nodeList.set(i, rewriteExpr(nodeList.get(i)));
        }
        return nodeList;
    }
}
