package com.smartbill.app;

import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.smartbill.app.database.DBHelper;
import com.smartbill.app.model.DailySales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesDashboardActivity extends AppCompatActivity {

    private DBHelper dbHelper;

    // Existing dashboard values
    private TextView txtTodaySales;
    private TextView txtTodayBills;
    private TextView txtCashSales;
    private TextView txtUpiSales;

    private TextView txtWeeklySales;
    private TextView txtMonthlySales;

    private TextView txtPendingPayments;
    private TextView txtTotalProducts;

    private LinearLayout dailySalesContainer;

    // Swipeable dashboard
    private ViewPager2 viewPager;
    private TextView[] pageIndicators;

    private final int NAVY = 0xFF173F73;
    private final int BACKGROUND = 0xFFF4F6FA;
    private final int TEXT_SECONDARY = 0xFF667085;
    private final int WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        createDashboardLayout();

        loadDashboard();
    }

    // --------------------------------------------------
    // MAIN DASHBOARD LAYOUT
    // --------------------------------------------------

    private void createDashboardLayout() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        // Professional header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(22, 22, 22, 20);
        header.setBackgroundColor(NAVY);

        TextView brand = new TextView(this);
        brand.setText("SMARTBILL  /  BUSINESS ANALYTICS");
        brand.setTextSize(11);
        brand.setTextColor(0xFFBFD8F5);
        brand.setLetterSpacing(0.08f);
        brand.setTypeface(null, Typeface.BOLD);

        header.addView(brand);

        TextView title = new TextView(this);
        title.setText("Sales Dashboard");
        title.setTextSize(27);
        title.setTextColor(WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 8, 0, 0);

        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Track your shop's performance");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFFDCE8F5);
        subtitle.setPadding(0, 5, 0, 0);

        header.addView(subtitle);

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ViewPager2
        viewPager = new ViewPager2(this);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setOffscreenPageLimit(2);

        // Create the three dashboard pages
        View[] pages = new View[]{
                createTodayPage(),
                createAnalyticsPage(),
                createBusinessPage()
        };

        viewPager.setAdapter(new DashboardPagerAdapter(pages));

        LinearLayout.LayoutParams pagerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        root.addView(viewPager, pagerParams);

        // Bottom page indicators
        LinearLayout indicatorContainer = new LinearLayout(this);
        indicatorContainer.setGravity(Gravity.CENTER);
        indicatorContainer.setOrientation(LinearLayout.HORIZONTAL);
        indicatorContainer.setPadding(0, 12, 0, 16);
        indicatorContainer.setBackgroundColor(WHITE);

        pageIndicators = new TextView[3];

        String[] labels = {
                "TODAY",
                "ANALYTICS",
                "BUSINESS"
        };

        for (int i = 0; i < 3; i++) {

            final int page = i;

            TextView indicator = new TextView(this);
            indicator.setText(labels[i]);
            indicator.setTextSize(11);
            indicator.setGravity(Gravity.CENTER);
            indicator.setTypeface(null, Typeface.BOLD);
            indicator.setPadding(12, 10, 12, 10);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1
                    );

            params.setMargins(4, 0, 4, 0);

            indicatorContainer.addView(indicator, params);

            pageIndicators[i] = indicator;

            indicator.setOnClickListener(v ->
                    viewPager.setCurrentItem(page, true)
            );
        }

        root.addView(
                indicatorContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        updatePageIndicators(position);
                    }
                }
        );

        updatePageIndicators(0);

        setContentView(root);
    }

    // --------------------------------------------------
    // PAGE 1: TODAY'S SUMMARY
    // --------------------------------------------------

    private View createTodayPage() {

        LinearLayout page = createPageContainer();

        addPageTitle(
                page,
                "Today's Summary",
                "Your shop's performance today"
        );

        txtTodaySales = addDashboardCard(
                page,
                "TODAY'S PAID SALES",
                "₹0.00",
                true
        );

        txtTodaySales.setTextSize(30);

        txtTodayBills = addDashboardCard(
                page,
                "TODAY'S PAID BILLS",
                "0",
                false
        );

        txtCashSales = addDashboardCard(
                page,
                "CASH SALES",
                "₹0.00",
                false
        );

        txtCashSales.setTextColor(0xFF16803C);

        txtUpiSales = addDashboardCard(
                page,
                "UPI SALES",
                "₹0.00",
                false
        );

        txtUpiSales.setTextColor(0xFF1976D2);

        return createScrollPage(page);
    }

    // --------------------------------------------------
    // PAGE 2: WEEKLY AND MONTHLY ANALYTICS
    // --------------------------------------------------

    private View createAnalyticsPage() {

        LinearLayout page = createPageContainer();

        addPageTitle(
                page,
                "Sales Analytics",
                "Review your recent sales performance"
        );

        txtWeeklySales = addDashboardCard(
                page,
                "PAID SALES - LAST 7 DAYS",
                "₹0.00",
                false
        );

        txtMonthlySales = addDashboardCard(
                page,
                "PAID SALES - THIS MONTH",
                "₹0.00",
                false
        );

        addSectionHeading(
                page,
                "Daily Sales - Last 7 Days"
        );

        dailySalesContainer = new LinearLayout(this);
        dailySalesContainer.setOrientation(LinearLayout.VERTICAL);

        page.addView(
                dailySalesContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        return createScrollPage(page);
    }

    // --------------------------------------------------
    // PAGE 3: PAYMENTS AND INVENTORY
    // --------------------------------------------------

    private View createBusinessPage() {

        LinearLayout page = createPageContainer();

        addPageTitle(
                page,
                "Business Overview",
                "Monitor payments and inventory"
        );

        txtPendingPayments = addDashboardCard(
                page,
                "PENDING UPI PAYMENTS",
                "0",
                false
        );

        txtPendingPayments.setTextColor(0xFFF59E0B);

        txtTotalProducts = addDashboardCard(
                page,
                "TOTAL PRODUCTS",
                "0",
                false
        );

        txtTotalProducts.setTextColor(0xFF0F9D8A);

        // Supporting information
        TextView information = new TextView(this);
        information.setText(
                "Keep track of outstanding payments " +
                        "and the products available in your shop."
        );
        information.setTextSize(14);
        information.setTextColor(TEXT_SECONDARY);
        information.setPadding(4, 8, 4, 0);

        page.addView(information);

        return createScrollPage(page);
    }

    // --------------------------------------------------
    // COMMON PAGE DESIGN
    // --------------------------------------------------

    private LinearLayout createPageContainer() {

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(18, 18, 18, 24);
        page.setBackgroundColor(BACKGROUND);

        return page;
    }

    private View createScrollPage(LinearLayout page) {

        ScrollView scrollView = new ScrollView(this);

        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        scrollView.addView(
                page,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        return scrollView;
    }

    private void addPageTitle(
            LinearLayout parent,
            String titleText,
            String subtitleText) {

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(23);
        title.setTextColor(NAVY);
        title.setTypeface(null, Typeface.BOLD);

        parent.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(subtitleText);
        subtitle.setTextSize(13);
        subtitle.setTextColor(TEXT_SECONDARY);
        subtitle.setPadding(0, 5, 0, 20);

        parent.addView(subtitle);
    }

    private void addSectionHeading(
            LinearLayout parent,
            String heading) {

        TextView headingView = new TextView(this);

        headingView.setText(heading);
        headingView.setTextSize(18);
        headingView.setTextColor(NAVY);
        headingView.setTypeface(null, Typeface.BOLD);
        headingView.setPadding(0, 12, 0, 14);

        parent.addView(headingView);
    }

    // --------------------------------------------------
    // PROFESSIONAL STATISTIC CARD
    // --------------------------------------------------

    private TextView addDashboardCard(
            LinearLayout parent,
            String heading,
            String initialValue,
            boolean highlighted) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);

        if (highlighted) {
            card.setBackgroundResource(
                    R.drawable.bg_dashboard_revenue
            );
        } else {
            card.setBackgroundResource(
                    R.drawable.bg_dashboard_premium_card
            );
        }

        card.setElevation(2);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 14);

        card.setLayoutParams(cardParams);

        TextView headingView = new TextView(this);
        headingView.setText(heading);
        headingView.setTextSize(12);
        headingView.setTypeface(null, Typeface.BOLD);
        headingView.setLetterSpacing(0.04f);

        headingView.setTextColor(
                highlighted ? 0xFFDCE8F5 : TEXT_SECONDARY
        );

        TextView valueView = new TextView(this);
        valueView.setText(initialValue);
        valueView.setTextSize(27);
        valueView.setTypeface(null, Typeface.BOLD);
        valueView.setPadding(0, 10, 0, 0);

        valueView.setTextColor(
                highlighted ? WHITE : NAVY
        );

        card.addView(headingView);
        card.addView(valueView);

        parent.addView(card);

        return valueView;
    }

    // --------------------------------------------------
    // UPDATE PAGE INDICATORS
    // --------------------------------------------------

    private void updatePageIndicators(int selectedPage) {

        if (pageIndicators == null) {
            return;
        }

        for (int i = 0; i < pageIndicators.length; i++) {

            TextView indicator = pageIndicators[i];

            if (i == selectedPage) {

                indicator.setBackgroundResource(
                        R.drawable.bg_dashboard_revenue
                );

                indicator.setTextColor(WHITE);

            } else {

                indicator.setBackgroundResource(
                        R.drawable.bg_dashboard_premium_card
                );

                indicator.setTextColor(TEXT_SECONDARY);
            }
        }
    }

    // --------------------------------------------------
    // LOAD DASHBOARD DATA
    // Existing database calculations are preserved.
    // --------------------------------------------------

    private void loadDashboard() {

        BigDecimal todaySales = dbHelper.getTodaySales();
        BigDecimal cashSales = dbHelper.getTodayCashSales();
        BigDecimal upiSales = dbHelper.getTodayUpiSales();

        BigDecimal weeklySales = dbHelper.getWeeklySales();
        BigDecimal monthlySales = dbHelper.getMonthlySales();

        txtTodaySales.setText(formatRupees(todaySales));

        txtTodayBills.setText(
                String.valueOf(dbHelper.getTodayBillCount())
        );

        txtCashSales.setText(formatRupees(cashSales));

        txtUpiSales.setText(formatRupees(upiSales));

        txtWeeklySales.setText(formatRupees(weeklySales));

        txtMonthlySales.setText(formatRupees(monthlySales));

        txtPendingPayments.setText(
                String.valueOf(dbHelper.getPendingUpiBillCount())
        );

        txtTotalProducts.setText(
                String.valueOf(dbHelper.getTotalProductCount())
        );

        loadDailySales();
    }

    // --------------------------------------------------
    // DAILY SALES - LAST SEVEN DAYS
    // --------------------------------------------------

    private void loadDailySales() {

        dailySalesContainer.removeAllViews();

        List<DailySales> dailySalesList =
                dbHelper.getLastSevenDaysSales();

        SimpleDateFormat inputFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        SimpleDateFormat outputFormat =
                new SimpleDateFormat(
                        "EEE, dd MMM",
                        Locale.getDefault()
                );

        for (DailySales dailySales : dailySalesList) {

            String displayDate = dailySales.getDate();

            try {

                Date date = inputFormat.parse(
                        dailySales.getDate()
                );

                if (date != null) {
                    displayDate = outputFormat.format(date);
                }

            } catch (ParseException e) {

                android.util.Log.e(
                        "SmartBill",
                        "Could not format sales date",
                        e
                );
            }

            LinearLayout row = new LinearLayout(this);

            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 16, 16, 16);

            row.setBackgroundResource(
                    R.drawable.bg_dashboard_premium_card
            );

            row.setElevation(1);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            rowParams.setMargins(0, 0, 0, 8);

            row.setLayoutParams(rowParams);

            TextView dateView = new TextView(this);

            dateView.setText(displayDate);
            dateView.setTextSize(14);
            dateView.setTextColor(0xFF424242);

            LinearLayout.LayoutParams dateParams =
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1
                    );

            dateView.setLayoutParams(dateParams);

            TextView amountView = new TextView(this);

            amountView.setText(
                    formatRupees(dailySales.getTotalSales())
            );

            amountView.setTextSize(15);
            amountView.setTypeface(null, Typeface.BOLD);
            amountView.setTextColor(0xFF1565C0);

            row.addView(dateView);
            row.addView(amountView);

            dailySalesContainer.addView(row);
        }
    }

    // --------------------------------------------------
    // CURRENCY FORMATTING
    // --------------------------------------------------

    private String formatRupees(BigDecimal amount) {

        return "₹" + amount.setScale(
                2,
                RoundingMode.HALF_UP
        ).toPlainString();
    }

    // --------------------------------------------------
    // REFRESH WHEN ACTIVITY RESUMES
    // --------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        if (dbHelper != null && txtTodaySales != null) {
            loadDashboard();
        }
    }

    // --------------------------------------------------
    // CLOSE DATABASE
    // --------------------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // --------------------------------------------------
    // VIEWPAGER ADAPTER
    // --------------------------------------------------

    private static class DashboardPagerAdapter
            extends RecyclerView.Adapter<DashboardPagerAdapter.PageViewHolder> {

        private final View[] pages;

        DashboardPagerAdapter(View[] pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            FrameLayout container = new FrameLayout(
                    parent.getContext()
            );

            container.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );

            return new PageViewHolder(container);
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

            holder.container.addView(
                    page,
                    new FrameLayout.LayoutParams(
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

            final FrameLayout container;

            PageViewHolder(@NonNull FrameLayout container) {
                super(container);
                this.container = container;
            }
        }
    }
}