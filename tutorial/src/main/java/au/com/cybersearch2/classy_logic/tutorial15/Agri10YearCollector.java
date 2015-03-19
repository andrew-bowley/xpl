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
package au.com.cybersearch2.classy_logic.tutorial15;

import java.util.Collection;

import javax.inject.Inject;
import javax.persistence.Query;

import au.com.cybersearch2.classy_logic.jpa.JpaEntityCollector;
import au.com.cybersearch2.classy_logic.jpa.QueryForAllGenerator;
import au.com.cybersearch2.classyinject.DI;
import au.com.cybersearch2.classyjpa.EntityManagerLite;
import au.com.cybersearch2.classyjpa.persist.Persistence;
import au.com.cybersearch2.classyjpa.persist.PersistenceAdmin;
import au.com.cybersearch2.classyjpa.persist.PersistenceFactory;

/**
 * Agri10YearCollector extends JpaEntityCollector to create an external axiom source
 * for Agri10Year axioms translated from Agri10Year JPA entity objects. The data is
 * obtained from "all_agri_10_year" named query.
 * @author Andrew Bowley
 * 10 Feb 2015
 */
public class Agri10YearCollector extends JpaEntityCollector 
{
    /** Named query to find all cities */
    static public final String ALL_AGRI_10_YEAR = "all_agri_10_year";

    /** Factory object to create "cities" Persistence Unit implementation */
    @Inject PersistenceFactory persistenceFactory;

    /**
     * Construct a Agri10YearCollector object
     * @param persistenceUnit
     */
	public Agri10YearCollector(String persistenceUnit) 
	{
		super(persistenceUnit);
		// JpaEntityCollector needs the name of the query to fetch all cities 
		this.namedJpaQuery = ALL_AGRI_10_YEAR;
        // Inject persistenceFactory
        DI.inject(this); 
		setUp(persistenceUnit);
	}

	/**
	 * Set up the named query which uses utility class QueryForAllGenerator
	 * @param persistenceUnit
	 */
	protected void setUp(String persistenceUnit)
	{
        Persistence persistence = persistenceFactory.getPersistenceUnit(persistenceUnit);
        // Get Interface for JPA Support, required to create named queries
        PersistenceAdmin persistenceAdmin = persistence.getPersistenceAdmin();
        QueryForAllGenerator allEntitiesQuery = 
                new QueryForAllGenerator(persistenceAdmin);
        persistenceAdmin.addNamedQuery(Agri10Year.class, ALL_AGRI_10_YEAR, allEntitiesQuery);
	}

    /**
     * Get data using JPA entity manager
     * @see au.com.cybersearch2.classyjpa.entity.PersistenceWork#doInBackground(au.com.cybersearch2.classyjpa.EntityManagerLite)
     */
	@SuppressWarnings("unchecked")
	@Override
	public void doInBackground(EntityManagerLite entityManager) 
	{
        Query query = entityManager.createNamedQuery(namedJpaQuery);
        if (maxResults > 0)
        {   // Paging enabled
        	query.setMaxResults(maxResults);
        	query.setFirstResult(startPosition);
        }
        data = (Collection<Object>) query.getResultList();
        if (maxResults > 0)
        {   // Advance start position or 
        	// clear "moreExpected" flag if no more results avaliable
        	if (data.size() > 0)
        	{
        		startPosition += data.size();
        		moreExpected = true;
        	}
        	else
        		moreExpected = false;
        }
	}

}
