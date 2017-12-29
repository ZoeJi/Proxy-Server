import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Logger {
	BufferedWriter writer;

	public synchronized void write(String content) {
		try {
			writer.write(content);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Logger(String logPath) {
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logPath, true)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
	}
}
