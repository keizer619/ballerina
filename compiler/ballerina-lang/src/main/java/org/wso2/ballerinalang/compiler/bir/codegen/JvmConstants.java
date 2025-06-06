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

package org.wso2.ballerinalang.compiler.bir.codegen;

import org.ballerinalang.model.elements.PackageID;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;

import static org.wso2.ballerinalang.compiler.util.Names.DEFAULT_VERSION;

/**
 * JVM bytecode generation related constants.
 *
 * @since 1.2.0
 */
public final class JvmConstants {

    // jvm values public API classes
    public static final String B_XML_QNAME = "io/ballerina/runtime/api/values/BXmlQName";
    public static final String B_FUNCTION_POINTER = "io/ballerina/runtime/api/values/BFunctionPointer";
    public static final String B_MAP = "io/ballerina/runtime/api/values/BMap";
    public static final String B_OBJECT = "io/ballerina/runtime/api/values/BObject";
    public static final String B_ARRAY = "io/ballerina/runtime/api/values/BArray";

    // jvm runtime values related classes
    public static final String MAP_VALUE = "io/ballerina/runtime/internal/values/MapValue";
    public static final String MAP_VALUE_IMPL = "io/ballerina/runtime/internal/values/MapValueImpl";
    public static final String STREAM_VALUE = "io/ballerina/runtime/internal/values/StreamValue";
    public static final String TABLE_VALUE = "io/ballerina/runtime/internal/values/TableValue";
    public static final String ARRAY_VALUE = "io/ballerina/runtime/internal/values/ArrayValue";
    public static final String ABSTRACT_OBJECT_VALUE = "io/ballerina/runtime/internal/values/AbstractObjectValue";
    public static final String BREF_VALUE = "io/ballerina/runtime/api/values/BRefValue";
    public static final String ERROR_VALUE = "io/ballerina/runtime/internal/values/ErrorValue";
    public static final String BERROR = "io/ballerina/runtime/api/values/BError";
    public static final String STRING_VALUE = "java/lang/String";
    public static final String FUNCTION_PARAMETER = "io/ballerina/runtime/api/types/Parameter";
    public static final String B_STRING_VALUE = "io/ballerina/runtime/api/values/BString";
    public static final String NON_BMP_STRING_VALUE = "io/ballerina/runtime/internal/values/NonBmpStringValue";
    public static final String BMP_STRING_VALUE = "io/ballerina/runtime/internal/values/BmpStringValue";
    public static final String LONG_VALUE = "java/lang/Long";
    public static final String BYTE_VALUE = "java/lang/Byte";
    public static final String SHORT_VALUE = "java/lang/Short";
    public static final String BOOLEAN_VALUE = "java/lang/Boolean";
    public static final String DOUBLE_VALUE = "java/lang/Double";
    public static final String DECIMAL_VALUE = "io/ballerina/runtime/internal/values/DecimalValue";
    public static final String INT_VALUE = "java/lang/Integer";
    public static final String XML_VALUE = "io/ballerina/runtime/internal/values/XmlValue";
    public static final String XML_QNAME = "io/ballerina/runtime/internal/values/XmlQName";
    public static final String FUTURE_VALUE = "io/ballerina/runtime/internal/values/FutureValue";
    public static final String TYPEDESC_VALUE_IMPL = "io/ballerina/runtime/internal/values/TypedescValueImpl";
    public static final String TYPEDESC_VALUE = "io/ballerina/runtime/internal/values/TypedescValue";
    public static final String HANDLE_VALUE = "io/ballerina/runtime/internal/values/HandleValue";
    public static final String LOCK_STORE = "io/ballerina/runtime/internal/lock/BLockStore";
    public static final String FUNCTION_POINTER = "io/ballerina/runtime/internal/values/FPValue";
    public static final String ARRAY_VALUE_IMPL = "io/ballerina/runtime/internal/values/ArrayValueImpl";
    public static final String TABLE_VALUE_IMPL = "io/ballerina/runtime/internal/values/TableValueImpl";
    public static final String SIMPLE_VALUE = "io/ballerina/runtime/internal/values/SimpleValue";
    public static final String REG_EXP_VALUE = "io/ballerina/runtime/internal/values/RegExpValue";
    public static final String REG_EXP_DISJUNCTION = "io/ballerina/runtime/internal/values/RegExpDisjunction";
    public static final String REG_EXP_SEQUENCE = "io/ballerina/runtime/internal/values/RegExpSequence";
    public static final String REG_EXP_ASSERTION = "io/ballerina/runtime/internal/values/RegExpAssertion";
    public static final String REG_EXP_ATOM_QUANTIFIER = "io/ballerina/runtime/internal/values/RegExpAtomQuantifier";
    public static final String REG_EXP_QUANTIFIER = "io/ballerina/runtime/internal/values/RegExpQuantifier";
    public static final String REG_EXP_CHAR_ESCAPE = "io/ballerina/runtime/internal/values/RegExpLiteralCharOrEscape";
    public static final String REG_EXP_CHAR_CLASS = "io/ballerina/runtime/internal/values/RegExpCharacterClass";
    public static final String REG_EXP_CHAR_SET = "io/ballerina/runtime/internal/values/RegExpCharSet";
    public static final String REG_EXP_CHAR_SET_RANGE = "io/ballerina/runtime/internal/values/RegExpCharSetRange";
    public static final String REG_EXP_CAPTURING_GROUP = "io/ballerina/runtime/internal/values/RegExpCapturingGroup";
    public static final String REG_EXP_FLAG_EXPR = "io/ballerina/runtime/internal/values/RegExpFlagExpression";
    public static final String REG_EXP_FLAG_ON_OFF = "io/ballerina/runtime/internal/values/RegExpFlagOnOff";

    public static final String B_HANDLE = "io/ballerina/runtime/api/values/BHandle";

    public static final String B_INITIAL_VALUE_ENTRY = "io/ballerina/runtime/api/values/BInitialValueEntry";
    public static final String B_LIST_INITIAL_VALUE_ENTRY = "io/ballerina/runtime/api/values/BListInitialValueEntry";
    public static final String B_MAPPING_INITIAL_VALUE_ENTRY = "io/ballerina/runtime/api/values/BMapInitialValueEntry";
    public static final String MAPPING_INITIAL_KEY_VALUE_ENTRY =
            "io/ballerina/runtime/internal/values/MappingInitialValueEntry$KeyValueEntry";
    public static final String MAPPING_INITIAL_SPREAD_FIELD_ENTRY =
            "io/ballerina/runtime/internal/values/MappingInitialValueEntry$SpreadFieldEntry";
    public static final String LIST_INITIAL_VALUE_ENTRY = "io/ballerina/runtime/internal/values/ListInitialValueEntry";
    public static final String LIST_INITIAL_EXPRESSION_ENTRY =
            "io/ballerina/runtime/internal/values/ListInitialValueEntry$ExpressionEntry";
    public static final String LIST_INITIAL_SPREAD_ENTRY =
            "io/ballerina/runtime/internal/values/ListInitialValueEntry$SpreadEntry";

    // types related classes
    public static final String TYPE = "io/ballerina/runtime/api/types/Type";
    public static final String PREDEFINED_TYPES = "io/ballerina/runtime/api/types/PredefinedTypes";

    public static final String ARRAY_TYPE = "io/ballerina/runtime/api/types/ArrayType";
    public static final String XML_TYPE = "io/ballerina/runtime/api/types/XmlType";
    public static final String JSON_TYPE = "io/ballerina/runtime/api/types/JsonType";
    public static final String STREAM_TYPE = "io/ballerina/runtime/api/types/StreamType";
    public static final String TABLE_TYPE = "io/ballerina/runtime/api/types/TableType";
    public static final String UNION_TYPE = "io/ballerina/runtime/api/types/UnionType";
    public static final String INTERSECTION_TYPE = "io/ballerina/runtime/api/types/IntersectionType";
    public static final String RECORD_TYPE = "io/ballerina/runtime/api/types/RecordType";
    public static final String OBJECT_TYPE = "io/ballerina/runtime/api/types/ObjectType";
    public static final String SERVICE_TYPE = "io/ballerina/runtime/api/types/ServiceType";
    public static final String ERROR_TYPE = "io/ballerina/runtime/api/types/ErrorType";
    public static final String TUPLE_TYPE = "io/ballerina/runtime/api/types/TupleType";
    public static final String FUNCTION_TYPE = "io/ballerina/runtime/api/types/FunctionType";
    public static final String FIELD = "io/ballerina/runtime/api/types/Field";
    public static final String METHOD_TYPE = "io/ballerina/runtime/api/types/MethodType";
    public static final String RESOURCE_METHOD_TYPE = "io/ballerina/runtime/api/types/ResourceMethodType";
    public static final String FINITE_TYPE = "io/ballerina/runtime/api/types/FiniteType";
    public static final String INTEGER_TYPE = "io/ballerina/runtime/api/types/IntegerType";
    public static final String BYTE_TYPE = "io/ballerina/runtime/api/types/ByteType";
    public static final String FLOAT_TYPE = "io/ballerina/runtime/api/types/FloatType";
    public static final String STRING_TYPE = "io/ballerina/runtime/api/types/StringType";
    public static final String BOOLEAN_TYPE = "io/ballerina/runtime/api/types/BooleanType";
    public static final String DECIMAL_TYPE = "io/ballerina/runtime/api/types/DecimalType";
    public static final String READONLY_TYPE = "io/ballerina/runtime/api/types/ReadonlyType";
    public static final String ANY_TYPE = "io/ballerina/runtime/api/types/AnyType";
    public static final String ANYDATA_TYPE = "io/ballerina/runtime/api/types/AnydataType";
    public static final String NEVER_TYPE = "io/ballerina/runtime/api/types/NeverType";
    public static final String NULL_TYPE = "io/ballerina/runtime/api/types/NullType";
    public static final String HANDLE_TYPE = "io/ballerina/runtime/api/types/HandleType";
    public static final String INTERSECTABLE_REFERENCE_TYPE
            = "io/ballerina/runtime/api/types/IntersectableReferenceType";

    public static final String ARRAY_TYPE_IMPL = "io/ballerina/runtime/internal/types/BArrayType";
    public static final String MAP_TYPE_IMPL = "io/ballerina/runtime/internal/types/BMapType";
    public static final String XML_TYPE_IMPL = "io/ballerina/runtime/internal/types/BXmlType";
    public static final String STREAM_TYPE_IMPL = "io/ballerina/runtime/internal/types/BStreamType";
    public static final String TABLE_TYPE_IMPL = "io/ballerina/runtime/internal/types/BTableType";
    public static final String UNION_TYPE_IMPL = "io/ballerina/runtime/internal/types/BUnionType";
    public static final String INTERSECTION_TYPE_IMPL = "io/ballerina/runtime/internal/types/BIntersectionType";
    public static final String RECORD_TYPE_IMPL = "io/ballerina/runtime/internal/types/BRecordType";
    public static final String OBJECT_TYPE_IMPL = "io/ballerina/runtime/internal/types/BObjectType";
    public static final String SERVICE_TYPE_IMPL = "io/ballerina/runtime/internal/types/BServiceType";
    public static final String CLIENT_TYPE_IMPL = "io/ballerina/runtime/internal/types/BClientType";
    public static final String ERROR_TYPE_IMPL = "io/ballerina/runtime/internal/types/BErrorType";
    public static final String TUPLE_TYPE_IMPL = "io/ballerina/runtime/internal/types/BTupleType";
    public static final String FUNCTION_TYPE_IMPL = "io/ballerina/runtime/internal/types/BFunctionType";
    public static final String TYPEDESC_TYPE_IMPL = "io/ballerina/runtime/internal/types/BTypedescType";
    public static final String PARAMETERIZED_TYPE_IMPL = "io/ballerina/runtime/internal/types/BParameterizedType";
    public static final String FIELD_IMPL = "io/ballerina/runtime/internal/types/BField";
    public static final String METHOD_TYPE_IMPL = "io/ballerina/runtime/internal/types/BMethodType";
    public static final String RESOURCE_METHOD_TYPE_IMPL = "io/ballerina/runtime/internal/types/BResourceMethodType";
    public static final String REMOTE_METHOD_TYPE_IMPL = "io/ballerina/runtime/internal/types/BRemoteMethodType";
    public static final String FINITE_TYPE_IMPL = "io/ballerina/runtime/internal/types/BFiniteType";
    public static final String FUTURE_TYPE_IMPL = "io/ballerina/runtime/internal/types/BFutureType";
    public static final String TYPE_REF_TYPE_IMPL = "io/ballerina/runtime/internal/types/BTypeReferenceType";
    public static final String TYPE_IMPL = "io/ballerina/runtime/internal/types/BType";
    public static final String MODULE = "io/ballerina/runtime/api/Module";
    public static final String CURRENT_MODULE_VAR_NAME = "$currentModule";
    public static final String B_STRING_VAR_PREFIX = "$bString";
    public static final String LARGE_STRING_VAR_PREFIX = "$stringChunk";
    public static final String GET_SURROGATE_ARRAY_METHOD_PREFIX = "getSurrogateArray";
    public static final String UNION_TYPE_VAR_PREFIX = "$unionType";
    public static final String ERROR_TYPE_VAR_PREFIX = "$errorType";
    public static final String TYPEREF_TYPE_VAR_PREFIX = "$typeRefType$";
    public static final String TUPLE_TYPE_VAR_PREFIX = "$tupleType";
    public static final String ARRAY_TYPE_VAR_PREFIX = "$arrayType";
    public static final String FUNCTION_TYPE_VAR_PREFIX = "$functionType";
    public static final String MODULE_VAR_PREFIX = "$module";

    public static final String VARIABLE_KEY = "io/ballerina/runtime/internal/configurable/VariableKey";
    public static final String CONFIG_DETAILS = "io/ballerina/runtime/internal/configurable/providers/ConfigDetails";
    public static final String TEST_ARGUMENTS = "io/ballerina/runtime/internal/testable/TestArguments";
    public static final String TEST_CONFIG_ARGS = "io/ballerina/runtime/internal/testable/TestConfigArguments";
    public static final String TYPE_ID_SET = "io/ballerina/runtime/internal/types/BTypeIdSet";

    // other jvm-specific classes
    public static final String BAL_RUNTIME = "io/ballerina/runtime/internal/BalRuntime";
    public static final String TYPE_CHECKER = "io/ballerina/runtime/internal/TypeChecker";
    public static final String SCHEDULER = "io/ballerina/runtime/internal/scheduling/Scheduler";
    public static final String JSON_UTILS = "io/ballerina/runtime/internal/json/JsonInternalUtils";
    public static final String STRAND_CLASS = "io/ballerina/runtime/internal/scheduling/Strand";
    public static final String STRAND_METADATA = "io/ballerina/runtime/api/concurrent/StrandMetadata";
    public static final String BAL_ENV_CLASS = "io/ballerina/runtime/internal/BalEnvironment";
    public static final String BAL_ENV = "io/ballerina/runtime/api/Environment";
    public static final String TYPE_CONVERTER = "io/ballerina/runtime/internal/TypeConverter";
    public static final String VALUE_CREATOR = "io/ballerina/runtime/internal/values/ValueCreator";
    public static final String XML_FACTORY = "io/ballerina/runtime/internal/xml/XmlFactory";
    public static final String XML_SEQUENCE = "io/ballerina/runtime/internal/values/XmlSequence";
    public static final String ASYNC_UTILS = "io/ballerina/runtime/internal/scheduling/AsyncUtils";
    public static final String WORKER_UTILS = "io/ballerina/runtime/internal/scheduling/WorkerUtils";
    public static final String WORKER_CHANNEL_MAP = "io/ballerina/runtime/internal/scheduling/WorkerChannelMap";
    public static final String MAP_UTILS = "io/ballerina/runtime/internal/utils/MapUtils";
    public static final String TABLE_UTILS = "io/ballerina/runtime/internal/utils/TableUtils";
    public static final String STRING_UTILS = "io/ballerina/runtime/api/utils/StringUtils";
    public static final String ERROR_UTILS = "io/ballerina/runtime/internal/utils/ErrorUtils";
    public static final String RUNTIME_UTILS = "io/ballerina/runtime/internal/utils/RuntimeUtils";
    public static final String LARGE_STRUCTURE_UTILS = "io/ballerina/runtime/internal/utils/LargeStructureUtils";
    public static final String OPTION = "io/ballerina/runtime/internal/cli/Option";
    public static final String OPERAND = "io/ballerina/runtime/internal/cli/Operand";
    public static final String CLI_SPEC = "io/ballerina/runtime/internal/cli/CliSpec";
    public static final String LAUNCH_UTILS = "io/ballerina/runtime/internal/launch/LaunchUtils";
    public static final String MATH_UTILS = "io/ballerina/runtime/internal/utils/MathUtils";
    public static final String ERROR_REASONS = "io/ballerina/runtime/internal/errors/ErrorReasons";
    public static final String ERROR_CODES = "io/ballerina/runtime/internal/errors/ErrorCodes";
    public static final String ERROR_HELPER = "io/ballerina/runtime/internal/errors/ErrorHelper";
    public static final String COMPATIBILITY_CHECKER = "io/ballerina/runtime/internal/utils/CompatibilityChecker";
    public static final String RUNTIME_REGISTRY_CLASS = "io/ballerina/runtime/internal/scheduling/RuntimeRegistry";
    public static final String VALUE_COMPARISON_UTILS = "io/ballerina/runtime/internal/utils/ValueComparisonUtils";
    public static final String REG_EXP_FACTORY = "io/ballerina/runtime/internal/regexp/RegExpFactory";
    public static final String REPOSITORY_IMPL = "io/ballerina/runtime/internal/repository/RepositoryImpl";

    // other java classes
    public static final String OBJECT = "java/lang/Object";
    public static final String MAP = "java/util/Map";
    public static final String LINKED_HASH_MAP = "java/util/LinkedHashMap";
    public static final String ARRAY_LIST = "java/util/ArrayList";
    public static final String LIST = "java/util/List";
    public static final String SET = "java/util/Set";
    public static final String LINKED_HASH_SET = "java/util/LinkedHashSet";
    public static final String STRING_BUILDER = "java/lang/StringBuilder";
    public static final String FUNCTION = "java/util/function/Function";
    public static final String LONG_STREAM = "java/util/stream/LongStream";
    public static final String JAVA_THREAD = "java/lang/Thread";
    public static final String JAVA_RUNTIME = "java/lang/Runtime";
    public static final String MAP_ENTRY = "java/util/Map$Entry";
    public static final String MAP_SIMPLE_ENTRY = "java/util/AbstractMap$SimpleEntry";
    public static final String COLLECTION = "java/util/Collection";
    public static final String NUMBER = "java/lang/Number";
    public static final String HASH_MAP = "java/util/HashMap";
    public static final String PATH = "java/nio/file/Path";
    public static final String SYSTEM = "java/lang/System";
    public static final String REENTRANT_LOCK = "java/util/concurrent/locks/ReentrantLock";

    // service objects, annotation processing related classes
    public static final String ANNOTATION_UTILS = "io/ballerina/runtime/internal/utils/AnnotationUtils";
    public static final String ANNOTATION_MAP_NAME = "$annotation_data";
    public static final String ANNOTATIONS_FIELD = "$annotations";
    public static final String DEFAULTABLE_ARGS_ANOT_NAME = "DefaultableArgs";
    public static final String DEFAULTABLE_ARGS_ANOT_FIELD = "args";

    // types related constants
    public static final String TYPES_ERROR = "TYPE_ERROR";
    public static final String TYPE_ANYDATA_ARRAY = "TYPE_ANYDATA_ARRAY";
    public static final String TYPE_ANY_ARRAY = "TYPE_ANY_ARRAY";

    // error related constants
    public static final String SET_DETAIL_TYPE_METHOD = "setDetailType";
    public static final String SET_TYPEID_SET_METHOD = "setTypeIdSet";
    public static final String TRAP_ERROR_METHOD = "trapError";

    // future related constants
    public static final String GET = "get";

    // union and tuple related constants
    public static final String SET_MEMBERS_METHOD = "setMemberTypes";
    public static final String SET_ORIGINAL_MEMBERS_METHOD = "setOriginalMemberTypes";
    public static final String SET_CYCLIC_METHOD = "setCyclic";

    // Immutable type related constants.
    public static final String SET_IMMUTABLE_TYPE_METHOD = "setImmutableType";

    // exception classes
    public static final String THROWABLE = "java/lang/Throwable";
    public static final String STACK_OVERFLOW_ERROR = "java/lang/StackOverflowError";
    public static final String UNSUPPORTED_OPERATION_EXCEPTION = "java/lang/UnsupportedOperationException";
    public static final String HANDLE_FUTURE_METHOD = "handleFuture";
    public static final String HANDLE_FUTURE_AND_RETURN_IS_PANIC_METHOD = "handleFutureAndReturnIsPanic";
    public static final String HANDLE_FUTURE_AND_EXIT_METHOD = "handleFutureAndExit";
    public static final String HANDLE_THROWABLE_METHOD = "handleThrowable";

    // code generation related constants.
    public static final String MODULE_INIT_CLASS_NAME = "$_init";
    public static final String OBJECT_SELF_INSTANCE = "self";
    public static final String UNION_TYPE_CONSTANT_CLASS_NAME = "constants/$_union_type_constants";
    public static final String ERROR_TYPE_CONSTANT_CLASS_NAME = "constants/$_error_type_constants";
    public static final String TUPLE_TYPE_CONSTANT_CLASS_NAME = "constants/$_tuple_type_constants";
    public static final String ARRAY_TYPE_CONSTANT_CLASS_NAME = "constants/$_array_type_constants";
    public static final String TYPEREF_TYPE_CONSTANT_CLASS_NAME = "constants/$_typeref_type_constants";
    public static final String FUNCTION_TYPE_CONSTANT_CLASS_NAME = "constants/$_function_type_constants";
    public static final String MODULE_STRING_CONSTANT_CLASS_NAME = "constants/$_string_constants";
    public static final String MODULE_SURROGATES_CLASS_NAME = "constants/$_surrogate_methods";
    public static final String MODULE_CONSTANT_CLASS_NAME = "constants/$_module_constants";
    public static final String CONSTANTS_CLASS_NAME = "constants/$_constants";
    public static final String MODULE_TYPES_CLASS_NAME = "types/$_types";
    public static final String MODULE_RECORD_TYPES_CLASS_NAME = "types/$_record_types";
    public static final String MODULE_OBJECT_TYPES_CLASS_NAME = "types/$_object_types";
    public static final String MODULE_ERROR_TYPES_CLASS_NAME = "types/$_error_types";
    public static final String MODULE_UNION_TYPES_CLASS_NAME = "types/$_union_types";
    public static final String MODULE_TUPLE_TYPES_CLASS_NAME = "types/$_tuple_types";
    public static final String MODULE_ANON_TYPES_CLASS_NAME = "types/$_anon_types";
    public static final String MODULE_FUNCTION_TYPES_CLASS_NAME = "types/$_function_types";
    public static final String B_FUNCTION_TYPE_INIT_METHOD_PREFIX = "$function_type_init";
    public static final String MODULE_RECORDS_CREATOR_CLASS_NAME = "creators/$_records";
    public static final String MODULE_OBJECTS_CREATOR_CLASS_NAME = "creators/$_objects";
    public static final String MODULE_FUNCTION_CALLS_CLASS_NAME = "creators/$_function_calls";
    public static final String MODULE_ERRORS_CREATOR_CLASS_NAME = "creators/$_errors";
    public static final String MODULE_ANNOTATIONS_CLASS_NAME = "annotations/$_annotations";
    public static final String MODULE_GENERATED_FUNCTIONS_CLASS_NAME = "functions/$_generated";
    public static final String MODULE_LAMBDAS_CLASS_NAME = "lambdas/$_generated";
    public static final String B_STRING_INIT_METHOD_PREFIX = "$string_init";
    public static final String B_UNION_TYPE_INIT_METHOD = "$union_type_init";
    public static final String B_ERROR_TYPE_INIT_METHOD = "$error_type_init";
    public static final String B_TUPLE_TYPE_INIT_METHOD = "$tuple_type_init";
    public static final String B_ARRAY_TYPE_INIT_METHOD = "$array_type_init";
    public static final String B_TYPEREF_TYPE_INIT_METHOD = "$typeref_type_init";
    public static final String B_UNION_TYPE_POPULATE_METHOD = "$populate_union_types";
    public static final String B_TUPLE_TYPE_POPULATE_METHOD = "$populate_tuple_types";
    public static final String B_ARRAY_TYPE_POPULATE_METHOD = "$populate_array_types";
    public static final String B_TYPEREF_TYPE_POPULATE_METHOD = "$populate_typeref_types";
    public static final String B_ERROR_TYPE_POPULATE_METHOD = "$populate_error_typeS";
    public static final String MODULE_INIT_METHOD_PREFIX = "$module_init";
    public static final String CONSTANT_INIT_METHOD_PREFIX = "$constant_init";
    public static final String ANNOTATIONS_METHOD_PREFIX = "$process_annotations";
    public static final String CURRENT_MODULE_INIT_METHOD = "$currentModuleInit";
    public static final String CURRENT_MODULE_STOP_METHOD = "$currentModuleStop";
    public static final String MODULE_INIT_METHOD = "$moduleInit";
    public static final String MODULE_START_METHOD = "$moduleStart";
    public static final String MODULE_STOP_METHOD = "$moduleStop";
    public static final String MODULE_EXECUTE_METHOD = "$moduleExecute";
    public static final String TEST_EXECUTE_METHOD = "__execute__";
    public static final String MAIN_METHOD = "main";
    public static final String BAL_EXTENSION = ".bal";
    public static final String WINDOWS_PATH_SEPERATOR = "\\";
    public static final String JAVA_PACKAGE_SEPERATOR = "/";
    public static final String FILE_NAME_PERIOD_SEPERATOR = "$$$";
    public static final String VALUE_CLASS_PREFIX = "$value$";
    public static final String TYPEDESC_CLASS_PREFIX = "$typedesc$";
    public static final String BALLERINA = "ballerina";
    public static final String ENCODED_DOT_CHARACTER = "&0046";
    public static final String ENCODED_JAVA_MODULE = "jballerina&0046java";
    public static final PackageID DEFAULT = new PackageID(Names.ANON_ORG, new Name(ENCODED_DOT_CHARACTER),
            DEFAULT_VERSION);
    public static final String BUILT_IN_PACKAGE_NAME = "lang" + ENCODED_DOT_CHARACTER + "annotations";
    public static final String MODULE_START_ATTEMPTED = "$moduleStartAttempted";
    public static final String PARENT_MODULE_START_ATTEMPTED = "$parentModuleStartAttempted";
    public static final String NO_OF_DEPENDANT_MODULES = "$noOfDependantModules";
    public static final String MODULE_STARTED = "$moduleStarted";
    public static final String WRAPPER_GEN_BB_ID_NAME = "wrapperGen";
    public static final String JVM_INIT_METHOD = "<init>";
    public static final String JVM_STATIC_INIT_METHOD = "<clinit>";
    public static final String JVM_TO_STRING_METHOD = "toString";
    public static final String JVM_TO_UNSIGNED_INT_METHOD = "toUnsignedInt";
    public static final String GET_VALUE_METHOD = "getValue";
    public static final String ANY_TO_BYTE_METHOD = "anyToByte";
    public static final String ANY_TO_INT_METHOD = "anyToInt";
    public static final String ANY_TO_FLOAT_METHOD = "anyToFloat";
    public static final String ANY_TO_DECIMAL_METHOD = "anyToDecimal";
    public static final String ANY_TO_BOOLEAN_METHOD = "anyToBoolean";
    public static final String DECIMAL_VALUE_OF_J_METHOD = "valueOfJ";
    public static final String VALUE_OF_METHOD = "valueOf";
    public static final String EQUALS_METHOD = "equals";
    public static final String POPULATE_INITIAL_VALUES_METHOD = "populateInitialValues";
    public static final String CREATE_TYPES_METHOD = "$createTypes";
    public static final String CREATE_TYPE_CONSTANTS_METHOD = "$createTypeConstants";
    public static final String CREATE_TYPE_INSTANCES_METHOD = "$createTypeInstances";
    public static final String CLASS_LOCK_VAR_NAME = "$lock";
    public static final String GLOBAL_LOCK_NAME = "lock";
    public static final String SERVICE_EP_AVAILABLE = "$serviceEPAvailable";
    public static final String BAL_RUNTIME_VAR_NAME = "$balRuntime";
    public static final String LOCK_STORE_VAR_NAME = "$lockStore";
    public static final String WORKER_CHANNEL_MAP_VAR_NAME = "$channelMap";
    public static final String SEND_WORKER_CHANNEL_NAMES_VAR_NAME = "$sendWorkerChannelNames";
    public static final String RECEIVE_WORKER_CHANNEL_NAMES_VAR_NAME = "$receiveWorkerChannelNames";
    public static final String WORKER_CHANNEL_NAMES_MAP = "$channelNamesMap";
    public static final String WORKER_CHANNELS_ADD_METHOD = "addWorkerChannels";
    public static final String WORKER_CHANNELS_COMPLETE_METHOD = "completedWorkerChannels";
    public static final String WORKER_CHANNELS_COMPLETE_WITH_PANIC_METHOD = "completeWorkerChannelsWithPanic";
    public static final String RECORD_INIT_WRAPPER_NAME = "$init";
    public static final String RUNTIME_REGISTRY_VARIABLE = "runtimeRegistry";
    public static final String SCHEDULER_VARIABLE = "scheduler";
    public static final String CONFIGURE_INIT = "$configureInit";
    public static final String CONFIGURATION_CLASS_NAME = "$configurationMapper";
    public static final String POPULATE_CONFIG_DATA_METHOD = "$initAndPopulateConfigData";
    public static final String CONFIGURE_INIT_ATTEMPTED = "$configureInitAttempted";
    public static final String HANDLE_ANYDATA_VALUES = "handleAnydataValues";
    public static final String CREATE_INTEROP_ERROR_METHOD = "createInteropError";
    public static final String LAMBDA_PREFIX = "$lambda$";
    public static final String SPLIT_CLASS_SUFFIX = "$split$";
    public static final String POPULATE_METHOD_PREFIX = "$populate";
    public static final String ADD_METHOD = "add";
    public static final String TEST_EXECUTION_STATE = "__gH7W16nQmp0TestExecState__";
    public static final String GET_TEST_EXECUTION_STATE = "$getTestExecutionState";
    public static final String STRAND_LOCAL_VARIABLE_NAME = "__strand";
    public static final String CLASS_FILE_SUFFIX = ".class";

    // scheduler related constants
    public static final String START_ISOLATED_WORKER = "startIsolatedWorker";
    public static final String START_NON_ISOLATED_WORKER = "startNonIsolatedWorker";
    public static final String CREATE_RECORD_VALUE = "createRecordValue";
    public static final String CREATE_OBJECT_VALUE = "createObjectValue";
    public static final String CREATE_ERROR_VALUE = "createErrorValue";
    public static final String CALL_FUNCTION = "call";
    public static final String INSTANTIATE_FUNCTION = "instantiate";

    public static final String GET_ANON_TYPE_METHOD = "getAnonType";
    public static final String GET_FUNCTION_TYPE_METHOD = "getFunctionType";

    // strand data related constants
    public static final String STRAND = "strand";
    public static final String STRAND_THREAD = "thread";
    public static final String STRAND_NAME = "name";
    public static final String STRAND_POLICY_NAME = "policy";
    public static final String STRAND_WORKER_CHANNEL_MAP = "workerChannelMap";
    public static final String STRAND_VALUE_ANY = "any";
    public static final String STRAND_METADATA_VAR_PREFIX = "$strand_metadata$";
    public static final String MAIN_ARG_VAR_PREFIX = "%param";
    public static final String WAIT_ON_LISTENERS_METHOD_NAME = "waitOnListeners";
    public static final String DEFAULT_STRAND_DISPATCHER = "DEFAULT";
    public static final String DEFAULT_STRAND_NAME = "anon";

    // transaction related constants
    public static final String TRANSACTION_CONTEXT_CLASS = "io/ballerina/runtime/transactions/TransactionLocalContext";

    // observability related constants
    public static final String OBSERVE_UTILS = "io/ballerina/runtime/observability/ObserveUtils";
    public static final String START_RESOURCE_OBSERVATION_METHOD = "startResourceObservation";
    public static final String START_CALLABLE_OBSERVATION_METHOD = "startCallableObservation";
    public static final String STOP_OBSERVATION_WITH_ERROR_METHOD = "stopObservationWithError";
    public static final String REPORT_ERROR_METHOD = "reportError";
    public static final String STOP_OBSERVATION_METHOD = "stopObservation";
    public static final String OBSERVABLE_ANNOTATION = "ballerina/observe/Observable";
    public static final String DISPLAY_ANNOTATION = "display";
    public static final String RECORD_CHECKPOINT_METHOD = "recordCheckpoint";
    public static final String BALLERINA_HOME = "ballerina.home";
    public static final String BALLERINA_VERSION = "ballerina.version";
    public static final String GET_ELEMENT_OR_NIL = "getElementOrNil";
    public static final String GET_ELEMENT = "getElement";
    public static final String FILL_AND_GET = "fillAndGet";
    public static final String GET_BOXED_VALUE = "get";
    public static final String GET_UNBOXED_INT_VALUE = "getUnboxedIntValue";
    public static final String GET_UNBOXED_FLOAT_VALUE = "getUnboxedFloatValue";
    public static final String GET_STRING_VALUE = "getStringValue";
    public static final String GET_UNBOXED_BOOLEAN_VALUE = "getUnboxedBooleanValue";
    // visibility flags
    public static final int BAL_OPTIONAL = 4096;

    public static final String TYPE_NOT_SUPPORTED_MESSAGE = "JVM generation is not supported for type ";

    public static final int MAX_MEMBERS_PER_METHOD = 100;
    public static final int MAX_TYPES_PER_METHOD = 100;
    public static final int MAX_FIELDS_PER_SPLIT_METHOD = 500;
    public static final int MAX_FUNCTION_TYPE_FIELDS_PER_SPLIT_METHOD = 140;
    public static final int MAX_MODULES_PER_METHOD = 100;
    public static final int MAX_CALLS_PER_CLIENT_METHOD = 100;
    public static final int MAX_CONSTANTS_PER_METHOD = 100;
    public static final int MAX_CALLS_PER_FUNCTION_CALL_METHOD = 100;
    public static final int MAX_METHOD_COUNT_PER_BALLERINA_OBJECT = 100;
    /*
    MAX_STRINGS_PER_METHOD is calculated as below.
    No of instructions required for create ballerina string constant object = 12
    Java method limit = 64000
    Max strings constant initializations per method = 64000/12 -> 5000
    */
    public static final int MAX_STRINGS_PER_METHOD = 5000;
    public static final int VISIT_MAX_SAFE_MARGIN = 10;
    public static final int OVERFLOW_LINE_NUMBER = 0x80000000;
    public static final int MAX_GENERATED_METHODS_PER_CLASS = 100;
    public static final int MAX_GENERATED_LAMBDAS_PER_CLASS = 500;

    private JvmConstants() {
    }
}
