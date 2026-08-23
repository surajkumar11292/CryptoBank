package com.cryptobank.service;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.BeneficiaryRequest;
import com.cryptobank.web.dto.response.BeneficiaryResponse;

import java.util.List;

public interface BeneficiaryService {
    List<BeneficiaryResponse> mine(UserEntity owner);
    BeneficiaryResponse add(UserEntity owner, BeneficiaryRequest req);
    void remove(UserEntity owner, Long id);
}
