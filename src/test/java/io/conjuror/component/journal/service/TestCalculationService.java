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
import io.conjuror.component.journal.data.PriceComponent.Period;
import io.conjuror.component.journal.data.PriceComponent.Type;
import io.conjuror.component.journal.data.RecurringFee;
import io.conjuror.component.journal.repository.AdHocFeeRepository;
import io.conjuror.component.journal.repository.EventFeeRepository;
import io.conjuror.component.journal.repository.PercentageFeeRepository;
import io.conjuror.component.journal.repository.PriceComponentRepository;
import io.conjuror.component.journal.repository.RecurringFeeRepository;
import io.conjuror.component.journal.request.CalculationRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
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
public class TestCalculationService {

  private static final Random RANDOM = new Random();

  @Autowired
  private SnowflakeService snowflakeService;

  @Autowired
  private PriceComponentRepository priceComponentRepository;

  @Autowired
  private AdHocFeeRepository adHocFeeRepository;

  @Autowired
  private EventFeeRepository eventFeeRepository;

  @Autowired
  private RecurringFeeRepository recurringFeeRepository;

  @Autowired
  private PercentageFeeRepository percentageFeeRepository;

  @Autowired
  private CalculationService calculationService;

  public TestCalculationService() {
    super();
  }

  @Test
  public void givenExistingAdHocFeeWhenCalculatingShouldSucceed() {
    final PriceComponent adHocPriceComponent = new PriceComponent();
    adHocPriceComponent.setType(Type.AD_HOC);
    adHocPriceComponent.setPeriod(Period.MONTH);
    adHocPriceComponent.setCode(this.randomString());
    adHocPriceComponent.setName(this.randomString());
    adHocPriceComponent.setControlAccount(this.randomString());
    adHocPriceComponent.setCreatedAt(LocalDateTime.now());
    adHocPriceComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(adHocPriceComponent);

    final AdHocFee adHocFee = new AdHocFee();
    adHocFee.setIdentifier(this.snowflakeService.next());
    adHocFee.setPriceComponentCode(adHocPriceComponent.getCode());
    adHocFee.setAmount(BigDecimal.TEN);
    adHocFee.setWorkUnit(Boolean.TRUE);
    adHocFee.setValidFrom(LocalDateTime.now());
    adHocFee.setCreatedAt(LocalDateTime.now());
    adHocFee.setCreatedBy("unit-test");

    this.adHocFeeRepository.save(adHocFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            this.randomString(),
            adHocFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .underlying(BigDecimal.valueOf(5.0D))
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);

    Assertions.assertEquals(0, BigDecimal.valueOf(50.0D).compareTo(response.getAmount()));
    Assertions.assertEquals(adHocPriceComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(adHocPriceComponent.getCode(), response.getPriceComponentCode());
  }

  @Test
  public void givenOverriddenAdHocFeeWhenCalculatingShouldSucceed() {
    final PriceComponent adHocPriceComponent = new PriceComponent();
    adHocPriceComponent.setType(Type.AD_HOC);
    adHocPriceComponent.setPeriod(Period.MONTH);
    adHocPriceComponent.setCode(this.randomString());
    adHocPriceComponent.setName(this.randomString());
    adHocPriceComponent.setControlAccount(this.randomString());
    adHocPriceComponent.setCreatedAt(LocalDateTime.now());
    adHocPriceComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(adHocPriceComponent);

    final AdHocFee adHocFee = new AdHocFee();
    adHocFee.setIdentifier(this.snowflakeService.next());
    adHocFee.setPriceComponentCode(adHocPriceComponent.getCode());
    adHocFee.setAmount(BigDecimal.TEN);
    adHocFee.setWorkUnit(Boolean.TRUE);
    adHocFee.setValidFrom(LocalDateTime.now());
    adHocFee.setCreatedAt(LocalDateTime.now());
    adHocFee.setCreatedBy("unit-test");

    this.adHocFeeRepository.save(adHocFee);

    final String agreementNumber = this.randomString();
    final AdHocFee overriddenFee = new AdHocFee();
    overriddenFee.setIdentifier(this.snowflakeService.next());
    overriddenFee.setAgreementNumber(agreementNumber);
    overriddenFee.setPriceComponentCode(adHocPriceComponent.getCode());
    overriddenFee.setAmount(BigDecimal.ONE);
    overriddenFee.setWorkUnit(Boolean.TRUE);
    overriddenFee.setValidFrom(LocalDateTime.now());
    overriddenFee.setCreatedAt(LocalDateTime.now());
    overriddenFee.setCreatedBy("unit-test");

    this.adHocFeeRepository.save(overriddenFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            agreementNumber,
            overriddenFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .underlying(BigDecimal.valueOf(5.0D))
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);

    Assertions.assertEquals(0, BigDecimal.valueOf(5.0D).compareTo(response.getAmount()));
    Assertions.assertEquals(adHocPriceComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(adHocPriceComponent.getCode(), response.getPriceComponentCode());
  }

  @Test
  public void givenExistingEventFeeWhenCalculatingShouldSucceed() {
    final PriceComponent eventPriceComponent = new PriceComponent();
    eventPriceComponent.setType(Type.EVENT);
    eventPriceComponent.setPeriod(Period.MONTH);
    eventPriceComponent.setCode(this.randomString());
    eventPriceComponent.setName(this.randomString());
    eventPriceComponent.setControlAccount(this.randomString());
    eventPriceComponent.setCreatedAt(LocalDateTime.now());
    eventPriceComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(eventPriceComponent);

    final EventFee eventFee = new EventFee();
    eventFee.setIdentifier(this.snowflakeService.next());
    eventFee.setPriceComponentCode(eventPriceComponent.getCode());
    eventFee.setAmount(BigDecimal.valueOf(5.0D));
    eventFee.setMinimalValue(BigDecimal.TEN);
    eventFee.setValidFrom(LocalDateTime.now());
    eventFee.setCreatedAt(LocalDateTime.now());
    eventFee.setCreatedBy("unit-test");

    this.eventFeeRepository.save(eventFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            this.randomString(),
            eventFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .underlying(BigDecimal.valueOf(1000.0D))
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);

    Assertions.assertEquals(0, BigDecimal.valueOf(50.0D).compareTo(response.getAmount()));
    Assertions.assertEquals(eventPriceComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(eventPriceComponent.getCode(), response.getPriceComponentCode());
  }

  @Test
  public void givenExistingEventFeeWhenCalculatingShouldReturnMinvalue() {
    final PriceComponent eventPriceComponent = new PriceComponent();
    eventPriceComponent.setType(Type.EVENT);
    eventPriceComponent.setPeriod(Period.MONTH);
    eventPriceComponent.setCode(this.randomString());
    eventPriceComponent.setName(this.randomString());
    eventPriceComponent.setControlAccount(this.randomString());
    eventPriceComponent.setCreatedAt(LocalDateTime.now());
    eventPriceComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(eventPriceComponent);

    final EventFee eventFee = new EventFee();
    eventFee.setIdentifier(this.snowflakeService.next());
    eventFee.setPriceComponentCode(eventPriceComponent.getCode());
    eventFee.setAmount(BigDecimal.valueOf(5.0D));
    eventFee.setMinimalValue(BigDecimal.TEN);
    eventFee.setValidFrom(LocalDateTime.now());
    eventFee.setCreatedAt(LocalDateTime.now());
    eventFee.setCreatedBy("unit-test");

    this.eventFeeRepository.save(eventFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            this.randomString(),
            eventFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .underlying(BigDecimal.valueOf(100.0D))
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);

    Assertions.assertEquals(0, BigDecimal.TEN.compareTo(response.getAmount()));
    Assertions.assertEquals(eventPriceComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(eventPriceComponent.getCode(), response.getPriceComponentCode());
  }

  @Test
  public void givenExistingRecurringFeeWhenCalculatingShouldSucceed() {
    final PriceComponent recurringPriceComponent = new PriceComponent();
    recurringPriceComponent.setType(Type.RECURRING);
    recurringPriceComponent.setPeriod(Period.MONTH);
    recurringPriceComponent.setCode(this.randomString());
    recurringPriceComponent.setName(this.randomString());
    recurringPriceComponent.setControlAccount(this.randomString());
    recurringPriceComponent.setCreatedAt(LocalDateTime.now());
    recurringPriceComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(recurringPriceComponent);

    final BigDecimal monthlyFee = BigDecimal.valueOf(9.95D);
    final RecurringFee recurringFee = new RecurringFee();
    recurringFee.setIdentifier(this.snowflakeService.next());
    recurringFee.setPriceComponentCode(recurringPriceComponent.getCode());
    recurringFee.setAmount(monthlyFee);
    recurringFee.setValidFrom(LocalDateTime.now());
    recurringFee.setCreatedAt(LocalDateTime.now());
    recurringFee.setCreatedBy("unit-test");

    this.recurringFeeRepository.save(recurringFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            this.randomString(),
            recurringFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);

    Assertions.assertEquals(0, monthlyFee.compareTo(response.getAmount()));
    Assertions.assertEquals(recurringPriceComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(recurringPriceComponent.getCode(), response.getPriceComponentCode());
  }

  @Test
  public void givenExistingPercentageFeeWhenCalculatingShouldSucceed
      () {
    final PriceComponent percentageFeeComponent = new PriceComponent();
    percentageFeeComponent.setType(Type.PERCENTAGE);
    percentageFeeComponent.setPeriod(Period.MONTH);
    percentageFeeComponent.setCode(this.randomString());
    percentageFeeComponent.setName(this.randomString());
    percentageFeeComponent.setControlAccount(this.randomString());
    percentageFeeComponent.setCreatedAt(LocalDateTime.now());
    percentageFeeComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(percentageFeeComponent);

    final PercentageFee percentageFee = new PercentageFee();
    percentageFee.setIdentifier(this.snowflakeService.next());
    percentageFee.setPriceComponentCode(percentageFeeComponent.getCode());
    percentageFee.setAmount(BigDecimal.valueOf(0.5D).negate());
    percentageFee.setValidFrom(LocalDateTime.now());
    percentageFee.setCreatedAt(LocalDateTime.now());
    percentageFee.setCreatedBy("unit-test");

    this.percentageFeeRepository.save(percentageFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            this.randomString(),
            percentageFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .underlying(BigDecimal.valueOf(360000.0D).negate())
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);
    Assertions.assertEquals(0, BigDecimal.valueOf(5.0D).compareTo(response.getAmount()));
    Assertions.assertEquals(percentageFeeComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(percentageFeeComponent.getCode(), response.getPriceComponentCode());
  }

  @Test
  public void givenMultipleAdHocFeesWhenCalculatingShouldSucceed() {
    final PriceComponent adHocPriceComponent = new PriceComponent();
    adHocPriceComponent.setType(Type.AD_HOC);
    adHocPriceComponent.setPeriod(Period.MONTH);
    adHocPriceComponent.setCode(this.randomString());
    adHocPriceComponent.setName(this.randomString());
    adHocPriceComponent.setControlAccount(this.randomString());
    adHocPriceComponent.setCreatedAt(LocalDateTime.now());
    adHocPriceComponent.setCreatedBy("unit-test");

    this.priceComponentRepository.save(adHocPriceComponent);

    final LocalDateTime oldestValidDate = LocalDateTime.now().minusDays(5L);

    final AdHocFee oldestFee = new AdHocFee();
    oldestFee.setIdentifier(this.snowflakeService.next());
    oldestFee.setPriceComponentCode(adHocPriceComponent.getCode());
    oldestFee.setAmount(BigDecimal.ZERO);
    oldestFee.setWorkUnit(Boolean.TRUE);
    oldestFee.setValidFrom(oldestValidDate);
    oldestFee.setCreatedAt(LocalDateTime.now());
    oldestFee.setCreatedBy("unit-test");

    this.adHocFeeRepository.save(oldestFee);

    final AdHocFee currentFee = new AdHocFee();
    currentFee.setIdentifier(this.snowflakeService.next());
    currentFee.setPriceComponentCode(adHocPriceComponent.getCode());
    currentFee.setAmount(BigDecimal.TEN);
    currentFee.setWorkUnit(Boolean.TRUE);
    currentFee.setValidFrom(oldestFee.getValidFrom().plusDays(2L));
    currentFee.setCreatedAt(LocalDateTime.now());
    currentFee.setCreatedBy("unit-test");

    this.adHocFeeRepository.save(currentFee);

    final AdHocFee futureFee = new AdHocFee();
    futureFee.setIdentifier(this.snowflakeService.next());
    futureFee.setPriceComponentCode(adHocPriceComponent.getCode());
    futureFee.setAmount(BigDecimal.ONE);
    futureFee.setWorkUnit(Boolean.TRUE);
    futureFee.setValidFrom(currentFee.getValidFrom().plusDays(4L));
    futureFee.setCreatedAt(LocalDateTime.now());
    futureFee.setCreatedBy("unit-test");

    this.adHocFeeRepository.save(futureFee);

    final CalculationRequest calculationRequest = CalculationRequest
        .create(
            this.randomString(),
            futureFee.getPriceComponentCode(),
            LocalDateTime.now()
        )
        .underlying(BigDecimal.valueOf(5.0D))
        .build();

    final CalculationResponse response = this.calculationService.process(calculationRequest);

    Assertions.assertEquals(0, BigDecimal.valueOf(50.0D).compareTo(response.getAmount()));
    Assertions.assertEquals(adHocPriceComponent.getControlAccount(), response.getControlAccount());
    Assertions.assertEquals(adHocPriceComponent.getCode(), response.getPriceComponentCode());
  }

  private String randomString() {
    final byte[] randomBytes = new byte[32];
    RANDOM.nextBytes(randomBytes);
    return Base64.getEncoder().encodeToString(randomBytes);
  }
}
