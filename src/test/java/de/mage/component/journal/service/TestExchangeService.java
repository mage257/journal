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

import de.mage.component.journal.data.Currency;
import de.mage.component.journal.repository.CurrencyRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        TestConfiguration.class
    }
)
public class TestExchangeService {

  @Autowired
  private CurrencyRepository currencyRepository;

  @Autowired
  private ExchangeService exchangeService;

  public TestExchangeService() {
    super();
  }

  @Test
  public void shouldNotExchangeBaseCurrencyOnly() {
    final BigDecimal amount = BigDecimal.TEN;
    final String eur = "EUR";

    final BigDecimal result = this.exchangeService.estimateAmount(amount, eur, eur);
    Assertions.assertEquals(0, amount.compareTo(result));
  }

  @Test
  public void shouldNotExchangeSimilarCurrency() {
    final BigDecimal amount = BigDecimal.TEN;
    final String usd = "USD";

    final BigDecimal result = this.exchangeService.estimateAmount(amount, usd, usd);
    Assertions.assertEquals(0, amount.compareTo(result));
  }

  @Test
  public void shouldExchangeDifferentSourceCurrency() {
    final String usd = "USD";
    this.currencyRepository.findById(usd)
        .orElseGet(() -> {
          final Currency newCurrency = new Currency();
          newCurrency.setCode(usd);
          newCurrency.setRate(BigDecimal.valueOf(1.1925535D));
          newCurrency.setPrecision(2);
          this.currencyRepository.save(newCurrency);
          return newCurrency;
        });

    final String eur = "EUR";
    this.currencyRepository.findById(eur)
        .orElseGet(() -> {
          final Currency newCurrency = new Currency();
          newCurrency.setCode(eur);
          newCurrency.setRate(BigDecimal.ONE);
          newCurrency.setPrecision(2);
          this.currencyRepository.save(newCurrency);
          return newCurrency;
        });

    final BigDecimal result =
        this.exchangeService.estimateAmount(BigDecimal.TEN, usd, eur);
    Assertions.assertEquals(0, BigDecimal.valueOf(8.39D).compareTo(result));
  }

  @Test
  public void shouldExchangeDifferentTargetCurrency() {
    final String eur = "EUR";
    this.currencyRepository.findById(eur)
        .orElseGet(() -> {
          final Currency newCurrency = new Currency();
          newCurrency.setCode(eur);
          newCurrency.setRate(BigDecimal.ONE);
          newCurrency.setPrecision(2);
          this.currencyRepository.save(newCurrency);
          return newCurrency;
        });

    final String gbp = "GBP";
    this.currencyRepository.findById(gbp)
        .orElseGet(() -> {
          final Currency newCurrency = new Currency();
          newCurrency.setCode(gbp);
          newCurrency.setRate(BigDecimal.valueOf(0.85430612D));
          newCurrency.setPrecision(2);
          this.currencyRepository.save(newCurrency);
          return newCurrency;
        });

    final BigDecimal result = this.exchangeService.estimateAmount(BigDecimal.TEN, eur, gbp);
    Assertions.assertEquals(0, BigDecimal.valueOf(8.54D).compareTo(result));
  }

  @Test
  public void shouldExchangeDifferentSourceAndTargetCurrency() {
    final String gbp = "GBP";
    this.currencyRepository.findById(gbp)
        .orElseGet(() -> {
          final Currency newCurrency = new Currency();
          newCurrency.setCode(gbp);
          newCurrency.setRate(BigDecimal.valueOf(0.85430612D));
          newCurrency.setPrecision(2);
          this.currencyRepository.save(newCurrency);
          return newCurrency;
        });

    final String usd = "USD";
    this.currencyRepository.findById(usd)
        .orElseGet(() -> {
          final Currency newCurrency = new Currency();
          newCurrency.setCode(usd);
          newCurrency.setRate(BigDecimal.valueOf(1.1925535D));
          newCurrency.setPrecision(2);
          this.currencyRepository.save(newCurrency);
          return newCurrency;
        });

    final BigDecimal result =
        this.exchangeService.estimateAmount(BigDecimal.valueOf(8.54D), gbp, usd);
    Assertions.assertEquals(0, BigDecimal.valueOf(11.92D).compareTo(result));
  }
}
