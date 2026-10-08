package uz.kapitalbank.loanlineapplications.loanlines.internal.infrastructure.integration.abs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.kapitalbank.integration.loans.request.GetContragentContractsClientReq;
import uz.kapitalbank.integration.loans.response.ContragentContract;
import uz.kapitalbank.integration.loans.response.GetContragentContractsClientResp;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.AbsLoanLineContract;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.AbsLoanLineSyncRequest;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.AbsLoanLineSyncResult;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface LoanLineAbsClientMapper {

    GetContragentContractsClientReq toClientRequest(AbsLoanLineSyncRequest request);

    @Mapping(target = "pageSize", ignore = true)
    @Mapping(target = "pageNum", ignore = true)
    @Mapping(target = "totalPages", ignore = true)
    @Mapping(target = "loadedCount", ignore = true)
    AbsLoanLineSyncResult toDto(GetContragentContractsClientResp response);

    AbsLoanLineContract toDto(ContragentContract contract);
}
