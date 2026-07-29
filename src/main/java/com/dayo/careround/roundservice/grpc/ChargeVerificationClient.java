package com.dayo.careround.roundservice.grpc;

import com.dayo.careround.wardservice.grpc.ChargeVerificationRequest;
import com.dayo.careround.wardservice.grpc.ChargeVerificationResponse;
import com.dayo.careround.wardservice.grpc.ChargeVerificationServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChargeVerificationClient {

    @GrpcClient("ward-service")
    private ChargeVerificationServiceGrpc.ChargeVerificationServiceBlockingStub chargeStub;

    public boolean isActiveChargeNurse(UUID wardId, UUID nurseId) {
        ChargeVerificationRequest request = ChargeVerificationRequest.newBuilder()
                .setWardId(wardId.toString())
                .setNurseId(nurseId.toString())
                .build();
        return chargeStub.isActiveChargeNurse(request).getIsActiveChargeNurse();
    }
}