/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */
package io.conjuror.component.journal.service;

import io.conjuror.component.journal.common.Balance;
import io.conjuror.component.journal.data.Account;
import io.conjuror.component.journal.data.Journal.State;
import io.conjuror.component.journal.repository.AccountRepository;
import io.conjuror.component.journal.repository.JournalItemRepository;
import io.conjuror.component.journal.repository.JournalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

  private final ExchangeService exchangeService;
  private final JournalRepository journalRepository;
  private final JournalItemRepository journalItemRepository;
  private final AccountRepository accountRepository;

  @Value("${io.conjuror.base-currency:EUR}")
  private String baseCurrency;

  @Autowired
  public AccountService(
      final ExchangeService exchangeService,
      final JournalRepository journalRepository,
      final JournalItemRepository journalItemRepository,
      final AccountRepository accountRepository) {
    super();
    this.exchangeService = exchangeService;
    this.journalRepository = journalRepository;
    this.journalItemRepository = journalItemRepository;
    this.accountRepository = accountRepository;
  }

  public Balance determineBalance(final String accountNumber, final String currencyCode) {
    final Account account = this.accountRepository.findById(accountNumber)
        .orElseGet(() -> {
          final Account newAccount = new Account();
          newAccount.setNumber(accountNumber);
          newAccount.setBalance(BigDecimal.ZERO);
          newAccount.setLastSynchronizedSequence(Long.MIN_VALUE);
          return newAccount;
        });

    final AtomicReference<BigDecimal> accountBalanceReference = new AtomicReference<>(account.getBalance());
    final AtomicReference<Long> valueDateReference = new AtomicReference<>(account.getLastSynchronizedSequence());

    final LocalDate now = LocalDate.now(Clock.systemUTC());
    this.journalRepository
        .findAllByStateAndSequenceGreaterThan(State.RELEASED, account.getLastSynchronizedSequence())
            .stream()
            .filter(journal -> journal.getValueDate().equals(now) || journal.getValueDate().isBefore(now))
            .forEach(journal -> {
              this.journalItemRepository.findAllByJournalSequenceOrderBySequence(journal.getSequence())
                  .forEach(journalItem -> {
                    if (journalItem.getSource().getAccountReference().equals(accountNumber)) {
                      accountBalanceReference
                          .getAndAccumulate(
                              this.exchangeService.estimateAmount(journalItem.getSource().getAmount(), journal.getCurrencyCode(), currencyCode),
                              BigDecimal::subtract
                          );
                    }

                    accountBalanceReference.getAndAccumulate(
                        journalItem.getTargets()
                            .stream()
                            .filter(
                                allocation -> allocation.getAccountReference().equals(accountNumber))
                            .map(allocation ->
                                this.exchangeService.estimateAmount(allocation.getAmount(), journal.getCurrencyCode(), currencyCode))
                            .reduce(BigDecimal::add)
                            .orElse(BigDecimal.ZERO),
                        BigDecimal::add
                    );
                  });

              valueDateReference.updateAndGet(sequence -> {
                if (journal.getSequence() >= sequence) {
                  return journal.getSequence();
                } else {
                  return sequence;
                }
              });
            });

    account.setBalance(accountBalanceReference.get());
    account.setLastSynchronizedSequence(valueDateReference.get());
    this.accountRepository.save(account);

    final Balance balance = new Balance();
    balance.setAccountNumber(accountNumber);
    balance.setCurrencyCode(currencyCode);
    balance.setAccountBalance(accountBalanceReference.get());
    balance.setReportingBalance(
        this.exchangeService.estimateAmount(
            balance.getAccountBalance(),
            balance.getCurrencyCode(),
            this.baseCurrency
        )
    );

    return balance;
  }
}
