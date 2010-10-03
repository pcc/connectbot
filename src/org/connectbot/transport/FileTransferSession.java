package org.connectbot.transport;

import java.io.IOException;

public interface FileTransferSession {

	void cd(String dir) throws IOException;
	String pwd();
	FileInfo[] ls() throws IOException;

}
