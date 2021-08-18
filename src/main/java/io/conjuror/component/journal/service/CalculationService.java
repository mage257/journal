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

import io.conjuror.component.journal.common.CalculationResponse;
import io.conjuror.component.journal.data.AdHocFee;
import io.conjuror.component.journal.data.EventFee;
import io.conjuror.component.journal.data.PercentageFee;
import io.conjuror.component.journal.data.PriceComponent;
import io.conjuror.component.journal.data.RecurringFee;
import io.conjuror.component.journal.exception.ResourceNotFoundException;
import io.conjuror.component.journal.repository.AdHocFeeRepository;
import io.conjuror.component.journal.repository.EventFeeRepository;
import io.conjuror.component.journal.repository.PercentageFeeRepository;
import io.conjuror.component.journal.repository.PriceComponentRepository;
import io.conjuror.component.journal.repository.RecurringFeeRepository;
import io.conjuror.component.journal.request.CalculationRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalculationService {

  private static final BigDecimal BASE = BigDecimal.valueOf(100.0D);
  private static final BigDecimal YEAR_DAY_COUNT = BigDecimal.valueOf(360.0D);

  private final PriceComponentRepository priceComponentRepository;
  private final EventFeeRepository eventFeeRepository;
  private final AdHocFeeRepository adHocFeeRepository;
  private final RecurringFeeRepository recurringFeeRepository;
  private final PercentageFeeRepository percentageFeeRepository;

  @Autowired
  public CalculationService(
      final PriceComponentRepository priceComponentRepository,
      final EventFeeRepository eventFeeRepository,
      final AdHocFeeRepository adHocFeeRepository,
      final RecurringFeeRepository recurringFeeRepository,
      final PercentageFeeRepository percentageFeeRepository) {
    super();
    this.priceComponentRepository = priceComponentRepository;
    this.eventFeeRepository = eventFeeRepository;
    this.adHocFeeRepository = adHocFeeRepository;
    this.recurringFeeRepository = recurringFeeRepository;
    this.percentageFeeRepository = percentageFeeRepository;
  }

  public CalculationResponse process(final CalculationRequest calculationRequest) {
    final CalculationResponse calculationResponse = new CalculationResponse();

    final PriceComponent priceComponent = this.priceComponentRepository.findById(
            calculationRequest.getPriceComponentCode())
        .orElseThrow(() ->
            new ResourceNotFoundException(
                String.format(
                    "Price component '%s' not found.",
                    calculationRequest.getPriceComponentCode()
                )
            )
        );
    calculationResponse.setPriceComponentCode(priceComponent.getCode());
    calculationResponse.setControlAccount(priceComponent.getControlAccount());

    switch (priceComponent.getType()) {
      case EVENT:
        calculationResponse.setAmount(
            this.calculateEventFee(
                calculationRequest.getAgreementNumber(),
                priceComponent.getCode(),
                calculationRequest.getReferenceDate(),
                calculationRequest.getUnderlying()
            )
        );
        break;
      case AD_HOC:
        calculationResponse.setAmount(
            this.calculateAdHocFee(
                calculationRequest.getAgreementNumber(),
                priceComponent.getCode(),
                calculationRequest.getReferenceDate(),
                calculationRequest.getUnderlying()
            )
        );
        break;
      case RECURRING:
        calculationResponse.setAmount(
            this.determineRecurringFee(
                calculationRequest.getAgreementNumber(),
                priceComponent.getCode(),
                calculationRequest.getReferenceDate()
            )
        );
        break;
      case PERCENTAGE:
        calculationResponse.setAmount(
            this.accruePercentageFee(
                calculationRequest.getAgreementNumber(),
                priceComponent.getCode(),
                calculationRequest.getReferenceDate(),
                calculationRequest.getUnderlying()
            )
        );
        break;
    }

    return calculationResponse;
  }

  private BigDecimal calculateEventFee(final String agreementNumber, final String priceComponentCode,
      final LocalDateTime referenceDate, final BigDecimal transactionAmount) {
    final AtomicReference<BigDecimal> amountReference = new AtomicReference<>(BigDecimal.ZERO);

    this.eventFeeRepository
        .findByAgreementNumberAndPriceComponentCodeAndValidFromIsLessThanEqual(
            agreementNumber, priceComponentCode, referenceDate
        )
        .or(() ->
          this.eventFeeRepository
              .findAllByPriceComponentCodeAndValidFromIsLessThanEqualAndAgreementNumberIsNull(
                  priceComponentCode, referenceDate
              )
              .stream()
              .max(Comparator.comparing(EventFee::getValidFrom))
        )
        .ifPresent(eventFee -> {
          final BigDecimal percentage =
              eventFee.getAmount().divide(BASE, MathContext.DECIMAL128);

          final BigDecimal calculatedAmount =
              transactionAmount.multiply(percentage, MathContext.DECIMAL128);
          amountReference.set(calculatedAmount.max(eventFee.getMinimalValue()));
        });

    return amountReference.get();
  }

  private BigDecimal calculateAdHocFee(final String agreementNumber, final String priceComponentCode,
      final LocalDateTime referenceDate, final BigDecimal workUnits) {
    final AtomicReference<BigDecimal> amountReference = new AtomicReference<>(BigDecimal.ZERO);

    this.adHocFeeRepository
        .findByAgreementNumberAndPriceComponentCodeAndValidFromIsLessThanEqual(
            agreementNumber, priceComponentCode, referenceDate
        )
        .or(() ->
            this.adHocFeeRepository
                .findAllByPriceComponentCodeAndValidFromIsLessThanEqualAndAgreementNumberIsNull(
                    priceComponentCode, referenceDate
                )
                .stream()
                .max(Comparator.comparing(AdHocFee::getValidFrom))
        )
        .ifPresent(adHocFee -> {
          amountReference.set(adHocFee.getAmount());
          if (adHocFee.getWorkUnit()) {
            amountReference.accumulateAndGet(
                workUnits,
                (amount, units) -> amount.multiply(units, MathContext.DECIMAL128)
            );
          }
        });

    return amountReference.get();
  }

  private BigDecimal determineRecurringFee(final String agreementNumber, final String priceComponentCode,
      final LocalDateTime referenceDate) {
    final AtomicReference<BigDecimal> amountReference = new AtomicReference<>(BigDecimal.ZERO);

    this.recurringFeeRepository
        .findByAgreementNumberAndPriceComponentCodeAndValidFromIsLessThanEqual(
            agreementNumber, priceComponentCode, referenceDate
        )
        .or(() ->
            this.recurringFeeRepository
                .findAllByPriceComponentCodeAndValidFromIsLessThanEqualAndAgreementNumberIsNull(
                    priceComponentCode, referenceDate
                )
                .stream()
                .max(Comparator.comparing(RecurringFee::getValidFrom))
        )
        .ifPresent(recurringFee -> amountReference.set(recurringFee.getAmount()));

    return amountReference.get();
  }

  private BigDecimal accruePercentageFee(final String agreementNumber,  final String priceComponentCode,
      final LocalDateTime referenceDate, final BigDecimal currentBalance) {
    final AtomicReference<BigDecimal> amountReference = new AtomicReference<>(BigDecimal.ZERO);

    this.percentageFeeRepository
        .findByAgreementNumberAndPriceComponentCodeAndValidFromIsLessThanEqual(
            agreementNumber, priceComponentCode, referenceDate
        )
        .or(() ->
            this.percentageFeeRepository
                .findAllByPriceComponentCodeAndValidFromIsLessThanEqualAndAgreementNumberIsNull(
                    priceComponentCode, referenceDate
                )
                .stream()
                .max(Comparator.comparing(PercentageFee::getValidFrom))
        )
        .ifPresent(percentageFee -> {
          final BigDecimal percentage =
              percentageFee.getAmount().divide(BASE, MathContext.DECIMAL128);

          amountReference.set(
              currentBalance
                  .multiply(percentage, MathContext.DECIMAL128)
                  .divide(YEAR_DAY_COUNT, MathContext.DECIMAL128)
          );
        });

    return amountReference.get();
  }
}
