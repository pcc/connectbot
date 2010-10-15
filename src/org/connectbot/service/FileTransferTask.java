package org.connectbot.service;

import java.io.FileInputStream;
import java.io.IOException;

import org.connectbot.service.ConnectionNotifier.FileTransferNotification;
import org.connectbot.transport.FileTransport;

import android.app.Service;
import android.os.AsyncTask;

public class FileTransferTask extends AsyncTask<String, Long, Boolean> {

	protected Service context;
	protected TerminalBridge bridge;
	protected String filename;
	protected FileTransferNotification notification;

	public FileTransferTask(Service context, TerminalBridge bridge, String filename) {
		this.context = context;
		this.bridge = bridge;
		this.filename = filename;
	}

	protected void onPreExecute() {
		notification = ConnectionNotifier.getInstance().showFileTransferNotification(context, bridge.host, filename, true);
	}

	protected Boolean doInBackground(String... paths) {
		String localPath = paths[0], remotePath = paths[1];
		FileTransport fileTransport = null;
		boolean result;
		try {
			fileTransport = bridge.getFileTransport();
			FileInputStream in = new FileInputStream(localPath);
			fileTransport.put(remotePath, in);

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
