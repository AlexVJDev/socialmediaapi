package uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.kapitalbank.loanlineapplications.currencies.api.Currencies;
import uz.kapitalbank.loanlineapplications.currencies.api.model.CurrencyView;
import uz.kapitalbank.loanlineapplications.loantemplates.api.model.LoanTemplateCurrency;

@Component
@RequiredArgsConstructor
public class LoanTemplateCurrencyMapper {
    private final Currencies currencies;

    public LoanTemplateCurrency toCurrency(String numericCode) {
        if (numericCode == null) {
            return null;
        }
        return currencies.findByCode(numericCode).map(this::toValue)
            .orElseThrow(() -> new IllegalStateException("Currency not found: " + numericCode));
    }

    private LoanTemplateCurrency toValue(CurrencyView currency) {
        return LoanTemplateCurrency.builder()
            .numericCode(currency.numericCode())
            .alphaCode(currency.alphaCode())
            .name(currency.name())
            .scale(currency.scale() == null ? null : currency.scale().shortValue())
            .build();
    }

    public String toNumericCode(LoanTemplateCurrency currency) {
        return currency == null ? null : currency.getNumericCode();
    }
}
