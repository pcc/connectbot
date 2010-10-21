package org.connectbot.transport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Vector;

import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3DirectoryEntry;
import com.trilead.ssh2.SFTPv3FileAttributes;
import com.trilead.ssh2.SFTPv3FileHandle;

public class SFTPFileTransport implements FileTransport {

	SFTPv3Client sftp;

	public SFTPFileTransport(SFTPv3Client sftp) throws IOException {
		this.sftp = sftp;
	}

	public String startingDirectory() throws IOException {
		return sftp.canonicalPath(".");
	}

	public String realpath(String pwd, String dir) throws IOException {
		return sftp.canonicalPath(pwd + "/" + dir);
	}

	public FileInfo stat(String path) throws IOException {
		SFTPv3FileAttributes attr = sftp.stat(path);
		return asFileInfo(attr);
	}

	private FileInfo asFileInfo(SFTPv3FileAttributes entry) {
		FileInfo info = new FileInfo();
		info.isDirectory = entry.isDirectory();
		info.size = entry.size;
		info.permissions = entry.permissions;
		if (entry.mtime != null)
			info.lastModified = new Long(entry.mtime.longValue() * 1000);
		return info;
	}

	private FileInfo asFileInfo(SFTPv3DirectoryEntry entry) {
		FileInfo info = asFileInfo(entry.attributes);
		info.name = entry.filename;
		return info;
	}

	public FileInfo[] ls(String dir) throws IOException {
		Vector<SFTPv3DirectoryEntry> list = sftp.ls(dir);
		ArrayList<FileInfo> infos = new ArrayList<FileInfo>();
		for (SFTPv3DirectoryEntry entry : list) {
			infos.add(asFileInfo(entry));
		}
		return infos.toArray(new FileInfo[infos.size()]);
	}

	public void put(String path, InputStream in, FileProgressListener progress) throws IOException {
		SFTPv3FileHandle fh = sftp.createFileTruncate(path);

		byte buf[] = new byte[32768];
		long ofs = 0;
		while (true) {
			if (progress != null) progress.onFileProgress(ofs);
			int len = in.read(buf);
			if (len <= 0)
				break;

			sftp.write(fh, ofs, buf, 0, len);
			ofs += len;
		}

		sftp.closeFile(fh);
	}

	public void get(String path, OutputStream out, FileProgressListener progress) throws IOException {
		SFTPv3FileHandle fh = sftp.openFileRO(path);

		byte buf[] = new byte[32768];
		long ofs = 0;
		while (true) {
			if (progress != null) progress.onFileProgress(ofs);
			int len = sftp.read(fh, ofs, buf, 0, 32768);
			if (len <= 0)
				break;

			out.write(buf, 0, len);
			ofs += len;
		}

		sftp.closeFile(fh);
	}

}
