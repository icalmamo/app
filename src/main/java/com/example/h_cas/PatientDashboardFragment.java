package com.example.h_cas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.h_cas.database.HCasDatabaseHelper;
import com.example.h_cas.models.Patient;

import java.util.List;

/**
 * PatientDashboardFragment shows a list of registered patients for nurses.
 */
public class PatientDashboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyView;
    private HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.patientRecyclerView);
        emptyView = view.findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        databaseHelper = new HCasDatabaseHelper(view.getContext());
        List<Patient> patients = databaseHelper.getAllPatients();

        if (patients.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            recyclerView.setAdapter(new PatientAdapter(patients));
        }
    }

    private static class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
        private final List<Patient> patients;

        PatientAdapter(List<Patient> patients) {
            this.patients = patients;
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
            return new PatientViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            Patient p = patients.get(position);
            holder.nameText.setText(p.getFirstName() + " " + p.getLastName());
            holder.subText.setText(buildSubText(p));
        }

        private String buildSubText(Patient p) {
            StringBuilder sb = new StringBuilder();
            if (p.getGender() != null && !p.getGender().isEmpty()) sb.append(p.getGender());
            if (p.getDateOfBirth() != null && !p.getDateOfBirth().isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(p.getDateOfBirth());
            }
            if (p.getPhone() != null && !p.getPhone().isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(p.getPhone());
            }
            return sb.toString();
        }

        @Override
        public int getItemCount() {
            return patients.size();
        }

        static class PatientViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView subText;
            PatientViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.patientNameText);
                subText = itemView.findViewById(R.id.patientSubText);
            }
        }
    }
}
























