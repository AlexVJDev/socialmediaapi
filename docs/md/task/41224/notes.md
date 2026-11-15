# 41224 — заметки анализа

Урок: job4j #505788 «HTTP-статусы процесса» (Desc-41224.pdf).

## Что уже есть в проекте
- Модульные `*ExceptionHandler` (`@RestControllerAdvice(assignableTypes=…)`, `@Order(HIGHEST_PRECEDENCE)`) + модульные `ErrorCode` (статус + сообщение).
- `shared.exception.GlobalExceptionHandler` — только `DataIntegrityViolationException` → 500 `INTERNAL_ERROR`; обработчика `Exception.class` нет.
- Тело ошибки — рукописный `shared.web.ErrorResponse(code, message)`; в генераторе схема `error` замаплена на него (`openapi.<module>.schemas` / `imports`).
- Контракт: `docs/api/schemas/error.yaml`, ответы ошибок `application/json`.
- Генератор: `useSpringBoot4`, `interfaceOnly`, `skipDefaultInterface`, по execution на модуль. Методы сейчас возвращают `ResponseEntity<КонкретныйDto>`.
- Особенность create: генератор берёт тип по младшему 2xx (200), поэтому `createFriendshipOffer_200_response` замаплен на тип 201. С `generateGenericResponseEntity` это обходное решение, вероятно, станет не нужно.
- Дубликаты БД переводятся в исключения приложения в адаптерах (`PostgresErrors.constraint`).
- E2E: `FriendshipOfferE2ETest` покрывает 400/404/409; 500 покрыт только unit-тестом `GlobalExceptionHandlerTest`.

## Указание ментора (13.11.2026, со слов пользователя)
«Все исключения нужно сделать RuntimeException. Обрабатывать их через ControllerAdvice».
Это противоречит п.4 урока (`try/catch` в endpoint). Требование про `RuntimeException` в проекте уже выполнено (все 12 исключений).
Вопрос 6 в fix.md переписан под вариант ментора.

## Расхождения с уроком
Формат тела, общий enum кодов, расположение схем, content-type, try/catch vs advice, 400 до вызова метода, 200 на повтор create, объём, обработчик 500 — вынесены в fix.md (Раунд 1).
