package common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import kakomontoujyouCSVProcessor.CsvProcessor;

public class PropertiesUtils {

	private Properties properties = null;

	private void initProperties() throws IOException {

		properties = new Properties();

		InputStream in = CsvProcessor.class.getClassLoader().getResourceAsStream("system.properties");
		properties.load(in);
	}

	public String getValueString(String key) throws IOException {

		initProperties();

		return properties.getProperty(key);

	}

}