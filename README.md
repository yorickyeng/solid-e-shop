# Лабораторная работа №3 — Антипаттерны проектирования
## Дисциплина: «Моделирование и надежность систем»
### Вариант 14: Информационная система интернет‑магазина (консоль)

## 🚨 Выявленные Антипаттерны

### 1. **God Class (Божественный класс)**
Класс нарушает **Single Responsibility Principle (SRP)**, выполняя 7 различных ответственностей:

```
┌─────────────────────────────────────────────────────────┐
│          GodDiscountCalculator                          │
├─────────────────────────────────────────────────────────┤
│ 1. Хранение состояния расчёта (9 полей-переменных)      │
│ 2. Получение данных из репозиториев                     │
│ 3. Расчёт скидок по категориям товаров                  │
│ 4. Расчёт объёмных скидок по порогу                     │
│ 5. Расчёт налогов                                       │
│ 6. Расчёт стоимости доставки                            │
│ 7. Расчёт комиссий платежных методов                    │
│ 8. Форматирование и вывод отчёта в консоль              │
│ 9. Быстрая оценка стоимости (quickEstimate)             │
│ 10. Экспорт в строку (exportToString)                   │
└─────────────────────────────────────────────────────────┘
```

### 2. **Feature Envy**
Метод `calculateAndPrint` чрезмерно использует данные из `ProductRepository` и `CartRepository`, но логика принадлежит доменной области, а не презентации.

### 3. **Primitive Obsession**
Использование примитивных типов (`Double`, `String`) вместо доменных объектов:
```kotlin
private val TAX_RATE = 0.13                    // Должно быть Money/TaxRate
private val DISCOUNT_THRESHOLD_1 = 5000.0      // Должно быть Money
private val SHIPPING_BASE = 300.0              // Должно быть Money
```

### 4. **Magic Numbers**
Числовые литералы без пояснений:
```kotlin
private val TAX_RATE = 0.13           // Почему 13%?
private val CARD_COMMISSION = 0.02    // Откуда 2%?
private val WEIGHT_PER_ITEM = 0.5     // Почему 0.5 кг?
```

### 5. **Data Clump**
Группы связанных полей, которые должны быть объектом:
```kotlin
// Эти 7 полей должны быть классом CalculationResult:
private var subtotal = 0.0
private var categoryDiscount = 0.0
private var volumeDiscount = 0.0
private var tax = 0.0
private var shipping = 0.0
private var commission = 0.0
private var finalTotal = 0.0
```

### 6. **Temporal Coupling**
Методы зависят от порядка вызова:
```kotlin
fun calculateAndPrint(paymentMethod: String) {  // Должен вызываться первым
    // ... устанавливает все поля
}

fun exportToString(): String {  // Требует предварительного вызова calculateAndPrint
    return "SUBTOTAL:${subtotal};..."  // Использует поля, установленные выше
}
```

### 7. **Inappropriate Intimacy**
Класс знает слишком много о внутренней структуре `CartItem` и `Product`:
```kotlin
val cartItems = cart.getItems().map { cartItem ->
    val product = productMap[cartItem.product.id] ?: cartItem.product
    Pair(product, cartItem.amount)  // Прямой доступ к внутренностям
}
```

### 8. **Switch Statements (When-выражения)**
Длинные when-выражения, которые должны быть полиморфизмом:
```kotlin
val catDiscount = when (product.category) {
    Category.SNOWBOARDS -> SNOWBOARDS_DISCOUNT
    Category.BINDINGS -> BINDINGS_DISCOUNT
    Category.BOOTS -> BOOTS_DISCOUNT
    // ... 7 случаев
}
```

### 9. **Hardcoded Values**
Жестко закодированные значения конфигурации:
```kotlin
private val SNOWBOARDS_DISCOUNT = 0.10
private val BINDINGS_DISCOUNT = 0.05
private val BOOTS_DISCOUNT = 0.07
// ... должны быть в конфигурации
```

### 10. **Mixed Concerns (Смешение ответственностей)**
Класс находится в пакете `presentation.console`, но содержит бизнес-логику:
```
presentation.console.GodDiscountCalculator
    ├── Репозитории (domain layer)
    ├── Бизнес-правила расчёта (domain layer)
    └── Вывод в консоль (presentation layer)
```

### 11. **Mutable State**
Изменяемое состояние приводит к ошибкам:
```kotlin
private var subtotal = 0.0    // Может быть изменено
private var tax = 0.0         // Зависит от порядка вызовов
```

### 12. **Poor Naming**
- `GodDiscountCalculator` — имя само признаёт проблему
- `calculateAndPrint` — нарушает Single Responsibility
- `quickEstimate` — неясное назначение

---

## 📊 Метрики класса

| Метрика | Значение | Порог | Статус |
|---------|----------|-------|--------|
| Lines of Code (LOC) | 120 | < 50 | ❌ |
| Number of Fields | 22 | < 10 | ❌ |
| Number of Methods | 4 | < 5 | ⚠️ |
| Coupling (CBO) | 2 репозитория + Category | < 3 | ⚠️ |
| Responsibility Count | 7+ | < 3 | ❌ |

---

## 🛠️ Рекомендации по Рефакторингу

### Стратегия разделения:

```
┌─────────────────────────────────────────────────────────────┐
│                    После рефакторинга                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  DiscountCalculator (Domain Layer)                          │
│  ├── calculateSubtotal(cart: Cart): Money                  │
│  └── calculateDiscounts(cart: Cart): DiscountResult        │
│                                                             │
│  TaxCalculator (Domain Layer)                               │
│  └── calculateTax(amount: Money): Money                    │
│                                                             │
│  ShippingCalculator (Domain Layer)                          │
│  └── calculateShipping(cart: Cart): Money                  │
│                                                             │
│  PaymentCommissionStrategy (Domain Layer)                   │
│  └── calculateCommission(amount: Money): Money             │
│                                                             │
│  DiscountPolicy (Domain Layer)                              │
│  └── getCategoryDiscount(category: Category): Double       │
│                                                             │
│  ConsoleReportPrinter (Presentation Layer)                  │
│  └── printReport(result: CalculationResult)                │
│                                                             │
│  CalculationResult (Value Object)                           │
│  ├── subtotal: Money                                        │
│  ├── categoryDiscount: Money                                │
│  ├── volumeDiscount: Money                                  │
│  ├── tax: Money                                             │
│  ├── shipping: Money                                        │
│  ├── commission: Money                                      │
│  └── total: Money                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Оценка Рисков

| Риск | Вероятность | Влияние | Приоритет |
|------|-------------|---------|-----------|
| **Баги при изменении логики скидок** | Высокая | Критическое | 🔴 HIGH |
| **Невозможность тестирования** | Высокая | Высокое | 🔴 HIGH |
| **Сложность добавления новых типов скидок** | Средняя | Высокое | 🟠 MEDIUM |
| **Дублирование кода в других калькуляторах** | Средняя | Среднее | 🟠 MEDIUM |
| **Нарушение работы при изменении порядка вызовов** | Высокая | Критическое | 🔴 HIGH |
| **Невозможность повторного использования** | Высокая | Среднее | 🟠 MEDIUM |

### Конкретные риски текущего кода:

1. **Гонка состояний**: Если `calculateAndPrint` вызывается дважды с разными параметрами, `exportToString` вернёт некорректные данные.

2. **Отсутствие валидации**: Нет проверки на null, отрицательные значения, пустую корзину.

3. **Невозможность unit-тестирования**: Нельзя протестировать расчёт налогов отдельно от расчёта доставки.

4. **Жесткая связка с консолью**: Невозможно использовать калькулятор в веб-интерфейсе без модификации.

5. **Финансовые ошибки**: Округление `Double` может привести к потере копеек в финансовых расчётах.

---

## Автор
- Студент: _Султанов Айнур Салаватович_
- Группа: _ИУ3-61Б_
- Вариант: 14
- Дата: _21.04.2026_