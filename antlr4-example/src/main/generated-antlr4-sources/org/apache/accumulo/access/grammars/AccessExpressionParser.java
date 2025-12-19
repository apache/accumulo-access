// Generated from AccessExpression.g4 by ANTLR 4.13.1
package org.apache.accumulo.access.grammars;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class AccessExpressionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, ACCESS_TOKEN=5, WS=6;
	public static final int
		RULE_access_expression = 0, RULE_expression = 1, RULE_and_expression = 2, 
		RULE_or_expression = 3, RULE_access_token = 4, RULE_and_operator = 5, 
		RULE_or_operator = 6;
	private static String[] makeRuleNames() {
		return new String[] {
			"access_expression", "expression", "and_expression", "or_expression", 
			"access_token", "and_operator", "or_operator"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'&'", "'|'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, "ACCESS_TOKEN", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "AccessExpression.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public AccessExpressionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Access_expressionContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(AccessExpressionParser.EOF, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Access_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_access_expression; }
	}

	public final Access_expressionContext access_expression() throws RecognitionException {
		Access_expressionContext _localctx = new Access_expressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_access_expression);
		try {
			setState(18);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
				enterOuterAlt(_localctx, 1);
				{
				setState(14);
				match(EOF);
				}
				break;
			case T__0:
			case ACCESS_TOKEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(15);
				expression();
				setState(16);
				match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public And_expressionContext and_expression() {
			return getRuleContext(And_expressionContext.class,0);
		}
		public Or_expressionContext or_expression() {
			return getRuleContext(Or_expressionContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Access_tokenContext access_token() {
			return getRuleContext(Access_tokenContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(27);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(20);
				and_expression();
				}
				break;
			case 2:
				{
				setState(21);
				or_expression();
				}
				break;
			case 3:
				{
				setState(22);
				match(T__0);
				setState(23);
				expression();
				setState(24);
				match(T__1);
				}
				break;
			case 4:
				{
				setState(26);
				access_token();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class And_expressionContext extends ParserRuleContext {
		public List<Access_tokenContext> access_token() {
			return getRuleContexts(Access_tokenContext.class);
		}
		public Access_tokenContext access_token(int i) {
			return getRuleContext(Access_tokenContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<And_operatorContext> and_operator() {
			return getRuleContexts(And_operatorContext.class);
		}
		public And_operatorContext and_operator(int i) {
			return getRuleContext(And_operatorContext.class,i);
		}
		public And_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_expression; }
	}

	public final And_expressionContext and_expression() throws RecognitionException {
		And_expressionContext _localctx = new And_expressionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_and_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(34);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ACCESS_TOKEN:
				{
				setState(29);
				access_token();
				}
				break;
			case T__0:
				{
				setState(30);
				match(T__0);
				setState(31);
				expression();
				setState(32);
				match(T__1);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(44); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(36);
				and_operator();
				setState(42);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ACCESS_TOKEN:
					{
					setState(37);
					access_token();
					}
					break;
				case T__0:
					{
					setState(38);
					match(T__0);
					setState(39);
					expression();
					setState(40);
					match(T__1);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				}
				setState(46); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__2 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Or_expressionContext extends ParserRuleContext {
		public List<Access_tokenContext> access_token() {
			return getRuleContexts(Access_tokenContext.class);
		}
		public Access_tokenContext access_token(int i) {
			return getRuleContext(Access_tokenContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<Or_operatorContext> or_operator() {
			return getRuleContexts(Or_operatorContext.class);
		}
		public Or_operatorContext or_operator(int i) {
			return getRuleContext(Or_operatorContext.class,i);
		}
		public Or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or_expression; }
	}

	public final Or_expressionContext or_expression() throws RecognitionException {
		Or_expressionContext _localctx = new Or_expressionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_or_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(53);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ACCESS_TOKEN:
				{
				setState(48);
				access_token();
				}
				break;
			case T__0:
				{
				setState(49);
				match(T__0);
				setState(50);
				expression();
				setState(51);
				match(T__1);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(63); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(55);
				or_operator();
				setState(61);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ACCESS_TOKEN:
					{
					setState(56);
					access_token();
					}
					break;
				case T__0:
					{
					setState(57);
					match(T__0);
					setState(58);
					expression();
					setState(59);
					match(T__1);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				}
				setState(65); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__3 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Access_tokenContext extends ParserRuleContext {
		public TerminalNode ACCESS_TOKEN() { return getToken(AccessExpressionParser.ACCESS_TOKEN, 0); }
		public Access_tokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_access_token; }
	}

	public final Access_tokenContext access_token() throws RecognitionException {
		Access_tokenContext _localctx = new Access_tokenContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_access_token);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(ACCESS_TOKEN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class And_operatorContext extends ParserRuleContext {
		public And_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_operator; }
	}

	public final And_operatorContext and_operator() throws RecognitionException {
		And_operatorContext _localctx = new And_operatorContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_and_operator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Or_operatorContext extends ParserRuleContext {
		public Or_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or_operator; }
	}

	public final Or_operatorContext or_operator() throws RecognitionException {
		Or_operatorContext _localctx = new Or_operatorContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_or_operator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0006J\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0003\u0000\u0013\b\u0000\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001\u001c"+
		"\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003"+
		"\u0002#\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0003\u0002+\b\u0002\u0004\u0002-\b\u0002\u000b\u0002"+
		"\f\u0002.\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u00036\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0003\u0003>\b\u0003\u0004\u0003@\b\u0003\u000b"+
		"\u0003\f\u0003A\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0000\u0000\u0007\u0000\u0002\u0004\u0006"+
		"\b\n\f\u0000\u0000L\u0000\u0012\u0001\u0000\u0000\u0000\u0002\u001b\u0001"+
		"\u0000\u0000\u0000\u0004\"\u0001\u0000\u0000\u0000\u00065\u0001\u0000"+
		"\u0000\u0000\bC\u0001\u0000\u0000\u0000\nE\u0001\u0000\u0000\u0000\fG"+
		"\u0001\u0000\u0000\u0000\u000e\u0013\u0005\u0000\u0000\u0001\u000f\u0010"+
		"\u0003\u0002\u0001\u0000\u0010\u0011\u0005\u0000\u0000\u0001\u0011\u0013"+
		"\u0001\u0000\u0000\u0000\u0012\u000e\u0001\u0000\u0000\u0000\u0012\u000f"+
		"\u0001\u0000\u0000\u0000\u0013\u0001\u0001\u0000\u0000\u0000\u0014\u001c"+
		"\u0003\u0004\u0002\u0000\u0015\u001c\u0003\u0006\u0003\u0000\u0016\u0017"+
		"\u0005\u0001\u0000\u0000\u0017\u0018\u0003\u0002\u0001\u0000\u0018\u0019"+
		"\u0005\u0002\u0000\u0000\u0019\u001c\u0001\u0000\u0000\u0000\u001a\u001c"+
		"\u0003\b\u0004\u0000\u001b\u0014\u0001\u0000\u0000\u0000\u001b\u0015\u0001"+
		"\u0000\u0000\u0000\u001b\u0016\u0001\u0000\u0000\u0000\u001b\u001a\u0001"+
		"\u0000\u0000\u0000\u001c\u0003\u0001\u0000\u0000\u0000\u001d#\u0003\b"+
		"\u0004\u0000\u001e\u001f\u0005\u0001\u0000\u0000\u001f \u0003\u0002\u0001"+
		"\u0000 !\u0005\u0002\u0000\u0000!#\u0001\u0000\u0000\u0000\"\u001d\u0001"+
		"\u0000\u0000\u0000\"\u001e\u0001\u0000\u0000\u0000#,\u0001\u0000\u0000"+
		"\u0000$*\u0003\n\u0005\u0000%+\u0003\b\u0004\u0000&\'\u0005\u0001\u0000"+
		"\u0000\'(\u0003\u0002\u0001\u0000()\u0005\u0002\u0000\u0000)+\u0001\u0000"+
		"\u0000\u0000*%\u0001\u0000\u0000\u0000*&\u0001\u0000\u0000\u0000+-\u0001"+
		"\u0000\u0000\u0000,$\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000"+
		".,\u0001\u0000\u0000\u0000./\u0001\u0000\u0000\u0000/\u0005\u0001\u0000"+
		"\u0000\u000006\u0003\b\u0004\u000012\u0005\u0001\u0000\u000023\u0003\u0002"+
		"\u0001\u000034\u0005\u0002\u0000\u000046\u0001\u0000\u0000\u000050\u0001"+
		"\u0000\u0000\u000051\u0001\u0000\u0000\u00006?\u0001\u0000\u0000\u0000"+
		"7=\u0003\f\u0006\u00008>\u0003\b\u0004\u00009:\u0005\u0001\u0000\u0000"+
		":;\u0003\u0002\u0001\u0000;<\u0005\u0002\u0000\u0000<>\u0001\u0000\u0000"+
		"\u0000=8\u0001\u0000\u0000\u0000=9\u0001\u0000\u0000\u0000>@\u0001\u0000"+
		"\u0000\u0000?7\u0001\u0000\u0000\u0000@A\u0001\u0000\u0000\u0000A?\u0001"+
		"\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000B\u0007\u0001\u0000\u0000"+
		"\u0000CD\u0005\u0005\u0000\u0000D\t\u0001\u0000\u0000\u0000EF\u0005\u0003"+
		"\u0000\u0000F\u000b\u0001\u0000\u0000\u0000GH\u0005\u0004\u0000\u0000"+
		"H\r\u0001\u0000\u0000\u0000\b\u0012\u001b\"*.5=A";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}