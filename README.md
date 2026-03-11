# Лабораторная работа №1 — Принципы SOLID (DRY, KISS)
## Дисциплина: «Моделирование и надежность систем»
### Вариант 14: Информационная система интернет‑магазина (консоль)

---

## 1. Описание
Консольная информационная система интернет‑магазина. Поддерживает:

- каталог товаров;
- фильтрацию товаров (по категории, по цене);
- корзину (добавление/удаление/просмотр/очистка);
- оформление заказа (checkout) с выбором способа оплаты;
- историю заказов пользователя.

Ключевая цель — продемонстрировать соответствие принципам **SOLID**, а также **DRY** и **KISS**, плюс наличие собственной иерархии исключений для повышения надежности.

---

## 2. Стек и технологии
- **Kotlin**
- Консольный ввод/вывод (`readln()`, `println`)
- Хранение данных: **in-memory** реализации репозиториев/шлюзов (без БД)

---

## 3. Архитектура и слои
Проект организован по идее “Clean Architecture / layered architecture”:

### `domain`
Содержит бизнес‑модель и контракты:
- `model`: `Product`, `Cart`, `Order`, `PaymentMethod`, `ProductFilter`, etc.
- `repository`: интерфейсы репозиториев (`ProductRepository`, `OrderRepository`, `CartRepository`)
- `payment`: `PaymentGateway`
- `exception`: иерархия исключений `ShopException -> ...`

> Domain не зависит ни от UI, ни от инфраструктуры.

### `application`
Сценарии использования (use-cases) и бизнес‑оркестрация:
- `usecase`: интерфейсы и имплементации:
    - `GetAllProductsUseCase`
    - `GetFilteredProductsUseCase`
    - `AddProductToCartUseCase`
    - `RemoveProductFromCartUseCase`
    - `GetCartUseCase`
    - `ClearCartUseCase`
    - `CheckoutUseCase`
    - `GetOrderHistoryUseCase`
- `generator`: `IdGenerator` (абстракция генерации id)

> Application зависит только от `domain` (абстракции), но не от конкретных реализаций инфраструктуры.

### `infrastructure`
Конкретные реализации интерфейсов domain/application:
- `repository`: `ProductRepositoryImpl`, `OrderRepositoryImpl`, `CartRepositoryImpl`
- `payment`: `PaymentGatewayImpl`
- `generator`: `UuidGenerator`
- `seed`: `StaticProducts` (статический каталог товаров)

### `presentation.console`
Консольный UI:
- `ConsoleApp` — один цикл меню, ввод/вывод + вызов use-case’ов
- `ConsoleDependencies` — DTO с зависимостями для UI

### `di`
Ручная сборка зависимостей (composition root):
- `AppFactory` — создаёт репозитории/шлюзы и связывает их с use-case’ами

---

## 4. Запуск
### Требования
- JDK 17+ (или подходящая версия под ваш Gradle/IDE)
- IntelliJ IDEA / JetBrains IDE

### Как запустить
Запустить `Main.kt`:

- `src/main/kotlin/ru/iu3/Main.kt`

---

## 5. Сценарии использования (функциональность)
После запуска доступно меню:

1) Показать все товары
2) Фильтр товаров
3) Добавить товар в корзину
4) Удалить товар из корзины
5) Показать корзину
6) Очистить корзину
7) Оформить заказ
8) История заказов
0) Выход

Пример потока:
- посмотреть товары → добавить `p2` x2 → показать корзину → оформить заказ → посмотреть историю

---

## 6. Надежность и обработка ошибок
В домене определена иерархия исключений:

- `ShopException` — базовое доменное исключение
    - `ValidationException`
        - `InvalidAmountException`
        - `EmptyCartException`
        - `InvalidFilterException`
    - `NotFoundException`
        - `ProductNotFoundException`
        - `OrderNotFoundException`
        - `PaymentMethodNotFoundException`

В `ConsoleApp` реализован единый обработчик ошибок:
- доменные ошибки (`ShopException`) выводятся пользователю как понятное сообщение;
- остальные (`Exception`) трактуются как непредвиденные.

---

## 7. SOLID — где и как соблюдается

### S — Single Responsibility Principle (SRP)
Каждый класс имеет одну ответственность:
- `Cart` — бизнес‑логика корзины;
- use-case классы (`AddProductToCartUseCaseImpl`, `CheckoutUseCaseImpl`, …) — один сценарий;
- `ConsoleApp` — только UI (ввод/вывод/навигация);
- `AppFactory` — только сборка зависимостей.

**Как намеренно нарушить (но оставить работоспособность):**
- перенести логику оформления заказа (создание `Order`, оплата, сохранение, очистка корзины) из `CheckoutUseCaseImpl` прямо в `ConsoleApp`.

---

### O — Open/Closed Principle (OCP)
Система расширяется добавлением новых реализаций интерфейсов, не меняя use-case:
- можно заменить `PaymentGatewayImpl` на другую реализацию оплаты;
- можно заменить репозитории на файловые/БД реализации.

**Как намеренно нарушить:**
- реализовать оплату в `CheckoutUseCaseImpl` через большой `when(method)` и при добавлении нового метода оплаты постоянно менять этот `when`.

---

### L — Liskov Substitution Principle (LSP)
Use-case работают с абстракциями (`CartRepository`, `OrderRepository`, `PaymentGateway`), и любая корректная реализация должна быть взаимозаменяемой.

**Как нарушить:**
- написать реализацию `CartRepository`, которая на каждый `get()` возвращает новый пустой `Cart`. Код будет компилироваться и запускаться, но логика сломается из‑за нарушения контракта.

---

### I — Interface Segregation Principle (ISP)
Интерфейсы маленькие и целевые:
- отдельные use-case интерфейсы под каждый сценарий;
- отдельные репозитории по области ответственности (`ProductRepository`, `OrderRepository`, `CartRepository`);
- отдельный `PaymentGateway`.

**Как нарушить:**
- сделать один “God interface” (например `ShopRepository`) со всеми методами сразу и заставить все модули зависеть от него.

---

### D — Dependency Inversion Principle (DIP)
Высокоуровневые модули (use-case) не зависят от инфраструктуры:
- `CheckoutUseCaseImpl` зависит от `PaymentGateway`, `OrderRepository`, `CartRepository`, `IdGenerator` (интерфейсы)
- конкретные реализации создаются в `AppFactory`

**Как нарушить:**
- создать `PaymentGatewayImpl()` прямо внутри `CheckoutUseCaseImpl` (жёсткая зависимость от инфраструктуры), при этом всё продолжит работать.

---

## 8. DRY и KISS
- **DRY:** общие правила и ошибки вынесены в доменные исключения; UI использует единый `handle { ... }` для обработки ошибок.
- **KISS:** простые in-memory реализации, один цикл

## 9. Структура проекта (пакеты)
Ниже приведена логическая структура пакетов:

- `ru.iu3.domain`
    - `model` — доменные сущности и value‑объекты (`Product`, `Cart`, `Order`, фильтры, перечисления)
    - `exception` — доменная иерархия исключений (надежность)
    - `repository` — контракты хранилищ (`ProductRepository`, `OrderRepository`, `CartRepository`)
    - `payment` — контракт оплаты (`PaymentGateway`)
- `ru.iu3.application`
    - `usecase` — интерфейсы сценариев
    - `usecase.impl` — реализации сценариев
    - `generator` — `IdGenerator` (абстракция генерации id)
- `ru.iu3.infrastructure`
    - `repository` — in-memory реализации репозиториев
    - `payment` — реализация `PaymentGateway`
    - `generator` — `UuidGenerator`
    - `seed` — `StaticProducts` (источник тестового каталога)
- `ru.iu3.presentation.console`
    - `ConsoleApp` — консольное меню (один цикл)
    - `ConsoleDependencies` — зависимости UI
- `ru.iu3.di`
    - `AppFactory` — composition root (сборка зависимостей)
- `ru.iu3.Main.kt`
    - точка входа: создаёт зависимости через `AppFactory` и запускает `ConsoleApp`

---

## 10. Почему репозитории/оплата сделаны in-memory
Для лабораторной работы акцент сделан на принципах проектирования и надежности, поэтому:
- данные хранятся в памяти процесса;
- можно легко заменить реализации на файловые/БД, не меняя use-case и UI (демонстрация DIP).

---

## 11. Пример расширения системы (демонстрация OCP + DIP)
Пример: добавить новый способ оплаты `CRYPTO`.

Шаги (в “правильном” варианте):
1) добавить новое значение в `PaymentMethod`;
2) реализовать новый `PaymentGateway` (или новую стратегию оплаты, если введена стратегия);
3) зарегистрировать реализацию в `AppFactory`.

При этом:
- `CheckoutUseCaseImpl` не меняется;
- `ConsoleApp` не меняется (кроме отображения списка методов, если он не строится автоматически).

---

## 12. Известные ограничения и допущения
- в системе один пользователь (`userId="u1"`), аутентификация не реализована;
- хранение in-memory, при перезапуске приложения данные сбрасываются;
- `StaticProducts` содержит тестовый набор товаров;
- `PaymentGatewayImpl` возвращает `SUCCESS` при валидной сумме (можно усложнить при необходимости).

---

## 13. Как подготовиться к защите (чек-лист)
1) Показать разбиение по слоям и зависимости: `presentation -> application -> domain <- infrastructure`, сборка в `di`.
2) По каждому SOLID:
    - показать 1–2 файла как пример соблюдения;
    - выполнить “нарушение принципа”, но оставить работоспособность:
        - SRP: вынести бизнес-логику в UI
        - OCP: сделать большой `when(method)` в checkout
        - LSP: заменить репозиторий на реализацию, нарушающую контракт
        - ISP: объединить интерфейсы в один “толстый”
        - DIP: создать конкретную инфраструктуру внутри use-case
3) Показать обработку ошибок через `ShopException` и понятные сообщения в UI.

---

## 14. Автор
- Студент: _Султанов Айнур Салаватович_
- Группа: _ИУ3-61Б_
- Вариант: 14
- Дата: _11.03.2026_