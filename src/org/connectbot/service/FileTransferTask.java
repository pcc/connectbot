package org.connectbot.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.connectbot.service.ConnectionNotifier.FileTransferNotification;
import org.connectbot.transport.FileTransport;

import android.app.Service;
import android.os.AsyncTask;

public class FileTransferTask extends AsyncTask<String, Long, Boolean> {

	public static final boolean UPLOAD = true;
	public static final boolean DOWNLOAD = false;

	protected Service context;
	protected TerminalBridge bridge;
	protected String filename;
	protected boolean isUpload;
	protected FileTransferNotification notification;

	public FileTransferTask(Service context, TerminalBridge bridge, String filename, boolean isUpload) {
		this.context = context;
		this.bridge = bridge;
		this.filename = filename;
		this.isUpload = isUpload;
	}

	protected void onPreExecute() {
		notification = ConnectionNotifier.getInstance().showFileTransferNotification(context, bridge.host, filename, isUpload);
	}

	protected Boolean doInBackground(String... paths) {
		String localPath = paths[0], remotePath = paths[1];
		FileTransport fileTransport = null;
		boolean result;
		try {
			fileTransport = bridge.getFileTransport();
			if (isUpload) {
				FileInputStream in = new FileInputStream(localPath);
				fileTransport.put(remotePath, in);
			} else {
				FileOutputStream out = new FileOutputStream(localPath);
				fileTransport.get(remotePath, out);
			}

			result = true;
		} catch (IOException e) {
			result = false;
		} finally {
			if (fileTransport != null)
				bridge.releaseFileTransport(fileTransport);
		}
		return Boolean.valueOf(result);
	}

	protected void onPostExecute(Boolean result) {
		ConnectionNotifier.getInstance().hideFileTransferNotification(context, notification);
	}

}
