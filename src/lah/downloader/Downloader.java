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
		public void onReceive(Context ctx, Intent intent) {
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(download_id);
			Cursor cursor = download_manager.query(query);
			if (cursor.moveToFirst()) {
				int columnIndex = cursor
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = cursor.getInt(columnIndex);

				// set the flag download_finish appropriately
				switch (status) {
				// download is not done
				case DownloadManager.STATUS_RUNNING:
				case DownloadManager.STATUS_PAUSED:
				case DownloadManager.STATUS_PENDING:
					break;
				// download is done
				case DownloadManager.STATUS_SUCCESSFUL:
				case DownloadManager.STATUS_FAILED:
				default:
					context.unregisterReceiver(this);
					download_finish = true;
				}
			}
		}

	}

	private static final IntentFilter intent_filter = new IntentFilter(
			DownloadManager.ACTION_DOWNLOAD_COMPLETE);

	private final Context context;

	private boolean download_finish;

	private long download_id;

	private final DownloadManager download_manager;

	private final BroadcastReceiver download_receiver;

	public Downloader(Context ctx) {
		context = ctx;
		download_manager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);
		download_receiver = new DownloadBroadcastReceiver();
	}

	/**
	 * Download a file
	 * 
	 * @param uri
	 *            URI of the file
	 * @param file_name
	 *            The name of the download file
	 * @return a {@link File} representing the download result or
	 *         {@literal null} if the download fails
	 * @throws Exception
	 */
	public File downloadFile(String uri, String file_name) throws Exception {
		// The expected result download file
		File download_result = new File(context.getExternalFilesDir(null) + "/"
				+ file_name).getAbsoluteFile();

		// Return the file if it is already there
		if (download_result.exists())
			return download_result;

		// File is not there yet, request it
		Request request = new Request(Uri.parse(uri));
		request.setTitle(file_name);
		Uri download_result_uri = Uri.fromFile(download_result);
		request.setDestinationUri(download_result_uri);
		download_id = download_manager.enqueue(request);
		context.registerReceiver(download_receiver, intent_filter);

		// Waiting until the download is done (successful|fail)
		download_finish = false;
		while (!download_finish) {
			Thread.yield();
		}

		// Download result does not exist means failure
		return download_result.exists() ? download_result : null;
	}

}
