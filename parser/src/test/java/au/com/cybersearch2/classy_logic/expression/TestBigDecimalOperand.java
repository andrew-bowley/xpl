/**
    Copyright (C) 2015  www.cybersearch2.com.au

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
package au.com.cybersearch2.classy_logic.expression;

import java.math.BigDecimal;

import au.com.cybersearch2.classy_logic.helper.QualifiedName;
import au.com.cybersearch2.classy_logic.interfaces.Operand;

/**
 * TestBigDecimalOperand
 * @author Andrew Bowley
 * 24 Aug 2015
 */
public class TestBigDecimalOperand extends BigDecimalOperand
{

    /**
     * @param QualifiedName.parseName(name)
     */
    public TestBigDecimalOperand(String name)
    {
        super(QualifiedName.parseName(name));

    }

    /**
     * @param QualifiedName.parseName(name)
     * @param value
     */
    public TestBigDecimalOperand(String name, BigDecimal value)
    {
        super(QualifiedName.parseName(name), value);

    }

    /**
     * @param QualifiedName.parseName(name)
     * @param expression
     */
    public TestBigDecimalOperand(String name, Operand expression)
    {
        super(QualifiedName.parseName(name), expression);

    }

}
