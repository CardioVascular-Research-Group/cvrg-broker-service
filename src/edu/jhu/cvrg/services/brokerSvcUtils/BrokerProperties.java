package edu.jhu.cvrg.services.brokerSvcUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class BrokerProperties {
	
	private static String brokerPropertiesPath = "/axis2/WEB-INF/conf/service.properties";
	private static Properties prop;
	private static BrokerProperties singleton;
	private static File propertiesFile = null;
	private static long lastChange = 0;
	

	private BrokerProperties() {
		prop = new Properties();
		propertiesFile = new File(System.getProperty("wtp.deploy")+brokerPropertiesPath);
		loadProperties();
	}
	
	public static BrokerProperties getInstance(){
		if(singleton == null){
			singleton = new BrokerProperties();
		}
		return singleton;
	}
	
	public String getProperty(String propertyName){
		loadProperties();
		return prop.getProperty(propertyName);
	}
	
	private void loadProperties(){
		try {
			if(propertiesFile.lastModified() > lastChange){
				prop.clear();
				prop.load(new FileReader(propertiesFile));
				lastChange = propertiesFile.lastModified();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
