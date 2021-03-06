/*
 * Copyright (c) 2013 LDBC
 * Linked Data Benchmark Council (http://ldbc.eu)
 *
 * This file is part of ldbc_socialnet_dbgen.
 *
 * ldbc_socialnet_dbgen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ldbc_socialnet_dbgen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ldbc_socialnet_dbgen.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2011 OpenLink Software <bdsmt@openlinksw.com>
 * All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation;  only Version 2 of the License dated
 * June 1991.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ldbc.socialnet.dbgen.dictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import ldbc.socialnet.dbgen.util.RandomGeneratorFarm;

public class UniversityDictionary {
    
    private static final String SEPARATOR = "  ";
    
    String dicFileName; 

    TreeMap<Long,String> universityName;
	TreeMap<Long, Integer> universityToLocation;
	TreeMap<Integer, Vector<Long>> universitiesByLocation;
	
	double probTopUniv; 
	double probUncorrelatedUniversity;
	LocationDictionary locationDic;
    long startIndex = 0;

	public UniversityDictionary(String dicFileName, LocationDictionary locationDic, 
									 double probUncorrelatedUniversity, 
									 double probTopUni, int startIndex){
		this.dicFileName = dicFileName;
		this.probTopUniv = probTopUni;
		this.locationDic = locationDic;
		this.probUncorrelatedUniversity = probUncorrelatedUniversity;
        this.startIndex = startIndex;
	}
	
	public void init(){
        universityName = new TreeMap<Long,String>();
	    universityToLocation = new TreeMap<Long, Integer>();
	    universitiesByLocation = new TreeMap<Integer, Vector<Long>>();
	    for (Integer id : locationDic.getCountries()){
	        universitiesByLocation.put(id, new Vector<Long>());
	    }
	    extractUniversityNames();
	}
	
	public int getUniversityLocation( long university ) {
	    return universityToLocation.get(university);
	}
	
	public void extractUniversityNames() {
		try {
		    BufferedReader dicAllInstitutes = new BufferedReader(
		            new InputStreamReader(getClass( ).getResourceAsStream(dicFileName), "UTF-8"));
		    
		    String line;
		    int curLocationId = -1; 
		    String lastLocationName = "";
            long totalNumUniversities = startIndex;
			while ((line = dicAllInstitutes.readLine()) != null){
				String data[] = line.split(SEPARATOR);
				String locationName = data[0];
                String cityName = data[2];
                if (locationDic.getCountryId(locationName) != LocationDictionary.INVALID_LOCATION &&
                        locationDic.getCityId(cityName) != LocationDictionary.INVALID_LOCATION ) {
                    curLocationId = locationDic.getCountryId(locationName);
                    String universityName = data[1].trim();
                    universitiesByLocation.get(curLocationId).add(totalNumUniversities);
                    Integer cityId = locationDic.getCityId(cityName);
                    universityToLocation.put(totalNumUniversities, cityId);
                    this.universityName.put(totalNumUniversities,universityName);
                    totalNumUniversities++;
                }
			}
			dicAllInstitutes.close();
			System.out.println("Done ... " + totalNumUniversities + " universities were extracted");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// 90% of people go to top-10 universities
	// 10% go to remaining universities
	public int getRandomUniversity(RandomGeneratorFarm randomFarm, int locationId) {

        double prob = randomFarm.get(RandomGeneratorFarm.Aspect.UNCORRELATED_UNIVERSITY).nextDouble();
		Vector<Integer> countries = locationDic.getCountries();
		if (randomFarm.get(RandomGeneratorFarm.Aspect.UNCORRELATED_UNIVERSITY).nextDouble() <= probUncorrelatedUniversity) {
		    locationId = countries.get(randomFarm.get(RandomGeneratorFarm.Aspect.UNCORRELATED_UNIVERSITY_LOCATION).nextInt(countries.size()));
		}
		
		while (universitiesByLocation.get(locationId).size() == 0) {
            locationId = countries.get(randomFarm.get(RandomGeneratorFarm.Aspect.UNCORRELATED_UNIVERSITY_LOCATION).nextInt(countries.size()));
        }
		
		int range = universitiesByLocation.get(locationId).size();
		if (prob > probUncorrelatedUniversity && randomFarm.get(RandomGeneratorFarm.Aspect.TOP_UNIVERSITY).nextDouble() < probTopUniv) {
				range = Math.min(universitiesByLocation.get(locationId).size(), 10);
		}
		
		int randomUniversityIdx = randomFarm.get(RandomGeneratorFarm.Aspect.UNIVERSITY).nextInt(range);
		int zOrderLocation = locationDic.getZorderID(locationId);
        int universityLocation = (zOrderLocation << 24) | (randomUniversityIdx << 12);
		return universityLocation;
	}
	
	public long getUniversityFromLocation(int universityLocation) {
		int zOrderLocationId = universityLocation >> 24;
		int universityId = (universityLocation >> 12) & 0x0FFF;
		int locationId = locationDic.getLocationIdFromZOrder(zOrderLocationId);
		return universitiesByLocation.get(locationId).get(universityId);
	}

    public String getUniversityName( long university ) {
        return universityName.get(university);
    }

    public Set<Long> getUniversities() {
        return universityToLocation.keySet();
    }
}
