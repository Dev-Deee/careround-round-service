package com.dayo.careround.roundservice.grpc;

import com.dayo.careround.patientservice.grpc.PatientCensusServiceGrpc;
import com.dayo.careround.patientservice.grpc.PatientExistsRequest;
import com.dayo.careround.patientservice.grpc.PatientExistsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PatientVerificationClient {

    @GrpcClient("patient-service")
    private PatientCensusServiceGrpc.PatientCensusServiceBlockingStub patientStub;

    @Value("${app.grpc.deadline-ms:5000}")
    private long deadlineMs;

    public boolean patientExists(UUID patientId) {
        return lookup(patientId).getExists();
    }

    // exists plus the patient's ward_id (empty when the patient does not exist).
    public PatientExistsResponse lookup(UUID patientId) {
        PatientExistsRequest request = PatientExistsRequest.newBuilder()
                .setPatientId(patientId.toString())
                .build();
        return patientStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).patientExists(request);
    }
}