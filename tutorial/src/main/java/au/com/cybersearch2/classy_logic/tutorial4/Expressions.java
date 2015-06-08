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
package au.com.cybersearch2.classy_logic.tutorial4;

import au.com.cybersearch2.classy_logic.QueryProgram;
import au.com.cybersearch2.classy_logic.expression.ExpressionException;
import au.com.cybersearch2.classy_logic.interfaces.SolutionHandler;
import au.com.cybersearch2.classy_logic.pattern.Axiom;
import au.com.cybersearch2.classy_logic.query.QueryExecutionException;
import au.com.cybersearch2.classy_logic.query.Solution;

/**
 * Expressions
 * @author Andrew Bowley
 * 23 Feb 2015
 */
public class Expressions 
{
	static final String EXPRESSIONS =
       "integer x = 1;\n" +
       "integer y = 2;\n" +
       "axiom to_prove(can_evaluate) : (false);\n" +
       "template evaluate(\n" +
       "  boolean can_add = x + y == 3,\n" +
       "  boolean can_subtract = y - x == 1,\n" +
       "  boolean can_multiply = x * y == 2,\n" +
       "  boolean can_divide = 6 / y == 3,\n" +
       "  boolean can_override_precedence = (y + 1) * 2 > x * 5,\n" +
       "  boolean can_assign = (y *= 3) == 6 && y == 6,\n" +
       "  boolean can_evaluate = can_add && can_subtract && can_multiply && can_divide && can_override_precedence && can_assign\n" +
        ");\n" +
       "query expressions (to_prove:evaluate);";

	public void displayEvaluations()
	{
		QueryProgram queryProgram = new QueryProgram(EXPRESSIONS);
		queryProgram.executeQuery("expressions", new SolutionHandler(){
			@Override
			public boolean onSolution(Solution solution) {
				Axiom evaluateAxiom = solution.getAxiom("evaluate");
					System.out.println(evaluateAxiom.toString());
				return true;
			}});
	}
	
	public static void main(String[] args)
	{
		try 
		{
	        Expressions expressions = new Expressions();
			expressions.displayEvaluations();
		} 
		catch (ExpressionException e) 
		{
			e.printStackTrace();
			System.exit(1);
		}
        catch (QueryExecutionException e) 
        {
            e.printStackTrace();
            System.exit(1);
        }
		System.exit(0);
	}
}
