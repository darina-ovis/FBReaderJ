/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.preferences;

import java.util.*;

import android.content.*;
import android.app.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.Paths;

public class EditableStringListActivity extends ListActivity {
	public static final String LIST = "list";
	public static final String TITLE = "title";
	public static final String SUGGESTIONS = "suggestions";
	public static final String TYPE = "type";

	public static final String TYPE_FIRST_MAIN = "firstMain";

	private ImageButton myAddButton;
	private Button myOkButton;
	private List<String> mySuggestions;
	private String myType;

	private boolean myUserWasWarned = false;

	private void enableButtons() {
		if (myAddButton != null) {
			myAddButton.setEnabled(!getListAdapter().hasEmpty());
		}
		if (myOkButton != null) {
			myOkButton.setEnabled(!getListAdapter().hasEmpty());
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.editable_stringlist);
		setTitle(getIntent().getStringExtra(TITLE));

		final List<String> list = getIntent().getStringArrayListExtra(LIST);
		mySuggestions = getIntent().getStringArrayListExtra(SUGGESTIONS);
		myType = getIntent().getStringExtra(TYPE);

//		final View bottomView = findViewById(R.id.editable_stringlist_bottom);

		setListAdapter(new ItemAdapter());

		for (String s : list) {
			DirectoryItem i = new DirectoryItem();
			i.setPath(s);
			getListAdapter().addDirectoryItem(i);
		}

		myAddButton = (ImageButton)findViewById(R.id.editable_stringlist_addbutton);
		myAddButton.setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View view) {
					getListAdapter().addDirectoryItem(new DirectoryItem());
				}
			}
		);
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		final View buttonView = findViewById(R.id.editable_stringlist_buttons);
		myOkButton = (Button)buttonView.findViewById(R.id.ok_button);
		final Button cancelButton = (Button)buttonView.findViewById(R.id.cancel_button);
		cancelButton.setText(buttonResource.getResource("cancel").getValue());
		myOkButton.setText(buttonResource.getResource("ok").getValue());

		myOkButton.setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View view) {
					ArrayList<String> paths = new ArrayList<String>();
					for (int i = 0; i < getListAdapter().getCount(); i++) {
						paths.add(getListAdapter().getItem(i).getPath());
					}
					Intent intent = new Intent();
					intent.putStringArrayListExtra(LIST, paths);
					setResult(RESULT_OK, intent);
					finish();
				}
			}
		);
		enableButtons();

		cancelButton.setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View view) {
					setResult(RESULT_CANCELED);
					finish();
				}
			}
		);
	}

	@Override
	public ItemAdapter getListAdapter() {
		return (ItemAdapter)super.getListAdapter();
	}

	private class ItemAdapter extends BaseAdapter {
		private int nextId = 0;

		private final ArrayList<DirectoryItem> myItems = new ArrayList<DirectoryItem>();

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public DirectoryItem getItem(int position) {
			return myItems.get(position);
		}

		synchronized void addDirectoryItem(DirectoryItem i) {
			i.setId(nextId);
			nextId = nextId + 1;
			myItems.add(i);
			notifyDataSetChanged();
			enableButtons();
			getListView().post(new Runnable(){
				public void run() {
					getListView().setSelection(getCount() - 1);
				}
			});
		}

		@Override
		public synchronized int getCount() {
			return myItems.size();
		}

		public boolean hasEmpty() {
			for (DirectoryItem i : myItems) {
				if ("".equals(i.getPath())) return true;
			}
			return false;
		}

		synchronized void removeDirectoryItem(int id) {
			for (int i = 0; i < myItems.size(); i++) {
				if (myItems.get(i).getId() == id) {
					myItems.remove(i);
					notifyDataSetChanged();
					enableButtons();
					return;
				}
			}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final DirectoryItem item = getItem(position);
			final View view;
			if (TYPE_FIRST_MAIN.equals(myType) && position == 0) {
				view = LayoutInflater.from(EditableStringListActivity.this).inflate(R.layout.editable_stringlist_mainitem, parent, false);
				final TextView warningtext = (TextView)view.findViewById(R.id.editable_stringlist_maintext);
				warningtext.setText("Main directory:");
			} else {
				view = LayoutInflater.from(EditableStringListActivity.this).inflate(R.layout.editable_stringlist_item, parent, false);
			}
			final AutoCompleteTextView text = (AutoCompleteTextView)view.findViewById(R.id.editable_stringlist_text);
			text.setText(item.getPath());
			text.addTextChangedListener(new TextWatcher(){
				public void afterTextChanged(Editable s) {}
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					item.setPath(s.toString());
					enableButtons();
				}
			});
			text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
    					if (hasFocus) {
						text.setAdapter(new ArrayAdapter<String>(EditableStringListActivity.this,
							android.R.layout.simple_dropdown_item_1line, mySuggestions
						));
					} else {
						text.setAdapter(new ArrayAdapter<String>(EditableStringListActivity.this,
							android.R.layout.simple_dropdown_item_1line, Collections.<String>emptyList()));
					}
				}
			});


			if (TYPE_FIRST_MAIN.equals(myType) && position == 0) {
				final TextView inactiveText = (TextView)view.findViewById(R.id.editable_stringlist_text_inactive);
				if (!myUserWasWarned) {
					inactiveText.setText(item.getPath());
					inactiveText.setOnClickListener(
						new View.OnClickListener() {
							public void onClick(View v) {
								if (!myUserWasWarned) {
									new AlertDialog.Builder(EditableStringListActivity.this)
										.setTitle("Editing of main directory will lead to loss of data")
										.setMessage("Continue?")
										.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface arg0, int arg1) {
												inactiveText.setVisibility(View.GONE);
												text.setVisibility(View.VISIBLE);
												myUserWasWarned = true;
												view.requestFocus();
												view.clearFocus();
											}
										})
										.setNegativeButton("No", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface arg0, int arg1) {
												view.requestFocus();
												view.clearFocus();
											}
										}).show();
								}
							}
						}
					);
				} else {
					inactiveText.setVisibility(View.GONE);
					text.setVisibility(View.VISIBLE);
				}
			}

			if ("".equals(item.getPath())) {
				text.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(0, 0);
			}
			final ImageButton button = (ImageButton)view.findViewById(R.id.editable_stringlist_deletebutton);
			button.setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						removeDirectoryItem(item.getId());
						enableButtons();
					}
				}
			);
			button.setEnabled(getCount() > 1 && !(TYPE_FIRST_MAIN.equals(myType) && position == 0));
			return view;
		}
	}

	private static class DirectoryItem {
		private String myPath = "";
		private int myId;

		public String getPath() {
			return myPath;
		}
		public void setPath(String path) {
			myPath = path;
		}

		public int getId() {
			return myId;
		}
		public void setId(int id) {
			myId = id;
		}
	}
}