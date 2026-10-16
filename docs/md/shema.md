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

## posts

```text
posts/  ‹package-info (@ApplicationModule)›
└── internal/
    ├── application/
    │   └── port/  ‹interface›
    │       └── model/  ‹record›
    ├── domain/
    │   └── model/  ‹enum›
    └── infrastructure/
        └── persistence/
            ├── adapter/  ‹class @Repository›
            ├── mapper/  ‹interface @Mapper›
            └── row/  ‹record›
```

## friendships

```text
friendships/  ‹package-info (@ApplicationModule)›
└── internal/
    ├── application/
    │   ├── exception/  ‹class (исключение)›
    │   ├── mapper/  ‹interface @Mapper›
    │   ├── model/  ‹record›
    │   ├── port/  ‹interface›
    │   │   └── model/  ‹record›
    │   └── usecase/  ‹class @Service›
    ├── domain/
    │   ├── exception/  ‹class (исключение)›
    │   └── model/  ‹enum, record›
    └── infrastructure/
        ├── persistence/
        │   ├── adapter/  ‹class @Repository, class›
        │   ├── mapper/  ‹interface @Mapper›
        │   └── row/  ‹record›
        └── web/  ‹class @RestController, class @RestControllerAdvice›
            ├── error/  ‹enum›
            ├── mapper/  ‹interface @Mapper›
            ├── request/  ‹record›
            └── response/  ‹record›
```

## system

```text
system/  ‹package-info (@ApplicationModule)›
└── internal/
    └── infrastructure/
        └── web/  ‹class @RestController›
```
