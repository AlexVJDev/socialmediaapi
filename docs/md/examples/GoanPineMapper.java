package uz.kapitalbank.loanlineapplications.loanlines.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.kapitalbank.loanlineapplications.shared.controller.response.CurrencyResponse;
import uz.kapitalbank.loanlineapplications.loanlines.internal.infrastructure.web.response.LoanLineByIdResponse;
import uz.kapitalbank.loanlineapplications.loanlines.internal.infrastructure.web.response.LoanLineItemResponse;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.LoanLineCurrency;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.LoanLineDetails;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface LoanLineMapper {

    @Mapping(target = "contragentName", source = "clientName")
    @Mapping(target = "dealStateId", source = "dealState")
    @Mapping(target = "totalAmount", source = "maxAmount")
    @Mapping(target = "amount", source = "usedAmount")
    LoanLineByIdResponse toByIdResponse(LoanLineDetails dto);

    @Mapping(target = "contragentName", source = "clientName")
    @Mapping(target = "totalAmount", source = "maxAmount")
    @Mapping(target = "amount", source = "usedAmount")
    LoanLineItemResponse toItemResponse(LoanLineDetails dto);

    CurrencyResponse toCurrencyResponse(LoanLineCurrency currency);

}
