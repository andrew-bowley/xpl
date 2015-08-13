/**
 * 
 */
package au.com.cybersearch2.classy_logic.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import au.com.cybersearch2.classy_logic.pattern.Axiom;
import au.com.cybersearch2.classy_logic.terms.Parameter;
import au.com.cybersearch2.classy_logic.expression.Evaluator;
import au.com.cybersearch2.classy_logic.expression.ExpressionException;
import au.com.cybersearch2.classy_logic.expression.IntegerOperand;
import au.com.cybersearch2.classy_logic.expression.Variable;
import au.com.cybersearch2.classy_logic.helper.Null;
import au.com.cybersearch2.classy_logic.interfaces.Operand;
import au.com.cybersearch2.classy_logic.interfaces.ItemList;
import au.com.cybersearch2.classy_logic.interfaces.Term;
import au.com.cybersearch2.classy_logic.list.AxiomList;
import au.com.cybersearch2.classy_logic.list.AxiomListSpec;
import au.com.cybersearch2.classy_logic.list.AxiomListVariable;
import au.com.cybersearch2.classy_logic.list.AxiomTermList;
import au.com.cybersearch2.classy_logic.list.ItemListVariable;



/**
 * OperandMap
 * Operand and ItemList container. Items are referenced by name. 
 * @author Andrew Bowley
 *
 * @since 19/10/2010
 */
public class OperandMap
{
	/** Tree map of operands for efficient lookup */
    protected Map<String, Operand> operandMap;
    /** Tree Map of item lists, which create instances of variables, some of which need to be added to the operand map */
	protected Map<String, ItemList<?>> listMap;
    
	/**
	 * Construct OperandMap object
	 */
	public OperandMap()
	{
		operandMap = new TreeMap<String, Operand>();
		listMap = new TreeMap<String, ItemList<?>>();
	}

	/**
	 * Returns all operand values in a map container
	 * @return Map with key of type Operand and value of type Object
	 */
	public  Map<Operand, Object> getOperandValues()
	{
		Map<Operand, Object> operandValueMap = new HashMap<Operand, Object>();
		for (Operand operand: operandMap.values())
		{
			if (!operand.isEmpty())
				operandValueMap.put(operand, operand.getValue());
		}
		return operandValueMap;
	}

	/**
	 * Set operand values
	 * @param operandValueMap Map container
	 */
	public void setOperandValues(Map<Operand, Object> operandValueMap)
	{
		for (Entry<Operand, Object> entry: operandValueMap.entrySet())
			if (entry.getValue() instanceof Null) // Null special case
				((Parameter)entry.getKey()).clearValue();
			else
			   entry.getKey().assign(entry.getValue());
		// Clear operands not included in operandValueMap
		for (Entry<String, Operand> entry: operandMap.entrySet())
		{
			if (!operandValueMap.containsKey(entry.getValue()))
				((Parameter)entry.getValue()).clearValue();
		}
	}

	public void duplicateOperandCheck(String name)
	{
	    if (operandMap.containsKey(name))
	        throw new ExpressionException("Duplicate Operand name \"" + name + "\" encountered");
	}
    /**
     * Add new Variable operand of specified name, unless it already exists
     * @param name
     * @param expression Optional expression
     * @return New or existing Operand object
     */
	public Operand addOperand(String name, Operand expression)
    {
		Operand param = operandMap.get(name);
		if (param == null)
		{
			param = expression == null ? new Variable(name) : new Variable(name, expression);
			operandMap.put(name, param);
		}
		else if (expression != null)
			param = new Evaluator(name, param, "=", expression);
		return param;
    }

	/**
	 * Add supplied operand to map. Assumes operand is not annonymous.
	 * Note this will replace an existing operand with the same name
	 * @param operand Operand object
	 */
	public void addOperand(Operand operand)
    {
		if (operand.getName().isEmpty())
			throw new ExpressionException("addOperand() passed annonymous object");
		duplicateOperandCheck(operand.getName());
		operandMap.put(operand.getName(), operand);
    }

	/**
	 * Returns flag to indicate if this map contains operand with specified name
	 * @param name
	 * @return boolean
	 */
	public boolean hasOperand(String name)
	{
		return operandMap.containsKey(name);
	}

	/**
	 * Returns operand of specified name or null if not found
	 * @param name
	 * @return Operand
	 */
	public Operand getOperand(String name)
	{
		return operandMap.get(name);
	}
	
	/**
	 * Add ItemList object to this container and it's name to the list Set
	 * @param name Name of list
	 * @param itemList ItemList object 
	 */
	public void addItemList(String name, ItemList<?> itemList)
	{
		if ((operandMap.get(name) != null) || listMap.containsKey(name))
			throw new ExpressionException("ItemList name \"" + name + "\" clashes with existing Operand");
		listMap.put(name, itemList);
	}

	/** 
	 * Returns Operand list referenced by name
	 * @param name
	 * @return ItemList object or null if not exists
	 */
	public ItemList<?> getItemList(String name)
	{
		return listMap.get(name);
	}
	
	/**
	 * Returns ItemListVariable object with specified name and index expression operand. 
	 * Will create object if it does not already exist.
	 * @param itemList The list
	 * @param expression Index expression operand
	 * @return ItemListVariable object
	 */
	public ItemListVariable<?> newListVariableInstance(ItemList<?> itemList, Operand expression)
	{
		String name = itemList.getName();
		ItemListVariable<?> listVariable = null;
		int index = -1;
		String suffix = null;
		if (!expression.isEmpty() && (expression instanceof IntegerOperand) && Term.ANONYMOUS.equals(expression.getName()))
	    {
		   index = ((Long)(expression.getValue())).intValue();
		   suffix = Long.toString(index);
		}
		else if (expression.isEmpty() && (expression instanceof Variable))
		{
			if (itemList instanceof AxiomTermList)
			{
				AxiomTermList axiomTermList = (AxiomTermList)itemList;
				List<String> axiomTermNameList = axiomTermList.getAxiomTermNameList();
				if (axiomTermNameList != null)
				{
					suffix = expression.getName();
					index = getIndexForName(name, suffix, axiomTermNameList);
				}
				else
					suffix = expression.toString();

			}
			else // Interpret identifier as a variable name for any primitive list
			{
				expression = new Variable(expression.getName());
				suffix = expression.getName();
			}
		}
		if (suffix == null)
			suffix = expression.getName().isEmpty() ? expression.toString() : expression.getName();
		// IntegerOperand value is treated as fixed
		if (index >= 0)
			listVariable = getListVariable(itemList, name, index, suffix);
		else
			listVariable = itemList.newVariableInstance(expression, suffix, 0);
		return listVariable;
	}

	/**
	 * Returns index of item identified by name
	 * @param listName Name of list - used only for error reporting
	 * @param item Item name
	 * @param axiomTermNameList Term names of axiom source
	 * @return Index
	 */
	protected int getIndexForName(String listName, String item, List<String> axiomTermNameList) 
	{
		for (int i = 0; i < axiomTermNameList.size(); i++)
		{
			if (item.equals(axiomTermNameList.get(i)))
				return i;
		}
		throw new ExpressionException("List \"" + listName + "\" does not have term named \"" + item + "\"");
	}

	/**
	 * Returns a list variable for a term in an axiom in a axiom list. 
	 * Both the axiom and the term are referenced by index.
	 * There is only one variable instance for any specific index combination.
	 * @param axiomListSpec AxiomList specification
	 * @return AxiomListVariable object
	 */
	public AxiomListVariable newListVariableInstance(AxiomListSpec axiomListSpec) 
	{
        AxiomListVariable listVariable = null;
		// IntegerOperand value is treated as fixed
		if ((axiomListSpec.getAxiomIndex() >= 0) && (axiomListSpec.getTermIndex() >= 0))
			// The variable only has a single instance if both axiom and term indexes are fixed
			listVariable = getListVariable(axiomListSpec);
		else
		{
		    AxiomList axiomList = axiomListSpec.getAxiomList();
		    String suffix = axiomListSpec.getSuffix();
			if ((axiomListSpec.getAxiomIndex() >= 0))
				listVariable = axiomList.newVariableInstance(axiomListSpec.getAxiomIndex(), axiomListSpec.getTermExpression(), suffix);
			else if (axiomListSpec.getTermIndex() >= 0)
				listVariable = axiomList.newVariableInstance(axiomListSpec.getAxiomExpression(), axiomListSpec.getTermIndex(), suffix);
			else
				listVariable = axiomList.newVariableInstance(axiomListSpec.getAxiomExpression(), axiomListSpec.getTermExpression(), suffix);
		}
		return listVariable;
	}

	/**
	 * Returns operand referenced by name
	 * @param name
	 * @return Operand object or null if not exists
	 */
	public Operand get(String name)
	{
		return operandMap.get(name);
	}

	/**
	 * Returns list of operand names
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString()
    {
    	StringBuilder builder = new StringBuilder();
    	boolean firstTime = true;
		for (String name: operandMap.keySet())
		{
			if (firstTime)
				firstTime = false;
			else
				builder.append(", ");
			builder.append(name);
		}
		return builder.toString();
    }

    /**
     * Copy contents of other OperandMap
     * @param operandMap2 OperandMap object
     */
	public void putAll(OperandMap operandMap2) 
	{
		operandMap.putAll(operandMap2.operandMap);
		listMap.putAll(operandMap2.listMap);
	}

	/**
	 * Returns ItemListVariable object with specified name and index. 
	 * There is only one variable instance for any specific index.
	 * Will create object if it does not already exist.
	 * @param itemList The owner list
	 * @param name List name
	 * @param index int
	 * @param suffix To append to name
	 * @return ItemListVariable object
	 */
	protected ItemListVariable<?> getListVariable(ItemList<?> itemList, String name, int index, String suffix)
	{
		// Variable name is list name with dot index suffix
		String listVarName = name + "." + suffix;
		ItemListVariable<?> listVariable = (ItemListVariable<?>) get(listVarName);
		if (listVariable != null)
			return listVariable;
		// Use ItemList object to create new ItemListVariable instance
		listVariable = itemList.newVariableInstance(index, suffix, 0);
		return listVariable;
	}

	/**
	 * Returns a list variable for a term in an axiom in a axiom list. 
	 * Both the axiom and the term are referenced by index.
	 * There is only one variable instance for any specific index combination.
	 * @param axiomList The owner list
	 * @param name List name
	 * @param axiomIndex int
	 * @param termIndex int
	 * @param suffix To append to name
	 * @return AxiomListVariable object
	 */
	protected AxiomListVariable getListVariable(AxiomListSpec axiomListSpec) 
	{
		// Variable name is list name with dot index suffix
		String listVarName = axiomListSpec.getListName() + "." + axiomListSpec.getAxiomIndex() + "." + axiomListSpec.getSuffix();
		AxiomListVariable listVariable = (AxiomListVariable) get(listVarName);
		if (listVariable != null)
			return listVariable;
		// Use axiomList object to create new ItemListVariable instance
		listVariable = axiomListSpec.getAxiomList().newVariableInstance(axiomListSpec.getAxiomIndex(), axiomListSpec.getTermIndex(), axiomListSpec.getSuffix());
		return listVariable;
	}

	/**
	 * Returns new ItemListVariable instance. 
	 * This is wrapped in an assignment evaluator if optional expression parameter needs to be evaluated.
	 * @param itemList The list
	 * @param index List index
	 * @param expression Optional expression operand
	 * @return Operand object
	 */
	public Operand setListVariable(ItemList<?> itemList, Operand index, Operand expression) 
	{
		String name = itemList.getName();
		ItemListVariable<?> variable = newListVariableInstance(itemList, index);
		if (expression != null)
		{
			if (expression.isEmpty() || (expression instanceof Evaluator))
			    // Expression needs evaluation
				return new Evaluator(name, variable, "=", expression);
			// Expression has a value which will be assigned to the list item
			variable.assign(expression.getValue());
		}
		return variable;
	}

	/**
	 * Copy result axiom lists as iterables to supplied container
	 * @param prefix Scope name or empty if global scope
	 * @param listMap2 Container to receive lists
	 */
	public void copyLists(String prefix, Map<String, Iterable<Axiom>> listMap2) 
	{
		if (prefix == null)
			prefix = "";
		for (Entry<String, ItemList<?>> entry: listMap.entrySet())
		{
		    ItemList<?> itemList = entry.getValue();
		    if (itemList.getItemClass().equals(Axiom.class))
		    {
		        // Use fully qualified key to avoid name collisions
		        String key = prefix.isEmpty() ? entry.getKey() : (prefix + "." + entry.getKey());
		        AxiomList axiomList = (AxiomList) itemList;
		        final Iterable<AxiomTermList> axiomTermListIterble = axiomList.getIterable(); 
		        Iterable<Axiom> axiomIterable = new Iterable<Axiom>(){

                    @Override
                    public Iterator<Axiom> iterator()
                    {
                        return new Iterator<Axiom>(){
                            Iterator<AxiomTermList> axiomTermListIterator = axiomTermListIterble.iterator();
                            @Override
                            public boolean hasNext()
                            {
                                return axiomTermListIterator.hasNext();
                            }

                            @Override
                            public Axiom next()
                            {
                                return axiomTermListIterator.next().getAxiom();
                            }

                            @Override
                            public void remove()
                            {
                            }};
                    }};
		        listMap2.put(key, axiomIterable);
		    }
		}
	}

    /**
     * Copy result axioms to supplied container
     * @param prefix Scope name or empty if global scope
     * @param listMap2 Container to receive lists
     */
    public void copyAxioms(String prefix, Map<String, Axiom> axiomMap)
    {
        if (prefix == null)
            prefix = "";
        for (Entry<String, ItemList<?>> entry: listMap.entrySet())
        {
            ItemList<?> itemList = entry.getValue();
            // Create deep copy in case item list is cleared
            Axiom axiom = null;
            if (itemList.getItemClass().equals(Term.class))
            {   // AxiomTermList contains backing axiom
                AxiomTermList axiomTermList = (AxiomTermList)itemList;
                axiom = new Axiom(entry.getKey());
                Axiom source = axiomTermList.getAxiom();
                for (int i = 0; i < source.getTermCount(); i++)
                    axiom.addTerm(source.getTermByIndex(i));
            }
            else if (!itemList.getItemClass().equals(Axiom.class))
            {   // Regular ItemList contains objects which are packed into axiom to return
                axiom = new Axiom(entry.getKey());
                Iterator<?> iterator = itemList.getIterable().iterator();
                while (iterator.hasNext())
                    axiom.addTerm(new Parameter(Term.ANONYMOUS, iterator.next()));
            }
            if (axiom != null)
            {
                // Use fully qualified key to avoid name collisions
                String key = prefix.isEmpty() ? entry.getKey() : (prefix + "." + entry.getKey());
                axiomMap.put(key, axiom);
            }
        }
    }

	/**
	 * Clear result lists
	 */
	public void clearLists(List<String> listNames) 
	{
	    for (String listName: listNames)
	    {
		    ItemList<?> itemList= listMap.get(listName);
			itemList.clear();
	    }
	}

	public List<String> getEmptyListNames()
	{
	    List<String> listNames = new ArrayList<String>();
	    for (ItemList<?> itemList: listMap.values())
	        if (itemList.isEmpty())
	            listNames.add(itemList.getName());
	    return listNames;
	}

}
