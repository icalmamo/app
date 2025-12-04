package com.example.h_cas.utils;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.h_cas.R;
import com.example.h_cas.adapters.NotificationAdapter;
import com.example.h_cas.models.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to manage notification dropdown popup
 */
public class NotificationDropdownHelper {

    private PopupWindow popupWindow;
    private View popupView;
    private RecyclerView recyclerView;
    private TextView notificationCount;
    private TextView notificationTitle;
    private View emptyStateLayout;
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    private Context context;
    private NotificationCountListener countListener;
    
    /**
     * Listener interface for notification count changes
     */
    public interface NotificationCountListener {
        void onNotificationCountChanged(int count);
    }

    public NotificationDropdownHelper(Context context) {
        this.context = context;
        this.notifications = new ArrayList<>();
        initializePopup();
    }

    /**
     * Initialize the popup window
     */
    private void initializePopup() {
        LayoutInflater inflater = LayoutInflater.from(context);
        popupView = inflater.inflate(R.layout.dropdown_notifications, null);

        // Initialize views
        recyclerView = popupView.findViewById(R.id.notificationsRecyclerView);
        notificationCount = popupView.findViewById(R.id.notificationCount);
        notificationTitle = popupView.findViewById(R.id.notificationTitle);
        emptyStateLayout = popupView.findViewById(R.id.emptyStateLayout);

        // Setup RecyclerView
        adapter = new NotificationAdapter(notifications, notification -> {
            // Handle notification click
            onNotificationClick(notification);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        // Setup "See All" button
        popupView.findViewById(R.id.seeAllNotificationsButton).setOnClickListener(v -> {
            onSeeAllClick();
            dismiss();
        });

        // Create PopupWindow
        popupWindow = new PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );

        // Configure popup window
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        // Dismiss when clicking outside
        popupView.setOnClickListener(v -> {
            // Prevent dismissing when clicking inside
        });
    }

    /**
     * Show the dropdown below the notification button, aligned to right side
     */
    public void show(View anchorView) {
        if (popupWindow == null || popupView == null) {
            initializePopup();
        }

        // Update notification count
        updateNotificationCount();

        // Measure popup view to get its width
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
            (int) (320 * context.getResources().getDisplayMetrics().density), 
            View.MeasureSpec.EXACTLY
        );
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        popupView.measure(widthSpec, heightSpec);
        int popupWidth = popupView.getMeasuredWidth();

        // Calculate position (below the notification button, aligned to right side of screen)
        int[] location = new int[2];
        anchorView.getLocationInWindow(location);
        
        // Get screen width and convert dp to pixels
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int marginRight = (int) (16 * context.getResources().getDisplayMetrics().density); // 16dp margin
        
        // Calculate x position: align right edge of popup with right edge of screen (with margin)
        int x = screenWidth - popupWidth - marginRight;
        
        // Ensure popup doesn't go off screen (fallback)
        if (x < marginRight) {
            x = marginRight;
        }
        
        // Y position: below the notification button with small margin
        int marginTop = (int) (8 * context.getResources().getDisplayMetrics().density); // 8dp margin
        int y = location[1] + anchorView.getHeight() + marginTop;

        // Show popup
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }

    /**
     * Dismiss the dropdown
     */
    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    /**
     * Check if dropdown is showing
     */
    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    /**
     * Toggle dropdown visibility
     */
    public void toggle(View anchorView) {
        if (isShowing()) {
            dismiss();
        } else {
            show(anchorView);
        }
    }

    /**
     * Update notification count badge
     */
    private void updateNotificationCount() {
        int unreadCount = getUnreadCount();
        if (unreadCount > 0) {
            notificationCount.setVisibility(View.VISIBLE);
            notificationCount.setText(String.valueOf(unreadCount));
        } else {
            notificationCount.setVisibility(View.GONE);
        }

        // Update empty state
        if (notifications.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Get unread notification count
     */
    private int getUnreadCount() {
        int count = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Set notifications list
     */
    public void setNotifications(List<Notification> notifications) {
        this.notifications.clear();
        if (notifications != null) {
            this.notifications.addAll(notifications);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateNotificationCount();
        
        // Notify listener of count change
        if (countListener != null) {
            countListener.onNotificationCountChanged(notifications != null ? notifications.size() : 0);
        }
    }

    /**
     * Add a notification
     */
    public void addNotification(Notification notification) {
        notifications.add(0, notification); // Add to top
        if (adapter != null) {
            adapter.notifyItemInserted(0);
        }
        updateNotificationCount();
        
        // Notify listener of count change
        if (countListener != null) {
            countListener.onNotificationCountChanged(notifications.size());
        }
    }

    /**
     * Handle notification click
     */
    private void onNotificationClick(Notification notification) {
        // Mark as read
        notification.setRead(true);
        if (adapter != null) {
            adapter.notifyItemChanged(notifications.indexOf(notification));
        }
        updateNotificationCount();
        
        // Notify listener of count change (use total count for badge)
        if (countListener != null) {
            countListener.onNotificationCountChanged(notifications != null ? notifications.size() : 0);
        }

        // TODO: Handle notification action based on type
        // For example: navigate to specific screen, open detail, etc.
    }
    
    /**
     * Set notification count listener
     */
    public void setNotificationCountListener(NotificationCountListener listener) {
        this.countListener = listener;
    }
    
    /**
     * Get total notification count
     */
    public int getTotalNotificationCount() {
        return notifications != null ? notifications.size() : 0;
    }
    
    /**
     * Get unread notification count
     */
    public int getUnreadNotificationCount() {
        return getUnreadCount();
    }

    /**
     * Handle "See All" button click
     */
    private void onSeeAllClick() {
        // TODO: Navigate to full notifications screen
        // For example: startActivity(new Intent(context, NotificationsActivity.class));
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        dismiss();
        popupWindow = null;
        popupView = null;
        recyclerView = null;
        adapter = null;
        notifications = null;
    }
}

