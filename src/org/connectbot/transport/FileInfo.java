package org.connectbot.transport;

public class FileInfo implements Comparable<FileInfo> {

	public String name;
	public boolean isDirectory;

	public String toString() {
		return name;
	}

	public int compareTo(FileInfo other) {
		if (isDirectory && !other.isDirectory) return -1;
		if (!isDirectory && other.isDirectory) return 1;
		return name.compareTo(other.name);
	}

}
