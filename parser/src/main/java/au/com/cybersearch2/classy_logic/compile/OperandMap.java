/**
 * 
 */
package au.com.cybersearch2.classy_logic.compile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import au.com.cybersearch2.classy_logic.list.AxiomListVariable;
import au.com.cybersearch2.classy_logic.list.AxiomTermList;
import au.com.cybersearch2.classy_logic.list.ItemListVariable;



/**
 * OperandMap
 * References Operand and ItemList by name and supports parser object collection
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

	public void setOperandValues(Map<Operand, Object> operandValueMap)
	{
		for (Entry<Operand, Object> entry: operandValueMap.entrySet())
			if (entry.getValue() instanceof Null)
				((Parameter)entry.getKey()).clearValue();
			else
			   entry.getKey().assign(entry.getValue());
		for (Entry<String, Operand> entry: operandMap.entrySet())
		{
			if (!operandValueMap.containsKey(entry.getValue()))
				((Parameter)entry.getValue()).clearValue();
		}
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
		operandMap.put(operand.getName(), operand);
    }

	public boolean hasOperand(String name)
	{
		return operandMap.containsKey(name);
	}
	
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
	 * @return ItemList object
	 */
	public ItemList<?> getItemList(String name)
	{
		checkListName(name);
		return listMap.get(name);
	}
	
	/**
	 * Returns ItemListVariable object with specified name and index expression operand. 
	 * Will create object if it does not already exist.
	 * @param name List name
	 * @param expression Index expression operand
	 * @return ItemListVariable object
	 */
	public ItemListVariable<?> newListVariableInstance(String name, Operand expression)
	{
		checkListName(name);
		ItemList<?> itemList = listMap.get(name);
		ItemListVariable<?> listVariable = null;
		int index = -1;
		String suffix = null;
		if (!expression.isEmpty() && (expression instanceof IntegerOperand) && Term.ANONYMOUS.equals(expression.getName()))
	    {
		   index = ((Integer)(expression.getValue())).intValue();
		   suffix = Integer.toString(index);
		}
		else if (expression.isEmpty() && (expression instanceof Variable))
		{
			if (itemList instanceof AxiomTermList)
			{
				AxiomTermList axiomTermList = (AxiomTermList)itemList;
				List<String> axiomTermNameList = axiomTermList.getAxiomTermNameList();
				if (axiomTermNameList == null)
					throw new ExpressionException("List \"" + name + "\" cannot be referenced by name");
				suffix = expression.getName();
				index = getIndexForName(name, suffix, axiomTermNameList);
			}
			else // Interpret identifier as a variable name for any primitive list
			{
				expression = new Variable(expression.getValue().toString());
				suffix = expression.getName();
			}
		}
		if (suffix == null)
			suffix = expression.getName().isEmpty() ? expression.toString() : expression.getName();
		// IntegerOperand value is treated as fixed
		if (index >= 0)
			listVariable = getListVariable(itemList, name, index, suffix);
		else
			listVariable = itemList.newVariableInstance(expression, suffix);
		return listVariable;
	}

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
	 * @param name List name
	 * @param expression1 Index expression operand of axiom
	 * @param expression2 Index expression operand 0f term
	 * @return AxiomListVariable object
	 */

	public AxiomListVariable newListVariableInstance(String name, Operand expression1,
			Operand expression2) 
	{
		checkListName(name);
		AxiomList axiomList = (AxiomList) listMap.get(name);
		AxiomListVariable listVariable = null;
		int axiomIndex = -1;
		int termIndex = -1;
		String suffix = null;
		// Check for non-empty Integer operand, which is used for a fixed index
		if (!expression1.isEmpty() && (expression1 instanceof IntegerOperand && Term.ANONYMOUS.equals(expression1.getName())))
			axiomIndex = ((Integer)(expression1.getValue())).intValue();
		if (!expression2.isEmpty() && (expression2 instanceof IntegerOperand) && Term.ANONYMOUS.equals(expression2.getName()))
		{
			termIndex = ((Integer)(expression2.getValue())).intValue();
			suffix = Integer.toString(termIndex);
		}
		else if (expression2.isEmpty() && (expression2 instanceof Variable))
		{
			List<String> axiomTermNameList = axiomList.getAxiomTermNameList();
			if (axiomTermNameList == null)
				throw new ExpressionException("List \"" + name + "\" cannot be referenced by name");
			suffix = expression2.getName();
			termIndex = getIndexForName(name, suffix, axiomTermNameList);
		}
		else
			suffix = expression2.toString();
		// IntegerOperand value is treated as fixed
		if ((axiomIndex >= 0) && (termIndex >= 0))
			// The variable only has a single instance if both axiom and term indexes are fixed
			listVariable = getListVariable(axiomList, name, axiomIndex, termIndex, suffix);
		else
		{
			if ((axiomIndex >= 0))
				listVariable = axiomList.newVariableInstance(axiomIndex, expression2, suffix);
			else if (termIndex >= 0)
				listVariable = axiomList.newVariableInstance(expression1, termIndex, suffix);
			else
				listVariable = axiomList.newVariableInstance(expression1, expression2, suffix);
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
	 * Throw expection if list name not found in list set
	 * @param name
	 * @throws ExpressionException
	 */
	protected void checkListName(String name)
	{
		if (!listMap.containsKey(name))
			throw new ExpressionException("List not found with name \"" + name + "\"");
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
		listVariable = itemList.newVariableInstance(index, suffix);
		operandMap.put(listVarName, listVariable);
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
	protected AxiomListVariable getListVariable(AxiomList axiomList, String name, int axiomIndex,
			int termIndex, String suffix) 
	{
		// Variable name is list name with dot index suffix
		String listVarName = name + "." + axiomIndex + "." + suffix;
		AxiomListVariable listVariable = (AxiomListVariable) get(listVarName);
		if (listVariable != null)
			return listVariable;
		// Use axiomList object to create new ItemListVariable instance
		listVariable = axiomList.newVariableInstance(axiomIndex, termIndex, suffix);
		operandMap.put(listVarName, listVariable);
		return listVariable;
	}

	public Operand setListVariable(String name, Operand index, Operand expression) 
	{
		checkListName(name);
		ItemListVariable<?> variable = newListVariableInstance(name, index);
		if (expression != null)
		{
			if (expression.isEmpty() || (expression instanceof Evaluator))
				return new Evaluator(name, variable, "=", expression);
			variable.assign(expression.getValue());
		}
		return variable;
	}

	public void copyLists(String prefix, Map<String, Iterable<?>> listMap2) 
	{
		if (prefix == null)
			prefix = "";
		for (Entry<String, ItemList<?>> entry: listMap.entrySet())
		{
			String key = prefix.isEmpty() ? entry.getKey() : (prefix + "." + entry.getKey());
			listMap2.put(key, entry.getValue().getIterable());
		}
	}

	public void clearLists() 
	{
		for (ItemList<?> itemList: listMap.values())
			itemList.clear();
	}

}
