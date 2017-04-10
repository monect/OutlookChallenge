package com.monect.ui;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;


public abstract class FloatingHeaderRecyclerViewAdapter<H extends RecyclerView.ViewHolder, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    /**
     * Returns the header id for the item at the given position.
     *
     * @param position the item position
     * @return the header id
     */
    protected abstract long getHeaderId(int position);

    /**
     * Creates a new header ViewHolder.
     *
     * @param parent the header's view parent
     * @return a view holder for the created view
     */
    protected abstract H onCreateHeaderViewHolder(ViewGroup parent);

    /**
     * Updates the header view to reflect the header data for the given position
     *
     * @param viewHolder the header view holder
     * @param position   the header's item position
     */
    protected abstract void onBindHeaderViewHolder(H viewHolder, int position);
}
