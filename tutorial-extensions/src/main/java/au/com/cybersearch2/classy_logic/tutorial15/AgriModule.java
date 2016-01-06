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

import javax.inject.Singleton;

import au.com.cybersearch2.classy_logic.JpaProviderHelper;
import au.com.cybersearch2.classy_logic.TestModule;
import au.com.cybersearch2.classyinject.ApplicationModule;
import dagger.Module;
import dagger.Provides;

/**
 * CitiesModule is the PersistenceHighCities dependency inject configuration
 * @author Andrew Bowley
 * 9 Dec 2014
 */
@Module(/*injects= 
{
		IncreasedAgriculture.class,
        AgriPercentCollector.class, 
		JpaEntityCollector.class,
		AgriAxiomProvider.class
},*/
includes=TestModule.class)
public class AgriModule implements ApplicationModule 
{
    
    @Provides @Singleton JpaProviderHelper provideJpaProviderHelper()
    {
        return new JpaProviderHelper();
    }
}
