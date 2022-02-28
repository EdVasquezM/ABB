package com.sidemvm.ed.abluebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.android.gms.ads.AdView;

import java.util.List;

/** Created by super on 27/03/2018.*/

public class EfListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //Create fields
    private final List<AbbContent.eFormItem> efItemList;
    private final EfListFragment lFrag;

    //constructor
    EfListAdapter(EfListFragment lFrag, List<AbbContent.eFormItem> efItemList) {
        this.efItemList = efItemList;
        this.lFrag = lFrag;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //use proper view when creating/editing
        switch (viewType) {
            case 0:
                return new newEFormViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.ef_new_content_layout, parent, false));
            case 2:
                return new eFormAdViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.ad_list_layout, parent,false));
            default:
                return new eFormViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.ef_content_layout, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AbbContent.eFormItem thisEForm = efItemList.get(position);
        switch (holder.getItemViewType()) {
            case 0: //set views
                newEFormViewHolder newEFormViewHolder = (newEFormViewHolder) holder;
                newEFormViewHolder.nCardView.setTag(thisEForm);
                newEFormViewHolder.nCardView.requestFocus();
                newEFormViewHolder.nOwnerView.setText(thisEForm.efOwner);
                //set text on views hen editing
                if (thisEForm.efId != 0) {
                    newEFormViewHolder.nNameInput.setText(thisEForm.efName);
                    newEFormViewHolder.nCommentsInput
                            .setText(lFrag.mActivity.setTextWithLineBreaks(thisEForm.efDetails[3]));
                }
                break;
            case 2: //set ad view
                eFormAdViewHolder eFormAdViewHolder = (eFormAdViewHolder) holder;
                lFrag.mActivity.setAd(eFormAdViewHolder.efAdView);
                break;
            default: //set views
                eFormViewHolder eFormViewHolder = (eFormViewHolder) holder;
                eFormViewHolder.efCardView.setTag(thisEForm);
                Bitmap bmp = BitmapFactory.decodeFile(AbbContent
                        .getAbbFilesPath() + "/" + String.valueOf(thisEForm.efId) + ".jpeg");
                if (bmp != null) eFormViewHolder.efImageView.setImageBitmap(bmp);
                else eFormViewHolder.efImageView.setImageResource(R.mipmap.ic_launcher);
                eFormViewHolder.efNameView.setText(thisEForm.efName);
                eFormViewHolder.efNameView.setSelected(true);
                eFormViewHolder.efOwnerView.setText(thisEForm.efOwner);
                eFormViewHolder.efQsView
                        .setText(lFrag.getString(R.string.q_hint, thisEForm.efDetails[2]));
                eFormViewHolder.efAsView.setText(lFrag.getString(R.string.a_hint,
                        String.valueOf(AbbContent.getAnswersItems(thisEForm.efId).size())));
                eFormViewHolder.efCommentsView
                        .setText(lFrag.mActivity.setTextWithLineBreaks(thisEForm.efDetails[3]));
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return efItemList.get(position).efId;
    }

    @Override
    public int getItemCount() {
        return efItemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (efItemList.get(position).efType) {
            case "new":
                return 0;
            case "ad":
                return 2;
            default:
                return 1;
        }
    }

    //Class to visualize every e quiz item
    public class eFormViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //Create views
        final CardView efCardView;
        final AppCompatImageView efImageView;
        final AppCompatTextView efNameView;
        final AppCompatTextView efOwnerView;
        final AppCompatTextView efCommentsView;
        final AppCompatTextView efQsView;
        final AppCompatTextView efAsView;

        eFormViewHolder(View itemView) {
            super(itemView);
            //Initialize views
            efCardView = itemView.findViewById(R.id.ef_card_view);
            efImageView = itemView.findViewById(R.id.ef_item_image_view);
            efNameView = itemView.findViewById(R.id.ef_item_name_view);
            efOwnerView = itemView.findViewById(R.id.ef_item_owner_view);
            efCommentsView = itemView.findViewById(R.id.ef_item_comments_view);
            efQsView = itemView.findViewById(R.id.ef_item_q_count_view);
            efAsView = itemView.findViewById(R.id.ef_item_a_count_view);
            //Set click event
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            lFrag.mActivity.eFormEditIntent(efItemList.get(getAdapterPosition()).efId);
        }
    }

    //Class to visualize new test form item
    public class newEFormViewHolder extends RecyclerView.ViewHolder {
        //Create views
        final CardView nCardView;
        final EditText nNameInput;
        final AppCompatTextView nOwnerView;
        final CheckBox tapperCheck;
        final EditText nCommentsInput;
        final Toolbar nOptionsView;

        newEFormViewHolder(View itemView) {
            super(itemView);
            //Initialize views
            nCardView = itemView.findViewById(R.id.new_ef_card_view);
            nNameInput = itemView.findViewById(R.id.new_ef_item_name_view);
            nOwnerView = itemView.findViewById(R.id.new_ef_item_owner_view);
            tapperCheck = itemView.findViewById(R.id.tapper_ef_check);
            //set listener when check to indicate user what tapper means
            tapperCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) lFrag.mActivity
                            .showSnackBarMsg
                                    (lFrag.mActivity.getString(R.string.new_ef_tapper_tooltip), -2,
                                            android.R.string.ok, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                }
                                            });
                }
            });
            nCommentsInput = itemView.findViewById(R.id.new_ef_item_comments_view);
            //set card options
            nOptionsView = itemView.findViewById(R.id.new_ef_card_options_view);
            nOptionsView.inflateMenu(R.menu.new_item_menu);
            nOptionsView.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int thisItemPosition = getAdapterPosition();
                    AbbContent.eFormItem thisEQ = efItemList.get(thisItemPosition);
                    switch (item.getItemId()) {
                        case R.id.save:
                            if (thisEQ.efId == 0) {
                                if (lFrag.efNewFromForm(nNameInput, nCommentsInput, tapperCheck)) {
                                    AbbContent.removeFormItem(thisEQ);
                                    notifyItemRemoved(thisItemPosition);
                                    nNameInput.setError(null);
                                    nNameInput.setText("");
                                    nCommentsInput.setText("");
                                    return true;
                                } else return false;
                            } else {
                                if (lFrag.efEdit(thisEQ, nNameInput, nCommentsInput, tapperCheck)) {
                                    notifyItemChanged(thisItemPosition);
                                    nNameInput.setError(null);
                                    nNameInput.setText("");
                                    nCommentsInput.setText("");
                                    lFrag.mActivity.showIntAd();
                                    return true;
                                } else return false;
                            }
                        case R.id.attach:
                            if (lFrag.mActivity.selectImageIntent())
                                item.setIcon(android.R.drawable.ic_menu_report_image);
                            return false;
                        case R.id.cancel:
                            if (thisEQ.efId == 0) {
                                AbbContent.removeFormItem(thisEQ);
                                notifyItemRemoved(thisItemPosition);
                                if (getItemCount() == 0) lFrag.showNoDataMessage();
                                nNameInput.setError(null);
                                nNameInput.setText("");
                                nCommentsInput.setText("");
                                return true;
                            } else {
                                AbbContent.eFormItem eqToEdit = new AbbContent.eFormItem
                                        (thisEQ.efName, thisEQ.efId, thisEQ.efOwner, "eQuiz",
                                                thisEQ.efDetails);
                                lFrag.upDateCard(thisItemPosition, eqToEdit, false);
                                notifyItemChanged(thisItemPosition);
                                nNameInput.setError(null);
                                nNameInput.setText("");
                                nCommentsInput.setText("");
                                lFrag.mActivity.showIntAd();
                                return true;
                            }
                        default:
                            return false;
                    }
                }
            });
        }
    }

    //Class to visualize new test form item
    public class eFormAdViewHolder extends RecyclerView.ViewHolder {

        //Create views
        final AdView efAdView;

        eFormAdViewHolder(@NonNull View itemView) {
            super(itemView);
            //Initialize views
            efAdView = itemView.findViewById(R.id.ef_ad_iew);
        }
    }
}
