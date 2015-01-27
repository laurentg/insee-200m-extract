/*
 * This software is released under the European Union Public Licence (EUPL v.1.1).
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * 
 * Copyright (c) 2015 Mecatran / DREAL PACA
 */
package com.mecatran.insee.extract;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.xBaseJ.DBF;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.NumField;

public class Insee200mConv {

	public static final int GRID_SIZE_METERS = 200;

	public Envelope2D envelope;
	public boolean envelopeIsSourceCRS = true;
	public CoordinateReferenceSystem sourceCRS;
	/*
	 * Note: Raster output CRS *must* be the source CRS, otherwise we would need
	 * to resample and thus loose precision. For CSV output however, we can pick
	 * whatever CRS we want.
	 */
	public CoordinateReferenceSystem csvCRS;
	public MathTransform transform;

	public String baseFilename = "insee_200m";
	public boolean outputGeotiff;
	public boolean outputCsv;

	/* X/Y size of output raster grid */
	private int sx, sy;
	/* Data CRS to grid coordinates */
	private MathTransform2D gridTransform;

	/* All carreaux in the envelope */
	private Map<String, Carreau> carreaux;
	/* Index of rectangle ID -> carreaux */
	private Map<String, List<Carreau>> carreauxPerRect;

	/* Normalize per individual */
	private static final int INDIVIDU = 0;
	/* Normalize per household */
	private static final int MENAGE = 1;

	private static final int NDX_INDIVIDU = -1;
	private static final int NDX_MENAGE = -2;

	private static final String[] INSEE_VAR_NAMES = new String[] { "men_surf",
			"men_occ5", "men_coll", "men_5ind", "men_1ind", "men_prop",
			"men_basr", "ind_age1", "ind_age2", "ind_age3", "ind_age4",
			"ind_age5", "ind_age6", "ind_age7", "ind_age8", "ind_srf" };
	private static final int[] INSEE_DATA_TYPE = new int[] { MENAGE, MENAGE,
			MENAGE, MENAGE, MENAGE, MENAGE, MENAGE, INDIVIDU, INDIVIDU,
			INDIVIDU, INDIVIDU, INDIVIDU, INDIVIDU, INDIVIDU, INDIVIDU,
			INDIVIDU };

	public Insee200mConv() throws Exception {
		envelope = new Envelope2D();
		sourceCRS = CRS.decode("EPSG:3035", true);
		csvCRS = CRS.decode("EPSG:4326", true);
		transform = CRS.findMathTransform(sourceCRS, csvCRS);
	}

	public void run() throws Exception {

		/* Compute output raster grid size. */
		sx = (int) Math.round(envelope.width / GRID_SIZE_METERS);
		sy = (int) Math.round(envelope.height / GRID_SIZE_METERS);

		/*
		 * Compute data CRS to grid coordinate transform. TODO Is there a
		 * simpler way to create a GridGeometry with a given raster size
		 * (width/height) *and* giving the pixel orientation as a parameter?
		 */
		float[][] rasterData = createNanFloatArray(sx, sy);
		GridCoverage2D gridCoverage = new GridCoverageFactory().create("TEMP",
				rasterData, envelope);
		GridGeometry2D gridGeometry = gridCoverage.getGridGeometry();
		gridTransform = gridGeometry
				.getCRSToGrid2D(PixelOrientation.LOWER_LEFT);

		/* Read the data. */
		readCarreaux();
		readRectangles();

		if (outputGeotiff) {
			outputGeotiff(NDX_INDIVIDU, false);
			outputGeotiff(NDX_MENAGE, false);
			for (int varIndex = 0; varIndex < INSEE_VAR_NAMES.length; varIndex++) {
				outputGeotiff(varIndex, false);
				outputGeotiff(varIndex, true);
			}
		}

		if (outputCsv) {
			outputCsv();
		}
	}

	private void readCarreaux() throws Exception {
		System.out.println("Lecture de la table des carreaux...");

		carreaux = new HashMap<>(sx * sy);
		carreauxPerRect = new HashMap<>(sx * sy / 2);

		DBF tableCar = new DBF("car_m.dbf");
		NumField nbIndField = (NumField) tableCar.getField("ind_c");
		CharField idField = (CharField) tableCar.getField("id");
		CharField idInspireField = (CharField) tableCar.getField("idINSPIRE");
		CharField idRectField = (CharField) tableCar.getField("idk");

		for (int i = 0; i < tableCar.getRecordCount(); i++) {
			tableCar.read();

			String idInspire = idInspireField.get();
			String strPos = idInspire.substring(15);
			DirectPosition sPos = CRSUtils.parseCRS(sourceCRS, strPos);

			DirectPosition2D gridPosition = new DirectPosition2D();
			gridTransform.transform(sPos, gridPosition);
			GridCoordinates2D gridCoord = new GridCoordinates2D(
					(int) gridPosition.x, (int) gridPosition.y);

			/*
			 * To be sure we are on the raster grid, we check if the *grid*
			 * coordinate is in the grid envelope, not the data CRS (there could
			 * be rounding issues).
			 */
			if (gridCoord.x >= 0 && gridCoord.x < sx && gridCoord.y >= 0
					&& gridCoord.y < sy) {
				String idRectangle = idRectField.get();

				/* In bounds: create new carreau */
				Carreau carreau = new Carreau();
				carreau.id = idField.get();
				carreau.idRect = idRectangle;
				carreau.nbIndividus = Float.parseFloat(nbIndField.get());
				carreau.position = sPos;
				carreau.gridPosition = gridCoord;

				carreaux.put(carreau.id, carreau);

				/* Add it to rectangle -> carreau index */
				List<Carreau> carreauxForRect = carreauxPerRect
						.get(idRectangle);
				if (carreauxForRect == null) {
					carreauxForRect = new ArrayList<>(16);
					carreauxPerRect.put(idRectangle, carreauxForRect);
				}
				carreauxForRect.add(carreau);

			}

			if (i % 100000 == 0) {
				System.out.print(".");
			}
		}
		System.out.println(tableCar.getRecordCount() + " carreaux, "
				+ carreaux.size() + " dans l'enveloppe.");
		tableCar.close();
	}

	private void readRectangles() throws Exception {
		System.out.println("Lecture de la table des rectangles...");

		DBF tableRect = new DBF("rect_m.dbf");
		CharField idRectField = (CharField) tableRect.getField("idk");
		NumField nbIndRectField = (NumField) tableRect.getField("ind_r");
		NumField nbMenRectField = (NumField) tableRect.getField("men");
		NumField[] varField = new NumField[INSEE_VAR_NAMES.length];
		for (int i = 0; i < INSEE_VAR_NAMES.length; i++) {
			varField[i] = (NumField) tableRect.getField(INSEE_VAR_NAMES[i]);
		}

		int nProc = 0;
		for (int i = 0; i < tableRect.getRecordCount(); i++) {
			tableRect.read();

			String idRectangle = idRectField.get();
			List<Carreau> carreauxForRect = carreauxPerRect.get(idRectangle);
			if (carreauxForRect != null) {
				float nbIndividusRect = Float.parseFloat(nbIndRectField.get());
				float nbMenagesRect = Float.parseFloat(nbMenRectField.get());
				for (Carreau carreau : carreauxForRect) {
					float kSum = carreau.nbIndividus / nbIndividusRect;
					carreau.nbMenages = nbMenagesRect * kSum;
					carreau.varsSummed = new float[varField.length];
					carreau.varsNormalized = new float[varField.length];
					for (int j = 0; j < varField.length; j++) {
						carreau.varsSummed[j] = Float.NaN;
						carreau.varsNormalized[j] = Float.NaN;
					}
					for (int j = 0; j < varField.length; j++) {
						String varStr = varField[j].get();
						if (varStr.length() > 0) {
							float rectVar = Float.parseFloat(varStr);
							carreau.varsSummed[j] = rectVar * kSum;
							switch (INSEE_DATA_TYPE[j]) {
							case INDIVIDU:
								carreau.varsNormalized[j] = rectVar
										/ nbIndividusRect;
								break;
							case MENAGE:
								carreau.varsNormalized[j] = rectVar
										/ nbMenagesRect;
								break;
							default:
								throw new IllegalStateException();
							}
						}
					}
				}
				nProc++;
			}

			if (i % 100000 == 0) {
				System.out.print(".");
			}
		}
		System.out.println(tableRect.getRecordCount() + " rectangles, " + nProc
				+ " touchant l'enveloppe.");
		tableRect.close();
	}

	private void outputGeotiff(int varIndex, boolean normalized)
			throws Exception {
		float[][] rasterData = createNanFloatArray(sx, sy);

		for (Carreau carreau : carreaux.values()) {
			float var;
			switch (varIndex) {
			case NDX_INDIVIDU:
				var = carreau.nbIndividus;
				break;
			case NDX_MENAGE:
				var = carreau.nbMenages;
				break;
			default:
				var = normalized ? carreau.varsNormalized[varIndex]
						: carreau.varsSummed[varIndex];
				break;
			}
			rasterData[carreau.gridPosition.y][carreau.gridPosition.x] = var;
		}
		String suffix;
		switch (varIndex) {
		case NDX_INDIVIDU:
			suffix = "ind";
			break;
		case NDX_MENAGE:
			suffix = "men";
			break;
		default:
			suffix = INSEE_VAR_NAMES[varIndex]
					+ (normalized ? "_norm" : "_sum");
			break;
		}
		String fileName = baseFilename + "_" + suffix + ".tiff";
		GridCoverage2D gridCoverage = new GridCoverageFactory().create(
				"INSEE 200m", rasterData, envelope);
		writeGeoTiff(fileName, gridCoverage);
	}

	private float[][] createNanFloatArray(int sx, int sy) {
		float[][] ret = new float[sy][sx];
		for (int iy = 0; iy < ret.length; iy++) {
			for (int ix = 0; ix < ret[iy].length; ix++) {
				ret[iy][ix] = Float.NaN;
			}
		}
		return ret;
	}

	private void writeGeoTiff(String filename, GridCoverage2D gridCoverage)
			throws Exception {
		System.out.println("Sauvegarde fichier GeoTiff '" + filename + "'...");

		GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
		writeParams.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
		writeParams.setCompressionType("LZW");
		ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
		/*
		 * TODO Force GTRasterTypeGeoKey to RasterPixelIsArea (but this should
		 * be safe anyway as it is now, as this is the default.)
		 */
		params.parameter(
				AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
				.setValue(writeParams);
		GeoTiffWriter writer = new GeoTiffWriter(new File(filename));
		writer.write(gridCoverage,
				params.values().toArray(new GeneralParameterValue[1]));
	}

	private void outputCsv() throws Exception {
		String filename = baseFilename + ".csv";
		System.out.println("Sauvegarde fichier CSV '" + filename + "'...");
		List<String> headers = new ArrayList<String>();
		headers.add("x");
		headers.add("y");
		headers.add("ind");
		headers.add("men");
		for (String var : INSEE_VAR_NAMES) {
			headers.add(var + "_sum");
			headers.add(var + "_norm");
		}
		Appendable out = new FileWriter(filename);
		CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader(
				headers.toArray(new String[0])).print(out);
		for (Carreau carreau : carreaux.values()) {
			DirectPosition centerPosition = new DirectPosition2D(carreau.position);
			centerPosition.setOrdinate(0, centerPosition.getOrdinate(0) + 100);
			centerPosition.setOrdinate(1, centerPosition.getOrdinate(1) + 100);
			DirectPosition targetPosition = transform.transform(
					centerPosition, null);
			List<Object> values = new ArrayList<Object>(
					INSEE_VAR_NAMES.length * 2 + 4);
			values.add(targetPosition.getOrdinate(0));
			values.add(targetPosition.getOrdinate(1));
			values.add(carreau.nbIndividus);
			values.add(carreau.nbMenages);
			for (int i = 0; i < INSEE_VAR_NAMES.length; i++) {
				values.add(carreau.varsSummed[i]);
				values.add(carreau.varsNormalized[i]);
			}
			csvPrinter.printRecord(values);
		}
		csvPrinter.close();
	}
}
