package org.connectbot.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.connectbot.service.ConnectionNotifier.FileTransferNotification;
import org.connectbot.transport.FileTransport;
import org.connectbot.transport.FileProgressListener;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;

public class FileTransferTask extends AsyncTask<String, Long, String> implements FileProgressListener {

	public static final boolean UPLOAD = true;
	public static final boolean DOWNLOAD = false;

	protected Service context;
	protected TerminalBridge bridge;
	protected String changePath;
	protected String filename;
	protected long size;
	protected boolean isUpload;
	protected FileTransferNotification notification;

	public FileTransferTask(Service context, TerminalBridge bridge, String changePath, String filename, long size, boolean isUpload) {
		this.context = context;
		this.bridge = bridge;
		this.changePath = changePath;
		this.filename = filename;
		this.size = size;
		this.isUpload = isUpload;
	}

	protected void onPreExecute() {
		notification = ConnectionNotifier.getInstance().showFileTransferNotification(context, bridge.host, filename, isUpload);
	}

	protected String doInBackground(String... paths) {
		String localPath = paths[0], remotePath = paths[1];
		FileTransport fileTransport = null;
		String result;
		try {
			fileTransport = bridge.getFileTransport();
			if (isUpload) {
				FileInputStream in = new FileInputStream(localPath);
				fileTransport.put(remotePath, in, this);
			} else {
				FileOutputStream out = new FileOutputStream(localPath);
				fileTransport.get(remotePath, out, this);
			}

			result = null;
		} catch (IOException e) {
			result = e.toString();
		} finally {
			if (fileTransport != null)
				bridge.releaseFileTransport(fileTransport);
		}
		return result;
	}

	public void onFileProgress(long bytes) {
		publishProgress(new Long(bytes));
	}

	public void onProgressUpdate(Long... bytes) {
		notification.update(bytes[0], size);
	}

	protected void onPostExecute(String result) {
		ConnectionNotifier.getInstance().hideFileTransferNotification(context, notification);

		Intent fileListChanged = new Intent("org.connectbot.FileListChanged");
		if (isUpload) {
			fileListChanged.putExtra("remoteHost", bridge.host.getId());
			fileListChanged.putExtra("remotePath", changePath);
		} else {
			fileListChanged.putExtra("localPath", changePath);
		}
		context.sendBroadcast(fileListChanged);
	}

}
