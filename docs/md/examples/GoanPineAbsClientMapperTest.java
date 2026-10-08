package uz.kapitalbank.loanlineapplications.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.AbsLoanLineSyncResult;
import uz.kapitalbank.loanlineapplications.loanlines.internal.infrastructure.integration.abs.mapper.LoanLineAbsClientMapper;
import uz.kapitalbank.integration.loans.response.ContragentContract;
import uz.kapitalbank.integration.loans.response.GetContragentContractsClientResp;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.AbsLoanLineContract;
import uz.kapitalbank.loanlineapplications.loanlines.internal.application.model.AbsLoanLineSyncRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanLineAbsClientMapperTest {

    private final LoanLineAbsClientMapper mapper = Mappers.getMapper(LoanLineAbsClientMapper.class);

    @Test
    void toClientRequest_shouldMapRequestFields() {
        AbsLoanLineSyncRequest request = AbsLoanLineSyncRequest.builder()
            .dealTypeId(293)
            .pageSize(3)
            .pageNum(0)
            .build();

        var clientRequest = mapper.toClientRequest(request);

        assertThat(clientRequest.getDealTypeId()).isEqualTo(293);
    }

    @Test
    void toDto_shouldMapContractFields() {
        ContragentContract contract = new ContragentContract();
        contract.setDealId(10001L);
        contract.setDealTypeId(351);
        contract.setDealNumber("DL-10001");
        contract.setMaxAmount(BigDecimal.valueOf(1_000));

        AbsLoanLineContract dto = mapper.toDto(contract);

        assertThat(dto.dealId()).isEqualTo(10001L);
        assertThat(dto.dealTypeId()).isEqualTo(351);
        assertThat(dto.dealNumber()).isEqualTo("DL-10001");
        assertThat(dto.maxAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000));
    }

    @Test
    void toDto_shouldMapResponseWithData() {
        ContragentContract contract = new ContragentContract();
        contract.setDealId(10001L);
        contract.setDealTypeId(351);
        GetContragentContractsClientResp response = new GetContragentContractsClientResp();
        response.setData(List.of(contract));

        AbsLoanLineSyncResult dto = mapper.toDto(response);

        assertThat(dto.data()).hasSize(1);
        assertThat(dto.data().getFirst().dealId()).isEqualTo(10001L);
    }
}
