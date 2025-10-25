package com.example.h_cas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * DrugInteractionsFragment handles drug interaction checking for pharmacists
 */
public class DrugInteractionsFragment extends Fragment {

    private RecyclerView interactionsRecyclerView;
    private TextView emptyStateText;
    private MaterialButton checkInteractionsButton;
    private MaterialButton clearCheckButton;
    private TextInputEditText medicine1Input;
    private TextInputEditText medicine2Input;
    
    private InteractionsAdapter interactionsAdapter;
    private List<DrugInteraction> drugInteractions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drug_interactions, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadSampleInteractions();
        
        return view;
    }

    private void initializeViews(View view) {
        interactionsRecyclerView = view.findViewById(R.id.interactionsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        checkInteractionsButton = view.findViewById(R.id.checkInteractionsButton);
        clearCheckButton = view.findViewById(R.id.clearCheckButton);
        medicine1Input = view.findViewById(R.id.medicine1Input);
        medicine2Input = view.findViewById(R.id.medicine2Input);
    }

    private void setupRecyclerView() {
        drugInteractions = new ArrayList<>();
        interactionsAdapter = new InteractionsAdapter(drugInteractions);
        interactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        interactionsRecyclerView.setAdapter(interactionsAdapter);
    }

    private void setupClickListeners() {
        checkInteractionsButton.setOnClickListener(v -> checkDrugInteractions());
        clearCheckButton.setOnClickListener(v -> clearInteractionCheck());
    }

    private void loadSampleInteractions() {
        drugInteractions.clear();
        
        // Add sample drug interactions
        drugInteractions.add(new DrugInteraction(
            "Warfarin + Aspirin",
            "Major",
            "Increased risk of bleeding",
            "Monitor INR closely, consider dose adjustment",
            "⚠️"
        ));
        
        drugInteractions.add(new DrugInteraction(
            "Metformin + Alcohol",
            "Moderate",
            "Increased risk of lactic acidosis",
            "Avoid alcohol consumption while taking metformin",
            "🔶"
        ));
        
        drugInteractions.add(new DrugInteraction(
            "ACE Inhibitors + Potassium",
            "Major",
            "Hyperkalemia risk",
            "Monitor potassium levels, avoid potassium supplements",
            "⚠️"
        ));
        
        drugInteractions.add(new DrugInteraction(
            "Digoxin + Amiodarone",
            "Major",
            "Increased digoxin levels",
            "Reduce digoxin dose by 50%, monitor levels",
            "⚠️"
        ));
        
        drugInteractions.add(new DrugInteraction(
            "Statins + Grapefruit",
            "Moderate",
            "Increased statin levels",
            "Avoid grapefruit juice, monitor for muscle pain",
            "🔶"
        ));
        
        interactionsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (drugInteractions.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            interactionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            interactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void checkDrugInteractions() {
        String medicine1 = medicine1Input.getText().toString().trim();
        String medicine2 = medicine2Input.getText().toString().trim();
        
        if (medicine1.isEmpty() || medicine2.isEmpty()) {
            Toast.makeText(getContext(), "Please enter both medicines to check interactions", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Simulate checking for interactions
        checkForSpecificInteractions(medicine1, medicine2);
    }

    private void checkForSpecificInteractions(String med1, String med2) {
        String combination = med1 + " + " + med2;
        
        // Check for known interactions
        if ((med1.toLowerCase().contains("warfarin") && med2.toLowerCase().contains("aspirin")) ||
            (med2.toLowerCase().contains("warfarin") && med1.toLowerCase().contains("aspirin"))) {
            showInteractionResult(combination, "Major", "Increased risk of bleeding", "Monitor INR closely");
        } else if ((med1.toLowerCase().contains("metformin") && med2.toLowerCase().contains("alcohol")) ||
                   (med2.toLowerCase().contains("metformin") && med1.toLowerCase().contains("alcohol"))) {
            showInteractionResult(combination, "Moderate", "Increased risk of lactic acidosis", "Avoid alcohol consumption");
        } else if ((med1.toLowerCase().contains("ace") && med2.toLowerCase().contains("potassium")) ||
                   (med2.toLowerCase().contains("ace") && med1.toLowerCase().contains("potassium"))) {
            showInteractionResult(combination, "Major", "Hyperkalemia risk", "Monitor potassium levels");
        } else {
            showInteractionResult(combination, "None Found", "No known interactions detected", "Continue with normal monitoring");
        }
    }

    private void showInteractionResult(String combination, String severity, String effect, String recommendation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Drug Interaction Check");
        
        String message = "Medicines: " + combination + "\n\n" +
                        "Severity: " + severity + "\n\n" +
                        "Effect: " + effect + "\n\n" +
                        "Recommendation: " + recommendation;
        
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void clearInteractionCheck() {
        medicine1Input.setText("");
        medicine2Input.setText("");
        Toast.makeText(getContext(), "Interaction check cleared", Toast.LENGTH_SHORT).show();
    }

    // Drug Interaction class
    private static class DrugInteraction {
        private String combination;
        private String severity;
        private String effect;
        private String recommendation;
        private String icon;

        public DrugInteraction(String combination, String severity, String effect, String recommendation, String icon) {
            this.combination = combination;
            this.severity = severity;
            this.effect = effect;
            this.recommendation = recommendation;
            this.icon = icon;
        }

        public String getCombination() { return combination; }
        public String getSeverity() { return severity; }
        public String getEffect() { return effect; }
        public String getRecommendation() { return recommendation; }
        public String getIcon() { return icon; }
    }

    // RecyclerView Adapter for interactions
    private class InteractionsAdapter extends RecyclerView.Adapter<InteractionsAdapter.InteractionViewHolder> {
        private List<DrugInteraction> interactions;

        public InteractionsAdapter(List<DrugInteraction> interactions) {
            this.interactions = interactions;
        }

        @NonNull
        @Override
        public InteractionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drug_interaction, parent, false);
            return new InteractionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InteractionViewHolder holder, int position) {
            DrugInteraction interaction = interactions.get(position);
            holder.bind(interaction);
        }

        @Override
        public int getItemCount() {
            return interactions.size();
        }

        class InteractionViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView interactionIconText;
            private TextView combinationText;
            private TextView severityText;
            private TextView effectText;
            private TextView recommendationText;

            public InteractionViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.interactionCardView);
                interactionIconText = itemView.findViewById(R.id.interactionIconText);
                combinationText = itemView.findViewById(R.id.combinationText);
                severityText = itemView.findViewById(R.id.severityText);
                effectText = itemView.findViewById(R.id.effectText);
                recommendationText = itemView.findViewById(R.id.recommendationText);
            }

            public void bind(DrugInteraction interaction) {
                interactionIconText.setText(interaction.getIcon());
                combinationText.setText(interaction.getCombination());
                severityText.setText(interaction.getSeverity());
                effectText.setText(interaction.getEffect());
                recommendationText.setText(interaction.getRecommendation());
                
                // Color code severity
                if ("Major".equals(interaction.getSeverity())) {
                    severityText.setTextColor(getContext().getColor(R.color.error_red));
                } else if ("Moderate".equals(interaction.getSeverity())) {
                    severityText.setTextColor(getContext().getColor(R.color.warning_orange));
                } else {
                    severityText.setTextColor(getContext().getColor(R.color.success_green));
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Enable back navigation with safer implementation
        if (getActivity() != null) {
            getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Navigate back to dashboard
                    if (getActivity() instanceof PharmacistDashboardActivity) {
                        ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                        ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                    }
                }
            });
        }
    }
}



