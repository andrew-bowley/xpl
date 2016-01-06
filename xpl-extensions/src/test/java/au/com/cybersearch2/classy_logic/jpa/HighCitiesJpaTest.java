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
package au.com.cybersearch2.classy_logic.jpa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

import au.com.cybersearch2.classy_logic.compile.ParserAssembler;
import au.com.cybersearch2.classy_logic.compile.ParserResources;
import au.com.cybersearch2.classy_logic.jpa.JpaSource;
import au.com.cybersearch2.classy_logic.pattern.Axiom;
import au.com.cybersearch2.classydb.DatabaseAdminImpl;
import au.com.cybersearch2.classydb.NativeScriptDatabaseWork;
import au.com.cybersearch2.classyinject.ApplicationModule;
import au.com.cybersearch2.classyinject.DI;
import au.com.cybersearch2.classyjpa.EntityManagerLite;
import au.com.cybersearch2.classyjpa.entity.PersistenceContainer;
import au.com.cybersearch2.classyjpa.entity.PersistenceWork;
import au.com.cybersearch2.classyjpa.persist.PersistenceContext;
import au.com.cybersearch2.classyjpa.persist.PersistenceFactory;
import au.com.cybersearch2.classytask.WorkerRunnable;
import dagger.Component;

/**
 * HighCitiesJpaTest
 * @author Andrew Bowley
 * 8 Feb 2015
 */
public class HighCitiesJpaTest 
{
    @Singleton
    @Component(modules = HighCitiesModule.class)  
    static interface ApplicationComponent extends ApplicationModule
    {
        void inject(CityCollector cityCollector);
        void inject(ParserAssembler.ExternalAxiomSource externalAxiomSource);
        void inject(ParserResources parserResources);
        void inject(WorkerRunnable<Boolean> workerRunnable);
        void inject(PersistenceContext persistenceContext);
        void inject(PersistenceFactory persistenceFactory);
        void inject(DatabaseAdminImpl databaseAdminImpl);
        void inject(NativeScriptDatabaseWork nativeScriptDatabaseWork);
    }

   /** Named query to find all cities */
    static public final String ALL_CITIES = "all_cities";
    /** Persistence Unit name to look up configuration details in persistence.xml */
    static public final String PU_NAME = "cities";
    
    static public final String[] CITY_AXIOMS =
	{
    	"city(id = 1, altitude = 1718, name = bilene)",
    	"city(id = 2, altitude = 8000, name = addis ababa)",
    	"city(id = 3, altitude = 5280, name = denver)",
    	"city(id = 4, altitude = 6970, name = flagstaff)",
    	"city(id = 5, altitude = 8, name = jacksonville)",
    	"city(id = 6, altitude = 10200, name = leadville)",
    	"city(id = 7, altitude = 1305, name = madrid)",
    	"city(id = 8, altitude = 19, name = richmond)",
    	"city(id = 9, altitude = 1909, name = spokane)",
    	"city(id = 10, altitude = 1305, name = wichita)"
   	};

    static public final String[] CITY_AXIOMS2 =
	{
		"city(Name = bilene, Altitude = 1718)",
		"city(Name = addis ababa, Altitude = 8000)",
		"city(Name = denver, Altitude = 5280)",
		"city(Name = flagstaff, Altitude = 6970)",
		"city(Name = jacksonville, Altitude = 8)",
		"city(Name = leadville, Altitude = 10200)",
		"city(Name = madrid, Altitude = 1305)",
		"city(Name = richmond, Altitude = 19)",
		"city(Name = spokane, Altitude = 1909)",
		"city(Name = wichita, Altitude = 1305)"
	};

    @Before
    public void setUp() throws InterruptedException 
    {
        // Set up dependency injection
        ApplicationComponent component = 
                DaggerHighCitiesJpaTest_ApplicationComponent.builder()
                .highCitiesModule(new HighCitiesModule())
                .build();
        DI.getInstance(component);
        PersistenceWork setUpWork = new PersistenceWork(){

            @Override
            public void doTask(EntityManagerLite entityManager)
            {
            	entityManager.persist(new City("bilene", 1718));
            	entityManager.persist(new City("addis ababa", 8000));
            	entityManager.persist(new City("denver", 5280));
            	entityManager.persist(new City("flagstaff", 6970));
            	entityManager.persist(new City("jacksonville", 8));
            	entityManager.persist(new City("leadville", 10200));
            	entityManager.persist(new City("madrid", 1305));
            	entityManager.persist(new City("richmond",19));
            	entityManager.persist(new City("spokane", 1909));
            	entityManager.persist(new City("wichita", 1305));
                // Database updates commited upon exit
            }

            @Override
            public void onPostExecute(boolean success)
            {
                if (!success)
                    throw new IllegalStateException("Database set up failed. Check console for error details.");
            }

            @Override
            public void onRollback(Throwable rollbackException)
            {
                throw new IllegalStateException("Database set up failed. Check console for stack trace.", rollbackException);
            }
        };
        // Execute work and wait synchronously for completion
        PersistenceContainer container = new PersistenceContainer(PU_NAME);
        container.executeTask(setUpWork).waitForTask();
    }

    @Test
    public void test_query() throws Exception
    {
    	CityCollector cityCollector = new CityCollector(PU_NAME);
    	JpaSource jpaSource = new JpaSource(cityCollector, "city");
    	Iterator<Axiom> axiomIterator = jpaSource.iterator();
    	int next = 0;
    	while (axiomIterator.hasNext())
    		//System.out.println(axiomIterator.next().toString());
    		assertThat(CITY_AXIOMS[next++]).isEqualTo(axiomIterator.next().toString());
    }
   
    @Test
    public void test_query_term_names() throws Exception
    {
    	CityCollector cityCollector = new CityCollector(PU_NAME);
    	List<NameMap> termNameList = new ArrayList<NameMap>();
    	termNameList.add(new NameMap("Name", "name"));
    	termNameList.add(new NameMap("Altitude", "altitude"));
    	JpaSource jpaSource = new JpaSource(cityCollector, "city", termNameList); 
    	Iterator<Axiom> axiomIterator = jpaSource.iterator();
    	int next = 0;
    	while (axiomIterator.hasNext())
    		//System.out.println(axiomIterator.next().toString());
    		assertThat(CITY_AXIOMS2[next++]).isEqualTo(axiomIterator.next().toString());
    }
    
}
