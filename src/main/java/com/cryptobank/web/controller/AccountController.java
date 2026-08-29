package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.service.AccountService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.MoneyRequest;
import com.cryptobank.web.dto.request.OpenAccountRequest;
import com.cryptobank.web.dto.request.TransferRequest;
import com.cryptobank.web.dto.response.AccountResponse;
import com.cryptobank.web.dto.response.LedgerEntryResponse;
import com.cryptobank.web.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts & Transfers", description = "Account opening, balance queries, deposits, withdrawals, atomic transfers, and CSV statements")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;

    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    private UserEntity me() {
        return userService.currentUser();
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Returns all accounts owned by the authenticated user")
    public List<AccountResponse> myAccounts() {
        return accountService.myAccounts(me());
    }

    @GetMapping("/{number}")
    @Operation(summary = "Account detail", description = "Returns account details by account number (owner only)")
    public AccountResponse one(@PathVariable Long number) {
        return accountService.getOwnedAccount(me(), number);
    }

    @GetMapping("/{number}/history")
    @Operation(summary = "Paginated ledger history", description = "Returns paginated double-entry statement records for the account")
    public PagedResponse<LedgerEntryResponse> history(
            @PathVariable Long number,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return accountService.history(me(), number, page, size);
    }

    @PostMapping
    @Operation(summary = "Open new account", description = "Opens a new bank account with PIN and optional initial deposit")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.open(me(), req));
    }

    @PostMapping("/credit")
    @Operation(summary = "Deposit funds", description = "Credits account balance and creates ledger entry")
    public AccountResponse credit(@Valid @RequestBody MoneyRequest req) {
        return accountService.credit(me(), req);
    }

    @PostMapping("/debit")
    @Operation(summary = "Withdraw funds", description = "Debits account balance and creates ledger entry")
    public AccountResponse debit(@Valid @RequestBody MoneyRequest req) {
        return accountService.debit(me(), req);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds", description = "Executes an atomic, row-locked inter-account transfer")
    public AccountResponse transfer(@Valid @RequestBody TransferRequest req) {
        return accountService.transfer(me(), req);
    }

    @GetMapping("/{number}/statement.csv")
    @Operation(summary = "Export CSV statement", description = "Streams complete unpaginated account ledger to CSV")
    public ResponseEntity<byte[]> statementCsv(@PathVariable Long number) {
        List<LedgerEntryResponse> rows = accountService.fullHistory(me(), number);
        StringBuilder sb = new StringBuilder("Description,Reference,Date,Type,Amount,Balance After\n");
        for (LedgerEntryResponse e : rows) {
            sb.append(csvField(e.description())).append(',')
              .append(csvField(e.reference())).append(',')
              .append(csvField(e.createdAt() == null ? "" : e.createdAt().toString())).append(',')
              .append(csvField(e.type())).append(',')
              .append(csvField(e.amount())).append(',')
              .append(csvField(e.balanceAfter())).append('\n');
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"statement-" + number + ".csv\"")
                .body(body);
    }

    private static String csvField(Object v) {
        String s = String.valueOf(v == null ? "" : v);
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
