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
 * PatientsFragment shows a list of registered patients.
 */
public class PatientsFragment extends Fragment {

    private RecyclerView recyclerView;
    private HCasDatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.patientsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        databaseHelper = new HCasDatabaseHelper(view.getContext());
        loadPatients();
    }

    private void loadPatients() {
        List<Patient> patients = databaseHelper.getAllPatients();
        recyclerView.setAdapter(new PatientsAdapter(patients));
    }

    private static class PatientsAdapter extends RecyclerView.Adapter<PatientsAdapter.PatientViewHolder> {
        private final List<Patient> patients;

        PatientsAdapter(List<Patient> patients) {
            this.patients = patients;
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_row, parent, false);
            return new PatientViewHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            Patient p = patients.get(position);
            holder.nameText.setText(p.getFirstName() + " " + p.getLastName());
            String sub = (p.getGender() == null ? "" : p.getGender()) + (p.getDateOfBirth() == null ? "" : (" • " + p.getDateOfBirth()));
            holder.subText.setText(sub.trim());
        }

        @Override
        public int getItemCount() { return patients.size(); }

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
