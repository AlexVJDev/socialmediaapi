package uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.request.CreateLoanLineApplicationRequest;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.request.GetClientInfoRequest;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.request.GetDboLoanApplicationsRequest;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.request.GetLoanApplicationsTotalCountRequest;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.request.GetLoanLineApplicationsRequest;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.request.UploadDealDocumentRequest;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.AbsDealResultResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.ClientInfoResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.CreateLoanLineApplicationResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.DboLoanApplicationListItemResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.DboLoanApplicationListResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.DocumentBase64Response;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.EimzoWebSignResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.LoanApplicationHistoryEventResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.LoanApplicationTotalCountResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.LoanLineApplicationDetailsResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.LoanLineApplicationListItemResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.LoanLineApplicationListResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.StopFactorResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.UploadDealDocumentResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.ApplicationUserResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.ClientBaseDataResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.DocumentsResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.GrkiDataResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.LoanApplicationClientResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.LoanLineDataResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.infrastructure.web.response.loanapplication.LoanTemplateDataResponse;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.abs.AbsDealResult;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationClient;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationCount;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.query.ApplicationCountQuery;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationCurrency;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationDetails;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.documents.ApplicationDocuments;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationHistoryEvent;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationListItem;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.query.ApplicationListQuery;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationLoanLine;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationLoanTemplate;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationStopFactor;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.ApplicationUser;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.client.ClientBaseData;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.client.ClientInfo;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.client.ClientInfoQuery;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.creation.CreateApplicationCommand;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.creation.CreateApplicationResult;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.view.DboApplicationListItem;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.query.DboApplicationListQuery;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.documents.DocumentContent;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.GrkiData;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.signing.SigningResult;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.documents.UploadDealDocumentCommand;
import uz.kapitalbank.loanlineapplications.applications.internal.application.model.documents.UploadDealDocumentResult;
import uz.kapitalbank.loanlineapplications.shared.controller.response.CurrencyResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationWebMapper {

    @Autowired
    protected LoanLineApplicationPageableHelper pageableHelper;

    @Mapping(target = "documentBase64", source = "documents.application.base64String")
    public abstract CreateApplicationCommand toCommand(CreateLoanLineApplicationRequest request);

    public abstract UploadDealDocumentCommand toCommand(UploadDealDocumentRequest request);

    public abstract ClientInfoQuery toQuery(GetClientInfoRequest request);

    @Mapping(target = "statuses", expression = "java(request.resolvedStatuses())")
    @Mapping(target = "pageable", expression = "java(pageableHelper.buildPageable(request.getPageNumber(), request.getPageSize(), request.getSortBy(), request.getSortOrder()))")
    public abstract ApplicationListQuery toQuery(GetLoanLineApplicationsRequest request);

    @Mapping(target = "statuses", expression = "java(request.resolvedStatuses())")
    @Mapping(target = "pageable", expression = "java(pageableHelper.buildPageable(request.getPageNumber(), request.getPageSize(), request.getSortBy(), request.getSortOrder()))")
    public abstract DboApplicationListQuery toQuery(GetDboLoanApplicationsRequest request);

    @Mapping(target = "statuses", expression = "java(request.resolvedStatuses())")
    public abstract ApplicationCountQuery toQuery(GetLoanApplicationsTotalCountRequest request);

    public abstract AbsDealResultResponse toResponse(AbsDealResult result);

    public abstract CreateLoanLineApplicationResponse toResponse(CreateApplicationResult result);

    public abstract DocumentBase64Response toResponse(DocumentContent result);

    public abstract UploadDealDocumentResponse toResponse(UploadDealDocumentResult result);

    public abstract EimzoWebSignResponse toResponse(SigningResult result);

    @Mapping(target = "eventTypeName", source = "eventType.displayName")
    @Mapping(target = "lastStateName", source = "lastState.displayName")
    @Mapping(target = "newStateName", source = "newState.displayName")
    public abstract LoanApplicationHistoryEventResponse toResponse(ApplicationHistoryEvent result);

    public abstract LoanApplicationTotalCountResponse toResponse(ApplicationCount result);

    public abstract LoanLineApplicationDetailsResponse toResponse(ApplicationDetails result);

    public abstract LoanLineApplicationListItemResponse toResponse(ApplicationListItem result);

    public abstract DboLoanApplicationListItemResponse toResponse(DboApplicationListItem result);

    public abstract StopFactorResponse toResponse(ApplicationStopFactor result);

    public abstract ClientInfoResponse toResponse(ClientInfo result);

    public abstract ApplicationUserResponse toResponse(ApplicationUser result);

    public abstract ClientBaseDataResponse toResponse(ClientBaseData result);

    public abstract DocumentsResponse toResponse(ApplicationDocuments result);

    public abstract GrkiDataResponse toResponse(GrkiData result);

    public abstract LoanApplicationClientResponse toResponse(ApplicationClient result);

    @Mapping(target = "amount", source = "usedAmount")
    public abstract LoanLineDataResponse toResponse(ApplicationLoanLine result);

    public abstract LoanTemplateDataResponse toResponse(ApplicationLoanTemplate result);

    public abstract CurrencyResponse toResponse(ApplicationCurrency result);

    public abstract List<StopFactorResponse> toStopFactorResponses(List<ApplicationStopFactor> results);

    public abstract List<LoanApplicationHistoryEventResponse> toHistoryResponses(List<ApplicationHistoryEvent> results);

    public LoanLineApplicationListResponse toListResponse(Page<ApplicationListItem> page) {
        return new LoanLineApplicationListResponse(page.getContent().stream().map(this::toResponse).toList(), page);
    }

    public DboLoanApplicationListResponse toDboListResponse(Page<DboApplicationListItem> page) {
        return new DboLoanApplicationListResponse(page.getContent().stream().map(this::toResponse).toList(), page);
    }
}
