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
			System.out.println("Notification received.");
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(download_id);
			Cursor cursor = manager.query(query);
			if (cursor.moveToFirst()) {
				int columnIndex = cursor
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = cursor.getInt(columnIndex);
				int columnReason = cursor
						.getColumnIndex(DownloadManager.COLUMN_REASON);
				int reason = cursor.getInt(columnReason);
				if (status == DownloadManager.STATUS_RUNNING) {
					System.out.println("Running");
				} else if (status == DownloadManager.STATUS_PENDING) {
					System.out.println("Pending");
				} else {
					download_finish = true;
					if (status == DownloadManager.STATUS_SUCCESSFUL) {
						Uri local_uri = manager
								.getUriForDownloadedFile(download_id);
						if (local_uri != null) {
							System.out
									.println("Download successfully completes: "
											+ local_uri);
							File download_file = new File(local_uri.getPath());
							System.out.println("Rename file: " + download_file + " to " + download_result);
							download_file.renameTo(download_result);
							System.out.println(download_result.exists());
						}
					} else if (status == DownloadManager.STATUS_FAILED) {
						System.out.println("Fail: " + reason);
						download_result = null;
					} else if (status == DownloadManager.STATUS_PAUSED) {
						System.out.println("Pause: " + reason);
						download_result = null;
					}
					context.unregisterReceiver(this);
				}
			}
		}

	}

	Context context;

	boolean download_finish;

	long download_id;

	File download_result;

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
		return download_result;
	}

}
