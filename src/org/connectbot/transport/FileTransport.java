package org.connectbot.transport;

import java.io.IOException;

public interface FileTransport {

	String startingDirectory() throws IOException;
	FileInfo stat(String dir) throws IOException;
	FileInfo[] ls(String dir) throws IOException;
	String realpath(String pwd, String dir) throws IOException;

}
