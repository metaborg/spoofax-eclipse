package org.strategoxt.imp.runtime.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import lpg.runtime.IAst;
import lpg.runtime.Monitor;
import lpg.runtime.PrsStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.parser.IParser;
import org.spoofax.interpreter.Interpreter;
import org.spoofax.interpreter.InterpreterException;
import org.spoofax.interpreter.adapter.aterm.WrappedATerm;
import org.spoofax.interpreter.adapter.aterm.WrappedATermFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.InvalidParseTableException;
import org.spoofax.jsglr.ParseTable;
import org.spoofax.jsglr.ParseTableManager;
import org.spoofax.jsglr.SGLR;
import org.spoofax.jsglr.SGLRException;

import aterm.ATerm;
import aterm.ATermFactory;

/** IParser implementation for SGLR. */ 
public class SGLRParser implements IParser {
	private static final int EOFT_SYMBOL = -1;
	
	private final static WrappedATermFactory wrappedFactory
		= new WrappedATermFactory();
	
	private final static ATermFactory factory
		= wrappedFactory.getFactory();

	private final static Interpreter interpreter
		= new Interpreter(wrappedFactory);
	
	private final static ParseTableManager parseTables
		= new ParseTableManager(factory);
	
	private final SGLR parser;
	
	private final PrsStream parseStream = new PrsStream();
	
	// Simple accessors

	public int getEOFTokenKind() {
		return EOFT_SYMBOL;
	}

	public PrsStream getParseStream() {
		return parseStream;
	}
	
	public ATermFactory getFactory() {
		return factory;
	}
	
	// Initialization and parsing
	
	public SGLRParser(ParseTable parseTable) {		
		parser = new SGLR(factory, parseTable);		
	}
	
	public SGLRParser(String parseTable)
			throws FileNotFoundException, IOException, InvalidParseTableException {
		this(parseTables.loadFromFile(parseTable));
	}
	
	public ATerm parse(IPath input) throws SGLRException, IOException {
		InputStream stream = FileLocator.openStream(Activator.getBundle(), input, false);
		ATerm asfix = parser.parse(stream);
		
		try {
			interpreter.load(wrappedFactory.wrapTerm(asfix));
			interpreter.invoke("implode-asfix");
			WrappedATerm wrappedTerm = (WrappedATerm) interpreter.current();
			
			return wrappedTerm.getATerm();			
		} catch (InterpreterException x) {
			throw new RuntimeException(x);
		}
	}
	
	// LPG compatibility

	@Deprecated
	public SGLR parser(Monitor monitor, int error_repair_count) {
		// TODO: Return SGLR Parser implementation? 
		throw new UnsupportedOperationException();
	}

}
