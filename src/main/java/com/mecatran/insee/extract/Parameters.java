/*
 * This software is released under the European Union Public Licence (EUPL v.1.1).
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * 
 * Copyright (c) 2015 Mecatran / DREAL PACA
 */
package com.mecatran.insee.extract;

import com.beust.jcommander.Parameter;

public class Parameters {

	@Parameter(names = { "--help" }, description = "Print this help", help = true)
	public boolean help;

	@Parameter(names = { "--left" }, description = "Left side of extract bounding box (min lon/X)")
	public double left = new Float(-6.0);

	@Parameter(names = { "--right" }, description = "Right side of extract bounding box (max lon/X)")
	public double right = new Float(10);

	@Parameter(names = { "--bottom" }, description = "Bottom side of extract bounding box (min lat/Y)")
	public double bottom = new Float(40.0);

	@Parameter(names = { "--top" }, description = "Top side of extract bounding box (max lat/Y)")
	public double top = new Float(52);

	@Parameter(names = { "--bboxCRS" }, description = "CRS of the command-line bounding box. Please note that the extract bounding box is always a rectangle defined in the source data CRS (EPSG:3035)")
	public String bboxCRS = "EPSG:4326";

	@Parameter(names = { "--csvCRS" }, description = "CRS of the CSV output (default EPSG:4326 aka WGS84)")
	public String csvCRS = "EPSG:4326";

	@Parameter(names = { "-o", "--outputPrefix" }, description = "Output prefix for filenames. Default to 'INSEE_200m'")
	public String outputPrefix = "INSEE_200m";

	@Parameter(names = { "--geotiff" }, description = "Output GeoTiff")
	public boolean outputGeotiff = false;

	@Parameter(names = { "--csv" }, description = "Output CSV")
	public boolean outputCsv = false;

	// TODO Option to select the variables to output (ages, revenus, ...)
}