package edu.bu.zaman.ModelVisualizer;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;

public class App
{		
	public final static String ARRAY_DELIMITER = "#";	
	public final static String MODEL_FILE_EXT = ".json";	
	
	public static void main(String[] args)
	{
		// Fetch the model output directory from the model
		File outputDirectory = new File(edu.bu.zaman.MMHModel.App.getOutputDirectory());
		if (!outputDirectory.isDirectory())
		{
			System.out.println("Error: invalid model output directory " + outputDirectory.getAbsolutePath());
			return;
		}
		
		// List all of the output files in the model's output directory, filtered by the
		// specified output file extension
		String[] outputFiles = outputDirectory.list(new FilenameFilter() 
		{
			public boolean accept(File directory, String name)
			{
				return name.endsWith(MODEL_FILE_EXT);
			}
		});
		
		// Check to make sure that output files are available for analysis
		if (outputFiles == null || outputFiles.length <= 0)
		{
			System.out.println("No output files available for analysis.");
			return;
		}
		
		String analysisFile = outputFiles[0]; // Choose a file to analyze
		
		new ModelVisualizer(Paths.get(outputDirectory.getAbsolutePath(), analysisFile));
	}
}
