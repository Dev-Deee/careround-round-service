package com.dayo.careround.roundservice.grpc;

import com.dayo.careround.caretaskservice.grpc.CareTaskQueryServiceGrpc;
import com.dayo.careround.caretaskservice.grpc.PendingTaskIdsRequest;
import com.dayo.careround.caretaskservice.grpc.PendingTaskIdsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CareTaskQueryClient {

    @GrpcClient("care-task-service")
    private CareTaskQueryServiceGrpc.CareTaskQueryServiceBlockingStub careTaskStub;

    public List<String> getPendingTaskIds(UUID patientId) {
        PendingTaskIdsRequest request = PendingTaskIdsRequest.newBuilder()
                .setPatientId(patientId.toString())
                .build();
        return careTaskStub.getPendingTaskIds(request).getTaskIdsList();
    }
}