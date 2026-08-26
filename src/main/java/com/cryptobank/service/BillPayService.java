package com.cryptobank.service;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.BillPaymentRequest;
import com.cryptobank.web.dto.response.BillPaymentResponse;
import com.cryptobank.web.dto.response.PagedResponse;

public interface BillPayService {
    BillPaymentResponse pay(UserEntity owner, BillPaymentRequest req);
    PagedResponse<BillPaymentResponse> history(UserEntity owner, int page, int size);
}
