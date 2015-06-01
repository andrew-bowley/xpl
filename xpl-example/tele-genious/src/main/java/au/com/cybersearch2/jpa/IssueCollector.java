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
package au.com.cybersearch2.jpa;

import au.com.cybersearch2.classy_logic.jpa.JpaEntityCollector;
import au.com.cybersearch2.classy_logic.jpa.QueryForAllGenerator;
import au.com.cybersearch2.classyjpa.persist.Persistence;
import au.com.cybersearch2.classyjpa.persist.PersistenceAdmin;
import au.com.cybersearch2.classyjpa.persist.PersistenceContext;
import au.com.cybersearch2.entity.Issue;

/**
 * IssueCollector extends JpaEntityCollector to create an external axiom source
 * for Issue axioms translated from Issue JPA entity objects. The data is
 * obtained from "all_cities" named query.
 * @author Andrew Bowley
 * 10 Feb 2015
 */
public class IssueCollector extends JpaEntityCollector 
{
    /** Named query to find all cities */
    static public final String ALL_ISSUES = "all_issues";

    /** Factory object to create "cities" Persistence Unit implementation */
    protected PersistenceContext persistenceContext;

    /**
     * Construct a IssueCollector object
     * @param persistenceUnit
     */
	public IssueCollector(String persistenceUnit) 
	{
		super(persistenceUnit);
		// JpaEntityCollector needs the name of the query to fetch all cities 
		this.namedJpaQuery = ALL_ISSUES;
        // Inject persistenceContext
        persistenceContext = new PersistenceContext();
		setUp(persistenceUnit);
	}

	/**
	 * Set up the named query which uses utility class QueryForAllGenerator
	 * @param persistenceUnit
	 */
	protected void setUp(String persistenceUnit)
	{
        Persistence persistence = persistenceContext.getPersistenceUnit(persistenceUnit);
        // Get Interface for JPA Support, required to create named queries
        PersistenceAdmin persistenceAdmin = persistence.getPersistenceAdmin();
        QueryForAllGenerator allEntitiesQuery = 
                new QueryForAllGenerator(persistenceAdmin);
        persistenceAdmin.addNamedQuery(Issue.class, ALL_ISSUES, allEntitiesQuery);
	}
}
