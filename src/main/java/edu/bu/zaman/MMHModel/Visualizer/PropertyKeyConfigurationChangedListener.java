package edu.bu.zaman.MMHModel.Visualizer;

public interface PropertyKeyConfigurationChangedListener 
{
	void configurationChanged(PropertyKeyConfigurationPanel panel);
	void seriesNameChanged(PropertyKeyConfigurationPanel panel, String newName);
}
