package org.connectbot.transport;

import java.io.IOException;

public interface FileTransferSession {

	void cd(String dir) throws IOException;
	FileInfo[] ls() throws IOException;

}
