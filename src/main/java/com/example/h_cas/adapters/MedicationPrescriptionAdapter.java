package com.example.h_cas.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.h_cas.R;
import com.example.h_cas.models.MedicationItem;
import com.google.android.material.button.MaterialButton;
import android.widget.AutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adapter for displaying medication items with frequency and duration inputs
 */
public class MedicationPrescriptionAdapter extends RecyclerView.Adapter<MedicationPrescriptionAdapter.MedicationViewHolder> {

    private List<MedicationItem> medications;
    private OnRemoveClickListener onRemoveClickListener;

    public interface OnRemoveClickListener {
        void onRemove(int position);
    }

    public MedicationPrescriptionAdapter(List<MedicationItem> medications) {
        this.medications = medications != null ? medications : new ArrayList<>();
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.onRemoveClickListener = listener;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication_prescription, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        MedicationItem item = medications.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    public void addMedication(MedicationItem medication) {
        medications.add(medication);
        notifyItemInserted(medications.size() - 1);
    }

    public void removeMedication(int position) {
        if (position >= 0 && position < medications.size()) {
            medications.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, medications.size());
        }
    }

    public List<MedicationItem> getMedications() {
        return new ArrayList<>(medications);
    }

    public void updateMedication(int position, MedicationItem medication) {
        if (position >= 0 && position < medications.size()) {
            medications.set(position, medication);
            notifyItemChanged(position);
        }
    }

    class MedicationViewHolder extends RecyclerView.ViewHolder {
        private TextView medicationNameText;
        private AutoCompleteTextView frequencyInput;
        private TextInputEditText durationInput;
        private MaterialButton removeButton;
        private ArrayAdapter<String> frequencyAdapter;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            medicationNameText = itemView.findViewById(R.id.medicationNameText);
            frequencyInput = itemView.findViewById(R.id.frequencyInput);
            durationInput = itemView.findViewById(R.id.durationInput);
            removeButton = itemView.findViewById(R.id.removeButton);
            
            // Setup frequency dropdown (1-7)
            String[] frequencyOptions = {"1", "2", "3", "4", "5", "6", "7"};
            frequencyAdapter = new ArrayAdapter<>(itemView.getContext(), 
                android.R.layout.simple_dropdown_item_1line, frequencyOptions);
            frequencyInput.setAdapter(frequencyAdapter);
            frequencyInput.setThreshold(0); // Show dropdown immediately on click
            frequencyInput.setKeyListener(null); // Prevent keyboard from showing
            frequencyInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    frequencyInput.showDropDown();
                }
            });
        }

        public void bind(MedicationItem item, int position) {
            // Clear previous listeners to prevent crashes
            frequencyInput.setOnItemClickListener(null);
            durationInput.setOnFocusChangeListener(null);
            removeButton.setOnClickListener(null);
            
            medicationNameText.setText(item.getMedicationName());
            
            // Set frequency if it exists
            if (item.getFrequency() != null && !item.getFrequency().isEmpty()) {
                frequencyInput.setText(item.getFrequency(), false);
            } else {
                frequencyInput.setText("", false);
            }
            
            // Set duration if it exists
            if (item.getDuration() != null) {
                durationInput.setText(item.getDuration());
            } else {
                durationInput.setText("");
            }

            // Show dropdown when clicked
            frequencyInput.setOnClickListener(v -> {
                frequencyInput.showDropDown();
            });
            
            // Update item when frequency is selected
            frequencyInput.setOnItemClickListener((parent, view, pos, id) -> {
                int currentPosition = getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < medications.size()) {
                    String frequency = frequencyAdapter.getItem(pos);
                    MedicationItem currentItem = medications.get(currentPosition);
                    if (currentItem != null && frequency != null) {
                        currentItem.setFrequency(frequency);
                        frequencyInput.setText(frequency, false);
                    }
                }
            });

            // Update item when duration changes
            durationInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition < medications.size()) {
                        String duration = durationInput.getText() != null ? 
                            durationInput.getText().toString().trim() : "";
                        MedicationItem currentItem = medications.get(currentPosition);
                        if (currentItem != null) {
                            currentItem.setDuration(duration);
                        }
                    }
                }
            });

            // Remove button click - use getAdapterPosition() for safety
            removeButton.setOnClickListener(v -> {
                int currentPosition = getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && onRemoveClickListener != null) {
                    onRemoveClickListener.onRemove(currentPosition);
                }
            });
        }
    }
}

