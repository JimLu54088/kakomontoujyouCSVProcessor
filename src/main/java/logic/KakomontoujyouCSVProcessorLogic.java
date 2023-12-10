package logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.PropertiesUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class KakomontoujyouCSVProcessorLogic {

	private static final Logger logger = LogManager.getLogger(KakomontoujyouCSVProcessorLogic.class);
	private static final String LOG_HEADER = "[" + KakomontoujyouCSVProcessorLogic.class.getSimpleName() + "] :: ";
	private static final String ERROR_LOG_HEADER = "[" + KakomontoujyouCSVProcessorLogic.class.getName() + "] :: ";

	static PropertiesUtils property = new PropertiesUtils();

	private String localDirectory;

	public String getLocalDirectory() {
		return this.localDirectory;
	}

	public KakomontoujyouCSVProcessorLogic() {
		try {
			localDirectory = property.getValueString("localDirectory");
		} catch (IOException e) {
			// 处理异常，例如记录日志或抛出自定义异常
			logger.error("Failed to read configuration: " + e.getMessage());
		}
	}

	private static final String DATE_FORMAT_PATTERN = "([0-9]{12})";
	private static final String AVERAGE = "average";
	private static final String DECIMALFOUR = "#.####";
	private static final String FORMAT_PATTERN_YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

	public void readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE() {
		logger.info(LOG_HEADER + String.format(
				"Read CSV file and write avg to new csv and create total avg file start. Local_directory=%s",
				this.localDirectory));

		List<File> targetFiles = this.getTargetFileList();

		for (File targetFile : targetFiles) {

			try {
				removeTailingSpace(targetFile);
				int oCount = readoCount(targetFile);
				int lastNo = readLastCount(targetFile);
				double average_get = (double) oCount / lastNo;
				double round_average_get = Math.round(average_get * 100.0) / 100.0;
//				System.out.println("檔案 " + targetFile.getName() + " 中的平均值: " + round_average_get);

				// 將平均值寫入新的 CSV 文件
				writeAverageToFile(targetFile, round_average_get);
			} catch (Exception e) {
				System.out.println("error file is " + targetFile.getName());
				e.printStackTrace();
				// 遇到錯誤時的處理邏輯
			}

		}

		List<File> targetAVGFiles = this.getAVGFileList();
		List<Double> avgList = new ArrayList<>();

		for (File targetAVGFile : targetAVGFiles) {

			try {

				double avg = readAVG(targetAVGFile);

				avgList.add(avg);

			} catch (Exception e) {
				System.out.println("error file is " + targetAVGFile.getName());
				e.printStackTrace();
				// 遇到錯誤時的處理邏輯
			}

		}

		// 計算平均值
		double average_all = calculateAverage(avgList);

		// 格式化平均值，保留小數點後4位
		String formattedaverage_all = formatDecimal(average_all);

//		System.out.println("平均值: " + formattedaverage_all);

		// 將平均值寫入新的 CSV 文件
		try {
			writeTotalAverageToFile(formattedaverage_all);
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info(LOG_HEADER + "Read CSV file and write avg to new csv and create total avg file end.");

	}

	public double calculateAverage(List<Double> numbers) {
		if (numbers.isEmpty()) {
			return 0.0; // 或者您希望返回其他值，這裡返回 0.0 作為示例
		}

		double sum = 0.0;
		for (Double number : numbers) {
			sum += number;
		}

		return sum / numbers.size();
	}

	public String formatDecimal(double value) {
		DecimalFormat decimalFormat = new DecimalFormat(DECIMALFOUR);
		decimalFormat.setMinimumFractionDigits(4);
		return decimalFormat.format(value);
	}

	public void writeAverageToFile(File originalFile, double average) throws IOException {
		String fileName = originalFile.getName().replace(".csv", "_average_result.csv");
		File outputCsvFile = new File(originalFile.getParent(), fileName);

		try (FileWriter writer = new FileWriter(outputCsvFile)) {
			writer.write("average\n");
			writer.write(String.format("%.2f", average));
		}
		logger.info(
				LOG_HEADER + "File " + originalFile.getName() + " 's average is written to " + outputCsvFile.getName());
	}

	public void writeTotalAverageToFile(String avg_total) throws IOException {

		// 獲取當前時間
		LocalDateTime now = LocalDateTime.now();

		// 定義日期時間格式，包含秒
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN_YYYYMMDDHHMMSS);

		// 格式化時間
		String formattedDateTime = now.format(formatter);

		// 生成檔案名稱
		String fileName = formattedDateTime + "totalReport.csv";

		File outputCsvFile = new File(localDirectory, fileName);

		try (FileWriter writer = new FileWriter(outputCsvFile)) {
			writer.write("total_average\n");
			writer.write(avg_total);
		}
		logger.info(LOG_HEADER + "total average has been written to " + outputCsvFile.getName());

	}

	public int readoCount(File csvFile) throws IOException {
		int oCount = 0;

		try (CSVParser parser = new CSVParser(new FileReader(csvFile), CSVFormat.DEFAULT.withHeader())) {
			for (CSVRecord record : parser) {
				String value = record.get("正誤");
				if ("○".equals(value)) {
					oCount++;
				}
			}
		}

//		System.out.println("檔案 " + csvFile.getName() + " 中的 ○ 數: " + oCount);
		return oCount;
	}

	public int readLastCount(File csvFile) throws IOException {
		int lastCount = 0;

		try (CSVParser parser = new CSVParser(new FileReader(csvFile), CSVFormat.DEFAULT.withHeader())) {
			int totalRows = parser.getRecords().size(); // 獲取總行數
//			System.out.println("檔案 " + csvFile.getName() + " 中的 No. 的最後一個數字: " + totalRows);
			lastCount = totalRows;
		}

		return lastCount;
	}

	public double readAVG(File csvFile) throws IOException {

		double avg = 0.0;

		try (CSVParser parser = new CSVParser(new FileReader(csvFile), CSVFormat.DEFAULT.withHeader())) {

			CSVRecord record = parser.iterator().next(); // 取得第一行的記錄

			String averageValue = record.get(AVERAGE);

			// 將獲得的平均值值轉換為 double
			double averageDoubleValue = Double.parseDouble(averageValue);

			// 現在您可以使用 averageDoubleValue 進行需要的操作
//			System.out.println("Average: " + averageDoubleValue);

			avg = averageDoubleValue;
		}

		return avg;

	}

	public void removeTailingSpace(File csvFile) {
		try {
			// 讀取CSV文件
			BufferedReader reader = new BufferedReader(new FileReader(csvFile));
			String line;
			StringBuilder content = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				content.append(line).append(System.lineSeparator());
			}

			reader.close();

			// 刪除最後一行的半行空格
			String updatedContent = content.toString().replaceAll("\\s+$", "");

			// 寫入更新後的內容
			BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));
			writer.write(updatedContent);
			writer.close();

			logger.info(LOG_HEADER + "Delete half a line for completion of file: " + csvFile.getName());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<File> getTargetFileList() {

		logger.info(
				LOG_HEADER + String.format("getTargetFileList process start, CsvFilePath = " + this.localDirectory));

		List<File> targetFileList = new ArrayList<>();

		Pattern patternCsvFile = Pattern.compile(String.format("report%s\\.csv", DATE_FORMAT_PATTERN));

		File dir = new File(this.localDirectory);

		File[] fileList = dir.listFiles();
		List<String> targetFileNameList = new ArrayList<>();

		for (File targetFile : fileList) {

			if (patternCsvFile.matcher(targetFile.getName()).find()) {

				targetFileNameList.add(targetFile.getName());
				targetFileList.add(targetFile);

			}
		}

		targetFileList.sort(Comparator.comparing(File::getName));

		logger.info(LOG_HEADER + "getTargetFileList process end, targetFile = " + String.join(",", targetFileNameList));

		return targetFileList;

	}

	public List<File> getAVGFileList() {

		logger.info(
				LOG_HEADER + String.format("getAVGFileList process start, CsvFilePath = " + this.localDirectory));

		List<File> targetFileList = new ArrayList<>();

		Pattern patternCsvFile = Pattern.compile(String.format("report%s_average_result\\.csv", DATE_FORMAT_PATTERN));

		File dir = new File(this.localDirectory);

		File[] fileList = dir.listFiles();
		List<String> targetFileNameList = new ArrayList<>();

		for (File targetFile : fileList) {

			if (patternCsvFile.matcher(targetFile.getName()).find()) {

				targetFileNameList.add(targetFile.getName());
				targetFileList.add(targetFile);

			}
		}

		targetFileList.sort(Comparator.comparing(File::getName));

		logger.info(LOG_HEADER + "getAVGFileList process end, targetFile = " + String.join(",", targetFileNameList));

		return targetFileList;

	}

}
