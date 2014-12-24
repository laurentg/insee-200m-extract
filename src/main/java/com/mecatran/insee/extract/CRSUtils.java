/*
 * This software is released under the European Union Public Licence (EUPL v.1.1).
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * 
 * Copyright (c) 2015 Mecatran / DREAL PACA
 */
package com.mecatran.insee.extract;

import org.geotools.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CRSUtils {

	/**
	 * @param sourceCRS
	 *            The source CRS (Currently only EPSG:3035)
	 * @param strCoord
	 *            The coordinate in string format (NyyyyyExxxxx)
	 * @return
	 */
	public static DirectPosition parseCRS(CoordinateReferenceSystem sourceCRS,
			String strCoord) {
		String crsCode = sourceCRS.getName().getCode();
		if (!crsCode.equals("ETRS89 / LAEA Europe"))
			throw new UnsupportedOperationException();
		String northStr = strCoord.substring(strCoord.indexOf('N') + 1,
				strCoord.indexOf('E'));
		String eastStr = strCoord.substring(strCoord.indexOf('E') + 1,
				strCoord.length());
		double north = Integer.parseInt(northStr);
		double east = Integer.parseInt(eastStr);
		return new DirectPosition2D(sourceCRS, east, north);
	}
}
