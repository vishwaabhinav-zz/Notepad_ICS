package com.example.android.honeypad.widget;

import java.util.ArrayList;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.honeypad.NotepadActivity;
import com.example.android.honeypad.NotesProvider;
import com.example.android.honeypad.R;

public class WidgetService extends RemoteViewsService {

	private class StackRemoteViewsFactory implements
			RemoteViewsService.RemoteViewsFactory {
		private static final int COUNT = 10;
		private List<WidgetItem> mWidgetItems = new ArrayList<WidgetItem>();
		private Context mContext;
		private int mAppWidgetId;

		public StackRemoteViewsFactory(Context context, Intent intent) {
			mContext = context;
			mAppWidgetId = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		public void onCreate() {
			/*
			 * In onCreate() you setup any connections / cursors to your data
			 * source. Heavy lifting, for example downloading or creating
			 * content etc, should be deferred to getViewAt() or
			 * onDataSetChanged(). Taking more than 20 seconds in this call will
			 * result in an ANR.
			 */
			update();
		}

		private void update() {
			Cursor c = null;
			mWidgetItems.clear();
			try {
				c = getContentResolver().query(
						NotesProvider.CONTENT_URI,
						new String[] { NotesProvider.KEY_ID,
								NotesProvider.KEY_TITLE }, null, null,
						NotesProvider.KEY_TITLE + " COLLATE LOCALIZED ASC");
				if (c.moveToFirst()) {
					do {
						WidgetItem note = new WidgetItem(
								c.getLong(NotesProvider.ID_COLUMN),
								c.getString(NotesProvider.TITLE_COLUMN));
						mWidgetItems.add(note);
					} while (c.moveToNext() && mWidgetItems.size() < COUNT);
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}

		public void onDestroy() {
			/*
			 * In onDestroy() you should tear down anything that was setup for
			 * your data source, eg. cursors, connections, etc.
			 */
			mWidgetItems.clear();
		}

		public int getCount() {
			return mWidgetItems.size();
		}

		public RemoteViews getViewAt(int position) {

			// position will always range from 0 to getCount() - 1.
			if (position < 0 || position > mWidgetItems.size()) {
				return null;
			}

			/*
			 * We construct a remote views item based on our widget item xml
			 * file, and set the text based on the position.
			 */
			RemoteViews rv = new RemoteViews(mContext.getPackageName(),
					R.layout.widget_item);
			WidgetItem note = mWidgetItems.get(position);
			rv.setTextViewText(R.id.widget_item, note.title);

			// Next, we set an intent so that clicking on this view will result
			// in a toast message
			Bundle extras = new Bundle();
			extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			extras.putLong(NotepadActivity.EXTRA_NOTE_ID, note.id);
			Intent fillInIntent = new Intent();
			fillInIntent.putExtras(extras);
			rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

			/*
			 * You can do heaving lifting in here, synchronously. For example,
			 * if you need to process an image, fetch something from the
			 * network, etc., it is ok to do it here, synchronously. A loading
			 * view will show up in lieu of the actual contents in the interim.
			 */

			// Return our remote views object.
			return rv;
		}

		public RemoteViews getLoadingView() {
			/*
			 * You can create a custom loading view (for instance when
			 * getViewAt() is slow. If you return null here, you will get the
			 * default loading view.
			 */
			return null;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public long getItemId(int position) {
			return mWidgetItems.get(position).id;
		}

		public boolean hasStableIds() {
			return true;
		}

		public void onDataSetChanged() {
			/*
			 * This is triggered when you call AppWidgetManager
			 * notifyAppWidgetViewDataChanged on the collection view
			 * corresponding to this factory. You can do heaving lifting in
			 * here, synchronously. For example, if you need to process an
			 * image, fetch something from the network, etc., it is ok to do it
			 * here, synchronously. The widget will remain in its current state
			 * while work is being done here, so you don't need to worry about
			 * locking up the widget.
			 */
			update();
		}
	}

	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}
