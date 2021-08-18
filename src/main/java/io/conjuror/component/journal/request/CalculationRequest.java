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
package io.conjuror.component.journal.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CalculationRequest {
  @NotBlank
  @Size(min = 4, max = 64)
  private String agreementNumber;
  @NotBlank
  @Size(min = 4, max = 64)
  private String priceComponentCode;
  @NotNull
  private LocalDateTime referenceDate;
  @NotNull
  @Digits(integer = 12, fraction = 12)
  private BigDecimal underlying;

  public static Builder create(final String agreementNumber, final String priceComponentCode,
      final LocalDateTime referenceDate) {
    return new Builder(agreementNumber, priceComponentCode, referenceDate);
  }

  @RequiredArgsConstructor
  public static class Builder {
    private final String agreementNumber;
    private final String priceComponentCode;
    private final LocalDateTime referenceDate;
    private BigDecimal underlying;

    public Builder underlying(final BigDecimal underlying) {
      this.underlying = underlying;
      return this;
    }

    public CalculationRequest build() {
      return new CalculationRequest(this.agreementNumber, this.priceComponentCode, this.referenceDate, this.underlying);
    }
  }
}
