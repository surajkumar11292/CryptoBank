package com.cryptobank.service.impl;

import com.cryptobank.domain.entity.AccountEntity;
import com.cryptobank.domain.entity.LedgerEntryEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.domain.enums.LedgerType;
import com.cryptobank.repository.AccountRepository;
import com.cryptobank.repository.LedgerEntryRepository;
import com.cryptobank.security.TotpService;
import com.cryptobank.service.AccountService;
import com.cryptobank.util.AccountNumberGenerator;
import com.cryptobank.util.ReferenceGenerator;
import com.cryptobank.web.dto.request.MoneyRequest;
import com.cryptobank.web.dto.request.OpenAccountRequest;
import com.cryptobank.web.dto.request.TransferRequest;
import com.cryptobank.web.dto.response.AccountResponse;
import com.cryptobank.web.dto.response.LedgerEntryResponse;
import com.cryptobank.web.dto.response.PagedResponse;
import com.cryptobank.web.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000.00");

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final PasswordEncoder encoder;
    private final TotpService totpService;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountServiceImpl(AccountRepository accounts, LedgerEntryRepository ledger,
                              PasswordEncoder encoder, TotpService totpService,
                              AccountNumberGenerator accountNumberGenerator) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.encoder = encoder;
        this.totpService = totpService;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> myAccounts(UserEntity owner) {
        return accounts.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream().map(AccountResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getOwnedAccount(UserEntity owner, Long accountNumber) {
        return AccountResponse.from(requireOwned(owner, accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LedgerEntryResponse> history(UserEntity owner, Long accountNumber, int page, int size) {
        AccountEntity acct = requireOwned(owner, accountNumber);
        Page<LedgerEntryEntity> p = ledger.findByAccountIdOrderByCreatedAtDesc(
                acct.getId(), PageRequest.of(page, Math.min(size, 100)));
        return PagedResponse.of(p, LedgerEntryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> fullHistory(UserEntity owner, Long accountNumber) {
        AccountEntity acct = requireOwned(owner, accountNumber);
        return ledger.findByAccountIdOrderByCreatedAtDesc(acct.getId())
                .stream().map(LedgerEntryResponse::from).toList();
    }

    @Override
    @Transactional
    public AccountResponse open(UserEntity owner, OpenAccountRequest req) {
        AccountEntity a = new AccountEntity();
        a.setOwner(owner);
        a.setHolderName(req.holderName());
        a.setBalance(req.openingBalance());
        a.setPinHash(encoder.encode(req.pin()));
        a.setAccountNumber(accountNumberGenerator.generate());
        AccountEntity saved = accounts.save(a);

        if (req.openingBalance().signum() > 0) {
            writeEntry(saved, LedgerType.CREDIT, req.openingBalance(),
                    saved.getBalance(), "Opening deposit", ReferenceGenerator.generateTransactionReference());
        }
        return AccountResponse.from(saved);
    }

    @Override
    @Transactional
    public AccountResponse credit(UserEntity owner, MoneyRequest req) {
        AccountEntity a = lockOwned(owner, req.accountNumber());
        requireNotFrozen(a);
        verifyPin(a, req.pin());
        a.setBalance(a.getBalance().add(req.amount()));
        writeEntry(a, LedgerType.CREDIT, req.amount(), a.getBalance(), "Cash deposit", ReferenceGenerator.generateTransactionReference());
        return AccountResponse.from(a);
    }

    @Override
    @Transactional
    public AccountResponse debit(UserEntity owner, MoneyRequest req) {
        return debitInternal(owner, req.accountNumber(), req.amount(), req.pin(), "Cash withdrawal").account();
    }

    @Override
    @Transactional
    public DebitResult debitInternal(UserEntity owner, Long accountNumber, BigDecimal amount, String pin, String description) {
        AccountEntity a = lockOwned(owner, accountNumber);
        requireNotFrozen(a);
        verifyPin(a, pin);
        if (a.getBalance().compareTo(amount) < 0) {
            throw ApiException.badRequest("Insufficient balance");
        }
        a.setBalance(a.getBalance().subtract(amount));
        String ref = ReferenceGenerator.generateTransactionReference();
        writeEntry(a, LedgerType.DEBIT, amount, a.getBalance(), description, ref);
        return new DebitResult(AccountResponse.from(a), ref);
    }

    @Override
    @Transactional
    public AccountResponse transfer(UserEntity owner, TransferRequest req) {
        if (req.fromAccountNumber().equals(req.toAccountNumber())) {
            throw ApiException.badRequest("Cannot transfer to the same account");
        }

        // Lock accounts in deterministic order (lowest account number first) to eliminate deadlocks
        long lo = Math.min(req.fromAccountNumber(), req.toAccountNumber());
        long hi = Math.max(req.fromAccountNumber(), req.toAccountNumber());
        AccountEntity first = lock(lo);
        AccountEntity second = lock(hi);
        AccountEntity from = req.fromAccountNumber() == lo ? first : second;
        AccountEntity to = req.toAccountNumber() == lo ? first : second;

        if (!from.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You don't own the source account");
        }
        requireNotFrozen(from);
        requireNotFrozen(to);
        verifyPin(from, req.pin());
        if (from.getBalance().compareTo(req.amount()) < 0) {
            throw ApiException.badRequest("Insufficient balance");
        }
        requireTotpForHighValue(from, req.amount(), req.totpCode());

        String ref = ReferenceGenerator.generateTransactionReference();
        from.setBalance(from.getBalance().subtract(req.amount()));
        to.setBalance(to.getBalance().add(req.amount()));

        writeEntry(from, LedgerType.DEBIT, req.amount(), from.getBalance(),
                "Transfer to #" + to.getAccountNumber(), ref);
        writeEntry(to, LedgerType.CREDIT, req.amount(), to.getBalance(),
                "Transfer from #" + from.getAccountNumber(), ref);

        return AccountResponse.from(from);
    }

    @Override
    @Transactional
    public void setFrozen(Long accountNumber, boolean frozen) {
        AccountEntity a = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account #" + accountNumber + " not found"));
        a.setFrozen(frozen);
    }

    private void requireNotFrozen(AccountEntity a) {
        if (a.isFrozen()) {
            throw ApiException.forbidden("Account #" + a.getAccountNumber() + " is frozen — contact support");
        }
    }

    private void requireTotpForHighValue(AccountEntity from, BigDecimal amount, String totpCode) {
        if (!from.getOwner().isTotpEnabled()) return;
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) < 0) return;
        if (totpCode == null || totpCode.isBlank()) {
            throw ApiException.totpRequired("This transfer is above ₹" + HIGH_VALUE_THRESHOLD.toPlainString()
                    + " — enter your 2FA code to confirm");
        }
        if (!totpService.verify(from.getOwner().getTotpSecret(), totpCode)) {
            throw ApiException.totpInvalid("Incorrect 2FA code");
        }
    }

    private AccountEntity requireOwned(UserEntity owner, Long number) {
        AccountEntity a = accounts.findByAccountNumber(number)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!a.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You don't own this account");
        }
        return a;
    }

    private AccountEntity lockOwned(UserEntity owner, Long number) {
        AccountEntity a = lock(number);
        if (!a.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You don't own this account");
        }
        return a;
    }

    private AccountEntity lock(Long number) {
        return accounts.lockByAccountNumber(number)
                .orElseThrow(() -> ApiException.notFound("Account #" + number + " not found"));
    }

    private void verifyPin(AccountEntity a, String pin) {
        if (!encoder.matches(pin, a.getPinHash())) {
            throw ApiException.forbidden("Invalid security PIN");
        }
    }

    private void writeEntry(AccountEntity a, LedgerType type, BigDecimal amount,
                            BigDecimal balanceAfter, String desc, String ref) {
        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setAccount(a);
        e.setType(type);
        e.setAmount(amount);
        e.setBalanceAfter(balanceAfter);
        e.setDescription(desc);
        e.setReference(ref);
        ledger.save(e);
    }
}
