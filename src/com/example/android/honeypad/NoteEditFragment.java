
package com.example.android.honeypad;

import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.honeypad.widget.WidgetProvider;

public class NoteEditFragment extends Fragment {

	private EditText mTitleText;
	private EditText mBodyText;

	// expose the currently displayed note
	protected Uri mCurrentNote;

	// default constructor
	public NoteEditFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.note_edit, container, false);

		mTitleText = (EditText) v.findViewById(R.id.title);
		mBodyText = (EditText) v.findViewById(R.id.body);
		Button confirmButton = (Button) v.findViewById(R.id.confirm);
		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				saveNote();
			}
		});

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(NotesProvider.KEY_ID)) {
			mCurrentNote = Uri.parse((String) savedInstanceState
					.getString(NotesProvider.KEY_ID));
		}

		populateFields();
		return v;
	}

	/**
	 * Display a particular note in this fragment.
	 * 
	 * @param noteUri
	 *            The Uri of the note to display
	 */
	protected void loadNote(Uri noteUri) {
		mCurrentNote = noteUri;
		if (isAdded()) {
			populateFields();
		}
	}

	/**
	 * Clear all fields on this fragment.
	 */
	protected void clear() {
		mTitleText.setText(null);
		mBodyText.setText(null);
		mCurrentNote = null;
	}

	/**
	 * Helper method which retrieves & displays the content of the current note.
	 */
	private void populateFields() {
		if (mCurrentNote != null) {

			Cursor c = null;
			try {
				c = getActivity().getContentResolver().query(mCurrentNote,
						null, null, null, null);
				if (c.moveToFirst()) {
					mTitleText.setText(c.getString(NotesProvider.TITLE_COLUMN));
					mBodyText.setText(c.getString(NotesProvider.BODY_COLUMN));
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mCurrentNote != null) {
			outState.putString(NotesProvider.KEY_ID, mCurrentNote.toString());
		}
	}

	/**
	 * Persists the details of the current note. This will either create a new
	 * note, or update an existing note.
	 */
	private void saveNote() {
		ContentValues values = new ContentValues(2);
		values.put(NotesProvider.KEY_TITLE, mTitleText.getText().toString());
		values.put(NotesProvider.KEY_BODY, mBodyText.getText().toString());
		final boolean updating = mCurrentNote != null;
		if (updating) {
			getActivity().getContentResolver().update(mCurrentNote, values,
					null, null);
		} else {
			Uri newNote = getActivity().getContentResolver().insert(
					NotesProvider.CONTENT_URI, values);

			if (newNote != null) {
				mCurrentNote = newNote;
			}
		}

		// show a toast confirmation
		Toast.makeText(getActivity(),
				updating ? R.string.note_updated : R.string.note_saved,
				Toast.LENGTH_SHORT).show();

		// update widget
		AppWidgetManager awm = AppWidgetManager.getInstance(getActivity());
		awm.notifyAppWidgetViewDataChanged(awm
				.getAppWidgetIds(new ComponentName(getActivity(),
						WidgetProvider.class)), R.id.stack_view);
	}

}
