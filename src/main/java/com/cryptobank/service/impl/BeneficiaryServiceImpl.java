package com.cryptobank.service.impl;

import com.cryptobank.domain.entity.BeneficiaryEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.repository.AccountRepository;
import com.cryptobank.repository.BeneficiaryRepository;
import com.cryptobank.service.BeneficiaryService;
import com.cryptobank.web.dto.request.BeneficiaryRequest;
import com.cryptobank.web.dto.response.BeneficiaryResponse;
import com.cryptobank.web.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaries;
    private final AccountRepository accounts;

    public BeneficiaryServiceImpl(BeneficiaryRepository beneficiaries, AccountRepository accounts) {
        this.beneficiaries = beneficiaries;
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> mine(UserEntity owner) {
        return beneficiaries.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream().map(BeneficiaryResponse::from).toList();
    }

    @Override
    @Transactional
    public BeneficiaryResponse add(UserEntity owner, BeneficiaryRequest req) {
        if (!accounts.existsByAccountNumber(req.accountNumber())) {
            throw ApiException.badRequest("No account exists with that number");
        }
        BeneficiaryEntity b = new BeneficiaryEntity();
        b.setOwner(owner);
        b.setName(req.name());
        b.setAccountNumber(req.accountNumber());
        b.setNickname(req.nickname());
        return BeneficiaryResponse.from(beneficiaries.save(b));
    }

    @Override
    @Transactional
    public void remove(UserEntity owner, Long id) {
        BeneficiaryEntity b = beneficiaries.findById(id)
                .orElseThrow(() -> ApiException.notFound("Beneficiary not found"));
        if (!b.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You don't own this beneficiary");
        }
        beneficiaries.delete(b);
    }
}
