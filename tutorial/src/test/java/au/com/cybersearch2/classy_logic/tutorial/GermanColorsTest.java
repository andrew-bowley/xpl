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
package au.com.cybersearch2.classy_logic.tutorial;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

import au.com.cybersearch2.classy_logic.tutorial13.GermanColors;

/**
 * GermanColorsTest
 * @author Andrew Bowley
 * 5 Jun 2015
 */
public class GermanColorsTest
{
    @Test
    public void GermanColors() throws Exception
    {
        GermanColors germanColors = new GermanColors();
        assertThat(germanColors.getColorSwatch("Wasser")).isEqualTo("swatch(name = Wasser, red = 0, green = 255, blue = 255)");
        assertThat(germanColors.getColorSwatch("schwarz")).isEqualTo("swatch(name = schwarz, red = 0, green = 0, blue = 0)");
        assertThat(germanColors.getColorSwatch("weiß")).isEqualTo("swatch(name = weiß, red = 255, green = 255, blue = 255)");
        assertThat(germanColors.getColorSwatch("blau")).isEqualTo("swatch(name = blau, red = 0, green = 0, blue = 255)");
    }
}
