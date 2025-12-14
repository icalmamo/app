package com.healthcare.cas.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.healthcare.cas.R;
import com.healthcare.cas.models.Notification;

import java.util.List;

/**
 * Adapter for notification RecyclerView
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;
    
    /**
     * Update the notifications list
     */
    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications != null ? newNotifications : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications != null ? notifications : new java.util.ArrayList<>();
        this.listener = listener;
        android.util.Log.d("NotificationAdapter", "Adapter created with " + this.notifications.size() + " notifications, listener: " + (listener != null ? "SET" : "NULL"));
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView senderText;
        private TextView messageText;
        private TextView timeText;
        private View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.notificationSender);
            messageText = itemView.findViewById(R.id.notificationMessage);
            timeText = itemView.findViewById(R.id.notificationTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);

            // Make the entire card clickable
            itemView.setClickable(true);
            itemView.setFocusable(true);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                android.util.Log.d("NotificationAdapter", "Item clicked at position: " + position + ", listener: " + (listener != null ? "SET" : "NULL"));
                if (position != RecyclerView.NO_POSITION && position < notifications.size() && listener != null) {
                    android.util.Log.d("NotificationAdapter", "Calling listener.onNotificationClick for: " + notifications.get(position).getId());
                    listener.onNotificationClick(notifications.get(position));
                } else {
                    android.util.Log.w("NotificationAdapter", "Click not handled - position: " + position + ", size: " + notifications.size() + ", listener: " + (listener != null ? "SET" : "NULL"));
                }
            });
            
            // Also make the inner LinearLayout clickable as backup
            View innerLayout = itemView.findViewById(R.id.notificationContentLayout);
            if (innerLayout != null) {
                innerLayout.setClickable(true);
                innerLayout.setFocusable(true);
                innerLayout.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    android.util.Log.d("NotificationAdapter", "Inner layout clicked at position: " + position);
                    if (position != RecyclerView.NO_POSITION && position < notifications.size() && listener != null) {
                        android.util.Log.d("NotificationAdapter", "Calling listener.onNotificationClick from inner layout");
                        listener.onNotificationClick(notifications.get(position));
                    }
                });
            }
            
            // Make the entire card clickable too
            if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                itemView.setClickable(true);
                itemView.setFocusable(true);
            }
        }

        public void bind(Notification notification) {
            senderText.setText(notification.getSender());
            messageText.setText(notification.getMessage());
            timeText.setText(notification.getTimestamp());

            // Show/hide unread indicator
            boolean isRead = notification.isRead();
            android.util.Log.d("NotificationAdapter", "Binding notification - ID: " + notification.getId() + ", isRead: " + isRead);
            
            if (isRead) {
                unreadIndicator.setVisibility(View.GONE);
                android.util.Log.d("NotificationAdapter", "Hiding unread indicator for notification: " + notification.getId());
            } else {
                unreadIndicator.setVisibility(View.VISIBLE);
                android.util.Log.d("NotificationAdapter", "Showing unread indicator for notification: " + notification.getId());
            }
        }
    }
}





