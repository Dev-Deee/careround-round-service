package com.dayo.careround.roundservice.grpc;

import com.dayo.careround.caretaskservice.grpc.CareTaskQueryServiceGrpc;
import com.dayo.careround.caretaskservice.grpc.PendingTaskIdsRequest;
import com.dayo.careround.roundservice.exception.CareTaskServiceUnavailableException;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CareTaskQueryClient {

    @GrpcClient("care-task-service")
    private CareTaskQueryServiceGrpc.CareTaskQueryServiceBlockingStub careTaskStub;

    @Value("${app.grpc.deadline-ms:5000}")
    private long deadlineMs;

    // Public ids of the patient's PENDING / IN_PROGRESS care tasks. Fails closed: if care-task-service
    // cannot answer, callers must not treat that as "no pending tasks".
    public Set<UUID> getPendingTaskIds(UUID patientId) {
        PendingTaskIdsRequest request = PendingTaskIdsRequest.newBuilder()
                .setPatientId(patientId.toString())
                .build();
        try {
            return careTaskStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .getPendingTaskIds(request)
                    .getTaskIdsList().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
        } catch (StatusRuntimeException e) {
            throw new CareTaskServiceUnavailableException(
                    "Could not check pending care tasks (care-task-service: " + e.getStatus().getCode() + ")", e);
        }
    }
}
