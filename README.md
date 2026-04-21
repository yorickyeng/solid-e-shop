# Лабораторная работа 4: Базовый системный дизайн
## Дисциплина: «Моделирование и надежность систем»
### Вариант 14: Информационная система интернет‑магазина (консоль)

## Разделение монолитного приложения на микросервисы с использованием gRPC

### Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client / Test                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ gRPC (port 50051)
                              │ Trace ID в метаданных
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              Service A - Core Service                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  • Управление корзиной (Cart)                             │  │
│  │  • Оформление заказов (Checkout)                          │  │
│  │  • История заказов (Order History)                        │  │
│  │  • Генерация Trace ID                                     │  │
│  │  • Graceful degradation при недоступности Service B       │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ gRPC (port 50052)
                              │ Trace ID в метаданных
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│           Service B - Reference Service                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  • Каталог товаров (Products Catalog)                     │  │
│  │  • Категории (Categories)                                 │  │
│  │  • Валидация товаров (Product Validation)                 │  │
│  │  • Валидация цен (Price Validation)                       │  │
│  │  • Логирование с Trace ID                                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Структура проекта

```
eshop/
├── proto/                          # Proto-контракт и сгенерированный код
│   ├── build.gradle.kts
│   └── src/main/proto/
│       └── shop.proto              # gRPC IDL контракт
├── service-a-core/                 # Service A - Core Service
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/ru/iu3/servicea/
│       │   ├── CoreServiceApplication.kt    # Точка входа
│       │   ├── CoreServiceImpl.kt           # Бизнес-логика
│       │   ├── ReferenceServiceClient.kt    # gRPC клиент к Service B
│       │   ├── TraceIdInterceptor.kt        # Перехватчик Trace ID
│       │   └── TestClient.kt                # Тестовый клиент
│       └── resources/
│           └── logback.xml                  # Конфигурация логирования
├── service-b-reference/            # Service B - Reference Service
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/ru/iu3/serviceb/
│       │   ├── ReferenceServiceApplication.kt  # Точка входа
│       │   ├── ReferenceServiceImpl.kt         # Реализация сервиса
│       │   └── TraceIdInterceptor.kt           # Перехватчик Trace ID
│       └── resources/
│           └── logback.xml                     # Конфигурация логирования
└── README.md                       # Этот файл
```

## Быстрый старт

### Локальный запуск через Gradle

**Терминал 1 — Service B (справочник, запускать первым):**
```bash
./gradlew :service-b-reference:run
```

**Терминал 2 — Service A (бизнес-логика):**
```bash
./gradlew :service-a-core:run
```

**Терминал 3 — демонстрационный клиент:**
```bash
./gradlew :service-a-core:runTestClient
```

Клиент прогонит сценарий: добавление 3 товаров в корзину → просмотр корзины → оформление заказа → просмотр истории.

---

## Демонстрация сценариев

### Сценарий 1 — штатный режим (оба сервиса работают)

Клиент:
```
Результат: success=true, message=Товар 'Snowboard Nitro Prime' добавлен в корзину
Корзина: 3 товаров, общая сумма: 71960.0
Результат: success=true, message=Заказ <UUID> успешно оформлен
```

Service A (фрагмент):
```
[grpc-default-executor-0] [<TRACE_ID>] INFO  CoreServiceImpl - addToCart: UserID=user-..., ProductID=p1, Quantity=1
[grpc-default-executor-0] [<TRACE_ID>] DEBUG ReferenceServiceClient - getProductById: Товар p1 найден
[grpc-default-executor-0] [<TRACE_ID>] INFO  CoreServiceImpl - Товар p1 добавлен в корзину пользователя user-...
```

Service B (тот же момент времени):
```
[grpc-default-executor-0] [<TRACE_ID>] INFO  ReferenceServiceImpl - getProductById: Запрос товара с ID=p1
[grpc-default-executor-0] [<TRACE_ID>] INFO  ReferenceServiceImpl - getProductById: Товар найден: Snowboard Nitro Prime
```

**В обоих сервисах `<TRACE_ID>` одинаковый** — цепочка вызовов отслеживается сквозь сеть.

### Сценарий 2 — Service B недоступен (graceful degradation)

1. Остановить Service B (Ctrl+C в его терминале).
2. Запустить `./gradlew :service-a-core:runTestClient`.

Клиент получает понятные сообщения, **без исключений**:
```
Результат: success=false, message=Сервис каталога временно недоступен. Попробуйте позже.
```

Service A (лог одного запроса):
```
[<TRACE_ID>] INFO  CoreServiceImpl - addToCart: UserID=..., ProductID=p1
[<TRACE_ID>] WARN  ReferenceServiceClient - Попытка 1/3 не удалась: UNAVAILABLE
[<TRACE_ID>] INFO  ReferenceServiceClient - Retry через 100 мс...
[<TRACE_ID>] WARN  ReferenceServiceClient - Попытка 2/3 не удалась: UNAVAILABLE
[<TRACE_ID>] INFO  ReferenceServiceClient - Retry через 200 мс...
[<TRACE_ID>] WARN  ReferenceServiceClient - Попытка 3/3 не удалась: UNAVAILABLE
[<TRACE_ID>] ERROR CoreServiceImpl - Reference Service недоступен после 3 попыток
```

Service A **остаётся работоспособным**: операции, не требующие Service B (`getCart`, `getOrderHistory`), продолжают отвечать корректно.

---

## Proto-контракт

Файл `proto/src/main/proto/shop.proto` определяет:

### Service B - ReferenceService
- `GetAllProducts()` - получить все товары
- `GetProductById()` - получить товар по ID
- `GetProductsByCategory()` - получить товары по категории
- `GetAllCategories()` - получить все категории
- `ValidateProductExists()` - валидировать существование товара
- `ValidateProductPrice()` - валидировать цену товара

### Service A - CoreService
- `AddToCart()` - добавить товар в корзину
- `GetCart()` - получить корзину
- `RemoveFromCart()` - удалить товар из корзины
- `ClearCart()` - очистить корзину
- `Checkout()` - оформить заказ
- `GetOrderHistory()` - получить историю заказов

## Ключевые решения

### 1. Trace ID и трассировка

Trace ID генерируется на входе в Service A и передаётся через:
- gRPC метаданные (ключ: `trace-id`)
- MDC (Mapped Diagnostic Context) для логирования

```kotlin
// TraceIdInterceptor.kt
class TraceIdInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(...) {
        val traceId = UUID.randomUUID().toString()
        MDC.put("traceId", traceId)
        // Передаём в метаданных для следующих вызовов
    }
}
```

### 2. Обработка недоступности Service B

ReferenceServiceClient реализует:
- **Таймауты**: 5 секунд на каждый вызов
- **Retry-логика**: до 3 попыток с экспоненциальной задержкой (100ms → 200ms → 400ms)
- **Graceful degradation**: при недоступности возвращается понятное сообщение пользователю

```kotlin
catch (e: ReferenceServiceUnavailableException) {
    logger.error("Reference Service недоступен. {}", e.message)
    // Возвращаем пользователю понятное сообщение
    return AddToCartResponse.newBuilder()
        .setSuccess(false)
        .setMessage("Сервис каталога временно недоступен. Попробуйте позже.")
        .build()
}
```

### 3. Структурированное логирование

Logback конфигурация включает Trace ID в каждый лог:
```
%d{HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n
```

## Автор
- Студент: _Султанов Айнур Салаватович_
- Группа: _ИУ3-61Б_
- Вариант: 14
- Дата: _22.04.2026_