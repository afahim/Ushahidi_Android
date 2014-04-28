/**
 ** Copyright (c) 2010 Ushahidi Inc
 ** All rights reserved
 ** Contact: team@ushahidi.com
 ** Website: http://www.ushahidi.com
 **
 ** GNU Lesser General Public License Usage
 ** This file may be used under the terms of the GNU Lesser
 ** General Public License version 3 as published by the Free Software
 ** Foundation and appearing in the file LICENSE.LGPL included in the
 ** packaging of this file. Please review the following information to
 ** ensure the GNU Lesser General Public License version 3 requirements
 ** will be met: http://www.gnu.org/licenses/lgpl.html.
 **
 **
 ** If you have questions regarding the use of this file, please contact
 ** Ushahidi developers at team@ushahidi.com.
 **
 **/

package com.ushahidi.android.app.ui.phone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.VimeoApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;

import com.actionbarsherlock.view.MenuItem;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.DatePicker;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.ViewSwitcher;

import com.google.android.gms.maps.SupportMapFragment;
import com.ushahidi.android.app.Preferences;
import com.ushahidi.android.app.R;
import com.ushahidi.android.app.activities.BaseEditMapActivity;
import com.ushahidi.android.app.adapters.UploadPhotoAdapter;
import com.ushahidi.android.app.database.Database;
import com.ushahidi.android.app.database.IOpenGeoSmsSchema;
import com.ushahidi.android.app.database.IReportSchema;
import com.ushahidi.android.app.database.OpenGeoSmsDao;
import com.ushahidi.android.app.entities.CategoryEntity;
import com.ushahidi.android.app.entities.MediaEntity;
import com.ushahidi.android.app.entities.PhotoEntity;
import com.ushahidi.android.app.entities.ReportEntity;
import com.ushahidi.android.app.entities.ReportCategory;
import com.ushahidi.android.app.models.AddReportModel;
import com.ushahidi.android.app.models.ListReportModel;
import com.ushahidi.android.app.tasks.GeocoderTask;
import com.ushahidi.android.app.util.ImageManager;
import com.ushahidi.android.app.util.PhotoUtils;
import com.ushahidi.android.app.util.Util;
import com.ushahidi.android.app.views.AddReportView;
import com.ushahidi.java.sdk.api.Incident;

/**
 * @author eyedol
 */
public class AddReportActivity extends
		BaseEditMapActivity<AddReportView, AddReportModel> implements
		OnClickListener, ViewSwitcher.ViewFactory, OnItemClickListener,
		DialogInterface.OnClickListener {

	private ReverseGeocoderTask reverseGeocoderTask;

	private static final int DIALOG_ERROR_NETWORK = 0;

	private static final int DIALOG_ERROR_SAVING = 1;

	private static final int DIALOG_CHOOSE_IMAGE_METHOD = 2;

	private static final int DIALOG_MULTIPLE_CATEGORY = 3;

	private static final int TIME_DIALOG_ID = 4;

	private static final int DATE_DIALOG_ID = 5;

	private static final int DIALOG_SHOW_MESSAGE = 6;

	private static final int DIALOG_SHOW_REQUIRED = 7;

	private static final int DIALOG_SHOW_PROMPT = 8;

	private static final int DIALOG_SHOW_DELETE_PROMPT = 9;

	private static final int DIALOG_CHOOSE_VIDEO_METHOD = 10;
	
	private static final int REQUEST_CODE_CAMERA = 0;

	private static final int REQUEST_CODE_IMAGE = 1;

	private static final int REQUEST_CODE_VIDEO = 2;
	
	private Calendar mCalendar;

	// private String mDateToSubmit = "";

	private Date date = null;

	private int mCategoryLength;

	private Vector<Integer> mVectorCategories = new Vector<Integer>();

	private Vector<Integer> mCategoriesId = new Vector<Integer>();

	private HashMap<String, String> mCategoriesTitle = new HashMap<String, String>();

	private boolean mError = false;

	private int id = 0;

	private UploadPhotoAdapter pendingPhoto;

	private String mErrorMessage;

	private String photoName;

	private String videoName;
	
	private AddReportModel model;
	
    private static OAuthService service;
    private static Token accessToken;
    private static String fileLocation;
    private static String newline = System.getProperty("line.separator");
    private static int bufferSize = 1048576; // 1 MB = 1048576 bytes


	public AddReportActivity() {
		super(AddReportView.class, R.layout.add_report, R.menu.add_report,
				R.id.location_map);
		model = new AddReportModel();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view.mLatitude.addTextChangedListener(latLonTextWatcher);
		view.mLongitude.addTextChangedListener(latLonTextWatcher);
		if (checkForGMap()) {
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.location_map);
			view.map = mapFrag.getMap();
		}

		view.mBtnPicture.setOnClickListener(this);
		view.mBtnVideo.setOnClickListener(this);
		view.mBtnAddCategory.setOnClickListener(this);
		view.mPickDate.setOnClickListener(this);
		view.mPickTime.setOnClickListener(this);
		mCalendar = Calendar.getInstance();
		pendingPhoto = new UploadPhotoAdapter(this);
		view.gallery.setAdapter(pendingPhoto);
		view.gallery.setOnItemClickListener(this);
		view.mSwitcher.setFactory(this);
		if (getIntent().getExtras() != null) {
			this.id = getIntent().getExtras().getInt("id", 0);
		}
		mOgsDao = Database.mOpenGeoSmsDao;
		// edit existing report
		if (id > 0) {

			// make the delete button visible because we're editing
			view.mDeleteReport.setOnClickListener(this);
			view.mDeleteReport.setVisibility(View.VISIBLE);
			setSavedReport(id);
		} else {
			// add a new report
			updateDisplay();
			pendingPhoto.refresh();
		}

		registerForContextMenu(view.gallery);
		createSendMethodDialog();

	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (reverseGeocoderTask != null) {
			reverseGeocoderTask.cancel(true);
		}
	}

	/**
	 * Upon being resumed we can retrieve the current state. This allows us to
	 * update the state if it was changed at any time while paused.
	 */
	@Override
	protected void onResume() {
		getSharedText();
		super.onResume();
	}

	// Context Menu Stuff
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		new MenuInflater(this).inflate(R.menu.photo_context, menu);

	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		boolean result = performAction(item, info.position);

		if (!result) {
			result = super.onContextItemSelected(item);
		}

		return result;

	}

	public boolean performAction(android.view.MenuItem item, int position) {

		if (item.getItemId() == R.id.remove_photo) {

			// adding a new report
			if (id == 0) {

				// Delete by name
				if (ImageManager.deletePendingPhoto(this, "/"
						+ pendingPhoto.getItem(position).getPhoto())) {
					pendingPhoto.refresh();
				}
				return true;
			} else {

				// editing existing report
				if (ImageManager.deletePendingPhoto(this, "/"
						+ pendingPhoto.getItem(position).getPhoto())) {

					pendingPhoto.removeItem(position);
				}
				return true;
			}

		}
		return false;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			showDialog();
			return true;

		} else if (item.getItemId() == R.id.menu_send) {
			validateReports();
			return true;
		} else if (item.getItemId() == R.id.menu_clear) {
			createDialog(DIALOG_SHOW_PROMPT);
			return true;
		}
		return super.onOptionsItemSelected(item);

	}

	@Override
	public void onClick(View button) {
		if (button.getId() == R.id.btnPicture) {
			// get a file name for the photo to be uploaded
			photoName = Util.getDateTime() + ".jpg";

			// keep a copy of the filename for later reuse
			Preferences.fileName = photoName;
			Preferences.saveSettings(AddReportActivity.this);
			createDialog(DIALOG_CHOOSE_IMAGE_METHOD);

		} else if (button.getId() == R.id.btnVideo) {
			// get a file name for the photo to be uploaded
			videoName = Util.getDateTime() + ".mp4";

			// keep a copy of the filename for later reuse
			Preferences.fileName = videoName;
			Preferences.saveSettings(AddReportActivity.this);
			createDialog(DIALOG_CHOOSE_VIDEO_METHOD);			
		} else if (button.getId() == R.id.add_category) {
			createDialog(DIALOG_MULTIPLE_CATEGORY);
		} else if (button.getId() == R.id.pick_date) {
			createDialog(DATE_DIALOG_ID);
		} else if (button.getId() == R.id.pick_time) {
			createDialog(TIME_DIALOG_ID);
		} else if (button.getId() == R.id.delete_report) {
			createDialog(DIALOG_SHOW_DELETE_PROMPT);
		}

	}

	private void validateReports() {
		// STATE_SENT means no change in report fields
		// only the list of photos can be changed
		if (!mIsReportEditable) {
			onClick(mDlgSendMethod, 1);
			return;
		}
		// Dipo Fix
		mError = false;
		boolean required = false;
		// @inoran
		// validate the title field
		mErrorMessage = "";
		if (TextUtils.isEmpty(view.mIncidentTitle.getText())) {
			mErrorMessage = getString(R.string.title) + "\n";
			required = true;

		} else if (view.mIncidentTitle.getText().length() < 3
				|| view.mIncidentTitle.getText().length() > 200) {
			mErrorMessage = getString(R.string.less_report_title) + "\n";
			mError = true;
		}

		if (TextUtils.isEmpty(view.mIncidentDesc.getText())) {
			mErrorMessage += getString(R.string.description) + "\n";
			required = true;
		}

		// Dipo Fix
		if (mVectorCategories.size() == 0) {
			mErrorMessage += getString(R.string.category) + "\n";
			required = true;
		}

		// validate lat long
		if (TextUtils.isEmpty(view.mLatitude.getText().toString())) {
			mErrorMessage += getString(R.string.latitude) + "\n";
			required = true;
		} else {

			try {
				Double.parseDouble(view.mLatitude.getText().toString());
			} catch (NumberFormatException ex) {
				mErrorMessage += getString(R.string.invalid_latitude) + "\n";
				mError = true;
			}
		}

		// validate lat long
		if (TextUtils.isEmpty(view.mLongitude.getText().toString())) {
			mErrorMessage += getString(R.string.longitude) + "\n";
			mError = true;
		} else {

			try {
				Double.parseDouble(view.mLongitude.getText().toString());
			} catch (NumberFormatException ex) {
				mErrorMessage += getString(R.string.invalid_longitude) + "\n";
				mError = true;
			}
		}

		// validate location
		if (TextUtils.isEmpty(view.mIncidentLocation.getText())) {
			mErrorMessage += getString(R.string.location);
			required = true;
		}

		if (required) {
			createDialog(DIALOG_SHOW_REQUIRED);
		} else if (mError) {
			createDialog(DIALOG_SHOW_MESSAGE);
		} else {
			if (Preferences.canReceiveOpenGeoSms()) {
				mDlgSendMethod.show();
			} else {
				onClick(mDlgSendMethod, 0);
			}
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		mSendOpenGeoSms = which == 1;
		new SaveTask(this).execute((String) null);
	}

	private boolean mSendOpenGeoSms = false;
	private AlertDialog mDlgSendMethod;

	private void createSendMethodDialog() {
		Resources r = getResources();
		String[] items = new String[] { r.getString(R.string.internet),
				r.getString(R.string.opengeosms) };
		mDlgSendMethod = new AlertDialog.Builder(this).setItems(items, this)
				.setTitle(R.string.send_report_dlg_title).create();
	}

	private OpenGeoSmsDao mOgsDao;

	/**
	 * Post to local database
	 * 
	 * @author henryaddo
	 */
	private boolean addReport() {
		log("Adding new reports");
		File[] pendingPhotos = PhotoUtils.getPendingPhotos(this);

		ReportEntity report = new ReportEntity();
		Incident incident = new Incident();
		incident.setTitle(view.mIncidentTitle.getText().toString());
		incident.setDescription(view.mIncidentDesc.getText().toString());
		incident.setMode(0);
		incident.setLocationName(view.mIncidentLocation.getText().toString());
		incident.setVerified(0);
		incident.setLatitude(Double
				.valueOf(view.mLatitude.getText().toString()));
		incident.setLongitude(Double.valueOf(view.mLongitude.getText()
				.toString()));
		if (date != null) {
			incident.setDate(date);
		} else {
			incident.setDate(new Date());
		}

		report.setIncident(incident);
		report.setPending(1);

		if (id == 0) {
			// Add a new pending report
			if (model.addPendingReport(report, mVectorCategories,
					pendingPhotos, view.mNews.getText().toString())) {
				// move saved photos
				log("Moving photos to fetched folder");
				ImageManager.movePendingPhotos(this);
				id = report.getDbId();
			} else {
				return false;
			}
		} else {
			// Update existing report
			List<PhotoEntity> photos = new ArrayList<PhotoEntity>();
			for (int i = 0; i < pendingPhoto.getCount(); i++) {
				photos.add(pendingPhoto.getItem(i));
			}
			if (model.updatePendingReport(id, report, mVectorCategories,
					photos, view.mNews.getText().toString())) {
				// move saved photos
				log("Moving photos to fetched folder");
				ImageManager.movePendingPhotos(this);
			} else {
				return false;
			}
		}
		if (mSendOpenGeoSms) {
			mOgsDao.addReport(id);
		} else {
			mOgsDao.deleteReport(id);
		}
		return true;

	}

	/**
	 * Edit existing report
	 * 
	 * @author henryaddo
	 */
	private void setSavedReport(int reportId) {

		// set text part of reports
		ReportEntity report = model.fetchPendingReportById(reportId);
		if (report != null) {
			view.mIncidentTitle.setText(report.getIncident().getTitle());
			view.mIncidentDesc.setText(report.getIncident().getDescription());
			view.mLongitude.setText(String.valueOf(report.getIncident()
					.getLongitude()));
			view.mLatitude.setText(String.valueOf(report.getIncident()
					.getLatitude()));
			view.mIncidentLocation.setText(report.getIncident()
					.getLocationName());

			// set date and time
			setDateAndTime(report.getIncident().getDate());
		}

		// set Categories.
		mVectorCategories.clear();
		for (ReportCategory reportCategory : model
				.fetchReportCategories(reportId, IReportSchema.PENDING)) {
			mVectorCategories.add(reportCategory.getCategoryId());
		}

		setSelectedCategories(mVectorCategories);

		// set the photos
		pendingPhoto.refresh(id);

		// set news
		List<MediaEntity> newsMedia = model.fetchReportNews(reportId);
		if (newsMedia != null && newsMedia.size() > 0) {
			view.mNews.setText(newsMedia.get(0).getLink());
		}

		mIsReportEditable = mOgsDao.getReportState(id) != IOpenGeoSmsSchema.STATE_SENT;

		if (!mIsReportEditable) {
			View views[] = new View[] { view.mBtnAddCategory,
					view.mIncidentDesc, view.mIncidentLocation,
					view.mIncidentTitle, view.mLatitude, view.mLongitude,
					view.mPickDate, view.mPickTime };
			for (View v : views) {
				v.setEnabled(false);
			}
			updateMarker(report.getIncident().getLatitude(), report
					.getIncident().getLongitude(), true);
		}
	}

	private boolean mIsReportEditable = true;

	private void deleteReport() {
		// make sure it's an existing report
		if (id > 0) {
			if (model.deleteReport(id)) {
				// delete images
				for (int i = 0; i < pendingPhoto.getCount(); i++) {
					ImageManager.deletePendingPhoto(this, "/"
							+ pendingPhoto.getItem(i).getPhoto());
				}
				// return to report listing page.
				finish();
			}
		}
	}

	/**
	 * Create various dialog
	 */
	private void createDialog(int id) {
		switch (id) {
		case DIALOG_ERROR_NETWORK: {
			Dialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.network_error)
					.setMessage(R.string.network_error_msg)
					.setPositiveButton(getString(R.string.ok),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).setCancelable(false).create();
			dialog.show();
			break;
		}
		case DIALOG_ERROR_SAVING: {
			Dialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.network_error)
					.setMessage(R.string.file_system_error_msg)
					.setPositiveButton(getString(R.string.ok),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).setCancelable(false).create();
			dialog.show();
			break;
		}

		case DIALOG_CHOOSE_IMAGE_METHOD: {

			Dialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.choose_method)
					.setMessage(R.string.how_to_select_pic)
					.setPositiveButton(getString(R.string.gallery_option),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent();
									intent.setAction(Intent.ACTION_PICK);
									intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
									startActivityForResult(intent,
											REQUEST_CODE_IMAGE);
									dialog.dismiss();
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							})

					.setNeutralButton(R.string.camera_option,
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent(
											android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
									intent.putExtra(MediaStore.EXTRA_OUTPUT,
											PhotoUtils.getPhotoUri(photoName,
													AddReportActivity.this));
									startActivityForResult(intent,
											REQUEST_CODE_CAMERA);
									dialog.dismiss();
								}
							})

					.setCancelable(false).create();
			dialog.show();
			break;
		}

		case DIALOG_CHOOSE_VIDEO_METHOD: {

			Dialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.choose_method)
					.setMessage(R.string.how_to_select_vid)
					.setPositiveButton(getString(R.string.gallery_option),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent();
									intent.setAction(Intent.ACTION_PICK);
									intent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
									startActivityForResult(intent,
											REQUEST_CODE_VIDEO);
									dialog.dismiss();
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							})

					.setNeutralButton(R.string.camera_option,
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent(
											android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
									intent.putExtra(MediaStore.EXTRA_OUTPUT,
											PhotoUtils.getPhotoUri(photoName,
													AddReportActivity.this));
									startActivityForResult(intent,
											REQUEST_CODE_CAMERA);
									dialog.dismiss();
								}
							})

					.setCancelable(false).create();
			dialog.show();
			break;
		}

		
		case DIALOG_MULTIPLE_CATEGORY: {
			if (showCategories() != null) {
				new AlertDialog.Builder(this)
						.setTitle(R.string.choose_categories)
						.setMultiChoiceItems(
								showCategories(),
								setCheckedCategories(),
								new DialogInterface.OnMultiChoiceClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton, boolean isChecked) {
										// see if categories have previously

										if (isChecked) {
											mVectorCategories.add(mCategoriesId
													.get(whichButton));

											mError = false;
										} else {
											mVectorCategories
													.remove(mCategoriesId
															.get(whichButton));
										}

										setSelectedCategories(mVectorCategories);
									}
								})
						.setPositiveButton(R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {

										/* User clicked Yes so do some stuff */
									}
								}).create().show();
			}
			break;
		}

		case TIME_DIALOG_ID:
			new TimePickerDialog(this, mTimeSetListener,
					mCalendar.get(Calendar.HOUR),
					mCalendar.get(Calendar.MINUTE), false).show();
			break;

		case DATE_DIALOG_ID:
			new DatePickerDialog(this, mDateSetListener,
					mCalendar.get(Calendar.YEAR),
					mCalendar.get(Calendar.MONTH),
					mCalendar.get(Calendar.DAY_OF_MONTH)).show();
			break;

		case DIALOG_SHOW_MESSAGE:
			AlertDialog.Builder messageBuilder = new AlertDialog.Builder(this);
			messageBuilder.setMessage(mErrorMessage).setPositiveButton(
					getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog showDialog = messageBuilder.create();
			showDialog.show();
			break;

		case DIALOG_SHOW_REQUIRED:
			AlertDialog.Builder requiredBuilder = new AlertDialog.Builder(this);
			requiredBuilder.setTitle(R.string.required_fields);
			requiredBuilder.setMessage(mErrorMessage).setPositiveButton(
					getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog showRequiredDialog = requiredBuilder.create();
			showRequiredDialog.show();
			break;

		// prompt for unsaved changes
		case DIALOG_SHOW_PROMPT: {
			Dialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.unsaved_changes)
					.setMessage(R.string.want_to_cancel)
					.setNegativeButton(getString(R.string.no),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

									dialog.dismiss();
								}
							})
					.setPositiveButton(getString(R.string.yes),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									new DiscardTask(AddReportActivity.this)
											.execute((String) null);
									finish();
									dialog.dismiss();
								}
							})

					.setCancelable(false).create();
			dialog.show();
			break;
		}

		// prompt for report deletion
		case DIALOG_SHOW_DELETE_PROMPT: {
			Dialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.delete_report)
					.setMessage(R.string.want_to_delete)
					.setNegativeButton(getString(R.string.no),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

									dialog.dismiss();
								}
							})
					.setPositiveButton(getString(R.string.yes),
							new Dialog.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// delete report
									deleteReport();
									dialog.dismiss();
								}
							}).setCancelable(false).create();
			dialog.show();
		}
			break;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case TIME_DIALOG_ID:
			((TimePickerDialog) dialog).updateTime(
					mCalendar.get(Calendar.HOUR_OF_DAY),
					mCalendar.get(Calendar.MINUTE));
			break;
		case DATE_DIALOG_ID:
			((DatePickerDialog) dialog).updateDate(
					mCalendar.get(Calendar.YEAR),
					mCalendar.get(Calendar.MONTH),
					mCalendar.get(Calendar.DAY_OF_MONTH));
			break;

		case DIALOG_MULTIPLE_CATEGORY:
			final AlertDialog alert = (AlertDialog) dialog;
			final ListView list = alert.getListView();
			// been
			// selected, then uncheck
			// selected categories
			if (mVectorCategories.size() > 0) {
				for (Integer s : mVectorCategories) {
					try {
						// @inoran fix
						if (list != null) {
							list.setItemChecked(mCategoryLength - s, true);
						}
					} catch (NumberFormatException e) {
						log("NumberFormatException", e);
					}
				}
			} else {
				if (list != null) {
					list.clearChoices();
				}
			}

			break;

		}
	}

	// fetch categories
	public String[] showCategories() {
		ListReportModel mListReportModel = new ListReportModel();
		List<CategoryEntity> listCategories = mListReportModel
				.getAllCategories();
		if (listCategories != null && listCategories.size() > 0) {
			int categoryCount = listCategories.size();
			int categoryAmount = 0;
			if (categoryCount > 0) {
				categoryAmount = categoryCount;
			} else {
				mCategoriesId.clear();
				mCategoriesTitle.clear();
				categoryAmount = 1;
			}

			String categories[] = new String[categoryAmount];
			mCategoryLength = categories.length;

			int i = 0;
			for (CategoryEntity category : mListReportModel.getAllCategories()) {

				categories[i] = category.getCategoryTitle();
				mCategoriesTitle.put(String.valueOf(category.getCategoryId()),
						category.getCategoryTitle());
				mCategoriesId.add(category.getCategoryId());
				i++;
			}
			return categories;
		}
		return null;
	}

	private void updateDisplay() {
		date = mCalendar.getTime();
		if (date != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy",
					Locale.US);
			view.mPickDate.setText(dateFormat.format(date));

			SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a",
					Locale.US);
			view.mPickTime.setText(timeFormat.format(date));

		} else {
			view.mPickDate.setText(R.string.change_date);
			view.mPickTime.setText(R.string.change_time);
			date = null;
		}
	}

	private void setDateAndTime(Date d) {
		
		if (d != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss", Locale.US);
			String formatedDate = Util.datePattern("yyyy-MM-dd HH:mm:ss", d);
			try {

				date = dateFormat.parse(formatedDate);

				if (date != null) {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
							"MMMM dd, yyyy", Locale.US);
					view.mPickDate.setText(simpleDateFormat.format(date));

					SimpleDateFormat timeFormat = new SimpleDateFormat(
							"h:mm a", Locale.US);
					view.mPickTime.setText(timeFormat.format(date));

				} else {
					view.mPickDate.setText(R.string.change_date);
					view.mPickTime.setText(R.string.change_time);
					date = new Date();
				}

			} catch (ParseException e) {
				log(e.getMessage());

			}
		}
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mCalendar.set(year, monthOfYear, dayOfMonth);
			updateDisplay();
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
			mCalendar.set(Calendar.MINUTE, minute);
			updateDisplay();
		}
	};

	/**
	 * Sets the selected categories for submission
	 * 
	 * @param aSelectedCategories
	 */
	private void setSelectedCategories(Vector<Integer> aSelectedCategories) {
		// initilaize categories
		showCategories();

		// clear
		view.mBtnAddCategory.setText(R.string.select_category);
		if (aSelectedCategories.size() > 0) {
			StringBuilder categories = new StringBuilder();
			for (Integer category : aSelectedCategories) {
				if (categories.length() > 0) {
					categories.append(", ");
				}
				if (category > 0) {
					categories.append(mCategoriesTitle.get(String
							.valueOf(category)));
				}
			}

			if (!TextUtils.isEmpty(categories.toString())) {
				view.mBtnAddCategory.setText(categories.toString());
			} else {
				view.mBtnAddCategory.setText(R.string.select_category);
			}
		}
	}

	/**
	 * Get check selected categories
	 * 
	 * @param aSelectedCategories
	 */
	private boolean[] setCheckedCategories() {
		// FIXME: Look into making this more efficient
		if (mVectorCategories != null && mVectorCategories.size() > 0) {
			ListReportModel mListReportModel = new ListReportModel();
			List<CategoryEntity> listCategories = mListReportModel
					.getAllCategories();
			if (listCategories != null && listCategories.size() > 0) {
				int categoryCount = listCategories.size();
				int categoryAmount = 0;
				if (categoryCount > 0) {
					categoryAmount = categoryCount;
				} else {
					categoryAmount = 1;
				}

				boolean categories[] = new boolean[categoryAmount];
				mCategoryLength = categories.length;

				int i = 0;
				for (CategoryEntity category : mListReportModel
						.getAllCategories()) {

					if (mVectorCategories.contains(String.valueOf(category
							.getCategoryId()))) {

						categories[i] = true;
					} else {
						categories[i] = false;
					}

					i++;
				}
				return categories;

			}
		}
		return null;
	}

	/**
	 * Set photo to be attached to an existing report
	 */
	private void addPhotoToReport() {
		File[] pendingPhotos = PhotoUtils.getPendingPhotos(this);
		if (pendingPhotos != null && pendingPhotos.length > 0) {
			int id = 0;
			for (File file : pendingPhotos) {
				if (file.exists()) {
					id += 1;
					PhotoEntity photo = new PhotoEntity();
					photo.setDbId(id);
					photo.setPhoto(file.getName());
					pendingPhoto.addItem(photo);
				}
			}
		}
	}

	/**
	 * Get shared text from other Android applications
	 */
	public void getSharedText() {
		Intent intent = getIntent();
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_SEND)
					|| action.equals(Intent.ACTION_CHOOSER)) {
				CharSequence text = intent
						.getCharSequenceExtra(Intent.EXTRA_TEXT);
				if (text != null) {
					view.mIncidentDesc.setText(text);
				}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			// get the saved file name
			Preferences.loadSettings(AddReportActivity.this);
			photoName = Preferences.fileName;
			if (requestCode == REQUEST_CODE_CAMERA) {

				Uri uri = PhotoUtils.getPhotoUri(photoName, this);
				Bitmap bitmap = PhotoUtils.getCameraPhoto(this, uri);
				PhotoUtils.savePhoto(this, bitmap, photoName);
				log(String.format("REQUEST_CODE_CAMERA %dx%d",
						bitmap.getWidth(), bitmap.getHeight()));

			} else if (requestCode == REQUEST_CODE_IMAGE) {

				Bitmap bitmap = PhotoUtils
						.getGalleryPhoto(this, data.getData());
				PhotoUtils.savePhoto(this, bitmap, photoName);
				log(String.format("REQUEST_CODE_IMAGE %dx%d",
						bitmap.getWidth(), bitmap.getHeight()));
				
			} else if (requestCode == REQUEST_CODE_VIDEO) {
				Uri uri = data.getData();
			    String[] proj = { MediaStore.Images.Media.DATA };
			    Cursor cursor = this.getContentResolver().query(uri,  proj, null, null, null);
			    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			    cursor.moveToFirst();
			    final String fileLocation = cursor.getString(column_index);
			    
		        class UploadVideo extends AsyncTask {
		            protected Object doInBackground(Object... urls) {
					    uploadToVimeo(fileLocation);
						return null;
		            }
		        }
		        
		        UploadVideo sendVideo = new UploadVideo();
		        sendVideo.execute();
			}
			if (id > 0) {
				addPhotoToReport();
			} else {
				pendingPhoto.refresh();
			}
		}
	}

	  /**
	   * Sets the video's meta-data
	   */
	  private static void uploadToVimeo(String fileLocation) {
	        String apiKey = "46808ac7e90549f2536ab035b449b095bc2fa373"; 
	        String apiSecret = "98bbb44fb4fbcf5d8a32a051a7e4d5289a4b9470";
	        String vimeoAPIURL = "http://vimeo.com/api/rest/v2";
	        service = new ServiceBuilder().provider(VimeoApi.class).apiKey(apiKey).apiSecret(apiSecret).build();

	        OAuthRequest request;
	        Response response;
	     
	        accessToken = new Token("f4c3cd65f825eab77dc6c3a27293b419", "e851d1c36c4634a2985e9f23198a282218875185");
	        
	        accessToken = checkToken(vimeoAPIURL, accessToken, service);
	        if (accessToken == null) {
	          return;
	        }
	        
	        // Get Quota
	        request = new OAuthRequest(Verb.GET, vimeoAPIURL);
	        request.addQuerystringParameter("method", "vimeo.videos.upload.getQuota");
	        signAndSendToVimeo(request, "getQuota", true);
	     
	        // Get Ticket
	        request = new OAuthRequest(Verb.GET, vimeoAPIURL);
	        request.addQuerystringParameter("method", "vimeo.videos.upload.getTicket");
	        request.addQuerystringParameter("upload_method", "streaming");
	        response = signAndSendToVimeo(request, "getTicket", true);
	     
	        // Get Endpoint and ticket ID
	        System.out.println(newline + newline + "We're sending the video for upload!");
	        Document doc = readXML(response.getBody());
	        Element ticketElement = (Element) doc.getDocumentElement().getElementsByTagName("ticket").item(0);
	        String endpoint = ticketElement.getAttribute("endpoint");
	        String ticketId = ticketElement.getAttribute("id");
	        
	        // Setup File
	        File testUp = new File(fileLocation);
	        boolean sendVideo = false;
			try {
				sendVideo = sendVideo(endpoint, testUp);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        if (!sendVideo) {
	          Log.v("VidGrab", "Didn't Send Video SUCCESSFULLY");
	        } else {
		      Log.v("VidGrab", "VIDEO SENT");
	        }
	        
	        // Complete Upload
	        request = new OAuthRequest(Verb.PUT, vimeoAPIURL);
	        request.addQuerystringParameter("method", "vimeo.videos.upload.complete");
	        request.addQuerystringParameter("filename", testUp.getName());
	        request.addQuerystringParameter("ticket_id", ticketId);
	        Response completeResponse = signAndSendToVimeo(request, "complete", true);
	     
	        //Set video info
	        setVimeoVideoInfo(completeResponse, service, accessToken, vimeoAPIURL);
	  }

	
	  /**
	   * Sets the video's meta-data
	   */
	  private static void setVimeoVideoInfo(Response response, OAuthService service, Token token, String vimeoAPIURL) {
	    OAuthRequest request;
	    Document doc = readXML(response.getBody());
	    org.w3c.dom.Element ticketElement = (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("ticket").item(0);
	    String vimeoVideoId = ticketElement.getAttribute("video_id");
	    //Set title, description, category, tags, private
	    //Set Title
	    request = new OAuthRequest(Verb.POST, vimeoAPIURL);
	    request.addQuerystringParameter("method", "vimeo.videos.setTitle");
	    request.addQuerystringParameter("title", "Test Title");
	    request.addQuerystringParameter("video_id", vimeoVideoId);
	    signAndSendToVimeo(request, "setTitle", true);
	 
	    //Set description
	    request = new OAuthRequest(Verb.POST, vimeoAPIURL);
	    request.addQuerystringParameter("method", "vimeo.videos.setDescription");
	    request.addQuerystringParameter("description", "This is my test description");
	    request.addQuerystringParameter("video_id", vimeoVideoId);
	    signAndSendToVimeo(request, "setDescription", true);
	 
	    List<String> videoTags = new ArrayList<String>();
	    videoTags.add("test1");
	    videoTags.add("");
	    videoTags.add("test3");
	    videoTags.add("test4");
	    videoTags.add("test 5");
	    videoTags.add("test-6");
	    videoTags.add("test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7 test 7");
	 
	    //Create tags string
	    String tags = "";
	    for (String tag : videoTags) {
	      tags += tag + ", ";
	    }
	    tags.replace(", , ", ", "); //if by chance there are empty tags.
	 
	    //Set Tags
	    request = new OAuthRequest(Verb.POST, vimeoAPIURL);
	    request.addQuerystringParameter("method", "vimeo.videos.addTags");
	    request.addQuerystringParameter("tags", tags);
	    request.addQuerystringParameter("video_id", vimeoVideoId);
	    signAndSendToVimeo(request, "addTags", true);
	 
	    //Set Privacy
	    request = new OAuthRequest(Verb.POST, vimeoAPIURL);
	    request.addQuerystringParameter("method", "vimeo.videos.setPrivacy");
	    request.addQuerystringParameter("privacy", (true) ? "nobody" : "anybody");
	    request.addQuerystringParameter("video_id", vimeoVideoId);
	    signAndSendToVimeo(request, "setPrivacy", true);
	    
	    Log.v("VidGrab", "Video URL: vimeo.com/" + vimeoVideoId);
	  }

	
	  /**
	   * Send the video data
	   *
	   * @return whether the video successfully sent
	   */
	  private static boolean sendVideo(String endpoint, File file) throws FileNotFoundException, IOException {
	    // Setup File
	    long contentLength = file.length();
	    String contentLengthString = Long.toString(contentLength);
	    FileInputStream is = new FileInputStream(file);
	    byte[] bytesPortion = new byte[bufferSize];
	    int maxAttempts = 5; //This is the maximum attempts that will be given to resend data if the vimeo server doesn't have the right number of bytes for the given portion of the video
	    long lastByteOnServer = 0;
	    boolean first = false;
	    while (is.read(bytesPortion, 0, bufferSize) != -1) {
	      lastByteOnServer = prepareAndSendByteChunk(endpoint, contentLengthString, lastByteOnServer, bytesPortion, first, 0, maxAttempts);
	      if (lastByteOnServer == -1) {
	        return false;
	      }
	      first = true;
//	      getProgressBar().setValue(NumberHelper.getPercentFromTotal(byteNumber, getFileSize()));
	    }
	    return true;
	  }

	  /**
	   * Prepares the given bytes to be sent to Vimeo
	   *
	   * @param endpoint
	   * @param contentLengthString
	   * @param lastByteOnServer
	   * @param byteChunk
	   * @param first
	   * @param attempt
	   * @param maxAttempts
	   * @return number of bytes currently on the server
	   * @throws FileNotFoundException
	   * @throws IOException
	   */
	  @SuppressLint("NewApi")
	private static long prepareAndSendByteChunk(String endpoint, String contentLengthString, long lastByteOnServer, byte[] byteChunk, boolean first, int attempt, int maxAttempts) throws FileNotFoundException, IOException {
	    if (attempt > maxAttempts) {
	      return -1;
	    } else if (attempt > 0) {
	      System.out.println("Attempt number " + attempt + " for video " + "Test Video");
	    }
	    long totalBytesShouldBeOnServer = lastByteOnServer + byteChunk.length;
	    String contentRange = lastByteOnServer + "-" + totalBytesShouldBeOnServer;
	    long bytesOnServer = sendVideoBytes(endpoint, contentLengthString, "video/mp4", contentRange, byteChunk, first);
	    if (bytesOnServer != totalBytesShouldBeOnServer) {
	      System.err.println(bytesOnServer + " (bytesOnServer)" + " != " + totalBytesShouldBeOnServer + " (totalBytesShouldBeOnServer)");
	      long remainingBytes = totalBytesShouldBeOnServer - bytesOnServer;
	      int beginning = (int) (byteChunk.length - remainingBytes);
	      int ending = (int) byteChunk.length;
	      byte[] newByteChunk = Arrays.copyOfRange(byteChunk, beginning, ending);
	      return prepareAndSendByteChunk(endpoint, contentLengthString, bytesOnServer, newByteChunk, first, attempt + 1, maxAttempts);
	    } else {
	      return bytesOnServer;
	    }
	  }
	 
	  /**
	   * Sends the given bytes to the given endpoint
	   *
	   * @return the last byte on the server (from verifyUpload(endpoint))
	   */
	  private static long sendVideoBytes(String endpoint, String contentLength, String fileType, String contentRange, byte[] fileBytes, boolean addContentRange) throws FileNotFoundException, IOException {
	    OAuthRequest request = new OAuthRequest(Verb.PUT, endpoint);
	    request.addHeader("Content-Length", contentLength);
	    request.addHeader("Content-Type", fileType);
	    if (addContentRange) {
	      request.addHeader("Content-Range", "bytes " + contentRange);
	    }
	    request.addPayload(fileBytes);
	    Response response = signAndSendToVimeo(request, "sendVideo on " + "Test title", false);
	    if (response.getCode() != 200 && !response.isSuccessful()) {
	      return -1;
	    }
	    return verifyUpload(endpoint);
	  }
	 
	  /**
	   * Verifies the upload and returns whether it's successful
	   *
	   * @param endpoint to verify upload to
	   * @return the last byte on the server
	   */
	  private static long verifyUpload(String endpoint) {
	    // Verify the upload
	    OAuthRequest request = new OAuthRequest(Verb.PUT, endpoint);
	    request.addHeader("Content-Length", "0");
	    request.addHeader("Content-Range", "bytes */*");
	    Response response = signAndSendToVimeo(request, "verifyUpload to " + endpoint, true);
	    if (response.getCode() != 308 || !response.isSuccessful()) {
	      return -1;
	    }
	    String range = response.getHeader("Range");
	    //range = "bytes=0-10485759"
	    return Long.parseLong(range.substring(range.lastIndexOf("-") + 1)) + 1;
	    //The + 1 at the end is because Vimeo gives you 0-whatever byte where 0 = the first byte
	  }

	
	  /**
	   * This method will Read the XML and act accordingly
	   *
	   * @param xmlString - the XML String
	   * @return the list of elements within the XML
	   */
	  private static Document readXML(String xmlString) {
	    Document doc = null;
	    try {
	      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	      InputSource xmlStream = new InputSource();
	      xmlStream.setCharacterStream(new StringReader(xmlString));
	      doc = dBuilder.parse(xmlStream);
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    return doc;
	  }


    /**
     * Checks the token to make sure it's still valid. If not, it pops up a dialog asking the user to
     * authenticate.
     */
     private static Token checkToken(String vimeoAPIURL, Token vimeoToken, OAuthService vimeoService) {
       if (vimeoToken == null) {
         vimeoToken = getNewToken(vimeoService);
       } else {
         OAuthRequest request = new OAuthRequest(Verb.GET, vimeoAPIURL);
         request.addQuerystringParameter("method", "vimeo.oauth.checkAccessToken");
         Response response = signAndSendToVimeo(request, "checkAccessToken", true);
         if (response.isSuccessful()
                 && (response.getCode() != 200 || response.getBody().contains("<err code=\"302\"")
                 || response.getBody().contains("<err code=\"401\""))) {
           Log.v("VidGrab", "Token Isn't valid");
           vimeoToken = getNewToken(vimeoService);
         } else {
           Log.v("VidGrab", "Token IS valid");
         }
       }
       return vimeoToken;
     }
     
     /**
      * Signs the request and sends it. Returns the response.
      *
      * @param request
      * @return response
      */
	@SuppressLint("NewApi")
	public static Response signAndSendToVimeo(final OAuthRequest request, final String description, final boolean printBody) throws org.scribe.exceptions.OAuthException {
        System.out.println(newline + newline
                + "Signing " + description + " request:"
                + ((printBody && !request.getBodyContents().isEmpty()) ? newline + "\tBody Contents:" + request.getBodyContents() : "")
                + ((!request.getHeaders().isEmpty()) ? newline + "\tHeaders: " + request.getHeaders() : ""));
        service.signRequest(accessToken, request);
        printRequest(request, description);

        class SendRequest extends AsyncTask {
            protected Response doInBackground(Object... urls) {
                Response response = request.send();
                printResponse(response, description, printBody);
				return response;
            }

            protected Response onPostExecute(Response result) {
                return result;
            }
        }
        
        SendRequest sender = new SendRequest();
        sender.execute(request);
        
        Response response = null;
        
        try {
			response = (Response) sender.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return response;
      }

     /**
      * Gets authorization URL, pops up a dialog asking the user to authenticate with the url and the user
      * returns the authorization code
      *
      * @param service
      * @return
      */
      private static Token getNewToken(OAuthService service) {
        // Obtain the Authorization URL
        Token requestToken = service.getRequestToken();
        String authorizationUrl = service.getAuthorizationUrl(requestToken);
        do {
          Log.v("VidGrab", "Auth URL: " + authorizationUrl);
          break;
          /*code = null;
          Verifier verifier = new Verifier(code);
          // Trade the Request Token and Verfier for the Access Token
          System.out.println("Trading the Request Token for an Access Token...");
          try {
            Token token = service.getAccessToken(requestToken, verifier);
            System.out.println(token); //Use this output to copy the token into your code so you don't have to do this over and over.
            return token;
          } catch (OAuthException ex) {
            int choice = JOptionPane.showConfirmDialog(null, "There was an OAuthException" + newline
                    + ex + newline
                    + "Would you like to try again?", "OAuthException", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.NO_OPTION) {
              break;
            }
          }*/
        } while (true);
        return null;
      }

      /**
       * Prints the given description, and the headers, verb, and complete URL of the request.
       *
       * @param request
       * @param description
       */
      private static void printRequest(OAuthRequest request, String description) {
        Log.v("Req", "");
        Log.v("Req", description + " >>> Request");
        Log.v("Req", "Headers: " + request.getHeaders());
        Log.v("Req", "Verb: " + request.getVerb());
        Log.v("Req", "Complete URL: " + request.getCompleteUrl());
      }

      /**
       * Prints the given description, and the code, headers, and body of the given response
       *
       * @param response
       * @param description
       */
      private static void printResponse(Response response, String description, boolean printBody) {
        Log.v("Req", "");
        Log.v("Req", description + " >>> Response");
        Log.v("Req", "Code: " + response.getCode());
        Log.v("Req", "Headers: " + response.getHeaders());
        if (printBody) {
          Log.v("Req", "Body: " + response.getBody());
        }
      }

	
	@Override
	protected void locationChanged(double latitude, double longitude) {
		if (!mIsReportEditable) {
			return;
		}
		updateMarker(latitude, longitude, true);
		if (!view.mLatitude.hasFocus() && !view.mLongitude.hasFocus()) {
			view.mLatitude.setText(String.valueOf(latitude));
			view.mLongitude.setText(String.valueOf(longitude));
		}
		if (reverseGeocoderTask == null || !reverseGeocoderTask.isExecuting()) {
			reverseGeocoderTask = new ReverseGeocoderTask(this);
			reverseGeocoderTask.execute(latitude, longitude);
		}

	}

	/**
	 * Asynchronous Reverse Geocoder Task
	 */
	private class ReverseGeocoderTask extends GeocoderTask {

		public ReverseGeocoderTask(Context context) {
			super(context);
		}

		@Override
		protected void onPostExecute(String result) {
			log(getClass().getSimpleName(),
					String.format("onPostExecute %s", result));
			if (TextUtils.isEmpty(view.mIncidentLocation.getText().toString()))
				view.mIncidentLocation.setText(result);
			executing = false;
		}
	}

	private TextWatcher latLonTextWatcher = new TextWatcher() {
		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			try {
				if (view.mLatitude.hasFocus() || view.mLongitude.hasFocus()) {
					locationChanged(Double.parseDouble(view.mLatitude.getText()
							.toString()), Double.parseDouble(view.mLongitude
							.getText().toString()));
				}
			} catch (Exception ex) {
				log("TextWatcher", ex);
			}
		}
	};

	/**
	 * Go to reports screen
	 */
	public void goToReports() {
		finish();
	}

	public void onLocationChanged(Location arg0) {
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	protected boolean onSaveChanges() {
		return addReport();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ViewSwitcher.ViewFactory#makeView()
	 */
	@Override
	public View makeView() {
		ImageView i = new ImageView(this);
		i.setAdjustViewBounds(true);
		i.setScaleType(ImageView.ScaleType.FIT_CENTER);
		i.setLayoutParams(new ImageSwitcher.LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

		return i;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
	 *      .AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		this.view.mSwitcher.setImageDrawable(ImageManager.getPendingDrawables(
				this, pendingPhoto.getItem(position).getPhoto(),
				Util.getScreenWidth(this)));

	}

	/**
	 * Delete any existing photo in the pending folder
	 */
	private void deleteExistingPhoto() {
		File[] pendingPhotos = PhotoUtils.getPendingPhotos(this);
		if (pendingPhotos != null && pendingPhotos.length > 0) {
			for (File file : pendingPhotos) {
				if (file.exists()) {

					file.delete();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ushahidi.android.app.activities.BaseEditMapActivity#onDiscardChanges
	 * ()
	 */
	@Override
	protected boolean onDiscardChanges() {
		deleteExistingPhoto();
		return true;
	}

}
