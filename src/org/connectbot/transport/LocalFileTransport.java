package org.connectbot.transport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class LocalFileTransport implements FileTransport {

	public LocalFileTransport() throws IOException {
	}

	public String startingDirectory() throws IOException {
		return "/";
	}

	public FileInfo stat(String path) throws IOException {
		return asFileInfo(new File(path));
	}

	public String realpath(String pwd, String path) throws IOException {
		return new File(pwd, path).getCanonicalFile().toString();
	}

	protected FileInfo asFileInfo(File f) {
		FileInfo fi = new FileInfo();
		fi.name = f.getName();
		fi.isDirectory = f.isDirectory();
		fi.size = new Long(f.length());
		return fi;
	}

	public FileInfo[] ls(String path) throws IOException {
		File files[] = new File(path).listFiles();
		if (files == null)
			throw new IOException("Unable to read directory");
		ArrayList<FileInfo> infos = new ArrayList<FileInfo>();
		for (File f : files) {
			infos.add(asFileInfo(f));
		}
		return infos.toArray(new FileInfo[infos.size()]);
	}

	public void put(String path, InputStream in) throws IOException {}
	public void get(String path, OutputStream out) throws IOException {}

}
