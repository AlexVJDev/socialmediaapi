package uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.adapter;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.application.model.LoanTemplateListItem;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.domain.LoanTemplate;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.domain.LoanTemplateHistoryEntry;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.domain.LoanTemplateVersion;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.domain.LoanTerm;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.domain.RepaymentDay;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.entity.LoanTemplateEntity;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.entity.LoanTemplateHistoryEntity;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.entity.LoanTemplateVersionEntity;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.mapper.LoanTemplateCurrencyMapper;
import uz.kapitalbank.loanlineapplications.loantemplates.internal.infrastructure.persistence.repository.LoanTemplateListRow;
import uz.kapitalbank.loanlineapplications.loantemplates.api.model.LoanTermType;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, uses = LoanTemplateCurrencyMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface LoanTemplatePersistenceMapper {
    LoanTemplate toDomain(LoanTemplateEntity entity);

    LoanTemplateEntity toEntity(LoanTemplate template);

    @Mapping(target = "term", expression = "java(toTerm(entity))")
    LoanTemplateVersion toDomain(LoanTemplateVersionEntity entity);

    @Mapping(target = "loanTermType", expression = "java(version.getTerm().type())")
    @Mapping(target = "loanTerm", expression = "java(version.getTerm().loanTerm())")
    @Mapping(target = "loanTermDate", expression = "java(version.getTerm().loanTermDate())")
    LoanTemplateVersionEntity toEntity(LoanTemplateVersion version);

    LoanTemplateHistoryEntry toDomain(LoanTemplateHistoryEntity entity);

    LoanTemplateHistoryEntity toEntity(LoanTemplateHistoryEntry event);

    LoanTemplateListItem toListItem(LoanTemplateListRow row);

    default String toValue(RepaymentDay day) {
        return day == null ? null : day.value();
    }

    default RepaymentDay toRepaymentDay(String value) {
        return value == null ? null : new RepaymentDay(value);
    }

    default LoanTerm toTerm(LoanTemplateVersionEntity entity) {
        Integer months = entity.getLoanTermType() == LoanTermType.FIXED && entity.getLoanTerm() != null
            ? Integer.valueOf(entity.getLoanTerm()) : null;
        return LoanTerm.of(entity.getLoanTermType(), months, entity.getLoanTermDate());
    }
}
