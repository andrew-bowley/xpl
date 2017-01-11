/**
 * 
 */
package au.com.cybersearch2.classy_logic.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.com.cybersearch2.classy_logic.FunctionManager;
import au.com.cybersearch2.classy_logic.ProviderManager;
import au.com.cybersearch2.classy_logic.QueryParams;
import au.com.cybersearch2.classy_logic.QueryProgram;
import au.com.cybersearch2.classy_logic.Scope;
import au.com.cybersearch2.classy_logic.expression.CallOperand;
import au.com.cybersearch2.classy_logic.expression.ExpressionException;
import au.com.cybersearch2.classy_logic.expression.StringOperand;
import au.com.cybersearch2.classy_logic.expression.Variable;
import au.com.cybersearch2.classy_logic.helper.OperandParam;
import au.com.cybersearch2.classy_logic.helper.QualifiedName;
import au.com.cybersearch2.classy_logic.helper.QualifiedTemplateName;
import au.com.cybersearch2.classy_logic.interfaces.AxiomListener;
import au.com.cybersearch2.classy_logic.interfaces.AxiomProvider;
import au.com.cybersearch2.classy_logic.interfaces.AxiomSource;
import au.com.cybersearch2.classy_logic.interfaces.CallEvaluator;
import au.com.cybersearch2.classy_logic.interfaces.FunctionProvider;
import au.com.cybersearch2.classy_logic.interfaces.ItemList;
import au.com.cybersearch2.classy_logic.interfaces.LocaleListener;
import au.com.cybersearch2.classy_logic.interfaces.Operand;
import au.com.cybersearch2.classy_logic.pattern.Axiom;
import au.com.cybersearch2.classy_logic.pattern.KeyName;
import au.com.cybersearch2.classy_logic.pattern.Template;
import au.com.cybersearch2.classy_logic.query.AxiomListSource;
import au.com.cybersearch2.classy_logic.query.QuerySpec;
import au.com.cybersearch2.classy_logic.query.QueryType;
import au.com.cybersearch2.classy_logic.query.SingleAxiomSource;
import au.com.cybersearch2.classy_logic.interfaces.Term;
import au.com.cybersearch2.classy_logic.list.AxiomList;
import au.com.cybersearch2.classy_logic.list.AxiomListSpec;
import au.com.cybersearch2.classy_logic.list.AxiomListVariable;
import au.com.cybersearch2.classy_logic.list.AxiomTermList;


/**
 * ParserAssembler
 * Collects and organizes information gathered compiling an XPL script.
 * Contained are the details gathered from parsing all statements within a specified scoped.
 * @author Andrew Bowley
 *
 * @since 15/10/2010
 */
public class ParserAssembler implements LocaleListener
{
    /**
     * ExternalAxiomSource
     * Binds client-supplied ProviderManager object. 
     * Allows dependency injection to be avoided if external axiom sources are not used. 
     * @author Andrew Bowley
     * 4 Aug 2015
     */
	public static class ExternalAxiomSource
	{
		protected ProviderManager providerManager;
		
		public ExternalAxiomSource(ProviderManager providerManager)
		{
			this.providerManager = providerManager;
		}

	    /**
	     * Returns Axiom Provider specified by name
	     * @param name Axiom Provider qualified name
	     * @return AxiomProvider implementation or null if not found
	     */
		public AxiomProvider getAxiomProvider(QualifiedName name)
		{
			return providerManager.getAxiomProvider(name);
		}
		
	    /**
	     * Returns Axiom Source of specified Axiom Provider and Axiom names
	     * @param name Axiom Provider qualified name
	     * @param axiomName Axiom name
	     * @param axiomTermNameList List of term names constrains which terms are included and their order
	     * @return AxiomSource implementation or null if axiom provider not found
	     * @throws ExpressionException if axiom provider not found
	     */
		public AxiomSource getAxiomSource(QualifiedName name, String axiomName,
				List<String> axiomTermNameList) 
		{
	        return providerManager.getAxiomSource(name, axiomName, axiomTermNameList);
		}
	}

	/**
	 * 
	 * ExternalFunctionProvider
     * Binds client-supplied FunctionManager object. 
     * Allows dependency injection to be avoided if external functions are not used. 
	 * @author Andrew Bowley
	 * 4 Aug 2015
	 */
	public static class ExternalFunctionProvider
	{
        protected FunctionManager functionManager;
        
        public ExternalFunctionProvider(FunctionManager functionManager)
        {
        	this.functionManager = functionManager;
        }

        public FunctionProvider<?> getFunctionProvider(String name)
        {
            return functionManager.getFunctionProvider(name);
        }
	}
	
	/** Scope */
    protected Scope scope;
    /** The operands, which are terms placed in expressions */
	protected OperandMap operandMap;
	/** The axioms which are declared within the enclosing scope */ 
	protected Map<QualifiedName, List<Axiom>> axiomListMap;
	/** Container for axioms under construction */
	protected Map<QualifiedName, Axiom> axiomMap;
	/** Axiom term names */
	protected Map<QualifiedName, List<String>> axiomTermNameMap;
	/** The templates */
	protected Map<QualifiedName, Template> templateMap;
	/** The axiom listeners, all belonging to list variables */
	protected Map<QualifiedName, List<AxiomListener>> axiomListenerMap;
	/** Maps qualified axiom name to resource name */
	protected Map<QualifiedName, QualifiedName> axiomResourceMap;
	/** List of Locale listeners which are notified of change of scope */
	protected List<LocaleListener> localeListenerList;
	/** Axioms which are bound to the current scope */
	protected Map<QualifiedName, Axiom> scopeAxiomMap;
	/** Axioms used as parameters */
	protected List<QualifiedName> parameterList;
	/** Optional ProviderManager for external axiom sources */
	protected ProviderManager providerManager;
	/** Optional FunctionManager for library operands */
	protected FunctionManager functionManager;


	/** Axiom provider connects to persistence back end */
	ExternalAxiomSource externalAxiomSource;
	/** External function provider for exection plug ins */
	ExternalFunctionProvider externalFunctionProvider;
	
	/**
	 * Construct a ParserAssembler object 
	 * @param scope The name of the enclosing scope 
	 */
	public ParserAssembler(Scope scope)
	{
		this.scope = scope;
		operandMap = new OperandMap(new QualifiedName(scope.getAlias(), QualifiedName.EMPTY));
	    axiomListMap = new HashMap<QualifiedName, List<Axiom>>();
	    axiomMap = new HashMap<QualifiedName, Axiom>();
	    axiomTermNameMap = new HashMap<QualifiedName, List<String>>();
	    templateMap = new HashMap<QualifiedName, Template>();
	    axiomListenerMap = new HashMap<QualifiedName, List<AxiomListener>>();
	    axiomResourceMap = new HashMap<QualifiedName, QualifiedName>();
	    localeListenerList = new ArrayList<LocaleListener>();
	    parameterList = new ArrayList<QualifiedName>();
	}
	
	/**
	 * Returns object containing all operands and item lists
	 * @return OperandMap object
	 */
    public OperandMap getOperandMap()
    {
    	return operandMap;
    }

    public void setProviderManager(ProviderManager providerManager) 
    {
		this.providerManager = providerManager;
	}

	public void setFunctionManager(FunctionManager functionManager) 
	{
		this.functionManager = functionManager;
	}

	/**
     * Returns enclosing scope
     * @return Scope object
     */
    public Scope getScope()
    {
    	return scope;
    }
 
    /**
     * Returns Locale of enclosing scope
     * @return Locale object
     */
    public Locale getScopeLocale()
    {
    	return scope.getLocale();
    }
    
    /**
     * Add contents of another ParserAssembler to this object
     * @param parserAssembler Other ParserAssembler object
     */
	public void addAll(ParserAssembler parserAssembler) 
	{
		operandMap.putAll(parserAssembler.operandMap);
		axiomListMap.putAll(parserAssembler.axiomListMap);
		axiomMap.putAll(parserAssembler.axiomMap);
		axiomTermNameMap.putAll(parserAssembler.axiomTermNameMap);
		templateMap.putAll(parserAssembler.templateMap);
		axiomListenerMap.putAll(parserAssembler.getAxiomListenerMap());
		axiomResourceMap.putAll(parserAssembler.axiomResourceMap);
		parameterList.addAll(parserAssembler.parameterList);
	}

	/**
	 * Add a new template to this ParserAssembler
     * @param qualifiedTemplateName Qualified template name
	 * @param isCalculator Flag true if template declared a calculator
	 */
	public Template createTemplate(QualifiedName qualifiedTemplateName, boolean isCalculator)
	{
		Template template = new Template(qualifiedTemplateName);
		template.setCalculator(isCalculator);
		templateMap.put(qualifiedTemplateName, template);
		return template;
	}

    /**
     * Add a term to a template
     * @param qualifiedTemplateName Qualified template name
     * @param term Operand object
     */
    public void addTemplate(QualifiedName qualifiedTemplateName, Operand term)
    {
        Template template = templateMap.get(qualifiedTemplateName);
        template.addTerm(term);
    }

	/**
	 * Set template properties - applies only to Calculator
     * @param qualifiedTemplateName Qualified template name
	 * @param properties
	 * @see au.com.cybersearch2.classy_logic.pattern.Template#initialize()
	 */
	public void addTemplate(QualifiedName qualifiedTemplateName, Map<String, Object> properties)
	{
		Template template = templateMap.get(qualifiedTemplateName);
		template.addProperties(properties);
	}

	/**
	 * Returns template with specified qualified name
	 * @param qualifiedTemplateName Qualified template name
	 * @return Template object or null if template not found
	 */
	public Template getTemplate(QualifiedName qualifiedTemplateName)
    {
	    return templateMap.get(qualifiedTemplateName);
    }

	/**
	 * Returns template with specified name
	 * @param textName
	 * @return Template object or null if template not found
	 */
    public Template getTemplate(String textName)
    {
        return templateMap.get(QualifiedName.parseTemplateName(textName));
    }

	/**
	 * Create Variable to contain inner Tempate values and add to OperandMap
	 * @param innerTemplate The inner Template
	 */
	public void addInnerTemplate(Template innerTemplate)
	{
	    // Create Variable to be axiomTermList container. Give it the same name as the inner Template 
	    // so it is qualified by the name of the enclosing Template
	    Variable listVariable = new Variable(innerTemplate.getQualifiedName());
	    // Add variable to OperandMap so it can be referenced from script
	    operandMap.addOperand(listVariable);
	}
	
	/**
	 * Add a new axiom to this ParserAssembler
	 * @param qualifiedAxiomName Qualified axiom name
	 */
	public void createAxiom(QualifiedName qualifiedAxiomName)
	{
		List<Axiom> axiomList = new ArrayList<Axiom>();
		axiomListMap.put(qualifiedAxiomName, axiomList);
	}

	/**
	 * Add a term to axiom under construction
	 * @param qualifiedAxiomName
	 * @param term Term object
	 */
	public void addAxiom(QualifiedName qualifiedAxiomName, Term term)
	{
		Axiom axiom = axiomMap.get(qualifiedAxiomName);
		if (axiom == null)
		{   // No axiom currently under construction, so create one.
			axiom = new Axiom(qualifiedAxiomName.getName());
			axiomMap.put(qualifiedAxiomName, axiom);
		}
		// Use declared term name, if specified
		List<String> termNameList = axiomTermNameMap.get(qualifiedAxiomName);
		if (termNameList != null)
			axiom.addTerm(term, termNameList);
		else
			axiom.addTerm(term);
	}

	/**
	 * Add scope-bound axiom. 
	 * This axiom will be passed to the query by containing it in
	 * the QueryParams initialSolution object.
	 * The axiom can also be declared in the script as a parameter,
	 * which allows checking that required terms are present.
	 * All scope-bound axioms are removed on scope context reset at the
	 * conclusion of a query.
	 * @param axiom Axiom object
	 * @see au.com.cybersearch2.classy_logic.QueryParams#initialize()
	 * @see #setParameter(au.com.cybersearch2.classy_logic.helper.QualifiedName)
     */
	public void addScopeAxiom(Axiom axiom)
	{
		if (scopeAxiomMap == null)
			scopeAxiomMap = new HashMap<QualifiedName, Axiom>();
		String axiomName = axiom.getName();
		QualifiedName qualifiedAxiomName = QualifiedName.parseGlobalName(axiomName);
		List<String> termNameList = getAxiomTermNameList(qualifiedAxiomName);
		if (termNameList != null)
		{	
		    for (String termName: termNameList)
		    {
		        if (axiom.getTermByName(termName) == null)
		            throw new ExpressionException("Axiom \"" + axiomName + "\" missing term \"" + termName + "\"");
		    }
		}
		scopeAxiomMap.put(qualifiedAxiomName, axiom);
	}
	
	/**
	 * Add name to list of axiom term names
	 * @param qualifiedAxiomName
	 * @param termName
	 */
	public void addAxiomTermName(QualifiedName qualifiedAxiomName, String termName)
	{
		List<String> termNameList = axiomTermNameMap.get(qualifiedAxiomName);
		if (termNameList == null)
		{
			termNameList = new ArrayList<String>();
			axiomTermNameMap.put(qualifiedAxiomName, termNameList);
		}
		termNameList.add(termName);
	}
	
	/**
	 * Get axiom term name by position
	 * @param qualifiedAxiomName
	 * @param position 
	 */
	public String getAxiomTermName(QualifiedName qualifiedAxiomName, int position)
	{
		List<String> termNameList = axiomTermNameMap.get(qualifiedAxiomName);
		if (termNameList == null)
		    return null;
		return termNameList.get(position);
	}
	
	/**
	 * Returns list of axiom term names
	 * @param qualifiedAxionName
	 */
	public List<String> getAxiomTermNameList(QualifiedName qualifiedAxionName)
	{
	    List<String> axiomTermNameList = scope.getGlobalParserAssembler().axiomTermNameMap.get(qualifiedAxionName);
	    if ((axiomTermNameList == null) && !QueryProgram.GLOBAL_SCOPE.equals(scope.getName()))
	        axiomTermNameList = axiomTermNameMap.get(qualifiedAxionName);
	    return axiomTermNameList;
	}
	
	/**
	 * Transfer axiom under construction to the list of axioms with same name
	 * @param qualifiedAxiomName
	 */
	public Axiom saveAxiom(QualifiedName qualifiedAxiomName)
	{
		Axiom axiom = axiomMap.get(qualifiedAxiomName);
		List<Axiom> axiomList = axiomListMap.get(qualifiedAxiomName);
		axiomList.add(axiom);
		axiomMap.remove(qualifiedAxiomName);
		return axiom;
	}

    /**
     * Bind resource to axiom provider using qualified axiom name as resource name
     * @param qualifiedBindingName Qualified name of axiom or template
     * @throws ExpressionException if axiom provider not found
     */
    public AxiomProvider bindResource(QualifiedName qualifiedBindingName)
    {
        // Try implicit resource name same as axiom name
        // If this fails, try match to full qualified name as text
        String name = qualifiedBindingName.getName();
        if (name.isEmpty())
            name = qualifiedBindingName.getTemplate();
        QualifiedName resourceName = new QualifiedName(name);
        AxiomProvider axiomProvider = getAxiomProvider(resourceName);
        return (axiomProvider != null) ? 
            bindResource(resourceName, qualifiedBindingName) :
            bindResource(qualifiedBindingName, qualifiedBindingName);
    }
    
    /**
     * Bind resource to axiom provider for given qualified axiom name and remove internal reference
     * @param resourceName Resource qualified name
     * @param qualifiedBindingName Qulified name of axiom or template
     * @throws ExpressionException if axiom provider not found
     */
    public AxiomProvider bindResource(QualifiedName resourceName, QualifiedName qualifiedBindingName)
    {
        // Note resource references axiom by qualified name, which prepends scope name
        AxiomProvider axiomProvider = getAxiomProvider(resourceName);
        if (axiomProvider == null) 
            throw new ExpressionException("Axiom provider \"" + resourceName + "\" not found");
        if (!qualifiedBindingName.getTemplate().isEmpty())
            registerAxiomListener(qualifiedBindingName, axiomProvider.getAxiomListener(qualifiedBindingName.toString()));
        else
            // Remove entry from axiomListMap so the axiom is not regarded as internal
            axiomListMap.remove(qualifiedBindingName);
            
        // Preserve mapping of qualified axiom name to resource name
        axiomResourceMap.put(qualifiedBindingName, resourceName);
        return axiomProvider;
    }

	/**
	 * Register name of axiom to be supplied as a parameter
	 * @param qualifiedAxiomName Qualified name of axiom to which properties apply
	 */
	public void setParameter(QualifiedName qualifiedAxiomName)
	{
	    parameterList.add(qualifiedAxiomName);
	    if (!axiomListMap.containsKey(qualifiedAxiomName))
	        createAxiom(qualifiedAxiomName);
	}

	/**
	 * Returns flag to indicate if the axiom specified by name  is a parameter
	 * @param qualifedAxiomName Qualified name of axiom
	 * @return boolean
	 */
	public boolean isParameter(QualifiedName qualifedAxiomName)
	{
        if (parameterList == null)
            return false;
        return  parameterList.contains(qualifedAxiomName);
	}
	
	/**
	 * Returns axiom source for specified axiom name
	 * @param qualifiedAxiomName Qualified axiom name
	 * @return AxiomSource object
	 */
    public AxiomSource getAxiomSource(QualifiedName qualifiedAxiomName)
    {
        // Scope-bound axioms are passed in query parameters and
        // removed at the end of the query
    	if (scopeAxiomMap != null)
    	{   // Scope axioms are provided in query parameters.
    		Axiom axiom = scopeAxiomMap.get(qualifiedAxiomName);
    		if (axiom != null)
    			return new SingleAxiomSource(axiom);
    	}
    	// Look for list defined in the script
        if ((parameterList == null) || !parameterList.contains(qualifiedAxiomName))
        {   // Axiom is declared in script?
           	List<Axiom> axiomList = axiomListMap.get(qualifiedAxiomName);
        	if (axiomList != null)
        	{   
        	    List<String> terminalNameList = (axiomTermNameMap.get(qualifiedAxiomName));
        	    AxiomListSource axiomListSource = new AxiomListSource(axiomList);
        	    axiomListSource.setAxiomTermNameList(terminalNameList);
        		return axiomListSource;
        	}
        }
        QualifiedName resourceName = axiomResourceMap.get(qualifiedAxiomName);
    	if (resourceName == null)
    		return null;
    	List<String> axiomTermNameList = axiomTermNameMap.get(qualifiedAxiomName);
    	if (axiomTermNameList == null)
    	    axiomTermNameList = Collections.emptyList();
     	return getAxiomProvider(resourceName).getAxiomSource(qualifiedAxiomName.toString(), axiomTermNameList); 
    }

    /**
     * Returns object containing all axiom listeners belonging to this scope
     * @return  Unmodifiable AxiomListener map object
     */
	public Map<QualifiedName, List<AxiomListener>> getAxiomListenerMap()
	{
		return Collections.unmodifiableMap(axiomListenerMap);
	}

	/**
	 * Register axiom term list by adding it's axiom listener to this ParserAssembler object
	 * @param axiomTermList The term list
	 */
	public void registerAxiomTermList(AxiomTermList axiomTermList)
	{
		AxiomListener axiomListener = axiomTermList.getAxiomListener();
		QualifiedName qualifiedAxiomName = QualifiedName.parseName(axiomTermList.getKey());
		List<AxiomListener> axiomListenerList = getAxiomListenerList(qualifiedAxiomName);
		axiomListenerList.add(axiomListener);
		axiomTermList.setAxiomTermNameList(axiomTermNameMap.get(qualifiedAxiomName));
	}

	/**
	 * Register axiom term list for local axiom
	 * @param axiomTermList The term list
	 */
	public void registerLocalList(AxiomTermList axiomTermList)
	{
        QualifiedName qualifiedAxiomName = QualifiedName.parseName(axiomTermList.getKey());
		axiomTermList.setAxiomTermNameList(axiomTermNameMap.get(qualifiedAxiomName));
		scope.addLocalAxiomListener(qualifiedAxiomName, axiomTermList.getAxiomListener());
	}

	/**
	 * Register locale listener to be notified when the scope changes
	 * @param localeListener LocaleListener object
	 */
	public void registerLocaleListener(LocaleListener localeListener)
	{
		localeListenerList.add(localeListener);
	}

	/**
	 * Notify all locale listeners that the scope has changed
	 * @see au.com.cybersearch2.classy_logic.interfaces.LocaleListener#onScopeChange(au.com.cybersearch2.classy_logic.Scope)
	 */
	@Override
	public void onScopeChange(Scope scope) 
	{
		for (LocaleListener localeListener: localeListenerList)
			localeListener.onScopeChange(scope);
	}

	/**
	 * Remove all scope-bound axioms
	 */
	public void clearScopeAxioms()
	{
        if (scopeAxiomMap != null)
          scopeAxiomMap.clear();
	}
	
	/**
	 * Returns list of axiom listeners for specified key
	 * @param qname
	 * @return List containing AxiomListener objects 
	 */
	protected List<AxiomListener> getAxiomListenerList(QualifiedName qname)
	{
		List<AxiomListener> axiomListenerList = axiomListenerMap.get(qname);
		if (axiomListenerList == null)
		{
			axiomListenerList = new ArrayList<AxiomListener>();
			axiomListenerMap.put(qname, axiomListenerList);
		}
		return axiomListenerList;
	}
	
	/**
	 * Register axiom list by adding it's axiom listener to this ParserAssembler object
	 * @param axiomList The axiom list
	 */
	public void registerAxiomList(AxiomList axiomList) 
	{
		AxiomListener axiomListener = axiomList.getAxiomListener();
		QualifiedName axiomKey = QualifiedName.parseName(axiomList.getKey());
        QualifiedName qualifiedAxiomName = findQualifiedAxiomName(axiomKey);
        if (qualifiedAxiomName == null)
            // Assume key is for template
            qualifiedAxiomName = axiomKey;
        QualifiedName qualifiedTemplateName = new QualifiedTemplateName(qualifiedAxiomName.getScope(), axiomKey.getName());
        boolean isChoice = templateMap.containsKey(qualifiedTemplateName) &&
                            templateMap.get(qualifiedTemplateName).isChoice();
        List<Axiom> internalAxiomList = axiomListMap.get(qualifiedAxiomName);
        // Populate list if already created by the script being compiled
        if (!isChoice && (internalAxiomList != null))
        {
            for (Axiom axiom: internalAxiomList)
                axiomListener.onNextAxiom(axiom);
        }
        else
        {
		    List<AxiomListener> axiomListenerList = getAxiomListenerList(qualifiedTemplateName);
		    axiomListenerList.add(axiomListener);
        }
		List<String> axiomTermNameList = axiomTermNameMap.get(qualifiedAxiomName);
		if (axiomTermNameList != null)
		{
	        axiomList.setAxiomTermNameList(axiomTermNameList);
	        return;
		}
		setAxiomTermNameList(qualifiedTemplateName, axiomList);
	}

	/**
	 * Returns qualified name of axiom source specified by qname
	 * @param qname
	 * @return QualifiedName object or null if axiom source not found
	 */
	public QualifiedName findQualifiedAxiomName(QualifiedName qname)
    {
        //QualifiedName qname = getContextName(key);
        if (isQualifiedAxiomName(qname))
            return qname;
        if (!qname.getTemplate().isEmpty())
        {
            qname.clearTemplate();
            if (isQualifiedAxiomName(qname))
                return qname;
        }
        if (!qname.getScope().isEmpty())
        {
            qname.clearScope();
            if (isQualifiedAxiomName(qname))
                return qname;
        }
        return null; 
    }

	/**
	 * Returns flag set true if given qualified name identifies an axiom source
	 * @param qname Qualified axiom name
	 * @return boolean
	 */
	public boolean isQualifiedAxiomName(QualifiedName qname)
    {
        if ((scopeAxiomMap != null) && (scopeAxiomMap.get(qname) != null))
            return true;
        if (axiomListMap.get(qname) != null)
            return true;
        if (axiomResourceMap.get(qname) != null)
            return true;
        return false;
    }

    /**
	 * Set axiom term name list from template
	 * @param qualifiedTemplateName Qualified name of template
	 * @param axiomList Axiom list to be updated
	 * @return List of term names
	 */
	public List<String> setAxiomTermNameList(QualifiedName qualifiedTemplateName, AxiomList axiomList)
    {
	    List<String> axiomTermNameList = null;
        Template template = templateMap.get(qualifiedTemplateName);
        if (template == null)
            throw new ExpressionException("Template \"" + qualifiedTemplateName.toString() + "\" not found");
        if (template.isChoice())
        {
            // Get axiom source for this Choice and determine it's scope
            String axiomKey = qualifiedTemplateName.getTemplate();
            AxiomSource choiceAxiomSource = 
                qualifiedTemplateName.getScope().isEmpty() ? 
                scope.getGlobalScope().getAxiomSource(axiomKey) : 
                scope.getAxiomSource(axiomKey);
            return choiceAxiomSource.getAxiomTermNameList();
        }
        if (template != null)
        {
            axiomTermNameList = new ArrayList<String>();
            for (int i = 0; i < template.getTermCount(); i++)
            {
                Term term = template.getTermByIndex(i);
                if (term.getName().isEmpty())
                    break;
                axiomTermNameList.add(term.getName());
            }
            if (axiomTermNameList.size() > 0)
                axiomList.setAxiomTermNameList(axiomTermNameList);
        }
        return axiomTermNameList;
    }

	/**
	 * Register axiom list by adding it's axiom listener to this ParserAssembler object
	 * @param qname The qualified name of the axioms inserted into the list
	 * @param axiomListener The axiom listener
	 */
	public void registerAxiomListener(QualifiedName qname, AxiomListener axiomListener) 
	{   // Note to listen for solution notifications, qname must be template name 
		List<AxiomListener> axiomListenerList = getAxiomListenerList(qname);
		axiomListenerList.add(axiomListener);
	}

	/**
	 * Returns the given name prepended with the scope name and a dot 
	 * unless this ParserAssembler is enclosing the Global scope.
	 * Required for axiomProvider which is possibly shared by more than one scope.
	 * @param name Simple name
	 * @return Qualified name
	 */
	protected String getQualifiedName(String name)
	{
		return QueryProgram.GLOBAL_SCOPE.equals(scope.getName()) ? name : (scope.getName() + "." + name);
	}

	/**
	 * Create new template and add to head template chain
	 * @param qualifiedTemplateName Qualified name of head template
	 * @param chainName Name of template to add to chain
	 * @return Template object
	 */
	public Template chainTemplate(QualifiedName qualifiedTemplateName, String chainName) 
	{
		Template template = getTemplate(qualifiedTemplateName);
		Template chainTemplate = new Template(new QualifiedTemplateName(qualifiedTemplateName.getScope(), chainName));
		template.setNext(chainTemplate);
		templateMap.put(chainTemplate.getQualifiedName(), chainTemplate);
		return chainTemplate;
	}

	/**
	 * Returns operand which invokes a function call in script. The function must be
	 * provided in an external library.
	 * The function name must consist of 2 parts. The first is the name of a library.
	 * If the first part is a library name, then the second part is the name of a function in that library.
	 * The type of object returned from the call depends on the library.
	 * @param qname Qualified function name
	 * @param operandParamList List of Operand arguments or null for no arguments
	 * @return CallOperand object
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
    public Operand getCallOperand(QualifiedName qname, List<OperandParam> operandParamList)
	{
        String library = qname.getTemplate();
        String name = qname.getName();
        if (library.isEmpty())
            throw new ExpressionException("Call name \"" + qname.toString() + "\" is invalid");
        String callName = library + "." + name;
	    if (externalFunctionProvider == null)
	    {
	    	if (functionManager == null)
	    		functionManager = new FunctionManager(){};
	        externalFunctionProvider = new ExternalFunctionProvider(functionManager);
	    }
	    FunctionProvider<?> functionProvider = externalFunctionProvider.getFunctionProvider(library);
	    CallEvaluator<?>callEvaluator = functionProvider.getCallEvaluator(name);
	    if (callEvaluator == null)
	        throw new ExpressionException("Function \"" + name + "\" not supported");
        return new CallOperand(QualifiedName.parseName(callName, qname), callEvaluator, operandParamList);
	}

	/**
	 * Returns operand which invokes a query call.
	 * @param qualifiedQueryName Qualified query name - can be qualified by the name of a scope
     * @param operandParamList List of Operand arguments null for no arguments
	 * @param innerTemplate Optional template to recieve query results
	 * @return CallOperand object containing a QueryEvaluator object
	 * @see QueryEvaluator
	 */
    public Operand getQueryOperand(QualifiedName qualifiedQueryName, List<OperandParam> operandParamList, Template innerTemplate)
    {
        Scope functionScope = null;
        String library = qualifiedQueryName.getScope();
        final String queryName = qualifiedQueryName.getName();
        if (library.isEmpty())
        {
            // Inner call
            functionScope = scope;
            library = scope.getAlias();
        }
        else
            functionScope = scope.findScope(library);
        String callName = library + "." + queryName;
        if (functionScope == null)
            throw new ExpressionException("Scope \"" + queryName + "\" not found");
        QuerySpec querySpec = functionScope.getQuerySpec(queryName);
        if (querySpec == null)
        {
            QualifiedName qualifiedTemplateName = new QualifiedTemplateName(library, queryName);
            if ((functionScope == scope) && (getTemplate(qualifiedTemplateName) == null))
                throw new ExpressionException("Query \"" + queryName + "\" not found in scope \"" + library + "\"");
            querySpec = new QuerySpec(callName);
            querySpec.addKeyName(new KeyName("", queryName));
            querySpec.setQueryType(QueryType.calculator);
        }
        QueryParams queryParams = new QueryParams(functionScope, querySpec);
        if (innerTemplate != null)
            addInnerTemplate(innerTemplate);
        QueryEvaluator queryEvaluator = 
                new QueryEvaluator(queryParams, scope, innerTemplate);
        QualifiedName qualifiedCallName = new QualifiedName(library, queryName);
        return new CallOperand<AxiomTermList>(qualifiedCallName, queryEvaluator, operandParamList);
    }

	/**
	 * Returns axiom provider specified by resource name
	 * @param resourceName AxiomProvider qualified name
	 * @return AxiomProvider object or null if object not found
	 */
	public AxiomProvider getAxiomProvider(QualifiedName resourceName) 
	{
    	if (externalAxiomSource == null)
    	{
    		if (providerManager == null)
    		    providerManager = new ProviderManager(){};
    		externalAxiomSource = new ExternalAxiomSource(providerManager);
    	}
		return externalAxiomSource.getAxiomProvider(resourceName);
	}

	/**
	 * Returns item list specified by qualified name.
	 * @param qname Qualified name of list
	 * @return ItemList object or null if an axiom parameter is named and is not yet set 
	 */
    public ItemList<?> getItemList(QualifiedName qname)
    {
        ItemList<?> itemList = findItemList(qname);
        if (itemList == null)
            throw new ExpressionException("List not found with name \"" + qname.toString() + "\"");
        return itemList;
    }

    /**
     * Returns item list identified by name
     * @param listName
     * @return ItemList
     * @throws ExpressionException if item list not found
     */
    public ItemList<?> getItemList(String listName)
    {
        ItemList<?> itemList = findItemList(listName);
        if (itemList == null)
            itemList = getItemList(QualifiedName.parseName(listName));
        return itemList;
    }
    
    /**
     * Returns item list specified by qualified name.
     * @param qname Qualified name of list
     * @return ItemList object or null if not found
     */
    public ItemList<?> findItemList(QualifiedName qname)
    {
        Scope nameScope = qname.getScope().isEmpty() ? scope.getGlobalScope() : scope.findScope(qname.getScope());
        if (nameScope == null)
            return null;
        OperandMap operandMap = nameScope.getParserAssembler().getOperandMap();
        ItemList<?> itemList = operandMap.getItemList(qname);
        if (itemList == null)
        {
            Operand operand = operandMap.getOperand(qname);
            if ((operand != null) && 
                 !operand.isEmpty() &&
                 (operand.getValueClass() == AxiomTermList.class))
            {
                AxiomTermList axiomTermList = (AxiomTermList)operand.getValue();
                axiomTermNameMap.put(qname, axiomTermList.getAxiomTermNameList());
                return axiomTermList;
            }
       }
       return itemList;
    }

    /**
     * Returns item list specified by name.
     * @param listName Name of list
     * @return ItemList object or null if not found
     */
    public ItemList<?> findItemList(String listName)
    {
        QualifiedName qualifiedListName = QualifiedName.parseName(listName, operandMap.getQualifiedContextname());
        ItemList<?> itemList = findItemList(qualifiedListName);
        if ((itemList == null) && !qualifiedListName.getTemplate().isEmpty())
        {
            qualifiedListName.clearTemplate();
            itemList = findItemList(qualifiedListName);
        }
        if ((itemList == null) && !qualifiedListName.getScope().isEmpty())
        {
            qualifiedListName.clearScope();
            itemList = findItemList(qualifiedListName);
        }
        return itemList;
    }
    
    /**
     * Returns Operand which accesses a list
     * @param listName Name of list
     * @param param1 Operand of first index
     * @param param2 Operand of second index or null if not specified
     * @return Operand from package au.com.cybersearch2.classy_logic.list
     */
    public Operand newListVariableInstance(String listName, Operand param1, Operand param2)
    {
        QualifiedName qualifiedListName = null;
        ItemList<?> itemList = findItemList(listName);
        if (itemList != null)
            qualifiedListName = itemList.getQualifiedName(); 
        else 
        {
            Operand operand = findOperandByName(listName);
            if (operand != null)
                 qualifiedListName = operand.getQualifiedName();
        }
        if (qualifiedListName == null)
            throw new ExpressionException("List \"" + listName + "\" cannot be found");
        return newListVariableInstance(qualifiedListName, param1, param2);
    }

    /**
     * Returns Operand which accesses a list
     * @param qualifiedListName Qualified name of list
     * @param param1 Operand of first index
     * @param param2 Operand of second index or null if not specified
     * @return Operand from package au.com.cybersearch2.classy_logic.list
     */
    public Operand newListVariableInstance(QualifiedName qualifiedListName, Operand param1, Operand param2)
    {
        if ((param2 != null) && (param1 instanceof StringOperand))
            throw new ExpressionException("List \"" + qualifiedListName.toString() + "\" cannot be indexed by name");
        ItemList<?> itemList = null;
        AxiomListSpec axiomListSpec = null;
        // When an axiom parameter is specified, then initialization of the list variable must be delayed 
        // until evaluation occurs when running the first query.
        boolean isAxiomListVariable = isParameter(qualifiedListName) || (operandMap.get(qualifiedListName) != null);
        if (isAxiomListVariable)
        {   // Return dynamic AxiomListVariable instance
            axiomListSpec = new AxiomListSpec(qualifiedListName, operandMap.get(qualifiedListName), param1, param2);
            return new AxiomListVariable(axiomListSpec);
        }
        // A normal list should be ready to go
        itemList = getItemList(qualifiedListName);
        //operandMap.addItemList(listName, itemList);
        if (param2 == null) // Single index case is easy
            return operandMap.newListVariableInstance(itemList, param1);
        axiomListSpec = new AxiomListSpec((AxiomList)itemList, param1, param2);
        return operandMap.newListVariableInstance(axiomListSpec);
    }

    /**
     * Returns qualified name for name in current context
     * @param name
     * @return QualifiedName object
     */
    public QualifiedName getContextName(String name)
    {
        return QualifiedName.parseName(name, operandMap.getQualifiedContextname());
    }
    
    /**
     * Returns new ItemListVariable instance. 
     * This is wrapped in an assignment evaluator if optional expression parameter needs to be evaluated.
     * @param listName List name
     * @param index List index
     * @param expression Optional expression operand
     * @return Operand object
     */
    public Operand setListVariable(String listName, Operand index, Operand expression) 
    {
        QualifiedName qualifiedListName = QualifiedName.parseGlobalName(listName);
        ItemList<?> itemList = findItemList(qualifiedListName);
        if (itemList == null)
        {
            qualifiedListName = QualifiedName.parseName(listName, operandMap.getQualifiedContextname());
            qualifiedListName.clearTemplate();
            itemList = findItemList(qualifiedListName);
        }
        if (itemList != null)
            return operandMap.setListVariable(itemList, index, expression);
        return newListVariableInstance(listName, index, expression);
    }

    /**
     * Copy result axiom lists as iterables to supplied container
     * @param listMap Container to receive lists
     */
    public void copyLists(Map<QualifiedName, Iterable<Axiom>> listMap) 
    {
        operandMap.copyLists(listMap);    
        if (!scope.getName().equals(QueryProgram.GLOBAL_SCOPE))
            scope.getGlobalParserAssembler().copyLists(listMap);
    }

    /**
     * Returns operand identified by name
     * @param operandName
     * @return Operand object from same scope or global scope or null if not found
     */
    public Operand findOperandByName(String operandName)
    {
        QualifiedName qualifiedOperandName = QualifiedName.parseName(operandName, operandMap.getQualifiedContextname());
        Operand operand = operandMap.get(qualifiedOperandName);
        if ((operand == null) && !qualifiedOperandName.getTemplate().isEmpty())
        {
            qualifiedOperandName.clearTemplate();
            operand = operandMap.get(qualifiedOperandName);
        }
        if ((operand == null) && !qualifiedOperandName.getScope().isEmpty())
        {
            qualifiedOperandName.clearScope();
            operand = operandMap.get(qualifiedOperandName);
        }
        return operand;
    }

    /**
     * Returns qualified name for resource specified by qualified binding name
     * @param qname Qualidfied name of axiom or template bound to resource
     * @return QualifiedName object or null if not found
     */
    public QualifiedName getResourceName(QualifiedName qname)
    {
        return axiomResourceMap.get(qname);
    }
}
