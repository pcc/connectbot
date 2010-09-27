package org.connectbot.transport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3DirectoryEntry;
import com.trilead.ssh2.SFTPv3FileAttributes;

public class SFTPFileTransferSession implements FileTransferSession {

	SFTPv3Client sftp;
	String currentDirectory;

	public SFTPFileTransferSession(SFTPv3Client sftp) throws IOException {
		this.sftp = sftp;
		currentDirectory = sftp.canonicalPath(".");
	}

	public void cd(String path) throws IOException {
		String nextDir;
		if (path.charAt(0) == '/')
			nextDir = path;
		else
			nextDir = sftp.canonicalPath(currentDirectory + "/" + path);
		SFTPv3FileAttributes attr = sftp.stat(nextDir);
		if (!attr.isDirectory())
			throw new IOException("Target not a directory");
		currentDirectory = nextDir;
	}

	private FileInfo asFileInfo(SFTPv3DirectoryEntry entry) {
		FileInfo info = new FileInfo();
		info.name = entry.filename;
		info.isDirectory = entry.attributes.isDirectory();
		return info;
	}

	public FileInfo[] ls() throws IOException {
		Vector<SFTPv3DirectoryEntry> list = sftp.ls(currentDirectory);
		ArrayList<FileInfo> infos = new ArrayList<FileInfo>();
		for (SFTPv3DirectoryEntry entry : list) {
			infos.add(asFileInfo(entry));
		}
		return infos.toArray(new FileInfo[infos.size()]);
	}
}
