package edu.bu.zaman.MMHModel.Visualizer;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;

public class Visualizer
{		
	public final static String ARRAY_DELIMITER = "#";	
	public final static String MODEL_FILE_EXT = ".json";	
	
	public final static Font labelFont = new Font("System", Font.BOLD, 12);
	public final static Color labelColor = new Color(0x111111);
	
	/**
	 * Provides an absolute path to the output directory where all of the exported chart data
	 * is stored.
	 * 
	 * @return the absolute path to the export directory
	 */
	public static String getOutputDirectory()
	{
		String currentDir = System.getProperty("user.dir");		
        String outputDir = Paths.get(currentDir, "charts").toString();
        
        return outputDir;
	}
	
	public static void main(String[] args)
	{
		// Make sure the output directory for exporting chart data exists
		File outputDirectory = new File(getOutputDirectory());
		if (!outputDirectory.exists())
		{
			outputDirectory.mkdir();
		}
		
		// Fetch the model output directory from the model
		outputDirectory = new File(edu.bu.zaman.MMHModel.Simulator.Simulator.getOutputDirectory());
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
		
		new DataVisualizer(Paths.get(outputDirectory.getAbsolutePath(), analysisFile));
	}
}
