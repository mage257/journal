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
package de.mage.component.journal.service;

import de.mage.component.journal.JournalConfiguration;
import de.mage.component.journal.data.Currency;
import de.mage.component.journal.repository.CurrencyRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExchangeService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(JournalConfiguration.LOGGER_NAME);

  private final CurrencyRepository currencyRepository;

  @Value("${de.mage.base-currency:EUR}")
  private String baseCurrency;

  @Autowired
  public ExchangeService(final CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  public BigDecimal estimateAmount(final BigDecimal amount, final String sourceCurrencyCode,
      final String targetCurrencyCode) {
    final AtomicReference<BigDecimal> amountReference = new AtomicReference<>(amount);
    if (!sourceCurrencyCode.equals(targetCurrencyCode)) {
      final Optional<Currency> optionalTargetCurrency =
          this.currencyRepository.findById(targetCurrencyCode);
      if (!sourceCurrencyCode.equals(this.baseCurrency)) {
        this.currencyRepository.findById(sourceCurrencyCode)
            .ifPresent(currency -> {
              final BigDecimal tempAmount = amountReference.get();
              amountReference.set(
                  tempAmount.divide(currency.getRate(), MathContext.DECIMAL128)
              );
            });
      }
      if (!targetCurrencyCode.equals(this.baseCurrency)) {
        optionalTargetCurrency
            .ifPresent(currency -> {
              final BigDecimal tempAmount = amountReference.get();
              amountReference.set(
                  tempAmount.multiply(currency.getRate(), MathContext.DECIMAL128)
              );
            });
      }
      optionalTargetCurrency
          .ifPresent(targetCurrency -> {
            final BigDecimal amountToRound = amountReference.get();
            amountReference.set(
                amountToRound.setScale(targetCurrency.getPrecision(), RoundingMode.HALF_EVEN)
            );
          });
    }
    return amountReference.get();
  }
}
