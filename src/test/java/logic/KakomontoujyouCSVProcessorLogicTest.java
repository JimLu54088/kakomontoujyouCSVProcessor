package logic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.reflect.Whitebox;

import common.PropertiesUtils;

public class KakomontoujyouCSVProcessorLogicTest {

	@Spy
	@InjectMocks
	private KakomontoujyouCSVProcessorLogic logic;

	private AutoCloseable mockClose;

	private static Path localFileDirPath;

	String originalLocalDirectory;

	private static final String MOCK_APPENDER = "MockAppender";
	private static final String FORMAT_PATTERN_YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
	private static final Pattern TOTAL_RESULT_CSV_FILE_PATTERN = Pattern.compile("\\d{14}totalReport\\.csv");
	private static final String DATE_FORMAT_PATTERN = "([0-9]{14})";

	private static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;

	private static final String LOCAL_DIRECTORY_PATH = "D:\\shell_test_231103\\localtest\\";

	private static final String LOG_HEADER = "[" + KakomontoujyouCSVProcessorLogic.class.getSimpleName() + "] :: ";
	private static final String ERROR_LOG_HEADER = "[" + KakomontoujyouCSVProcessorLogic.class.getName() + "] :: ";

	public KakomontoujyouCSVProcessorLogicTest() {

	}

	@BeforeClass
	public static void createTestDirectory() throws IOException {

		localFileDirPath = Files.createDirectories(FileSystems.getDefault().getPath(LOCAL_DIRECTORY_PATH));

	}

	@Before
	public void setUp() {

		mockClose = MockitoAnnotations.openMocks(this);

		// 保存原始的 localDirectory 值
		this.originalLocalDirectory = logic.getLocalDirectory();

		// 设置 localDirectory 为测试临时文件夹路径
		Whitebox.setInternalState(logic, "localDirectory", "D:\\shell_test_231103\\localtest");

	}

	@AfterClass
	public static void removeTestDirectory() throws IOException {

		FileUtils.deleteDirectory(localFileDirPath.toFile());

	}

	@After
	public void tearDown() throws Exception {
		Whitebox.setInternalState(logic, "localDirectory", this.originalLocalDirectory);
		mockClose.close();
		removeAppender();
	}

	@Test
	public void success_readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE() throws Exception {

		StringWriter writer = new StringWriter();

		addAppender(writer, MOCK_APPENDER, Level.INFO);

		Files.deleteIfExists(localFileDirPath.resolve("report202312042301.csv"));
		Files.createFile(localFileDirPath.resolve("report202312042301.csv"));

		Path filepath = Paths.get(LOCAL_DIRECTORY_PATH + "report202312042301.csv");

		try (BufferedWriter br = new BufferedWriter(new FileWriter(filepath.toString(), StandardCharsets.UTF_8))) {

			br.write("No.,正誤,分野名,大分類,中分類,出典,学習日");
			br.newLine(); // 換行

			// 寫入第二行數據
			br.write(
					"1,○,テクノロジ系,基礎理論,基礎理論,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/17_aki/q5.html\"\",\"\"平成17年秋期 問5\"\")\",2023/12/4");
			br.newLine(); // 換行

			// 寫入第3行數據
			br.write(
					"2,○,テクノロジ系,基礎理論,アルゴリズムとプログラミング,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/17_haru/q13.html\"\",\"\"平成17年春期 問13\"\")\",2023/12/4");
			br.newLine(); // 換行

			// 寫入第4行數據
			br.write(
					"3,×,テクノロジ系,基礎理論,基礎理論,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/20_haru/q6.html\"\",\"\"平成20年春期 問6\"\")\",2023/12/4");
			br.newLine(); // 換行

			// 寫入第5行數據
			br.write(
					"4,×,テクノロジ系,基礎理論,基礎理論,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/29_aki/q3.html\"\",\"\"平成29年秋期 問3\"\")\",2023/12/4");
			br.newLine(); // 換行
			br.write(" ");

		}

		logic.readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE();

		assertTrue(Files.list(localFileDirPath).map(Path::getFileName).map(Path::toString)
				.anyMatch(fileName -> fileName.equals("report202312042301_average_result.csv")));

		// 用來格式化當前日期
		SimpleDateFormat dateFormat = new SimpleDateFormat(FORMAT_PATTERN_YYYYMMDDHHMMSS);

		// 產生一個正規表達式，匹配 "yyyyMMddHHmmss" 格式的檔案名稱
		String totalReportNamePattern = dateFormat.format(new Date()) + "totalReport\\.csv";

		assertTrue(Files.list(localFileDirPath).map(Path::getFileName).map(Path::toString)
				.anyMatch(fileName -> fileName.matches(totalReportNamePattern)));

		Path expectedFileReportAVGResult = Paths.get(LOCAL_DIRECTORY_PATH + "report202312042301_average_result.csv");
		String expectedHeaderReportAVGResult = "average";
		String expectedHeaderTotalReport = "total_average";

		try (BufferedReader bf = new BufferedReader(
				new InputStreamReader(new FileInputStream(expectedFileReportAVGResult.toString()), CHARSET_UTF8))) {
			String actualCSV;
			int numberCounter = 0;
			while ((actualCSV = bf.readLine()) != null) {

				if (numberCounter == 0) {
					assertThat(actualCSV, is(expectedHeaderReportAVGResult));
				} else {
					assertThat(actualCSV.trim(), is("0.50"));

				}

				numberCounter++;

			}
			assertThat(numberCounter, is(2)); // 假設只有兩行
		}

		Pattern patternCsvFile = Pattern.compile(String.format("%stotalReport.csv", DATE_FORMAT_PATTERN));

		File dir = new File(LOCAL_DIRECTORY_PATH);

		File[] fileList = dir.listFiles();
		List<String> targetFileNameList = new ArrayList<>();

		for (File targetFile : fileList) {

			if (patternCsvFile.matcher(targetFile.getName()).find()) {

				try (BufferedReader bf = new BufferedReader(
						new InputStreamReader(new FileInputStream(targetFile), CHARSET_UTF8))) {
					String actualCSV;
					int numberCounter = 0;
					while ((actualCSV = bf.readLine()) != null) {

						if (numberCounter == 0) {
							assertThat(actualCSV, is(expectedHeaderTotalReport));
						} else {
							assertThat(actualCSV.trim(), is("0.5000"));

						}

						numberCounter++;

					}
					assertThat(numberCounter, is(2)); // 假設只有兩行
				}

			}
		}

		assertTrue(writer.toString().contains(LOG_HEADER
				+ "Read CSV file and write avg to new csv and create total avg file start. Local_directory=D:\\shell_test_231103\\localtest"));
		assertTrue(writer.toString().contains(
				LOG_HEADER + "getTargetFileList process start, CsvFilePath = D:\\shell_test_231103\\localtest"));
		assertTrue(writer.toString()
				.contains(LOG_HEADER + "getTargetFileList process end, targetFile = report202312042301.csv"));
		assertTrue(writer.toString().contains(LOG_HEADER
				+ "File report202312042301.csv 's average is written to report202312042301_average_result.csv"));
		assertTrue(writer.toString()
				.contains(LOG_HEADER + "getAVGFileList process start, CsvFilePath = D:\\shell_test_231103\\localtest"));
		assertTrue(writer.toString().contains(
				LOG_HEADER + "getAVGFileList process end, targetFile = report202312042301_average_result.csv"));

		String logPattern = "\\[KakomontoujyouCSVProcessorLogic\\] :: total average has been written to .*totalReport\\.csv";
		Pattern pattern = Pattern.compile(logPattern);
		Matcher matcher = pattern.matcher(writer.toString());
		assertTrue(matcher.find());

		assertTrue(writer.toString()
				.contains(LOG_HEADER + "Read CSV file and write avg to new csv and create total avg file end."));

		this.deleteTempFile();
	}

	@Test
	public void calculateAverage_inputNull() throws Exception {

		List<Double> numbers = new ArrayList<>();

		double zero = logic.calculateAverage(numbers);
		assertEquals(0.0, zero, 0.0001);

		this.deleteTempFile();
	}

	@Test
	public void success_getTargetFileList_no_file_matched() throws Exception {

		Files.deleteIfExists(localFileDirPath.resolve("test.csv"));

		Files.createFile(localFileDirPath.resolve("test.csv"));

		try (StringWriter writer = new StringWriter()) {

			addAppender(writer, MOCK_APPENDER, Level.INFO);

			List<File> targetFiles = logic.getTargetFileList();

			assertEquals(0, targetFiles.size());

		}

		this.deleteTempFile();

	}

	@Test
	public void fail_removeTailingSpace() throws Exception {
		StringWriter writer = new StringWriter();
		addAppender(writer, MOCK_APPENDER, Level.INFO);

		Files.deleteIfExists(localFileDirPath.resolve("report202312042301.csv"));
		Files.createFile(localFileDirPath.resolve("report202312042301.csv"));

		Path filepath = Paths.get(LOCAL_DIRECTORY_PATH + "report202312042301.csv");

		try {
			try (MockedConstruction<FileWriter> mocked = Mockito.mockConstructionWithAnswer(FileWriter.class,
					invocation -> {
						throw new IOException();
					})) {
				logic.removeTailingSpace(filepath.toFile());

			}
		} catch (Exception ex) {

		}

		assertTrue(!writer.toString()
				.contains(LOG_HEADER + "Delete half a line for completion of file: report202312042301.csv"));
		this.deleteTempFile();

	}

	@Test
	public void fail_readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE_readOcountError() throws Exception {
		StringWriter writer = new StringWriter();
		addAppender(writer, MOCK_APPENDER, Level.INFO);

		Files.deleteIfExists(localFileDirPath.resolve("report202312042301.csv"));
		Files.createFile(localFileDirPath.resolve("report202312042301.csv"));

		Path filepath = Paths.get(LOCAL_DIRECTORY_PATH + "report202312042301.csv");

		try (BufferedWriter br = new BufferedWriter(new FileWriter(filepath.toString(), StandardCharsets.UTF_8))) {

			br.write("No.,正誤,分野名,大分類,中分類,出典,学習日");
			br.newLine(); // 換行

			// 寫入第二行數據
			br.write("yyyy");

		}

		try {

			logic.readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE();

		} catch (Exception ex) {

		}

		this.deleteTempFile();

	}

	@Test
	public void fail_readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE_readAVGError() throws Exception {
		StringWriter writer = new StringWriter();
		addAppender(writer, MOCK_APPENDER, Level.INFO);

		Files.deleteIfExists(localFileDirPath.resolve("report202312042301.csv"));
		Files.createFile(localFileDirPath.resolve("report202312042301.csv"));

		Path filepath = Paths.get(LOCAL_DIRECTORY_PATH + "report202312042301.csv");

		List<File> targetAVGFiles = new ArrayList<>();

		when(logic.getAVGFileList()).thenReturn(targetAVGFiles);

		try (BufferedWriter br = new BufferedWriter(new FileWriter(filepath.toString(), StandardCharsets.UTF_8))) {

			br.write("No.,正誤,分野名,大分類,中分類,出典,学習日");
			br.newLine(); // 換行

			// 寫入第二行數據
			br.write("yyyy");

		}

		targetAVGFiles.add(filepath.toFile());

		try {

			logic.readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE();

		} catch (Exception ex) {

		}

		this.deleteTempFile();

	}

	@Test
	public void fail_readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE_writeTotalAverageToFileError()
			throws Exception {
		StringWriter writer = new StringWriter();
		addAppender(writer, MOCK_APPENDER, Level.INFO);

		Files.deleteIfExists(localFileDirPath.resolve("report202312042301.csv"));
		Files.createFile(localFileDirPath.resolve("report202312042301.csv"));

		Path filepath = Paths.get(LOCAL_DIRECTORY_PATH + "report202312042301.csv");

		doThrow(new IOException()).when(logic).writeTotalAverageToFile(any());

		try (BufferedWriter br = new BufferedWriter(new FileWriter(filepath.toString(), StandardCharsets.UTF_8))) {

			br.write("No.,正誤,分野名,大分類,中分類,出典,学習日");
			br.newLine(); // 換行

			// 寫入第二行數據
			br.write(
					"1,○,テクノロジ系,基礎理論,基礎理論,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/17_aki/q5.html\"\",\"\"平成17年秋期 問5\"\")\",2023/12/4");
			br.newLine(); // 換行

			// 寫入第3行數據
			br.write(
					"2,○,テクノロジ系,基礎理論,アルゴリズムとプログラミング,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/17_haru/q13.html\"\",\"\"平成17年春期 問13\"\")\",2023/12/4");
			br.newLine(); // 換行

			// 寫入第4行數據
			br.write(
					"3,×,テクノロジ系,基礎理論,基礎理論,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/20_haru/q6.html\"\",\"\"平成20年春期 問6\"\")\",2023/12/4");
			br.newLine(); // 換行

			// 寫入第5行數據
			br.write(
					"4,×,テクノロジ系,基礎理論,基礎理論,\"=HYPERLINK(\"\"https://www.fe-siken.com/kakomon/29_aki/q3.html\"\",\"\"平成29年秋期 問3\"\")\",2023/12/4");
			br.newLine(); // 換行
			br.write(" ");

		}

		try {

			logic.readCSVFileAndWriteAVGToNewCsvAndCreateTotalAVGfILE();

		} catch (Exception ex) {

		}

		this.deleteTempFile();

	}

	private void addAppender(StringWriter writer, String name, Level level) {
		final LoggerContext context = LoggerContext.getContext(false);
		final Configuration config = context.getConfiguration();
		final PatternLayout layout = PatternLayout.createDefaultLayout(config);

		Appender appender = WriterAppender.createAppender(layout, null, writer, name, false, true);
		appender.start();
		config.addAppender(appender);
		updateLoggers(appender, config, level);

	}

	private void updateLoggers(Appender appender, Configuration config, Level level) {
		for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
			loggerConfig.addAppender(appender, level, null);

		}
		config.getRootLogger().addAppender(appender, level, null);
	}

	private void removeAppender() {
		final LoggerContext context = LoggerContext.getContext(false);
		final Configuration config = context.getConfiguration();

		for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
			loggerConfig.removeAppender(MOCK_APPENDER);

		}
		config.getRootLogger().removeAppender(MOCK_APPENDER);

	}

	private void deleteTempFile() throws IOException {

		if (Files.exists(localFileDirPath)) {
			Files.newDirectoryStream(localFileDirPath).forEach(file -> {
				try {
					Files.delete(file);

				} catch (IOException ioex) {
					ioex.getMessage();
				}
			});
		}

	}

}
