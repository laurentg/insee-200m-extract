/*
 * This software is released under the European Union Public Licence (EUPL v.1.1).
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * 
 * Copyright (c) 2015 Mecatran / DREAL PACA
 */
package com.mecatran.insee.extract;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.beust.jcommander.JCommander;

public class Main {

	public static void main(String[] args) throws Exception {
		System.out
				.println("Convertisseur/extracteur données carroyées 200m INSEE.");

		Parameters params = new Parameters();
		JCommander jCommander = new JCommander(params, args);

		if (params.help) {
			jCommander.setProgramName("java -jar insee-200m-extract.jar");
			usage();
			jCommander.usage();
			System.exit(0);
		}
		if (!params.outputGeotiff && !params.outputCsv) {
			System.err
					.println("Please use at least one of GeoTiff or CSV output.");
			jCommander.usage();
			System.exit(1);
		}

		Insee200mConv insee = new Insee200mConv();

		CoordinateReferenceSystem cmdLineCRS = CRS.decode(params.bboxCRS, true);
		CoordinateReferenceSystem dataCRS = CRS.decode("EPSG:3035", true);
		MathTransform transform = CRS.findMathTransform(cmdLineCRS, dataCRS);
		insee.csvCRS = CRS.decode(params.csvCRS, true);

		DirectPosition2D minCmdLine = new DirectPosition2D(params.left,
				params.bottom);
		DirectPosition2D maxCmdLine = new DirectPosition2D(params.right,
				params.top);

		// Transform to EPSG:3035 CRS
		DirectPosition2D minData = new DirectPosition2D();
		DirectPosition2D maxData = new DirectPosition2D();
		transform.transform(minCmdLine, minData);
		transform.transform(maxCmdLine, maxData);

		// Align to 200m
		minData.x = Math.round(minData.x / Insee200mConv.GRID_SIZE_METERS)
				* Insee200mConv.GRID_SIZE_METERS;
		minData.y = Math.round(minData.y / Insee200mConv.GRID_SIZE_METERS)
				* Insee200mConv.GRID_SIZE_METERS;
		maxData.x = Math.round(maxData.x / Insee200mConv.GRID_SIZE_METERS)
				* Insee200mConv.GRID_SIZE_METERS - Insee200mConv.GRID_SIZE_METERS;
		maxData.y = Math.round(maxData.y / Insee200mConv.GRID_SIZE_METERS)
				* Insee200mConv.GRID_SIZE_METERS - Insee200mConv.GRID_SIZE_METERS;

		insee.envelope = new Envelope2D(dataCRS, minData.x, minData.y,
				maxData.x - minData.x, maxData.y - minData.y);
		System.out.println("Rectangle extraction (EPSG:3035) : "
				+ insee.envelope);

		insee.envelopeIsSourceCRS = true;
		insee.baseFilename = params.outputPrefix;
		insee.outputGeotiff = params.outputGeotiff;
		insee.outputCsv = params.outputCsv;
		insee.run();
	}

	private static void usage() {
		String help = "--------------------------------------------------------------\n"
				+ "Ce programme extrait une partie des données carroyées 200m\n"
				+ "publiées par l'INSEE, au format GeoTiff et/ou CSV.\n"
				+ "\n"
				+ "Ce programme est un logiciel libre (licence EUPL v.1.1)\n"
				+ "développé par Mecatran pour le compte du DREAL PACA.\n"
				+ "Pour plus d'information sur la licence, veuillez consulter:\n" 
				+ "https://joinup.ec.europa.eu/software/page/eupl/licence-eupl\n"
				+ "\n"
				+ "Les données des rectangles sont fusionnées aux carrés selon\n"
				+ "la méthode préconisée par l'INSEE (voir méthodologie en ligne).\n"
				+ "\n"
				+ "Deux formats sont disponibles: GeoTIFF ou CSV.\n"
				+ "En GeoTiff, le CRS est celui des données sources (EPSG:3035).\n"
				+ "En CSV, il est possible de choisir le CRS de sortie.\n"
				+ "\n"
				+ "Les variables disponibles sont celles des données publiées\n"
				+ "(population, ages, revenus, mégages...)\n"
				+ "Elles sont disponibles en sortie à la fois sous forme de somme\n"
				+ "sur un carreau (suffixe 'sum') et normalisé par individu ou\n"
				+ "ménage (suffixe 'norm').\n"
				+ "\n"
				+ "Pour plus d'information sur les variables disponibles:\n"
				+ "http://www.insee.fr/fr/themes/detail.asp?reg_id=0&ref_id=donnees-carroyees\n"
				+ "--------------------------------------------------------------\n";
		System.out.println(help);
	}
}
