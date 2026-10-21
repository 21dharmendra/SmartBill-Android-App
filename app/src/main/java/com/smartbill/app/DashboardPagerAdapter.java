package com.smartbill.app;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class DashboardPagerAdapter
        extends RecyclerView.Adapter<DashboardPagerAdapter.PageViewHolder> {

    private final View[] pages;

    public DashboardPagerAdapter(View[] pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = new View(parent.getContext());

        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PageViewHolder holder,
            int position) {

        View page = pages[position];

        if (page.getParent() instanceof ViewGroup) {
            ((ViewGroup) page.getParent()).removeView(page);
        }

        holder.container.removeAllViews();

        if (page.getParent() != null) {
            ((ViewGroup) page.getParent()).removeView(page);
        }

        holder.container.addView(
                page,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
    }

    @Override
    public int getItemCount() {
        return pages.length;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {

        final ViewGroup container;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);

            container = new android.widget.FrameLayout(
                    itemView.getContext()
            );

            container.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );
        }
    }
}