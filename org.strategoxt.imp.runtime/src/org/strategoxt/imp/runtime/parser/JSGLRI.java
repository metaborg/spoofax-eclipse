package org.strategoxt.imp.runtime.parser;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.Asfix2TreeBuilder;
import org.spoofax.jsglr.client.Disambiguator;
import org.spoofax.jsglr.client.FilterException;
import org.spoofax.jsglr.client.ParseTable;
import org.spoofax.jsglr.client.imploder.ITreeFactory;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.jsglr.client.incremental.IncrementalSGLR;
import org.spoofax.jsglr.client.incremental.IncrementalSortSet;
import org.spoofax.jsglr.io.SGLR;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.attachments.ParentTermFactory;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.ParseTableProvider;

/**
 * IMP IParser implementation using JSGLR, imploding parse trees to AST nodes and tokens.
 *
 * @author Lennart Kats <L.C.L.Kats add tudelft.nl>
 */ 
public class JSGLRI extends AbstractSGLRI {
	
	private ParseTableProvider parseTable;
	
	private boolean useRecovery = false;
	
	private SGLR parser;
	
	private IncrementalSGLR incrementalParser;
	
	private Disambiguator disambiguator;
	
	private int timeout;
	
	// Initialization and parsing
	
	public JSGLRI(ParseTableProvider parseTable, String startSymbol,
			SGLRParseController controller) {
		super(parseTable, startSymbol, controller);
		
		this.parseTable = parseTable;
		this.parser = Environment.createSGLR(getParseTable());
		ITreeFactory factory = ((TreeBuilder) parser.getTreeBuilder()).getFactory();
		this.incrementalParser = new IncrementalSGLR<IStrategoTerm>(parser, null, factory, IncrementalSortSet.read(getParseTable()));
		// TODO: use IncrementalSGLR
		resetState();
	}
	
	public JSGLRI(ParseTableProvider parseTable, String startSymbol) {
		this(parseTable, startSymbol, null);
	}
	
	public JSGLRI(ParseTable parseTable, String startSymbol,
			SGLRParseController controller) {
		this(new ParseTableProvider(parseTable), startSymbol, controller);
	}
	
	public JSGLRI(ParseTable parseTable, String startSymbol) {
		this(new ParseTableProvider(parseTable), startSymbol, null);
	}
	
	public SGLR getParser() {
		return parser;
	}
	
	public Set<BadTokenException> getCollectedErrors() {
		return getParser().getCollectedErrors();
	}
	
	@Override
	public void setStartSymbol(String startSymbol) {
		super.setStartSymbol(startSymbol);
	}
	
	/**
	 * @see SGLR#setUseStructureRecovery(boolean)
	 */
	public void setUseRecovery(boolean useRecovery) {
		this.useRecovery = useRecovery;
		parser.setUseStructureRecovery(useRecovery);
	}
	
	public ParseTable getParseTable() {
		try {
			return parseTable.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Disambiguator getDisambiguator() {
		return disambiguator;
	}
	
	public void setParseTable(ParseTable parseTable) {
		this.parseTable = new ParseTableProvider(parseTable);
		resetState();
	}
	
	public void setParseTable(ParseTableProvider parseTable) {
		this.parseTable = parseTable;
		resetState();
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
		resetState();
	}
	
	/**
	 * Resets the state of this parser, reinitializing the SGLR instance
	 */
	void resetState() {
		// Reinitialize parser if parsetable changed (due to .meta file)
		if (getParseTable() != parser.getParseTable()) {
			parser = Environment.createSGLR(getParseTable());
		}
		parser.setTimeout(timeout);
		if (disambiguator != null) parser.setDisambiguator(disambiguator);
		else disambiguator = parser.getDisambiguator();
		setUseRecovery(useRecovery);
		if (!isImplodeEnabled()) {
			parser.setTreeBuilder(new Asfix2TreeBuilder(Environment.getTermFactory()));
		} else {
			assert parser.getTreeBuilder() instanceof TreeBuilder;
			assert ((TermTreeFactory) ((TreeBuilder) parser.getTreeBuilder()).getFactory()).getOriginalTermFactory()
				instanceof ParentTermFactory;
		}
	}
	
	@Override
	protected IStrategoTerm doParse(String input, String filename)
			throws TokenExpectedException, BadTokenException, SGLRException, IOException {
		
		// Read stream using tokenizer/lexstream
		
		if (parseTable.isDynamic()) {
			parseTable.initialize(new File(filename));
			resetState();
		}
		
		try {
			return (IStrategoTerm) parser.parse(input, filename, getStartSymbol());
		} catch (FilterException e) {
			if (e.getCause() == null && parser.getDisambiguator().getFilterPriorities()) {
				Environment.logException("Parse filter failure - disabling priority filters and trying again", e);
				getDisambiguator().setFilterPriorities(false);
				try {
					return (IStrategoTerm) parser.parse(input, filename, getStartSymbol());
				} finally {
					getDisambiguator().setFilterPriorities(true);
				}
			} else {
				throw new FilterException(e.getParser(), e.getMessage(), e);
			}
		}
	}
}
