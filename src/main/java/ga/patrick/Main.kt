package ga.patrick

import java.io.File
import java.sql.*

class Main {
    companion object {

        private var conn: Connection? = null
        private var statmt: Statement? = null

        private const val url = "jdbc:oracle:thin:@oraclebi.avalon.ru:1521:orcl12"
        private var login: String? = null
        private var password: String? = null

        private const val ERROR_OUT = "При (%s) \t получено'%11s', \tожидалось   \t'%10s'"
        private const val CORRECT_OUT = "Верно: (%s) -> '%s'"

        private const val SELECT_FILE: String = "select.sql"
        private const val TESTS_FILE: String = "tests.txt"

        @JvmStatic
        fun main(args: Array<String>) {
            println("Подключение...")

            if (args.size == 2) {
                login = args[0]
                password = args[1]
            } else {
                println("При запуске введите логин и пароль к своему аккаунту.")
                return
            }

            Class.forName("oracle.jdbc.driver.OracleDriver")
            conn = DriverManager.getConnection(url, login, password)
            statmt = conn!!.createStatement()

            println(runTests())
        }

        private fun runTests(): String {
            val selectFile = File(SELECT_FILE)
            val testsFile = File(TESTS_FILE)

            val flag1 = selectFile.createNewFile()
            val flag2 = testsFile.createNewFile()

            if (flag2)
                testsFile.writeText(
                        "-- Формат: строка1;строка2;...;строкаN;нужное_значение\n" +
                                "// Комменты, начинающиеся с '//', выведутся при запуске\n" +
                                "-- Сработает на дефолтном запросе:\n" +
                                "26.09.2018;26.10.2018;1\n" +
                                "-- Выдаст ошибку на дефолтном запросе:\n" +
                                "26.09.2018;26.10.2018;2\n")

            if (flag1)
                selectFile.writeText(
                        "select abs(months_between(to_date('%s', 'DD.MM.YYYY'), " +
                                "to_date('%s', 'DD.MM.YYYY'))) from dual")


            println("Чтение запроса из файла...")
            val sql = readFile(SELECT_FILE)
            if (sql == "") return "Пустой файл с запросом"
            if (!sql.contains("%s"))
                return "Чтобы подставлять значения из тестов, используйте '%s'."

            println("\n\nТестирование...\n")

            File("tests.txt").forEachLine { testLine(sql, it) }

            return "\n\nВсе тесты выполнены."
        }

        private fun testLine(sql: String, it: String) {
            if (it.isEmpty()) return
            if (it.startsWith("--")) return
            if (it.startsWith("//")) return println(it)
            else if (!it.contains(";")) println("Внимание! Тест в неверном формате: $it")
            test(sql, it.split(";"))

        }

        private fun test(sql: String, inp: List<String>) =
                println(check(sql, inp.subList(0, inp.size - 1).toTypedArray(), inp.last()))


        @Throws(Exception::class)
        fun check(sql: String, vals: Array<String>, expected: String): String {
            var s : String? = null
            try {
                s = replaceVars(sql, vals)
                val resultSet = statmt!!.executeQuery(s)

                resultSet.next()
                val result = resultSet.getString(1)

                return String.format(if (result == expected) CORRECT_OUT else ERROR_OUT,
                        arrToString(vals), result, expected)
            } catch (e: Exception) {
                e.printStackTrace()
                return "Ошибка при обработке: \n\t$s"
            }
        }

        private fun replaceVars(sql: String, vals: Array<String>): String {
            var i = 0
            var s = sql
            while (sql.contains("%s") and (i < vals.size)) {
                s = s.replaceFirst("%s", vals[i])
                i++
            }
            return s
        }

        private fun readFile(fileName: String): String = File(fileName).readText(Charsets.UTF_8)

        private fun arrToString(arr: Array<out Any>): String {
            var s = ""
            for (i in arr) s += "$i, "
            return s.substring(0, s.length - 2)
        }
    }
}