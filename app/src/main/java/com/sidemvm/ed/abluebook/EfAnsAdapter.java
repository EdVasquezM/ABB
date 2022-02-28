package com.sidemvm.ed.abluebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

/** Created by super on 3/03/2018.*/

public class EfAnsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //Create fields
    private EfAnsFragment ansFrag;
    private List<AbbContent.QuestionItem> qItemList;
    private AbbContent.AnswerItem aToShow;

    //constructor
    EfAnsAdapter(EfAnsFragment ansFrag, List<AbbContent.QuestionItem> qItemList,
                 AbbContent.AnswerItem aToShow){
        this.qItemList = qItemList;
        this.ansFrag = ansFrag;
        this.aToShow = aToShow;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new QViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.q_content_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AbbContent.QuestionItem thisQuestion = qItemList.get(position);
        QViewHolder qViewHolder = (QViewHolder) holder;
        final long qId = thisQuestion.qId;
        qViewHolder.qCardView.setTag(thisQuestion);
        qViewHolder.qView.setText(thisQuestion.q);
        Bitmap bmp = BitmapFactory.decodeFile(AbbContent.getAbbFilesPath()+"/"+
                String.valueOf(position)+String.valueOf(thisQuestion.efId)+".jpeg");
        if (bmp != null) qViewHolder.qImaView.setImageBitmap(bmp);
        //set as if question is multi a, and set answers if needed
        String a = aToShow.as.get(qId);
        qViewHolder.aLayout.removeAllViews();
        if (thisQuestion.qType == 1){
            for (String everyA: thisQuestion.as){
                CheckBox everyAView = new CheckBox(ansFrag.getContext());
                everyAView.setButtonDrawable(R.drawable.checkbox_selector);
                everyAView.setText(everyA);
                everyAView.setTextColor(ansFrag.getResources().getColor
                        (android.R.color.secondary_text_light));
                //check it if is viewing an answer and it haves that a
                if(a != null && a.contains(everyA)) everyAView.setChecked(true);
                // add listener to save answers
                everyAView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        String thisA = compoundButton.getText().toString();
                        String a = aToShow.as.get(qId);
                        if (isChecked) {
                            if (a == null) aToShow.as.put(qId, thisA);
                            else aToShow.as.put(qId, a + "; " + thisA);
                        } else {
                            if (a != null) aToShow.as.put(qId, a.replace("; "+thisA, ""));
                        }
                    }
                });
                qViewHolder.aLayout.addView(everyAView);
            }
        } else {
            //charge text on it if is viewing an answer
            EditText ansInput = qViewHolder.qOpenInput.findViewById(R.id.q_input_open);
            ansInput.setText(a);
            ansInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    aToShow.as.put(qId, editable.toString());
                }
            });
            qViewHolder.aLayout.addView(qViewHolder.qOpenInput);
        }
    }

    AbbContent.AnswerItem retrieveData(){return aToShow;}

    @Override
    public long getItemId (int position) {return qItemList.get(position).qId;}

    @Override
    public int getItemCount() {return qItemList.size();}

    @Override
    public int getItemViewType(int position) {return  qItemList.get(position).qType;}

    //Class to visualize every question item
    public class QViewHolder extends RecyclerView.ViewHolder {
        //Create views
        final CardView qCardView;
        final AppCompatTextView qView;
        final AppCompatImageView qImaView;
        final LinearLayout aLayout;
        final TextInputLayout qOpenInput;

        QViewHolder(View itemView) {
            super(itemView);
            //Initialize views
            qCardView = itemView.findViewById(R.id.q_card_view);
            qView = itemView.findViewById(R.id.q_text_view);
            qImaView = itemView.findViewById(R.id.q_item_image_view);
            qView.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            aLayout = itemView.findViewById(R.id.a_layout);
            //inflate layouts to dynamically include it
            LayoutInflater li = LayoutInflater.from(ansFrag.getContext());
            qOpenInput = (TextInputLayout) li.inflate(R.layout.layout_essai_q, aLayout, false);
            //create layouts to include it dynamically
        }
    }
}
