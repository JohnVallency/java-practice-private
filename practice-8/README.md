# Практическая работа №8

Вариант 27. Java 21+. Сборка и запуск из `practice-8/`:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr03.SelfCheck
```

После проверок доступны `NEW id;start;end`, `SUBMIT`, `APPROVE`, `REJECT:<причина>`,
`CANCEL`, `COMPLETE`, `STATUS`. Выход: `exit` или пустая строка.
Даты: `yyyy-MM-dd`. Пример: `REJECT:R-001: Недостаточно бюджета` после `SUBMIT`.

В CSV сохранены пять колонок. В `action` действия могут разделяться `>`:
`SUBMIT>REJECT:R-001: Недостаточно бюджета`. Причина содержит минимум 15 символов и код `R-NNN`.
