/**
    Copyright (C) 2014  www.cybersearch2.com.au

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/> */
package au.com.cybersearch2.classy_logic.query;

import au.com.cybersearch2.classy_logic.expression.ExpressionException;
import au.com.cybersearch2.classy_logic.helper.EvaluationStatus;
import au.com.cybersearch2.classy_logic.interfaces.AxiomListener;
import au.com.cybersearch2.classy_logic.interfaces.SolutionFinder;
import au.com.cybersearch2.classy_logic.pattern.Axiom;
import au.com.cybersearch2.classy_logic.pattern.Choice;
import au.com.cybersearch2.classy_logic.pattern.OperandWalker;
import au.com.cybersearch2.classy_logic.pattern.SolutionPairer;
import au.com.cybersearch2.classy_logic.pattern.Template;
import au.com.cybersearch2.classy_logic.pattern.Axiom.TermPair;

/**
 * Calculator
 * Query with extra evaluation features beyond LogicQuery.
 * Because loop evalation is permitted, a 2 second timer operates to break an infinite loop.
 * @author Andrew Bowley
 * 11 Jan 2015
 */
public class Calculator implements SolutionFinder 
{
	public static int CALCULATION_TIMEOUT_SECS = 2;
	
    /** Optional Axiom to initialize calculation */
	protected Axiom axiom;
    /** Axiom listener is notified of axiom sourced each iteration */
    protected AxiomListener axiomListener;
    /** Pairs axiom terms in a Solution object with terms in a template */
    protected SolutionPairer pairer;
    /** Choice set if template.isChoice() returns true */
    protected Choice choice;
 
	/**
	 * Set choice when template is part of a choice
	 * @param choice Choice object
	 */
	public void setChoice(Choice choice) 
	{
		this.choice = choice;
	}
	
	/**
	 * Find a solution for specified template
	 * @param solution Container to aggregate results  
	 * @param template Template used on each iteration
     * @return Always true to indicate the query is resolved
	 */
	@Override
	public boolean iterate(Solution solution, Template template)
	{
		template.initialize();
		execute(template, solution);
		return true;
	}
	
	/**
	 * Find a solution for specified template and initializer axiom.
	 * Return false when choice fails to match any of the alternatives.
	 * @param axiom Initializer axiom
	 * @param solution Resolution of current query managed by QueryExecuter up to this point  
	 * @param template Template used on each iteration
     * @return Flag to indicate whether or not the query is resolved
	 */
	public boolean iterate(Axiom axiom, Solution solution, Template template)
	{
		template.initialize();
		return execute(axiom, template, solution);
	}
	
	/**
	 * Set axiom listener to receive each solution as it is produced
	 * @param axiomListener The axiom listener object
	 */
	@Override
	public void setAxiomListener(AxiomListener axiomListener) 
	{
		this.axiomListener = axiomListener;
	}

	/**
	 * Execute calculation using specified solution and template.
	 * An axiom to seed the calculation is optional, but it will be set if this is called from iterate().
	 * @param template Template used on each iteration
	 * @param solution Container to aggregate results  
	 */
	public void execute(Template template, Solution solution)
	{
		execute(null, template, solution);
	}
	
	/**
	 * Execute calculation using specified solution and template.
	 * An axiom to seed the calculation is optional, but it will be set if this is called from iterate().
	 * @param solution Container to aggregate results  
	 * @param template Template used on each iteration
     * @return Flag to indicate whether or not the query is resolved
	 */
	public boolean execute(Axiom seedAxiom, Template template, Solution solution)
	{
		if (seedAxiom != null) 
		{
			if (!seedAxiom.getName().equals(template.getKey()))
				throw new QueryExecutionException("Axiom key \"" + axiom.getName() + "\" does not match Template key \"" + template.getKey() + "\"");
			axiom = seedAxiom;
		}
		else
		    axiom = solution.getAxiom(template.getKey());
		boolean unificationSuccess = true;
		if (axiom != null)
		{
			if (!axiom.unifyTemplate(template, solution))
				unificationSuccess = false;
			else
			{   // Unify enclosed templates which will participate in ensuing evaluation
				Template chainTemplate = template.getNext();
				while (chainTemplate != null)
				{
					axiom.unifyTemplate(chainTemplate, solution);
					chainTemplate = chainTemplate.getNext();
				}
			}
		}
		else 
            unifySolution(solution, template);
		if (unificationSuccess)
		{
			if (completeSolution(solution, template))
			{
			    if (template.isReplicate())
		            template.backup(true);
			    return true;
			}
            template.backup(true);
		}
		// Short circuit when solution not available
		return false;
	}

	/**
	 * Complete finding solution following successful unification
	 * @param solution Container to aggregate results  
	 * @param template Template used on each iteration
	 * @return Flag to indicate if the query is resolved
	 */
	protected boolean completeSolution(Solution solution, Template template)
	{
		EvaluationStatus evaluationStatus = EvaluationStatus.SHORT_CIRCUIT;
		try
		{
			// evaluate() may result in a short circuit exit flagged by returning false
			// isfact() flags true if each term of the template is non-empty
			if (choice == null)
			{
				evaluationStatus = template.evaluate();
				if (evaluationStatus == EvaluationStatus.COMPLETE)
				{
					axiom = template.toAxiom();
					solution.put(template.getQualifiedName().toString(), axiom);
					return true;
				}
				else
				    return evaluationStatus != EvaluationStatus.SKIP;
			}
			else
				return choice.completeSolution(solution, template, axiom);
		}
		catch (ExpressionException e)
		{   // evaluate() exceptions are thrown by Evaluator objects 
			throw new QueryExecutionException("Error evaluating: " + template.toString(), e);
		}
	}
	
	/**
	 * Unify template with solution.
	 * @param solution Container to aggregate results  
	 * @param template Structure to pair with axiom sequence
	 * @return Flag to indicate if the query is resolved
	 */
	protected boolean unifySolution(Solution solution, Template template)
    {
		if (solution.size() > 0)
		{
			if (pairer == null)
				pairer = new SolutionPairer(solution, template.getQualifiedName());
			else
				pairer.setSolution(solution);
			Template chainTemplate = template;
			while (chainTemplate != null)
			{
				OperandWalker walker = template.getOperandWalker();
				if (walker.visitAllNodes(pairer))
				{
					// Proceed with unification term by term
					for (TermPair termPair: pairer.getPairList())
						termPair.term1.unifyTerm(termPair.term2, chainTemplate.getId());
					return true;
				}
				chainTemplate = chainTemplate.getNext();
			}
		}
		return false;
    }

}
