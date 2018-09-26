package ga.patrick

import java.io.File
import java.sql.*

class Main {
    companion object {

        private var conn: Connection? = null
        private var statmt: Statement? = null

        private const val url = "jdbc:oracle:thin:@oraclebi.avalon.ru:1521:orcl12"
        private const val login = "kirniki"
        private const val password = "kirniki"

        private const val ERROR_OUT: String = "При (%s) получено '%s', ожидалось '%s'"
        private const val SELECT_FILE: String = "select.sql"
        private const val TESTS_FILE: String = "tests.txt"

        @JvmStatic
        fun main(args: Array<String>) {
            println("Подключение...")

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
                        "-- Формат: стока1;строка2;...;строкаN;нужное_значение\n" +
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
                try {
                    check(sql, inp.subList(0, inp.size - 1).toTypedArray(), inp.last())
                } catch (e: Exception) {
                    println("> " + e.message)
                }


        @Throws(Exception::class)
        fun check(sql: String, vals: Array<out String>, expected: String): Boolean {

            var result: String? = null
            var s: String = ""
            try {
                s = sql.format("26.09.2018", "26.10.2018")// vals)
                val resultSet = statmt!!.executeQuery(s)

                resultSet.next()
                result = resultSet.getString(1)
                if (result == expected) return true
            } catch (e: Exception) {
                println("Ошибка при обработке: \n\t$s")
                e.printStackTrace()
            }

            throw Exception(String.format(ERROR_OUT, arrToString(vals), result, expected))
        }

        private fun readFile(fileName: String): String = File(fileName).readText(Charsets.UTF_8)

        private fun arrToString(arr: Array<out Any>) : String {
            var s = ""
            for (i in arr) s += "$i, "
            return s
        }
    }
}