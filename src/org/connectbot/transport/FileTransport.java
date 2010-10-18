package org.connectbot.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileTransport {

	String startingDirectory() throws IOException;
	FileInfo stat(String dir) throws IOException;
	FileInfo[] ls(String dir) throws IOException;
	String realpath(String pwd, String dir) throws IOException;

	void put(String path, InputStream in) throws IOException;
	void get(String path, OutputStream out) throws IOException;

}
