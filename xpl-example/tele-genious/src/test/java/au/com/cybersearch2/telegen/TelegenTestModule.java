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
package au.com.cybersearch2.telegen;

import javax.inject.Singleton;

import au.com.cybersearch2.classy_logic.JpaProviderHelper;
import au.com.cybersearch2.classy_logic.ProviderManager;
import au.com.cybersearch2.classyapp.JavaTestResourceEnvironment;
import au.com.cybersearch2.classyapp.ResourceEnvironment;
import au.com.cybersearch2.classydb.DatabaseSupport.ConnectionType;
import au.com.cybersearch2.classydb.SQLiteDatabaseSupport;
import au.com.cybersearch2.classyinject.ApplicationModule;
import au.com.cybersearch2.classyjpa.persist.PersistenceContext;
import au.com.cybersearch2.classyjpa.persist.PersistenceFactory;
import dagger.Module;
import dagger.Provides;

/**
 * TelegenTestModule
 * @author Andrew Bowley
 * 22 May 2015
 */
@Module(/*injects = { 
        TelegenTest.class,
        ParserResources.class,
        ParserAssembler.ExternalAxiomSource.class,
        JpaEntityCollector.class,
        PersistenceFactory.class,
        NativeScriptDatabaseWork.class,
        PersistenceContext.class,
        DatabaseAdminImpl.class
        }*/)
public class TelegenTestModule implements ApplicationModule
{

    /**
     * TelegenTestModule
     */
    public TelegenTestModule()
    {
    }

    @Provides @Singleton ResourceEnvironment provideResourceEnvironment()
    {
        return new JavaTestResourceEnvironment("src/main/resources");
    }

    @Provides @Singleton PersistenceFactory providePersistenceModule()
    {
        return new PersistenceFactory(new SQLiteDatabaseSupport(ConnectionType.memory));
    }

    @Provides @Singleton PersistenceContext providesPersistenceContext()
    {
        return new PersistenceContext();
    }

    @Provides @Singleton ProviderManager provideProviderManager()
    {
        return new ProviderManager();
    }
    
    @Provides @Singleton JpaProviderHelper provideJpaProviderHelper()
    {
        return new JpaProviderHelper();
    }
}
