package com.cryptobank.service;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.CardUpdateRequest;
import com.cryptobank.web.dto.response.CardResponse;

import java.util.List;

public interface CardService {
    List<CardResponse> mine(UserEntity owner);
    CardResponse update(UserEntity owner, Long accountNumber, CardUpdateRequest req);
    CardResponse requestReplacement(UserEntity owner, Long accountNumber);
}
