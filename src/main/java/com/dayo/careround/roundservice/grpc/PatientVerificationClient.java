package com.dayo.careround.roundservice.grpc;

import com.dayo.careround.patientservice.grpc.PatientCensusServiceGrpc;
import com.dayo.careround.patientservice.grpc.PatientExistsRequest;
import com.dayo.careround.patientservice.grpc.PatientExistsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PatientVerificationClient {

    @GrpcClient("patient-service")
    private PatientCensusServiceGrpc.PatientCensusServiceBlockingStub patientStub;

    public boolean patientExists(UUID patientId) {
        PatientExistsRequest request = PatientExistsRequest.newBuilder()
                .setPatientId(patientId.toString())
                .build();
        return patientStub.patientExists(request).getExists();
    }
}