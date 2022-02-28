package com.sidemvm.ed.abluebook;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/** Created by super on 27/12/2017. */

public class listDialogFrag extends BottomSheetDialogFragment {

    /**Main activity*/
    public AbbMainActivity mActivity;
    /** The content of the file to edit*/
    public static final String eFORM_ID = "e_form_id";
    /** The id of the eForm selected*/
    public static long eFormId;
    /**set views*/
    AppCompatTextView abbCoinView;
    bsdListAdapter listAdapter;

    public static listDialogFrag newInstance(long eFormId) {
        final listDialogFrag fragment = new listDialogFrag();
        final Bundle args = new Bundle();
        args.putLong(eFORM_ID, eFormId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set id of eForm
        if (getArguments() != null) eFormId = getArguments().getLong(eFORM_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Initialize fields
        mActivity = (AbbMainActivity) getActivity();
        return inflater.inflate(R.layout.ans_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        final List<AbbContent.AnswerItem> aItemList = AbbContent.getAnswersItems(eFormId);
        // get the abb coin amount for this
        abbCoinView = view.findViewById(R.id.abb_coin_view);
        MaterialButton csvBtn = view.findViewById(R.id.import_csv_btn);
        if (eFormId == 0){
            listAdapter = new bsdListAdapter(null);
            abbCoinView.setText(getString
                    (R.string.drawer_ep_count, String.valueOf(AbbMainActivity.epList.size())));
            csvBtn.setText(R.string.share_all_hint);
            csvBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AbbMainActivity.epIdList = new ArrayList<>(AbbMainActivity.epList.keySet());
                    listDialogFrag.this.dismiss();
                }
            });
        }
        else {
            csvBtn.setText(getString(R.string.export_csv_hint, ""));
            //import ready answers to a csv file
            csvBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String importResult = AbbFileGenerator.generateCsvFile(eFormId, aItemList);
                    if (importResult.equals("ok")) {
                        mActivity.showSnackBarMsg(getString(R.string.file_generated_msg),
                                -1, android.R.string.ok, null);
                    }
                    else mActivity.showSnackBarMsg(getString(R.string.fail_message) + "\n" +
                            importResult, 0, android.R.string.ok, null);
                    listDialogFrag.this.dismiss();
                }
            });
            listAdapter = new bsdListAdapter(aItemList);
        }
        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(listAdapter);
    }

    /**view holder to the answers on the list*/
    private class ViewHolder extends RecyclerView.ViewHolder {

        final CardView itemCardView;
        final AppCompatTextView ownerView;
        final AppCompatTextView dataView;
        final MaterialButton showDataBtn;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.ans_list_item, parent, false));
            itemCardView = itemView.findViewById(R.id.ans_card_view);
            ownerView = itemView.findViewById(R.id.ans_owner_view);
            dataView = itemView.findViewById(R.id.ans_date_view);
            showDataBtn = itemView.findViewById(R.id.show_ans_btn);
        }
    }

    /**adapter for the list of answers*/
    private class bsdListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final List<AbbContent.AnswerItem> ansList;
        private final List<String> epList;
        private final List<String> epIdList;

        bsdListAdapter(List<AbbContent.AnswerItem> ansList) {
            this.ansList = ansList;
            List<String> eps = new ArrayList<>();
            List<String> epIds = new ArrayList<>();
            for (String epId: AbbMainActivity.epList.keySet()) {
                eps.add(AbbMainActivity.epList.get(epId));
                epIds.add(epId);
            }
            this.epList = eps;
            this.epIdList = epIds;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            if(ansList != null) {// show the answer on answer fragment
                holder.itemCardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.dataView.getVisibility() == View.VISIBLE) {
                            EfAnsFragment ansFrag = (EfAnsFragment)
                                    AbbMainActivity.gSFManager.findFragmentByTag("Answering");
                            if (ansFrag != null)
                                ansFrag.setPrevious(ansList.get(holder.getAdapterPosition()).aDate);
                            else {
                                AbbMainActivity.gSFManager.popBackStack();
                                AbbMainActivity.gSFManager.beginTransaction().setCustomAnimations
                                        (R.anim.in_from_right, R.anim.out_to_left, R.anim.in_from_left,
                                                R.anim.out_to_right).replace(R.id.fragmentContainer,
                                        EfAnsFragment.newInstance(eFormId, ansList.get(holder
                                                .getAdapterPosition()).aDate, "", 0),
                                        "Answering").addToBackStack(null).commit();
                            }
                            listDialogFrag.this.dismiss();
                        }
                    }
                });
                holder.ownerView.setText(ansList.get(position).aOwner);
                holder.ownerView.setSelected(true);
                holder.dataView.setText(ansList.get(position).aDate);
                holder.showDataBtn.setText(getString(R.string.unlock_answer_hint, "tap"));
                // show answer date
                holder.showDataBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mActivity.showRewardedAd();
                        holder.showDataBtn.setVisibility(View.GONE);
                        holder.dataView.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                String ep = epList.get(holder.getAdapterPosition());
                final String epId = epIdList.get(holder.getAdapterPosition());
                holder.itemCardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!AbbMainActivity.epIdList.contains(epId))
                            AbbMainActivity.epIdList.add(epId);
                        holder.showDataBtn.setVisibility(View.VISIBLE);
                        holder.dataView.setVisibility(View.GONE);
                    }
                });
                holder.ownerView.setText(ep);
                holder.ownerView.setSelected(true);
                holder.dataView.setText(getString(R.string.unlock_answer_hint, "tap"));
                if(!AbbMainActivity.epIdList.contains(epId))
                    holder.dataView.setVisibility(View.VISIBLE);
                holder.showDataBtn.setText(R.string.menu_share);
                if(!AbbMainActivity.epIdList.contains(epId))
                    holder.showDataBtn.setVisibility(View.GONE);
                holder.showDataBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AbbMainActivity.epIdList.remove(epId);
                        holder.showDataBtn.setVisibility(View.GONE);
                        holder.dataView.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            if(ansList != null) return ansList.size();
            else return epList.size();
        }
    }
}
