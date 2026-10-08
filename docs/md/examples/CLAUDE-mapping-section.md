## Маппинг

Для преобразования объектов между слоями используется **MapStruct 1.6.3**. Ручной маппинг в контроллерах, сервисах и адаптерах не пишется: его выносят в маппер.

**Сборка (Maven).** Зависимость `org.mapstruct:mapstruct:${mapstruct.version}`. В `maven-compiler-plugin` → `annotationProcessorPaths` процессоры перечисляются строго в порядке: `lombok` → `lombok-mapstruct-binding` (0.2.0) → `mapstruct-processor`. После указания `annotationProcessorPaths` Maven не ищет процессоры в classpath, поэтому все остальные процессоры проекта (`spring-boot-configuration-processor` и т.п.) тоже должны быть в этом списке.

**Образцы стиля** лежат в `../../mapper-examples`. Это код из другого проекта: его модели и пакеты не копируются, файлы используются только как эталон оформления.

| Образец | Когда брать за основу |
|---|---|
| `LoanLineMapper` | Простой web-маппер: Request/Response ↔ модель application |
| `ApplicationWebMapper` | Web-маппер, которому нужен Spring-бин или сложные выражения (abstract class + `@Autowired`) |
| `LoanLineAbsClientMapper` | Integration-маппер: DTO внешнего клиента ↔ модель |
| `LoanTemplatePersistenceMapper` + `LoanTemplateCurrencyMapper` | Entity ↔ Domain: `uses`, value objects через `default`-методы |
| `LoanLineAbsClientMapperTest` | Юнит-тест маппера |

**Правила:**
- Объявление: `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`. Константу `SPRING` подключать статическим импортом.
- Маппер — `interface`. Abstract class используется, только если нужен внедрённый бин (см. `ApplicationWebMapper`).
- Вложенные мапперы и конвертеры подключаются через `uses = {...}` с `injectionStrategy = InjectionStrategy.CONSTRUCTOR`. Конвертер, которому нужны бины (справочники, сервисы), — это обычный `@Component` (см. `LoanTemplateCurrencyMapper`).
- Отдельный маппер на каждую границу слоя. Маппер лежит в том же слое, что и его адаптер:
  - `infrastructure/web/mapper`: Request/Response ↔ Command/Query/Result;
  - `infrastructure/persistence/mapper`: Entity ↔ Domain;
  - `infrastructure/integration/<система>/mapper`: DTO внешнего клиента ↔ модель application;
  - `application/mapper`: Domain → Result/View.
  Domain не зависит от мапперов и DTO.
- Имена методов: `toDomain`, `toEntity`, `toResponse`, `toCommand`, `toQuery`, `toDto`, `toXxx`.
- Несовпадающие поля задаются через `@Mapping(target, source)`, вложенные пути — через `source = "a.b.c"`. Поле игнорируется только явно: `@Mapping(target = "...", ignore = true)`.
- Value objects (records) разворачиваются и собираются через `default`-методы маппера. `expression = "java(...)"` допустим только для однострочников.
- Маппер не содержит бизнес-логики и не обращается к репозиториям или внешним системам. Исключение — справочные конвертеры, подключённые через `uses`.

**Проверка:**
- `mvn clean compile` проходит без ошибок и предупреждений MapStruct об unmapped properties.
- Сгенерированная реализация лежит в `../../../target/generated-sources/annotations`. При сомнениях смотреть туда.
- Для нетривиальных мапперов (переименования, `default`-методы, `expression`) пишется юнит-тест через `Mappers.getMapper(XxxMapper.class)`, без Spring-контекста (см. `LoanLineAbsClientMapperTest`). Если у маппера есть `uses` с бинами, конвертеры подставляются вручную или маппер тестируется в составе адаптера.
