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
package org.wso2.ballerinalang.compiler.bir.model;

import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.model.elements.PackageID;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SchedulerPolicy;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.util.Name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.wso2.ballerinalang.compiler.bir.model.InstructionKind.RECORD_DEFAULT_FP_LOAD;

/**
 * A non-terminating instruction.
 * <p>
 * Non-terminating instructions do not terminate a basic block.
 *
 * @since 0.980.0
 */
public abstract class BIRNonTerminator extends BIRAbstractInstruction implements BIRInstruction {

    public BIRNonTerminator(Location pos, InstructionKind kind) {
        super(pos, kind);
    }

    /**
     * A move instruction that copy a value from variable to a temp location, vice versa.
     * <p>
     * e.g., _1 = move _2
     *
     * @since 0.980.0
     */
    public static class Move extends BIRNonTerminator implements BIRAssignInstruction {
        public BIROperand rhsOp;

        public Move(Location pos, BIROperand fromOperand, BIROperand toOperand) {
            super(pos, InstructionKind.MOVE);
            this.rhsOp = fromOperand;
            this.lhsOp = toOperand;
            toOperand.variableDcl.initialized = true;
        }

        @Override
        public BIROperand getLhsOperand() {
            return lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.rhsOp = operands[0];
        }
    }

    /**
     * A binary operator instruction.
     * <p>
     * e.g., _1 = add _2 _3
     *
     * @since 0.980.0
     */
    public static class BinaryOp extends BIRNonTerminator implements BIRAssignInstruction {
        public BIROperand rhsOp1;
        public BIROperand rhsOp2;

        public BinaryOp(Location pos,
                        InstructionKind kind,
                        BIROperand lhsOp,
                        BIROperand rhsOp1,
                        BIROperand rhsOp2) {
            super(pos, kind);
            this.lhsOp = lhsOp;
            this.rhsOp1 = rhsOp1;
            this.rhsOp2 = rhsOp2;
        }

        @Override
        public BIROperand getLhsOperand() {
            return lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp1, rhsOp2};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.rhsOp1 = operands[0];
            this.rhsOp2 = operands[1];
        }
    }

    /**
     * A unary operator instruction.
     * <p>
     * e.g., _1 = minus _2
     *
     * @since 0.980.0
     */
    public static class UnaryOP extends BIRNonTerminator implements BIRAssignInstruction {
        public BIROperand rhsOp;

        public UnaryOP(Location pos, InstructionKind kind, BIROperand lhsOp, BIROperand rhsOp) {
            super(pos, kind);
            this.lhsOp = lhsOp;
            this.rhsOp = rhsOp;
        }

        @Override
        public BIROperand getLhsOperand() {
            return lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.rhsOp = operands[0];
        }
    }

    /**
     * A constant value load instruction.
     * <p>
     * e.g., _1 = const 10 (int)
     *
     * @since 0.980.0
     */
    public static class ConstantLoad extends BIRNonTerminator implements BIRAssignInstruction {
        public Object value;
        public BType type;

        public ConstantLoad(Location pos, Object value, BType type, BIROperand lhsOp) {
            super(pos, InstructionKind.CONST_LOAD);
            this.value = value;
            this.type = type;
            this.lhsOp = lhsOp;
        }

        @Override
        public BIROperand getLhsOperand() {
            return lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[0];
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            // do nothing
        }
    }

    /**
     * A new map instruction.
     * <p>
     * e.g., map a = {}
     *
     * @since 0.980.0
     */
    public static class NewStructure extends BIRNonTerminator {
        public BIROperand rhsOp;
        public List<BIRMappingConstructorEntry> initialValues;

        public NewStructure(Location pos, BIROperand lhsOp, BIROperand rhsOp,
                            List<BIRMappingConstructorEntry> initialValues) {
            super(pos, InstructionKind.NEW_STRUCTURE);
            this.lhsOp = lhsOp;
            this.rhsOp = rhsOp;
            this.initialValues = initialValues;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            BIROperand[] operands = new BIROperand[2 * (initialValues.size()) + 1];
            int i = 0;
            operands[i++] = rhsOp;
            for (BIRMappingConstructorEntry mappingEntry : initialValues) {
                if (mappingEntry instanceof BIRMappingConstructorKeyValueEntry entry) {
                    operands[i++] = entry.keyOp;
                    operands[i++] = entry.valueOp;
                } else {
                    BIRMappingConstructorSpreadFieldEntry entry = (BIRMappingConstructorSpreadFieldEntry) mappingEntry;
                    operands[i++] = entry.exprOp;
                }
            }
            operands = Arrays.copyOf(operands, i);
            return operands;
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.rhsOp = operands[0];
            int i = 1;
            for (BIRMappingConstructorEntry mappingEntry : initialValues) {
                if (mappingEntry instanceof BIRMappingConstructorKeyValueEntry entry) {
                    entry.keyOp = operands[i++];
                    entry.valueOp = operands[i++];
                } else {
                    BIRMappingConstructorSpreadFieldEntry entry = (BIRMappingConstructorSpreadFieldEntry) mappingEntry;
                    entry.exprOp = operands[i++];
                }
            }
        }
    }

    /**
     * A new instruction.
     * <p>
     * e.g., object{int i;}  a = new;
     *
     * @since 0.995.0
     */
    public static class NewInstance extends BIRNonTerminator {
        public final boolean isExternalDef;
        public final PackageID externalPackageId;
        public BIRTypeDefinition def;
        public final String objectName;
        public final BType expectedType;

        public NewInstance(Location pos, BIRTypeDefinition def, BIROperand lhsOp, BType expectedType) {
            super(pos, InstructionKind.NEW_INSTANCE);
            this.lhsOp = lhsOp;
            this.def = def;
            this.objectName = null;
            this.externalPackageId = null;
            this.isExternalDef = false;
            this.expectedType = expectedType;
        }

        public NewInstance(Location pos, PackageID externalPackageId, String objectName,
                           BIROperand lhsOp, BType expectedType) {
            super(pos, InstructionKind.NEW_INSTANCE);
            this.objectName = objectName;
            this.lhsOp = lhsOp;
            this.def = null;
            this.externalPackageId = externalPackageId;
            this.isExternalDef = true;
            this.expectedType = expectedType;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[0];
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            // do nothing
        }
    }

    /**
     * A new array instruction.
     * <p>
     * e.g., int[] a = {}
     *
     * @since 0.980.0
     */
    public static class NewArray extends BIRNonTerminator {
        public BIROperand typedescOp;
        public BIROperand elementTypedescOp;
        public BIROperand sizeOp;
        public BType type;
        public List<BIRListConstructorEntry> values;

        public NewArray(Location location, BType type, BIROperand lhsOp, BIROperand sizeOp,
                        List<BIRListConstructorEntry> values) {
            super(location, InstructionKind.NEW_ARRAY);
            this.type = type;
            this.lhsOp = lhsOp;
            this.sizeOp = sizeOp;
            this.values = values;
        }

        public NewArray(Location location, BType type, BIROperand lhsOp, BIROperand typedescOp, BIROperand sizeOp,
                        List<BIRListConstructorEntry> values) {
            this(location, type, lhsOp, sizeOp, values);
            this.typedescOp = typedescOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            List<BIROperand> operands = new ArrayList<>();
            Optional.ofNullable(typedescOp).ifPresent(operands::add);
            Optional.ofNullable(elementTypedescOp).ifPresent(operands::add);
            operands.add(sizeOp);
            for (BIRListConstructorEntry listValueEntry : values) {
                operands.add(listValueEntry.exprOp);
            }

            return operands.toArray(new BIROperand[0]);
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            int i = 0;
            if (typedescOp != null) {
                typedescOp = operands[i++];
            }
            if (elementTypedescOp != null) {
                elementTypedescOp = operands[i++];
            }
            sizeOp = operands[i++];
            for (BIRListConstructorEntry listValueEntry : values) {
                listValueEntry.exprOp = operands[i++];
            }
        }
    }

    /**
     * A field access expression.
     * <p>
     * e.g., a["b"] = 10 (int)
     * or
     * _1 = mapload _3 _2
     *
     * @since 0.980.0
     */
    public static class FieldAccess extends BIRNonTerminator {
        public BIROperand keyOp;
        public BIROperand rhsOp;
        public boolean optionalFieldAccess = false;
        public boolean fillingRead = false;
        public boolean onInitialization = false;

        public FieldAccess(Location pos, InstructionKind kind,
                           BIROperand lhsOp, BIROperand keyOp, BIROperand rhsOp) {
            super(pos, kind);
            this.lhsOp = lhsOp;
            this.keyOp = keyOp;
            this.rhsOp = rhsOp;
        }

        public FieldAccess(Location pos, InstructionKind kind,
                           BIROperand lhsOp, BIROperand keyOp, BIROperand rhsOp, boolean onInitialization) {
            super(pos, kind);
            this.lhsOp = lhsOp;
            this.keyOp = keyOp;
            this.rhsOp = rhsOp;
            this.onInitialization = onInitialization;
        }

        public FieldAccess(Location pos, InstructionKind kind,
                           BIROperand lhsOp, BIROperand keyOp, BIROperand rhsOp, boolean optionalFieldAccess,
                           boolean fillingRead) {
            super(pos, kind);
            this.lhsOp = lhsOp;
            this.keyOp = keyOp;
            this.rhsOp = rhsOp;
            this.optionalFieldAccess = optionalFieldAccess;
            this.fillingRead = fillingRead;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{keyOp, rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            keyOp = operands[0];
            rhsOp = operands[1];
        }
    }

    /**
     * An error constructor expression.
     * <p>
     * error(reason as string, detail as map)
     *
     * @since 0.995.0
     */
    public static class NewError extends BIRNonTerminator {

        public BType type;

        public BIROperand messageOp;
        public BIROperand causeOp;
        public BIROperand detailOp;

        public NewError(Location location, BType type, BIROperand lhsOp, BIROperand messageOp,
                        BIROperand causeOp, BIROperand detailOp) {
            super(location, InstructionKind.NEW_ERROR);
            this.type = type;
            this.lhsOp = lhsOp;
            this.messageOp = messageOp;
            this.causeOp = causeOp;
            this.detailOp = detailOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{messageOp, causeOp, detailOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            messageOp = operands[0];
            causeOp = operands[1];
            detailOp = operands[2];
        }
    }

    /**
     * A type cast expression.
     * <p>
     * e.g., int a = cast(int) b;
     *
     * @since 0.980.0
     */
    public static class TypeCast extends BIRNonTerminator {
        public BIROperand rhsOp;
        public BType type;
        public boolean checkTypes;

        public TypeCast(Location location, BIROperand lhsOp, BIROperand rhsOp, BType castType,
                        boolean checkTypes) {
            super(location, InstructionKind.TYPE_CAST);
            this.lhsOp = lhsOp;
            this.rhsOp = rhsOp;
            this.type = castType;
            this.checkTypes = checkTypes;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            rhsOp = operands[0];
        }
    }

    /**
     * A is like instruction.
     * <p>
     * e.g., a isLike b
     *
     * @since 0.980.0
     */
    public static class IsLike extends BIRNonTerminator {
        public BIROperand rhsOp;
        public BType type;

        public IsLike(Location pos, BType type, BIROperand lhsOp, BIROperand rhsOp) {
            super(pos, InstructionKind.IS_LIKE);
            this.type = type;
            this.lhsOp = lhsOp;
            this.rhsOp = rhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            rhsOp = operands[0];
        }
    }

    /**
     * A type test instruction.
     * <p>
     * e.g., a is int
     *
     * @since 0.980.0
     */
    public static class TypeTest extends BIRNonTerminator {
        public BIROperand rhsOp;
        public BType type;

        public TypeTest(Location pos, BType type, BIROperand lhsOp, BIROperand rhsOp) {
            super(pos, InstructionKind.TYPE_TEST);
            this.type = type;
            this.lhsOp = lhsOp;
            this.rhsOp = rhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            rhsOp = operands[0];
        }
    }

    /**
     * New XML element instruction.
     *
     * @since 0.995.0
     */
    public static class NewXMLElement extends BIRNonTerminator {
        public BIROperand startTagOp;
        public BIROperand defaultNsURIOp;
        public boolean readonly;

        public NewXMLElement(Location location, BIROperand lhsOp, BIROperand startTagOp,
                             BIROperand defaultNsURIOp, boolean readonly) {
            super(location, InstructionKind.NEW_XML_ELEMENT);
            this.lhsOp = lhsOp;
            this.startTagOp = startTagOp;
            this.defaultNsURIOp = defaultNsURIOp;
            this.readonly = readonly;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{startTagOp, defaultNsURIOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            startTagOp = operands[0];
            defaultNsURIOp = operands[1];
        }
    }

    /**
     * New XML QName instruction.
     * <p>
     * e.g.: {@code ns0:foo}
     *
     * @since 0.995.0
     */
    public static class NewXMLQName extends BIRNonTerminator {
        public BIROperand localnameOp;
        public BIROperand nsURIOp;
        public BIROperand prefixOp;

        public NewXMLQName(Location pos, BIROperand lhsOp, BIROperand localnameOp, BIROperand nsURIOp,
                           BIROperand prefixOp) {
            super(pos, InstructionKind.NEW_XML_QNAME);
            this.lhsOp = lhsOp;
            this.localnameOp = localnameOp;
            this.nsURIOp = nsURIOp;
            this.prefixOp = prefixOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{localnameOp, nsURIOp, prefixOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            localnameOp = operands[0];
            nsURIOp = operands[1];
            prefixOp = operands[2];
        }
    }

    /**
     * New XML QName from a string.
     * <p>
     * e.g.: {@code "{http://nsuri/}foo"}
     *
     * @since 0.995.0
     */
    public static class NewStringXMLQName extends BIRNonTerminator {
        public BIROperand stringQNameOP;

        public NewStringXMLQName(Location pos, BIROperand lhsOp, BIROperand stringQName) {
            super(pos, InstructionKind.NEW_STRING_XML_QNAME);
            this.lhsOp = lhsOp;
            this.stringQNameOP = stringQName;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{stringQNameOP};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            stringQNameOP = operands[0];
        }
    }

    /**
     * New xml sequence instruction.
     *
     * @since 2.0.0
     */
    public static class NewXMLSequence extends BIRNonTerminator {

        public NewXMLSequence(Location location, BIROperand lhsOp) {
            super(location, InstructionKind.NEW_XML_SEQUENCE);
            this.lhsOp = lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[0];
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            // Do nothing
        }
    }

    /**
     * New XML text instruction.
     *
     * @since 0.995.0
     */
    public static class NewXMLText extends BIRNonTerminator {
        public BIROperand textOp;

        public NewXMLText(Location pos, BIROperand lhsOp, BIROperand textOp) {
            super(pos, InstructionKind.NEW_XML_TEXT);
            this.lhsOp = lhsOp;
            this.textOp = textOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{textOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            textOp = operands[0];
        }
    }

    /**
     * New XML text instruction.
     *
     * @since 0.995.0
     */
    public static class NewXMLProcIns extends BIRNonTerminator {
        public BIROperand dataOp;
        public BIROperand targetOp;
        public boolean readonly;

        public NewXMLProcIns(Location pos, BIROperand lhsOp, BIROperand dataOp, BIROperand targetOp,
                             boolean readonly) {
            super(pos, InstructionKind.NEW_XML_PI);
            this.lhsOp = lhsOp;
            this.dataOp = dataOp;
            this.targetOp = targetOp;
            this.readonly = readonly;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{dataOp, targetOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            dataOp = operands[0];
            targetOp = operands[1];
        }
    }

    /**
     * New XML comment instruction.
     *
     * @since 0.995.0
     */
    public static class NewXMLComment extends BIRNonTerminator {
        public BIROperand textOp;
        public boolean readonly;

        public NewXMLComment(Location pos, BIROperand lhsOp, BIROperand textOp, boolean readonly) {
            super(pos, InstructionKind.NEW_XML_COMMENT);
            this.lhsOp = lhsOp;
            this.textOp = textOp;
            this.readonly = readonly;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{textOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            textOp = operands[0];
        }
    }

    /**
     * XML access expression with two operands.
     * e.g: {@code InstructionKind.XML_SEQ_STORE}, {@code InstructionKind.XML_LOAD_ALL}
     *
     * @since 0.995.0
     */
    public static class XMLAccess extends BIRNonTerminator {
        public BIROperand rhsOp;

        public XMLAccess(Location pos, InstructionKind kind, BIROperand lhsOp, BIROperand rhsOp) {
            super(pos, kind);
            this.lhsOp = lhsOp;
            this.rhsOp = rhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{rhsOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            rhsOp = operands[0];
        }
    }

    /**
     * A FP load instruction.
     * <p>
     * e.g., function (string, string) returns (string) anonFunction =
     *             function (string x, string y) returns (string) {
     *                 return x + y;
     *             };
     *
     * @since 0.995.0
     */
    public static class FPLoad extends BIRNonTerminator {
        public SchedulerPolicy schedulerPolicy;
        public String strandName;
        public Name funcName;
        public PackageID pkgId;
        public List<BIRVariableDcl> params;
        public List<BIROperand> closureMaps;
        public BType type;
        public PackageID boundMethodPkgId;

        public FPLoad(Location location, PackageID pkgId, Name funcName, BIROperand lhsOp,
                      List<BIRVariableDcl> params, List<BIROperand> closureMaps, BType type, String strandName,
                      SchedulerPolicy schedulerPolicy, PackageID boundMethodPkgId) {
            super(location, InstructionKind.FP_LOAD);
            this.schedulerPolicy = schedulerPolicy;
            this.strandName = strandName;
            this.lhsOp = lhsOp;
            this.funcName = funcName;
            this.pkgId = pkgId;
            this.params = params;
            this.closureMaps = closureMaps;
            this.type = type;
            this.type.name = funcName;
            this.boundMethodPkgId = boundMethodPkgId == null ? pkgId : boundMethodPkgId;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return closureMaps.toArray(new BIROperand[0]);
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            closureMaps = Arrays.asList(operands);
        }
    }

    /**
     * The new table instruction.
     */
    public static class NewTable extends BIRNonTerminator {
        public BIROperand keyColOp;
        public BIROperand dataOp;
        public BType type;

        public NewTable(Location pos, BType type, BIROperand lhsOp, BIROperand keyColOp,
                        BIROperand dataOp) {
            super(pos, InstructionKind.NEW_TABLE);
            this.type = type;
            this.lhsOp = lhsOp;
            this.keyColOp = keyColOp;
            this.dataOp = dataOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{keyColOp, dataOp};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            keyColOp = operands[0];
            dataOp = operands[1];
        }
    }

    /**
     * A type cast expression.
     * <p>
     * e.g., int a = cast(int) b;
     *
     * @since 0.995.0
     */
    public static class NewTypeDesc extends BIRNonTerminator {
        public List<BIROperand> closureVars;
        public BType type;
        public BIROperand annotations;

        public NewTypeDesc(Location pos, BIROperand lhsOp, BType type, List<BIROperand> closureVars) {
            super(pos, InstructionKind.NEW_TYPEDESC);
            this.closureVars = closureVars;
            this.lhsOp = lhsOp;
            this.type = type;
        }

        public NewTypeDesc(Location pos, BIROperand lhsOp, BType type, List<BIROperand> closureVars,
                           BIROperand annotations) {
            this(pos, lhsOp, type, closureVars);
            this.annotations = annotations;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            if (annotations == null) {
                return closureVars.toArray(new BIROperand[0]);
            }
            BIROperand[] operands = new BIROperand[closureVars.size() + 1];
            int i = 0;
            for (; i < closureVars.size(); i++) {
                operands[i] = closureVars.get(i);
            }
            operands[i] = annotations;
            return operands;
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            closureVars = new ArrayList<>(Arrays.asList(operands));
            if (annotations != null) {
                closureVars.remove(closureVars.size() - 1);
                annotations = operands[operands.length - 1];
            }
        }
    }

    /**
     * New RegExp instruction.
     *
     * @since 2201.3.0
     */
    public static class NewRegExp extends BIRNonTerminator {
        public BIROperand reDisjunction;

        public NewRegExp(Location pos, BIROperand lhsOp, BIROperand patternOp) {
            super(pos, InstructionKind.NEW_REG_EXP);
            this.lhsOp = lhsOp;
            this.reDisjunction = patternOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{reDisjunction};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            reDisjunction = operands[0];
        }
    }

    /**
     * New ReDisjunction instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReDisjunction extends BIRNonTerminator {
        public BIROperand sequences;

        public NewReDisjunction(Location pos, BIROperand seqList, BIROperand lhsOp) {
            super(pos, InstructionKind.NEW_RE_DISJUNCTION);
            sequences = seqList;
            this.lhsOp = lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{sequences};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            sequences = operands[0];
        }
    }

    /**
     * New ReSequence instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReSequence extends BIRNonTerminator {
        public BIROperand terms;

        public NewReSequence(Location pos, BIROperand termsList, BIROperand lhsOp) {
            super(pos, InstructionKind.NEW_RE_SEQUENCE);
            terms = termsList;
            this.lhsOp = lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{terms};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            terms = operands[0];
        }
    }

    /**
     * New ReAssertion instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReAssertion extends BIRNonTerminator {
        public BIROperand assertion;

        public NewReAssertion(Location pos, BIROperand assertion, BIROperand lhsOp) {
            super(pos, InstructionKind.NEW_RE_ASSERTION);
            this.assertion = assertion;
            this.lhsOp = lhsOp;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.assertion};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.assertion = operands[0];
        }
    }

    /**
     * New ReAtom [ReQuantifier] instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReAtomQuantifier extends BIRNonTerminator {
        public BIROperand atom;
        public BIROperand quantifier;

        public NewReAtomQuantifier(Location pos, BIROperand lhsOp, BIROperand atom, BIROperand quantifier) {
            super(pos, InstructionKind.NEW_RE_ATOM_QUANTIFIER);
            this.lhsOp = lhsOp;
            this.atom = atom;
            this.quantifier = quantifier;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.atom, this.quantifier};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.atom = operands[0];
            this.quantifier = operands[1];
        }
    }

    /**
     * New ReLiteralChar, ".", or ReEscape instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReLiteralCharOrEscape extends BIRNonTerminator {
        public BIROperand charOrEscape;

        public NewReLiteralCharOrEscape(Location pos, BIROperand lhsOp, BIROperand charOrEscape) {
            super(pos, InstructionKind.NEW_RE_LITERAL_CHAR_ESCAPE);
            this.lhsOp = lhsOp;
            this.charOrEscape = charOrEscape;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.charOrEscape};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.charOrEscape = operands[0];
        }
    }

    /**
     * New character class instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReCharacterClass extends BIRNonTerminator {
        public BIROperand classStart;
        public BIROperand negation;
        public BIROperand charSet;
        public BIROperand classEnd;

        public NewReCharacterClass(Location pos, BIROperand lhsOp, BIROperand classStart, BIROperand negation,
                                   BIROperand charSet, BIROperand classEnd) {
            super(pos, InstructionKind.NEW_RE_CHAR_CLASS);
            this.lhsOp = lhsOp;
            this.classStart = classStart;
            this.negation = negation;
            this.charSet = charSet;
            this.classEnd = classEnd;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.classStart, this.negation, this.charSet, this.classEnd};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.classStart = operands[0];
            this.negation = operands[1];
            this.charSet = operands[2];
            this.classEnd = operands[3];
        }
    }

    /**
     * New ReCharSet instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReCharSet extends BIRNonTerminator {
        public BIROperand charSetAtoms;

        public NewReCharSet(Location pos, BIROperand lhsOp, BIROperand charSetAtoms) {
            super(pos, InstructionKind.NEW_RE_CHAR_SET);
            this.lhsOp = lhsOp;
            this.charSetAtoms = charSetAtoms;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.charSetAtoms};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.charSetAtoms = operands[0];
        }
    }

    /**
     * New ReCharSetRange instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReCharSetRange extends BIRNonTerminator {
        public BIROperand lhsCharSetAtom;
        public BIROperand dash;
        public BIROperand rhsCharSetAtom;

        public NewReCharSetRange(Location pos, BIROperand lhsOp, BIROperand lhsCharSetAtom,
                                 BIROperand dash, BIROperand rhsCharSetAtom) {
            super(pos, InstructionKind.NEW_RE_CHAR_SET_RANGE);
            this.lhsOp = lhsOp;
            this.lhsCharSetAtom = lhsCharSetAtom;
            this.dash = dash;
            this.rhsCharSetAtom = rhsCharSetAtom;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.lhsCharSetAtom, this.dash, this.rhsCharSetAtom};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.lhsCharSetAtom = operands[0];
            this.dash = operands[1];
            this.rhsCharSetAtom = operands[2];
        }
    }

    /**
     * New capturing group instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReCapturingGroup extends BIRNonTerminator {
        public BIROperand openParen;
        public BIROperand flagExpr;
        public BIROperand reDisjunction;
        public BIROperand closeParen;

        public NewReCapturingGroup(Location pos, BIROperand lhsOp, BIROperand openParen, BIROperand flagExpr,
                                   BIROperand reDisjunction, BIROperand closeParen) {
            super(pos, InstructionKind.NEW_RE_CAPTURING_GROUP);
            this.lhsOp = lhsOp;
            this.openParen = openParen;
            this.flagExpr = flagExpr;
            this.reDisjunction = reDisjunction;
            this.closeParen = closeParen;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.openParen, this.flagExpr, this.reDisjunction, this.closeParen};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.openParen = operands[0];
            this.flagExpr = operands[1];
            this.reDisjunction = operands[2];
            this.closeParen = operands[3];
        }
    }

    /**
     * New flag expression instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReFlagExpression extends BIRNonTerminator {
        public BIROperand questionMark;
        public BIROperand flagsOnOff;
        public BIROperand colon;

        public NewReFlagExpression(Location pos, BIROperand lhsOp, BIROperand questionMark,
                                   BIROperand flagsOnOff, BIROperand colon) {
            super(pos, InstructionKind.NEW_RE_FLAG_EXPR);
            this.lhsOp = lhsOp;
            this.questionMark = questionMark;
            this.flagsOnOff = flagsOnOff;
            this.colon = colon;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.questionMark, this.flagsOnOff, this.colon};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.questionMark = operands[0];
            this.flagsOnOff = operands[1];
            this.colon = operands[2];
        }
    }

    /**
     * New ReFlagOnOff instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReFlagOnOff extends BIRNonTerminator {
        public BIROperand flags;

        public NewReFlagOnOff(Location pos, BIROperand lhsOp, BIROperand flags) {
            super(pos, InstructionKind.NEW_RE_FLAG_ON_OFF);
            this.lhsOp = lhsOp;
            this.flags = flags;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.flags};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.flags = operands[0];
        }
    }

    /**
     * New ReQuantifier instruction.
     *
     * @since 2201.3.0
     */
    public static class NewReQuantifier extends BIRNonTerminator {
        public BIROperand quantifier;
        public BIROperand nonGreedyChar;

        public NewReQuantifier(Location pos, BIROperand lhsOp, BIROperand quantifier, BIROperand nonGreedyChar) {
            super(pos, InstructionKind.NEW_RE_QUANTIFIER);
            this.lhsOp = lhsOp;
            this.quantifier = quantifier;
            this.nonGreedyChar = nonGreedyChar;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[]{this.quantifier, this.nonGreedyChar};
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            this.quantifier = operands[0];
            this.nonGreedyChar = operands[1];
        }
    }

    /**
     * Function pointer load instruction for record default values.
     *
     * @since 2201.9.0
     */
    public static class RecordDefaultFPLoad extends BIRNonTerminator {
        public BType enclosedType;
        public String fieldName;

        public RecordDefaultFPLoad(Location pos, BIROperand lhsOp, BType enclosedType, String fieldName) {
            super(pos, RECORD_DEFAULT_FP_LOAD);
            this.enclosedType = enclosedType;
            this.fieldName = fieldName;
            this.lhsOp = lhsOp;
        }

        @Override
        public BIROperand[] getRhsOperands() {
            return new BIROperand[0];
        }

        @Override
        public void setRhsOperands(BIROperand[] operands) {
            // Do nothing
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }

    }

}
