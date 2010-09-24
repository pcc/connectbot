package org.connectbot.transport;

import java.io.IOException;

public interface FileTransferSession {

	void cd(String dir) throws IOException;
	FileInfo[] ls(String dir) throws IOException;

}
