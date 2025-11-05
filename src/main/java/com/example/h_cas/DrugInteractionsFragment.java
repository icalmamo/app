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
    private View emptyStateText; // Changed to View since it's a LinearLayout
    private MaterialButton checkInteractionsButton;
    private MaterialButton clearCheckButton;
    private TextInputEditText medicine1Input;
    private TextInputEditText medicine2Input;
    
    private InteractionsAdapter interactionsAdapter;
    private List<DrugInteraction> drugInteractions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_drug_interactions, container, false);
            
            if (view == null) {
                return null;
            }
            
            initializeViews(view);
            setupRecyclerView();
            setupClickListeners();
            loadSampleInteractions();
            
            return view;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initializeViews(View view) {
        if (view == null) {
            return;
        }
        
        interactionsRecyclerView = view.findViewById(R.id.interactionsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        checkInteractionsButton = view.findViewById(R.id.checkInteractionsButton);
        clearCheckButton = view.findViewById(R.id.clearCheckButton);
        medicine1Input = view.findViewById(R.id.medicine1Input);
        medicine2Input = view.findViewById(R.id.medicine2Input);
        
        drugInteractions = new ArrayList<>();
    }

    private void setupRecyclerView() {
        if (getContext() == null || interactionsRecyclerView == null) {
            return;
        }
        
        try {
            if (drugInteractions == null) {
                drugInteractions = new ArrayList<>();
            }
            
            interactionsAdapter = new InteractionsAdapter(drugInteractions);
            interactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            interactionsRecyclerView.setAdapter(interactionsAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up interactions list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (checkInteractionsButton != null) {
            checkInteractionsButton.setOnClickListener(v -> checkDrugInteractions());
        }
        if (clearCheckButton != null) {
            clearCheckButton.setOnClickListener(v -> clearInteractionCheck());
        }
    }

    private void loadSampleInteractions() {
        try {
            if (drugInteractions == null) {
                drugInteractions = new ArrayList<>();
            }
            
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
            
            if (interactionsAdapter != null) {
                interactionsAdapter.notifyDataSetChanged();
            }
            updateEmptyState();
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading interactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateEmptyState() {
        if (emptyStateText == null || interactionsRecyclerView == null) {
            return;
        }
        
        if (drugInteractions == null || drugInteractions.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            interactionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            interactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void checkDrugInteractions() {
        if (getContext() == null || medicine1Input == null || medicine2Input == null) {
            return;
        }
        
        String medicine1 = medicine1Input.getText() != null ? medicine1Input.getText().toString().trim() : "";
        String medicine2 = medicine2Input.getText() != null ? medicine2Input.getText().toString().trim() : "";
        
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
        if (getContext() == null) {
            return;
        }
        
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
        if (getContext() == null) {
            return;
        }
        
        if (medicine1Input != null) {
            medicine1Input.setText("");
        }
        if (medicine2Input != null) {
            medicine2Input.setText("");
        }
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
            this.interactions = interactions != null ? interactions : new ArrayList<>();
        }

        @NonNull
        @Override
        public InteractionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                if (parent == null) {
                    throw new IllegalArgumentException("Parent ViewGroup cannot be null");
                }
                
                android.content.Context context = parent.getContext();
                if (context == null) {
                    context = getContext();
                }
                
                if (context == null) {
                    throw new IllegalStateException("Cannot create view holder: no context available");
                }
                
                View view = LayoutInflater.from(context).inflate(R.layout.item_drug_interaction, parent, false);
                if (view == null) {
                    // Fallback to empty view if inflation fails
                    View emptyView = new View(context);
                    return new InteractionViewHolder(emptyView);
                }
                return new InteractionViewHolder(view);
            } catch (Exception e) {
                e.printStackTrace();
                // Return a basic view holder with empty view if layout inflation fails
                try {
                    android.content.Context context = null;
                    if (parent != null) {
                        context = parent.getContext();
                    }
                    if (context == null) {
                        context = getContext();
                    }
                    if (context != null) {
                        View emptyView = new View(context);
                        return new InteractionViewHolder(emptyView);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // Absolute last resort - this should never happen in normal operation
                // but we need to return something
                if (parent != null && parent.getContext() != null) {
                    return new InteractionViewHolder(new View(parent.getContext()));
                }
                // This should never execute, but provides a fallback
                throw new IllegalStateException("Cannot create InteractionViewHolder: no valid context");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull InteractionViewHolder holder, int position) {
            if (interactions == null || position < 0) {
                return;
            }
            
            try {
                if (position >= interactions.size()) {
                    return;
                }
                
                DrugInteraction interaction = interactions.get(position);
                if (holder != null) {
                    holder.bind(interaction);
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            try {
                return interactions != null ? interactions.size() : 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
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
                try {
                    if (itemView != null) {
                        cardView = itemView.findViewById(R.id.interactionCardView);
                        interactionIconText = itemView.findViewById(R.id.interactionIconText);
                        combinationText = itemView.findViewById(R.id.combinationText);
                        severityText = itemView.findViewById(R.id.severityText);
                        effectText = itemView.findViewById(R.id.effectText);
                        recommendationText = itemView.findViewById(R.id.recommendationText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Views will remain null, bind() method will handle it
                }
            }

            public void bind(DrugInteraction interaction) {
                if (interaction == null || getContext() == null) {
                    return;
                }
                
                // Safely set text with null checks
                if (interactionIconText != null) {
                    interactionIconText.setText(interaction.getIcon() != null ? interaction.getIcon() : "");
                }
                if (combinationText != null) {
                    combinationText.setText(interaction.getCombination() != null ? interaction.getCombination() : "");
                }
                if (severityText != null) {
                    severityText.setText(interaction.getSeverity() != null ? interaction.getSeverity() : "");
                }
                if (effectText != null) {
                    effectText.setText(interaction.getEffect() != null ? interaction.getEffect() : "");
                }
                if (recommendationText != null) {
                    recommendationText.setText(interaction.getRecommendation() != null ? interaction.getRecommendation() : "");
                }
                
                // Color code severity
                try {
                    if (severityText != null) {
                        String severity = interaction.getSeverity();
                        if ("Major".equals(severity)) {
                            severityText.setTextColor(getContext().getColor(R.color.error_red));
                        } else if ("Moderate".equals(severity)) {
                            severityText.setTextColor(getContext().getColor(R.color.warning_orange));
                        } else {
                            severityText.setTextColor(getContext().getColor(R.color.success_green));
                        }
                    }
                } catch (Exception e) {
                    // Fallback to default color if resource access fails
                    e.printStackTrace();
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
        
        try {
            // Enable back navigation with safer implementation
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        try {
                            // Navigate back to dashboard
                            if (getActivity() instanceof PharmacistDashboardActivity) {
                                ((PharmacistDashboardActivity) getActivity()).loadFragment(new PharmacistDashboardFragment());
                                if (((PharmacistDashboardActivity) getActivity()).getSupportActionBar() != null) {
                                    ((PharmacistDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pharmacist Dashboard");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



