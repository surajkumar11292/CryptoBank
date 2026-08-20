package com.cryptobank.service;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.MoneyRequest;
import com.cryptobank.web.dto.request.OpenAccountRequest;
import com.cryptobank.web.dto.request.TransferRequest;
import com.cryptobank.web.dto.response.AccountResponse;
import com.cryptobank.web.dto.response.LedgerEntryResponse;
import com.cryptobank.web.dto.response.PagedResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    record DebitResult(AccountResponse account, String reference) {}

    List<AccountResponse> myAccounts(UserEntity owner);
    AccountResponse getOwnedAccount(UserEntity owner, Long accountNumber);
    PagedResponse<LedgerEntryResponse> history(UserEntity owner, Long accountNumber, int page, int size);
    List<LedgerEntryResponse> fullHistory(UserEntity owner, Long accountNumber);
    AccountResponse open(UserEntity owner, OpenAccountRequest req);
    AccountResponse credit(UserEntity owner, MoneyRequest req);
    AccountResponse debit(UserEntity owner, MoneyRequest req);
    DebitResult debitInternal(UserEntity owner, Long accountNumber, BigDecimal amount, String pin, String description);
    AccountResponse transfer(UserEntity owner, TransferRequest req);
    void setFrozen(Long accountNumber, boolean frozen);
}
