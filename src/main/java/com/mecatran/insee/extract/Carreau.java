/*
 * This software is released under the European Union Public Licence (EUPL v.1.1).
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * 
 * Copyright (c) 2015 Mecatran / DREAL PACA
 */
package com.mecatran.insee.extract;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.opengis.geometry.DirectPosition;

public class Carreau {

	/* Position in data CRS (EPSG:3035) */
	public DirectPosition position;
	
	public GridCoordinates2D gridPosition;
	
	/* Carreau ID */
	public String id;
	
	/* Rectangle ID */
	public String idRect;
	
	public float nbIndividus;
	
	public float nbMenages;
	
	public float[] varsSummed;
	public float[] varsNormalized;
	
}
