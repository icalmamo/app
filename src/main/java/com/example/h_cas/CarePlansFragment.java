package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * CarePlansFragment handles care plans for nurses
 */
public class CarePlansFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        
        TextView titleText = view.findViewById(R.id.titleText);
        titleText.setText("Care Plans - Coming Soon");
        
        return view;
    }
}









