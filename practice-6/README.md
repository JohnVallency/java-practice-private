# Практическая работа №6

Вариант 27. Java 21+. Сборка и запуск из `practice-6/`:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr03.SelfCheck
```

После проверок доступен ввод `amount;urgent;risk;personal;department`.
Например: `600000;true;LOW;false;IT`. Выход: `exit` или пустая строка.
В CSV перед `expected` добавлена колонка `department`; `null` обозначает отсутствие отдела.
Персональные данные и HIGH сохраняют приоритет над срочностью IT.
