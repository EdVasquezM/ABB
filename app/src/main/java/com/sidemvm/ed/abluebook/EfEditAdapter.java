package com.sidemvm.ed.abluebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

/** Created by super on 04/02/2018.*/

public class EfEditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //Create fields
    private final EfEditFragment editFrag;
    private List<AbbContent.QuestionItem> qItemList;

    //constructor
    EfEditAdapter(EfEditFragment editFrag, List<AbbContent.QuestionItem> qItemList) {
        this.editFrag = editFrag;
        this.qItemList = qItemList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //use proper view if question or new question form
        switch (viewType) {
            case -1:
                return new NewQViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.q_new_content_layout, parent, false));
            default:
                return new QViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.q_content_layout, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AbbContent.QuestionItem thisQuestion = qItemList.get(position);
        switch (holder.getItemViewType()){
            case -1 : //set views
                NewQViewHolder newQViewHolder = (NewQViewHolder) holder;
                newQViewHolder.nQCardView.setTag(thisQuestion);
                newQViewHolder.nQCardView.requestFocus();
                newQViewHolder.nQInput.setText(thisQuestion.q);
                //set as if is multi a question
                if (thisQuestion.as.size() != 0){
                    newQViewHolder.nQType.setSelection(1);
                    newQViewHolder.nALayout.removeAllViews();
                    for (String a : thisQuestion.as)
                        newQViewHolder.nALayout.addView(newQViewHolder.setMultiAView(a));
                }
                break;
            default: //set views
                QViewHolder qViewHolder = (QViewHolder) holder;
                qViewHolder.qCardView.setTag(thisQuestion);
                qViewHolder.qView.setText(thisQuestion.q);
                Bitmap bmp = BitmapFactory.decodeFile(AbbContent.getAbbFilesPath()+"/"+
                        String.valueOf(position)+String.valueOf(thisQuestion.efId)+".jpeg");
                if (bmp != null) qViewHolder.qImaView.setImageBitmap(bmp);
                qViewHolder.aLayout.removeAllViews();
                //set share button visibility if is tapper eForm
                AbbContent.eFormItem efItem = AbbContent.eFORM_ITEMS.get(thisQuestion.efId);
                if (efItem != null && !efItem.efType.equals("eFormTapper"))
                    qViewHolder.qOptions.getMenu().getItem(1).setVisible(false);
                //set as if question is multi a
                if (thisQuestion.qType == 1){
                    for (String everyA: thisQuestion.as){
                        CheckBox everyAView = new CheckBox(editFrag.getContext());
                        everyAView.setButtonDrawable(R.drawable.checkbox_selector);
                        everyAView.setText(everyA);
                        everyAView.setTextColor(editFrag.getResources().getColor
                                (android.R.color.secondary_text_light));
                        qViewHolder.aLayout.addView(everyAView);
                    }
                } else {
                    TextView qOpenInput = new TextView(editFrag.getContext());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        qOpenInput.setBackground
                                (editFrag.getResources().getDrawable(R.drawable.depth_background));
                    }
                    qOpenInput.setText(R.string.open_a_input_hint);
                    qOpenInput.setTextColor
                            (editFrag.getResources().getColor(android.R.color.secondary_text_light));
                    qViewHolder.aLayout.addView(qOpenInput);
                }
                break;
        }
    }

    @Override
    public long getItemId (int position) {return qItemList.get(position).qId;}

    @Override
    public int getItemCount() {return qItemList.size();}

    @Override
    public int getItemViewType(int position) {return  qItemList.get(position).qType;}

    //Class to visualize every question item
    public class QViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        //Create views
        final CardView qCardView;
        final AppCompatTextView qView;
        final AppCompatImageView qImaView;
        final LinearLayout aLayout;
        final Toolbar qOptions;

        QViewHolder(View itemView) {
            super(itemView);
            //Initialize views
            qCardView = itemView.findViewById(R.id.q_card_view);
            qView = itemView.findViewById(R.id.q_text_view);
            qImaView = itemView.findViewById(R.id.q_item_image_view);
            aLayout = itemView.findViewById(R.id.a_layout);
            //set card options
            qOptions = itemView.findViewById(R.id.q_options_view);
            qOptions.inflateMenu(R.menu.item_options_menu);
            qOptions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int thisItemPos = getAdapterPosition();
                    AbbContent.QuestionItem thisQ = qItemList.get(thisItemPos);
                    switch (item.getItemId()){
                        case R.id.edit:
                            AbbContent.QuestionItem qToEdit = new AbbContent.QuestionItem
                                    (thisQ.efId, thisQ.qId, -1, thisQ.q, thisQ.as);
                            qItemList.set(thisItemPos, qToEdit);
                            notifyItemChanged(thisItemPos);
                            return true;
                        case R.id.share:
                            if(AbbMainActivity.epIdList.size() != 0)
                                editFrag.mActivity.sendMessage(thisQ.getString());
                            return  true;
                        case R.id.delete:
                            editFrag.deleteQ(qItemList.get(thisItemPos));
                            return true;
                        default: return false;
                    }
                }
            });
            //Set on every view the click event
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //set options visibility
            if (qOptions.getVisibility() == View.GONE) {
                qOptions.setVisibility(View.VISIBLE);
                qView.setCompoundDrawablesWithIntrinsicBounds
                        (0,0, android.R.drawable.arrow_up_float, 0);
            }
            else {
                qOptions.setVisibility(View.GONE);
                qView.setCompoundDrawablesWithIntrinsicBounds
                        (0,0, android.R.drawable.arrow_down_float, 0);
            }
        }
    }

    //Class to visualize new test form item
    public class NewQViewHolder extends RecyclerView.ViewHolder {
        //Create views
        final CardView nQCardView;
        final EditText nQInput;
        final Spinner nQType;
        final LinearLayout nALayout;
        final TextInputLayout nQOpenAView;
        final EditText nAInput;
        final Toolbar nMultiAView;
        final Toolbar nQOptionsView;
        private LayoutInflater li;

        NewQViewHolder(final View itemView) {
            super(itemView);
            //Initialize views
            li = LayoutInflater.from(editFrag.getContext());
            nQCardView = itemView.findViewById(R.id.new_q_card_view);
            nQInput = itemView.findViewById(R.id.new_q_input);
            nQType = itemView.findViewById(R.id.q_type_spinner);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(itemView.getContext(),
                    R.array.question_types, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter an item selected listeners to the spinner
            nQType.setAdapter(adapter);
            nQType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                    if (position == 1){
                        nALayout.removeAllViews();
                        nALayout.addView(nMultiAView);
                        if (qItemList.get(getAdapterPosition()).as.size() != 0){
                            for (String everyA: qItemList.get(getAdapterPosition()).as)
                                nALayout.addView(setMultiAView(everyA));
                        }
                    } else {
                        nALayout.removeAllViews();
                        nALayout.addView(nQOpenAView);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            //inflate layouts to dynamically include it
            nALayout = itemView.findViewById(R.id.a_layout);
            nQOpenAView = (TextInputLayout)
                    li.inflate(R.layout.layout_essai_q, nALayout, false);
            nMultiAView = (Toolbar)
                    li.inflate(R.layout.layout_edit_multi_a_q, nALayout, false);
            //add an set the new a view
            nAInput = nMultiAView.findViewById(R.id.new_multi_a_input);
            nMultiAView.inflateMenu(R.menu.new_item_menu);
            nMultiAView.getMenu().getItem(1).setVisible(false);
            nMultiAView.setOnMenuItemClickListener(defaultClkListener());
            //set card options menu
            nQOptionsView = itemView.findViewById(R.id.new_q_card_options);
            nQOptionsView.inflateMenu(R.menu.new_item_menu);
            nQOptionsView.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int thisItemPosition = getAdapterPosition();
                    switch (item.getItemId()){
                        case R.id.save:
                            AbbContent.QuestionItem qToSave = qItemList.get(thisItemPosition);
                            editFrag.saveQ(getQItemFromForm(qToSave.efId, nQInput, nQType,
                                    thisItemPosition, qToSave.as), thisItemPosition);
                            nQType.setSelection(0);
                            nALayout.removeAllViews();
                            nALayout.addView(nQOpenAView);
                            nQInput.setText("");
                            nAInput.setText("");
                            return true;
                        case R.id.attach:
                            //editFrag.saveQ(qItemList.get(thisItemPosition), thisItemPosition);
                            if (editFrag.mActivity.selectImageIntent())
                                item.setIcon(android.R.drawable.ic_menu_report_image);
                            return false;
                        case R.id.cancel:
                            if(qItemList.get(thisItemPosition).qId == -1) {
                                qItemList.remove(thisItemPosition);
                                notifyItemRemoved(thisItemPosition);
                            } else {
                                AbbContent.QuestionItem thisQ = qItemList.get(thisItemPosition);
                                int qType;
                                if (thisQ.as.size() != 0) qType = 1;
                                else qType = 0;
                                AbbContent.QuestionItem qToEdit = new AbbContent.QuestionItem
                                        (thisQ.efId, thisQ.qId, qType, thisQ.q, thisQ.as);
                                qItemList.set(thisItemPosition, qToEdit);
                                notifyItemChanged(thisItemPosition);
                            }
                            if (qItemList.size() == 0) editFrag.showNoDataMessage();
                            nQType.setSelection(0);
                            nALayout.removeAllViews();
                            nALayout.addView(nQOpenAView);
                            nQInput.setText("");
                            nAInput.setText("");
                            return true;
                        default: return false;
                    }
                }
            });
        }

        private Toolbar.OnMenuItemClickListener defaultClkListener(){
            return new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()){
                        case R.id.save:
                            String a = nAInput.getText().toString();
                            qItemList.get(getAdapterPosition()).as.add(a);
                            nALayout.addView(setMultiAView(a));
                            nAInput.setText("");
                            return true;
                        case R.id.cancel:
                            nAInput.setText("");
                            return true;
                        default: return false;
                    }
                }
            };
        }

        /**set listener for save button when editing*/
        private Toolbar.OnMenuItemClickListener eClkListener(final View aViewToEdit, final int idx){
            return new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.save:
                            AppCompatCheckBox aView = aViewToEdit.findViewById(R.id.a_view);
                            String a = nAInput.getText().toString();
                            aView.setText(a);
                            if(qItemList.get(getAdapterPosition()).as.get(idx) != null)
                                qItemList.get(getAdapterPosition()).as.set(idx, a);
                            nAInput.setText("");
                            nMultiAView.setOnMenuItemClickListener(defaultClkListener());
                            return true;
                        case R.id.cancel:
                            nAInput.setText("");
                            nMultiAView.setOnMenuItemClickListener(defaultClkListener());
                            return true;
                        default: return false;
                    }
                }
            };
        }

        /**To create a q item from form*/
        private AbbContent.QuestionItem getQItemFromForm(long eqId, EditText qInput, Spinner qType,
                                                         long newQId, List<String> newAs) {
            String newQText = qInput.getText().toString();
            int newQType = qType.getSelectedItemPosition();
            if (TextUtils.isEmpty(newQText)) newQText = "";
            return new AbbContent.QuestionItem(eqId, newQId, newQType, newQText, newAs);
        }

        /**sets every a when is a multi a question*/
        private View setMultiAView(String a){
            final Toolbar multiAView = (Toolbar) li.
                    inflate(R.layout.layout_multi_a_q, (ViewGroup) itemView, false);
            multiAView.inflateMenu(R.menu.item_options_menu);
            multiAView.getMenu().getItem(1).setVisible(false);
            multiAView.setOnMenuItemClickListener
                    (new Toolbar.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int index = nALayout.indexOfChild(multiAView)-1;
                            switch (item.getItemId()){
                                case R.id.edit:
                                    nAInput.setText(qItemList.get
                                            (getAdapterPosition()).as.get(index));
                                    nMultiAView.setOnMenuItemClickListener
                                            (eClkListener(multiAView, index));
                                    return true;
                                case R.id.delete:
                                    qItemList.get(getAdapterPosition()).as.
                                            remove(index);
                                    nALayout.removeView(multiAView);
                                    return true;
                                default: return false;
                            }
                        }
                    });
            AppCompatCheckBox aView = multiAView.findViewById(R.id.a_view);
            aView.setText(a);
            return multiAView;
        }
    }
}
