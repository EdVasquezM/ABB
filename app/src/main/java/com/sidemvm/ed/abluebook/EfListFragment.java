package com.sidemvm.ed.abluebook;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Created by super on 25/03/2018.*/

public class EfListFragment extends Fragment {

    /**Main activity*/
    public AbbMainActivity mActivity;

    /**Recycler view e form list adapter*/
    public EfListAdapter efListAdapter;
    public RecyclerView recyclerView;

    /**No data View*/
    private TextView noDataView;

    /**Mandatory empty constructor for the fragment manager to instantiate the fragment*/
    public EfListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = (AbbMainActivity) getActivity();
        return inflater.inflate(R.layout.fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        //erase ads from list if flag is up.
        if(mActivity.noAdsFlag){
            for (int i=0; i < AbbContent.eFORM_ITEM_LIST.size(); i++){
                if(AbbContent.eFORM_ITEM_LIST.get(i).efType.equals("ad"))
                    AbbContent.removeFormItem(AbbContent.eFORM_ITEM_LIST.get(i));
            }
        }
        //Set recycler view
        efListAdapter = new EfListAdapter(this, AbbContent.eFORM_ITEM_LIST);
        recyclerView = view.findViewById(R.id.list_view);
        recyclerView.setAdapter(efListAdapter);
        // set item movement on e form list
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP|ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos = target.getAdapterPosition();
                        // move item `fromPos` to `toPos`.
                        AbbContent.eFormItem eqItemToMove = AbbContent.eFORM_ITEM_LIST.get(fromPos);
                        AbbContent.removeFormItem(eqItemToMove);
                        AbbContent.addFormItem(eqItemToMove, toPos, false);
                        efListAdapter.notifyItemMoved(fromPos, toPos);
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int thisItemPosition = viewHolder.getAdapterPosition();
                        AbbContent.eFormItem thisEQ =
                                AbbContent.eFORM_ITEM_LIST.get(thisItemPosition);
                        if (thisEQ.efId == 0) {
                            AbbContent.removeFormItem(thisEQ);
                            efListAdapter.notifyItemRemoved(thisItemPosition);
                            if (efListAdapter.getItemCount() == 0) showNoDataMessage();
                        } else {
                            if(direction == ItemTouchHelper.LEFT) efDelete
                                    (AbbContent.eFORM_ITEM_LIST.get(viewHolder.getAdapterPosition()));
                            if(direction == ItemTouchHelper.RIGHT) {
                                mActivity.sendFile(AbbFileGenerator.saveEFromFile(thisEQ.efId));
                                efListAdapter.notifyItemChanged(thisItemPosition);
                            }
                        }
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        //Set no data view, if list is empty, show empty msg
        noDataView = view.findViewById(R.id.no_data_view);
        noDataView.setOnClickListener(newEQClkListener());
        noDataView.setText(R.string.empty_ef_list_message);
        if (efListAdapter.getItemCount() == 0) showNoDataMessage();
        //Set fab
        mActivity.mFAB.setOnClickListener(newEQClkListener());
        mActivity.mFAB.show();
        FloatingActionButton nFab = mActivity.findViewById(R.id.fab_two_pane);
        if (nFab != null) nFab.setOnClickListener(newEQClkListener());
    }

    public void showNoDataMessage() {noDataView.setVisibility(View.VISIBLE);}

    /**Set the click listener to fab for new e Form*/
    public View.OnClickListener newEQClkListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noDataView.getVisibility() == View.VISIBLE) noDataView.setVisibility(View.GONE);
                showNewTestCard();
            }
        };
    }

    public void showNewTestCard() {
        AbbContent.eFormItem newEFormItem = new AbbContent.eFormItem
                ("", 0, mActivity.eFormOwner, "new", new String[5]);
        AbbContent.addFormItem(newEFormItem, -1, false);
        int idx = AbbContent.eFORM_ITEM_LIST.indexOf(newEFormItem);
        efListAdapter.notifyItemInserted(idx);
        recyclerView.smoothScrollToPosition(idx);
    }

    /**call when tap ok button in new eForm from form, create new eFrom file*/
    public boolean efNewFromForm(EditText eFormName, EditText eFormComments, CheckBox tapperCheck){
        // Reset error
        eFormName.setError(null);

        // Store values at the time of the registration attempt.
        String newFormName = eFormName.getText().toString();
        String newEFComments = mActivity.getTextWithLineBreaks(eFormComments.getText().toString());

        // Check for a valid name.
        if (TextUtils.isEmpty(newFormName)) {
            eFormName.setError(getString(R.string.error_field_required));
            eFormName.requestFocus();
            return false;
        } else {
            //Set params
            long eFormUniqueID = UUID.randomUUID().hashCode();
            String actualDate = new Date().toString();
            String eFormType;
            if (tapperCheck.isChecked()) eFormType = "eFormTapper";
            else eFormType = "eForm";

            //Initialize eForm
            AbbContent.eFormItem eFormToAdd = new AbbContent.eFormItem(newFormName, eFormUniqueID,
                    mActivity.eFormOwner, eFormType, new String[]{actualDate, getString
                    (R.string.new_e_form_last_modified_date_msg), "0", newEFComments, "true"});

            //Verify if already exist
            if (mActivity.eFormAlreadyExist(eFormToAdd)) {
                eFormName.setError(getString(R.string.error_already_exist));
                eFormName.requestFocus();
                return false;
            } else {
                //First string to add in new created form
                String eFormInfoLine = eFormToAdd.getString();

                //array list for the writer method
                List<String> eFormInfoLines = new ArrayList<>();
                eFormInfoLines.add(eFormInfoLine);

                //try to save new eForm on File
                String saveResult = AbbContent.saveNewEForm(-1, eFormToAdd, eFormInfoLines);
                if (saveResult.equals("ok")) {
                    if(AbbContent.renameToGet(AbbContent.getAbbFilesPath()
                            +"/lastPick.jpeg", String.valueOf(eFormUniqueID)+".jpeg"))
                        mActivity.showSnackBarMsg(getString(R.string.image_attached),
                                -1, android.R.string.ok, null);
                    else mActivity.showSnackBarMsg(getString(R.string.success_message),
                            -1, android.R.string.ok, null);
                    efListAdapter.notifyItemInserted(AbbContent.eFORM_ITEM_LIST.indexOf(eFormToAdd));
                    return true;
                } else {
                    mActivity.showSnackBarMsg(getString(R.string.fail_message)+"\n"+
                                    saveResult, 0,android.R.string.ok,null);
                    efListAdapter.notifyItemRemoved(efListAdapter.getItemCount() - 1);
                    return false;
                }
            }
        }
    }

    /**call when need to update a card from activity*/
    public void upDateCard(int efPos, AbbContent.eFormItem efToEdit, boolean notify){
        AbbContent.setFormItem(efPos, efToEdit);
        if (notify) {
            efListAdapter.notifyItemChanged(efPos);
            recyclerView.smoothScrollToPosition(efPos);
        }
    }

    /**call when tap ok button in quiz form, edit eForm*/
    public boolean efEdit(AbbContent.eFormItem ef, EditText efName, EditText efCms, CheckBox tapper){
        // Reset error
        efName.setError(null);

        // Store values at the time of the registration attempt.
        String newFormName = efName.getText().toString();
        String newEFComments = mActivity.getTextWithLineBreaks(efCms.getText().toString());

        // Check for a valid name.
        if (TextUtils.isEmpty(newFormName)) {
            efName.setError(getString(R.string.error_field_required));
            efName.requestFocus();
            return false;
        } else {
            //Set params
            String actualDate = new Date().toString();
            String eFormType;
            if (tapper.isChecked()) eFormType = "eFormTapper";
            else eFormType = "eForm";

            //get edited eForm
            AbbContent.eFormItem efEdited = new AbbContent.eFormItem(newFormName, ef.efId,
                    mActivity.eFormOwner, eFormType, new String[]{ef.efDetails[0], actualDate,
                    ef.efDetails[2], newEFComments, ef.efDetails[4]});

            //save ef content and charge e form info line
            List<String> efContent = AbbContent.getContentByLines(ef.efName, false);
            efContent.set(0, efEdited.getString());

            //Try to change file name
            if (AbbContent.renameEFormFile(ef, efEdited))
                if(AbbContent.writeEFormFile(newFormName, efContent, false)) {
                    if(AbbContent.renameToGet(AbbContent.getAbbFilesPath()
                            +"/lastPick.jpeg",String.valueOf(ef.efId)+".jpeg"))
                        mActivity.showSnackBarMsg(getString(R.string.image_attached),
                                -1, android.R.string.ok, null);
                    else mActivity.showSnackBarMsg(getString(R.string.success_message),
                            -1, android.R.string.ok, null);
                    return true;
                } else return false;
            else {
                mActivity.showSnackBarMsg(getString(R.string.fail_message), 0,
                        android.R.string.ok,null);
                return false;
            }
        }
    }

    /**call when swipe eForm card and delete the file*/
    public void efDelete(final AbbContent.eFormItem efToDelete){
        final List<String> efContent = AbbContent.getContentByLines(efToDelete.efName, false);
        final int posToRemove = AbbContent.eFORM_ITEM_LIST.indexOf(efToDelete);
        //try to delete the eFrom file
        String delResult = AbbContent.deleteEF(efToDelete);
        if (delResult.equals("ok")) {
            efListAdapter.notifyItemRemoved(posToRemove);
            //Recreate eForm File if user tap undo
            mActivity.showSnackBarMsg(getString(R.string.deleted), 0,
                    R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String saveResult =
                                    AbbContent.saveNewEForm(posToRemove, efToDelete, efContent);
                            if (saveResult.equals("ok")){
                                efListAdapter.notifyItemInserted(posToRemove);
                                recyclerView.smoothScrollToPosition(posToRemove);
                                if (noDataView.getVisibility() == View.VISIBLE)
                                    noDataView.setVisibility(View.GONE);
                            }
                            else mActivity.showSnackBarMsg(getString(R.string.fail_message)
                                    +".\n"+saveResult,0,android.R.string.ok,null);
                        }
                    });
            //Show no data message if list gets empty
            if (AbbContent.eFORM_ITEM_LIST.size() == 0) showNoDataMessage();
        } else mActivity.showSnackBarMsg(getString(R.string.fail_message)+".\n"+delResult,
                    0, android.R.string.ok, null);
    }
}
