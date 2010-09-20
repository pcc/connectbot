package org.connectbot.transport;

public interface FileTransferSession {

	void cd(String dir);
	FileInfo[] ls(String dir);

}
