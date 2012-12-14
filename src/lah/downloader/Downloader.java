package lah.downloader;

import java.io.File;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

public class Downloader {

	private class DownloadBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// System.out.println("Notification received.");
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(download_id);
			Cursor cursor = manager.query(query);
			if (cursor.moveToFirst()) {
				int columnIndex = cursor
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = cursor.getInt(columnIndex);
				// int columnReason = cursor
				// .getColumnIndex(DownloadManager.COLUMN_REASON);
				// int reason = cursor.getInt(columnReason);
				if (status == DownloadManager.STATUS_RUNNING) {
					// System.out.println("Running");
				} else if (status == DownloadManager.STATUS_PAUSED) {
					// System.out.println("Pause: " + reason);
				} else if (status == DownloadManager.STATUS_PENDING) {
					// System.out.println("Pending");
				} else {
					if (status == DownloadManager.STATUS_SUCCESSFUL) {
						Uri local_uri = manager
								.getUriForDownloadedFile(download_id);
						if (local_uri != null) {
							// System.out
							// .println("Download successfully completes: "
							// + local_uri);
							original_download_file = new File(
									local_uri.getPath());
						} else
							original_download_file = null;
					} else if (status == DownloadManager.STATUS_FAILED) {
						// System.out.println("Fail: " + reason);
						original_download_file = null;
					}
					context.unregisterReceiver(this);
					download_finish = true;
				}
			}
		}

	}

	Context context;

	boolean download_finish;

	long download_id;

	File download_result;

	File original_download_file;

	private BroadcastReceiver downloadReceiver = new DownloadBroadcastReceiver();

	DownloadManager manager;

	public Downloader(Context ctx) {
		context = ctx;
		manager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);
	}

	public File downloadFile(String uri, String file_name) throws Exception {
		download_result = new File(context.getExternalFilesDir(null) + "/"
				+ file_name);
		if (download_result.exists())
			return download_result;

		Request request = new Request(Uri.parse(uri));
		request.setTitle(file_name);
		request.setDestinationInExternalFilesDir(context, null, "/" + file_name);
		download_id = manager.enqueue(request);
		// System.out.println(download_id);
		IntentFilter intent_filter = new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		context.registerReceiver(downloadReceiver, intent_filter);
		download_finish = false;
		while (!download_finish)
			Thread.sleep(500);
		if (original_download_file != null) {
			if (!original_download_file.getAbsolutePath().equals(
					download_result.getAbsolutePath()))
				// System.out.println("Rename file: " + original_download_file
				// + " to " + download_result + " : "
				// + );
				original_download_file.renameTo(download_result);
		} else
			download_result = null;
		// System.out.println(download_result.exists());
		return download_result;
	}

}
