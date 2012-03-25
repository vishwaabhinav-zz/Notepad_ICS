package com.example.android.honeypad;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.example.android.honeypad.widget.WidgetProvider;

public class NoteListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	// containing Activity must implement this interface
	public interface NoteListEventsCallback {
		public void onNoteSelected(Uri noteUri);

		public void onNoteDeleted();
	}

	// key for saving state
	private static final String KEY_CURRENT_ACTIVATED = "KEY_CURRENT_ACTIVATED";

	// the id of our loader
	private static final int LOADER_ID = 0;

	// This is the Adapter being used to display the list's data.
	private SimpleCursorAdapter mAdapter;

	// callback for notifying container of events
	private NoteListEventsCallback mContainerCallback;

	// track the currently activated item
	private int mCurrentActivePosition = ListView.INVALID_POSITION;

	// track if we need to set a note to activated once data is loaded
	private long mNoteIdToActivate = -1;

	// default constructor
	public NoteListFragment() {

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getActivity().getString(R.string.no_notes));

		// create an empty adapter, our Loader will retrieve the data
		// asynchronously
		mAdapter = new ActivatedCursorAdapter(getActivity(),
				R.layout.note_list_row, null,
				new String[] { NotesProvider.KEY_TITLE },
				new int[] { android.R.id.text1 }, 0);
		setListAdapter(mAdapter);

		// setup our list view
		final ListView notesList = getListView();
		notesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		notesList.setMultiChoiceModeListener(mNoteSelectionModeCallback);

		// restore any saved state
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(KEY_CURRENT_ACTIVATED)) {
			mCurrentActivePosition = savedInstanceState.getInt(
					KEY_CURRENT_ACTIVATED, ListView.INVALID_POSITION);
		}

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// check that the containing activity implements our callback
			mContainerCallback = (NoteListEventsCallback) activity;
		} catch (ClassCastException e) {
			activity.finish();
			throw new ClassCastException(activity.toString()
					+ " must implement NoteSelectedCallback");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_CURRENT_ACTIVATED, mCurrentActivePosition);
	}

	/**
	 * Helper method to set the activation state of a note
	 * 
	 * @param noteId
	 *            The id of the note to be activated
	 */
	protected void setActivatedNote(long noteId) {
		if (mAdapter != null) {
			// work out the position in the list of note with the given id
			final int N = mAdapter.getCount();
			for (int position = 0; position < N; position++) {
				if (mAdapter.getItemId(position) == noteId) {
					if (position != mCurrentActivePosition) {
						clearActivation();
						mCurrentActivePosition = position;
						View row = getListView().getChildAt(position);
						if (row != null) {
							row.setActivated(true);
						}
					}
					break;
				}
			}
		} else {
			// if we have not loaded our cursor yet then store the note id
			// for now & activate once loaded
			mNoteIdToActivate = noteId;
		}
	}

	/**
	 * Helper method to clear the list's activated state
	 */
	protected void clearActivation() {
		if (mCurrentActivePosition != ListView.INVALID_POSITION) {
			getListView().getChildAt(mCurrentActivePosition)
					.setActivated(false);
		}
		mCurrentActivePosition = ListView.INVALID_POSITION;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		return new CursorLoader(getActivity(), NotesProvider.CONTENT_URI,
				new String[] { NotesProvider.KEY_ID, NotesProvider.KEY_TITLE },
				null, null, NotesProvider.KEY_TITLE + " COLLATE LOCALIZED ASC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		// check if we need to set one of the (now loaded) notes as activated
		if (mNoteIdToActivate > -1) {
			setActivatedNote(mNoteIdToActivate);
			mNoteIdToActivate = -1;
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		mCurrentActivePosition = position;
		mContainerCallback.onNoteSelected(ContentUris.withAppendedId(
				NotesProvider.CONTENT_URI, id));
	}

	private ListView.MultiChoiceModeListener mNoteSelectionModeCallback = new ListView.MultiChoiceModeListener() {

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.notes_list_context, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.delete_notes:
				int deletedCount = 0;

				for (long id : getListView().getCheckItemIds()) {
					deletedCount += getActivity().getContentResolver().delete(
							ContentUris.withAppendedId(
									NotesProvider.CONTENT_URI, id), null, null);
				}

				// clear any selections
				clearActivation();

				// update container
				mContainerCallback.onNoteDeleted();

				// show a toast to confirm delete
				Toast.makeText(
						getActivity(),
						String.format(
								getActivity().getString(R.string.num_deleted),
								deletedCount, (deletedCount == 1 ? "" : "s")),
						Toast.LENGTH_SHORT).show();

				// update widget
				AppWidgetManager awm = AppWidgetManager
						.getInstance(getActivity());
				awm.notifyAppWidgetViewDataChanged(awm
						.getAppWidgetIds(new ComponentName(getActivity(),
								WidgetProvider.class)), R.id.stack_view);

				// clear the contextual action bar
				mode.finish();
			}
			return true;
		}

		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			// update the label to show the number of items selected
			mode.setTitle(String.format(
					getActivity().getString(R.string.num_selected),
					getListView().getCheckedItemCount()));
		}

		public void onDestroyActionMode(ActionMode mode) {
		}
	};

	/**
	 * 
	 * A trivial extension to {@link SimpleCursorAdapter} that sets a specified
	 * item's Activated state.
	 * 
	 */
	private class ActivatedCursorAdapter extends SimpleCursorAdapter {

		public ActivatedCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);
			v.setActivated(position == mCurrentActivePosition);
			return v;
		}

	}

}
