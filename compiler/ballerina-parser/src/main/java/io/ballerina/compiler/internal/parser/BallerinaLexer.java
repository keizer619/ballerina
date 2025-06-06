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

import io.ballerina.compiler.internal.diagnostics.DiagnosticErrorCode;
import io.ballerina.compiler.internal.parser.tree.STNode;
import io.ballerina.compiler.internal.parser.tree.STNodeFactory;
import io.ballerina.compiler.internal.parser.tree.STToken;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.text.CharReader;

import java.util.ArrayList;
import java.util.List;

/**
 * A LL(k) lexer for ballerina.
 *
 * @since 1.2.0
 */
public class BallerinaLexer extends AbstractLexer {

    public BallerinaLexer(CharReader charReader) {
        super(charReader, ParserMode.DEFAULT);
    }

    /**
     * Get the next lexical token.
     *
     * @return Next lexical token.
     */
    @Override
    public STToken nextToken() {
        STToken token = switch (this.mode) {
            case TEMPLATE -> readTemplateToken();
            case PROMPT -> readPromptToken();
            case REGEXP -> readRegExpTemplateToken();
            case INTERPOLATION -> {
                processLeadingTrivia();
                yield readTokenInInterpolation();
            }
            case INTERPOLATION_BRACED_CONTENT -> {
                processLeadingTrivia();
                yield readTokenInBracedContentInInterpolation();
            }
            default -> {
                processLeadingTrivia();
                yield readToken();
            }
        };

        // Can we improve this logic by creating the token with diagnostics then and there?
        return cloneWithDiagnostics(token);
    }

    /*
     * Private Methods
     */

    private STToken readToken() {
        reader.mark();
        if (reader.isEOF()) {
            return getSyntaxToken(SyntaxKind.EOF_TOKEN);
        }

        int c = reader.peek();
        if (c == LexerTerminals.BACKSLASH) {
            processUnquotedIdentifier();
            return getIdentifierToken();
        }

        reader.advance();
        STToken token;
        switch (c) {
            // Separators
            case LexerTerminals.COLON:
                token = getSyntaxToken(SyntaxKind.COLON_TOKEN);
                break;
            case LexerTerminals.SEMICOLON:
                token = getSyntaxToken(SyntaxKind.SEMICOLON_TOKEN);
                break;
            case LexerTerminals.DOT:
                token = processDot();
                break;
            case LexerTerminals.COMMA:
                token = getSyntaxToken(SyntaxKind.COMMA_TOKEN);
                break;
            case LexerTerminals.OPEN_PARANTHESIS:
                token = getSyntaxToken(SyntaxKind.OPEN_PAREN_TOKEN);
                break;
            case LexerTerminals.CLOSE_PARANTHESIS:
                token = getSyntaxToken(SyntaxKind.CLOSE_PAREN_TOKEN);
                break;
            case LexerTerminals.OPEN_BRACE:
                if (peek() == LexerTerminals.PIPE) {
                    reader.advance();
                    token = getSyntaxToken(SyntaxKind.OPEN_BRACE_PIPE_TOKEN);
                } else {
                    token = getSyntaxToken(SyntaxKind.OPEN_BRACE_TOKEN);
                }
                break;
            case LexerTerminals.CLOSE_BRACE:
                token = getSyntaxToken(SyntaxKind.CLOSE_BRACE_TOKEN);
                break;
            case LexerTerminals.OPEN_BRACKET:
                token = getSyntaxToken(SyntaxKind.OPEN_BRACKET_TOKEN);
                break;
            case LexerTerminals.CLOSE_BRACKET:
                token = getSyntaxToken(SyntaxKind.CLOSE_BRACKET_TOKEN);
                break;
            case LexerTerminals.PIPE:
                token = processPipeOperator();
                break;
            case LexerTerminals.QUESTION_MARK:
                if (peek() == LexerTerminals.DOT && reader.peek(1) != LexerTerminals.DOT) {
                    reader.advance();
                    token = getSyntaxToken(SyntaxKind.OPTIONAL_CHAINING_TOKEN);
                } else if (peek() == LexerTerminals.COLON) {
                    reader.advance();
                    token = getSyntaxToken(SyntaxKind.ELVIS_TOKEN);
                } else {
                    token = getSyntaxToken(SyntaxKind.QUESTION_MARK_TOKEN);
                }
                break;
            case LexerTerminals.DOUBLE_QUOTE:
                token = processStringLiteral();
                break;
            case LexerTerminals.HASH:
                token = processDocumentationString();
                break;
            case LexerTerminals.AT:
                token = getSyntaxToken(SyntaxKind.AT_TOKEN);
                break;

            // Arithmetic operators
            case LexerTerminals.EQUAL:
                token = processEqualOperator();
                break;
            case LexerTerminals.PLUS:
                token = getSyntaxToken(SyntaxKind.PLUS_TOKEN);
                break;
            case LexerTerminals.MINUS:
                if (reader.peek() == LexerTerminals.GT) {
                    reader.advance();
                    if (peek() == LexerTerminals.GT) {
                        reader.advance();
                        token = getSyntaxToken(SyntaxKind.SYNC_SEND_TOKEN);
                    } else {
                        token = getSyntaxToken(SyntaxKind.RIGHT_ARROW_TOKEN);
                    }
                } else {
                    token = getSyntaxToken(SyntaxKind.MINUS_TOKEN);
                }
                break;
            case LexerTerminals.ASTERISK:
                token = getSyntaxToken(SyntaxKind.ASTERISK_TOKEN);
                break;
            case LexerTerminals.SLASH:
                token = processSlashToken();
                break;
            case LexerTerminals.PERCENT:
                token = getSyntaxToken(SyntaxKind.PERCENT_TOKEN);
                break;
            case LexerTerminals.LT:
                token = processTokenStartWithLt();
                break;
            case LexerTerminals.GT:
                token = processTokenStartWithGt();
                break;
            case LexerTerminals.EXCLAMATION_MARK:
                token = processExclamationMarkOperator();
                break;
            case LexerTerminals.BITWISE_AND:
                if (peek() == LexerTerminals.BITWISE_AND) {
                    reader.advance();
                    token = getSyntaxToken(SyntaxKind.LOGICAL_AND_TOKEN);
                } else {
                    token = getSyntaxToken(SyntaxKind.BITWISE_AND_TOKEN);
                }
                break;
            case LexerTerminals.BITWISE_XOR:
                token = getSyntaxToken(SyntaxKind.BITWISE_XOR_TOKEN);
                break;
            case LexerTerminals.NEGATION:
                token = getSyntaxToken(SyntaxKind.NEGATION_TOKEN);
                break;
            case LexerTerminals.BACKTICK:
                startMode(ParserMode.TEMPLATE);
                token = getBacktickToken();
                break;
            case LexerTerminals.SINGLE_QUOTE:
                token = processQuotedIdentifier();
                break;

            // Numbers
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                token = processNumericLiteral(c);
                break;

            default:
                if (isIdentifierInitialChar(c)) {
                    token = processIdentifierOrKeyword();
                    break;
                }

                // Process invalid token as trivia, and continue to next token
                STToken invalidToken = processInvalidToken();
                token = nextToken();
                token = SyntaxErrors.addDiagnostic(token, DiagnosticErrorCode.ERROR_INVALID_TOKEN, invalidToken);
                break;
        }

        return token;
    }

    private STToken getSyntaxToken(SyntaxKind kind) {
        STNode leadingTrivia = getLeadingTrivia();
        STNode trailingTrivia = processTrailingTrivia();
        return STNodeFactory.createToken(kind, leadingTrivia, trailingTrivia);
    }

    private STToken getIdentifierToken() {
        STNode leadingTrivia = getLeadingTrivia();
        String lexeme = getLexeme();
        STNode trailingTrivia = processTrailingTrivia();
        return STNodeFactory.createIdentifierToken(lexeme, leadingTrivia, trailingTrivia);
    }

    private STToken getLiteral(SyntaxKind kind) {
        STNode leadingTrivia = getLeadingTrivia();
        String lexeme = getLexeme();
        STNode trailingTrivia = processTrailingTrivia();
        return STNodeFactory.createLiteralValueToken(kind, lexeme, leadingTrivia, trailingTrivia);
    }

    /**
     * Process leading trivia.
     */
    private void processLeadingTrivia() {
        processSyntaxTrivia(this.leadingTriviaList, true);
    }

    /**
     * Process and return trailing trivia.
     *
     * @return Trailing trivia
     */
    private STNode processTrailingTrivia() {
        List<STNode> triviaList = new ArrayList<>(INITIAL_TRIVIA_CAPACITY);
        processSyntaxTrivia(triviaList, false);
        return STNodeFactory.createNodeList(triviaList);
    }

    /**
     * Process syntax trivia and add it to the provided list.
     * <p>
     * <code>syntax-trivia := whitespace | end-of-line | comments</code>
     *
     * @param triviaList List of trivia
     * @param isLeading Flag indicating whether the currently processing leading trivia or not
     */
    private void processSyntaxTrivia(List<STNode> triviaList, boolean isLeading) {
        while (!reader.isEOF()) {
            reader.mark();
            char c = reader.peek();
            switch (c) {
                case LexerTerminals.SPACE:
                case LexerTerminals.TAB:
                case LexerTerminals.FORM_FEED:
                    triviaList.add(processWhitespaces());
                    break;
                case LexerTerminals.CARRIAGE_RETURN:
                case LexerTerminals.NEWLINE:
                    triviaList.add(processEndOfLine());
                    if (isLeading) {
                        break;
                    }
                    return;
                case LexerTerminals.SLASH:
                    if (reader.peek(1) == LexerTerminals.SLASH) {
                        triviaList.add(processComment());
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /**
     * Process whitespace up to an end of line.
     * <p>
     * <code>whitespace := 0x9 | 0xC | 0x20</code>
     *
     * @return Whitespace trivia
     */
    private STNode processWhitespaces() {
        while (!reader.isEOF()) {
            char c = reader.peek();
            switch (c) {
                case LexerTerminals.SPACE:
                case LexerTerminals.TAB:
                case LexerTerminals.FORM_FEED:
                    reader.advance();
                    continue;
                case LexerTerminals.CARRIAGE_RETURN:
                case LexerTerminals.NEWLINE:
                    break;
                default:
                    break;
            }
            break;
        }

        return STNodeFactory.createMinutiae(SyntaxKind.WHITESPACE_MINUTIAE, getLexeme());
    }

    /**
     * Process end of line.
     * <p>
     * <code>end-of-line := 0xA | 0xD</code>
     *
     * @return End of line trivia
     */
    private STNode processEndOfLine() {
        char c = reader.peek();
        switch (c) {
            case LexerTerminals.NEWLINE:
                reader.advance();
                return STNodeFactory.createMinutiae(SyntaxKind.END_OF_LINE_MINUTIAE, getLexeme());
            case LexerTerminals.CARRIAGE_RETURN:
                reader.advance();
                if (reader.peek() == LexerTerminals.NEWLINE) {
                    reader.advance();
                }
                return STNodeFactory.createMinutiae(SyntaxKind.END_OF_LINE_MINUTIAE, getLexeme());
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Process dot, ellipsis or decimal floating point token.
     *
     * @return Dot, ellipsis or decimal floating point token
     */
    private STToken processDot() {
        int nextChar = reader.peek();
        if (nextChar == LexerTerminals.DOT) {
            int nextNextChar = reader.peek(1);
            if (nextNextChar == LexerTerminals.DOT) {
                reader.advance(2);
                return getSyntaxToken(SyntaxKind.ELLIPSIS_TOKEN);
            } else if (nextNextChar == LexerTerminals.LT) {
                reader.advance(2);
                return getSyntaxToken(SyntaxKind.DOUBLE_DOT_LT_TOKEN);
            }
        } else if (nextChar == LexerTerminals.AT) {
            reader.advance();
            return getSyntaxToken(SyntaxKind.ANNOT_CHAINING_TOKEN);
        } else if (nextChar == LexerTerminals.LT) {
            reader.advance();
            return getSyntaxToken(SyntaxKind.DOT_LT_TOKEN);
        }

        if (this.mode != ParserMode.IMPORT && isDigit(nextChar)) {
            return processDecimalFloatLiteral();
        }
        return getSyntaxToken(SyntaxKind.DOT_TOKEN);
    }

    /**
     * <p>
     * Process a comment, and add it to trivia list.
     * </p>
     * <code>
     * Comment := // AnyCharButNewline*
     * <br/><br/>
     * AnyCharButNewline := ^ 0xA
     * </code>
     */
    private STNode processComment() {
        // We reach here after verifying up to 2 code-points ahead. Hence advance(2).
        reader.advance(2);
        int nextToken = peek();
        while (!reader.isEOF()) {
            switch (nextToken) {
                case LexerTerminals.NEWLINE:
                case LexerTerminals.CARRIAGE_RETURN:
                    break;
                default:
                    reader.advance();
                    nextToken = peek();
                    continue;
            }
            break;
        }

        return STNodeFactory.createMinutiae(SyntaxKind.COMMENT_MINUTIAE, getLexeme());
    }

    /**
     * Process any token that starts with '='.
     *
     * @return One of the tokens: <code>'=', '==', '=>', '==='</code>
     */
    private STToken processEqualOperator() {
        switch (peek()) { // check for the second char
            case LexerTerminals.EQUAL:
                reader.advance();
                if (peek() == LexerTerminals.EQUAL) {
                    // this is '==='
                    reader.advance();
                    return getSyntaxToken(SyntaxKind.TRIPPLE_EQUAL_TOKEN);
                } else {
                    // this is '=='
                    return getSyntaxToken(SyntaxKind.DOUBLE_EQUAL_TOKEN);
                }
            case LexerTerminals.GT:
                // this is '=>'
                reader.advance();
                return getSyntaxToken(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN);
            default:
                // this is '='
                return getSyntaxToken(SyntaxKind.EQUAL_TOKEN);
        }
    }

    /**
     * <p>
     * Process and returns a numeric literal.
     * </p>
     * <code>
     * numeric-literal := int-literal | floating-point-literal
     * <br/>
     * floating-point-literal := DecimalFloatingPointNumber | HexFloatingPointLiteral
     * <br/>
     * int-literal := DecimalNumber | HexIntLiteral
     * <br/>
     * DecimalNumber := 0 | NonZeroDigit Digit*
     * <br/>
     * Digit := 0 .. 9
     * <br/>
     * NonZeroDigit := 1 .. 9
     * </code>
     *
     * @return The numeric literal.
     */
    private STToken processNumericLiteral(int startChar) {
        int nextChar = peek();
        if (isHexIndicator(startChar, nextChar)) {
            return processHexLiteral();
        }

        int len = 1;
        while (!reader.isEOF()) {
            switch (nextChar) {
                case LexerTerminals.DOT:
                case 'e':
                case 'E':
                case 'f':
                case 'F':
                case 'd':
                case 'D':
                    char nextNextChar = reader.peek(1);
                    if (nextChar == LexerTerminals.DOT &&
                            (nextNextChar == LexerTerminals.DOT || isDecimalNumberFollowedIdentifier())) {
                        // This is to handle two cases:
                        // 1. More than one dot. e.g. 2...10
                        // 2. Method call. e.g. 2.toString()
                        break;
                    }

                    // In sem-var mode, only decimal integer literals are supported
                    if (this.mode == ParserMode.IMPORT) {
                        break;
                    }

                    // Integer part of the float cannot have a leading zero
                    if (startChar == '0' && len > 1) {
                        reportLexerError(DiagnosticErrorCode.ERROR_LEADING_ZEROS_IN_NUMERIC_LITERALS);
                    }

                    // Code would not reach here if the floating point starts with a dot
                    return processDecimalFloatLiteral();
                default:
                    if (isDigit(nextChar)) {
                        reader.advance();
                        len++;
                        nextChar = peek();
                        continue;
                    }
                    break;
            }
            break;
        }

        // Integer cannot have a leading zero
        if (startChar == '0' && len > 1) {
            reportLexerError(DiagnosticErrorCode.ERROR_LEADING_ZEROS_IN_NUMERIC_LITERALS);
        }

        return getLiteral(SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN);
    }

    /**
     * <p>
     * Process and returns a decimal floating point literal.
     * </p>
     * <code>
     * DecimalFloatingPointNumber :=
     *    DecimalNumber Exponent [FloatingPointTypeSuffix]
     *    | DottedDecimalNumber [Exponent] [FloatingPointTypeSuffix]
     *    | DecimalNumber FloatingPointTypeSuffix
     * <br/>
     * DottedDecimalNumber := DecimalNumber . Digit+ | . Digit+
     * <br/>
     * FloatingPointTypeSuffix := DecimalTypeSuffix | FloatTypeSuffix
     * <br/>
     * DecimalTypeSuffix := d | D
     * <br/>
     * FloatTypeSuffix :=  f | F
     * </code>
     *
     * @return The decimal floating point literal.
     */
    private STToken processDecimalFloatLiteral() {
        int nextChar = peek();

        // For float literals start with a DOT, this condition will always be false,
        // as the reader is already advanced for the DOT before coming here.
        if (nextChar == LexerTerminals.DOT) {
            reader.advance();
            nextChar = peek();

            if (!isDigit(nextChar)) {
                // Make sure there is at least one digit after the dot
                // e.g. 2., 2.e12
                reportLexerError(DiagnosticErrorCode.ERROR_MISSING_DIGIT_AFTER_DOT);
            }
        }

        while (isDigit(nextChar)) {
            reader.advance();
            nextChar = peek();
        }

        return switch (nextChar) {
            case 'e', 'E' -> processExponent(false);
            case 'f', 'F', 'd', 'D' -> parseFloatingPointTypeSuffix();
            default -> getLiteral(SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN);
        };
    }

    /**
     * <p>
     * Process an exponent or hex-exponent.
     * </p>
     * <code>
     * exponent := Exponent | HexExponent
     * <br/>
     * Exponent := ExponentIndicator [Sign] Digit+
     * <br/>
     * HexExponent := HexExponentIndicator [Sign] Digit+
     * <br/>
     * ExponentIndicator := e | E
     * <br/>
     * HexExponentIndicator := p | P
     * <br/>
     * Sign := + | -
     * <br/>
     * Digit := 0 .. 9
     * </code>
     *
     * @param isHex HexExponent or not
     * @return The decimal floating point literal.
     */
    private STToken processExponent(boolean isHex) {
        // Advance reader as exponent indicator is already validated
        reader.advance();
        int nextChar = peek();

        // Capture if there is a sign
        if (nextChar == LexerTerminals.PLUS || nextChar == LexerTerminals.MINUS) {
            reader.advance();
            nextChar = peek();
        }

        // Make sure at least one digit is present after the indicator
        if (!isDigit(nextChar)) {
            reportLexerError(DiagnosticErrorCode.ERROR_MISSING_DIGIT_AFTER_EXPONENT_INDICATOR);
        }

        while (isDigit(nextChar)) {
            reader.advance();
            nextChar = peek();
        }

        if (isHex) {
            return getLiteral(SyntaxKind.HEX_FLOATING_POINT_LITERAL_TOKEN);
        }

        return switch (nextChar) {
            case 'f', 'F', 'd', 'D' -> parseFloatingPointTypeSuffix();
            default -> getLiteral(SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN);
        };

    }

    /**
     * <p>
     * Parse floating point type suffix.
     * </p>
     * <code>
     * FloatingPointTypeSuffix := DecimalTypeSuffix | FloatTypeSuffix
     * <br/>
     * DecimalTypeSuffix := d | D
     * <br/>
     * FloatTypeSuffix :=  f | F
     * </code>
     *
     * @return The decimal floating point literal.
     */
    private STToken parseFloatingPointTypeSuffix() {
        reader.advance();
        return getLiteral(SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN);
    }

    /**
     * <p>
     * Process and returns a hex literal.
     * </p>
     * <code>
     * hex-literal := HexIntLiteral | HexFloatingPointLiteral
     * <br/>
     * HexIntLiteral := HexIndicator HexNumber
     * <br/>
     * HexNumber := HexDigit+
     * <br/>
     * HexIndicator := 0x | 0X
     * <br/>
     * HexDigit := Digit | a .. f | A .. F
     * <br/>
     * HexFloatingPointLiteral := HexIndicator HexFloatingPointNumber
     * <br/>
     * HexFloatingPointNumber := HexNumber HexExponent | DottedHexNumber [HexExponent]
     * <br/>
     * DottedHexNumber := HexDigit+ . HexDigit+ | . HexDigit+
     * </code>
     *
     * @return The hex literal.
     */
    private STToken processHexLiteral() {
        reader.advance(); // advance for "x" or "X"
        boolean containsHexDigit = false;
        
        while (isHexDigit(peek())) {
            reader.advance();
            containsHexDigit = true;
        }

        int nextChar = peek();
        switch (nextChar) {
            case LexerTerminals.DOT:
                if (isHexIntFollowedIdentifier()) {
                    // e.g. 0x.max(), 0xA2.max()
                    return getHexIntegerLiteral();
                }

                reader.advance();
                if (!isHexDigit(reader.peek())) {
                    // Make sure there is at least one hex-digit after the dot
                    // e.g. 0x., 0xAB.
                    reportLexerError(DiagnosticErrorCode.ERROR_MISSING_HEX_DIGIT_AFTER_DOT);
                }

                nextChar = peek();
                while (isHexDigit(nextChar)) {
                    reader.advance();
                    nextChar = peek();
                }

                switch (nextChar) {
                    case 'p':
                    case 'P':
                        return processExponent(true);
                }
                break;
            case 'p':
            case 'P':
                if (!containsHexDigit) {
                    reportLexerError(DiagnosticErrorCode.ERROR_MISSING_HEX_NUMBER_AFTER_HEX_INDICATOR);
                }
                return processExponent(true);
            default:
                return getHexIntegerLiteral();
        }

        return getLiteral(SyntaxKind.HEX_FLOATING_POINT_LITERAL_TOKEN);
    }

    private STToken getHexIntegerLiteral() {
        String lexeme = getLexeme();
        if ("0x".equals(lexeme) || "0X".equals(lexeme)) {
            reportLexerError(DiagnosticErrorCode.ERROR_MISSING_HEX_NUMBER_AFTER_HEX_INDICATOR);
        }

        return getLiteral(SyntaxKind.HEX_INTEGER_LITERAL_TOKEN);
    }

    /**
     * Checks whether DecimalNumber is followed by an identifier in order to special case tokenization.
     *
     * @return <code>true</code> if DecimalNumber is followed by an identifier. <code>false</code> otherwise.
     */
    private boolean isDecimalNumberFollowedIdentifier() {
        int lookahead = 1;
        char lookaheadChar = reader.peek(lookahead);

        if (isDigit(lookaheadChar)) {
            return false;
        }

        switch (lookaheadChar) {
            case 'e':
            case 'E':
                lookahead++;

                lookaheadChar = reader.peek(lookahead);
                if (lookaheadChar == LexerTerminals.PLUS || lookaheadChar == LexerTerminals.MINUS) {
                    return false;
                }

                lookaheadChar = reader.peek(lookahead);
                while (isDigit(lookaheadChar)) {
                    lookahead++;
                    lookaheadChar = reader.peek(lookahead);
                }

                if (lookaheadChar == 'd' || lookaheadChar == 'D' || lookaheadChar == 'f' || lookaheadChar == 'F') {
                    lookahead++;
                }

                break;
            case 'd':
            case 'D':
            case 'f':
            case 'F':
                lookahead++;
                break;
            default:
                break;
        }

        lookaheadChar = reader.peek(lookahead);
        return isIdentifierFollowingChar(lookaheadChar);
    }

    /**
     * Checks whether HexIntLiteral is followed by an identifier in order to special case tokenization.
     *
     * @return <code>true</code> if HexIntLiteral is followed by an identifier. <code>false</code> otherwise.
     */
    private boolean isHexIntFollowedIdentifier() {
        int lookahead = 1;
        char lookaheadChar = reader.peek(lookahead);

        if (isDigit(lookaheadChar)) {
            return false;
        }

        while (isHexDigit(lookaheadChar)) {
            lookahead++;
            lookaheadChar = reader.peek(lookahead);
        }

        switch (lookaheadChar) {
            case 'p':
            case 'P':
                lookahead++;

                lookaheadChar = reader.peek(lookahead);
                if (lookaheadChar == LexerTerminals.PLUS || lookaheadChar == LexerTerminals.MINUS) {
                    return false;
                }

                lookaheadChar = reader.peek(lookahead);
                while (isDigit(lookaheadChar)) {
                    lookahead++;
                    lookaheadChar = reader.peek(lookahead);
                }
        }

        return isIdentifierFollowingChar(lookaheadChar);
    }

    /**
     * Process and returns an identifier or a keyword.
     *
     * @return An identifier or a keyword.
     */
    private STToken processIdentifierOrKeyword() {
        processUnquotedIdentifier();
        String tokenText = getLexeme();
        return switch (tokenText) {
            // built-in named-types
            case LexerTerminals.INT -> getSyntaxToken(SyntaxKind.INT_KEYWORD);
            case LexerTerminals.FLOAT -> getSyntaxToken(SyntaxKind.FLOAT_KEYWORD);
            case LexerTerminals.STRING -> getSyntaxToken(SyntaxKind.STRING_KEYWORD);
            case LexerTerminals.BOOLEAN -> getSyntaxToken(SyntaxKind.BOOLEAN_KEYWORD);
            case LexerTerminals.DECIMAL -> getSyntaxToken(SyntaxKind.DECIMAL_KEYWORD);
            case LexerTerminals.XML -> getSyntaxToken(SyntaxKind.XML_KEYWORD);
            case LexerTerminals.JSON -> getSyntaxToken(SyntaxKind.JSON_KEYWORD);
            case LexerTerminals.HANDLE -> getSyntaxToken(SyntaxKind.HANDLE_KEYWORD);
            case LexerTerminals.ANY -> getSyntaxToken(SyntaxKind.ANY_KEYWORD);
            case LexerTerminals.ANYDATA -> getSyntaxToken(SyntaxKind.ANYDATA_KEYWORD);
            case LexerTerminals.NEVER -> getSyntaxToken(SyntaxKind.NEVER_KEYWORD);
            case LexerTerminals.BYTE -> getSyntaxToken(SyntaxKind.BYTE_KEYWORD);

            // Keywords
            case LexerTerminals.PUBLIC -> getSyntaxToken(SyntaxKind.PUBLIC_KEYWORD);
            case LexerTerminals.PRIVATE -> getSyntaxToken(SyntaxKind.PRIVATE_KEYWORD);
            case LexerTerminals.FUNCTION -> getSyntaxToken(SyntaxKind.FUNCTION_KEYWORD);
            case LexerTerminals.RETURN -> getSyntaxToken(SyntaxKind.RETURN_KEYWORD);
            case LexerTerminals.RETURNS -> getSyntaxToken(SyntaxKind.RETURNS_KEYWORD);
            case LexerTerminals.EXTERNAL -> getSyntaxToken(SyntaxKind.EXTERNAL_KEYWORD);
            case LexerTerminals.TYPE -> getSyntaxToken(SyntaxKind.TYPE_KEYWORD);
            case LexerTerminals.RECORD -> getSyntaxToken(SyntaxKind.RECORD_KEYWORD);
            case LexerTerminals.OBJECT -> getSyntaxToken(SyntaxKind.OBJECT_KEYWORD);
            case LexerTerminals.REMOTE -> getSyntaxToken(SyntaxKind.REMOTE_KEYWORD);
            case LexerTerminals.ABSTRACT -> getSyntaxToken(SyntaxKind.ABSTRACT_KEYWORD);
            case LexerTerminals.CLIENT -> getSyntaxToken(SyntaxKind.CLIENT_KEYWORD);
            case LexerTerminals.IF -> getSyntaxToken(SyntaxKind.IF_KEYWORD);
            case LexerTerminals.ELSE -> getSyntaxToken(SyntaxKind.ELSE_KEYWORD);
            case LexerTerminals.WHILE -> getSyntaxToken(SyntaxKind.WHILE_KEYWORD);
            case LexerTerminals.TRUE -> getSyntaxToken(SyntaxKind.TRUE_KEYWORD);
            case LexerTerminals.FALSE -> getSyntaxToken(SyntaxKind.FALSE_KEYWORD);
            case LexerTerminals.CHECK -> getSyntaxToken(SyntaxKind.CHECK_KEYWORD);
            case LexerTerminals.FAIL -> getSyntaxToken(SyntaxKind.FAIL_KEYWORD);
            case LexerTerminals.CHECKPANIC -> getSyntaxToken(SyntaxKind.CHECKPANIC_KEYWORD);
            case LexerTerminals.CONTINUE -> getSyntaxToken(SyntaxKind.CONTINUE_KEYWORD);
            case LexerTerminals.BREAK -> getSyntaxToken(SyntaxKind.BREAK_KEYWORD);
            case LexerTerminals.PANIC -> getSyntaxToken(SyntaxKind.PANIC_KEYWORD);
            case LexerTerminals.IMPORT -> getSyntaxToken(SyntaxKind.IMPORT_KEYWORD);
            case LexerTerminals.AS -> getSyntaxToken(SyntaxKind.AS_KEYWORD);
            case LexerTerminals.SERVICE -> getSyntaxToken(SyntaxKind.SERVICE_KEYWORD);
            case LexerTerminals.ON -> getSyntaxToken(SyntaxKind.ON_KEYWORD);
            case LexerTerminals.RESOURCE -> getSyntaxToken(SyntaxKind.RESOURCE_KEYWORD);
            case LexerTerminals.LISTENER -> getSyntaxToken(SyntaxKind.LISTENER_KEYWORD);
            case LexerTerminals.CONST -> getSyntaxToken(SyntaxKind.CONST_KEYWORD);
            case LexerTerminals.FINAL -> getSyntaxToken(SyntaxKind.FINAL_KEYWORD);
            case LexerTerminals.TYPEOF -> getSyntaxToken(SyntaxKind.TYPEOF_KEYWORD);
            case LexerTerminals.IS -> getSyntaxToken(SyntaxKind.IS_KEYWORD);
            case LexerTerminals.NULL -> getSyntaxToken(SyntaxKind.NULL_KEYWORD);
            case LexerTerminals.LOCK -> getSyntaxToken(SyntaxKind.LOCK_KEYWORD);
            case LexerTerminals.ANNOTATION -> getSyntaxToken(SyntaxKind.ANNOTATION_KEYWORD);
            case LexerTerminals.SOURCE -> getSyntaxToken(SyntaxKind.SOURCE_KEYWORD);
            case LexerTerminals.VAR -> getSyntaxToken(SyntaxKind.VAR_KEYWORD);
            case LexerTerminals.WORKER -> getSyntaxToken(SyntaxKind.WORKER_KEYWORD);
            case LexerTerminals.PARAMETER -> getSyntaxToken(SyntaxKind.PARAMETER_KEYWORD);
            case LexerTerminals.FIELD -> getSyntaxToken(SyntaxKind.FIELD_KEYWORD);
            case LexerTerminals.ISOLATED -> getSyntaxToken(SyntaxKind.ISOLATED_KEYWORD);
            case LexerTerminals.XMLNS -> getSyntaxToken(SyntaxKind.XMLNS_KEYWORD);
            case LexerTerminals.FORK -> getSyntaxToken(SyntaxKind.FORK_KEYWORD);
            case LexerTerminals.MAP -> getSyntaxToken(SyntaxKind.MAP_KEYWORD);
            case LexerTerminals.FUTURE -> getSyntaxToken(SyntaxKind.FUTURE_KEYWORD);
            case LexerTerminals.TYPEDESC -> getSyntaxToken(SyntaxKind.TYPEDESC_KEYWORD);
            case LexerTerminals.TRAP -> getSyntaxToken(SyntaxKind.TRAP_KEYWORD);
            case LexerTerminals.IN -> getSyntaxToken(SyntaxKind.IN_KEYWORD);
            case LexerTerminals.FOREACH -> getSyntaxToken(SyntaxKind.FOREACH_KEYWORD);
            case LexerTerminals.TABLE -> getSyntaxToken(SyntaxKind.TABLE_KEYWORD);
            case LexerTerminals.ERROR -> getSyntaxToken(SyntaxKind.ERROR_KEYWORD);
            case LexerTerminals.LET -> getSyntaxToken(SyntaxKind.LET_KEYWORD);
            case LexerTerminals.STREAM -> getSyntaxToken(SyntaxKind.STREAM_KEYWORD);
            case LexerTerminals.NEW -> getSyntaxToken(SyntaxKind.NEW_KEYWORD);
            case LexerTerminals.READONLY -> getSyntaxToken(SyntaxKind.READONLY_KEYWORD);
            case LexerTerminals.DISTINCT -> getSyntaxToken(SyntaxKind.DISTINCT_KEYWORD);
            case LexerTerminals.FROM -> getSyntaxToken(SyntaxKind.FROM_KEYWORD);
            case LexerTerminals.START -> getSyntaxToken(SyntaxKind.START_KEYWORD);
            case LexerTerminals.FLUSH -> getSyntaxToken(SyntaxKind.FLUSH_KEYWORD);
            case LexerTerminals.WAIT -> getSyntaxToken(SyntaxKind.WAIT_KEYWORD);
            case LexerTerminals.DO -> getSyntaxToken(SyntaxKind.DO_KEYWORD);
            case LexerTerminals.TRANSACTION -> getSyntaxToken(SyntaxKind.TRANSACTION_KEYWORD);
            case LexerTerminals.COMMIT -> getSyntaxToken(SyntaxKind.COMMIT_KEYWORD);
            case LexerTerminals.RETRY -> getSyntaxToken(SyntaxKind.RETRY_KEYWORD);
            case LexerTerminals.ROLLBACK -> getSyntaxToken(SyntaxKind.ROLLBACK_KEYWORD);
            case LexerTerminals.TRANSACTIONAL -> getSyntaxToken(SyntaxKind.TRANSACTIONAL_KEYWORD);
            case LexerTerminals.ENUM -> getSyntaxToken(SyntaxKind.ENUM_KEYWORD);
            case LexerTerminals.BASE16 -> getSyntaxToken(SyntaxKind.BASE16_KEYWORD);
            case LexerTerminals.BASE64 -> getSyntaxToken(SyntaxKind.BASE64_KEYWORD);
            case LexerTerminals.MATCH -> getSyntaxToken(SyntaxKind.MATCH_KEYWORD);
            case LexerTerminals.CONFLICT -> getSyntaxToken(SyntaxKind.CONFLICT_KEYWORD);
            case LexerTerminals.CLASS -> getSyntaxToken(SyntaxKind.CLASS_KEYWORD);
            case LexerTerminals.CONFIGURABLE -> getSyntaxToken(SyntaxKind.CONFIGURABLE_KEYWORD);
            case LexerTerminals.WHERE -> getSyntaxToken(SyntaxKind.WHERE_KEYWORD);
            case LexerTerminals.SELECT -> getSyntaxToken(SyntaxKind.SELECT_KEYWORD);
            case LexerTerminals.LIMIT -> getSyntaxToken(SyntaxKind.LIMIT_KEYWORD);
            case LexerTerminals.OUTER -> getSyntaxToken(SyntaxKind.OUTER_KEYWORD);
            case LexerTerminals.EQUALS -> getSyntaxToken(SyntaxKind.EQUALS_KEYWORD);
            case LexerTerminals.ORDER -> getSyntaxToken(SyntaxKind.ORDER_KEYWORD);
            case LexerTerminals.BY -> getSyntaxToken(SyntaxKind.BY_KEYWORD);
            case LexerTerminals.ASCENDING -> getSyntaxToken(SyntaxKind.ASCENDING_KEYWORD);
            case LexerTerminals.DESCENDING -> getSyntaxToken(SyntaxKind.DESCENDING_KEYWORD);
            case LexerTerminals.JOIN -> getSyntaxToken(SyntaxKind.JOIN_KEYWORD);
            case LexerTerminals.NATURAL -> getSyntaxToken(SyntaxKind.NATURAL_KEYWORD);
            case LexerTerminals.RE -> {
                if (getNextNonWSOrNonCommentChar() == LexerTerminals.BACKTICK) {
                    yield getSyntaxToken(SyntaxKind.RE_KEYWORD);
                }
                yield getIdentifierToken();
            }
            default ->
//                if (this.keywordModes.contains(KeywordMode.QUERY)) {
//                    return getQueryCtxKeywordOrIdentifier(tokenText);
//                }
                    getIdentifierToken();
        };
    }

    private int getNextNonWSOrNonCommentChar() {
        int lookaheadCount = 0;
        char nextChar = reader.peek(lookaheadCount);
        while (nextChar != Character.MAX_VALUE) {
            switch (nextChar) {
                case LexerTerminals.SPACE:
                case LexerTerminals.TAB:
                case LexerTerminals.FORM_FEED:
                case LexerTerminals.CARRIAGE_RETURN:
                case LexerTerminals.NEWLINE:
                    lookaheadCount++;
                    break;
                case LexerTerminals.SLASH:
                    if (reader.peek(lookaheadCount + 1) == LexerTerminals.SLASH) {
                        lookaheadCount += 2;
                        lookaheadCount = skipComment(lookaheadCount);
                        break;
                    }
                    return nextChar;
                default:
                    return nextChar;
            }
            nextChar = reader.peek(lookaheadCount);
        }
        return nextChar;
    }

    private int skipComment(int lookaheadCount) {
        int nextChar = reader.peek(lookaheadCount);
        while (nextChar != Character.MAX_VALUE) {
            switch (nextChar) {
                case LexerTerminals.NEWLINE:
                case LexerTerminals.CARRIAGE_RETURN:
                    break;
                default:
                    lookaheadCount++;
                    nextChar = reader.peek(lookaheadCount);
                    continue;
            }
            break;
        }
        return lookaheadCount;
    }

//    private STToken getQueryCtxKeywordOrIdentifier(String tokenText) {
//        switch (tokenText) {
//            case LexerTerminals.WHERE:
//                return getSyntaxToken(SyntaxKind.WHERE_KEYWORD);
//            case LexerTerminals.SELECT:
//                return getSyntaxToken(SyntaxKind.SELECT_KEYWORD);
//            case LexerTerminals.LIMIT:
//                return getSyntaxToken(SyntaxKind.LIMIT_KEYWORD);
//            case LexerTerminals.OUTER:
//                return getSyntaxToken(SyntaxKind.OUTER_KEYWORD);
//            case LexerTerminals.EQUALS:
//                return getSyntaxToken(SyntaxKind.EQUALS_KEYWORD);
//            case LexerTerminals.ORDER:
//                return getSyntaxToken(SyntaxKind.ORDER_KEYWORD);
//            case LexerTerminals.BY:
//                return getSyntaxToken(SyntaxKind.BY_KEYWORD);
//            case LexerTerminals.ASCENDING:
//                return getSyntaxToken(SyntaxKind.ASCENDING_KEYWORD);
//            case LexerTerminals.DESCENDING:
//                return getSyntaxToken(SyntaxKind.DESCENDING_KEYWORD);
//            case LexerTerminals.JOIN:
//                return getSyntaxToken(SyntaxKind.JOIN_KEYWORD);
//            default:
//                return getIdentifierToken();
//        }
//    }

    /**
     * Process and returns an invalid token. Consumes the input until {@link #isEndOfInvalidToken()}
     * is reached.
     */
    private STToken processInvalidToken() {
        while (!isEndOfInvalidToken()) {
            reader.advance();
        }

        String tokenText = getLexeme();
        STToken invalidToken = STNodeFactory.createInvalidToken(tokenText);
        STNode invalidNodeMinutiae = STNodeFactory.createInvalidNodeMinutiae(invalidToken);
        this.leadingTriviaList.add(invalidNodeMinutiae);
        return invalidToken;
    }

    /**
     * Check whether the current index is pointing to an end of an invalid lexer-token.
     * An invalid token is considered to end if one of the below is reached:
     * <ul>
     * <li>a whitespace</li>
     * <li>semicolon</li>
     * <li>newline</li>
     * </ul>
     *
     * @return <code>true</code>, if the end of an invalid token is reached, <code>false</code> otherwise
     */
    private boolean isEndOfInvalidToken() {
        if (reader.isEOF()) {
            return true;
        }

        int currentChar = peek();
        return switch (currentChar) {
            case LexerTerminals.NEWLINE,
                 LexerTerminals.CARRIAGE_RETURN,
                 LexerTerminals.SPACE,
                 LexerTerminals.TAB,
                 // Separators
                 LexerTerminals.SEMICOLON,
                 LexerTerminals.COLON,
                 LexerTerminals.DOT,
                 LexerTerminals.COMMA,
                 LexerTerminals.OPEN_PARANTHESIS,
                 LexerTerminals.CLOSE_PARANTHESIS,
                 LexerTerminals.OPEN_BRACE,
                 LexerTerminals.CLOSE_BRACE,
                 LexerTerminals.OPEN_BRACKET,
                 LexerTerminals.CLOSE_BRACKET,
                 LexerTerminals.PIPE,
                 LexerTerminals.QUESTION_MARK,
                 LexerTerminals.DOUBLE_QUOTE,
                 LexerTerminals.SINGLE_QUOTE,
                 LexerTerminals.HASH,
                 LexerTerminals.AT,
                 LexerTerminals.BACKTICK,
                 LexerTerminals.DOLLAR,
                 // Arithmetic operators
                 LexerTerminals.EQUAL,
                 LexerTerminals.PLUS,
                 LexerTerminals.MINUS,
                 LexerTerminals.ASTERISK,
                 LexerTerminals.SLASH,
                 LexerTerminals.PERCENT,
                 LexerTerminals.GT,
                 LexerTerminals.LT,
                 LexerTerminals.BACKSLASH,
                 LexerTerminals.EXCLAMATION_MARK,
                 LexerTerminals.BITWISE_AND,
                 LexerTerminals.BITWISE_XOR,
                 LexerTerminals.NEGATION -> true;
            default -> isIdentifierFollowingChar(currentChar);
        };
    }

    /**
     * <p>
     * Check whether current input index points to a start of a hex-numeric literal.
     * </p>
     * <code>HexIndicator := 0x | 0X</code>
     *
     * @param startChar Starting character of the literal
     * @param nextChar Second character of the literal
     * @return <code>true</code>, if the current input points to a start of a hex-numeric literal.
     *         <code>false</code> otherwise.
     */
    private boolean isHexIndicator(int startChar, int nextChar) {
        return startChar == '0' && (nextChar == 'x' || nextChar == 'X');
    }

    /**
     * Returns the next character from the reader, without consuming the stream.
     *
     * @return Next character
     */
    private int peek() {
        return this.reader.peek();
    }

    /**
     * Get the text associated with the current token.
     *
     * @return Text associated with the current token.
     */
    private String getLexeme() {
        return reader.getMarkedChars();
    }

    /**
     * Process and return double-quoted string literal.
     * <p>
     * <code>string-literal := DoubleQuotedStringLiteral
     * <br/>
     * DoubleQuotedStringLiteral := " (StringChar | StringEscape)* "
     * <br/>
     * StringChar := ^ ( 0xA | 0xD | \ | " )
     * <br/>
     * StringEscape := StringSingleEscape | NumericEscape
     * <br/>
     * StringSingleEscape := \t | \n | \r | \\ | \"
     * <br/>
     * NumericEscape := \ u{ CodePoint }
     * <br/>
     * CodePoint := HexDigit+
     * </code>
     *
     * @return String literal token
     */
    private STToken processStringLiteral() {
        int nextChar;
        while (!reader.isEOF()) {
            nextChar = peek();
            switch (nextChar) {
                case LexerTerminals.NEWLINE:
                case LexerTerminals.CARRIAGE_RETURN:
                    reportLexerError(DiagnosticErrorCode.ERROR_MISSING_DOUBLE_QUOTE);
                    break;
                case LexerTerminals.DOUBLE_QUOTE:
                    this.reader.advance();
                    break;
                case LexerTerminals.BACKSLASH:
                    switch (this.reader.peek(1)) {
                        case 'n':
                        case 't':
                        case 'r':
                        case LexerTerminals.BACKSLASH:
                        case LexerTerminals.DOUBLE_QUOTE:
                            this.reader.advance(2);
                            continue;
                        case 'u':
                            if (this.reader.peek(2) == LexerTerminals.OPEN_BRACE) {
                                processNumericEscape();
                            } else {
                                reportLexerError(DiagnosticErrorCode.ERROR_INVALID_STRING_NUMERIC_ESCAPE_SEQUENCE);
                                this.reader.advance(2);
                            }
                            continue;
                        default:
                            reportInvalidEscapeSequence(this.reader.peek(1));
                            this.reader.advance();
                            continue;
                    }
                default:
                    this.reader.advance();
                    continue;
            }
            break;
        }

        return getLiteral(SyntaxKind.STRING_LITERAL_TOKEN);
    }

    /**
     * Process any token that starts with '!'.
     *
     * @return One of the tokens: <code>'!', '!=', '!=='</code>
     */
    private STToken processExclamationMarkOperator() {
        switch (peek()) { // check for the second char
            case LexerTerminals.EQUAL:
                reader.advance();
                if (peek() == LexerTerminals.EQUAL) {
                    // this is '!=='
                    reader.advance();
                    return getSyntaxToken(SyntaxKind.NOT_DOUBLE_EQUAL_TOKEN);
                } else {
                    // this is '!='
                    return getSyntaxToken(SyntaxKind.NOT_EQUAL_TOKEN);
                }
            default:
                // this is '!is'
                if (isNotIsToken()) {
                    reader.advance(2);
                    return getSyntaxToken(SyntaxKind.NOT_IS_KEYWORD);
                 }
                // this is '!'
                return getSyntaxToken(SyntaxKind.EXCLAMATION_MARK_TOKEN);
        }
    }

    private boolean isNotIsToken() {
        return (reader.peek() == 'i' && reader.peek(1) == 's') &&
                !(isIdentifierFollowingChar(reader.peek(2)) || reader.peek(2) == LexerTerminals.BACKSLASH);
    }

    /**
     * Process any token that starts with '|'.
     *
     * @return One of the tokens: <code>'|', '|}', '||'</code>
     */
    private STToken processPipeOperator() {
        return switch (peek()) { // check for the second char
            case LexerTerminals.CLOSE_BRACE -> {
                reader.advance();
                yield getSyntaxToken(SyntaxKind.CLOSE_BRACE_PIPE_TOKEN);
            }
            case LexerTerminals.PIPE -> {
                reader.advance();
                yield getSyntaxToken(SyntaxKind.LOGICAL_OR_TOKEN);
            }
            default -> getSyntaxToken(SyntaxKind.PIPE_TOKEN);
        };
    }

    /**
     * Process any token that starts with '/'.
     *
     * @return One of the tokens: <code>'/', '/*', '/**\/<' </code>
     */
    private STToken processSlashToken() {
        // check for the second char
        if (peek() != LexerTerminals.ASTERISK) {
            return getSyntaxToken(SyntaxKind.SLASH_TOKEN);
        }

        reader.advance();
        if (peek() != LexerTerminals.ASTERISK) {
            return getSyntaxToken(SyntaxKind.SLASH_ASTERISK_TOKEN);
        } else if (reader.peek(1) == LexerTerminals.SLASH && reader.peek(2) == LexerTerminals.LT) {
            reader.advance(3);
            return getSyntaxToken(SyntaxKind.DOUBLE_SLASH_DOUBLE_ASTERISK_LT_TOKEN);
        } else {
            return getSyntaxToken(SyntaxKind.SLASH_ASTERISK_TOKEN);
        }
    }

    /**
     * Process and return documentation string.
     * <p>
     * <code>
     * DocumentationContentString := ( BlankSpace* # [DocumentationContent] )+
     * <br/>
     * DocumentationContent := (^ 0xA)* 0xA
     * <br/>
     * BlankSpace := Tab | Space
     * <br/>
     * Space := 0x20
     * <br/>
     * Tab := 0x9
     * </code>
     *
     * @return Documentation string token
     */
    private STToken processDocumentationString() {
        int nextChar = peek();
        while (!reader.isEOF()) {
            switch (nextChar) {
                case LexerTerminals.CARRIAGE_RETURN:
                case LexerTerminals.NEWLINE:

                    // Advance reader for the new line
                    if (peek() == LexerTerminals.CARRIAGE_RETURN && reader.peek(1) == LexerTerminals.NEWLINE) {
                        reader.advance();
                    }
                    reader.advance();

                    // Look ahead and see if next line also belongs to the documentation.
                    // i.e. look for a `WS #` match
                    // If there's a match, advance reader for the next line as well.
                    // Otherwise terminate documentation content after the new line.
                    int lookAheadCount = 0;
                    int lookAheadChar = reader.peek(lookAheadCount);
                    while (lookAheadChar == LexerTerminals.SPACE || lookAheadChar == LexerTerminals.TAB) {
                        lookAheadCount++;
                        lookAheadChar = reader.peek(lookAheadCount);
                    }

                    if (lookAheadChar != LexerTerminals.HASH) {
                        // Next line does not belong to documentation, hence break
                        break;
                    }

                    reader.advance(lookAheadCount);
                    nextChar = peek();
                    continue;
                default:
                    reader.advance();
                    nextChar = peek();
                    continue;
            }
            break;
        }

        STNode leadingTrivia = getLeadingTrivia();
        String lexeme = getLexeme();
        STNode trailingTrivia = STNodeFactory.createNodeList(new ArrayList<>(0)); // No trailing trivia
        return STNodeFactory.createLiteralValueToken(SyntaxKind.DOCUMENTATION_STRING, lexeme, leadingTrivia,
                trailingTrivia);
    }

    private STToken getBacktickToken() {
        STNode leadingTrivia = getLeadingTrivia();
        // Trivia after the back-tick including whitespace belongs to the content of the back-tick.
        // Therefore, do not process trailing trivia for starting back-tick. We reach here only for
        // starting back-tick. Ending back-tick is processed by the template mode.
        STNode trailingTrivia = STNodeFactory.createEmptyNodeList();
        return STNodeFactory.createToken(SyntaxKind.BACKTICK_TOKEN, leadingTrivia, trailingTrivia);
    }

    private STToken readTemplateToken() {
        reader.mark();
        if (reader.isEOF()) {
            return getSyntaxToken(SyntaxKind.EOF_TOKEN);
        }

        char nextChar = this.reader.peek();
        if (nextChar == LexerTerminals.BACKTICK) {
            reader.advance();
            endMode();
            return getSyntaxToken(SyntaxKind.BACKTICK_TOKEN);
        }

        if (nextChar == LexerTerminals.DOLLAR && reader.peek(1) == LexerTerminals.OPEN_BRACE) {
                // Switch to interpolation mode. Then the next token will be read in that mode.
                startMode(ParserMode.INTERPOLATION);
                reader.advance(2);

                return getSyntaxToken(SyntaxKind.INTERPOLATION_START_TOKEN);
        }

        while (!reader.isEOF()) {
            reader.advance();
            nextChar = this.reader.peek();
            if (nextChar == LexerTerminals.BACKTICK ||
                    (nextChar == LexerTerminals.DOLLAR && reader.peek(1) == LexerTerminals.OPEN_BRACE)) {
                break;
            }
        }

        return getLiteral(SyntaxKind.TEMPLATE_STRING);
    }

    private STToken readPromptToken() {
        reader.mark();
        if (reader.isEOF()) {
            return getSyntaxToken(SyntaxKind.EOF_TOKEN);
        }

        char nextChar = this.reader.peek();
        if (nextChar == LexerTerminals.CLOSE_BRACE) {
            reader.advance();
            endMode();
            return getSyntaxToken(SyntaxKind.CLOSE_BRACE_TOKEN);
        }

        if (nextChar == LexerTerminals.DOLLAR && reader.peek(1) == LexerTerminals.OPEN_BRACE) {
            // Switch to interpolation mode. Then the next token will be read in that mode.
            startMode(ParserMode.INTERPOLATION);
            reader.advance(2);

            return getSyntaxToken(SyntaxKind.INTERPOLATION_START_TOKEN);
        }

        while (!reader.isEOF()) {
            reader.advance();
            nextChar = this.reader.peek();
            char nextNextChar = this.reader.peek(1);
            if (nextChar == LexerTerminals.CLOSE_BRACE ||
                    (nextChar == LexerTerminals.DOLLAR && nextNextChar == LexerTerminals.OPEN_BRACE)) {
                break;
            }

            if (nextChar == LexerTerminals.BACKSLASH) {
                if (nextNextChar != LexerTerminals.CLOSE_BRACE && nextNextChar != LexerTerminals.BACKSLASH) {
                    reportInvalidEscapeSequence(this.reader.peek(1));
                }
                reader.advance();
            }
        }

        return getLiteral(SyntaxKind.PROMPT_CONTENT);
    }

    /**
     * Read a regular expression template token.
     * Continues reading as a single token until it finds an interpolation or a backtick.
     * Interpolations are treated as a single literal if they are nested within a character class.
     *
     * @return The regexp literal.
     */
    private STToken readRegExpTemplateToken() {
        boolean shouldProcessInterpolations = true;
        reader.mark();
        if (reader.isEOF()) {
            return getSyntaxToken(SyntaxKind.EOF_TOKEN);
        }

        char nextChar = this.reader.peek();
        switch (nextChar) {
            case LexerTerminals.BACKTICK:
                reader.advance();
                endMode();
                return getSyntaxToken(SyntaxKind.BACKTICK_TOKEN);
            case LexerTerminals.DOLLAR:
                if (reader.peek(1) == LexerTerminals.OPEN_BRACE) {
                    // Switch to interpolation mode. Then the next token will be read in that mode.
                    startMode(ParserMode.INTERPOLATION);
                    reader.advance(2);

                    return getSyntaxToken(SyntaxKind.INTERPOLATION_START_TOKEN);
                }
                // fall through
            default:
                if (nextChar == LexerTerminals.OPEN_BRACKET) {
                    shouldProcessInterpolations = false;
                }
                while (!reader.isEOF()) {
                    if (shouldProcessInterpolations && this.reader.peek() == LexerTerminals.BACKSLASH 
                            && this.reader.peek(1) == LexerTerminals.OPEN_BRACKET) {
                        // Escaped open brackets are not considered as the start of a no interpolation context.
                        reader.advance();
                    }
                    reader.advance();
                    nextChar = this.reader.peek();
                    switch (nextChar) {
                        case LexerTerminals.DOLLAR:
                            if (shouldProcessInterpolations && this.reader.peek(1) == LexerTerminals.OPEN_BRACE) {
                                break;
                            }
                            continue;
                        case LexerTerminals.BACKTICK:
                            break;
                        case LexerTerminals.OPEN_BRACKET:
                            shouldProcessInterpolations = false;
                            continue;
                        case LexerTerminals.CLOSE_BRACKET:
                            shouldProcessInterpolations = true;
                            continue;
                        case LexerTerminals.BACKSLASH:
                            if (!shouldProcessInterpolations && this.reader.peek(1) == LexerTerminals.CLOSE_BRACKET) {
                                reader.advance();
                            }
                            continue;    
                        default:
                            continue;
                    }
                    break;
                }
        }
        return getLiteral(SyntaxKind.TEMPLATE_STRING);
    }

    /**
     * Process quoted identifier.
     * <p>
     * <code>
     * QuotedIdentifier := ' (IdentifierFollowingChar | IdentifierEscape) IdentifierEnd
     * </code>
     *
     * @return An identifier token.
     */
    private STToken processQuotedIdentifier() {
        processIdentifierEnd();
        if (String.valueOf(LexerTerminals.SINGLE_QUOTE).equals(getLexeme())) {
            reportLexerError(DiagnosticErrorCode.ERROR_INCOMPLETE_QUOTED_IDENTIFIER);
        }
        return getIdentifierToken();
    }

    /**
     * Process unquoted identifier.
     * <p>
     * <code>
     * UnquotedIdentifier := (IdentifierInitialChar | IdentifierEscape) IdentifierEnd
     * </code>
     */
    private void processUnquotedIdentifier() {
        processIdentifierEnd();
    }

    /**
     * Process identifier end.
     * <p>
     * <i>Note: Need to update the {@link DocumentationLexer} whenever changing the identifier processing.</i>
     * <p>
     * <code>
     * IdentifierEnd := IdentifierChar*
     * <br/>
     * IdentifierChar := IdentifierFollowingChar | IdentifierEscape
     * <br/>
     * IdentifierEscape := IdentifierSingleEscape | NumericEscape
     * </code>
     *
     */
    private void processIdentifierEnd() {
        while (!reader.isEOF()) {
            int nextChar = reader.peek();
            if (isIdentifierFollowingChar(nextChar)) {
                reader.advance();
                continue;
            }

            if (nextChar != LexerTerminals.BACKSLASH) {
                break;
            }

            // IdentifierSingleEscape | NumericEscape

            nextChar = reader.peek(1);
            switch (nextChar) {
                case LexerTerminals.NEWLINE:
                case LexerTerminals.CARRIAGE_RETURN:
                case LexerTerminals.TAB:
                    reader.advance();
                    reportLexerError(DiagnosticErrorCode.ERROR_INVALID_ESCAPE_SEQUENCE, "");
                    break;
                case 'u':
                    // NumericEscape
                    if (reader.peek(2) == LexerTerminals.OPEN_BRACE) {
                        processNumericEscape();
                    } else {
                        reader.advance(2);
                    }
                    continue;
                default:
                    if (!isValidQuotedIdentifierEscapeChar(nextChar)) {
                        reportInvalidEscapeSequence((char) nextChar);
                    }

                    reader.advance(2);
                    continue;
            }
            break;
        }
    }

    private void reportInvalidEscapeSequence(char nextChar) {
        String escapeSequence = String.valueOf(nextChar);
        reportLexerError(DiagnosticErrorCode.ERROR_INVALID_ESCAPE_SEQUENCE, escapeSequence);
    }

    private boolean isValidQuotedIdentifierEscapeChar(int nextChar) {
        // ASCII letters are not allowed
        if ('A' <= nextChar && nextChar <= 'Z') {
            return false;
        }

        if ('a' <= nextChar && nextChar <= 'z') {
            return false;
        }

        // Unicode pattern white space characters are not allowed
        return !isUnicodePatternWhiteSpaceChar(nextChar);
    }

    private STToken processTokenStartWithLt() {
        int nextChar = peek();
        switch (nextChar) {
            case LexerTerminals.EQUAL:
                reader.advance();
                return getSyntaxToken(SyntaxKind.LT_EQUAL_TOKEN);
            case LexerTerminals.MINUS:
                int nextNextChar = reader.peek(1);
                if (isDigit(nextNextChar)) {
                    return getSyntaxToken(SyntaxKind.LT_TOKEN);
                }
                reader.advance();
                return getSyntaxToken(SyntaxKind.LEFT_ARROW_TOKEN);
            case LexerTerminals.LT:
                reader.advance();
                return getSyntaxToken(SyntaxKind.DOUBLE_LT_TOKEN);
        }

        return getSyntaxToken(SyntaxKind.LT_TOKEN);
    }

    private STToken processTokenStartWithGt() {
        if (peek() == LexerTerminals.EQUAL) {
            reader.advance();
            return getSyntaxToken(SyntaxKind.GT_EQUAL_TOKEN);
        }

        if (reader.peek() != LexerTerminals.GT) {
            return getSyntaxToken(SyntaxKind.GT_TOKEN);
        }

        char nextChar = reader.peek(1);
        return switch (nextChar) {
            case LexerTerminals.GT -> {
                if (reader.peek(2) == LexerTerminals.EQUAL) {
                    // ">>>="
                    reader.advance(2);
                    yield getSyntaxToken(SyntaxKind.TRIPPLE_GT_TOKEN);
                }
                yield getSyntaxToken(SyntaxKind.GT_TOKEN);
            }
            case LexerTerminals.EQUAL -> {
                // ">>="
                reader.advance(1);
                yield getSyntaxToken(SyntaxKind.DOUBLE_GT_TOKEN);
            }
            default -> getSyntaxToken(SyntaxKind.GT_TOKEN);
        };
    }

    /*
     * ------------------------------------------------------------------------------------------------------------
     * INTERPOLATION Mode
     * ------------------------------------------------------------------------------------------------------------
     */

    private STToken readTokenInInterpolation() {
        reader.mark();
        int nextChar = peek();
        switch (nextChar) {
            case LexerTerminals.OPEN_BRACE:
                // Start braced-content mode. This is to keep track of the
                // open-brace and the corresponding close-brace. This way,
                // those will not be mistaken as the close-brace of the
                // interpolation end.
                startMode(ParserMode.INTERPOLATION_BRACED_CONTENT);
                return readToken();
            case LexerTerminals.CLOSE_BRACE:
                // Close-brace in the interpolation mode definitely means its
                // then end of the interpolation.
                endMode();
                reader.advance();
                return getSyntaxTokenWithoutTrailingTrivia(SyntaxKind.CLOSE_BRACE_TOKEN);
            case LexerTerminals.BACKTICK:
                // If we are inside the interpolation, that means its no longer XML
                // mode, but in the default mode. Hence treat the back-tick in the
                // same way as in the default mode.
            default:
                // Otherwise read the token from default mode.
                return readToken();
        }
    }

    private STToken getSyntaxTokenWithoutTrailingTrivia(SyntaxKind kind) {
        STNode leadingTrivia = getLeadingTrivia();
        STNode trailingTrivia = STNodeFactory.createNodeList(new ArrayList<>(0));
        return STNodeFactory.createToken(kind, leadingTrivia, trailingTrivia);
    }

    /*
     * ------------------------------------------------------------------------------------------------------------
     * INTERPOLATION_BRACED_CONTENT Mode
     * ------------------------------------------------------------------------------------------------------------
     */

    private STToken readTokenInBracedContentInInterpolation() {
        reader.mark();
        int nextChar = peek();
        switch (nextChar) {
            case LexerTerminals.OPEN_BRACE:
                startMode(ParserMode.INTERPOLATION_BRACED_CONTENT);
                break;
            case LexerTerminals.CLOSE_BRACE:
                endMode();
                break;
            case LexerTerminals.BACKTICK:
                // Recursively end backtick string related contexts
                while (this.mode != ParserMode.DEFAULT) {
                    endMode();
                }
                reader.advance();
                return getBacktickToken();
            default:
                // Otherwise read the token from default mode.
                break;
        }

        return readToken();
    }
}
