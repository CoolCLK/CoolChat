package me.coolclk.coolchat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication
@RestController
@Component
public class Application {
	public static Logger LOGGER = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	public static void main(String[] args) {
		try {
			copyFile(new File(Application.class.getResource("/default/").getPath()), new File(System.getProperty("user.dir")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		SpringApplication.run(Application.class, args);

		AccountController.checkAccountDatas();

		Object oldAccounts = AccountController.accounts;
		while (true) {
			if (AccountController.accounts != oldAccounts) {
				LOGGER.info("[账号管理器] 数据变化: " + oldAccounts.toString() + " -> " + AccountController.accounts.toString());
				oldAccounts = AccountController.accounts;
			}
		}
	}

	/**
	 * 复制文件。
	 * @author CoolCLK
	 */
	public static void copyFile(File from, File to, Boolean replace) throws IOException {
		if (from.isFile()) {
			if (!to.exists()) {
				to.createNewFile();
			}
			else if (!replace) {
				return;
			}
			FileReader fr = new FileReader(from);
			FileWriter fw = new FileWriter(to);
			int c;
			while ((c = fr.read()) != -1) {
				fw.write((char) c);
			}
			fw.flush();
			fr.close();
			fw.close();
		}
		else if (from.isDirectory()) {
			if (!to.exists()) {
				to.mkdirs();
			}
			File[] files = from.listFiles();
			for (File file : files) {
				copyFile(file, new File(to.getPath() + file.getPath().substring(from.getPath().length())));
			}
		}
	}

	/**
	 * 复制文件但不定义是否覆盖。
	 * @author CoolCLK
	 */
	public static void copyFile(File from, File to) throws IOException {
		copyFile(from, to, false);
	}

	/**
	 * 输出可关闭日志。
	 * @author CoolCLK
	 */
	public static void logAsFullLog(Object log) {
		LOGGER.info(log.toString());
	}
}