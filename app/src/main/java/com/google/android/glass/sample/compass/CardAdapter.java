package com.google.android.glass.sample.compass;


/**
 * Created by Derek on 2/5/2015.
 */

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.List;

/**
 * Adapter class that handles list of cards.
 */
public class CardAdapter extends CardScrollAdapter {

    final List<CardBuilder> mCards;

    public CardAdapter(List<CardBuilder> cards) {
        mCards = cards;
    }

    @Override
    public int getCount() {
        return mCards.size();
    }

    @Override
    public Object getItem(int position) {
        return mCards.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return mCards.get(position).getView(convertView, parent);
    }

    @Override
    public int getViewTypeCount() {
        return CardBuilder.getViewTypeCount();
    }

    @Override
    public int getItemViewType(int position){
        return mCards.get(position).getItemViewType();
    }

    @Override
    public int getPosition(Object item) {
        for (int i = 0; i < mCards.size(); i++) {
            if (getItem(i).equals(item)) {
                return i;
            }
        }
        return AdapterView.INVALID_POSITION;
    }
}
