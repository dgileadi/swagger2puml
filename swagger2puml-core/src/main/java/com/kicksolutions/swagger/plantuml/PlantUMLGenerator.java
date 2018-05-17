package com.kicksolutions.swagger.plantuml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.ISourceFileReader;
import net.sourceforge.plantuml.SourceFileReader;
import net.sourceforge.plantuml.preproc.Defines;

/**
 * MSANTOSH
 *
 */
public class PlantUMLGenerator
{
	public enum UMLType {
		full, model, entity;

		public static UMLType fromString(String name) {
			for (UMLType type : UMLType.values()) {
				if (type.toString().equalsIgnoreCase(name)) {
					return type;
				}
			}
			return full;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(PlantUMLGenerator.class.getName());

	public PlantUMLGenerator() {
		super();
	}

    /**
     *
     * @param specFile
     * @param output
     */
    public void transformSwagger2Puml(String specFile,String output, UMLType type,boolean includeCardinality,boolean generateSvg){
    	LOGGER.entering(LOGGER.getName(), "transformSwagger2Puml");

    	File swaggerSpecFile = new File(specFile);
    	File targetLocation = new File(output);

    	if(swaggerSpecFile.exists() && !swaggerSpecFile.isDirectory()
    			&& targetLocation.exists() && targetLocation.isDirectory()) {

    		Swagger swaggerObject = new SwaggerParser().read(swaggerSpecFile.getAbsolutePath());
			PlantUMLCodegen codegen;
			switch (type) {
			case full:
				codegen = new PlantUMLClassCodegen(swaggerObject, targetLocation, false, includeCardinality);
				break;
			case model:
				codegen = new PlantUMLClassCodegen(swaggerObject, targetLocation, true, includeCardinality);
				break;
			case entity:
				codegen = new PlantUMLEntityCodegen(swaggerObject, targetLocation);
				break;
			default:
				throw new RuntimeException("Unknown type argument");
			}
    		String pumlPath = null;

    		try{
    			LOGGER.info("Processing File --> "+ specFile);
    			pumlPath = codegen.generatePuml();
    			LOGGER.info("Sucessfully Create PUML !!!");

    			if(generateSvg)
    			{
    				generateUMLDiagram(pumlPath, targetLocation);
    			}
    		}
    		catch(Exception e){
    			LOGGER.log(Level.SEVERE, e.getMessage(),e);
    			throw new RuntimeException(e);
    		}
    	}else{
    		throw new RuntimeException("Spec File or Ouput Locations are not valid");
    	}

    	LOGGER.exiting(LOGGER.getName(), "transformSwagger2Puml");
    }

	private void generateUMLDiagram(String pumlLocation, File targetLocation)
			throws IOException, InterruptedException {

		File pumlFile = new File(pumlLocation);
		Defines defines = Defines.createWithFileName(pumlFile);
		List<String> config = Collections.emptyList();
		final ISourceFileReader sourceFileReader = new SourceFileReader(defines, pumlFile, targetLocation,
					config, null, new FileFormatOption(FileFormat.SVG));

		final List<GeneratedImage> result = sourceFileReader.getGeneratedImages();

		logErrors(pumlFile, result);
	}

	private static void logErrors(File f, final List<GeneratedImage> list) throws IOException {
		for (GeneratedImage i : list) {
			final int lineError = i.lineErrorRaw();
			if (lineError != -1) {
				LOGGER.severe("Error line " + lineError + " in file: " + f.getCanonicalPath());
			}
		}
	}

}