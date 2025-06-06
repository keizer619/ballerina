/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.ballerinalang.compiler.bir;

import io.ballerina.identifier.Utils;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.types.PredefinedType;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.AnnotationAttachmentSymbol;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.symbols.SymbolOrigin;
import org.ballerinalang.model.tree.BlockNode;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.OperatorKind;
import org.ballerinalang.model.tree.TopLevelNode;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotation;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotationAttachment;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRBasicBlock;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRConstant;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRFunction;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRFunctionParameter;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRGlobalVariableDcl;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRLockDetailsHolder;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRPackage;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRParameter;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRTypeDefinition;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRVariableDcl;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.ConstValue;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.BinaryOp;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.Move;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.UnaryOP;
import org.wso2.ballerinalang.compiler.bir.model.BIROperand;
import org.wso2.ballerinalang.compiler.bir.model.BIRTerminator;
import org.wso2.ballerinalang.compiler.bir.model.BirScope;
import org.wso2.ballerinalang.compiler.bir.model.InstructionKind;
import org.wso2.ballerinalang.compiler.bir.model.VarKind;
import org.wso2.ballerinalang.compiler.bir.model.VarScope;
import org.wso2.ballerinalang.compiler.bir.optimizer.BIROptimizer;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SemTypeHelper;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BClassSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BConstantSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BEnumSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BResourceFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BResourcePathSegmentSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BServiceSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeDefinitionSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BXMLNSSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTypeReferenceType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTypedescType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotation;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangClassDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangExternalFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangResourceFunction;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS.BLangLocalXMLNS;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS.BLangPackageXMLNS;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAlternateWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConstant;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangDynamicArgExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangErrorConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess.BLangStructFunctionVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangGroupExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIgnoreExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangArrayAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangJSONAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangMapAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangStringAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangStructFieldAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangTableAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangXMLAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIsAssignableExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIsLikeExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangJSONArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangListConstructorSpreadOpExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangTupleLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMultipleWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReAssertion;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReAtomCharOrEscape;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReAtomQuantifier;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReCapturingGroups;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReCharSet;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReCharSetRange;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReCharacterClass;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReDisjunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReFlagExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReFlagsOnOff;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReQuantifier;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangReSequence;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangMapLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKeyValueField;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangStructLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRegExpTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangFunctionVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangLocalVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangPackageVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStatementExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTableConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTrapExpr;
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
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerSendReceiveExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerSyncSendExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLCommentLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLProcInsLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQName;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQuotedString;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLSequenceLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBreak;
import org.wso2.ballerinalang.compiler.tree.statements.BLangContinue;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangFail;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForkJoin;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangLock.BLangLockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangLock.BLangUnLockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangPanic;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.statements.BLangXMLNSStatement;
import org.wso2.ballerinalang.compiler.tree.types.BLangStructureTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.util.BArrayState;
import org.wso2.ballerinalang.compiler.util.ClosureVarSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.CompilerUtils;
import org.wso2.ballerinalang.compiler.util.FieldKind;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.Unifier;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import static org.ballerinalang.model.tree.NodeKind.CLASS_DEFN;
import static org.ballerinalang.model.tree.NodeKind.INVOCATION;
import static org.ballerinalang.model.tree.NodeKind.STATEMENT_EXPRESSION;
import static org.wso2.ballerinalang.compiler.bir.writer.BIRWriterUtils.createBIRAnnotationAttachment;
import static org.wso2.ballerinalang.compiler.bir.writer.BIRWriterUtils.getBIRAnnotAttachments;
import static org.wso2.ballerinalang.compiler.bir.writer.BIRWriterUtils.getBIRConstantVal;
import static org.wso2.ballerinalang.compiler.desugar.AnnotationDesugar.ANNOTATION_DATA;
import static org.wso2.ballerinalang.compiler.util.Constants.RECORD_DELIMITER;

/**
 * Lower the AST to BIR.
 *
 * @since 0.980.0
 */
public class BIRGen extends BLangNodeVisitor {

    private static final CompilerContext.Key<BIRGen> BIR_GEN =
            new CompilerContext.Key<>();

    public static final String DEFAULT_WORKER_NAME = "function";
    private static final String TYPEDESC = "$typedesc$";
    private BIRGenEnv env;
    private final Names names;
    private final SymbolTable symTable;
    private final BIROptimizer birOptimizer;
    private final Types types;

    // Required variables to generate code for assignment statements
    private boolean varAssignment = false;
    private final Map<BSymbol, BIRTypeDefinition> typeDefs = new LinkedHashMap<>();
    private BlockNode currentBlock;
    // This is a global variable cache
    public Map<BSymbol, BIRGlobalVariableDcl> globalVarMap = new HashMap<>();

    // This map is used to create dependencies for imported module global variables
    private final Map<BSymbol, BIRGlobalVariableDcl> dummyGlobalVarMapForLocks = new HashMap<>();

    // This is to cache the lock statement to BIR Lock
    private final Map<BLangLockStmt, BIRTerminator.Lock> lockStmtMap = new HashMap<>();

    private final Unifier unifier;

    private BirScope currentScope;

    public static BIRGen getInstance(CompilerContext context) {
        BIRGen birGen = context.get(BIR_GEN);
        if (birGen == null) {
            birGen = new BIRGen(context);
        }

        return birGen;
    }

    private BIRGen(CompilerContext context) {
        context.put(BIR_GEN, this);

        this.names = Names.getInstance(context);
        this.symTable = SymbolTable.getInstance(context);
        this.birOptimizer = BIROptimizer.getInstance(context);
        this.unifier = new Unifier();
        this.types = Types.getInstance(context);
    }

    public BLangPackage genBIR(BLangPackage astPkg) {
        boolean skipTest = astPkg.moduleContextDataHolder.skipTests();
        String sourceRoot = astPkg.moduleContextDataHolder.sourceRoot().toString();
        BIRPackage birPkg = new BIRPackage(astPkg.pos, astPkg.packageID.orgName, astPkg.packageID.pkgName,
                astPkg.packageID.name, astPkg.packageID.version, astPkg.packageID.sourceFileName,
                sourceRoot, skipTest);

        astPkg.symbol.bir = birPkg; //TODO try to remove this

        this.env = new BIRGenEnv(birPkg);
        astPkg.accept(this);

        this.birOptimizer.optimizePackage(birPkg);
        if (!astPkg.moduleContextDataHolder.skipTests() && astPkg.hasTestablePackage()) {
            astPkg.getTestablePkgs().forEach(testPkg -> {
                BIRPackage testBirPkg = new BIRPackage(testPkg.pos, testPkg.packageID.orgName,
                        testPkg.packageID.pkgName, testPkg.packageID.name, testPkg.packageID.version,
                        testPkg.packageID.sourceFileName, sourceRoot, skipTest, true);
                this.env = new BIRGenEnv(testBirPkg);
                testPkg.accept(this);
                this.birOptimizer.optimizePackage(testBirPkg);
                testPkg.symbol.bir = testBirPkg;
                testBirPkg.importModules.add(new BIRNode.BIRImportModule(null, testPkg.packageID.orgName,
                        testPkg.packageID.name, testPkg.packageID.version));
            });
        }

        setEntryPoints(astPkg);
        return astPkg;
    }

    private void setEntryPoints(BLangPackage pkgNode) {
        BLangFunction mainFunc = getMainFunction(pkgNode);
        if (mainFunc != null || listenerDeclarationFound(pkgNode.getGlobalVariables()) || !pkgNode.services.isEmpty()) {
            pkgNode.symbol.entryPointExists = true;
        }
    }

    private boolean listenerDeclarationFound(List<BLangVariable> globalVars) {
        for (BLangVariable globalVar : globalVars) {
            if (Symbols.isFlagOn(globalVar.symbol.flags, Flags.LISTENER)) {
                return true;
            }
        }
        return false;
    }

    private BLangFunction getMainFunction(BLangPackage pkgNode) {
        for (BLangFunction funcNode : pkgNode.functions) {
            if (CompilerUtils.isMainFunction(funcNode)) {
                return funcNode;
            }
        }
        return null;
    }

    // Nodes

    @Override
    public void visit(BLangPackage astPkg) {
        // Lower function nodes in AST to bir function nodes.
        // TODO handle init, start, stop functions
        astPkg.imports.forEach(impPkg -> impPkg.accept(this));
        astPkg.constants.forEach(astConst -> astConst.accept(this));
        astPkg.typeDefinitions.forEach(astTypeDef -> astTypeDef.accept(this));
        generateClassDefinitions(astPkg.topLevelNodes);
        astPkg.globalVars.forEach(astGlobalVar -> astGlobalVar.accept(this));
        astPkg.initFunction.accept(this);
        astPkg.startFunction.accept(this);
        astPkg.stopFunction.accept(this);
        astPkg.functions.forEach(astFunc -> astFunc.accept(this));
        astPkg.annotations.forEach(astAnn -> astAnn.accept(this));
        astPkg.services.forEach(service -> service.accept(this));
    }

    private void generateClassDefinitions(List<TopLevelNode> topLevelNodes) {
        for (TopLevelNode topLevelNode : topLevelNodes) {
            if (topLevelNode.getKind() == CLASS_DEFN) {
                ((BLangClassDefinition) topLevelNode).accept(this);
            }
        }
    }

    @Override
    public void visit(BLangTypeDefinition astTypeDefinition) {
        BType type = getDefinedType(astTypeDefinition);
        BType referredType = Types.getImpliedType(type);
        BSymbol symbol = astTypeDefinition.symbol;
        String displayName = symbol.name.value;
        if (referredType.tag == TypeTags.RECORD) {
            BRecordType recordType = (BRecordType) referredType;
            if (recordType.shouldPrintShape()) {
                displayName = recordType.toString();
            }
        }

        BIRTypeDefinition typeDef = new BIRTypeDefinition(astTypeDefinition.pos,
                                                          symbol.name,
                                                          symbol.flags,
                                                          astTypeDefinition.isBuiltinTypeDef,
                                                          type,
                                                          new ArrayList<>(),
                                                          symbol.origin.toBIROrigin(),
                                                          new Name(Utils.unescapeBallerina(displayName)),
                                                          symbol.originalName);
        if (symbol.tag == SymTag.TYPE_DEF) {
            BTypeReferenceType referenceType = ((BTypeDefinitionSymbol) symbol).referenceType;
            typeDef.referenceType = referenceType;
            BTypeSymbol typeSymbol = symbol.type.tsymbol;
            if (type.tsymbol.owner == symbol.owner
                    && !(Symbols.isFlagOn(typeSymbol.flags, Flags.CLASS))) {
                typeDefs.put(typeSymbol, typeDef);
            } else {
                if (referenceType != null) {
                    typeDef.type = referenceType;
                }
            }
            typeDef.annotAttachments.addAll(getBIRAnnotAttachments(((BTypeDefinitionSymbol) symbol).getAnnotations()));
        } else {
            // TODO: 2022-02-23 not necessarily enums, may not be a type definition symbol for generated type
            //  definitions.
            //enum symbols
            typeDefs.put(symbol, typeDef);

            if (astTypeDefinition.flagSet.contains(Flag.ENUM)) {
                typeDef.annotAttachments.addAll(getBIRAnnotAttachments(((BEnumSymbol) symbol).getAnnotations()));
            }
        }
        this.env.enclPkg.typeDefs.add(typeDef);
        typeDef.index = this.env.enclPkg.typeDefs.size() - 1;

        typeDef.setMarkdownDocAttachment(symbol.markdownDocumentation);

        if (astTypeDefinition.typeNode.getKind() == NodeKind.RECORD_TYPE ||
                astTypeDefinition.typeNode.getKind() == NodeKind.OBJECT_TYPE) {
            BLangStructureTypeNode typeNode = (BLangStructureTypeNode) astTypeDefinition.typeNode;
            for (BLangType typeRef : typeNode.typeRefs) {
                typeDef.referencedTypes.add(typeRef.getBType());
            }
        }

        BSymbol typeSymbol = symbol.tag == SymTag.TYPE_DEF ? symbol.type.tsymbol : symbol;
        // Write referenced functions, if this is an abstract-object
        if (typeSymbol.tag != SymTag.OBJECT || !Symbols.isFlagOn(typeSymbol.flags, Flags.CLASS)) {
            return;
        }

        for (BAttachedFunction func : ((BObjectTypeSymbol) typeSymbol).referencedFunctions) {
            if (!Symbols.isFlagOn(func.symbol.flags, Flags.INTERFACE)) {
                return;
            }

            BInvokableSymbol funcSymbol = func.symbol;
            BIRFunction birFunc = new BIRFunction(astTypeDefinition.pos, func.funcName, funcSymbol.flags, func.type,
                                                  Names.fromString(DEFAULT_WORKER_NAME), 0,
                                                  funcSymbol.origin.toBIROrigin());

            if (funcSymbol.receiverSymbol != null) {
                birFunc.receiver = getSelf(funcSymbol.receiverSymbol
                );
            }

            birFunc.setMarkdownDocAttachment(funcSymbol.markdownDocumentation);

            int defaultableParamsCount = 0;
            birFunc.argsCount = funcSymbol.params.size() + defaultableParamsCount +
                    (funcSymbol.restParam != null ? 1 : 0);
            funcSymbol.params.forEach(requiredParam -> addParam(birFunc, requiredParam, astTypeDefinition.pos));
            if (funcSymbol.restParam != null) {
                addRestParam(birFunc, funcSymbol.restParam, astTypeDefinition.pos);
            }

            birFunc.returnVariable = new BIRVariableDcl(astTypeDefinition.pos, funcSymbol.retType,
                    this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.RETURN, null);
            birFunc.localVars.add(0, birFunc.returnVariable);

            typeDef.attachedFuncs.add(birFunc);
        }
    }

    private BType getDefinedType(BLangTypeDefinition astTypeDefinition) {
        BType nodeType = astTypeDefinition.typeNode.getBType();
        // Consider: type DE distinct E;
        // For distinct types, the type defined by typeDefStmt (DE) is different from type used to define it (E).
        if (Types.getImpliedType(nodeType).tag == TypeTags.ERROR) {
            return astTypeDefinition.symbol.type;
        }
        return nodeType;
    }

    @Override
    public void visit(BLangClassDefinition classDefinition) {
        BIRTypeDefinition typeDef = new BIRTypeDefinition(classDefinition.pos,
                                                          classDefinition.symbol.name,
                                                          classDefinition.symbol.originalName,
                                                          classDefinition.symbol.flags,
                                                          false,
                                                          classDefinition.getBType(),
                                                          new ArrayList<>(),
                                                          classDefinition.symbol.origin.toBIROrigin());
        typeDefs.put(classDefinition.symbol, typeDef);
        this.env.enclPkg.typeDefs.add(typeDef);
        typeDef.index = this.env.enclPkg.typeDefs.size() - 1;

        typeDef.setMarkdownDocAttachment(classDefinition.symbol.markdownDocumentation);

        for (BLangType typeRef : classDefinition.typeRefs) {
            typeDef.referencedTypes.add(typeRef.getBType());
        }

        typeDef.annotAttachments.addAll(getBIRAnnotAttachments(
                ((BClassSymbol) classDefinition.symbol).getAnnotations()));

        for (BAttachedFunction func : ((BObjectTypeSymbol) classDefinition.symbol).referencedFunctions) {
            BInvokableSymbol funcSymbol = func.symbol;

            BIRFunction birFunc = new BIRFunction(classDefinition.pos, func.funcName, funcSymbol.flags, func.type,
                    Names.fromString(DEFAULT_WORKER_NAME), 0, funcSymbol.origin.toBIROrigin());

            if (funcSymbol.receiverSymbol != null) {
                birFunc.receiver = getSelf(funcSymbol.receiverSymbol);
            }

            birFunc.setMarkdownDocAttachment(funcSymbol.markdownDocumentation);

            int defaultableParamsCount = 0;
            birFunc.argsCount = funcSymbol.params.size() + defaultableParamsCount +
                    (funcSymbol.restParam != null ? 1 : 0);
            funcSymbol.params.forEach(requiredParam -> addParam(birFunc, requiredParam, classDefinition.pos));
            if (funcSymbol.restParam != null) {
                addRestParam(birFunc, funcSymbol.restParam, classDefinition.pos);
            }

            birFunc.returnVariable = new BIRVariableDcl(classDefinition.pos, funcSymbol.retType,
                                                        this.env.nextLocalVarId(names), VarScope.FUNCTION,
                                                        VarKind.RETURN, null);
            birFunc.localVars.add(0, birFunc.returnVariable);

            typeDef.attachedFuncs.add(birFunc);
        }
    }

    @Override
    public void visit(BLangService serviceNode) {
        BServiceSymbol symbol = (BServiceSymbol) serviceNode.symbol;
        List<String> attachPoint = symbol.getAbsResourcePath().orElse(null);
        String attachPointLiteral = symbol.getAttachPointStringLiteral().orElse(null);
        BIRNode.BIRServiceDeclaration serviceDecl =
                new BIRNode.BIRServiceDeclaration(attachPoint, attachPointLiteral, symbol.getListenerTypes(),
                        symbol.name, symbol.getAssociatedClassSymbol().name, symbol.type,
                        symbol.origin, symbol.flags, symbol.pos);
        serviceDecl.setMarkdownDocAttachment(symbol.markdownDocumentation);
        this.env.enclPkg.serviceDecls.add(serviceDecl);
    }

    @Override
    public void visit(BLangConstant astConstant) {
        BConstantSymbol constantSymbol = astConstant.symbol;
        Name constName = constantSymbol.name;
        Name constOriginalName = constantSymbol.getOriginalName();
        BType type = constantSymbol.type;

        // Get the value of the constant.
        ConstValue constantValue = getBIRConstantVal(constantSymbol.value);

        // Create a new constant info object.
        BIRConstant birConstant = new BIRConstant(astConstant.pos, constName, constOriginalName, constantSymbol.flags,
                                                  type, constantValue, constantSymbol.origin.toBIROrigin());
        birConstant.constValue = constantValue;

        birConstant.setMarkdownDocAttachment(astConstant.symbol.markdownDocumentation);
        birConstant.annotAttachments.addAll(getBIRAnnotAttachments(constantSymbol.getAnnotations()));

        // Add the constant to the package.
        this.env.enclPkg.constants.add(birConstant);
    }

    @Override
    public void visit(BLangImportPackage impPkg) {
        if (impPkg.symbol == null) {
            return;
        }
        this.env.enclPkg.importModules.add(new BIRNode.BIRImportModule(impPkg.pos, impPkg.symbol.pkgID.orgName,
                impPkg.symbol.pkgID.name, impPkg.symbol.pkgID.version));
    }

    @Override
    public void visit(BLangResourceFunction resourceFunction) {
        visit((BLangFunction) resourceFunction);
    }

    @Override
    public void visit(BLangFunction astFunc) {
        BInvokableType type = astFunc.symbol.getType();

        boolean isTypeAttachedFunction = astFunc.flagSet.contains(Flag.ATTACHED) &&
                !typeDefs.containsKey(astFunc.receiver.getBType().tsymbol);

        Name workerName = names.fromIdNode(astFunc.defaultWorkerName);

        this.env.unlockVars.push(new BIRLockDetailsHolder());
        Name funcName;
        if (isTypeAttachedFunction) {
            funcName = Names.fromString(astFunc.symbol.name.value);
        } else {
            funcName = getFuncName(astFunc.symbol);
        }
        BIRFunction birFunc = new BIRFunction(astFunc.pos, funcName,
                Names.fromString(astFunc.symbol.getOriginalName().value), astFunc.symbol.flags, type, workerName,
                astFunc.sendsToThis.size(), astFunc.symbol.origin.toBIROrigin());
        this.currentScope = new BirScope(0, null);
        if (astFunc.receiver != null) {
            BIRFunctionParameter birVarDcl = new BIRFunctionParameter(astFunc.pos, astFunc.receiver.getBType(),
                    this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.ARG, astFunc.receiver.name.value,
                    false, false);
            this.env.symbolVarMap.put(astFunc.receiver.symbol, birVarDcl);
            birFunc.receiver = getSelf(astFunc.receiver.symbol);
        }

        birFunc.setMarkdownDocAttachment(astFunc.symbol.markdownDocumentation);

        //create channelDetails array
        int i = 0;
        for (BLangWorkerSendReceiveExpr.Channel channel : astFunc.sendsToThis) {
            String channelId = channel.channelId();
            birFunc.workerChannels[i] = new BIRNode.ChannelDetails(channelId, astFunc.defaultWorkerName.value
                    .equals(DEFAULT_WORKER_NAME), isWorkerSend(channelId, astFunc.defaultWorkerName.value));
            i++;
        }

        // Populate annotation attachments on external in BIRFunction node
        if (astFunc.hasBody() && astFunc.body.getKind() == NodeKind.EXTERN_FUNCTION_BODY) {
            birFunc.annotAttachments.addAll(getBIRAnnotAttachmentsForASTAnnotAttachments(
                    ((BLangExternalFunctionBody) astFunc.body).annAttachments));
        }
        // Populate annotation attachments on function in BIRFunction node
        birFunc.annotAttachments.addAll(getBIRAnnotAttachments(astFunc.symbol.getAnnotations()));
        if (Symbols.isFlagOn(astFunc.symbol.flags, Flags.NATIVE)) {
            birFunc.annotAttachmentsOnExternal =
                    getBIRAnnotAttachments(astFunc.symbol.getAnnotationAttachmentsOnExternal());
        }

        // Populate annotation attachments on return type
        BTypeSymbol tsymbol = astFunc.symbol.type.tsymbol;
        if (astFunc.returnTypeNode != null && tsymbol != null) {
            birFunc.returnTypeAnnots.addAll(getBIRAnnotAttachments(((BInvokableTypeSymbol) tsymbol).returnTypeAnnots));
        }

        birFunc.argsCount = astFunc.requiredParams.size()
                + (astFunc.restParam != null ? 1 : 0) + astFunc.paramClosureMap.size();
        if (astFunc.flagSet.contains(Flag.ATTACHED) && typeDefs.containsKey(astFunc.receiver.getBType().tsymbol)) {
            typeDefs.get(astFunc.receiver.getBType().tsymbol).attachedFuncs.add(birFunc);
        } else {
            this.env.enclPkg.functions.add(birFunc);
        }

        this.env.enclFunc = birFunc;

        // TODO: Return variable with NIL type should be written to BIR
        // Special %0 location for storing return values
        BType retType = unifier.build(symTable.typeEnv(), astFunc.symbol.type.getReturnType());
        birFunc.returnVariable = new BIRVariableDcl(astFunc.pos, retType, this.env.nextLocalVarId(names),
                                                    VarScope.FUNCTION, VarKind.RETURN, null);
        birFunc.localVars.add(0, birFunc.returnVariable);

        //add closure vars
        astFunc.paramClosureMap.forEach((k, v) -> addRequiredParam(birFunc, v, astFunc.pos));

        // Create variable declaration for function params
        astFunc.requiredParams.forEach(requiredParam -> addParam(birFunc, requiredParam));
        if (astFunc.restParam != null) {
            addRestParam(birFunc, astFunc.restParam.symbol, astFunc.restParam.pos);
        }

        if (astFunc.flagSet.contains(Flag.RESOURCE)) {
            BTypeSymbol parentTSymbol = astFunc.parent.getBType().tsymbol;
            // Parent symbol will always be BObjectTypeSymbol for resource functions
            BObjectTypeSymbol objectTypeSymbol = (BObjectTypeSymbol) parentTSymbol;
            for (BAttachedFunction func : objectTypeSymbol.attachedFuncs) {
                if (func.funcName.value.equals(funcName.value)) {
                    BResourceFunction resourceFunction = (BResourceFunction) func;
                    
                    List<BVarSymbol> pathParamSymbols = resourceFunction.pathParams;
                    List<BIRVariableDcl> pathParams = new ArrayList<>(pathParamSymbols.size());
                    for (BVarSymbol pathParamSym : pathParamSymbols) {
                        pathParams.add(createBIRVarDeclForPathParam(pathParamSym));
                    }
                    birFunc.pathParams = pathParams;
                            
                    BVarSymbol restPathParamSym = resourceFunction.restPathParam;
                    if (restPathParamSym != null) {
                        birFunc.restPathParam = createBIRVarDeclForPathParam(restPathParamSym);
                    }

                    List<BResourcePathSegmentSymbol> pathSegmentSymbols = resourceFunction.pathSegmentSymbols;
                    int pathSegmentSize = pathSegmentSymbols.size();
                    List<Name> pathSegmentNameList = new ArrayList<>(pathSegmentSize);
                    List<Location> pathSegmentPosList = new ArrayList<>(pathSegmentSize);
                    List<BType> pathSegmentTypeList = new ArrayList<>(pathSegmentSize);
                    for (BSymbol pathSegmentSym : pathSegmentSymbols) {
                        pathSegmentNameList.add(pathSegmentSym.name);
                        pathSegmentPosList.add(pathSegmentSym.pos);
                        pathSegmentTypeList.add(pathSegmentSym.type);
                    }

                    birFunc.resourcePathSegmentPosList = pathSegmentPosList;
                    birFunc.resourcePath = pathSegmentNameList;
                    birFunc.accessor = resourceFunction.accessor;
                    birFunc.pathSegmentTypeList = pathSegmentTypeList;
                    break;
                }
            }
        }

        if (astFunc.interfaceFunction || Symbols.isNative(astFunc.symbol)) {
            this.env.clear();
            return;
        }

        // Create the entry basic block
        BIRBasicBlock entryBB = new BIRBasicBlock(this.env.nextBBId());
        this.env.enclBasicBlocks = birFunc.basicBlocks;
        birFunc.basicBlocks.add(entryBB);
        this.env.enclBB = entryBB;
        addToTrapStack(entryBB);

        astFunc.body.accept(this);
        birFunc.basicBlocks.add(this.env.returnBB);

        // Due to the current algorithm, some basic blocks will not contain any instructions or a terminator.
        // These basic blocks will be remove by the optimizer, but for now just add a return terminator
        BIRBasicBlock enclBB = this.env.enclBB;
        if (enclBB.terminator == null && this.env.returnBB != null) {
            enclBB.terminator = new BIRTerminator.GOTO(null, this.env.returnBB, this.currentScope);
        }

        this.env.clear();
        birFunc.dependentGlobalVars = astFunc.symbol.dependentGlobalVars.stream()
                .map(varSymbol -> this.globalVarMap.get(varSymbol)).collect(Collectors.toSet());
    }
    
    private BIRVariableDcl createBIRVarDeclForPathParam(BVarSymbol pathParamSym) {
        return new BIRVariableDcl(pathParamSym.pos, pathParamSym.type, this.env.nextLocalVarId(names), 
                VarScope.FUNCTION, VarKind.ARG, pathParamSym.name.value);
    }

    private BIRVariableDcl getSelf(BSymbol receiver) {
        BIRVariableDcl self = this.env.symbolVarMap.get(receiver);
        if (self == null) {
            return new BIRVariableDcl(null, receiver.type, receiver.name,
                                      VarScope.FUNCTION, VarKind.SELF, null);
        }
        self.kind = VarKind.SELF;
        self.name = new Name("%self");

        return self;
    }

    @Override
    public void visit(BLangBlockFunctionBody astBody) {
        BIRBasicBlock endLoopEndBB = this.env.enclLoopEndBB;
        BlockNode prevBlock = this.currentBlock;
        this.currentBlock = astBody;
        this.env.varDclsByBlock.computeIfAbsent(astBody, k -> new ArrayList<>());

        for (BLangStatement astStmt : astBody.stmts) {
            astStmt.accept(this);
        }

        List<BIRVariableDcl> varDecls = this.env.varDclsByBlock.get(astBody);
        for (BIRVariableDcl birVariableDcl : varDecls) {
            birVariableDcl.endBB = this.env.enclBasicBlocks.get(this.env.enclBasicBlocks.size() - 1);
        }
        this.env.enclLoopEndBB = endLoopEndBB;
        this.currentBlock = prevBlock;
        addReturnBB(astBody.pos);
    }

    private BIRBasicBlock beginBreakableBlock(BLangBlockStmt.FailureBreakMode mode) {
        BIRBasicBlock blockBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(blockBB);
        this.env.enclBasicBlocks.add(blockBB);

        // Insert a GOTO instruction as the terminal instruction into current basic block.
        this.env.enclBB.terminator = new BIRTerminator.GOTO(null, blockBB, this.currentScope);

        BIRBasicBlock blockEndBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(blockEndBB);

        blockBB.terminator = new BIRTerminator.GOTO(null, blockEndBB, this.currentScope);

        this.env.enclBB = blockBB;
        if (mode == BLangBlockStmt.FailureBreakMode.BREAK_WITHIN_BLOCK) {
            this.env.enclInnerOnFailEndBB = blockEndBB;
        } else {
            this.env.enclOnFailEndBB = blockEndBB;
        }
        this.env.unlockVars.push(new BIRLockDetailsHolder());
        return blockEndBB;
    }

    private void endBreakableBlock(BIRBasicBlock blockEndBB) {
        this.env.unlockVars.pop();
        if (this.env.enclBB.terminator == null) {
            this.env.enclBB.terminator = new BIRTerminator.GOTO(null, blockEndBB, this.currentScope);
        }
        this.env.enclBasicBlocks.add(blockEndBB);
        this.env.enclBB = blockEndBB;
    }

    @Override
    public void visit(BLangAnnotation astAnnotation) {
        BAnnotationSymbol annSymbol = (BAnnotationSymbol) astAnnotation.symbol;
        BIRAnnotation birAnn = getBirAnnotation(annSymbol, astAnnotation.pos);
        this.env.enclPkg.annotations.add(birAnn);
    }

    private BIRAnnotation getBirAnnotation(BAnnotationSymbol annSymbol, Location pos) {
        BIRAnnotation birAnn = new BIRAnnotation(pos, annSymbol.name, annSymbol.originalName,
                                                 annSymbol.flags, annSymbol.points,
                                                 annSymbol.attachedType == null ? symTable.trueType :
                                                         annSymbol.attachedType, annSymbol.origin.toBIROrigin());
        birAnn.packageID = annSymbol.pkgID;
        birAnn.setMarkdownDocAttachment(annSymbol.markdownDocumentation);
        birAnn.annotAttachments.addAll(getBIRAnnotAttachments(annSymbol.getAnnotations()));
        return birAnn;
    }


    private boolean isWorkerSend(String chnlName, String workerName) {
        return chnlName.split("->")[0].equals(workerName);
    }

    @Override
    public void visit(BLangLambdaFunction lambdaExpr) {
        //fpload instruction
        BIRVariableDcl tempVarLambda = new BIRVariableDcl(lambdaExpr.pos, lambdaExpr.getBType(),
                                                          this.env.nextLocalVarId(names), VarScope.FUNCTION,
                                                          VarKind.TEMP, null);
        this.env.enclFunc.localVars.add(tempVarLambda);
        BIROperand lhsOp = new BIROperand(tempVarLambda);
        Name funcName = getFuncName(lambdaExpr.function.symbol);

        List<BIRVariableDcl> params = new ArrayList<>();

        lambdaExpr.function.requiredParams.forEach(param -> {

            BIRVariableDcl birVarDcl = new BIRVariableDcl(param.pos, param.symbol.type,
                    this.env.nextLambdaVarId(names), VarScope.FUNCTION, VarKind.ARG, param.name.value);
            params.add(birVarDcl);
        });

        BLangSimpleVariable restParam = lambdaExpr.function.restParam;
        if (restParam != null) {
            BIRVariableDcl birVarDcl = new BIRVariableDcl(restParam.pos, restParam.symbol.type,
                    this.env.nextLambdaVarId(names), VarScope.FUNCTION, VarKind.ARG, null);
            params.add(birVarDcl);
        }

        PackageID pkgID = lambdaExpr.function.symbol.pkgID;
        PackageID boundMethodPkgId = getPackageIdForBoundMethod(lambdaExpr, funcName.value);
        List<BIROperand> closureMapOperands = getClosureMapOperands(lambdaExpr);
        BIRNonTerminator.FPLoad fpLoad = new BIRNonTerminator.FPLoad(lambdaExpr.pos, pkgID, funcName, lhsOp, params,
                closureMapOperands, lambdaExpr.getBType(), lambdaExpr.function.symbol.strandName,
                lambdaExpr.function.symbol.schedulerPolicy, boundMethodPkgId);
        setScopeAndEmit(fpLoad);
        BType targetType = getRecordTargetType(funcName.value);
        if (lambdaExpr.function.flagSet.contains(Flag.RECORD) && targetType != null &&
                targetType.tag == TypeTags.RECORD) {
            // If the function is for record type and has captured variables, then we need to create a
            // temp value in the type and keep it
            setScopeAndEmit(
                    new BIRNonTerminator.RecordDefaultFPLoad(lhsOp.pos, lhsOp, targetType,
                            getFieldName(funcName.value, targetType.tsymbol.name.value)));
        }
        this.env.targetOperand = lhsOp;
    }

    private String getFieldName(String funcName, String typeName) {
        String[] splitNames = funcName.split(Pattern.quote(typeName + RECORD_DELIMITER));
        return splitNames[splitNames.length - 1];
    }

    private BType getRecordTargetType(String funcName) {
        if (!funcName.contains(RECORD_DELIMITER)) {
            return null;
        }
        String[] split = funcName.split(Pattern.quote(RECORD_DELIMITER));
        String typeName = split[split.length - 2];
        for (BIRTypeDefinition type : this.env.enclPkg.typeDefs) {
            if (typeName.equals(type.originalName.value)) {
                return type.type;
            }
        }
        return null;
    }

    private List<BIROperand> getClosureMapOperands(BLangLambdaFunction lambdaExpr) {
        List<BIROperand> closureMaps = new ArrayList<>(lambdaExpr.function.paramClosureMap.size());

        lambdaExpr.function.paramClosureMap.forEach((k, v) -> {
            BVarSymbol symbol = lambdaExpr.enclMapSymbols.get(k);
            if (symbol == null) {
                symbol = lambdaExpr.paramMapSymbolsOfEnclInvokable.get(k);
            }
            BIROperand varRef = new BIROperand(this.env.symbolVarMap.get(symbol));
            closureMaps.add(varRef);
        });

        return closureMaps;
    }

    private Name getFuncName(BInvokableSymbol symbol) {
        if (symbol.receiverSymbol == null) {
            return Names.fromString(symbol.name.value);
        }

        int offset = symbol.receiverSymbol.type.tsymbol.name.value.length() + 1;
        String attachedFuncName = symbol.name.value;
        return Names.fromString(attachedFuncName.substring(offset));
    }

    private void addParam(BIRFunction birFunc, BLangVariable functionParam) {
        addParam(birFunc, functionParam.symbol, functionParam.expr, functionParam.pos,
                 functionParam.symbol.getAnnotations());
    }

    private void addParam(BIRFunction birFunc, BVarSymbol paramSymbol, Location pos) {
        addParam(birFunc, paramSymbol, null, pos, paramSymbol.getAnnotations());
    }

    private void addParam(BIRFunction birFunc, BVarSymbol paramSymbol, BLangExpression defaultValExpr,
                          Location pos, List<? extends AnnotationAttachmentSymbol> annots) {
        boolean isPathParam = paramSymbol.kind == SymbolKind.PATH_PARAMETER ||
                paramSymbol.kind == SymbolKind.PATH_REST_PARAMETER;
        BIRFunctionParameter birVarDcl = new BIRFunctionParameter(pos, paramSymbol.type,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.ARG,
                paramSymbol.name.value, defaultValExpr != null, isPathParam);

        birFunc.localVars.add(birVarDcl);

        BIRParameter parameter = new BIRParameter(pos, paramSymbol.name, paramSymbol.flags);
        parameter.annotAttachments.addAll(getBIRAnnotAttachments(annots));
        birFunc.requiredParams.add(parameter);
        birFunc.parameters.add(birVarDcl);

        // We maintain a mapping from variable symbol to the bir_variable declaration.
        // This is required to pull the correct bir_variable declaration for variable references.
        this.env.symbolVarMap.put(paramSymbol, birVarDcl);
    }

    private void addRestParam(BIRFunction birFunc, BVarSymbol paramSymbol, Location pos) {
        BIRFunctionParameter birVarDcl = new BIRFunctionParameter(pos, paramSymbol.type,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.ARG, paramSymbol.name.value, false,
                paramSymbol.kind == SymbolKind.PATH_REST_PARAMETER);
        birFunc.parameters.add(birVarDcl);
        birFunc.localVars.add(birVarDcl);

        BIRParameter restParam = new BIRParameter(pos, paramSymbol.name, paramSymbol.flags);
        birFunc.restParam = restParam;
        restParam.annotAttachments.addAll(getBIRAnnotAttachments(paramSymbol.getAnnotations()));

        // We maintain a mapping from variable symbol to the bir_variable declaration.
        // This is required to pull the correct bir_variable declaration for variable references.
        this.env.symbolVarMap.put(paramSymbol, birVarDcl);
    }

    private void addRequiredParam(BIRFunction birFunc, BVarSymbol paramSymbol, Location pos) {
        boolean isPathParam = paramSymbol.kind == SymbolKind.PATH_PARAMETER ||
                paramSymbol.kind == SymbolKind.PATH_REST_PARAMETER;
        BIRFunctionParameter birVarDcl = new BIRFunctionParameter(pos, paramSymbol.type,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.ARG, paramSymbol.name.value,
                false, isPathParam);
        birFunc.parameters.add(birVarDcl);
        birFunc.localVars.add(birVarDcl);

        BIRParameter parameter = new BIRParameter(pos, paramSymbol.name, paramSymbol.flags);
        birFunc.requiredParams.add(parameter);

        // We maintain a mapping from variable symbol to the bir_variable declaration.
        // This is required to pull the correct bir_variable declaration for variable references.
        this.env.symbolVarMap.put(paramSymbol, birVarDcl);
    }

    private PackageID getPackageIdForBoundMethod(BLangLambdaFunction lambdaExpr, String funcName) {
        if (!funcName.startsWith("$anon$method$delegate$")) {
            return null;
        }

        Set<ClosureVarSymbol> closureVarSymbols = lambdaExpr.function.closureVarSymbols;
        if (closureVarSymbols.isEmpty()) {
            return null;
        }

        Object[] symbols = closureVarSymbols.toArray();
        ClosureVarSymbol next = (ClosureVarSymbol) symbols[symbols.length - 1];
        return next.bSymbol.type.tsymbol.pkgID;
    }

    // Statements

    @Override
    public void visit(BLangBlockStmt astBlockStmt) {
        BIRBasicBlock blockEndBB = null;
        BIRBasicBlock currentOnFailEndBB = this.env.enclOnFailEndBB;
        BIRBasicBlock currentWithinOnFailEndBB = this.env.enclInnerOnFailEndBB;
        BlockNode prevBlock = this.currentBlock;
        this.currentBlock = astBlockStmt;
        this.env.varDclsByBlock.computeIfAbsent(astBlockStmt, k -> new ArrayList<>());
        if (astBlockStmt.failureBreakMode != BLangBlockStmt.FailureBreakMode.NOT_BREAKABLE) {
            blockEndBB = beginBreakableBlock(astBlockStmt.failureBreakMode);
        }
        for (BLangStatement astStmt : astBlockStmt.stmts) {
            astStmt.accept(this);
        }
        if (astBlockStmt.failureBreakMode != BLangBlockStmt.FailureBreakMode.NOT_BREAKABLE) {
            endBreakableBlock(blockEndBB);
        }
        this.env.varDclsByBlock.get(astBlockStmt).forEach(birVariableDcl ->
                birVariableDcl.endBB = this.env.enclBasicBlocks.get(this.env.enclBasicBlocks.size() - 1)
        );
        if (astBlockStmt.isLetExpr) {
            breakBBForLetExprVariables(astBlockStmt.pos);
        }
        this.env.enclInnerOnFailEndBB = currentWithinOnFailEndBB;
        this.env.enclOnFailEndBB = currentOnFailEndBB;
        this.currentBlock = prevBlock;
    }

    private void breakBBForLetExprVariables(Location pos) {
        BIRBasicBlock letExprEndBB = new BIRBasicBlock(this.env.nextBBId());
        this.env.enclBB.terminator = new BIRTerminator.GOTO(pos, letExprEndBB, this.currentScope);
        this.env.enclBasicBlocks.add(letExprEndBB);
        this.env.enclBB = letExprEndBB;
    }

    @Override
    public void visit(BLangFail failNode) {
        if (failNode.expr == null) {
            if (this.env.enclInnerOnFailEndBB != null) {
                this.env.enclBB.terminator = new BIRTerminator.GOTO(null, this.env.enclInnerOnFailEndBB,
                        this.currentScope);
            }
            return;
        }

        BIRLockDetailsHolder toUnlock = this.env.unlockVars.peek();
        if (!toUnlock.isEmpty()) {
            BIRBasicBlock goToBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(goToBB);
            this.env.enclBB.terminator = new BIRTerminator.GOTO(null, goToBB, this.currentScope);
            this.env.enclBB = goToBB;
        }

        int numLocks = toUnlock.size();
        while (numLocks > 0) {
            BIRBasicBlock unlockBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(unlockBB);
            BIRTerminator.Unlock unlock = new BIRTerminator.Unlock(null, unlockBB, this.currentScope);
            this.env.enclBB.terminator = unlock;
            unlock.relatedLock = toUnlock.getLock(numLocks - 1);
            this.env.enclBB = unlockBB;
            numLocks--;
        }

        // Create a basic block for the on fail clause.
        BIRBasicBlock onFailBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(onFailBB);
        this.env.enclBasicBlocks.add(onFailBB);

        // Insert a GOTO instruction as the terminal instruction into current basic block.
        this.env.enclBB.terminator = new BIRTerminator.GOTO(null, onFailBB, this.currentScope);

        // Visit condition expression
        this.env.enclBB = onFailBB;
        failNode.exprStmt.accept(this);
        if (this.env.enclBB.terminator == null) {
            this.env.enclBB.terminator = new BIRTerminator.GOTO(null, this.env.enclOnFailEndBB,
                    this.currentScope);
        }

        // Statements after fail expression are unreachable, hence ignored
        BIRBasicBlock ignoreBlock = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(ignoreBlock);
        ignoreBlock.terminator = new BIRTerminator.GOTO(null, this.env.enclOnFailEndBB, this.currentScope);
        this.env.enclBasicBlocks.add(ignoreBlock);
        this.env.enclBB = ignoreBlock;
    }


    @Override
    public void visit(BLangSimpleVariableDef astVarDefStmt) {
        VarKind kind;
        if (astVarDefStmt.var.symbol.origin == SymbolOrigin.VIRTUAL) {
            kind = VarKind.SYNTHETIC;
        } else {
            kind = VarKind.LOCAL;
        }
        BIRVariableDcl birVarDcl = new BIRVariableDcl(astVarDefStmt.pos, astVarDefStmt.var.symbol.type,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, kind, astVarDefStmt.var.name.value);
        birVarDcl.startBB = this.env.enclBB;
        this.env.varDclsByBlock.get(this.currentBlock).add(birVarDcl);
        this.env.enclFunc.localVars.add(birVarDcl);
        // We maintain a mapping from variable symbol to the bir_variable declaration.
        // This is required to pull the correct bir_variable declaration for variable references.
        this.env.symbolVarMap.put(astVarDefStmt.var.symbol, birVarDcl);

        BirScope newScope = new BirScope(this.currentScope.id() + 1, this.currentScope);
        birVarDcl.insScope = newScope;
        this.currentScope = newScope;

        if (astVarDefStmt.var.expr == null) {
            return;
        }

        // Visit the rhs expression.
        astVarDefStmt.var.expr.accept(this);

        // Create a variable reference and
        BIROperand varRef = new BIROperand(birVarDcl);
        setScopeAndEmit(new Move(astVarDefStmt.pos, this.env.targetOperand, varRef));
        birVarDcl.insOffset = this.env.enclBB.instructions.size() - 1;
    }

    @Override
    public void visit(BLangSimpleVariable varNode) {
        String name = ANNOTATION_DATA.equals(varNode.symbol.name.value) ? ANNOTATION_DATA : varNode.name.value;
        String originalName = ANNOTATION_DATA.equals(varNode.symbol.getOriginalName().value) ?
                                                                    ANNOTATION_DATA : varNode.name.originalValue;
        BIRGlobalVariableDcl birVarDcl = new BIRGlobalVariableDcl(varNode.pos, varNode.symbol.flags,
                                                                  varNode.symbol.type, varNode.symbol.pkgID,
                                                                  Names.fromString(name),
                                                                  Names.fromString(originalName), VarScope.GLOBAL,
                                                                  VarKind.GLOBAL, varNode.name.value,
                                                                  varNode.symbol.origin.toBIROrigin());
        birVarDcl.setMarkdownDocAttachment(varNode.symbol.markdownDocumentation);
        birVarDcl.annotAttachments.addAll(getBIRAnnotAttachments(varNode.symbol.getAnnotations()));

        this.env.enclPkg.globalVars.add(birVarDcl);

        this.globalVarMap.put(varNode.symbol, birVarDcl);
        env.enclPkg.isListenerAvailable |= Symbols.isFlagOn(varNode.symbol.flags, Flags.LISTENER);
    }

    @Override
    public void visit(BLangAssignment astAssignStmt) {
        astAssignStmt.expr.accept(this);

        this.varAssignment = true;
        astAssignStmt.varRef.accept(this);
        this.varAssignment = false;
    }

    @Override
    public void visit(BLangExpressionStmt exprStmtNode) {
        BLangExpression expr = exprStmtNode.expr;
        expr.accept(this);
        if (this.env.returnBB == null && expr.getKind() == STATEMENT_EXPRESSION &&
                ((BLangStatementExpression) expr).expr.getKind() == INVOCATION &&
        types.isNeverTypeOrStructureTypeWithARequiredNeverMember(expr.getBType())) {
            BIRBasicBlock returnBB = new BIRBasicBlock(this.env.nextBBId());
            returnBB.terminator = new BIRTerminator.Return(exprStmtNode.pos);
            this.env.returnBB = returnBB;
        }
    }

    @Override
    public void visit(BLangInvocation invocationExpr) {
        createCall(invocationExpr, false);
    }

    @Override
    public void visit(BLangInvocation.BLangActionInvocation actionInvocation) {
        createCall(actionInvocation, false);
    }

    @Override
    public void visit(BLangStatementExpression statementExpression) {
        statementExpression.stmt.accept(this);
        statementExpression.expr.accept(this);
    }

    @Override
    public void visit(BLangInvocation.BLangAttachedFunctionInvocation invocationExpr) {
        createCall(invocationExpr, true);
    }

    @Override
    public void visit(BLangInvocation.BFunctionPointerInvocation invocation) {
        invocation.functionPointerInvocation = true;
        createCall(invocation, false);
    }

    @Override
    public void visit(BLangForkJoin forkJoin) {
        forkJoin.workers.forEach(worker -> worker.accept(this));
    }

    @Override
    public void visit(BLangAlternateWorkerReceive altWorkerReceive) {
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(altWorkerReceive.getBType(), this.env.nextLocalVarId(names),
                VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        boolean isOnSameStrand = DEFAULT_WORKER_NAME.equals(this.env.enclFunc.workerName.value);

        this.env.enclBB.terminator = new BIRTerminator.WorkerAlternateReceive(altWorkerReceive.pos,
                getChannelList(altWorkerReceive), lhsOp, isOnSameStrand, thenBB, this.currentScope);
        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    private List<String> getChannelList(BLangAlternateWorkerReceive alternateWorkerReceive) {
        List<String> channels = new ArrayList<>();
        for (BLangWorkerReceive workerReceive : alternateWorkerReceive.getWorkerReceives()) {
            channels.add(workerReceive.getChannel().channelId());
        }
        return channels;
    }

    private List<BIRTerminator.WorkerMultipleReceive.ReceiveField> getChannelList(
            BLangMultipleWorkerReceive multipleWorkerReceive) {
        List<BIRTerminator.WorkerMultipleReceive.ReceiveField> channels = new ArrayList<>();
        for (BLangMultipleWorkerReceive.BLangReceiveField workerReceive : multipleWorkerReceive.getReceiveFields()) {
            channels.add(new BIRTerminator.WorkerMultipleReceive.ReceiveField(workerReceive.getKey().value,
                    workerReceive.getWorkerReceive().getChannel().channelId()));
        }
        return channels;
    }

    @Override
    public void visit(BLangMultipleWorkerReceive multipleWorkerReceive) {
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(multipleWorkerReceive.getBType(), this.env.nextLocalVarId(names),
                VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;
        boolean isOnSameStrand = DEFAULT_WORKER_NAME.equals(this.env.enclFunc.workerName.value);

        this.env.enclBB.terminator = new BIRTerminator.WorkerMultipleReceive(multipleWorkerReceive.pos,
                getChannelList(multipleWorkerReceive), lhsOp, isOnSameStrand, thenBB, this.currentScope);
        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    @Override
    public void visit(BLangWorkerReceive workerReceive) {
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);

        BIRVariableDcl tempVarDcl = new BIRVariableDcl(workerReceive.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        boolean isOnSameStrand = DEFAULT_WORKER_NAME.equals(this.env.enclFunc.workerName.value);

        this.env.enclBB.terminator = new BIRTerminator.WorkerReceive(workerReceive.pos,
                Names.fromString(workerReceive.getChannel().channelId()), lhsOp, isOnSameStrand, thenBB,
                this.currentScope);

        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    @Override
    public void visit(BLangWorkerAsyncSendExpr asyncSendExpr) {
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);

        asyncSendExpr.expr.accept(this);
        BIROperand dataOp = this.env.targetOperand;
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(asyncSendExpr.receive.matchingSendsError,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;
        boolean isOnSameStrand = DEFAULT_WORKER_NAME.equals(this.env.enclFunc.workerName.value);

        this.env.enclBB.terminator = new BIRTerminator.WorkerSend(
                asyncSendExpr.pos, Names.fromString(asyncSendExpr.getChannel().channelId()), dataOp, isOnSameStrand,
                false, lhsOp, thenBB, this.currentScope);

        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    @Override
    public void visit(BLangWorkerSyncSendExpr syncSend) {
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        syncSend.expr.accept(this);
        BIROperand dataOp = this.env.targetOperand;

        BIRVariableDcl tempVarDcl = new BIRVariableDcl(syncSend.receive.matchingSendsError,
                                                       this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        boolean isOnSameStrand = DEFAULT_WORKER_NAME.equals(this.env.enclFunc.workerName.value);

        this.env.enclBB.terminator = new BIRTerminator.WorkerSend(
                syncSend.pos, Names.fromString(syncSend.getChannel().channelId()), dataOp, isOnSameStrand, true, lhsOp,
                thenBB, this.currentScope);

        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    @Override
    public void visit(BLangWorkerFlushExpr flushExpr) {
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);

        //create channelDetails array
        BIRNode.ChannelDetails[] channels = new BIRNode.ChannelDetails[flushExpr.cachedWorkerSendStmts.size()];
        int i = 0;
        boolean isOnSameStrand = DEFAULT_WORKER_NAME.equals(this.env.enclFunc.workerName.value);
        for (BLangWorkerAsyncSendExpr sendStmt : flushExpr.cachedWorkerSendStmts) {
            channels[i] = new BIRNode.ChannelDetails(sendStmt.getChannel().channelId(), isOnSameStrand, true);
            i++;
        }
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(flushExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        this.env.enclBB.terminator = new BIRTerminator.Flush(flushExpr.pos, channels, lhsOp, thenBB,
                this.currentScope);
        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    private void createWait(BLangWaitExpr waitExpr) {

        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        // This only supports wait for single future and alternate wait
        List<BIROperand> exprList = new ArrayList<>();

        waitExpr.exprList.forEach(expr -> {
            expr.accept(this);
            exprList.add(this.env.targetOperand);
        });

        BIRVariableDcl tempVarDcl = new BIRVariableDcl(waitExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        this.env.enclBB.terminator = new BIRTerminator.Wait(waitExpr.pos, exprList, lhsOp, thenBB, this.currentScope);

        this.env.enclBasicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
    }

    @Override
    public void visit(BLangErrorConstructorExpr errorConstructorExpr) {
        BIRVariableDcl tempVarError = new BIRVariableDcl(errorConstructorExpr.getBType(),
                                                         this.env.nextLocalVarId(names), VarScope.FUNCTION,
                                                         VarKind.TEMP);

        this.env.enclFunc.localVars.add(tempVarError);
        BIROperand lhsOp = new BIROperand(tempVarError);

        this.env.targetOperand = lhsOp;
        List<BLangExpression> positionalArgs = errorConstructorExpr.positionalArgs;
        positionalArgs.get(0).accept(this);
        BIROperand messageOp = this.env.targetOperand;

        positionalArgs.get(1).accept(this);
        BIROperand causeOp = this.env.targetOperand;

        errorConstructorExpr.errorDetail.accept(this);
        BIROperand detailsOp = this.env.targetOperand;

        BIRNonTerminator.NewError newError =
                new BIRNonTerminator.NewError(errorConstructorExpr.pos, errorConstructorExpr.getBType(), lhsOp,
                                              messageOp, causeOp, detailsOp);
        setScopeAndEmit(newError);
        this.env.targetOperand = lhsOp;
    }

    private void createCall(BLangInvocation invocationExpr, boolean isVirtual) {
        List<BLangExpression> requiredArgs = invocationExpr.requiredArgs;
        List<BLangExpression> restArgs = invocationExpr.restArgs;
        List<BIROperand> args = new ArrayList<>(requiredArgs.size() + restArgs.size());

        for (BLangExpression requiredArg : requiredArgs) {
            if (requiredArg.getKind() != NodeKind.IGNORE_EXPR) {
                requiredArg.accept(this);
                args.add(this.env.targetOperand);
            } else {
                BIRVariableDcl birVariableDcl =
                        new BIRVariableDcl(requiredArg.getBType(), new Name("_"), VarScope.FUNCTION, VarKind.ARG);
                birVariableDcl.ignoreVariable = true;
                args.add(new BIROperand(birVariableDcl));
            }
        }

        // seems like restArgs.size() is always 1 or 0, but lets iterate just in case
        for (BLangExpression arg : restArgs) {
            arg.accept(this);
            args.add(this.env.targetOperand);
        }

        BIROperand fp = null;
        if (invocationExpr.functionPointerInvocation) {
            invocationExpr.expr.accept(this);
            fp = this.env.targetOperand;
        }
        // Create a temporary variable to store the return operation result.
        BIRVariableDcl tempVarDcl;
        if (invocationExpr.async && invocationExpr.parent instanceof BLangSimpleVariable simpleVar) {
            tempVarDcl = new BIRVariableDcl(invocationExpr.pos, invocationExpr.getBType(),
                    this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP, simpleVar.name.value);
        } else {
            tempVarDcl = new BIRVariableDcl(invocationExpr.getBType(), this.env.nextLocalVarId(names),
                    VarScope.FUNCTION, VarKind.TEMP);
        }


        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        // Lets create a block the jump after successful function return
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        this.env.enclBasicBlocks.add(thenBB);


        // TODO: make vCall a new instruction to avoid package id in vCall
        if (invocationExpr.functionPointerInvocation) {
            boolean workerDerivative = Symbols.isFlagOn(invocationExpr.symbol.flags, Flags.WORKER);
            List<BIRAnnotationAttachment> annots =
                    getBIRAnnotAttachmentsForASTAnnotAttachments(invocationExpr.annAttachments);
            this.env.enclBB.terminator = new BIRTerminator.FPCall(invocationExpr.pos, InstructionKind.FP_CALL,
                    fp, args, lhsOp, invocationExpr.async, thenBB, this.currentScope, annots);
            if (workerDerivative) {
                this.env.enclFunc.hasWorkers = true;
            }
        } else if (invocationExpr.async) {
            BInvokableSymbol bInvokableSymbol = (BInvokableSymbol) invocationExpr.symbol;
            List<BIRAnnotationAttachment> calleeAnnots = getBIRAnnotAttachments(bInvokableSymbol.getAnnotations());

            List<BIRAnnotationAttachment> annots =
                    getBIRAnnotAttachmentsForASTAnnotAttachments(invocationExpr.annAttachments);
            this.env.enclBB.terminator = new BIRTerminator.AsyncCall(invocationExpr.pos, InstructionKind.ASYNC_CALL,
                    isVirtual, invocationExpr.symbol.pkgID, getFuncName((BInvokableSymbol) invocationExpr.symbol),
                    args, lhsOp, thenBB, annots, calleeAnnots, bInvokableSymbol.getFlags(), this.currentScope);
        } else {
            BInvokableSymbol bInvokableSymbol = (BInvokableSymbol) invocationExpr.symbol;
            List<BIRAnnotationAttachment> calleeAnnots = getBIRAnnotAttachments(bInvokableSymbol.getAnnotations());

            this.env.enclBB.terminator = new BIRTerminator.Call(invocationExpr.pos, InstructionKind.CALL, isVirtual,
                    invocationExpr.symbol.pkgID, getFuncName((BInvokableSymbol) invocationExpr.symbol), args, lhsOp,
                    thenBB, calleeAnnots, bInvokableSymbol.getFlags(), this.currentScope);
        }
        this.env.enclBB = thenBB;
    }

    @Override
    public void visit(BLangReturn astReturnStmt) {
        astReturnStmt.expr.accept(this);
        BIROperand retVarRef = new BIROperand(this.env.enclFunc.returnVariable);
        setScopeAndEmit(new Move(astReturnStmt.pos, this.env.targetOperand, retVarRef));

        // Check and add a returnBB if the function does not have a returnBB.
        // A given function can have only one BB that has a return instruction.
        addReturnBB(getFunctionLastLinePos());
        if (this.env.enclBB.terminator == null) {
            this.env.unlockVars.forEach(s -> {
                int i = s.size();
                while (i > 0) {
                    BIRBasicBlock unlockBB = new BIRBasicBlock(this.env.nextBBId());
                    this.env.enclBasicBlocks.add(unlockBB);
                    BIRTerminator.Unlock unlock = new BIRTerminator.Unlock(null,  unlockBB, this.currentScope);
                    this.env.enclBB.terminator = unlock;
                    unlock.relatedLock = s.getLock(i - 1);
                    this.env.enclBB = unlockBB;
                    i--;
                }
            });

            this.env.enclBB.terminator = new BIRTerminator.GOTO(astReturnStmt.pos, this.env.returnBB,
                    this.currentScope);
            BIRBasicBlock nextBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(nextBB);
            this.env.enclBB = nextBB;
            addToTrapStack(nextBB);
        }
    }

    private BLangDiagnosticLocation getFunctionLastLinePos() {
        if (this.env.enclFunc.pos == null) {
            return null;
        }
        LineRange lineRange = this.env.enclFunc.pos.lineRange();
        LinePosition endLine = lineRange.endLine();
        return new BLangDiagnosticLocation(lineRange.fileName(), endLine.line(), endLine.line(), endLine.offset(),
                endLine.offset(), 0, 0);
    }

    @Override
    public void visit(BLangPanic panicNode) {
        panicNode.expr.accept(this);
        // Some functions will only have panic but we need to add return for them to make current algorithm work.
        addReturnBB(panicNode.pos);
        this.env.enclBB.terminator = new BIRTerminator.Panic(panicNode.pos, this.env.targetOperand, this.currentScope);

        // This basic block will contain statement that comes right after this 'if' statement.
        BIRBasicBlock unlockBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(unlockBB);
        this.env.enclBasicBlocks.add(unlockBB);
        this.env.enclBB = unlockBB;
    }

    @Override
    public void visit(BLangIf astIfStmt) {
        astIfStmt.expr.accept(this);
        BIROperand ifExprResult = this.env.targetOperand;

        // Create the basic block for the if-then block.
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        this.env.enclBasicBlocks.add(thenBB);

        // This basic block will contain statement that comes right after this 'if' statement.
        BIRBasicBlock nextBB = new BIRBasicBlock(this.env.nextBBId());

        // Add the branch instruction to the current basic block.
        // This is the end of the current basic block.
        BIRTerminator.Branch branchIns = new BIRTerminator.Branch(astIfStmt.pos, ifExprResult, thenBB, null,
                this.currentScope);
        this.env.enclBB.terminator = branchIns;

        // Visit the then-block
        this.env.enclBB = thenBB;
        astIfStmt.body.accept(this);

        // If a terminator statement has not been set for the then-block then just add it.
        if (this.env.enclBB.terminator == null) {
            this.env.enclBB.terminator = new BIRTerminator.GOTO(null, nextBB, this.currentScope);
        }

        // Check whether there exists an else-if or an else block.
        if (astIfStmt.elseStmt != null) {
            // Create a basic block for the else block.
            BIRBasicBlock elseBB = new BIRBasicBlock(this.env.nextBBId());
            addToTrapStack(elseBB);
            this.env.enclBasicBlocks.add(elseBB);
            branchIns.falseBB = elseBB;

            // Visit the else block. This could be an else-if block or an else block.
            this.env.enclBB = elseBB;
            astIfStmt.elseStmt.accept(this);

            // If a terminator statement has not been set for the else-block then just add it.
            if (this.env.enclBB.terminator == null) {
                this.env.enclBB.terminator = new BIRTerminator.GOTO(null, nextBB, this.currentScope);
            }
        } else {
            branchIns.falseBB = nextBB;
        }

        // Set the elseBB as the basic block for the rest of statements followed by this if.
        addToTrapStack(nextBB);
        this.env.enclBasicBlocks.add(nextBB);
        this.env.enclBB = nextBB;
    }

    @Override
    public void visit(BLangWhile astWhileStmt) {
        BIRBasicBlock currentEnclLoopBB = this.env.enclLoopBB;
        BIRBasicBlock currentEnclLoopEndBB = this.env.enclLoopEndBB;

        // Create a basic block for the while expression.
        BIRBasicBlock whileExprBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(whileExprBB);
        this.env.enclBasicBlocks.add(whileExprBB);

        // Insert a GOTO instruction as the terminal instruction into current basic block.
        this.env.enclBB.terminator = new BIRTerminator.GOTO(null, whileExprBB, this.currentScope);

        // Visit condition expression
        this.env.enclBB = whileExprBB;
        astWhileStmt.expr.accept(this);
        BIROperand whileExprResult = this.env.targetOperand;

        // Create the basic block for the while-body block.
        BIRBasicBlock whileBodyBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(whileBodyBB);
        this.env.enclBasicBlocks.add(whileBodyBB);

        // Create the basic block for the statements that comes after the while statement.
        BIRBasicBlock whileEndBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(whileEndBB);

        // Add the branch instruction to the while expression basic block.
        this.env.enclBB.terminator =
                new BIRTerminator.Branch(astWhileStmt.pos, whileExprResult, whileBodyBB,
                        whileEndBB, this.currentScope);

        // Visit while body
        this.env.enclBB = whileBodyBB;
        this.env.enclLoopBB = whileExprBB;
        this.env.enclLoopEndBB = whileEndBB;
        this.env.unlockVars.push(new BIRLockDetailsHolder());
        astWhileStmt.body.accept(this);
        this.env.unlockVars.pop();
        if (this.env.enclBB.terminator == null) {
            this.env.enclBB.terminator = new BIRTerminator.GOTO(null, whileExprBB, this.currentScope);
        }

        this.env.enclBasicBlocks.add(whileEndBB);
        this.env.enclBB = whileEndBB;

        this.env.enclLoopBB = currentEnclLoopBB;
        this.env.enclLoopEndBB = currentEnclLoopEndBB;
    }


    // Expressions

    @Override
    public void visit(BLangIgnoreExpr ignoreExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(ignoreExpr.getBType(),
                                                       this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
    }

    @Override
    public void visit(BLangLiteral astLiteralExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astLiteralExpr.getBType(),
                                                       this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);
        setScopeAndEmit(new BIRNonTerminator.ConstantLoad(astLiteralExpr.pos,
                                                          astLiteralExpr.value, astLiteralExpr.getBType(), toVarRef));
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangMapLiteral astMapLiteralExpr) {
        this.env.isInArrayOrStructure++;

        BType type = astMapLiteralExpr.getBType();
        this.env.isInArrayOrStructure++;

        BIRVariableDcl tempVarDcl = new BIRVariableDcl(type, this.env.nextLocalVarId(names), VarScope.FUNCTION,
                                                       VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);
        BIROperand typeDesc = new BIROperand(getOrCreateTypedescVariable(type, astMapLiteralExpr.pos));
        setScopeAndEmit(createNewStructureInst(typeDesc, generateMappingConstructorEntries(astMapLiteralExpr.fields),
                toVarRef, astMapLiteralExpr.pos));
        this.env.targetOperand = toVarRef;
        this.env.isInArrayOrStructure--;
    }

    @Override
    public void visit(BLangTypeConversionExpr astTypeConversionExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astTypeConversionExpr.getBType(),
                                                       this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        astTypeConversionExpr.expr.accept(this);
        BIROperand rhsOp = this.env.targetOperand;

        setScopeAndEmit(
                new BIRNonTerminator.TypeCast(astTypeConversionExpr.pos, toVarRef, rhsOp, toVarRef.variableDcl.type,
                        astTypeConversionExpr.checkTypes));
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangStructLiteral astStructLiteralExpr) {
        this.env.isInArrayOrStructure++;
        BType type = astStructLiteralExpr.getBType();
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astStructLiteralExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        BIROperand typeDesc = new BIROperand(getOrCreateTypedescVariable(type, astStructLiteralExpr.pos));
        List<BIRNode.BIRMappingConstructorEntry> fields =
                                                   generateMappingConstructorEntries(astStructLiteralExpr.fields);
        setScopeAndEmit(createNewStructureInst(typeDesc, fields, toVarRef, astStructLiteralExpr.pos));
        this.env.targetOperand = toVarRef;
        this.env.isInArrayOrStructure--;
    }

    @Override
    public void visit(BLangTypeInit connectorInitExpr) {
        BType exprType = connectorInitExpr.getBType();
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(exprType, this.env.nextLocalVarId(names), VarScope.FUNCTION,
                VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);
        BType objectType = getEffectiveObjectType(exprType);
        BTypeSymbol objectTypeSymbol = Types.getImpliedType(objectType).tsymbol;
        BIRNonTerminator.NewInstance instruction;
        if (isInSameModule(objectTypeSymbol, env.enclPkg.packageID)) {
            BIRTypeDefinition def = typeDefs.get(objectTypeSymbol);
            instruction = new BIRNonTerminator.NewInstance(connectorInitExpr.pos, def, toVarRef, objectType);
        } else {
            String objectName = objectTypeSymbol.name.value;
            instruction = new BIRNonTerminator.NewInstance(connectorInitExpr.pos, objectTypeSymbol.pkgID, objectName,
                    toVarRef, objectType);
        }
        setScopeAndEmit(instruction);
        this.env.targetOperand = toVarRef;
    }

    private boolean isInSameModule(BSymbol symbol, PackageID packageID) {
        return symbol.pkgID.equals(packageID);
    }

    @Override
    public void visit(BLangSimpleVarRef.BLangFieldVarRef fieldVarRef) {
    }

    @Override
    public void visit(BLangArrayLiteral astArrayLiteralExpr) {
        generateListConstructorExpr(astArrayLiteralExpr);
    }

    @Override
    public void visit(BLangTupleLiteral tupleLiteral) {
        generateListConstructorExpr(tupleLiteral);
    }

    @Override
    public void visit(BLangGroupExpr groupExpr) {
        groupExpr.expression.accept(this);
    }

    @Override
    public void visit(BLangJSONArrayLiteral jsonArrayLiteralExpr) {
        generateListConstructorExpr(jsonArrayLiteralExpr);
    }

    @Override
    public void visit(BLangMapAccessExpr astMapAccessExpr) {
        boolean variableStore = this.varAssignment;
        this.varAssignment = false;
        BIROperand rhsOp = this.env.targetOperand;

        astMapAccessExpr.expr.accept(this);
        BIROperand varRefRegIndex = this.env.targetOperand;

        astMapAccessExpr.indexExpr.accept(this);
        BIROperand keyRegIndex = this.env.targetOperand;
        if (variableStore) {
            setScopeAndEmit(
                    new BIRNonTerminator.FieldAccess(astMapAccessExpr.pos, InstructionKind.MAP_STORE, varRefRegIndex,
                            keyRegIndex, rhsOp, astMapAccessExpr.isStoreOnCreation));
            return;
        }
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astMapAccessExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand tempVarRef = new BIROperand(tempVarDcl);

        setScopeAndEmit(new BIRNonTerminator.FieldAccess(astMapAccessExpr.pos, InstructionKind.MAP_LOAD, tempVarRef,
                keyRegIndex, varRefRegIndex, astMapAccessExpr.optionalFieldAccess,
                                              astMapAccessExpr.isLValue && !astMapAccessExpr.leafNode));
        this.env.targetOperand = tempVarRef;
        this.varAssignment = false;
    }

    @Override
    public void visit(BLangTableAccessExpr astTableAccessExpr) {
        boolean variableStore = this.varAssignment;
        this.varAssignment = false;
        BIROperand rhsOp = this.env.targetOperand;

        astTableAccessExpr.expr.accept(this);
        BIROperand varRefRegIndex = this.env.targetOperand;

        astTableAccessExpr.indexExpr.accept(this);
        BIROperand keyRegIndex = this.env.targetOperand;
        if (variableStore) {
            setScopeAndEmit(new BIRNonTerminator.FieldAccess(astTableAccessExpr.pos, InstructionKind.TABLE_STORE,
                    varRefRegIndex, keyRegIndex, rhsOp));
            return;
        }
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astTableAccessExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand tempVarRef = new BIROperand(tempVarDcl);

        setScopeAndEmit(new BIRNonTerminator.FieldAccess(astTableAccessExpr.pos, InstructionKind.TABLE_LOAD, tempVarRef,
                keyRegIndex, varRefRegIndex));
        this.env.targetOperand = tempVarRef;
        this.varAssignment = false;
    }

    @Override
    public void visit(BLangStructFieldAccessExpr astStructFieldAccessExpr) {
        generateMappingAccess(astStructFieldAccessExpr, astStructFieldAccessExpr.optionalFieldAccess);
    }

    @Override
    public void visit(BLangJSONAccessExpr astJSONFieldAccessExpr) {
        if (Types.getImpliedType(astJSONFieldAccessExpr.indexExpr.getBType()).tag == TypeTags.INT) {
            generateArrayAccess(astJSONFieldAccessExpr);
            return;
        }

        generateMappingAccess(astJSONFieldAccessExpr, astJSONFieldAccessExpr.optionalFieldAccess);
    }

    @Override
    public void visit(BLangDynamicArgExpr dynamicParamExpr) {
        dynamicParamExpr.condition.accept(this);
        dynamicParamExpr.conditionalArgument.accept(this);
    }

    @Override
    public void visit(BLangStringAccessExpr stringAccessExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(stringAccessExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand tempVarRef = new BIROperand(tempVarDcl);

        stringAccessExpr.expr.accept(this);
        BIROperand varRefRegIndex = this.env.targetOperand;

        stringAccessExpr.indexExpr.accept(this);
        BIROperand keyRegIndex = this.env.targetOperand;

        setScopeAndEmit(new BIRNonTerminator.FieldAccess(stringAccessExpr.pos, InstructionKind.STRING_LOAD, tempVarRef,
                                              keyRegIndex, varRefRegIndex));
        this.env.targetOperand = tempVarRef;
    }

    @Override
    public void visit(BLangArrayAccessExpr astArrayAccessExpr) {
        generateArrayAccess(astArrayAccessExpr);
    }

    @Override
    public void visit(BLangIndexBasedAccess.BLangTupleAccessExpr tupleAccessExpr) {
        generateArrayAccess(tupleAccessExpr);
    }

    @Override
    public void visit(BLangIsLikeExpr isLikeExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(symTable.booleanType,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        isLikeExpr.expr.accept(this);
        BIROperand exprIndex = this.env.targetOperand;

        setScopeAndEmit(new BIRNonTerminator.IsLike(isLikeExpr.pos, isLikeExpr.typeNode.getBType(), toVarRef,
                                                    exprIndex));

        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangTypeTestExpr typeTestExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(symTable.booleanType,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        typeTestExpr.expr.accept(this);
        BIROperand exprIndex = this.env.targetOperand;

        setScopeAndEmit(
                new BIRNonTerminator.TypeTest(typeTestExpr.pos, typeTestExpr.typeNode.getBType(), toVarRef, exprIndex));

        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangLocalVarRef astVarRefExpr) {
        boolean variableStore = this.varAssignment;
        this.varAssignment = false;
        BSymbol varSymbol = astVarRefExpr.symbol;

        if (variableStore) {
            if (astVarRefExpr.symbol.name != Names.IGNORE) {
                BIROperand varRef = new BIROperand(this.env.symbolVarMap.get(varSymbol));
                setScopeAndEmit(new Move(astVarRefExpr.pos, this.env.targetOperand, varRef));
            }
        } else {
            BIRVariableDcl tempVarDcl = new BIRVariableDcl(varSymbol.type,
                    this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
            this.env.enclFunc.localVars.add(tempVarDcl);
            BIROperand tempVarRef = new BIROperand(tempVarDcl);

            BIRVariableDcl varDecl = this.env.symbolVarMap.get(varSymbol);

            BIROperand fromVarRef = new BIROperand(varDecl);

            setScopeAndEmit(new Move(astVarRefExpr.pos, fromVarRef, tempVarRef));
            this.env.targetOperand = tempVarRef;
        }
        this.varAssignment = variableStore;
    }

    @Override
    public void visit(BLangPackageVarRef astPackageVarRefExpr) {
        boolean variableStore = this.varAssignment;
        this.varAssignment = false;

        if (variableStore) {
            if (astPackageVarRefExpr.symbol.name != Names.IGNORE) {
                BIROperand varRef = new BIROperand(getVarRef(astPackageVarRefExpr));
                setScopeAndEmit(new Move(astPackageVarRefExpr.pos, this.env.targetOperand, varRef));
            }
        } else {
            if (this.env.isInArrayOrStructure > 0) {
                BIRVariableDcl tempVarDcl = new BIRVariableDcl(astPackageVarRefExpr.getBType(),
                        this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
                this.env.enclFunc.localVars.add(tempVarDcl);
                BIROperand tempVarRef = new BIROperand(tempVarDcl);
                BIROperand fromVarRef = new BIROperand(getVarRef(astPackageVarRefExpr));
                setScopeAndEmit(new Move(astPackageVarRefExpr.pos, fromVarRef, tempVarRef));
                this.env.targetOperand = tempVarRef;
            } else {
                this.env.targetOperand = new BIROperand(getVarRef(astPackageVarRefExpr));
            }
        }
        this.varAssignment = variableStore;
    }

    private BIRGlobalVariableDcl getVarRef(BLangPackageVarRef astPackageVarRefExpr) {
        BSymbol symbol = astPackageVarRefExpr.symbol;
        BIRGlobalVariableDcl globalVarDcl = this.globalVarMap.get(symbol);
        if (globalVarDcl == null) {
            globalVarDcl = new BIRGlobalVariableDcl(astPackageVarRefExpr.pos, symbol.flags, symbol.type, symbol.pkgID,
                    symbol.name, symbol.getOriginalName(), VarScope.GLOBAL, VarKind.CONSTANT, symbol.name.getValue(),
                    symbol.origin.toBIROrigin());
            this.globalVarMap.put(symbol, globalVarDcl);
        }

        if (!isInSameModule(astPackageVarRefExpr.varSymbol, env.enclPkg.packageID) ||
                env.enclPkg.packageID.isTestPkg) {
            this.env.enclPkg.importedGlobalVarsDummyVarDcls.add(globalVarDcl);
        }
        return globalVarDcl;
    }

    @Override
    public void visit(BLangBinaryExpr astBinaryExpr) {
        astBinaryExpr.lhsExpr.accept(this);
        BIROperand rhsOp1 = this.env.targetOperand;

        astBinaryExpr.rhsExpr.accept(this);
        BIROperand rhsOp2 = this.env.targetOperand;

        // Create a temporary variable to store the binary operation result.
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astBinaryExpr.getBType(),
                                                       this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);
        this.env.targetOperand = lhsOp;

        // Create binary instruction
        BinaryOp binaryIns = new BinaryOp(astBinaryExpr.pos, getBinaryInstructionKind(astBinaryExpr.opKind),
                lhsOp, rhsOp1, rhsOp2);
        setScopeAndEmit(binaryIns);
    }

    @Override
    public void visit(BLangUnaryExpr unaryExpr) {
        unaryExpr.expr.accept(this);
        BIROperand rhsOp = this.env.targetOperand;

        // Create a temporary variable to store the unary operation result.
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(unaryExpr.getBType(),
                                                       this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand lhsOp = new BIROperand(tempVarDcl);

        if (OperatorKind.ADD.equals(unaryExpr.operator) || OperatorKind.UNTAINT.equals(unaryExpr.operator)) {
            setScopeAndEmit(new Move(unaryExpr.pos, rhsOp, lhsOp));
            this.env.targetOperand = lhsOp;
            return;
        }

        UnaryOP unaryIns = new UnaryOP(unaryExpr.pos, getUnaryInstructionKind(unaryExpr.operator), lhsOp, rhsOp);
        setScopeAndEmit(unaryIns);
        this.env.targetOperand = lhsOp;
    }

    @Override
    public void visit(BLangTrapExpr trapExpr) {
        BIRBasicBlock trapBB = new BIRBasicBlock(this.env.nextBBId());
        this.env.enclBasicBlocks.add(trapBB);
        this.env.enclBB.terminator = new BIRTerminator.GOTO(trapExpr.pos, trapBB, this.currentScope);
        this.env.enclBB = trapBB;
        this.env.trapBlocks.push(new ArrayList<>());
        addToTrapStack(trapBB);

        trapExpr.expr.accept(this);

        List<BIRBasicBlock> trappedBlocks = this.env.trapBlocks.pop();
        // Create new block for instructions after trap.
        BIRBasicBlock nextBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(nextBB);
        env.enclBasicBlocks.add(nextBB);
        if (this.env.enclBB.terminator == null) {
            this.env.enclBB.terminator = new BIRTerminator.GOTO(trapExpr.pos, nextBB, this.currentScope);
        }

        env.enclFunc.errorTable.add(new BIRNode.BIRErrorEntry(trappedBlocks.getFirst(), trappedBlocks.getLast(),
                env.targetOperand, nextBB));

        this.env.enclBB = nextBB;
    }

    @Override
    public void visit(BLangWaitExpr waitExpr) {
        createWait(waitExpr);
    }

    @Override
    public void visit(BLangWaitForAllExpr.BLangWaitLiteral waitLiteral) {
        this.env.isInArrayOrStructure++;
        BIRBasicBlock thenBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(thenBB);
        BType type = waitLiteral.getBType();
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(type, this.env.nextLocalVarId(names), VarScope.FUNCTION,
                                                       VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);
        Location pos = waitLiteral.pos;

        BIROperand typeDesc = new BIROperand(getOrCreateTypedescVariable(type, pos));
        setScopeAndEmit(createNewStructureInst(typeDesc, new ArrayList<>(), toVarRef, pos));
        this.env.targetOperand = toVarRef;

        List<String> keys = new ArrayList<>();
        List<BIROperand> valueExprs = new ArrayList<>();
        for (BLangWaitForAllExpr.BLangWaitKeyValue keyValue : waitLiteral.keyValuePairs) {
            keys.add(keyValue.key.value);
            BLangExpression expr = keyValue.valueExpr != null ? keyValue.valueExpr : keyValue.keyExpr;
            expr.accept(this);
            BIROperand valueRegIndex = this.env.targetOperand;
            valueExprs.add(valueRegIndex);
        }
        this.env.enclBB.terminator = new BIRTerminator.WaitAll(pos, toVarRef, keys,
                valueExprs, thenBB, this.currentScope);
        this.env.targetOperand = toVarRef;
        this.env.enclFunc.basicBlocks.add(thenBB);
        this.env.enclBB = thenBB;
        this.env.isInArrayOrStructure--;
    }

    @Override
    public void visit(BLangIsAssignableExpr assignableExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(symTable.booleanType, this.env.nextLocalVarId(names),
                VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        assignableExpr.lhsExpr.accept(this);
        BIROperand exprIndex = this.env.targetOperand;

        setScopeAndEmit(
                new BIRNonTerminator.TypeTest(assignableExpr.pos, assignableExpr.targetType, toVarRef, exprIndex));
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLQName xmlQName) {
        BIRVariableDcl tempVarDcl =
                new BIRVariableDcl(symTable.anyType, this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        // If the QName is use outside XML, treat it as string.
        if (!xmlQName.isUsedInXML) {
            String qName = xmlQName.namespaceURI == null ? xmlQName.localname.value
                    : ("{" + xmlQName.namespaceURI + "}" + xmlQName.localname);
            generateStringLiteral(qName);
            return;
        }

        // Else, treat it as QName
        BIROperand nsURIIndex = generateStringLiteral(xmlQName.namespaceURI);
        BIROperand localnameIndex = generateStringLiteral(xmlQName.localname.value);
        BIROperand prefixIndex = generateStringLiteral(xmlQName.prefix.value);
        BIRNonTerminator.NewXMLQName newXMLQName =
                new BIRNonTerminator.NewXMLQName(xmlQName.pos, toVarRef, localnameIndex, nsURIIndex, prefixIndex);
        setScopeAndEmit(newXMLQName);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLElementLiteral xmlElementLiteral) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(xmlElementLiteral.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        // Visit in-line namespace declarations. These needs to be visited first before visiting the
        // attributes, start and end tag names of the element.
        xmlElementLiteral.inlineNamespaces.forEach(xmlns -> xmlns.accept(this));

        // Create start tag name
        BLangExpression startTagName = (BLangExpression) xmlElementLiteral.getStartTagName();
        startTagName.accept(this);
        BIROperand startTagNameIndex = this.env.targetOperand;

        // Create default namespace uri
        BIROperand defaultNsURIVarRef = generateNamespaceRef(xmlElementLiteral.defaultNsSymbol, xmlElementLiteral.pos);

        // Create xml element
        BIRNonTerminator.NewXMLElement newXMLElement =
                new BIRNonTerminator.NewXMLElement(xmlElementLiteral.pos, toVarRef, startTagNameIndex,
                                                   defaultNsURIVarRef,
                                                   Symbols.isFlagOn(xmlElementLiteral.getBType().getFlags(),
                                                                    Flags.READONLY));
        setScopeAndEmit(newXMLElement);

        // Populate the XML by adding namespace declarations, attributes and children
        populateXML(xmlElementLiteral, toVarRef);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLAttribute attribute) {
        BIROperand xmlVarRef = this.env.targetOperand;

        attribute.name.accept(this);
        BIROperand attrNameOp = this.env.targetOperand;

        attribute.value.accept(this);
        BIROperand attrValueOp = this.env.targetOperand;
        setScopeAndEmit(new BIRNonTerminator.FieldAccess(attribute.pos, InstructionKind.XML_ATTRIBUTE_STORE, xmlVarRef,
                attrNameOp, attrValueOp));
    }

    @Override
    public void visit(BLangXMLSequenceLiteral xmlSequenceLiteral) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(xmlSequenceLiteral.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);

        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        BIRNonTerminator.NewXMLSequence newXMLSequence =
                new BIRNonTerminator.NewXMLSequence(xmlSequenceLiteral.pos, toVarRef);

        setScopeAndEmit(newXMLSequence);
        populateXMLSequence(xmlSequenceLiteral, toVarRef);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLTextLiteral xmlTextLiteral) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(xmlTextLiteral.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        xmlTextLiteral.concatExpr.accept(this);
        BIROperand xmlTextIndex = this.env.targetOperand;

        BIRNonTerminator.NewXMLText newXMLElement =
                new BIRNonTerminator.NewXMLText(xmlTextLiteral.pos, toVarRef, xmlTextIndex);
        setScopeAndEmit(newXMLElement);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLCommentLiteral xmlCommentLiteral) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(xmlCommentLiteral.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        xmlCommentLiteral.concatExpr.accept(this);
        BIROperand xmlCommentIndex = this.env.targetOperand;

        BIRNonTerminator.NewXMLComment newXMLComment =
                new BIRNonTerminator.NewXMLComment(xmlCommentLiteral.pos, toVarRef, xmlCommentIndex,
                                                   Symbols.isFlagOn(xmlCommentLiteral.getBType().getFlags(),
                                                                    Flags.READONLY));
        setScopeAndEmit(newXMLComment);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLProcInsLiteral xmlProcInsLiteral) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(xmlProcInsLiteral.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        xmlProcInsLiteral.dataConcatExpr.accept(this);
        BIROperand dataIndex = this.env.targetOperand;

        xmlProcInsLiteral.target.accept(this);
        BIROperand targetIndex = this.env.targetOperand;

        BIRNonTerminator.NewXMLProcIns newXMLProcIns =
                new BIRNonTerminator.NewXMLProcIns(xmlProcInsLiteral.pos, toVarRef, dataIndex, targetIndex,
                                                   Symbols.isFlagOn(xmlProcInsLiteral.getBType().getFlags(),
                                                                    Flags.READONLY));
        setScopeAndEmit(newXMLProcIns);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangXMLQuotedString xmlQuotedString) {
        xmlQuotedString.concatExpr.accept(this);
    }

    @Override
    public void visit(BLangXMLNSStatement xmlnsStmtNode) {
        xmlnsStmtNode.xmlnsDecl.accept(this);
    }

    @Override
    public void visit(BLangXMLNS xmlnsNode) {
        // do nothing
    }

    @Override
    public void visit(BLangLocalXMLNS xmlnsNode) {
        generateXMLNamespace(xmlnsNode);
    }

    @Override
    public void visit(BLangPackageXMLNS xmlnsNode) {
        generateXMLNamespace(xmlnsNode);
    }

    @Override
    public void visit(BLangXMLAccessExpr xmlAccessExpr) {
        generateMappingAccess(xmlAccessExpr, false);
    }

    @Override
    public void visit(BLangTypedescExpr accessExpr) {
        this.env.targetOperand = new BIROperand(getOrCreateTypedescVariable(accessExpr.resolvedType, accessExpr.pos));
    }

    @Override
    public void visit(BLangTableConstructorExpr tableConstructorExpr) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(tableConstructorExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);

        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);

        BLangArrayLiteral keySpecifierLiteral = new BLangArrayLiteral();
        keySpecifierLiteral.pos = tableConstructorExpr.pos;
        keySpecifierLiteral.setBType(symTable.stringArrayType);
        keySpecifierLiteral.exprs = new ArrayList<>();
        BTableType type = (BTableType) Types.getImpliedType(tableConstructorExpr.getBType());

        if (!type.fieldNameList.isEmpty()) {
            type.fieldNameList.forEach(col -> {
                BLangLiteral colLiteral = new BLangLiteral();
                colLiteral.pos = tableConstructorExpr.pos;
                colLiteral.setBType(symTable.stringType);
                colLiteral.value = col;
                keySpecifierLiteral.exprs.add(colLiteral);
            });
        }

        keySpecifierLiteral.accept(this);
        BIROperand keyColOp = this.env.targetOperand;

        BLangArrayLiteral dataLiteral = new BLangArrayLiteral();
        dataLiteral.pos = tableConstructorExpr.pos;
        dataLiteral.setBType(
                new BArrayType(symTable.typeEnv(),
                        ((BTableType) Types.getImpliedType(tableConstructorExpr.getBType())).constraint));
        dataLiteral.exprs = new ArrayList<>(tableConstructorExpr.recordLiteralList);
        dataLiteral.accept(this);
        BIROperand dataOp = this.env.targetOperand;

        setScopeAndEmit(
                new BIRNonTerminator.NewTable(tableConstructorExpr.pos, tableConstructorExpr.getBType(), toVarRef,
                                              keyColOp, dataOp));

        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangSimpleVarRef.BLangTypeLoad typeLoad) {
        BType type = typeLoad.symbol.tag == SymTag.TYPE_DEF ?
                ((BTypeDefinitionSymbol) typeLoad.symbol).referenceType : typeLoad.symbol.type;
        env.targetOperand = new BIROperand(getOrCreateTypedescVariable(type, typeLoad.pos));
    }

    @Override
    public void visit(BLangBreak breakStmt) {
        BIRLockDetailsHolder toUnlock = this.env.unlockVars.peek();
        if (!toUnlock.isEmpty()) {
            BIRBasicBlock goToBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(goToBB);
            this.env.enclBB.terminator = new BIRTerminator.GOTO(breakStmt.pos, goToBB, this.currentScope);
            this.env.enclBB = goToBB;
        }

        int numLocks = toUnlock.size();
        while (numLocks > 0) {
            BIRBasicBlock unlockBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(unlockBB);
            BIRTerminator.Unlock unlock = new BIRTerminator.Unlock(null, unlockBB, this.currentScope);
            this.env.enclBB.terminator = unlock;
            unlock.relatedLock = toUnlock.getLock(numLocks - 1);
            this.env.enclBB = unlockBB;
            numLocks--;
        }
        this.env.enclBB.terminator = new BIRTerminator.GOTO(breakStmt.pos, this.env.enclLoopEndBB, this.currentScope);
    }

    @Override
    public void visit(BLangContinue continueStmt) {
        BIRLockDetailsHolder toUnlock = this.env.unlockVars.peek();
        if (!toUnlock.isEmpty()) {
            BIRBasicBlock goToBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(goToBB);
            this.env.enclBB.terminator = new BIRTerminator.GOTO(continueStmt.pos, goToBB, this.currentScope);
            this.env.enclBB = goToBB;
        }
        int numLocks = toUnlock.size();
        while (numLocks > 0) {
            BIRBasicBlock unlockBB = new BIRBasicBlock(this.env.nextBBId());
            this.env.enclBasicBlocks.add(unlockBB);
            BIRTerminator.Unlock unlock = new BIRTerminator.Unlock(null,  unlockBB, this.currentScope);
            this.env.enclBB.terminator = unlock;
            unlock.relatedLock = toUnlock.getLock(numLocks - 1);
            this.env.enclBB = unlockBB;
            numLocks--;
        }

        this.env.enclBB.terminator = new BIRTerminator.GOTO(continueStmt.pos, this.env.enclLoopBB, this.currentScope);
    }

    @Override
    public void visit(BLangFunctionVarRef fpVarRef) {
        generateFPVarRef(fpVarRef, (BInvokableSymbol) fpVarRef.symbol);
    }

    @Override
    public void visit(BLangStructFunctionVarRef structFpVarRef) {
        generateFPVarRef(structFpVarRef, (BInvokableSymbol) structFpVarRef.symbol);
    }

    @Override
    public void visit(BLangLockStmt lockStmt) {
        BIRBasicBlock lockedBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(lockedBB);
        this.env.enclBasicBlocks.add(lockedBB);
        BIRTerminator.Lock lock = new BIRTerminator.Lock(lockStmt.pos, lockedBB, this.currentScope);
        this.env.enclBB.terminator = lock;
        lockStmtMap.put(lockStmt, lock); // Populate the cache.
        this.env.unlockVars.peek().addLock(lock);
        populateBirLockWithGlobalVars(lockStmt);
        this.env.enclBB = lockedBB;

    }

    private void populateBirLockWithGlobalVars(BLangLockStmt lockStmt) {
        for (BVarSymbol globalVar : lockStmt.lockVariables) {
            BIRGlobalVariableDcl birGlobalVar = this.globalVarMap.get(globalVar);

            // If null query the dummy map for dummy variables.
            if (birGlobalVar == null) {
                birGlobalVar = dummyGlobalVarMapForLocks.computeIfAbsent(globalVar, k ->
                        new BIRGlobalVariableDcl(null, globalVar.flags, globalVar.type, globalVar.pkgID,
                                                 globalVar.name, globalVar.getOriginalName(), VarScope.GLOBAL,
                                                 VarKind.GLOBAL, globalVar.name.value,
                                                 globalVar.origin.toBIROrigin()));
            }

            ((BIRTerminator.Lock) this.env.enclBB.terminator).lockVariables.add(birGlobalVar);
        }
    }

    @Override
    public void visit(BLangUnLockStmt unLockStmt) {
        BIRLockDetailsHolder lockDetailsHolder = this.env.unlockVars.peek();
        if (lockDetailsHolder.isEmpty()) {
            return;
        }
        BIRBasicBlock unLockedBB = new BIRBasicBlock(this.env.nextBBId());
        addToTrapStack(unLockedBB);
        this.env.enclBasicBlocks.add(unLockedBB);
        this.env.enclBB.terminator = new BIRTerminator.Unlock(unLockStmt.pos, unLockedBB, this.currentScope);
        ((BIRTerminator.Unlock) this.env.enclBB.terminator).relatedLock = lockStmtMap.get(unLockStmt.relatedLock);
        this.env.enclBB = unLockedBB;

        lockDetailsHolder.removeLastLock();
    }

    @Override
    public void visit(BLangRegExpTemplateLiteral regExpTemplateLiteral) {
        BIROperand toVarRef = createVarRefOperand(regExpTemplateLiteral.getBType());

        regExpTemplateLiteral.reDisjunction.accept(this);
        BIROperand reDisjunction = this.env.targetOperand;

        BIRNonTerminator.NewRegExp newRegExp = new BIRNonTerminator.NewRegExp(regExpTemplateLiteral.pos, toVarRef,
                reDisjunction);
        setScopeAndEmit(newRegExp);
        this.env.targetOperand = toVarRef;
    }

    private BIRNonTerminator.NewStructure createNewStructureInst(BIROperand typeDesc,
                                                                 List<BIRNode.BIRMappingConstructorEntry> fields,
                                                                 BIROperand toVarRef, Location pos) {
        return new BIRNonTerminator.NewStructure(pos, toVarRef, typeDesc, fields);
    }

    private BIRVariableDcl getOrCreateTypedescVariable(BType type, Location pos) {
        if (type.tag == TypeTags.TYPEREFDESC) {
            BType referredType = ((BTypeReferenceType) type).referredType;
            int tag = referredType.tag;
            if (tag == TypeTags.RECORD) {
                type = referredType;
            }
        }

        BIRVariableDcl variableDcl = findInLocalSymbolVarMap(type, env.symbolVarMap);
        if (variableDcl != null && variableDcl.initialized) {
            return variableDcl;
        }

        variableDcl = findInGlobalSymbolVarMap(type, this.globalVarMap);
        if (variableDcl != null && variableDcl.initialized) {
            return variableDcl;
        }

        variableDcl = findInDifferentPackage(type);
        if (variableDcl != null && variableDcl.initialized) {
            return variableDcl;
        }

        // Create new type desc instruction if the typedesc variable is not found
        // TODO: we need to handle duplicate typedesc creation for some anonymous type cases.
        //   eg: function params ie `foo(record {int a;})`
        createNewTypedescInst(type, pos);
        return this.env.targetOperand.variableDcl;
    }

    private BIRVariableDcl findInDifferentPackage(BType type) {
        BTypeSymbol typeSymbol = type.tsymbol;
        if (typeSymbol == null || typeSymbol.owner.tag != SymTag.PACKAGE ||
                isInSameModule(typeSymbol, env.enclPkg.packageID)) {
            return null;
        }
        BPackageSymbol packageSymbol = (BPackageSymbol) typeSymbol.owner;
        BSymbol symbol = packageSymbol.scope.entries.entrySet().stream()
                .filter(entry -> entry.getKey().value.startsWith(TYPEDESC))
                .map(entry -> entry.getValue().symbol)
                .filter(entrySymbol -> {
                    BType symbolType = entrySymbol.getType();
                    return symbolType.getKind() == TypeKind.TYPEDESC &&
                            ((BTypedescType) symbolType).constraint == type;
                })
                .findFirst()
                .orElse(null);

        return symbol != null ? getVarRef(createPackageVarRef(symbol)) : null;
    }

    private BLangPackageVarRef createPackageVarRef(BSymbol symbol) {
        BLangPackageVarRef packageVarRef = new BLangPackageVarRef((BVarSymbol) symbol);
        packageVarRef.pos = symbol.pos;
        packageVarRef.setBType(symbol.getType());
        return packageVarRef;
    }

    private BIRVariableDcl findInLocalSymbolVarMap(BType type, Map<BSymbol, BIRVariableDcl> varMap) {
        for (Map.Entry<BSymbol, BIRVariableDcl> entry : varMap.entrySet()) {
            if (isMatchingTypeDescSymbol(entry.getKey(), type)) {
                return varMap.get(entry.getKey());
            }
        }
        return null;
    }

    private BIRVariableDcl findInGlobalSymbolVarMap(BType type, Map<BSymbol, BIRGlobalVariableDcl> varMap) {
        for (Map.Entry<BSymbol, BIRGlobalVariableDcl> entry : varMap.entrySet()) {
            BSymbol varSymbol = entry.getKey();
            if (isMatchingTypeDescSymbol(varSymbol, type)) {
                BIRGlobalVariableDcl globalVarDcl =  varMap.get(varSymbol);
                if (!isInSameModule(varSymbol, env.enclPkg.packageID) || env.enclPkg.packageID.isTestPkg) {
                    this.env.enclPkg.importedGlobalVarsDummyVarDcls.add(globalVarDcl);
                }
                return globalVarDcl;
            }
        }
        return null;
    }

    private boolean isMatchingTypeDescSymbol(BSymbol symbol, BType targetType) {
        // ATM there is no proper way to generate the full name of the target typedesc variable for the anonymous
        // types because `tsymbol.name.value` is empty for those.
        // Hence, we are using the TYPEDESC prefix to identify the typedesc variables and then match type constant type.
        if (!symbol.name.value.startsWith(TYPEDESC)) {
            return false;
        }
        BType constraint = ((BTypedescType) symbol.type).constraint;
        return targetType == constraint;
    }

    private void createNewTypedescInst(BType resolveType, Location position) {
        BTypeSymbol typeSymbol = resolveType.tsymbol;
        BIRVariableDcl tempVarDcl = createTempVariable();
        BIROperand toVarRef = new BIROperand(tempVarDcl);
        BIRNonTerminator.NewTypeDesc newTypeDesc = createNewTypeDesc(position, toVarRef, resolveType, typeSymbol);
        this.env.targetOperand = toVarRef;
        setScopeAndEmit(newTypeDesc);
    }

    private BIRVariableDcl createTempVariable() {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(symTable.typeDesc, this.env.nextLocalVarId(names),
                VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        return tempVarDcl;
    }

    private BIRNonTerminator.NewTypeDesc createNewTypeDesc(Location position, BIROperand toVarRef, BType resolveType,
                                                           BTypeSymbol typeSymbol) {
        List<BIROperand> closures = Collections.emptyList();
        if (typeSymbol != null && typeSymbol.annotations != null) {
            BVarSymbol typeAnnotations = typeSymbol.annotations;
            BIRVariableDcl annotations = this.env.symbolVarMap.get(typeAnnotations);
            BIROperand symbolVarOperand = annotations != null ?
                    new BIROperand(annotations) : new BIROperand(this.globalVarMap.get(typeAnnotations));
            return new BIRNonTerminator.NewTypeDesc(position, toVarRef, resolveType, closures, symbolVarOperand);
        } else {
            return new BIRNonTerminator.NewTypeDesc(position, toVarRef, resolveType, closures);
        }
    }

    private BIRNonTerminator.NewArray createNewArrayInst(BIROperand typeDesc,
                                                         List<BIRNode.BIRListConstructorEntry> initialValues,
                                                         BType listConstructorExprType, BIROperand sizeOp,
                                                         BIROperand toVarRef, Location pos) {
        return new BIRNonTerminator.NewArray(pos, listConstructorExprType, toVarRef, typeDesc, sizeOp, initialValues);
    }

    @Override
    public void visit(BLangReDisjunction reDisjunction) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        BLangArrayLiteral seqList = new BLangArrayLiteral();
        seqList.pos = reDisjunction.pos;
        seqList.setBType(symTable.arrayAnydataType);
        seqList.exprs = new ArrayList<>();
        seqList.exprs.addAll(reDisjunction.sequenceList);

        seqList.accept(this);
        BIROperand sequences = this.env.targetOperand;

        BIRNonTerminator.NewReDisjunction newRegExp = new BIRNonTerminator.NewReDisjunction(reDisjunction.pos,
                sequences, toVarRef);
        setScopeAndEmit(newRegExp);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReSequence reSequence) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        BLangArrayLiteral terms = new BLangArrayLiteral();
        terms.pos = reSequence.pos;
        terms.setBType(symTable.arrayAnydataType);
        terms.exprs = new ArrayList<>();
        terms.exprs.addAll(reSequence.termList);

        terms.accept(this);
        BIROperand sequences = this.env.targetOperand;

        BIRNonTerminator.NewReSequence newReSequence =
                new BIRNonTerminator.NewReSequence(reSequence.pos, sequences, toVarRef);
        setScopeAndEmit(newReSequence);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReAssertion reAssertion) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reAssertion.assertion.accept(this);
        BIROperand assertion = this.env.targetOperand;

        BIRNonTerminator.NewReAssertion newReAssertion = new BIRNonTerminator.NewReAssertion(reAssertion.pos,
                assertion, toVarRef);
        setScopeAndEmit(newReAssertion);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReAtomQuantifier reAtomQuantifier) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reAtomQuantifier.atom.accept(this);
        BIROperand atom = this.env.targetOperand;

        reAtomQuantifier.quantifier.accept(this);
        BIROperand quantifier = this.env.targetOperand;

        BIRNonTerminator.NewReAtomQuantifier newReAtomQuantifier =
                new BIRNonTerminator.NewReAtomQuantifier(reAtomQuantifier.pos,
                toVarRef, atom, quantifier);
        setScopeAndEmit(newReAtomQuantifier);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReQuantifier reQuantifier) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reQuantifier.quantifier.accept(this);
        BIROperand quantifier = this.env.targetOperand;

        reQuantifier.nonGreedyChar.accept(this);
        BIROperand nonGreedyChar = this.env.targetOperand;

        BIRNonTerminator.NewReQuantifier newReQuantifier =
                new BIRNonTerminator.NewReQuantifier(reQuantifier.pos, toVarRef, quantifier, nonGreedyChar);
        setScopeAndEmit(newReQuantifier);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReAtomCharOrEscape reLiteralCharOrEscape) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reLiteralCharOrEscape.charOrEscape.accept(this);
        BIROperand charOrEscape = this.env.targetOperand;

        BIRNonTerminator.NewReLiteralCharOrEscape newReLiteralCharOrEscape =
                new BIRNonTerminator.NewReLiteralCharOrEscape(reLiteralCharOrEscape.pos, toVarRef, charOrEscape);
        setScopeAndEmit(newReLiteralCharOrEscape);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReCharacterClass reCharacterClass) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reCharacterClass.characterClassStart.accept(this);
        BIROperand classStart = this.env.targetOperand;

        reCharacterClass.negation.accept(this);
        BIROperand negation = this.env.targetOperand;

        reCharacterClass.charSet.accept(this);
        BIROperand charSet = this.env.targetOperand;

        reCharacterClass.characterClassEnd.accept(this);
        BIROperand classEnd = this.env.targetOperand;

        BIRNonTerminator.NewReCharacterClass newReCharacterClass =
                new BIRNonTerminator.NewReCharacterClass(reCharacterClass.pos, toVarRef, classStart,
                        negation, charSet, classEnd);
        setScopeAndEmit(newReCharacterClass);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReCharSet reCharSet) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        BLangArrayLiteral atoms = new BLangArrayLiteral();
        atoms.pos = reCharSet.pos;
        atoms.setBType(symTable.arrayAnydataType);
        atoms.exprs = new ArrayList<>();
        atoms.exprs.addAll(reCharSet.charSetAtoms);

        atoms.accept(this);
        BIROperand charSetAtoms = this.env.targetOperand;

        BIRNonTerminator.NewReCharSet newReCharSet = new BIRNonTerminator.NewReCharSet(reCharSet.pos, toVarRef,
                charSetAtoms);
        setScopeAndEmit(newReCharSet);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReCharSetRange reCharSetRange) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reCharSetRange.lhsCharSetAtom.accept(this);
        BIROperand lhsCharSetAtom = this.env.targetOperand;

        reCharSetRange.dash.accept(this);
        BIROperand dash = this.env.targetOperand;

        reCharSetRange.rhsCharSetAtom.accept(this);
        BIROperand rhsCharSetAtom = this.env.targetOperand;

        BIRNonTerminator.NewReCharSetRange newReCharSet = new BIRNonTerminator.NewReCharSetRange(reCharSetRange.pos,
                toVarRef, lhsCharSetAtom, dash, rhsCharSetAtom);
        setScopeAndEmit(newReCharSet);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReCapturingGroups reCapturingGroups) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reCapturingGroups.openParen.accept(this);
        BIROperand openParen = this.env.targetOperand;

        reCapturingGroups.flagExpr.accept(this);
        BIROperand flagExpr = this.env.targetOperand;

        reCapturingGroups.disjunction.accept(this);
        BIROperand reDisjunction = this.env.targetOperand;

        reCapturingGroups.closeParen.accept(this);
        BIROperand closeParen = this.env.targetOperand;

        BIRNonTerminator.NewReCapturingGroup newReCapturingGroup =
                new BIRNonTerminator.NewReCapturingGroup(reCapturingGroups.pos, toVarRef, openParen, flagExpr,
                        reDisjunction, closeParen);
        setScopeAndEmit(newReCapturingGroup);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReFlagExpression reFlagExpression) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reFlagExpression.questionMark.accept(this);
        BIROperand questionMark = this.env.targetOperand;

        reFlagExpression.flagsOnOff.accept(this);
        BIROperand flagsOnOff = this.env.targetOperand;

        reFlagExpression.colon.accept(this);
        BIROperand colon = this.env.targetOperand;

        BIRNonTerminator.NewReFlagExpression newReFlagExpression =
                new BIRNonTerminator.NewReFlagExpression(reFlagExpression.pos, toVarRef, questionMark, flagsOnOff,
                        colon);
        setScopeAndEmit(newReFlagExpression);
        this.env.targetOperand = toVarRef;
    }

    @Override
    public void visit(BLangReFlagsOnOff reFlagsOnOff) {
        BIROperand toVarRef = createVarRefOperand(symTable.anydataType);

        reFlagsOnOff.flags.accept(this);
        BIROperand flags = this.env.targetOperand;

        BIRNonTerminator.NewReFlagOnOff newReFlagOnOff = new BIRNonTerminator.NewReFlagOnOff(reFlagsOnOff.pos,
                toVarRef, flags);
        setScopeAndEmit(newReFlagOnOff);
        this.env.targetOperand = toVarRef;
    }

    private BIROperand createVarRefOperand(BType type) {
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(type, this.env.nextLocalVarId(names), VarScope.FUNCTION,
                VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        return new BIROperand(tempVarDcl);
    }

    private void setScopeAndEmit(BIRNonTerminator instruction) {
        instruction.scope = this.currentScope;
        this.env.enclBB.instructions.add(instruction);
    }

    private InstructionKind getBinaryInstructionKind(OperatorKind opKind) {
        return switch (opKind) {
            case ADD -> InstructionKind.ADD;
            case SUB -> InstructionKind.SUB;
            case MUL -> InstructionKind.MUL;
            case DIV -> InstructionKind.DIV;
            case MOD -> InstructionKind.MOD;
            case EQUAL, EQUALS -> InstructionKind.EQUAL;
            case NOT_EQUAL -> InstructionKind.NOT_EQUAL;
            case GREATER_THAN -> InstructionKind.GREATER_THAN;
            case GREATER_EQUAL -> InstructionKind.GREATER_EQUAL;
            case LESS_THAN -> InstructionKind.LESS_THAN;
            case LESS_EQUAL -> InstructionKind.LESS_EQUAL;
            case AND -> InstructionKind.AND;
            case OR -> InstructionKind.OR;
            case REF_EQUAL -> InstructionKind.REF_EQUAL;
            case REF_NOT_EQUAL -> InstructionKind.REF_NOT_EQUAL;
            case CLOSED_RANGE -> InstructionKind.CLOSED_RANGE;
            case HALF_OPEN_RANGE -> InstructionKind.HALF_OPEN_RANGE;
            case ANNOT_ACCESS -> InstructionKind.ANNOT_ACCESS;
            case BITWISE_AND -> InstructionKind.BITWISE_AND;
            case BITWISE_OR -> InstructionKind.BITWISE_OR;
            case BITWISE_XOR -> InstructionKind.BITWISE_XOR;
            case BITWISE_LEFT_SHIFT -> InstructionKind.BITWISE_LEFT_SHIFT;
            case BITWISE_RIGHT_SHIFT -> InstructionKind.BITWISE_RIGHT_SHIFT;
            case BITWISE_UNSIGNED_RIGHT_SHIFT -> InstructionKind.BITWISE_UNSIGNED_RIGHT_SHIFT;
            default -> throw new IllegalStateException("unsupported binary operation: " + opKind.value());
        };
    }

    private InstructionKind getUnaryInstructionKind(OperatorKind opKind) {
        return switch (opKind) {
            case TYPEOF -> InstructionKind.TYPEOF;
            case NOT -> InstructionKind.NOT;
            case SUB -> InstructionKind.NEGATE;
            case ADD -> InstructionKind.MOVE;
            default -> throw new IllegalStateException("unsupported unary operator: " + opKind.value());
        };
    }

    private void generateListConstructorExpr(BLangListConstructorExpr listConstructorExpr) {
        this.env.isInArrayOrStructure++;
        // Emit create array instruction
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(listConstructorExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand toVarRef = new BIROperand(tempVarDcl);
        BType listConstructorExprType = listConstructorExpr.getBType();

        List<BLangExpression> exprs = listConstructorExpr.exprs;
        BType referredType = Types.getImpliedType(listConstructorExprType);
        BIROperand typeDesc = null;
        boolean isRecordArray = false;
        if (referredType.tag == TypeTags.TUPLE) {
            typeDesc = new BIROperand(getOrCreateTypedescVariable(listConstructorExprType, listConstructorExpr.pos));
        } else {
            BType elementType = ((BArrayType) referredType).getElementType();
            if (Types.getImpliedType(elementType).tag == TypeTags.RECORD) {
                typeDesc = new BIROperand(getOrCreateTypedescVariable(elementType, elementType.tsymbol.pos));
                isRecordArray = true;
            }
        }

        long size = -1L;
        if (referredType.tag == TypeTags.ARRAY &&
                ((BArrayType) referredType).state != BArrayState.OPEN) {
            size = ((BArrayType) referredType).getSize();
        } else if (referredType.tag == TypeTags.TUPLE) {
            size = exprs.size();
        }

        BLangLiteral literal = new BLangLiteral();
        literal.pos = listConstructorExpr.pos;
        literal.value = size;
        literal.setBType(symTable.intType);
        literal.accept(this);
        BIROperand sizeOp = this.env.targetOperand;

        List<BIRNode.BIRListConstructorEntry> initialValues = new ArrayList<>(exprs.size());

        for (BLangExpression expr : exprs) {
            if (expr.getKind() == NodeKind.LIST_CONSTRUCTOR_SPREAD_OP) {
                BLangListConstructorSpreadOpExpr spreadMember = (BLangListConstructorSpreadOpExpr) expr;
                spreadMember.expr.accept(this);
                initialValues.add(new BIRNode.BIRListConstructorSpreadMemberEntry(this.env.targetOperand));
            } else {
                expr.accept(this);
                initialValues.add(new BIRNode.BIRListConstructorExprEntry(this.env.targetOperand));
            }
        }

        if (referredType.tag == TypeTags.TUPLE) {
            setScopeAndEmit(createNewArrayInst(typeDesc, initialValues, listConstructorExprType, sizeOp, toVarRef,
                                               listConstructorExpr.pos));

        } else {
            BIRNonTerminator.NewArray newArrayIns = new BIRNonTerminator.NewArray(listConstructorExpr.pos,
                    listConstructorExprType, toVarRef, sizeOp, initialValues);
            // If the referredType is an array type and the element type is record type, then we need to set
            // the element typeDesc which will be used to initialize the `ArrayValueImpl`
            if (isRecordArray) {
                newArrayIns.elementTypedescOp = typeDesc;
            }
            setScopeAndEmit(newArrayIns);
        }
        this.env.targetOperand = toVarRef;
        this.env.isInArrayOrStructure--;
    }

    private void generateArrayAccess(BLangIndexBasedAccess astArrayAccessExpr) {
        boolean variableStore = this.varAssignment;
        this.varAssignment = false;

        BIROperand rhsOp = this.env.targetOperand;

        astArrayAccessExpr.expr.accept(this);
        BIROperand varRefRegIndex = this.env.targetOperand;

        astArrayAccessExpr.indexExpr.accept(this);
        BIROperand keyRegIndex = this.env.targetOperand;

        if (variableStore) {
            setScopeAndEmit(new BIRNonTerminator.FieldAccess(astArrayAccessExpr.pos, InstructionKind.ARRAY_STORE,
                    varRefRegIndex, keyRegIndex, rhsOp));
            return;
        }
        BIRVariableDcl tempVarDcl = new BIRVariableDcl(astArrayAccessExpr.getBType(), this.env.nextLocalVarId(names),
                                                       VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarDcl);
        BIROperand tempVarRef = new BIROperand(tempVarDcl);

        setScopeAndEmit(new BIRNonTerminator.FieldAccess(astArrayAccessExpr.pos, InstructionKind.ARRAY_LOAD, tempVarRef,
                                              keyRegIndex, varRefRegIndex, false,
                                              astArrayAccessExpr.isLValue && !astArrayAccessExpr.leafNode));
        this.env.targetOperand = tempVarRef;

        this.varAssignment = false;
    }

    private void generateMappingAccess(BLangIndexBasedAccess astIndexBasedAccessExpr, boolean except) {
        boolean variableStore = this.varAssignment;
        this.varAssignment = false;
        InstructionKind insKind;
        BType astAccessExprExprType = Types.getImpliedType(astIndexBasedAccessExpr.expr.getBType());
        if (variableStore) {
            BIROperand rhsOp = this.env.targetOperand;

            astIndexBasedAccessExpr.expr.accept(this);
            BIROperand varRefRegIndex = this.env.targetOperand;

            astIndexBasedAccessExpr.indexExpr.accept(this);
            BIROperand keyRegIndex = this.env.targetOperand;

            if (astIndexBasedAccessExpr.getKind() == NodeKind.XML_ATTRIBUTE_ACCESS_EXPR) {
                insKind = InstructionKind.XML_ATTRIBUTE_STORE;
                keyRegIndex = getQNameOP(astIndexBasedAccessExpr.indexExpr, keyRegIndex);
            } else if (SemTypeHelper.isSubtypeSimple(astAccessExprExprType, PredefinedType.OBJECT)) {
                insKind = InstructionKind.OBJECT_STORE;
            } else {
                insKind = InstructionKind.MAP_STORE;
            }
            setScopeAndEmit(
                    new BIRNonTerminator.FieldAccess(astIndexBasedAccessExpr.pos, insKind, varRefRegIndex, keyRegIndex,
                            rhsOp, astIndexBasedAccessExpr.isStoreOnCreation));
        } else {
            BIRVariableDcl tempVarDcl = new BIRVariableDcl(astIndexBasedAccessExpr.getBType(),
                                                           this.env.nextLocalVarId(names),
                                                           VarScope.FUNCTION, VarKind.TEMP);
            this.env.enclFunc.localVars.add(tempVarDcl);
            BIROperand tempVarRef = new BIROperand(tempVarDcl);

            astIndexBasedAccessExpr.expr.accept(this);
            BIROperand varRefRegIndex = this.env.targetOperand;

            astIndexBasedAccessExpr.indexExpr.accept(this);
            BIROperand keyRegIndex = this.env.targetOperand;

            if (astIndexBasedAccessExpr.getKind() == NodeKind.XML_ATTRIBUTE_ACCESS_EXPR) {
                insKind = InstructionKind.XML_ATTRIBUTE_LOAD;
                keyRegIndex = getQNameOP(astIndexBasedAccessExpr.indexExpr, keyRegIndex);
            } else if (types.isAssignable(astAccessExprExprType, symTable.xmlType)) {
                generateXMLAccess((BLangXMLAccessExpr) astIndexBasedAccessExpr, tempVarRef, varRefRegIndex,
                        keyRegIndex);
                this.varAssignment = false;
                return;
            } else if (SemTypeHelper.isSubtypeSimple(astAccessExprExprType, PredefinedType.OBJECT)) {
                insKind = InstructionKind.OBJECT_LOAD;
            } else {
                insKind = InstructionKind.MAP_LOAD;
            }
            setScopeAndEmit(
                    new BIRNonTerminator.FieldAccess(astIndexBasedAccessExpr.pos, insKind, tempVarRef, keyRegIndex,
                            varRefRegIndex, except,
                            astIndexBasedAccessExpr.isLValue && !astIndexBasedAccessExpr.leafNode));
            this.env.targetOperand = tempVarRef;
        }
        this.varAssignment = variableStore;
    }

    private BType getEffectiveObjectType(BType objType) {
        BType type = Types.getImpliedType(objType);
        if (type.tag == TypeTags.UNION) {
            return ((BUnionType) type).getMemberTypes().stream()
                    .filter(t -> Types.getImpliedType(t).tag != TypeTags.ERROR)
                    .findFirst()
                    .orElse(symTable.noType);
        }
        return objType;
    }

    private BIROperand generateStringLiteral(String value) {
        BLangLiteral prefixLiteral = (BLangLiteral) TreeBuilder.createLiteralExpression();
        prefixLiteral.value = value;

        if (value == null) {
            prefixLiteral.setBType(symTable.nilType);
        } else {
            prefixLiteral.setBType(symTable.stringType);
        }

        prefixLiteral.accept(this);
        return this.env.targetOperand;
    }

    private void generateXMLNamespace(BLangXMLNS xmlnsNode) {
        BIRVariableDcl birVarDcl = new BIRVariableDcl(xmlnsNode.pos, symTable.stringType,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.LOCAL, null);
        this.env.enclFunc.localVars.add(birVarDcl);
        this.env.symbolVarMap.put(xmlnsNode.symbol, birVarDcl);

        // Visit the namespace uri expression.
        xmlnsNode.namespaceURI.accept(this);

        // Create a variable reference and
        BIROperand varRef = new BIROperand(birVarDcl);
        setScopeAndEmit(new Move(xmlnsNode.pos, this.env.targetOperand, varRef));
    }

    private BIROperand generateNamespaceRef(BXMLNSSymbol nsSymbol, Location pos) {
        if (nsSymbol == null) {
            return generateStringLiteral(null);
        }

        // global-level, object-level, record-level namespace declarations will not have
        // any interpolated content. hence the namespace URI is statically known.
        long ownerTag = nsSymbol.owner.tag;
        if ((ownerTag & SymTag.PACKAGE) == SymTag.PACKAGE ||
                (ownerTag & SymTag.OBJECT) == SymTag.OBJECT ||
                (ownerTag & SymTag.RECORD) == SymTag.RECORD) {
            return generateStringLiteral(nsSymbol.namespaceURI);
        }

        BIRVariableDcl nsURIVarDcl = new BIRVariableDcl(symTable.stringType, this.env.nextLocalVarId(names),
                VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(nsURIVarDcl);
        BIROperand nsURIVarRef = new BIROperand(nsURIVarDcl);

        BIRVariableDcl varDecl = this.env.symbolVarMap.get(nsSymbol);
        BIROperand fromVarRef = new BIROperand(varDecl);
        setScopeAndEmit(new Move(pos, fromVarRef, nsURIVarRef));
        return nsURIVarRef;
    }

    private void populateXMLSequence(BLangXMLSequenceLiteral xmlSequenceLiteral, BIROperand toVarRef) {
        for (BLangExpression xmlItem : xmlSequenceLiteral.xmlItems) {
            xmlItem.accept(this);
            BIROperand childOp = this.env.targetOperand;
            setScopeAndEmit(
                    new BIRNonTerminator.XMLAccess(xmlItem.pos, InstructionKind.XML_SEQ_STORE, toVarRef, childOp));
        }
    }

    private void populateXML(BLangXMLElementLiteral xmlElementLiteral, BIROperand toVarRef) {
        // Add namespaces decelerations visible to this element.
        xmlElementLiteral.namespacesInScope.forEach((name, symbol) -> {
            BLangXMLQName nsQName = new BLangXMLQName(name.getValue(), XMLConstants.XMLNS_ATTRIBUTE);
            nsQName.setBType(symTable.stringType);
            nsQName.accept(this);
            BIROperand nsQNameIndex = this.env.targetOperand;
            BIROperand nsURIIndex = generateNamespaceRef(symbol, xmlElementLiteral.pos);
            setScopeAndEmit(new BIRNonTerminator.FieldAccess(xmlElementLiteral.pos, InstructionKind.XML_ATTRIBUTE_STORE,
                    toVarRef, nsQNameIndex, nsURIIndex));
        });

        // Add attributes
        xmlElementLiteral.attributes.forEach(attribute -> {
            this.env.targetOperand = toVarRef;
            attribute.accept(this);
        });

        // Add children
        xmlElementLiteral.modifiedChildren.forEach(child -> {
            child.accept(this);
            BIROperand childOp = this.env.targetOperand;
            setScopeAndEmit(
                    new BIRNonTerminator.XMLAccess(child.pos, InstructionKind.XML_SEQ_STORE, toVarRef, childOp));
        });
    }

    private BIROperand getQNameOP(BLangExpression qnameExpr, BIROperand keyRegIndex) {
        if (qnameExpr.getKind() == NodeKind.XML_QNAME) {
            return keyRegIndex;
        }

        BIRVariableDcl tempQNameVarDcl = new BIRVariableDcl(symTable.anyType,
                this.env.nextLocalVarId(names), VarScope.FUNCTION, VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempQNameVarDcl);
        BIROperand qnameVarRef = new BIROperand(tempQNameVarDcl);
        setScopeAndEmit(new BIRNonTerminator.NewStringXMLQName(qnameExpr.pos, qnameVarRef, keyRegIndex));
        return qnameVarRef;
    }

    // todo: remove/move this, we no longer support xml access like this
    private void generateXMLAccess(BLangXMLAccessExpr xmlAccessExpr, BIROperand tempVarRef,
                                   BIROperand varRefRegIndex, BIROperand keyRegIndex) {
        this.env.targetOperand = tempVarRef;
        InstructionKind insKind;
        if (xmlAccessExpr.fieldType == FieldKind.ALL) {
            setScopeAndEmit(new BIRNonTerminator.XMLAccess(xmlAccessExpr.pos, InstructionKind.XML_LOAD_ALL, tempVarRef,
                    varRefRegIndex));
            return;
        } else if (Types.getImpliedType(xmlAccessExpr.indexExpr.getBType()).tag == TypeTags.STRING) {
            insKind = InstructionKind.XML_LOAD;
        } else {
            insKind = InstructionKind.XML_SEQ_LOAD;
        }

        setScopeAndEmit(
                new BIRNonTerminator.FieldAccess(xmlAccessExpr.pos, insKind, tempVarRef, keyRegIndex, varRefRegIndex));
    }

    private void generateFPVarRef(BLangExpression fpVarRef, BInvokableSymbol funcSymbol) {
        // fpload instruction
        BIRVariableDcl tempVarLambda =
                new BIRVariableDcl(fpVarRef.getBType(), this.env.nextLocalVarId(names), VarScope.FUNCTION,
                                   VarKind.TEMP);
        this.env.enclFunc.localVars.add(tempVarLambda);
        BIROperand lhsOp = new BIROperand(tempVarLambda);
        Name funcName = getFuncName(funcSymbol);

        List<BIRVariableDcl> params = new ArrayList<>();

        funcSymbol.params.forEach(param -> {
            BIRVariableDcl birVarDcl = new BIRVariableDcl(fpVarRef.pos, param.type, this.env.nextLambdaVarId(names),
                    VarScope.FUNCTION, VarKind.ARG, null);
            params.add(birVarDcl);
        });

        BVarSymbol restParam = funcSymbol.restParam;
        if (restParam != null) {
            BIRVariableDcl birVarDcl = new BIRVariableDcl(fpVarRef.pos, restParam.type, this.env.nextLambdaVarId(names),
                    VarScope.FUNCTION, VarKind.ARG, null);
            params.add(birVarDcl);
        }
        setScopeAndEmit(new BIRNonTerminator.FPLoad(fpVarRef.pos, funcSymbol.pkgID, funcName, lhsOp, params,
                new ArrayList<>(), funcSymbol.type, funcSymbol.strandName, funcSymbol.schedulerPolicy,
                funcSymbol.pkgID));
        this.env.targetOperand = lhsOp;
    }

    private void addToTrapStack(BIRBasicBlock birBasicBlock) {
        if (this.env.trapBlocks.isEmpty()) {
            return;
        }
        this.env.trapBlocks.peek().add(birBasicBlock);
    }

    private List<BIRNode.BIRMappingConstructorEntry> generateMappingConstructorEntries(
            List<RecordLiteralNode.RecordField> fields) {

        List<BIRNode.BIRMappingConstructorEntry> initialValues = new ArrayList<>(fields.size());

        for (RecordLiteralNode.RecordField field : fields) {
            if (field.isKeyValueField()) {
                BLangRecordKeyValueField keyValueField = (BLangRecordKeyValueField) field;
                keyValueField.key.expr.accept(this);
                BIROperand keyOperand = this.env.targetOperand;

                keyValueField.valueExpr.accept(this);
                BIROperand valueOperand = this.env.targetOperand;
                initialValues.add(new BIRNode.BIRMappingConstructorKeyValueEntry(keyOperand, valueOperand));
                continue;
            }

            BLangRecordLiteral.BLangRecordSpreadOperatorField spreadField =
                    (BLangRecordLiteral.BLangRecordSpreadOperatorField) field;
            spreadField.expr.accept(this);
            initialValues.add(new BIRNode.BIRMappingConstructorSpreadFieldEntry(this.env.targetOperand));
        }
        return initialValues;
    }


    // For invocation expressions, there is no symbol to attach the annotation annotation symbols to. So we
    // add the attachments symbols to the attachment expression and extract them here.
    private List<BIRAnnotationAttachment> getBIRAnnotAttachmentsForASTAnnotAttachments(
            List<BLangAnnotationAttachment> astAnnotAttachments) {
        List<BIRAnnotationAttachment> annotationAttachments = new ArrayList<>(astAnnotAttachments.size());
        for (BLangAnnotationAttachment astAnnotAttachment : astAnnotAttachments) {
            annotationAttachments.add(createBIRAnnotationAttachment(astAnnotAttachment.annotationAttachmentSymbol));
        }
        return annotationAttachments;
    }

    private void addReturnBB(Location pos) {
        if (this.env.returnBB == null) {
            BIRBasicBlock returnBB = new BIRBasicBlock(this.env.nextBBId());
            addToTrapStack(returnBB);
            returnBB.terminator = new BIRTerminator.Return(pos);
            this.env.returnBB = returnBB;
        }
    }
}
