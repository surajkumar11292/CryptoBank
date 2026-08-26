package com.cryptobank.service.impl;

import com.cryptobank.domain.entity.AccountEntity;
import com.cryptobank.domain.entity.BillPaymentEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.repository.AccountRepository;
import com.cryptobank.repository.BillPaymentRepository;
import com.cryptobank.service.AccountService;
import com.cryptobank.service.BillPayService;
import com.cryptobank.web.dto.request.BillPaymentRequest;
import com.cryptobank.web.dto.response.BillPaymentResponse;
import com.cryptobank.web.dto.response.PagedResponse;
import com.cryptobank.web.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillPayServiceImpl implements BillPayService {

    private final BillPaymentRepository billPayments;
    private final AccountRepository accounts;
    private final AccountService accountService;

    public BillPayServiceImpl(BillPaymentRepository billPayments, AccountRepository accounts, AccountService accountService) {
        this.billPayments = billPayments;
        this.accounts = accounts;
        this.accountService = accountService;
    }

    @Override
    @Transactional
    public BillPaymentResponse pay(UserEntity owner, BillPaymentRequest req) {
        var result = accountService.debitInternal(owner, req.accountNumber(), req.amount(), req.pin(),
                req.category() + " · " + req.consumer());

        AccountEntity acct = accounts.findByAccountNumber(req.accountNumber())
                .orElseThrow(() -> ApiException.notFound("Account not found"));

        BillPaymentEntity bp = new BillPaymentEntity();
        bp.setOwner(owner);
        bp.setAccount(acct);
        bp.setCategory(req.category());
        bp.setConsumer(req.consumer());
        bp.setAmount(req.amount());
        bp.setReference(result.reference());
        return BillPaymentResponse.from(billPayments.save(bp));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BillPaymentResponse> history(UserEntity owner, int page, int size) {
        Page<BillPaymentEntity> p = billPayments.findByOwnerIdOrderByCreatedAtDesc(
                owner.getId(), PageRequest.of(page, Math.min(size, 50)));
        return PagedResponse.of(p, BillPaymentResponse::from);
    }
}
