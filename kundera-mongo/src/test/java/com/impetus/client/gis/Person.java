/**
 * Copyright 2012 Impetus Infotech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.impetus.client.gis;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.impetus.kundera.gis.geometry.Point;
import com.impetus.kundera.index.Index;
import com.impetus.kundera.index.IndexCollection;

/**
 * Person Entity Class
 * 
 * @author amresh.singh
 */

@Entity
@Table(name = "PERSON_LOCATION", schema = "KunderaExamples@mongoTest")
@IndexCollection(columns = { @Index(name = "currentLocation", type = "GEO2D") })
public class Person {
	@Id
	@Column(name = "PERSON_ID")
	private int personId;

	@Column(name = "PERSON_NAME")
	private String name;

	@Column(name = "CURRENT_LOCATION")
	private Point currentLocation;

	@Embedded
	private Vehicle vehicle;

	/**
	 * @return the personId
	 */
	public int getPersonId() {
		return personId;
	}

	/**
	 * @param personId
	 *            the personId to set
	 */
	public void setPersonId(int personId) {
		this.personId = personId;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the currentLocation
	 */
	public Point getCurrentLocation() {
		return currentLocation;
	}

	/**
	 * @param currentLocation
	 *            the currentLocation to set
	 */
	public void setCurrentLocation(Point currentLocation) {
		this.currentLocation = currentLocation;
	}

	/**
	 * @return the vehicle
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}

	/**
	 * @param vehicle
	 *            the vehicle to set
	 */
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

}
