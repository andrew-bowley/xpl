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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * YearPercent
 * @author Andrew Bowley
 * 10 Feb 2015
 */
@Entity(name = "YearPercent")
public class YearPercent 
{

	public YearPercent() 
	{
	}

	/** This id is generated by the database and set on the object when it is passed to the create method */
    @Id @GeneratedValue
 	int id;

    @OneToOne
    @JoinColumn(name="country_id", referencedColumnName="id")
	Country country;
	
	@Column(nullable = false)
	String year;
	
	@Column(nullable = false)
	Double percent;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public Double getPercent() {
		return percent;
	}

	public void setPercent(Double percent) {
		this.percent = percent;
	}
}