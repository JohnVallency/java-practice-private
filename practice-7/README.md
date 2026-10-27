# Практическая работа №7

Вариант 27. Java 21+. Сборка и запуск из `practice-7/`:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr04.SelfCheck
```

После проверок доступны команды `add <id> <name> <price> <qty>`, `remove <id>`,
`total`, `items`, `confirm`, `status`, `exit`. Имя товара может содержать пробелы.

В CSV строка `TOTAL` дополнена `history=N` — ожидаемым количеством записей истории.
