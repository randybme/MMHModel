package edu.bu.zaman.MMHModel.Visualizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

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
		File chartDirectory = new File(getOutputDirectory());
		if (!chartDirectory.exists())
		{
			chartDirectory.mkdir();
		}
		
		// Fetch the model output directory from the model
		final File outputDirectory = new File(edu.bu.zaman.MMHModel.Simulator.Simulator.getOutputDirectory());
		if (!outputDirectory.isDirectory())
		{
			System.out.println("Error: invalid model output directory " + outputDirectory.getAbsolutePath());
			return;
		}
		
		// Open a directory chooser on the event dispatch thread
		Runnable chooserTask = new Runnable()
		{

			@Override
			public void run() 
			{
				JFileChooser fileChooser = new JFileChooser(outputDirectory);		
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileFilter(new FileFilter() {
					
					@Override
					public String getDescription() 
					{
						return "JSON";
					}
					
					@Override
					public boolean accept(File f) 
					{
						return f.getName().endsWith(MODEL_FILE_EXT);
					}
				});
				
				int returnValue = fileChooser.showOpenDialog(null);		
				if (returnValue == JFileChooser.APPROVE_OPTION)
				{
					File selectedFile = fileChooser.getSelectedFile();
					new DataVisualizer(selectedFile.toPath());
				}				
			}			
		};
		
		SwingUtilities.invokeLater(chooserTask);
	}
}
