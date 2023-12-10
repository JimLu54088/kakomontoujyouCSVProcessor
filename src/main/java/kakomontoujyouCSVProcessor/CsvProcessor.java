package kakomontoujyouCSVProcessor;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import logic.KakomontoujyouCSVProcessorLogic;

public class CsvProcessor {
	public static void main(String[] args) {
		try {

			KakomontoujyouCSVProcessorLogic logic = new KakomontoujyouCSVProcessorLogic();
			logic.readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
