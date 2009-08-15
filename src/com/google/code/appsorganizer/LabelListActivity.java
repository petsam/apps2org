package com.google.code.appsorganizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.google.code.appsorganizer.chooseicon.ChooseIconActivity;
import com.google.code.appsorganizer.db.DatabaseHelper;
import com.google.code.appsorganizer.db.DbChangeListener;
import com.google.code.appsorganizer.db.LabelDao;
import com.google.code.appsorganizer.dialogs.GenericDialogManager;
import com.google.code.appsorganizer.model.AppLabel;
import com.google.code.appsorganizer.model.Label;

/**
 * Demonstrates expandable lists backed by a Simple Map-based adapter
 */
public class LabelListActivity extends ExpandableListActivity {
	private MyExpandableListAdapter mAdapter;

	private DatabaseHelper dbHelper;

	private ApplicationInfoManager applicationInfoManager;

	private ChooseLabelDialogCreator chooseLabelDialog;

	private final GenericDialogManager genericDialogManager = new GenericDialogManager();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbHelper = new DatabaseHelper(this);
		applicationInfoManager = new ApplicationInfoManager(getPackageManager());

		mAdapter = new MyExpandableListAdapter();

		dbHelper.appsLabelDao.addListener(new DbChangeListener() {
			public void notifyDataSetChanged() {
				mAdapter.reload();
			}
		});

		setListAdapter(mAdapter);

		chooseLabelDialog = new ChooseLabelDialogCreator(this, dbHelper);
		genericDialogManager.addDialog(chooseLabelDialog);

		getExpandableListView().setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Application app = mAdapter.getChild(groupPosition, childPosition);
				chooseLabelDialog.setCurrentApp(app.getPackage());
				showDialog(chooseLabelDialog.getDialogId());
				return false;
			}
		});

		registerForContextMenu(getExpandableListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		int type = ExpandableListView.getPackedPositionType(((ExpandableListContextMenuInfo) menuInfo).packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			menu.add(0, 0, 0, R.string.launch);
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			menu.add(0, 0, 0, R.string.rename);
			menu.add(0, 1, 1, R.string.change_icon);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

		// String title = ((TextView) info.targetView).getText().toString();

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			// int groupPos =
			// ExpandableListView.getPackedPositionGroup(info.packedPosition);
			// int childPos =
			// ExpandableListView.getPackedPositionChild(info.packedPosition);
			// // Toast.makeText(this, title + ": Child " + childPos +
			// // " clicked in group " + groupPos, Toast.LENGTH_SHORT).show();
			// return true;
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			if (item.getItemId() == 1) {
				int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
				startActivityForResult(new Intent(this, ChooseIconActivity.class), groupPos);
				// Toast.makeText(this, ": Group " + groupPos + " clicked",
				// Toast.LENGTH_SHORT).show();
				return true;
			}
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			int icon = data.getIntExtra("icon", -1);
			Label label = mAdapter.getGroup(requestCode);
			label.setIcon(icon);
			LabelDao.getSingleton().update(label);
			mAdapter.notifyDataSetChanged();
		}
	}

	public class MyExpandableListAdapter extends BaseExpandableListAdapter {
		private List<Label> groups = dbHelper.labelDao.getLabels();

		private Map<Long, List<Application>> apps = new HashMap<Long, List<Application>>();

		public void reload() {
			groups = dbHelper.labelDao.getLabels();
			apps = new HashMap<Long, List<Application>>();
			notifyDataSetChanged();
		}

		private List<Application> getAppsInPos(Integer pos) {
			Label label = groups.get(pos);
			return getApps(label.getId());
		}

		private List<Application> getApps(Long labelId) {
			List<Application> ret = apps.get(labelId);
			if (ret == null) {
				List<AppLabel> l = dbHelper.appsLabelDao.getApps(labelId);
				ret = new ArrayList<Application>(applicationInfoManager.convertToApplicationList(l));
				apps.put(labelId, ret);
			}
			return ret;
		}

		public Application getChild(int groupPosition, int childPosition) {
			return getAppsInPos(groupPosition).get(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public int getChildrenCount(int groupPosition) {
			return getAppsInPos(groupPosition).size();
		}

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View cv, ViewGroup parent) {
			if (cv == null) {
				LayoutInflater factory = LayoutInflater.from(LabelListActivity.this);
				cv = factory.inflate(R.layout.app_row, null);
			}
			TextView v = (TextView) cv.findViewById(R.id.name);
			TextView vl = (TextView) cv.findViewById(R.id.labels);
			ImageView image = (ImageView) cv.findViewById(R.id.image);

			Application application = getChild(groupPosition, childPosition);

			vl.setText(dbHelper.labelDao.getLabelsString(application.getPackage()));
			v.setText(application.getName());
			image.setImageDrawable(application.getIcon());
			return cv;
		}

		public Label getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		public int getGroupCount() {
			return groups.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded, View cv, ViewGroup parent) {
			if (cv == null) {
				LayoutInflater factory = LayoutInflater.from(LabelListActivity.this);
				cv = factory.inflate(R.layout.label_row_with_icon, null);
			}
			TextView v = (TextView) cv.findViewById(R.id.name);
			ImageView image = (ImageView) cv.findViewById(R.id.image);

			Label label = getGroup(groupPosition);

			v.setText(label.getName());
			Integer icon = label.getIcon();
			if (icon != null) {
				image.setImageResource(icon);
			} else {
				image.setImageDrawable(null);
			}
			return cv;
			// if (cv == null) {
			// LayoutInflater factory =
			// LayoutInflater.from(LabelListActivity.this);
			// cv =
			// factory.inflate(android.R.layout.simple_expandable_list_item_1,
			// null);
			// }
			// TextView textView = (TextView)
			// cv.findViewById(android.R.id.text1);
			// AbsListView.LayoutParams lp = new
			// AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 64);
			// textView.setLayoutParams(lp);
			// textView.setText(getGroup(groupPosition).getName());
			// return cv;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public boolean hasStableIds() {
			return true;
		}

	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		genericDialogManager.onPrepareDialog(id, dialog);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return genericDialogManager.onCreateDialog(id);
	}
}