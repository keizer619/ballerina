/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.compiler.internal.parser;

/**
 * Parser rule contexts that represent each point in the grammar.
 * These represent the current scope during the parsing.
 *
 * @since 1.2.0
 */
public enum ParserRuleContext {

    // Productions
    COMP_UNIT("comp-unit"),
    EOF("eof"),
    TOP_LEVEL_NODE("top-level-node"),
    TOP_LEVEL_NODE_WITHOUT_METADATA("top-level-node-without-metadata"),
    TOP_LEVEL_NODE_WITHOUT_MODIFIER("top-level-node-without-modifier"),
    FUNC_DEF("func-def"),
    FUNC_DEF_START("function-def-start"),
    FUNC_DEF_OR_FUNC_TYPE("func-def-or-func-type"),
    FUNC_DEF_FIRST_QUALIFIER("func-def-first-qualifier"),
    FUNC_DEF_SECOND_QUALIFIER("func-def-second-qualifier"),
    FUNC_DEF_WITHOUT_FIRST_QUALIFIER("func-def-without-first-qualifier"),
    PARAM_LIST("parameters"),
    PARAMETER_START("parameter-start"),
    PARAMETER_START_WITHOUT_ANNOTATION("parameter-start-without-annotation"),
    PARAM_END("param-end"),
    REQUIRED_PARAM("required-parameter"),
    DEFAULTABLE_PARAM("defaultable-parameter"),
    REST_PARAM("rest-parameter"),
    PARAM_START("parameter-start"),
    PARAM_RHS("param-rhs"),
    FUNC_TYPE_PARAM_RHS("function-type-desc-param-rhs"),
    REST_PARAM_RHS("rest-param-rhs"),
    AFTER_PARAMETER_TYPE("after-parameter-type"),
    PARAMETER_NAME_RHS("parameter-name-rhs"),
    REQUIRED_PARAM_NAME_RHS("required-param-name-rhs"),
    FUNC_OPTIONAL_RETURNS("func-optional-returns"),
    FUNC_BODY("func-body"),
    FUNC_BODY_OR_TYPE_DESC_RHS("func-body-or-type-desc-rhs"),
    ANON_FUNC_BODY("annon-func-body"),
    FUNC_TYPE_DESC_END("func-type-desc-end"),
    EXTERNAL_FUNC_BODY("external-func-body"),
    EXTERNAL_FUNC_BODY_OPTIONAL_ANNOTS("external-func-body-optional-annots"),
    FUNC_BODY_BLOCK("func-body-block"),
    MODULE_TYPE_DEFINITION("type-definition"),
    MODULE_CLASS_DEFINITION("class-definition"),
    MODULE_CLASS_DEFINITION_START("class-definition-start"),
    FIRST_CLASS_TYPE_QUALIFIER("first-class-type-qualifier"),
    SECOND_CLASS_TYPE_QUALIFIER("second-class-type-qualifier"),
    THIRD_CLASS_TYPE_QUALIFIER("third-class-type-qualifier"),
    FOURTH_CLASS_TYPE_QUALIFIER("fourth-class-type-qualifier"),
    CLASS_DEF_WITHOUT_FIRST_QUALIFIER("class-def-without-first-qualifier"),
    CLASS_DEF_WITHOUT_SECOND_QUALIFIER("class-def-without-second-qualifier"),
    CLASS_DEF_WITHOUT_THIRD_QUALIFIER("class-def-without-third-qualifier"),
    FIELD_OR_REST_DESCIPTOR_RHS("field-or-rest-descriptor-rhs"),
    FIELD_DESCRIPTOR_RHS("field-descriptor-rhs"),
    RECORD_BODY_START("record-body-start"),
    RECORD_BODY_END("record-body-end"),
    RECORD_FIELD("record-field"),
    RECORD_FIELD_OR_RECORD_END("record-field-orrecord-end"),
    RECORD_FIELD_START("record-field-start"),
    RECORD_FIELD_WITHOUT_METADATA("record-field-without-metadata"),
    TYPE_DESCRIPTOR("type-descriptor"),
    TYPE_DESC_WITHOUT_ISOLATED("type-desc-without-isolated"),
    CLASS_DESCRIPTOR("class-descriptor"),
    RECORD_TYPE_DESCRIPTOR("record-type-desc"),
    TYPE_REFERENCE("type-reference"),
    TYPE_REFERENCE_IN_TYPE_INCLUSION("type-reference-in-type-inclusion"),
    SIMPLE_TYPE_DESC_IDENTIFIER("simple-type-desc-identifier"),
    ARG_LIST_OPEN_PAREN("("),
    ARG_LIST("arguments"),
    ARG_START("argument-start"),
    ARG_END("arg-end"),
    ARG_LIST_END("argument-end"),
    ARG_LIST_CLOSE_PAREN(")"),
    ARG_START_OR_ARG_LIST_END("arg-start-or-args-list-end"),
    NAMED_OR_POSITIONAL_ARG_RHS("named-or-positional-arg"),
    OBJECT_TYPE_DESCRIPTOR("object-type-desc"),
    OBJECT_CONSTRUCTOR_MEMBER("object-constructor-member"),
    CLASS_MEMBER("class-member"),
    OBJECT_TYPE_MEMBER("object-type-member"),
    CLASS_MEMBER_OR_OBJECT_MEMBER_START("class-member-or-object-member-start"),
    OBJECT_CONSTRUCTOR_MEMBER_START("object-constructor-member-start"),
    CLASS_MEMBER_OR_OBJECT_MEMBER_WITHOUT_META("class-member-or-object-member-without-metadata"),
    OBJECT_CONS_MEMBER_WITHOUT_META("object-constructor-member-without-metadata"),
    OBJECT_FUNC_OR_FIELD("object-func-or-field"),
    OBJECT_FUNC_OR_FIELD_WITHOUT_VISIBILITY("object-func-or-field-without-visibility"),
    OBJECT_MEMBER_VISIBILITY_QUAL("object-member-visibility-qual"),
    OBJECT_METHOD_START("object-method-start"),
    OBJECT_METHOD_FIRST_QUALIFIER("object-method-first-qualifier"),
    OBJECT_METHOD_SECOND_QUALIFIER("object-method-second-qualifier"),
    OBJECT_METHOD_THIRD_QUALIFIER("object-method.third-qualifier"),
    OBJECT_METHOD_FOURTH_QUALIFIER("object-method-fourth-qualifier"),
    OBJECT_METHOD_WITHOUT_FIRST_QUALIFIER("object.method.without.first.qualifier"),
    OBJECT_METHOD_WITHOUT_SECOND_QUALIFIER("object.method.without.transactional"),
    OBJECT_METHOD_WITHOUT_THIRD_QUALIFIER("object.method.without.isolated"),
    OBJECT_FIELD_START("object-field-start"),
    OBJECT_FIELD_QUALIFIER("object-field-qualifier"),
    OBJECT_FIELD_RHS("object-field-rhs"),
    OPTIONAL_FIELD_INITIALIZER("optional-field-initializer"),
    ON_FAIL_OPTIONAL_BINDING_PATTERN("on-fail-optional-binding-pattern"),
    FIRST_OBJECT_TYPE_QUALIFIER("first-object-type-qualifier"),
    SECOND_OBJECT_TYPE_QUALIFIER("second-object-type-qualifier"),
    FIRST_OBJECT_CONS_QUALIFIER("first-object-cons-qualifier"),
    SECOND_OBJECT_CONS_QUALIFIER("second-object-cons-qualifier"),
    OBJECT_CONS_WITHOUT_FIRST_QUALIFIER("object-cons-without-first-qualifier"),
    OBJECT_TYPE_WITHOUT_FIRST_QUALIFIER("object-type-without-first-qualifier"),
    OBJECT_TYPE_START("object-type-start"),
    OBJECT_CONSTRUCTOR_START("object-constructor-start"),
    IMPORT_DECL("import-decl"),
    IMPORT_ORG_OR_MODULE_NAME("import-org-or-module-name"),
    IMPORT_MODULE_NAME("module-name"),
    IMPORT_PREFIX("import-prefix"),
    IMPORT_PREFIX_DECL("import-alias"),
    IMPORT_DECL_ORG_OR_MODULE_NAME_RHS("import-decl-org-or-module-name-rhs"),
    AFTER_IMPORT_MODULE_NAME("after-import-module-name"),
    SERVICE_DECL("service-decl"),
    SERVICE_DECL_START("service-decl-start"),
    SERVICE_DECL_QUALIFIER("service-decl-qualifier"),
    SERVICE_DECL_OR_VAR_DECL("service-decl-or-var-decl"),
    SERVICE_VAR_DECL_RHS("service-var-decl-rhs"),
    OPTIONAL_SERVICE_DECL_TYPE("optional-service-decl-type"),
    OPTIONAL_ABSOLUTE_PATH("optional-absolute-path"),
    ABSOLUTE_RESOURCE_PATH("absolute-resource-path"),
    ABSOLUTE_RESOURCE_PATH_START("absolute-resource-path-start"),
    ABSOLUTE_PATH_SINGLE_SLASH("absolute-path-single-slash"),
    ABSOLUTE_RESOURCE_PATH_END("absolute-resource-path-end"),
    SERVICE_DECL_RHS("service-decl-rhs"),
    LISTENERS_LIST("listeners-list"),
    LISTENERS_LIST_END("listeners-list-end"),
    OBJECT_CONSTRUCTOR_BLOCK("object-constructor-block"),
    RESOURCE_KEYWORD_RHS("resource-keyword-rhs"),
    OPTIONAL_RELATIVE_PATH("optional-relative-path"),
    RELATIVE_RESOURCE_PATH("relative-resource-path"),
    RELATIVE_RESOURCE_PATH_START("relative-resource-path-start"),
    RESOURCE_PATH_SEGMENT("resource-path-segment"),
    RESOURCE_PATH_PARAM("resource-path-param"),
    PATH_PARAM_OPTIONAL_ANNOTS("path-param-optional-annots"),
    PATH_PARAM_ELLIPSIS("path-param-ellipsis"),
    OPTIONAL_PATH_PARAM_NAME("optional-path-param-name"),
    RELATIVE_RESOURCE_PATH_END("relative-resource-path-end"),
    RESOURCE_PATH_END("relative-resource-path-end"),
    RESOURCE_ACCESSOR_DEF_OR_DECL_RHS("resource-accessor-def-or-decl-rhs"),
    LISTENER_DECL("listener-decl"),
    CONSTANT_DECL("const-decl"),
    CONST_DECL_TYPE("const-decl-type"),
    CONST_DECL_RHS("const-decl-rhs"),
    NIL_TYPE_DESCRIPTOR("nil-type-descriptor"),
    OPTIONAL_TYPE_DESCRIPTOR("optional-type-descriptor"),
    ARRAY_TYPE_DESCRIPTOR("array-type-descriptor"),
    ARRAY_LENGTH("array-length"),
    ARRAY_LENGTH_START("array-length-start"),
    ANNOT_REFERENCE("annot-reference"),
    ANNOTATIONS("annots"),
    ANNOTATION_END("annot-end"),
    ANNOTATION_REF_RHS("annot-ref-rhs"),
    DOC_STRING("doc-string"),
    QUALIFIED_IDENTIFIER("qualified-identifier"),
    EQUAL_OR_RIGHT_ARROW("equal-or-right-arrow"),
    ANNOTATION_DECL("annotation-decl"),
    ANNOT_DECL_OPTIONAL_TYPE("annot-decl-optional-type"),
    ANNOT_DECL_RHS("annot-decl-rhs"),
    ANNOT_OPTIONAL_ATTACH_POINTS("annot-optional-attach-points"),
    ANNOT_ATTACH_POINTS_LIST("annot-attach-points-list"),
    ATTACH_POINT("attach-point"),
    ATTACH_POINT_IDENT("attach-point-ident"),
    SINGLE_KEYWORD_ATTACH_POINT_IDENT("single-keyword-attach-point-ident"),
    IDENT_AFTER_OBJECT_IDENT("ident-after-object-ident"),
    XML_NAMESPACE_DECLARATION("xml-namespace-decl"),
    XML_NAMESPACE_PREFIX_DECL("namespace-prefix-decl"),
    DEFAULT_WORKER_INIT("default-worker-init"),
    NAMED_WORKERS("named-workers"),
    WORKER_NAME_RHS("worker-name-rhs"),
    DEFAULT_WORKER("default-worker-init"),
    KEY_SPECIFIER("key-specifier"),
    KEY_SPECIFIER_RHS("key-specifier-rhs"),
    TABLE_KEY_RHS("table-key-rhs"),
    LET_EXPR_LET_VAR_DECL("let-expr-let-var-decl"),
    LET_CLAUSE_LET_VAR_DECL("let-clause-let-var-decl"),
    LET_VAR_DECL_START("let-var-decl-start"),
    FUNC_TYPE_DESC("func-type-desc"),
    FUNC_TYPE_DESC_START("func-type-desc-start"),
    FUNC_TYPE_FIRST_QUALIFIER("func-type-first-qualifier"),
    FUNC_TYPE_SECOND_QUALIFIER("func-type-second-qualifier"),
    FUNC_TYPE_DESC_START_WITHOUT_FIRST_QUAL("func-type-desc-start-without-first-qual"),
    FUNCTION_KEYWORD_RHS("func-keyword-rhs"),
    END_OF_TYPE_DESC("end-of-type-desc"),
    SELECT_CLAUSE("select-clause"),
    COLLECT_CLAUSE("collect-clause"),
    RESULT_CLAUSE("result-clause"),
    WHERE_CLAUSE("where-clause"),
    FROM_CLAUSE("from-clause"),
    LET_CLAUSE("let-clause"),
    MODULE_LEVEL_AMBIGUOUS_FUNC_TYPE_DESC_RHS("module-level-func-type-desc-rhs"),
    EXPLICIT_ANON_FUNC_EXPR_BODY_START("explicit-anon-func-expr-body-start"),
    BRACED_EXPR_OR_ANON_FUNC_PARAMS("braced-expr-or-anon-func-params"),
    BRACED_EXPR_OR_ANON_FUNC_PARAM_RHS("braced-expr-or-anon-func-param-rhs"),
    ANON_FUNC_PARAM_RHS("anon-func-param-rhs"),
    IMPLICIT_ANON_FUNC_PARAM("implicit-anon-func-param"),
    OPTIONAL_PEER_WORKER("optional-peer-worker"),
    METHOD_NAME("method-name"),
    PEER_WORKER_NAME("peer-worker-name"),
    TYPE_DESC_IN_TUPLE_RHS("type-desc-in-tuple-rhs"),
    TUPLE_TYPE_MEMBER_RHS("tuple-type-member-rhs"),
    NIL_OR_PARENTHESISED_TYPE_DESC_RHS("nil-or-parenthesised-tpe-desc-rhs"),
    REMOTE_OR_RESOURCE_CALL_OR_ASYNC_SEND_RHS("remote-or-resource-call-or-async-send-rhs"),
    REMOTE_CALL_OR_ASYNC_SEND_END("remote-call-or-async-send-end"),
    DEFAULT_WORKER_NAME_IN_ASYNC_SEND("default-worker-name-in-async-send"),
    RECEIVE_WORKERS("receive-workers"),
    MULTI_RECEIVE_WORKERS("multi-receive-workers"),
    RECEIVE_FIELD_END("receive-field-end"),
    RECEIVE_FIELD("receive-field"),
    RECEIVE_FIELD_NAME("receive-field-name"),
    INFER_PARAM_END_OR_PARENTHESIS_END("infer-param-end-or-parenthesis-end"),
    LIST_CONSTRUCTOR_MEMBER_END("list-constructor-member-end"),
    TYPED_BINDING_PATTERN("typed-binding-pattern"),
    BINDING_PATTERN("binding-pattern"),
    CAPTURE_BINDING_PATTERN("capture-binding-pattern"),
    REST_BINDING_PATTERN("rest-binding-pattern"),
    LIST_BINDING_PATTERN("list-binding-pattern"),
    LIST_BINDING_PATTERNS_START("list-binding-patterns-start"),
    LIST_BINDING_PATTERN_MEMBER("list-binding-pattern-member"),
    LIST_BINDING_PATTERN_MEMBER_END("list-binding-pattern-member-end"),
    FIELD_BINDING_PATTERN("field-binding-pattern"),
    FIELD_BINDING_PATTERN_NAME("field-binding-pattern-name"),
    MAPPING_BINDING_PATTERN("mapping-binding-pattern"),
    MAPPING_BINDING_PATTERN_MEMBER("mapping-binding-pattern-member"),
    MAPPING_BINDING_PATTERN_END("mapping-binding-pattern-end"),
    FIELD_BINDING_PATTERN_END("field-binding-pattern-end-or-continue"),
    ERROR_BINDING_PATTERN("error-binding-pattern"),
    ERROR_BINDING_PATTERN_ERROR_KEYWORD_RHS("error-binding-pattern-error-keyword-rhs"),
    ERROR_ARG_LIST_BINDING_PATTERN_START("error-arg-list-binding-pattern-start"),
    SIMPLE_BINDING_PATTERN("simple-binding-pattern"),
    ERROR_MESSAGE_BINDING_PATTERN_END("error-message-binding-pattern-end"),
    ERROR_MESSAGE_BINDING_PATTERN_END_COMMA("error-message-binding-pattern-end-comma"),
    ERROR_MESSAGE_BINDING_PATTERN_RHS("error-message-binding-pattern-rhs"),
    ERROR_CAUSE_SIMPLE_BINDING_PATTERN("error-cause-simple-binding-pattern"),
    ERROR_FIELD_BINDING_PATTERN("error-field-binding-pattern"),
    ERROR_FIELD_BINDING_PATTERN_END("error-field-binding-pattern-end"),
    NAMED_ARG_BINDING_PATTERN("named-arg-binding-pattern"),
    BINDING_PATTERN_STARTING_IDENTIFIER("binding-pattern-starting-indentifier"),
    WAIT_KEYWORD_RHS("wait-keyword-rhs"),
    MULTI_WAIT_FIELDS("multi-wait-fields"),
    WAIT_FIELD_NAME("wait-field-name"),
    WAIT_FIELD_NAME_RHS("wait-field-name-rhs"),
    WAIT_FIELD_END("wait-field-end"),
    WAIT_FUTURE_EXPR_END("wait-future-expr-end"),
    ALTERNATE_WAIT_EXPRS("alternate-wait-exprs"),
    ALTERNATE_WAIT_EXPR_LIST_END("alternate-wait-expr-lit-end"),
    DO_CLAUSE("do-clause"),
    MODULE_ENUM_DECLARATION("module-enum-declaration"),
    MODULE_ENUM_NAME("module-enum-name"),
    ENUM_MEMBER_NAME("enum-member-name"),
    MEMBER_ACCESS_KEY_EXPR_END("member-access-key-expr-end"),
    MEMBER_ACCESS_KEY_EXPR("member-access-key-expr"),
    RETRY_KEYWORD_RHS("retry-keyword-rhs"),
    RETRY_TYPE_PARAM_RHS("retry-type-param-rhs"),
    RETRY_BODY("retry-body"),
    ROLLBACK_RHS("rollback-rhs"),
    STMT_START_BRACKETED_LIST("stmt-start-bracketed-list"),
    STMT_START_BRACKETED_LIST_MEMBER("stmt-start-bracketed-list-member"),
    STMT_START_BRACKETED_LIST_RHS("stmt-start-bracketed-list-rhs"),
    BRACKETED_LIST("bracketed-list"),
    BRACKETED_LIST_RHS("bracketed-list-rhs"),
    BRACED_LIST_RHS("braced-list-rhs"),
    BRACKETED_LIST_MEMBER("bracketed-list-member"),
    BRACKETED_LIST_MEMBER_END("bracketed-list-member-end"),
    LIST_BINDING_MEMBER_OR_ARRAY_LENGTH("list-binding-member-or-array-length"),
    TYPED_BINDING_PATTERN_TYPE_RHS("type-binding-pattern-type-rhs"),
    UNION_OR_INTERSECTION_TOKEN("union-or-intersection"),
    MAPPING_BP_OR_MAPPING_CONSTRUCTOR("mapping-bp-or-mapping-cons"),
    MAPPING_BP_OR_MAPPING_CONSTRUCTOR_MEMBER("mapping-bp-or-mapping-cons-member"),
    LIST_BP_OR_LIST_CONSTRUCTOR_MEMBER("list-bp-or-list-cons-member"),
    VAR_REF_OR_TYPE_REF("var-ref"),
    FUNC_TYPE_DESC_OR_ANON_FUNC("func-desc-type-or-anon-func"),
    FUNC_TYPE_DESC_OR_ANON_FUNC_START("func-desc-type-or-anon-func-start"),
    FUNC_TYPE_DESC_RHS_OR_ANON_FUNC_BODY("func-type-desc-rhs-or-anon-func-body"),
    STMT_LEVEL_AMBIGUOUS_FUNC_TYPE_DESC_RHS("stmt-level-func-type-desc-rhs"),
    RECORD_FIELD_NAME_OR_TYPE_NAME("record-field-name-or-type-name"),
    MATCH_BODY("match-body"),
    MATCH_PATTERN("match-pattern"),
    MATCH_PATTERN_START("match-pattern-start"),
    MATCH_PATTERN_END("match-pattern-end"),
    MATCH_PATTERN_RHS("match-pattern-rhs"),
    MATCH_PATTERN_LIST_MEMBER_RHS("match-pattern-list-memebr-rhs"),
    OPTIONAL_MATCH_GUARD("optional-match-guard"),
    LIST_MATCH_PATTERN("list-match-pattern"),
    LIST_MATCH_PATTERNS_START("list-match-patterns-start"),
    LIST_MATCH_PATTERN_MEMBER("list-match-pattern-member"),
    LIST_MATCH_PATTERN_MEMBER_RHS("list-match-pattern-member-rhs"),
    REST_MATCH_PATTERN("rest-match-pattern"),
    MAPPING_MATCH_PATTERN("mapping-match-pattern"),
    FIELD_MATCH_PATTERNS_START("field-match-patterns-start"),
    FIELD_MATCH_PATTERN_MEMBER_RHS("field-match-pattern-member-rhs"),
    FIELD_MATCH_PATTERN_MEMBER("field-match-pattern-member"),
    ERROR_MATCH_PATTERN("error-match-pattern"),
    ERROR_MATCH_PATTERN_ERROR_KEYWORD_RHS("error-match-pattern-error-keyword-rhs"),
    ERROR_ARG_LIST_MATCH_PATTERN_FIRST_ARG("error-arg-list-match-pattern-first-arg"),
    ERROR_ARG_LIST_MATCH_PATTERN_START("error-arg-list-match-pattern-start"),
    ERROR_MESSAGE_MATCH_PATTERN_END("error-message-match-pattern-end"),
    ERROR_MESSAGE_MATCH_PATTERN_END_COMMA("error-message-match-pattern-end-comma"),
    ERROR_MESSAGE_MATCH_PATTERN_RHS("error-message-match-pattern-rhs"),
    ERROR_CAUSE_MATCH_PATTERN("error-cause-match-pattern"),
    ERROR_FIELD_MATCH_PATTERN("error-field-match-pattern"),
    ERROR_FIELD_MATCH_PATTERN_RHS("error-field-match-pattern-rhs"),
    ERROR_MATCH_PATTERN_OR_CONST_PATTERN("error-match-pattern-or-const-pattern"),
    NAMED_ARG_MATCH_PATTERN("named-arg-match-pattern"),
    NAMED_ARG_MATCH_PATTERN_RHS("named-arg-match-pattern-rhs"),
    ORDER_BY_CLAUSE("order-by-clause"),
    ORDER_KEY_LIST("order-key-list"),
    ORDER_KEY_LIST_END("order-key-list-end"),
    GROUP_BY_CLAUSE("group-by-clause"),
    GROUPING_KEY_LIST_ELEMENT("grouping-key-list-element"),
    GROUPING_KEY_LIST_ELEMENT_END("grouping-key-list-element-end"),
    GROUP_BY_CLAUSE_END("group-by-clause-end"),
    ON_CONFLICT_CLAUSE("on-conflict-clause"),
    LIMIT_CLAUSE("limit-clause"),
    JOIN_CLAUSE("join-clause"),
    JOIN_CLAUSE_START("join-clause-start"),
    JOIN_CLAUSE_END("join-clause-end"),
    ON_CLAUSE("on-clause"),
    INTERMEDIATE_CLAUSE("intermediate-clause"),
    INTERMEDIATE_CLAUSE_START("intermediate-clause-start"),
    ON_FAIL_CLAUSE("on_fail_clause"),
    ON_FA("on_fail_clause"),
    OPTIONAL_TYPE_PARAMETER("optional-type-parameter"),
    PARAMETERIZED_TYPE("parameterized-type"),
    MAP_TYPE_DESCRIPTOR("map-type-descriptor"),
    MODULE_VAR_DECL("module-var-decl"),
    MODULE_VAR_FIRST_QUAL("module-var-first-qual"),
    MODULE_VAR_SECOND_QUAL("module-var-second-qual"),
    MODULE_VAR_THIRD_QUAL("module-var-third-qual"),
    MODULE_VAR_DECL_START("module-var-decl-start"),
    MODULE_VAR_WITHOUT_FIRST_QUAL("module-var-without-first-qual"),
    MODULE_VAR_WITHOUT_SECOND_QUAL("module-var-without-second-qual"),
    FUNC_DEF_OR_TYPE_DESC_RHS("func-def-or-type-desc-rhs"),
    CLIENT_RESOURCE_ACCESS_ACTION("client-resource-access-action"),
    OPTIONAL_RESOURCE_ACCESS_PATH("optional-resource-access-path"),
    RESOURCE_ACCESS_PATH_SEGMENT("resource-access-path-segment"),
    COMPUTED_SEGMENT_OR_REST_SEGMENT("computed-segment-or-rest-segment"),
    RESOURCE_ACCESS_SEGMENT_RHS("resource-access-segment-rhs"),
    OPTIONAL_RESOURCE_ACCESS_METHOD("optional-resource-access-method"),
    OPTIONAL_RESOURCE_ACCESS_ACTION_ARG_LIST("optional-resource-method-call-arg-list"),
    ACTION_END("action-end"),
    OPTIONAL_PARENTHESIZED_ARG_LIST("optional-parenthesized-arg-list"),
    NATURAL_EXPRESSION("natural-expression"),
    NATURAL_EXPRESSION_START("natural-expression-start"),

    // Statements
    STATEMENT("statement"),
    STATEMENTS("statements"),
    STATEMENT_WITHOUT_ANNOTS("statement-without-annots"),
    ASSIGNMENT_STMT("assignment-stmt"),
    VAR_DECL_STMT("var-decl-stmt"),
    VAR_DECL_STMT_RHS("var-decl-rhs"),
    CONFIG_VAR_DECL_RHS("config-var-decl-rhs"),
    TYPE_NAME_OR_VAR_NAME("type-or-var-name"),
    ASSIGNMENT_OR_VAR_DECL_STMT("assign-or-var-decl"),
    IF_BLOCK("if-block"),
    BLOCK_STMT("block-stmt"),
    ELSE_BLOCK("else-block"),
    ELSE_BODY("else-body"),
    WHILE_BLOCK("while-block"),
    DO_BLOCK("do-block"),
    CALL_STMT("call-statement"),
    CALL_STMT_START("call-statement-start"),
    CONTINUE_STATEMENT("continue-statement"),
    BREAK_STATEMENT("break-statement"),
    PANIC_STMT("panic-statement"),
    RETURN_STMT("return-stmt"),
    RETURN_STMT_RHS("return-stmt-rhs"),
    REGULAR_COMPOUND_STMT_RHS("regular-compound-statement-rhs"),
    LOCAL_TYPE_DEFINITION_STMT("local-type-definition-statement"),
    BINDING_PATTERN_OR_EXPR_RHS("binding-pattern-or-expr-rhs"),
    BINDING_PATTERN_OR_VAR_REF_RHS("binding.pattern.or.var.ref.rhs"),
    TYPE_DESC_OR_EXPR_RHS("type-desc-or-expr-rhs"),
    STMT_START_WITH_EXPR_RHS("stmt-start-with-expr-rhs"),
    EXPR_STMT_RHS("expr-stmt-rhs"),
    EXPRESSION_STATEMENT("expression-statement"),
    EXPRESSION_STATEMENT_START("expression-statement-start"),
    LOCK_STMT("lock-stmt"),
    NAMED_WORKER_DECL("named-worker-decl"),
    NAMED_WORKER_DECL_START("named-worker-decl-start"),
    FORK_STMT("fork-stmt"),
    FOREACH_STMT("foreach-stmt"),
    TRANSACTION_STMT("transaction-stmt"),
    RETRY_STMT("retry-stmt"),
    ROLLBACK_STMT("rollback-stmt"),
    AMBIGUOUS_STMT("ambiguous-stmt"),
    MATCH_STMT("match-stmt"),
    FAIL_STATEMENT("fail-stmt"),

    // Keywords
    RETURNS_KEYWORD("returns"),
    TYPE_KEYWORD("type"),
    CLASS_KEYWORD("class"),
    PUBLIC_KEYWORD("public"),
    PRIVATE_KEYWORD("private"),
    FUNCTION_KEYWORD("function"),
    EXTERNAL_KEYWORD("external"),
    RECORD_KEYWORD("record"),
    OBJECT_KEYWORD("object"),
    ABSTRACT_KEYWORD("abstract"),
    CLIENT_KEYWORD("client"),
    IF_KEYWORD("if"),
    ELSE_KEYWORD("else"),
    WHILE_KEYWORD("while"),
    CONTINUE_KEYWORD("continue"),
    BREAK_KEYWORD("break"),
    PANIC_KEYWORD("panic"),
    IMPORT_KEYWORD("import"),
    AS_KEYWORD("as"),
    RETURN_KEYWORD("return"),
    SERVICE_KEYWORD("service"),
    ON_KEYWORD("on"),
    FINAL_KEYWORD("final"),
    LISTENER_KEYWORD("listener"),
    CONST_KEYWORD("const"),
    TYPEOF_KEYWORD("typeof"),
    IS_KEYWORD("is"),
    MAP_KEYWORD("map"),
    NULL_KEYWORD("null"),
    LOCK_KEYWORD("lock"),
    ANNOTATION_KEYWORD("annotation"),
    SOURCE_KEYWORD("source"),
    XMLNS_KEYWORD("xmlns"),
    WORKER_KEYWORD("worker"),
    FORK_KEYWORD("fork"),
    TRAP_KEYWORD("trap"),
    IN_KEYWORD("in"),
    FOREACH_KEYWORD("foreach"),
    TABLE_KEYWORD("table"),
    KEY_KEYWORD("key"),
    ERROR_KEYWORD("error"),
    LET_KEYWORD("let"),
    STREAM_KEYWORD("stream"),
    XML_KEYWORD("xml"),
    STRING_KEYWORD("string"),
    NEW_KEYWORD("new"),
    FROM_KEYWORD("from"),
    WHERE_KEYWORD("where"),
    SELECT_KEYWORD("select"),
    COLLECT_KEYWORD("collect"),
    START_KEYWORD("start"),
    FLUSH_KEYWORD("flush"),
    WAIT_KEYWORD("wait"),
    DO_KEYWORD("do"),
    TRANSACTION_KEYWORD("transaction"),
    COMMIT_KEYWORD("commit"),
    RETRY_KEYWORD("retry"),
    ROLLBACK_KEYWORD("rollback"),
    TRANSACTIONAL_KEYWORD("transactional"),
    ENUM_KEYWORD("enum"),
    BASE16_KEYWORD("base16"),
    BASE64_KEYWORD("base64"),
    READONLY_KEYWORD("readonly"),
    MATCH_KEYWORD("match"),
    DISTINCT_KEYWORD("distinct"),
    CONFLICT_KEYWORD("conflict"),
    LIMIT_KEYWORD("limit"),
    JOIN_KEYWORD("join"),
    OUTER_KEYWORD("outer"),
    VAR_KEYWORD("var"),
    FAIL_KEYWORD("fail"),
    ORDER_KEYWORD("order"),
    BY_KEYWORD("by"),
    EQUALS_KEYWORD("equals"),
    NOT_IS_KEYWORD("!is"),
    RE_KEYWORD("re"),
    GROUP_KEYWORD("group"),
    NATURAL_KEYWORD("natural"),

    // Syntax tokens
    OPEN_PARENTHESIS("("),
    CLOSE_PARENTHESIS(")"),
    OPEN_BRACE("{"),
    CLOSE_BRACE("}"),
    ASSIGN_OP("="),
    SEMICOLON(";"),
    COLON(":"),
    COMMA(","),
    ELLIPSIS("..."),
    QUESTION_MARK("?"),
    ASTERISK("*"),
    CLOSED_RECORD_BODY_START("{|"),
    CLOSED_RECORD_BODY_END("|}"),
    DOT("."),
    OPEN_BRACKET("["),
    CLOSE_BRACKET("]"),
    SLASH("/"),
    AT("@"),
    RIGHT_ARROW("->"),
    GT(">"),
    LT("<"),
    PIPE("|"),
    TEMPLATE_START("`"),
    TEMPLATE_END("`"),
    LT_TOKEN("<"),
    GT_TOKEN(">"),
    ERROR_TYPE_PARAM_START("<"),
    PARENTHESISED_TYPE_DESC_START("("),
    BITWISE_AND_OPERATOR("&"),
    EXPR_FUNC_BODY_START("=>"),
    PLUS_TOKEN("+"),
    MINUS_TOKEN("-"),
    TUPLE_TYPE_DESC_START("["),
    SYNC_SEND_TOKEN("->>"),
    LEFT_ARROW_TOKEN("<-"),
    ANNOT_CHAINING_TOKEN(".@"),
    OPTIONAL_CHAINING_TOKEN("?."),
    DOT_LT_TOKEN(".<"),
    SLASH_LT_TOKEN("/<"),
    DOUBLE_SLASH_DOUBLE_ASTERISK_LT_TOKEN("/**/<"),
    SLASH_ASTERISK_TOKEN("/*"),
    RIGHT_DOUBLE_ARROW("=>"),
    DOUBLE_LT("<<"),
    DOUBLE_EQUAL("=="),
    BITWISE_XOR("^"),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    ELVIS("?:"),

    // Other terminals
    FUNC_NAME("func-name"),
    VARIABLE_NAME("variable-name"),
    SIMPLE_TYPE_DESCRIPTOR("simple-type-desc"),
    BINARY_OPERATOR("binary-operator"),
    TYPE_NAME("type-name"),
    CLASS_NAME("class-name"),
    BOOLEAN_LITERAL("boolean-literal"),
    CHECKING_KEYWORD("checking-keyword"),
    COMPOUND_BINARY_OPERATOR("compound-binary-operator"),
    UNARY_OPERATOR("unary-operator"),
    FUNCTION_IDENT("func-ident"),
    FIELD_IDENT("field-ident"),
    OBJECT_IDENT("object-ident"),
    SERVICE_IDENT("service-ident"),
    SERVICE_IDENT_RHS("service-ident-rhs"),
    REMOTE_IDENT("remote-ident"),
    RECORD_IDENT("record-ident"),
    ANNOTATION_TAG("annotation-tag"),
    ATTACH_POINT_END("attach-point-end"),
    IDENTIFIER("identifier"),
    PATH_SEGMENT_IDENT("path-segment-ident"),
    NAMESPACE_PREFIX("namespace-prefix"),
    WORKER_NAME("worker-name"),
    FIELD_OR_FUNC_NAME("field-or-func-name"),
    ORDER_DIRECTION("order-direction"),
    VAR_REF_COLON("var-ref-colon"),
    TYPE_REF_COLON("type-ref-colon"),
    METHOD_CALL_DOT("method-call-dot"),
    RESOURCE_METHOD_CALL_SLASH_TOKEN("resource-method-call-slash-token"),

    // Expressions
    EXPRESSION("expression"),
    TERMINAL_EXPRESSION("terminal-expression"),
    EXPRESSION_RHS("expression-rhs"),
    FUNC_CALL("func-call"),
    BASIC_LITERAL("basic-literal"),
    ACCESS_EXPRESSION("access-expr"),   // method-call, field-access, member-access
    DECIMAL_INTEGER_LITERAL_TOKEN("decimal-int-literal-token"),
    VARIABLE_REF("var-ref"),
    STRING_LITERAL_TOKEN("string-literal-token"),
    MAPPING_CONSTRUCTOR("mapping-constructor"),
    MAPPING_FIELD("maping-field"),
    FIRST_MAPPING_FIELD("first-mapping-field"),
    MAPPING_FIELD_NAME("maping-field-name"),
    SPECIFIC_FIELD_RHS("specific-field-rhs"),
    SPECIFIC_FIELD("specific-field"),
    COMPUTED_FIELD_NAME("computed-field-name"),
    MAPPING_FIELD_END("mapping-field-end"),
    TYPEOF_EXPRESSION("typeof-expr"),
    UNARY_EXPRESSION("unary-expr"),
    HEX_INTEGER_LITERAL_TOKEN("hex-integer-literal-token"),
    NIL_LITERAL("nil-literal"),
    CONSTANT_EXPRESSION("constant-expr"),
    CONSTANT_EXPRESSION_START("constant-expr-start"),
    DECIMAL_FLOATING_POINT_LITERAL_TOKEN("decimal-floating-point-literal-token"),
    HEX_FLOATING_POINT_LITERAL_TOKEN("hex-floating-point-literal-token"),
    LIST_CONSTRUCTOR("list-constructor"),
    LIST_CONSTRUCTOR_FIRST_MEMBER("list-constructor-first-member"),
    LIST_CONSTRUCTOR_MEMBER("list-constructor-member"),
    TYPE_CAST("type-cast"),
    TYPE_CAST_PARAM("type-cast-param"),
    TYPE_CAST_PARAM_RHS("type-cast-param-rhs"),
    TYPE_CAST_PARAM_START("type-cast-param-start"),
    TABLE_CONSTRUCTOR("table-constructor"),
    TABLE_KEYWORD_RHS("table-keyword-rhs"),
    ROW_LIST_RHS("row-list-rhs"),
    TABLE_ROW_END("table-row-end"),
    NEW_KEYWORD_RHS("new-keyword-rhs"),
    IMPLICIT_NEW("implicit-new"),
    CLASS_DESCRIPTOR_IN_NEW_EXPR("class-descriptor-in-new-expr"),
    LET_EXPRESSION("let-expr"),
    ANON_FUNC_EXPRESSION("anon-func-expression"),
    ANON_FUNC_EXPRESSION_START("anon-func-expression-start"),
    TABLE_CONSTRUCTOR_OR_QUERY_EXPRESSION("table-constructor-or-query-expr"),
    TABLE_CONSTRUCTOR_OR_QUERY_START("table-constructor-or-query-start"),
    TABLE_CONSTRUCTOR_OR_QUERY_RHS("table-constructor-or-query-rhs"),
    QUERY_EXPRESSION("query-expr"),
    QUERY_EXPRESSION_RHS("query-expr-rhs"),
    QUERY_ACTION_RHS("query-action-rhs"),
    QUERY_EXPRESSION_END("query-expr-end"),
    FIELD_ACCESS_IDENTIFIER("field-access-identifier"),
    QUERY_PIPELINE_RHS("query-pipeline-rhs"),
    LET_CLAUSE_END("let-clause-end"),
    CONDITIONAL_EXPRESSION("conditional-expr"),
    XML_NAVIGATE_EXPR("xml-navigate-expr"),
    XML_FILTER_EXPR("xml-filter-expr"),
    XML_STEP_EXPR("xml-step-expr"),
    XML_NAME_PATTERN("xml-name-pattern"),
    XML_NAME_PATTERN_RHS("xml-name-pattern-rhs"),
    XML_ATOMIC_NAME_PATTERN("xml-atomic_name-pattern"),
    XML_ATOMIC_NAME_PATTERN_START("xml-atomic_name-pattern-start"),
    XML_ATOMIC_NAME_IDENTIFIER("xml-atomic_name-identifier"),
    XML_ATOMIC_NAME_IDENTIFIER_RHS("xml-atomic_name-identifier-rhs"),
    XML_STEP_START("xml-step-start"),
    VARIABLE_REF_RHS("variable-ref-rhs"),
    ORDER_CLAUSE_END("order-clause-end"),
    OBJECT_CONSTRUCTOR("object-constructor"),
    OBJECT_CONSTRUCTOR_TYPE_REF("object-constructor-type-ref"),
    ERROR_CONSTRUCTOR("error-constructor"),
    ERROR_CONSTRUCTOR_RHS("error-constructor-rhs"),
    INFERRED_TYPEDESC_DEFAULT_START_LT("inferred-typedesc-default-start-lt"),
    INFERRED_TYPEDESC_DEFAULT_END_GT("inferred-typedesc-default-end-gt"),
    EXPR_START_OR_INFERRED_TYPEDESC_DEFAULT_START("expr-start-or-inferred-typedesc-default-start"),
    TYPE_CAST_PARAM_START_OR_INFERRED_TYPEDESC_DEFAULT_END("type-cast-param-start-or-inferred-typedesc-default-end"),
    END_OF_PARAMS_OR_NEXT_PARAM_START("end-of-params-or-next-param-start"),
    BRACED_EXPRESSION("braced-expression"),
    ACTION("action"),

    // Contexts that expect a type
    TYPE_DESC_IN_ANNOTATION_DECL("type-desc-annotation-descl"),
    TYPE_DESC_BEFORE_IDENTIFIER("type-desc-before-identifier"), // object/record fields, params, const, listener
    TYPE_DESC_IN_RECORD_FIELD("type-desc-in-record-field"),
    TYPE_DESC_IN_PARAM("type-desc-in-param"),
    TYPE_DESC_IN_TYPE_BINDING_PATTERN("type-desc-in-type-binding-pattern"), // foreach, let-var-decl, var-decl
    TYPE_DESC_IN_TYPE_DEF("type-def-type-desc"),                            // local/mdule type defitions
    TYPE_DESC_IN_ANGLE_BRACKETS("type-desc-in-angle-bracket"),              // type-cast, parameterized-type
    TYPE_DESC_IN_RETURN_TYPE_DESC("type-desc-in-return-type-desc"),
    TYPE_DESC_IN_EXPRESSION("type-desc-in-expression"),
    TYPE_DESC_IN_STREAM_TYPE_DESC("type-desc-in-stream-type-desc"),
    TYPE_DESC_IN_TUPLE("type-desc-in-tuple"),
    TYPE_DESC_IN_PARENTHESIS("type-desc-in-parenthesis"),
    VAR_DECL_STARTED_WITH_DENTIFIER("var-decl-started-with-dentifier"),
    TYPE_DESC_IN_SERVICE("type-desc-in-service"),
    TYPE_DESC_IN_PATH_PARAM("type-desc-in-path-param"),
    TYPE_DESC_BEFORE_IDENTIFIER_IN_GROUPING_KEY("type-desc-before-identifier-in-grouping-key"),

    // XML
    XML_CONTENT("xml-content"),
    XML_TAG("xml-tag"),
    XML_START_OR_EMPTY_TAG("xml-start-or-empty-tag"),
    XML_START_OR_EMPTY_TAG_END("xml-start-or-empty-tag-end"),
    XML_END_TAG("xml-end-tag"),
    XML_NAME("xml-name"),
    XML_PI("xml-pi"),
    XML_TEXT("xml-text"),
    XML_ATTRIBUTES("xml-attributes"),
    XML_ATTRIBUTE("xml-attribute"),
    XML_ATTRIBUTE_VALUE_ITEM("xml-attribute-value-item"),
    XML_ATTRIBUTE_VALUE_TEXT("xml-attribute-value-text"),
    XML_COMMENT_START("<!--"),
    XML_COMMENT_END("-->"),
    XML_COMMENT_CONTENT("xml-comment-content"),
    XML_PI_START("<?"),
    XML_PI_END("?>"),
    XML_PI_DATA("xml-pi-data"),
    XML_PI_TARGET_RHS("xml-pi-target-rhs"),
    INTERPOLATION_START_TOKEN("${"),
    INTERPOLATION("interoplation"),
    TEMPLATE_BODY("template-body"),
    TEMPLATE_MEMBER("template-member"),
    TEMPLATE_STRING("template-string"),
    TEMPLATE_STRING_RHS("template-string-rhs"),
    XML_QUOTE_START("xml-quote-start"),
    XML_QUOTE_END("xml-quote-end"),
    XML_CDATA_START("xml-cdata-start"),
    XML_OPTIONAL_CDATA_CONTENT("xml-optional-cdata-content"),
    XML_CDATA_CONTENT("xml-cdata-content"),
    XML_CDATA_END("xml-cdata-end"),

    //Other
    TYPE_DESC_RHS("type-desc-rhs"),
    FUNC_TYPE_FUNC_KEYWORD_RHS("func-type-func-keyword-rhs"),
    FUNC_TYPE_FUNC_KEYWORD_RHS_START("func-type-func-keyword-rhs-start"),
    STREAM_TYPE_PARAM_START_TOKEN("stream-type-param-start-token"),
    STREAM_TYPE_FIRST_PARAM_RHS("stream-type-params"),
    KEY_CONSTRAINTS_RHS("key-constraints-rhs"),
    ROW_TYPE_PARAM("row-type-param"),
    TABLE_TYPE_DESC_RHS("table-type-desc-rhs"),
    SIGNED_INT_OR_FLOAT_RHS("signed-int-or-float-rhs"),
    ENUM_MEMBER_LIST("enum-member-list"),
    ENUM_MEMBER_END("enum-member-rhs"),
    ENUM_MEMBER_RHS("enum-member-internal-rhs"),
    ENUM_MEMBER_START("enum-member-start"),
    TUPLE_TYPE_DESC_OR_LIST_CONST_MEMBER("tuple-type-desc-or-list-cont-member"),
    MAP_TYPE_OR_TYPE_REF("map-type-or-type-ref"),
    OBJECT_TYPE_OR_TYPE_REF("object-type-or-type-ref"),
    STREAM_TYPE_OR_TYPE_REF("stream-type-or-type-ref"),
    TABLE_TYPE_OR_TYPE_REF("table-type-or-type-ref"),
    PARAMETERIZED_TYPE_OR_TYPE_REF("parameterized-type-or-type-ref"),
    TYPE_DESC_RHS_OR_TYPE_REF("type-desc-rhs-or-type-ref"),
    OBJECT_TYPE_OBJECT_KEYWORD_RHS("object-type-object-keyword-rhs"),
    TABLE_CONS_OR_QUERY_EXPR_OR_VAR_REF("table-cons-or-query-expr-or-var-ref"),
    EXPRESSION_START_TABLE_KEYWORD_RHS("expression-start-table-keyword-rhs"),
    QUERY_EXPR_OR_VAR_REF("query-expr-or-var-ref"),
    QUERY_CONSTRUCT_TYPE_RHS("query-construct-type-rhs"),
    ERROR_CONS_EXPR_OR_VAR_REF("error-cons-expr-or-var-ref"),
    ERROR_CONS_ERROR_KEYWORD_RHS("error-cons-error-keyword-rhs"),
    TRANSACTION_STMT_TRANSACTION_KEYWORD_RHS("transaction-stmt-transaction-keyword-rhs"),
    TRANSACTION_STMT_RHS_OR_TYPE_REF("transaction-stmt-rhs-or-type-ref"),
    QUALIFIED_IDENTIFIER_START_IDENTIFIER("qualified-identifier-start-identifier"),
    QUALIFIED_IDENTIFIER_PREDECLARED_PREFIX("qualified-identifier-predeclared-prefix"),
    TYPE_DESC_RHS_OR_BP_RHS("type-desc-rhs-or-binding-pattern-rhs"),
    LIST_BINDING_PATTERN_RHS("list-binding-pattern-rhs"),
    TYPE_DESC_RHS_IN_TYPED_BP("type-desc-rhs-in-typed-binding-pattern"),
    ASSIGNMENT_STMT_RHS("assignment-stmt-rhs"),
    ANNOTATION_DECL_START("annotation-declaration-start"),
    OPTIONAL_TOP_LEVEL_SEMICOLON("optional-top-level-semicolon"),
    TUPLE_MEMBERS("tuple-members"),
    TUPLE_MEMBER("tuple-member"),
    SINGLE_OR_ALTERNATE_WORKER("single-or-alternate-worker"),
    SINGLE_OR_ALTERNATE_WORKER_SEPARATOR("single-or-alternate-worker-separator"),
    SINGLE_OR_ALTERNATE_WORKER_END("single-or-alternate-worker-end"),
    XML_STEP_EXTENDS("xml-step-extends"),
    XML_STEP_EXTEND("xml-step-extend"),
    XML_STEP_EXTEND_END("xml-step-extend-end"),
    XML_STEP_START_END("xml-step-start-end")
    ;

    private final String value;

    ParserRuleContext(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
