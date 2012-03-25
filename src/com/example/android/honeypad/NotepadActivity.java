package com.example.android.honeypad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.honeypad.NoteListFragment.NoteListEventsCallback;

public class NotepadActivity extends Activity implements NoteListEventsCallback {

	// action to launch straight to a specific note
	public static final String ACTION_VIEW_NOTE = "com.example.android.honeypad.ACTION_VIEW_NOTE";

	// extra for the above action
	public static final String EXTRA_NOTE_ID = "noteId";

	// key for adding NoteEditFragment to this Activity
	private static final String NOTE_EDIT_TAG = "Edit";
	
	// for Property Animation
	private static final String ROTATION_AXIS_PROP = "rotationY";
	private static final int ROTATION_HALF_DURATION = 250;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notepad);
		if (ACTION_VIEW_NOTE.equals(getIntent().getAction())) {
			viewNote(getIntent());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (ACTION_VIEW_NOTE.equals(intent.getAction())) {
			viewNote(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.notepad_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_note:
			showNote(null);
			NoteListFragment list = (NoteListFragment) getFragmentManager()
					.findFragmentById(R.id.list);
			list.clearActivation();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void viewNote(Intent launchIntent) {
		final long noteId = launchIntent.getLongExtra(EXTRA_NOTE_ID, -1);
		showNote(ContentUris.withAppendedId(NotesProvider.CONTENT_URI, noteId));
		NoteListFragment list = (NoteListFragment) getFragmentManager()
				.findFragmentById(R.id.list);
		list.setActivatedNote(noteId);
	}

	/**
	 * 
	 * This method controls both fragments, instructing them to display a
	 * certain note.
	 * 
	 * @param noteUri
	 *            The {@link Uri} of the note to show. To create a new note,
	 *            pass {@code null}.
	 */
	private void showNote(final Uri noteUri) {
		// check if the NoteEditFragment has been added
		FragmentManager fm = getFragmentManager();
		NoteEditFragment edit = (NoteEditFragment) fm
				.findFragmentByTag(NOTE_EDIT_TAG);
		final boolean editNoteAdded = (edit != null);

		if (editNoteAdded) {
			if (edit.mCurrentNote != null && edit.mCurrentNote.equals(noteUri)) {
				// clicked on the currently selected note
				return;
			}

			// animate the note transition. We do this in 3 steps:
			// 1. Rotate out the current note
			// 2. Switch the data to the new note
			// 3. Rotate in the new note
			ObjectAnimator anim = ObjectAnimator.ofFloat(edit.getView(),
					ROTATION_AXIS_PROP, 0, 90).setDuration(
					ROTATION_HALF_DURATION);
			anim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					NoteEditFragment editFrag = (NoteEditFragment) getFragmentManager()
							.findFragmentByTag(NOTE_EDIT_TAG);
					if (noteUri != null) {
						// load an existing note
						editFrag.loadNote(noteUri);
					} else {
						// creating a new note - clear the form & list
						// activation
						if (editNoteAdded) {
							editFrag.clear();
						}
						NoteListFragment list = (NoteListFragment) getFragmentManager()
								.findFragmentById(R.id.list);
						list.clearActivation();
					}
					// rotate in the new note
					ObjectAnimator.ofFloat(editFrag.getView(),
							ROTATION_AXIS_PROP, -90, 0).start();
				}
			});
			anim.start();
		} else {
			// add the NoteEditFragment to the container
			FragmentTransaction ft = fm.beginTransaction();
			edit = new NoteEditFragment();
			ft.add(R.id.note_detail_container, edit, NOTE_EDIT_TAG);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.commit();
			edit.loadNote(noteUri);
		}
	}

	/**
	 * Callback from child fragment
	 */
	public void onNoteSelected(Uri noteUri) {
		showNote(noteUri);
	}

	/**
	 * Callback from child fragment
	 */
	public void onNoteDeleted() {
		// remove the NoteEditFragment after a deletion
		FragmentManager fm = getFragmentManager();
		NoteEditFragment edit = (NoteEditFragment) fm
				.findFragmentByTag(NOTE_EDIT_TAG);
		if (edit != null) {
			FragmentTransaction ft = fm.beginTransaction();
			ft.remove(edit);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}
	}
}
