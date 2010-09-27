package org.connectbot.transport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LocalFileTransferSession implements FileTransferSession {

	File currentDirectory;

	protected void assertDir(File dir) throws IOException {
		if (!dir.isDirectory())
			throw new IOException("Target is not a directory");
	}

	public LocalFileTransferSession(File startingDirectory) throws IOException {
		assertDir(startingDirectory);
		currentDirectory = startingDirectory;
	}

	protected File relFile(String path) throws IOException {
		return new File(currentDirectory, path).getCanonicalFile();
	}

	public void cd(String path) throws IOException {
		File nextDir = relFile(path);
		assertDir(nextDir);
		currentDirectory = nextDir;
	}

	protected FileInfo asFileInfo(File f) {
		FileInfo fi = new FileInfo();
		fi.name = f.getName();
		fi.isDirectory = f.isDirectory();
		return fi;
	}

	public FileInfo[] ls() throws IOException {
		File files[] = currentDirectory.listFiles();
		ArrayList<FileInfo> infos = new ArrayList<FileInfo>();
		for (File f : files) {
			infos.add(asFileInfo(f));
		}
		return infos.toArray(new FileInfo[infos.size()]);
	}

}
