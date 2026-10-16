# Схема пакетов модулей

Генерируется по каждому модулю:
`python3 -I docs/refactoring-kit/templates/gen-schema.py src/main/java/ru/job4j/socialmediaapi/<module>`
(с `--classes` — со списком классов). Обновляется в той же итерации, что и код.
Правила размещения — `docs/architecture.md`.

## subscriptions

```text
subscriptions/  ‹package-info (@ApplicationModule)›
├── api/  ‹interface @NamedInterface›
└── internal/
    ├── application/
    │   ├── exception/  ‹class (исключение)›
    │   ├── port/  ‹interface›
    │   │   └── model/  ‹record›
    │   └── service/  ‹class @Service›
    ├── domain/
    │   └── exception/  ‹class (исключение)›
    └── infrastructure/
        └── persistence/
            ├── adapter/  ‹class, class @Repository›
            ├── mapper/  ‹interface @Mapper›
            └── row/  ‹record›
```
