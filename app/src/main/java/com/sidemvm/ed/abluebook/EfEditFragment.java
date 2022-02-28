package com.sidemvm.ed.abluebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Created by super on 04/02/2018.*/

public class EfEditFragment extends Fragment {
    /**Main activity*/
    public AbbMainActivity mActivity;
    /**Recycler view adapter*/
    public EfEditAdapter editAdapter;
    /**List of questions and answers*/
    List<AbbContent.QuestionItem> qItemList;
    List<AbbContent.AnswerItem> aItemList;
    List<String> efAllLines;
    /**No data View and toolbar views*/
    private TextView noDataView;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    /** The content of the file to edit*/
    public static final String eFORM_ID = "e_form_id";
    /** The id of the eForm selected*/
    public static long eFormId;
    public static AbbContent.eFormItem thisEf;
    public static int thisEfListIdx;
    /**needed to not notify eForm list fragment adapter when screen changes*/
    public boolean notifyEfList;

    /** Mandatory empty constructor */
    public EfEditFragment() {}

    public static EfEditFragment newInstance(long eFormId) {
        final EfEditFragment eFragment = new EfEditFragment();
        final Bundle args = new Bundle();
        args.putLong(eFORM_ID, eFormId);
        eFragment.setArguments(args);
        return  eFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set id of eForm an edited flag
        if (getArguments() != null) eFormId = getArguments().getLong(eFORM_ID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //show menu
        setHasOptionsMenu(true);
        //Initialize fields
        mActivity = (AbbMainActivity) getActivity();
        notifyEfList = savedInstanceState == null;
        thisEf = AbbContent.eFORM_ITEMS.get(eFormId);
        thisEfListIdx = AbbContent.eFORM_ITEM_LIST.indexOf(thisEf);
        qItemList = AbbContent.getQuestionItems(eFormId);
        aItemList = AbbContent.getAnswersItems(eFormId);
        efAllLines = AbbContent.getContentByLines(thisEf.efName, false);
        // Inflate the View for this fragment
        return inflater.inflate(R.layout.fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        //Set title
        CollapsingToolbarLayout appBarLayout = mActivity.findViewById(R.id.toolbar_layout);
        appBarLayout.setTitle(thisEf.efName);
        //set image in appbar
        AppCompatImageView imageView = mActivity.findViewById(R.id.toolbar_image);
        Bitmap bmp = BitmapFactory.decodeFile(AbbContent
                .getAbbFilesPath()+"/"+String.valueOf(eFormId)+".jpeg");
        if (bmp != null) imageView.setImageBitmap(bmp);
        else imageView.setImageBitmap(null);
        // add back arrow to toolbar
        toolbar = mActivity.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbbMainActivity.gSFManager.popBackStack();
            }
        });
        // Set the adapter
        editAdapter = new EfEditAdapter(this, qItemList);
        recyclerView = view.findViewById(R.id.list_view);
        recyclerView.setAdapter(editAdapter);
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
                        // move item `fromPos` to `toPos` .
                        AbbContent.QuestionItem qItemToMove = qItemList.get(fromPos);
                        qItemList.remove(qItemToMove);
                        efAllLines.remove(fromPos + 1);
                        qItemList.add(toPos, qItemToMove);
                        efAllLines.add(toPos + 1, qItemToMove.getString());
                        editAdapter.notifyItemMoved(fromPos, toPos);
                        saveOnEForm();
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction){
                        int thisItemPosition = viewHolder.getAdapterPosition();
                        if(qItemList.get(thisItemPosition).qId == -1) {
                            qItemList.remove(thisItemPosition);
                            editAdapter.notifyItemRemoved(thisItemPosition);
                            if (qItemList.size() == 0) showNoDataMessage();
                        } else deleteQ(qItemList.get(viewHolder.getAdapterPosition()));
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        //Set no data view, if list is empty, show empty msg
        noDataView = view.findViewById(R.id.no_data_view);
        noDataView.setText(R.string.empty_q_list_message);
        noDataView.setOnClickListener(newQClkListener());
        if (editAdapter.getItemCount() == 0) showNoDataMessage();
        //Set fab and collapsing views
       mActivity.aBtn.setText(getString(R.string.a_hint, String.valueOf(aItemList.size())));
        mActivity.aBtn.setVisibility(View.VISIBLE);
        mActivity.aBtn.setOnClickListener(mActivity.setAnsViewClkListener(eFormId));
        mActivity.mFAB = mActivity.findViewById(R.id.fab);
        mActivity.mFAB.setOnClickListener(newQClkListener());
        mActivity.mFAB.show();
        //show bottom ad
        mActivity.setAd((AdView) view.findViewById(R.id.ad_view));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        MenuItem abbFileMenu = menu.add(Menu.NONE, 3, 3, getString(R.string.menu_create_abb));
        abbFileMenu.setIcon(R.drawable.ic_abb_generate);
        abbFileMenu.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        inflater.inflate(R.menu.item_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        EfListFragment lFrag=(EfListFragment) AbbMainActivity.gSFManager.findFragmentByTag("eFormList");
        switch (id){
            case R.id.edit:
                AbbContent.eFormItem editedEf = new AbbContent.eFormItem
                        (thisEf.efName, thisEf.efId, thisEf.efOwner, "new", thisEf.efDetails);
                if (lFrag != null) lFrag.upDateCard(thisEfListIdx, editedEf, notifyEfList);
                AbbMainActivity.gSFManager.popBackStack();
                return true;
            case R.id.share:
                mActivity.sendFile(AbbFileGenerator.saveEFromFile(eFormId));
                return true;
            case R.id.delete:
                if (lFrag != null) lFrag.efDelete(AbbContent.eFORM_ITEMS.get(eFormId));
                AbbMainActivity.gSFManager.popBackStack();
                return true;
            case 3:
                if (AbbFileGenerator.saveEFromFile(eFormId) != null) {
                    mActivity.showSnackBarMsg(getString(R.string.file_generated_msg), 0,
                            android.R.string.ok, null);
                    mActivity.showRewardedAd();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        mActivity.mFAB.hide();
        //set toolbar and buttons at default
        toolbar.setNavigationIcon(R.drawable.ic_menu_hamburger);
        toolbar.setNavigationOnClickListener(mActivity.setNavClkListener());
    }

    /**Call to show msg when is no q*/
    public void showNoDataMessage() {noDataView.setVisibility(View.VISIBLE);}

    /**To set the new q click listener*/
    private View.OnClickListener newQClkListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (noDataView.getVisibility() == View.VISIBLE) noDataView.setVisibility(View.GONE);
                AbbContent.QuestionItem newQItem = new AbbContent.QuestionItem(eFormId,
                        -1, -1, "", new ArrayList<String>());
                qItemList.add(newQItem);
                int idx = qItemList.indexOf(newQItem);
                editAdapter.notifyItemInserted(idx);
                recyclerView.smoothScrollToPosition(idx);
            }
        };
    }

    /**call to save content on the eForm*/
    public void saveOnEForm(){
        //when eForm is edited, set info line, then writhe file
        String[] efDetails = thisEf.efDetails;
        efDetails[1] = new Date().toString();
        efDetails[2] =  String.valueOf(qItemList.size());
        AbbContent.eFormItem editedEf = new AbbContent.eFormItem
                (thisEf.efName, thisEf.efId, thisEf.efOwner, thisEf.efType, efDetails);
        efAllLines.set(0, editedEf.getString());
        AbbContent.writeEFormFile(thisEf.efName, efAllLines,false);
        EfListFragment lFrag = (EfListFragment)
                AbbMainActivity.gSFManager.findFragmentByTag("eFormList");
        if (lFrag != null) lFrag.upDateCard(thisEfListIdx, editedEf, notifyEfList);
    }

    /**call when tap save button in question form*/
    public void saveQ(AbbContent.QuestionItem qToSave, int qPos){
        qItemList.set(qPos, qToSave);
        if (qItemList.size() == qPos) efAllLines.add(qToSave.getString());
        else {
            if (efAllLines.size() == qPos+1) efAllLines.add(qToSave.getString());
            else efAllLines.set(qPos+1, qToSave.getString());
        }
        //set the image
        AbbContent.renameToGet(AbbContent.getAbbFilesPath()
                +"/lastPick.jpeg", String.valueOf(qPos)+String.valueOf(eFormId)+".jpeg");
        editAdapter.notifyItemChanged(qItemList.indexOf(qToSave));
        //actualize coins on firs question
        saveOnEForm();
    }

    /**To delete actual question*/
    public void deleteQ(final AbbContent.QuestionItem qToDelete) {
        final int posToRemove = qItemList.indexOf(qToDelete);
        qItemList.remove(qToDelete);
        efAllLines.remove(posToRemove+1);
        editAdapter.notifyItemRemoved(posToRemove);
        final String imaDelAppend;
        if(AbbContent.delImageFile
                (String.valueOf(posToRemove)+String.valueOf(eFormId)+".jpeg"))
            imaDelAppend = getString(R.string.image_deleted);
        else imaDelAppend = "";
        //Recreate question  if user tap undo on snack bar message
        if (qItemList.size() == 0) showNoDataMessage();
        else {
            mActivity.showSnackBarMsg(getString(R.string.deleted) + "\n" + imaDelAppend, 0,
                    R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            efAllLines.add(posToRemove +1, qToDelete.getString());
                            qItemList.add(posToRemove, qToDelete);
                            editAdapter.notifyItemInserted(posToRemove);
                        }
                    });
        }
        saveOnEForm();
    }
}
