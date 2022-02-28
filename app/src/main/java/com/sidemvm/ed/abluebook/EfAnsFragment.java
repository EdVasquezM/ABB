package com.sidemvm.ed.abluebook;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Created by super on 02/01/2018. */

public class EfAnsFragment extends Fragment {
    /**Main activity*/
    private AbbMainActivity mActivity;
    /**List of questions and list of answers*/
    List<AbbContent.QuestionItem> qItemList;
    AbbContent.AnswerItem aToShow;
    private RecyclerView recyclerView;
    private EfAnsAdapter efAnsAdapter;
    private Toolbar toolbar;
    /** The content of the file to edit*/
    public static final String eFORM_ID = "e_form_id";
    public static final String DATE_ANS_TO_SHOW = "date_ans_to_show";
    public static final String QUESTION = "question";
    public static final String QUESTION_ID = "question_id";
    /** The id of the eForm selected*/
    public static long eFormId;
    public AbbContent.eFormItem thisEf;
    public static String dAns;
    public static String question;
    public static long qId;
    /** Mandatory empty constructor */
    public EfAnsFragment() {}

    public static EfAnsFragment newInstance(long eQuizId, String dAns, String question, long qid) {
        final EfAnsFragment aFrag = new EfAnsFragment();
        final Bundle args = new Bundle();
        args.putLong(eFORM_ID, eQuizId);
        args.putString(DATE_ANS_TO_SHOW, dAns);
        args.putLong(QUESTION_ID, qid);
        args.putString(QUESTION, question);
        aFrag.setArguments(args);
        return  aFrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set id and date of ans to show of eForm
        if (getArguments() != null) {
            eFormId = getArguments().getLong(eFORM_ID);
            dAns = getArguments().getString(DATE_ANS_TO_SHOW);
            qId = getArguments().getLong(QUESTION_ID);
            question = getArguments().getString(QUESTION);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //show menu
        setHasOptionsMenu(true);
        //Initialize fields
        mActivity = (AbbMainActivity) getActivity();
        // Inflate the View for this fragment
        if(!dAns.equals("eFormTapper")) {
            qItemList = AbbContent.getQuestionItems(eFormId);
            thisEf = AbbContent.eFORM_ITEMS.get(eFormId);
            return inflater.inflate(R.layout.fragment_layout, container, false);
        } else return inflater.inflate(R.layout.instant_q_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        //retrieve ans when screen change
        if (savedInstanceState != null){
            LongSparseArray<String> as = new LongSparseArray<>();
            long[] longs = savedInstanceState.getLongArray("longs");
            if(longs != null){
                for (long aLong : longs) {
                    as.put(aLong, savedInstanceState.getString(String.valueOf(aLong)));
                }
            } aToShow = new AbbContent.AnswerItem(eFormId, mActivity.eFormOwner, "", as);
        }
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
        //Set fab and collapsing views
        //mActivity.setAbbCoinView(mActivity.abbCoinView,
        //        getString(R.string.abb_coins_owner, String.valueOf(eFormId)), 0, false);
        mActivity.aBtn.setText(getString(R.string.a_hint,
                String.valueOf(AbbContent.getAnswersItems(eFormId).size())));
        mActivity.aBtn.setVisibility(View.VISIBLE);
        mActivity.aBtn.setOnClickListener(mActivity.setAnsViewClkListener(eFormId));
        mActivity.mFAB = mActivity.findViewById(R.id.fab);
        mActivity.mFAB.hide();
        //set fab visibility
        if(dAns.equals("")) {
            mActivity.mFAB.setImageResource(android.R.drawable.ic_menu_save);
            mActivity.mFAB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        saveA();
                    }
                });
            mActivity.mFAB.show();
        }
        if(!dAns.equals("eFormTapper")){
            //Set title
            CollapsingToolbarLayout appBarLayout = mActivity.findViewById(R.id.toolbar_layout);
            appBarLayout.setTitle(thisEf.efName);
            //Set the recycler view and adapter
            recyclerView = view.findViewById(R.id.list_view);
            setPrevious(dAns);
        } else {
            // Set views
            TextView questionView = view.findViewById(R.id.show_question);
            questionView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Large);
            questionView.setText(question);
            EditText aBox = view.findViewById(R.id.input_answer);
            view.findViewById(R.id.yes_button)
                    .setOnClickListener(finalMessageToSend(getString(R.string.yes), aBox));
            view.findViewById(R.id.no_button)
                    .setOnClickListener(finalMessageToSend(getString(R.string.no), aBox));
            //set click event of addQuestion button and cleans relative layout
            mActivity.mFAB.setImageResource(android.R.drawable.ic_menu_send);
            mActivity.mFAB.setOnClickListener(finalMessageToSend("", aBox));

        }
        //show bottom ad
        mActivity.setAd((AdView) view.findViewById(R.id.ad_view));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        if(!dAns.equals("eFormTapper")){
            AbbContent.AnswerItem ans = efAnsAdapter.retrieveData();
            long[] longs = new long[ans.as.size()];
            for(int i = 0; i < ans.as.size(); i++ ){
                longs[i]= ans.as.keyAt(i);
                outState.putString(String.valueOf(longs[i]), ans.as.valueAt(i));
            } outState.putLongArray("longs", longs);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.ans_menu, menu);
        if(!dAns.equals("")) menu.findItem(R.id.save).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.save:
                saveA();
                return true;
            case R.id.share:
                sendA();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        //set toolbar at default
        mActivity.mFAB.setImageResource(android.R.drawable.ic_input_add);
        mActivity.mFAB.setOnClickListener(null);
        mActivity.mFAB.hide();
        toolbar.setNavigationIcon(R.drawable.ic_menu_hamburger);
        toolbar.setNavigationOnClickListener(mActivity.setNavClkListener());
    }

    /**set questions if needed and change aForm to show it if last modification was before*/
    public void setPrevious(String dAns){
        //set aToShow
        List<AbbContent.AnswerItem> aItemList = AbbContent.getAnswersItems(eFormId);
        for (int i = 0; i < aItemList.size(); i++)
            if(aItemList.get(i).aDate.equals(dAns) && !aItemList.get(i).aDate.equals(""))
                aToShow = aItemList.get(i);
        if (aToShow == null) aToShow = new AbbContent
                .AnswerItem(eFormId, mActivity.eFormOwner, "", new LongSparseArray<String>());
        //change questions
        if(!dAns.equals("")){
            try {
                Date ansDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'Z yyyy",
                        Locale.ENGLISH).parse(dAns);
                Date efLastMod = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'Z yyyy",
                        Locale.ENGLISH).parse(thisEf.efDetails[1]);
                if (!efLastMod.before(ansDate)){
                    qItemList = new ArrayList<>();
                    mActivity.showSnackBarMsg(getString(R.string.ans_after_ef_msg), -2,
                            android.R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {}
                            });
                    for(int i = 0; i < aToShow.as.size(); i++){
                        qItemList.add(new AbbContent.QuestionItem(eFormId, i, 0,
                                getString(R.string.q_num_hint, String.valueOf(i+1)), null));

                    }
                }
            } catch (ParseException e) {
                mActivity.showSnackBarMsg(getString(R.string.getting_date_fail_msg)+"\n"+e,
                        0, android.R.string.ok, null);
            }
        }
        //set Recycler View
        efAnsAdapter = new EfAnsAdapter(this, qItemList, aToShow);
        recyclerView.setAdapter(efAnsAdapter);
    }

    public void saveA(){
        List<String> linesToWrite = new ArrayList<>();
        linesToWrite.add(getALine());
        if(AbbContent.writeEFormFile(thisEf.efName, linesToWrite, true)) {
            mActivity.showSnackBarMsg(getString(R.string.success_message), -1,
                    android.R.string.ok, null);
            mActivity.showIntAd();
        }
        else mActivity.showSnackBarMsg(getString(R.string.fail_message),0,
                android.R.string.ok,null);
    }

    public void sendA(){
        if(AbbMainActivity.epIdList.size() != 0) mActivity.sendMessage(getALine());
        else mActivity.sendFile(AbbFileGenerator.saveAnsFile(getString(R.string.
                    a_hint,thisEf.efName), getALine()));
    }

    public String getALine(){
        AbbContent.AnswerItem aFromForm = efAnsAdapter.retrieveData();
        AbbContent.AnswerItem a = new AbbContent
                .AnswerItem(eFormId, mActivity.eFormOwner, new Date().toString(), aFromForm.as);
        return a.getString();
    }

    /**listener to send a message when instant question*/
    public View.OnClickListener finalMessageToSend (final String answer, final EditText aBox){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LongSparseArray<String> as = new LongSparseArray<>();
                as.put(qId, answer+". "+aBox.getText().toString());
                AbbContent.AnswerItem a = new AbbContent.AnswerItem
                        (eFormId, mActivity.eFormOwner, new Date().toString(), as);
                if(AbbMainActivity.epIdList.size() != 0) mActivity.sendMessage(a.getString());
                else mActivity.sendFile(AbbFileGenerator.saveAnsFile
                            (getString(R.string.a_hint,thisEf.efName), a.getString()));
                AbbMainActivity.gSFManager.popBackStack();
            }
        };
    }
}
