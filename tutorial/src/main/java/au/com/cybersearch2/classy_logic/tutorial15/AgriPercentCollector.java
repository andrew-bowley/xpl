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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.persistence.Query;

import au.com.cybersearch2.classy_logic.jpa.JpaEntityCollector;
import au.com.cybersearch2.classy_logic.jpa.QueryForAllGenerator;
import au.com.cybersearch2.classybean.BeanMap;
import au.com.cybersearch2.classyinject.DI;
import au.com.cybersearch2.classyjpa.EntityManagerLite;
import au.com.cybersearch2.classyjpa.persist.Persistence;
import au.com.cybersearch2.classyjpa.persist.PersistenceAdmin;
import au.com.cybersearch2.classyjpa.persist.PersistenceFactory;

/**
 * AgriPercentCollector extends JpaEntityCollector to create an external axiom source
 * for   axioms translated from    JPA entity objects. The data is
 * obtained from "" named query.

 * @author Andrew Bowley
 * 10 Feb 2015
 */
public class AgriPercentCollector extends JpaEntityCollector 
{
	
    /** Named query to find the percent change in agriculture land for all years */
    static public final String ALL_YEAR_PERCENTS = "all_year_percents";

    protected YearPercent yearPercent;
    protected Data fact;
    protected BeanMap beanMap;
    protected String currentCountry = "";
    
    /** Factory object to create "agriculture" Persistence Unit implementation */
    @Inject PersistenceFactory persistenceFactory;

	/**
	 * 
	 */
	public AgriPercentCollector(String persistenceUnit) 
	{
		super(persistenceUnit);
		this.namedJpaQuery = ALL_YEAR_PERCENTS;
        // Inject persistenceFactory
        DI.inject(this); 
		setUp(persistenceUnit);
	}

	@Override
	public void doInBackground(EntityManagerLite entityManager) 
	{
		// Collect all year percent items 
        Query query = entityManager.createNamedQuery(namedJpaQuery);
        if (maxResults > 0)
        {
        	query.setMaxResults(maxResults);
        	query.setFirstResult(startPosition);
        }
        @SuppressWarnings({"unchecked"})
		Collection<YearPercent> yearPercentList = (Collection<YearPercent>) query.getResultList();
        startPosition += yearPercentList.size();
		//System.out.println("Size = " + yearPercentList.size() + ", Position = " + startPosition);
        // Collate into country list
        Iterator<YearPercent> iterator = yearPercentList.iterator();
        if (!iterator.hasNext())
        {
        	if (moreExpected)
        	{
        		moreExpected = false;
    			if (data == null)
    				data = new ArrayList<Object>();
    			data.add(fact);
         	}
   			return;
        }
        while (iterator.hasNext())
        {
        	yearPercent = iterator.next();
        	String year = yearPercent.getYear();
        	String country = yearPercent.getCountry().getCountry();
        	if (!currentCountry.equals(country))
        	{
        		currentCountry = country;
        		if (fact != null)
        		{
        			if (data == null)
        				data = new ArrayList<Object>();
        			data.add(fact);
        		}
        		fact = new Data();
        		fact.setCountry(country);
        		beanMap = new BeanMap(fact);
        	}
        	beanMap.put(year, yearPercent.getPercent());
        }
    	moreExpected = true;
	}

	protected void setUp(String persistenceUnit)
	{
        Persistence persistence = persistenceFactory.getPersistenceUnit(persistenceUnit);
        // Get Interface for JPA Support, required to create named queries
        PersistenceAdmin persistenceAdmin = persistence.getPersistenceAdmin();
        QueryForAllGenerator allEntitiesQuery = 
                new QueryForAllGenerator(persistenceAdmin);
        persistenceAdmin.addNamedQuery(YearPercent.class, ALL_YEAR_PERCENTS, allEntitiesQuery);
	}
}
